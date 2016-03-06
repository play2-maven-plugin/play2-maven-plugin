/*
 * Copyright 2013-2016 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.provider.api.AssetCompilationException;
import com.google.code.play2.provider.api.LessCompilationResult;
import com.google.code.play2.provider.api.Play2LessCompiler;
import com.google.code.play2.provider.api.Play2Provider;

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
    private static final String cacheDirectoryName = "cache";

    private static final String cacheFileName = "less";

    /**
     * Less compiler entry points includes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.lessEntryPointsIncludes", defaultValue = "**/*.less" )
    private String lessEntryPointsIncludes;

    /**
     * Less compiler entry points excludes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.lessEntryPointsExcludes", defaultValue = "**/_*" )
    private String lessEntryPointsExcludes;

    /**
     * Less compiler options, separated by spaces.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.lessOptions", defaultValue = "" )
    private String lessOptions;

    /**
     * For M2E integration.
     */
    @Component
    private BuildContext buildContext;

    @Override
    protected String getAssetsIncludes()
    {
        return lessEntryPointsIncludes;
    }

    @Override
    protected String getAssetsExcludes()
    {
        return lessEntryPointsExcludes;
    }

    @Override
    protected void compileAssets( File assetsSourceDirectory, String[] fileNames, File outputDirectory )
        throws AssetCompilationException, IOException, MojoExecutionException
    {
        LessDependencyCache allDependencies = new LessDependencyCache();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        File cacheDirectory = new File( targetDirectory, cacheDirectoryName );
        File lessCacheFile = new File( cacheDirectory, cacheFileName );
        if ( lessCacheFile.isFile() )
        {
            allDependencies.readFromFile( lessCacheFile );
        }

        LessDependencyCache newAllDependencies = new LessDependencyCache();

        int compiledFiles = 0;

        Play2Provider play2Provider = getProvider();
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
            Set<String> fileDependencies = allDependencies.get( templateFile.getAbsolutePath() );

            // check if file needs recompilation
            boolean modified = true;
            if ( fileDependencies != null ) // not first compilation
            {
                if ( cssFile.isFile() && minifiedCssFile.isFile() )
                {
                    modified =
                        cssFile.lastModified() < templateFile.lastModified() && minifiedCssFile.lastModified() < templateFile.lastModified();
                }

                // maybe dependent files are modified
                if ( !modified )
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
                    if ( !file.getPath().equals( templateFile.getAbsolutePath() ) )
                    {
                        fileDependencies.add( file.getPath() );
                    }
                }
                compiledFiles++;
            }
            else
            {
                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
            }
            newAllDependencies.set( templateFile.getAbsolutePath(), fileDependencies );
        }

        //getLog().debug( newAllDependencies.toString() );
        if ( !newAllDependencies.equals( allDependencies ) )
        {
            createDirectory( lessCacheFile.getParentFile(), false );
            newAllDependencies.writeToFile( lessCacheFile );
        }

        getLog().info( String.format( "%d assets processed, %d compiled", Integer.valueOf( fileNames.length ),
                                      Integer.valueOf( compiledFiles ) ) );
    }

}
