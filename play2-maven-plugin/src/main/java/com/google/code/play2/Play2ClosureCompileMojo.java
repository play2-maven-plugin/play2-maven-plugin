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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.code.play2.provider.AssetCompilationException;
import com.google.code.play2.provider.JavascriptCompilationResult;
import com.google.code.play2.provider.Play2JavascriptCompiler;

/**
 * Compile JavaScript assets
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "closure-compile", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class Play2ClosureCompileMojo
    extends AbstractPlay2AssetsCompileMojo
{
    /**
     * Javascript compiler entry points includes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.javascriptEntryPointsIncludes", defaultValue = "**/*.js" )
    private String javascriptEntryPointsIncludes;

    /**
     * Javascript compiler entry points excludes, separated by commas.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.javascriptEntryPointsExcludes", defaultValue = "**/_*" )
    private String javascriptEntryPointsExcludes;

    /**
     * Javascript compiler options, separated by spaces.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.closureCompilerOptions", defaultValue = "" )
    private String closureCompilerOptions;

    protected String getAssetsIncludes()
    {
        return javascriptEntryPointsIncludes;
    }

    protected String getAssetsExcludes()
    {
        return javascriptEntryPointsExcludes;
    }

    protected void compileAssets( File assetsSourceDirectory, String[] fileNames, File outputDirectory )
        throws AssetCompilationException, IOException
    {
        Play2JavascriptCompiler compiler = play2Provider.getJavascriptCompiler();
        if ( closureCompilerOptions != null )
        {
            compiler.setCompilerOptions( Arrays.asList( closureCompilerOptions.split( " " ) ) );
        }

        for ( String fileName : fileNames )
        {
            getLog().debug( String.format( "Processing file \"%s\"", fileName ) );
            File srcJsFile = new File( assetsSourceDirectory, fileName );

            // String jsFileName = fileName.replace( ".coffee", ".js" );
            File jsFile = new File( outputDirectory, fileName/* jsFileName */ );

            String minifiedJsFileName = fileName.replace( ".js", ".min.js" );
            File minifiedJsFile = new File( outputDirectory, minifiedJsFileName );

            boolean modified = true;
            if ( jsFile.isFile() )
            {
                modified = ( jsFile.lastModified() < srcJsFile.lastModified() );
            }

            if ( modified )
            {
                JavascriptCompilationResult result = compiler.compile( srcJsFile );
                String jsContent = result.getJs();
                String minifiedJsContent = result.getMinifiedJs();
                createDirectory( jsFile.getParentFile(), false );
                writeToFile( jsFile, jsContent );
                if ( minifiedJsContent != null )
                {
                    createDirectory( minifiedJsFile.getParentFile(), false );
                    writeToFile( minifiedJsFile, minifiedJsContent );
                }
                else
                {
                    if ( minifiedJsFile.exists() )
                    { // TODO-check if isFile
                        minifiedJsFile.delete(); // TODO-check result
                    }
                }
            }
        }
    }

}
