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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.codehaus.plexus.util.DirectoryScanner;

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
    extends AbstractPlay2Mojo
{

    private final static String assetsSourceDirectoryName = "app/assets";

    private final static String targetDirectoryName = "resource_managed/main";

    private final static String[] lessExcludes = new String[] { "**/_*" };

    private final static String[] lessIncludes = new String[] { "**/*.less" };

    private final static String cacheFileName = ".less-deps";

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File basedir = project.getBasedir();
        File assetsSourceDirectory = new File( basedir, assetsSourceDirectoryName );

        if ( !assetsSourceDirectory.isDirectory() )
            return; // nothing to do

        LessDependencyCache allDependencies = new LessDependencyCache();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        File depsFile = new File( targetDirectory, cacheFileName );
        if ( depsFile.isFile() )
        {
            //TODO - change file format, disable caching for now
            // allDependencies.readFromFile( depsFile );
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( assetsSourceDirectory );
        scanner.setIncludes( lessIncludes );
        scanner.setExcludes( lessExcludes );
        scanner.addDefaultExcludes();
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        LessDependencyCache newAllDependencies = new LessDependencyCache();
        if ( files.length > 0 )
        {
            File generatedDirectory = new File( targetDirectory, targetDirectoryName );
            File outputDirectory = new File( generatedDirectory, "public" );

            for ( String fileName : files )
            {
                getLog().debug( String.format( "Processing file \"%s\"", fileName ) );
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
                    Play2LessCompiler compiler = play2Provider.getLessCompiler();
                    try
                    {
                        LessCompilationResult result = compiler.compile( templateFile );
                        String cssContent = result.getCss();
                        String minifiedCssContent = result.getMinifiedCss();
                        // writeOutputToFiles(new File(generatedDirectory, "public"), fileName, cssContent,
                        // minifiedCssContent);
                        createDirectory( cssFile.getParentFile(), false );
                        writeToFile( cssFile, cssContent );
                        if ( minifiedCssContent != null )
                        {
                            createDirectory( minifiedCssFile.getParentFile(), false );
                            writeToFile( minifiedCssFile, minifiedCssContent );
                        }
                        else
                        {
                            if ( minifiedCssFile.exists() )
                            {// TODO-check if isFile
                                minifiedCssFile.delete();// TODO-check result
                            }
                        }
                        List<File> allSourceFiles = result.getDependencies();
                        fileDependencies = new HashSet<String>();
                        for ( File file : allSourceFiles )
                        {
                            getLog().debug( String.format( "Source file \"%s\"", file.getPath() ) );
                            fileDependencies.add( file.getPath() );
                        }
                        // allDependencies.put(fileName, fileDependencies);
                        // System.out.println(allSourceFiles);
                        // System.out.println(":::end:::");
                    }
                    catch ( AssetCompilationException e )
                    {
                        throw new MojoExecutionException("Less compilation failed", e);
                    }
                }
                newAllDependencies.set( fileName, fileDependencies );
            }
            //TODO - change file format, disable caching for now
            // newAllDependencies.writeToFile( depsFile );

            boolean resourceAlreadyAdded = false;
            for ( Resource res : (List<Resource>) project.getResources() )
            {
                if ( res.getDirectory().equals( generatedDirectory.getAbsolutePath() ) )
                {
                    resourceAlreadyAdded = true;
                    break;
                }
            }
            if ( !resourceAlreadyAdded )
            {
                Resource resource = new Resource();
                resource.setDirectory( generatedDirectory.getAbsolutePath() );
                project.addResource( resource );
                getLog().debug( "Added resource: " + resource.getDirectory() );
            }
        }
    }

}
