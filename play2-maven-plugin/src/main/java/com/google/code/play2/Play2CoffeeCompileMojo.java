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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.codehaus.plexus.util.DirectoryScanner;

import com.google.code.play2.coffeescript.CoffeescriptCompiler;
import com.google.code.play2.jscompile.JavascriptCompiler;

/**
 * Compile Coffee Script assets
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "coffee-compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class Play2CoffeeCompileMojo
    extends AbstractPlay2Mojo
{

    private final static String assetsSourceDirectoryName = "app/assets";

    private final static String targetDirectoryName = "resource_managed/main";

    private final static String[] coffeeExcludes = new String[] {};

    private final static String[] coffeeIncludes = new String[] { "**/*.coffee" };

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File basedir = project.getBasedir();
        File assetsSourceDirectory = new File( basedir, assetsSourceDirectoryName );

        if ( !assetsSourceDirectory.isDirectory() )
            return; // nothing to do

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( assetsSourceDirectory );
        scanner.setExcludes( coffeeExcludes );
        scanner.setIncludes( coffeeIncludes );
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        if ( files.length > 0 )
        {
            File targetDirectory = new File( project.getBuild().getDirectory() );
            File generatedDirectory = new File( targetDirectory, targetDirectoryName );
            File outputDirectory = new File( generatedDirectory, "public" );

            for ( String fileName : files )
            {
                getLog().debug( String.format( "Processing file \"%s\"", fileName ) );
                File coffeeFile = new File( assetsSourceDirectory, fileName );

                String jsFileName = fileName.replace( ".coffee", ".js" );
                File jsFile = new File( outputDirectory, jsFileName );

                String minifiedJsFileName = fileName.replace( ".coffee", ".min.js" );
                File minifiedJsFile = new File( outputDirectory, minifiedJsFileName );

                boolean modified = true;
                if ( jsFile.isFile() )
                {
                    modified = ( jsFile.lastModified() < coffeeFile.lastModified() );
                }

                if ( modified )
                {
                    CoffeescriptCompiler compiler = CoffeescriptCompiler.getInstance();
                    String jsContent = compiler.compile( coffeeFile, new ArrayList<String>()/* TEMP */);
                    createDirectory( jsFile.getParentFile(), false );
                    writeToFile( jsFile, jsContent );
                    try
                    {
                        String minifiedJsContent = JavascriptCompiler.minify( jsContent, coffeeFile.getName() );
                        createDirectory( minifiedJsFile.getParentFile(), false );
                        writeToFile( minifiedJsFile, minifiedJsContent );
                    }
                    catch ( AssetCompilationException e )
                    {
                        // ignore
                        if ( minifiedJsFile.exists() )
                        {// TODO-check if isFile
                            minifiedJsFile.delete();// TODO-check result
                        }
                    }
                }
            }

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
