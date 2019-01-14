/*
 * Copyright 2013-2019 Grzegorz Slowikowski (gslowikowski at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.code.play2.provider.play22;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

import play.api.PlayException;
import play.api.UsefulException;

import play.core.SBTDocHandler;
import play.core.SBTLink;
import play.core.server.ServerWithStop;

import com.google.code.play2.provider.api.Play2DevServer;
import com.google.code.play2.provider.api.Play2Runner;
import com.google.code.play2.provider.api.Play2RunnerConfiguration;

import com.google.code.play2.provider.play22.run.NamedURLClassLoader;
import com.google.code.play2.provider.play22.run.Reloader;
import com.google.code.play2.provider.play22.run.ReloaderApplicationClassLoaderProvider;
import com.google.code.play2.provider.play22.run.ReloaderPlayDevServer;
import play.runsupport.classloader.DelegatingClassLoader;

public class Play22Runner
    implements Play2Runner
{
    private static final List<String> BUILD_SHARED_CLASSES;

    static
    {
        List<String> list = new ArrayList<String>();
        list.add( SBTLink.class.getName() );
        list.add( SBTDocHandler.class.getName() );
        list.add( ServerWithStop.class.getName() );
        list.add( UsefulException.class.getName() );
        list.add( PlayException.class.getName() );
        list.add( PlayException.InterestingLines.class.getName() );
        list.add( PlayException.RichDescription.class.getName() );
        list.add( PlayException.ExceptionSource.class.getName() );
        list.add( PlayException.ExceptionAttachment.class.getName() );
        BUILD_SHARED_CLASSES = Collections.unmodifiableList( list );
    }

    @Override
    public String getServerMainClass()
    {
        return "play.core.server.NettyServer";
    }

    @Override
    public boolean supportsRunInDevMode()
    {
        return true;
    }

    @Override
    public String getPlayDocsModuleId( String scalaBinaryVersion, String playVersion )
    {
        return String.format( "com.typesafe.play:play-docs_%s:%s", scalaBinaryVersion, playVersion );
    }

    @Override
    public Play2DevServer runInDevMode( Play2RunnerConfiguration configuration )
        throws Throwable
    {
        ClassLoader buildLoader = Reloader.class.getClassLoader();
        ClassLoader commonClassLoader = commonClassLoader( configuration.getDependencyClasspath() );
        ReloaderApplicationClassLoaderProvider applicationClassLoaderProvider =
            new ReloaderApplicationClassLoaderProvider();
        ClassLoader delegatingLoader =
            new DelegatingClassLoader( commonClassLoader, BUILD_SHARED_CLASSES, buildLoader,
                                       applicationClassLoaderProvider );
        ClassLoader applicationLoader =
            new NamedURLClassLoader( "PlayDependencyClassLoader",
                                     Reloader.toUrls( configuration.getDependencyClasspath() ), delegatingLoader );

        Reloader reloader =
            new Reloader( configuration.getBuildLink(), applicationLoader, configuration.getBaseDirectory(),
                          configuration.getOutputDirectories(), configuration.getDevSettings() );
        applicationClassLoaderProvider.setReloader( reloader );

        ClassLoader docsLoader =
            new URLClassLoader( Reloader.toUrls( configuration.getDocsClasspath() ), applicationLoader );
        JarFile docsJarFile = new JarFile( configuration.getDocsFile() ); // throws NPE if docsFile == null
        Class<?> docHandlerFactoryClass = docsLoader.loadClass( "play.docs.SBTDocHandlerFactory" );
        Method factoryMethod = docHandlerFactoryClass.getMethod( "fromJar", JarFile.class, String.class );
        SBTDocHandler sbtDocHandler = (SBTDocHandler) factoryMethod.invoke( null, docsJarFile, "play/docs/content" );

        Class<?> mainClass = applicationLoader.loadClass( "play.core.server.NettyServer" );
        String mainMethod = configuration.getHttpPort() != null ? "mainDevHttpMode" : "mainDevOnlyHttpsMode";
        int port =
            configuration.getHttpPort() != null ? configuration.getHttpPort().intValue()
                            : configuration.getHttpsPort().intValue();
        Method mainDev = mainClass.getMethod( mainMethod, SBTLink.class, SBTDocHandler.class, Integer.TYPE );
        ServerWithStop server = (ServerWithStop) mainDev.invoke( null, reloader, sbtDocHandler, port );

        return new ReloaderPlayDevServer( server, docsJarFile, reloader );
    }

    private ClassLoader commonClassLoader( List<File> classpath )
        throws MalformedURLException
    {
        List<URL> commonClasspath = new ArrayList<URL>( 1 );
        for ( File depFile : classpath )
        {
            String name = depFile.getName();
            if ( name.startsWith( "h2-" ) )
            {
                commonClasspath.add( depFile.toURI().toURL() );
            }
        }

        // sun.misc.Launcher$ExtClassLoader (see: https://github.com/playframework/playframework/pull/3420)
        // #3420 was fixed for 2.4.0-M2
        // (https://github.com/playframework/playframework/commit/1091e5a267eb72262f8f384b691799491ea44936)
        // Backported here only in play2-maven-plugin.
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();

        return new URLClassLoader( commonClasspath.toArray( new URL[commonClasspath.size()] ), parent );
    }

}
