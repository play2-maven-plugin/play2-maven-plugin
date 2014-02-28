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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.less.LessDependencyCache;
import com.google.code.play2.provider.AssetCompilationException;
import com.google.code.play2.provider.LessCompilationResult;
import com.google.code.play2.provider.Play2LessCompiler;

/**
 * Compile Less assets
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "less-compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class Play2LessCompileMojo
    extends AbstractPlay2AssetsCompileMojo
{
    private static final String cacheFileName = ".less-deps";

    /**
     * Less compiler entry points includes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.lessEntryPointsIncludes", defaultValue = "**/*.less" )
    private String lessEntryPointsIncludes;

    /**
     * Less compiler entry points excludes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.lessEntryPointsExcludes", defaultValue = "**/_*" )
    private String lessEntryPointsExcludes;

    /**
     * Less compiler options, separated by spaces.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.lessOptions", defaultValue = "" )
    private String lessOptions;

    /**
     * For M2E integration.
     */
    @Component
    private BuildContext buildContext;

    protected String getAssetsIncludes()
    {
        return lessEntryPointsIncludes;
    }

    protected String getAssetsExcludes()
    {
        return lessEntryPointsExcludes;
    }

    protected void compileAssets( File assetsSourceDirectory, String[] fileNames, File outputDirectory )
        throws AssetCompilationException, IOException
    {
        LessDependencyCache allDependencies = new LessDependencyCache();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        File depsFile = new File( targetDirectory, cacheFileName );
        if ( depsFile.isFile() )
        {
            // TODO - change file format, disable caching for now
            // allDependencies.readFromFile( depsFile );
        }

        LessDependencyCache newAllDependencies = new LessDependencyCache();

        Play2LessCompiler compiler = play2Provider.getLessCompiler();
        if ( lessOptions != null )
        {
            compiler.setCompilerOptions( Arrays.asList( lessOptions.split( " " ) ) );
        }

        for ( String fileName : fileNames )
        {
            File templateFile = new File( assetsSourceDirectory, fileName );

            String cssFileName = fileName.replace( ".less", ".css" );
            File cssFile = new File( outputDirectory, cssFileName );

            String minifiedCssFileName = fileName.replace( ".less", ".min.css" );
            File minifiedCssFile = new File( outputDirectory, minifiedCssFileName );

            // previous dependencies
            Set<String> fileDependencies = null;
            // if ( allDependencies != null )
            // {
            fileDependencies = allDependencies.get( fileName );
            // }

            // check if file needs recompilation
            boolean modified = false;
            if ( fileDependencies == null )
            {
                modified = true; // first compilation
            }
            else
            {
                if ( cssFile.isFile() )
                {
                    long cssFileLastModified = cssFile.lastModified();
                    for ( String fName : fileDependencies )
                    {
                        File srcFile = new File( fName );
                        if ( srcFile.isFile() )
                        {
                            if ( cssFileLastModified < srcFile.lastModified() )
                            {
                                modified = true;
                                break;
                            }
                        }
                        else
                        // source file or it's dependency deleted
                        {
                            modified = true;
                            break;
                        }
                    }
                }
                else
                {
                    modified = true; // missing destination .css file
                }
            }

            if ( modified )
            {
                getLog().debug( String.format( "Processing \"%s\"", fileName ) );

                LessCompilationResult result = compiler.compile( templateFile );
                String cssContent = result.getCss();
                String minifiedCssContent = result.getMinifiedCss();
                // writeOutputToFiles(new File(generatedDirectory, "public"), fileName, cssContent,
                // minifiedCssContent);
                createDirectory( cssFile.getParentFile(), false );
                writeToFile( cssFile, cssContent );
                buildContext.refresh( cssFile );
                if ( minifiedCssContent != null )
                {
                    createDirectory( minifiedCssFile.getParentFile(), false );
                    writeToFile( minifiedCssFile, minifiedCssContent );
                    buildContext.refresh( minifiedCssFile );
                }
                else
                {
                    if ( minifiedCssFile.exists() && minifiedCssFile.isFile() && minifiedCssFile.delete() )
                    {
                        buildContext.refresh( minifiedCssFile );
                    }
                }
                List<File> allSourceFiles = result.getDependencies();
                fileDependencies = new HashSet<String>();
                for ( File file : allSourceFiles )
                {
                    //getLog().debug( String.format( "Source file \"%s\"", file.getPath() ) );
                    fileDependencies.add( file.getPath() );
                }
                // allDependencies.put(fileName, fileDependencies);
                // System.out.println(allSourceFiles);
                // System.out.println(":::end:::");
                newAllDependencies.set( fileName, fileDependencies );
            }
            else
            {
                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
            }
        }
        // TODO - change file format, disable caching for now
        // newAllDependencies.writeToFile( depsFile );
    }

}
