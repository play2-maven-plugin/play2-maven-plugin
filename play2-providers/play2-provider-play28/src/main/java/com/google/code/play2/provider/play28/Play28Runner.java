/*
 * Copyright 2013-2020 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play28;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import play.core.Build;
import play.core.BuildLink;
import play.core.server.ReloadableServer;

import com.google.code.play2.provider.api.Play2DevServer;
import com.google.code.play2.provider.api.Play2Runner;
import com.google.code.play2.provider.api.Play2RunnerConfiguration;

import com.google.code.play2.provider.play28.run.AssetsClassLoader;
import com.google.code.play2.provider.play28.run.NamedURLClassLoader;
import com.google.code.play2.provider.play28.run.Reloader;
import com.google.code.play2.provider.play28.run.ReloaderApplicationClassLoaderProvider;
import com.google.code.play2.provider.play28.run.ReloaderPlayDevServer;
import com.google.code.play2.provider.play28.run.ServerStartException;

import play.runsupport.classloader.DelegatingClassLoader;

public class Play28Runner
    implements Play2Runner
{
    private static final String DEV_SERVER_MAIN_CLASS = "play.core.server.DevServerStart";

    @Override
    public String getServerMainClass()
    {
        return "play.core.server.ProdServerStart";
    }

    @Override
    public boolean supportsRunInDevMode()
    {
        return true;
    }

    @Override
    public String getPlayDocsModuleId( String scalaBinaryVersion, String playVersion )
    {
        return null;
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
            new DelegatingClassLoader( commonClassLoader, Build.sharedClasses, buildLoader,
                                       applicationClassLoaderProvider );
        ClassLoader applicationLoader =
            new NamedURLClassLoader( "PlayDependencyClassLoader",
                                     Reloader.toUrls( configuration.getDependencyClasspath() ), delegatingLoader );
        ClassLoader assetsLoader =
            new AssetsClassLoader( applicationLoader, configuration.getAssetsPrefix(),
                                   configuration.getAssetsDirectory() );

        Reloader reloader =
            new Reloader( configuration.getBuildLink(), assetsLoader, configuration.getBaseDirectory(),
                          configuration.getOutputDirectories(), configuration.getDevSettings() );
        applicationClassLoaderProvider.setReloader( reloader );

        try
        {
            Class<?> mainClass = applicationLoader.loadClass( DEV_SERVER_MAIN_CLASS );
            String mainMethod = configuration.getHttpPort() != null ? "mainDevHttpMode" : "mainDevOnlyHttpsMode";
            int port =
                configuration.getHttpPort() != null ? configuration.getHttpPort().intValue()
                                : configuration.getHttpsPort().intValue();
            String httpAddress = configuration.getHttpAddress();
            Method mainDev =
                mainClass.getMethod( mainMethod, BuildLink.class, Integer.TYPE, String.class );
            ReloadableServer server =
                (ReloadableServer) mainDev.invoke( null, reloader, port, httpAddress );

            return new ReloaderPlayDevServer( server, reloader );
        }
        catch ( Throwable t )
        {
            if ( "play.core.server.ServerListenException".equals( getRootCause( t ).getClass().getName() ) )
            {
                throw new ServerStartException( t );
            }
            throw t;
        }
    }

    private Throwable getRootCause( Throwable t )
    {
        return t.getCause() == null ? t : getRootCause( t.getCause() );
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
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();

        return new URLClassLoader( commonClasspath.toArray( new URL[commonClasspath.size()] ), parent );
    }

}
