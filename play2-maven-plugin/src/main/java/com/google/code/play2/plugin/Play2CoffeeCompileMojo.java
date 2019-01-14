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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.provider.api.AssetCompilationException;
import com.google.code.play2.provider.api.CoffeescriptCompilationResult;
import com.google.code.play2.provider.api.Play2CoffeescriptCompiler;
import com.google.code.play2.provider.api.Play2JavascriptCompiler;
import com.google.code.play2.provider.api.Play2Provider;

/**
 * Compile Coffee Script assets
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "coffee-compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class Play2CoffeeCompileMojo
    extends AbstractPlay2AssetsCompileMojo
{
    /**
     * CoffeeScript compiler entry points includes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.coffeeEntryPointsIncludes", defaultValue = "**/*.coffee" )
    private String coffeeEntryPointsIncludes;

    /**
     * CoffeeScript compiler entry points excludes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.coffeeEntryPointsExcludes", defaultValue = "" )
    private String coffeeEntryPointsExcludes;

    /**
     * CoffeeScript compiler options, separated by spaces.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.coffeescriptOptions", defaultValue = "" )
    private String coffeescriptOptions;

    /**
     * For M2E integration.
     */
    @Component
    private BuildContext buildContext;

    @Override
    protected String getAssetsIncludes()
    {
        return coffeeEntryPointsIncludes;
    }

    @Override
    protected String getAssetsExcludes()
    {
        return coffeeEntryPointsExcludes;
    }

    @Override
    protected void compileAssets( File assetsSourceDirectory, String[] fileNames, File outputDirectory )
        throws AssetCompilationException, IOException, MojoExecutionException
    {
        int compiledFiles = 0;

        Play2Provider play2Provider = getProvider();
        Play2CoffeescriptCompiler compiler = play2Provider.getCoffeescriptCompiler();
        if ( coffeescriptOptions != null )
        {
            compiler.setCompilerOptions( Arrays.asList( coffeescriptOptions.split( " " ) ) );
        }

        for ( String fileName : fileNames )
        {
            File coffeeFile = new File( assetsSourceDirectory, fileName );

            String jsFileName = fileName.replace( ".coffee", ".js" );
            File jsFile = new File( outputDirectory, jsFileName );

            String minifiedJsFileName = fileName.replace( ".coffee", ".min.js" );
            File minifiedJsFile = new File( outputDirectory, minifiedJsFileName );

            boolean modified = true;
            if ( jsFile.isFile() && minifiedJsFile.isFile() )
            {
                modified =
                    jsFile.lastModified() < coffeeFile.lastModified() && minifiedJsFile.lastModified() < coffeeFile.lastModified();
            }

            if ( modified )
            {
                getLog().debug( String.format( "Processing \"%s\"", fileName ) );

                CoffeescriptCompilationResult result = compiler.compile( coffeeFile );
                String jsContent = result.getJs();
                createDirectory( jsFile.getParentFile(), false );
                writeToFile( jsFile, "UTF-8", jsContent );
                buildContext.refresh( jsFile );
                try
                {
                    Play2JavascriptCompiler jsCompiler = play2Provider.getJavascriptCompiler();
                    String minifiedJsContent = jsCompiler.minify( jsContent, coffeeFile.getName() );
                    // String minifiedJsContent = JavascriptCompiler.minify( jsContent, coffeeFile.getName() );
                    createDirectory( minifiedJsFile.getParentFile(), false );
                    writeToFile( minifiedJsFile, "UTF-8", minifiedJsContent );
                    compiledFiles++;
                    buildContext.refresh( minifiedJsFile );
                }
                catch ( AssetCompilationException e )
                {
                    if ( minifiedJsFile.exists() && minifiedJsFile.isFile() && minifiedJsFile.delete() )
                    {
                        buildContext.refresh( minifiedJsFile );
                    }
                }
            }
            else
            {
                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
            }
        }

        getLog().info( String.format( "%d %s processed, %d compiled", Integer.valueOf( fileNames.length ),
                                      fileNames.length > 1 ? "assets" : "asset", Integer.valueOf( compiledFiles ) ) );
    }

}
