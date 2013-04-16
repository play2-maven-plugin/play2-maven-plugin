/*
 * Copyright 2013 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import com.avaje.ebean.enhance.agent.Transformer;
import com.avaje.ebean.enhance.ant.OfflineFileTransform;

/**
 * ...
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "ebean-enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2EbeanEnhanceMojo
    extends AbstractPlay2Mojo
{
    /**
     * Project classpath.
     * 
     */
    @Parameter( property = "project.compileClasspathElements", readonly = true, required = true )
    private List<String> classpathElements;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File outputDirectory = new File(project.getBuild().getOutputDirectory());

        classpathElements.remove( outputDirectory.getAbsolutePath() );
        List<File> classpathFiles = new ArrayList<File>( classpathElements.size() );
        for ( String path : classpathElements )
        {
            classpathFiles.add( new File( path ) );
        }

        // PlayCommands:352
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            List<URL> classPathUrls = new ArrayList<URL>();
            for ( File classpathFile : classpathFiles )
            {
                classPathUrls.add( classpathFile.toURI().toURL() );
            }
            classPathUrls.add( outputDirectory.toURI().toURL() );
            URL[] cp = classPathUrls.toArray( new URL[] {} );

            Thread.currentThread().setContextClassLoader( new URLClassLoader( cp, ClassLoader.getSystemClassLoader() ) );

            ClassLoader cl = ClassLoader.getSystemClassLoader();

            Transformer t = new Transformer( cp, "debug=-1" );

            OfflineFileTransform ft =
                new OfflineFileTransform( t, cl, /* classes */outputDirectory.getAbsolutePath(), /* classes */
                                          outputDirectory.getAbsolutePath() );

            Config config =
                ConfigFactory.load( ConfigFactory.parseFileAnySyntax( new File( "conf/application.conf" ) ) );

            String models = null;
            try
            {
                // see https://github.com/playframework/Play20/wiki/JavaEbean
                Set<Map.Entry<String, ConfigValue>> entries = config.getConfig( "ebean" ).entrySet();
                for ( Map.Entry<String, ConfigValue> entry : entries )
                {
                    ConfigValue configValue = entry.getValue();
                    Object configValueUnwrapped = configValue.unwrapped();
                    // TODO-optimize
                    if ( models == null )
                    {
                        models = configValueUnwrapped.toString();
                    }
                    else
                    {
                        models = models + "," + configValueUnwrapped.toString();
                    }
                }
            }
            catch ( ConfigException.Missing e )
            {
                models = "models.*";
            }

            try
            {
                ft.process( models );
            }
            catch ( Throwable/* ? */e )
            {

            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }

}
