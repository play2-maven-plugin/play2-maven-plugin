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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.code.play2.provider.Play2EbeanEnhancer;

/**
 * Ebean enhance
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

            Play2EbeanEnhancer enhancer = play2Provider.getEbeanEnhancer();
            enhancer.setOutputDirectory( outputDirectory );
            enhancer.setClassPathUrls( classPathUrls );

            //TODO-conf powinien byc "na bazie" basedir
            enhancer.enhance( new File( "conf/application.conf" ) );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }

}
