/*
 * Copyright 2013-2014 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.codehaus.plexus.util.DirectoryScanner;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2TemplateCompiler;
import com.google.code.play2.provider.api.TemplateCompilationException;

/**
 * Compile Scala templates
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "template-compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class Play2TemplateCompileMojo
    extends AbstractPlay2Mojo
{

    /**
     * Main language ("scala" or "java").
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.mainLang", required = true, defaultValue = "scala" )
    private String mainLang;

    /**
     * For M2E integration.
     */
    @Component
    private BuildContext buildContext;

    private static final String appDirectoryName = "app";

    private static final String targetDirectoryName = "src_managed/main";

    private static final String[] scalaTemplatesIncludes = new String[] { "**/*.scala.*" };

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( !"java".equals( mainLang ) && !"scala".equals( mainLang ) )
        {
            throw new MojoExecutionException(
                                              String.format( "Template compilation failed  - unsupported <mainLang> configuration parameter value \"%s\"",
                                                             mainLang ) );
        }

        File basedir = project.getBasedir();
        File appDirectory = new File( basedir, appDirectoryName );
        if ( !appDirectory.isDirectory() )
        {
            getLog().info( "No templates to compile" );
            return;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( appDirectory );
        scanner.setIncludes( scalaTemplatesIncludes );
        scanner.addDefaultExcludes();
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        if ( files.length == 0 )
        {
            getLog().debug( "No templates to compile" );
            return;
        }
        else
        {
            File targetDirectory = new File( project.getBuild().getDirectory() );
            File generatedDirectory = new File( targetDirectory, targetDirectoryName );

            Play2Provider play2Provider = getProvider();
            Play2TemplateCompiler compiler = play2Provider.getTemplatesCompiler();
            compiler.setAppDirectory( appDirectory );
            compiler.setOutputDirectory( generatedDirectory );
            compiler.setMainLang( mainLang );

            for ( String fileName : files )
            {
                File templateFile = new File( appDirectory, fileName );
                try
                {
                    File generatedFile = compiler.compile( templateFile );
                    if ( generatedFile != null )
                    {
                        getLog().debug( String.format( "\"%s\" processed", fileName ) );
                        buildContext.refresh( generatedFile );
                    }
                    else
                    {
                        getLog().debug( String.format( "\"%s\" skipped", fileName ) );
                    }
                }
                catch ( TemplateCompilationException e )
                {
                    throw new MojoExecutionException( String.format( "Template compilation failed (%s)",
                                                                     templateFile.getPath() ), e );
                }
            }

            if ( !project.getCompileSourceRoots().contains( generatedDirectory.getAbsolutePath() ) )
            {
                project.addCompileSourceRoot( generatedDirectory.getAbsolutePath() );
                getLog().debug( "Added source directory: " + generatedDirectory.getAbsolutePath() );
            }
        }
    }

}
