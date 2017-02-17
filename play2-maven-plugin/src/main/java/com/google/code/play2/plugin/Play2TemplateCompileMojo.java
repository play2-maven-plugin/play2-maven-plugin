/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.codehaus.plexus.util.DirectoryScanner;

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
    extends AbstractPlay2SourceGeneratorMojo
{

    /**
     * Main language ("scala" or "java").
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.mainLang", required = true, defaultValue = "scala" )
    private String mainLang;

    /**
     * Additional imports for templates.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.templateAdditionalImports" )
    private String templateAdditionalImports;

    private static final String[] SCALA_TEMPLATES_INCLUDES = new String[] { "**/*.scala.*" };

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( !"java".equals( mainLang ) && !"scala".equals( mainLang ) )
        {
            throw new MojoExecutionException(
                                              String.format( "Template compilation failed  - unsupported <mainLang> configuration parameter value \"%s\"",
                                                             mainLang ) );
        }

        Play2Provider play2Provider = getProvider();
        Play2TemplateCompiler compiler = play2Provider.getTemplatesCompiler();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        String outputDirectoryName = compiler.getCustomOutputDirectoryName();
        if ( outputDirectoryName == null )
        {
            outputDirectoryName = DEFAULT_TARGET_DIRECTORY_NAME;
        }
        File generatedDirectory = new File( targetDirectory, outputDirectoryName + "/main" );
        compiler.setOutputDirectory( generatedDirectory );

        List<String> resolvedAdditionalImports = Collections.emptyList();
        if ( "java".equalsIgnoreCase( mainLang ) )
        {
            resolvedAdditionalImports = compiler.getDefaultJavaImports();
        }
        else if ( "scala".equalsIgnoreCase( mainLang ) )
        {
            resolvedAdditionalImports = compiler.getDefaultScalaImports();
        }
        if ( templateAdditionalImports != null && !"".equals( templateAdditionalImports ) )
        {
            resolvedAdditionalImports = new ArrayList<String>( resolvedAdditionalImports ); // mutable list
            String[] additionalImports = templateAdditionalImports.split( "[ \\r\\n]+" );
            resolvedAdditionalImports.addAll( Arrays.asList( additionalImports ) );
        }
        compiler.setAdditionalImports( resolvedAdditionalImports );

        int processedFiles = 0;
        int compiledFiles = 0;
        TemplateCompilationException firstException = null;

        List<String> compileSourceRoots = project.getCompileSourceRoots();
        for ( String sourceRoot : compileSourceRoots )
        {
            File sourceRootDirectory = new File( sourceRoot );
            if ( sourceRootDirectory.isDirectory()
                && !sourceRootDirectory.getAbsolutePath().startsWith( targetDirectory.getAbsolutePath() ) )
            {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( sourceRootDirectory );
                scanner.setIncludes( SCALA_TEMPLATES_INCLUDES );
                scanner.addDefaultExcludes();
                scanner.scan();
                String[] files = scanner.getIncludedFiles();

                if ( files.length > 0 )
                {
                    compiler.setSourceDirectory( sourceRootDirectory );

                    for ( String fileName : files )
                    {
                        File templateFile = new File( sourceRootDirectory, fileName );
                        try
                        {
                            File generatedFile = compiler.compile( templateFile );
                            processedFiles++;
                            if ( generatedFile != null )
                            {
                                compiledFiles++;
                                getLog().debug( String.format( "\"%s\" processed", fileName ) );
                                buildContext.refresh( generatedFile );
                            }
                            else
                            {
                                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
                            }
                        }
                        catch ( TemplateCompilationException e )
                        {
                            if ( firstException == null )
                            {
                                firstException = e;
                            }
                            reportCompilationProblems( templateFile, e );
                        }
                    }
                }
            }
        }

        if ( processedFiles > 0 )
        {
            if ( firstException != null )
            {
                throw new MojoFailureException( "Template compilation failed", firstException );
            }

            getLog().info( String.format( "%d template%s processed, %d compiled", Integer.valueOf( processedFiles ),
                                          processedFiles > 1 ? "s" : "", Integer.valueOf( compiledFiles ) ) );

            addSourceRoot( generatedDirectory );
            configureSourcePositionMappers();
        }
        else
        {
            getLog().info( "No templates to compile" );
        }
    }

}
