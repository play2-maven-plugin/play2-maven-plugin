/*
 * Copyright 2013-2015 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play25;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

//import org.mozilla.javascript.Context;
//import org.mozilla.javascript.Function;
//import org.mozilla.javascript.NativeArray;
//import org.mozilla.javascript.NativeJavaObject;
//import org.mozilla.javascript.Scriptable;
//import org.mozilla.javascript.ScriptableObject;
//import org.mozilla.javascript.tools.shell.Global;

//import org.mozilla.javascript.*;
//import org.mozilla.javascript.tools.shell.*;

import com.google.code.play2.provider.api.AssetCompilationException;
import com.google.code.play2.provider.api.JavascriptCompilationResult;
import com.google.code.play2.provider.api.Play2JavascriptCompiler;

public class Play25JavascriptCompiler
    implements Play2JavascriptCompiler
{
    private List<String> compilerOptions = Collections.emptyList();

    @Override
    public void setCompilerOptions( List<String> compilerOptions )
    {
        this.compilerOptions = compilerOptions;
    }

    @Override
    public CompileResult compile( File source )
        throws AssetCompilationException, IOException
    {
        boolean requireJsMode = compilerOptions.contains( "rjs" );
        boolean commonJsMode = compilerOptions.contains( "commonJs" ) && !requireJsMode;

        String origin = readFileContent( source );

        CompilerOptions options = getOptions( source, commonJsMode );

        Compiler compiler = new Compiler();
        List<File> all = allSiblings( source );
        // In commonJsMode, we use all JavaScript sources in the same directory for some reason.
        // Otherwise, we only look at the current file.
        List<SourceFile> inputs = new ArrayList<SourceFile>();
        if ( commonJsMode )
        {
            for ( File f : all )
            {
                inputs.add( SourceFile.fromFile( f ) );
            }
        }
        else
        {
            inputs.add( SourceFile.fromFile( source ) );
        }

        try
        {
            List<SourceFile> externs = Collections.emptyList();
            Result result = compiler.compile( externs, inputs, options );
            if ( result.success )
            {
                String minifiedJs = null;
                if ( !requireJsMode )
                {
                    minifiedJs = compiler.toSource();
                }
                return new CompileResult( origin, minifiedJs, null );
            }
            else
            {
                // val error = compiler.getErrors().head
                // val errorFile = all.find(f => f.getAbsolutePath() == error.sourceName)
                // throw AssetCompilationException(errorFile, error.description, Some(error.lineNumber), None)
                JSError error = compiler.getErrors()[0];
                File errorFile = null;
                for ( File f: all )
                {
                    if ( f.getAbsolutePath().equals( error.sourceName ) )
                    {
                        errorFile = f;
                        break;
                    }
                }
                throw new AssetCompilationException( errorFile, error.description, error.lineNumber, null );
            }
        }
        catch ( Exception e )
        {
            throw new AssetCompilationException( source, "Internal Closure Compiler error (see logs)", null, null, e );
        }
    }

    private CompilerOptions getOptions( File source, boolean commonJsMode )
    {
        //TODO - add a possibility to specify "fullCompilerOptions"
        CompilerOptions defaultOptions = new CompilerOptions();
        defaultOptions.closurePass = true;

        if ( commonJsMode )
        {
            defaultOptions.setProcessCommonJSModules( true );
            // The compiler always expects forward slashes even on Windows.
            defaultOptions.setCommonJSModulePathPrefix( ( source.getParent() + File.separator ).replaceAll( "\\\\",
                                                                                                            "/" ) );
            List<String> entryPoints = new ArrayList<String>( 1 );
            entryPoints.add( toModuleName( source.getName() ) );
            defaultOptions.setManageClosureDependencies( entryPoints );
        }

        for ( String opt : compilerOptions )
        {
            if ( "advancedOptimizations".equals( opt ) )
            {
                CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel( defaultOptions );
            }
            /*???else if ( "checkCaja".equals( opt ) )
            {
                defaultOptions.setCheckCaja( true );
            }
            else if ( "checkControlStructures".equals( opt ) )
            {
                defaultOptions.setCheckControlStructures( true );
            }???*/
            else if ( "checkTypes".equals( opt ) )
            {
                defaultOptions.setCheckTypes( true );
            }
            else if ( "checkSymbols".equals( opt ) )
            {
                defaultOptions.setCheckSymbols( true );
            }
            else if ( "ecmascript5".equals( opt ) )
            {
                defaultOptions.setLanguageIn( CompilerOptions.LanguageMode.ECMASCRIPT5 );
            }
        }
        return defaultOptions;
    }

    @Override
    public String minify( String source, String name )
        throws AssetCompilationException
    {
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();

        if ( name == null )
        {
            name = "unknown";
        }
        List<SourceFile> inputs = new ArrayList<SourceFile>( 1 );
        inputs.add( SourceFile.fromCode( name, source ) );

        List<SourceFile> externs = Collections.emptyList();
        if ( compiler.compile( externs, inputs, options ).success )
        {
            return compiler.toSource();
        }
        else
        {
            JSError error = compiler.getErrors()[0];
            throw new AssetCompilationException( null, error.description, error.lineNumber, null );
        }
        /*
         * compiler.compile(Array[SourceFile](), input, options).success match { case true => compiler.toSource() case
         * false => { val error = compiler.getErrors().head throw AssetCompilationException(None, error.description,
         * Some(error.lineNumber), None) } }
         */
    }

    /**
     * Turns a filename into a JS identifier that is used for moduleNames in rewritten code. Removes leading ./,
     * replaces / with $, removes trailing .js and replaces - with _. All moduleNames get a "module$" prefix.
     */
    private String toModuleName( String filename )
    {
        return "module$"
            + filename.replaceAll( "^\\./", "" ).replaceAll( "/", "\\$" ).replaceAll( "\\.js$", "" ).replaceAll( "-",
                                                                                                                 "_" );
    }

    /**
     * Return all Javascript files in the same directory than the input file, or subdirectories
     */
    private List<File> allSiblings( File source )
    {
        return allJsFilesIn( source.getParentFile() );
    }

    private List<File> allJsFilesIn( File dir )/* : Seq[File] = */
    {
        List<File> result = new ArrayList<File>();
        // import scala.collection.JavaConversions._
        File[] jsFiles = dir.listFiles( new FileFilter()
        {
            public boolean accept( File f )
            {
                return f.getName().endsWith( ".js" );
            }
        } );
        result.addAll( Arrays.asList( jsFiles ) );
        File[] directories = dir.listFiles( new FileFilter()
        {
            public boolean accept( File f )
            {
                return f.isDirectory();
            }
        } );
        for ( File directory : directories )
        {
            result.addAll( allJsFilesIn( directory ) );
        }
        // val jsFilesChildren = directories.map(d => allJsFilesIn(d)).flatten
        // jsFiles ++ jsFilesChildren
        return result;
    }

    private String readFileContent( File file )
        throws IOException
    {
        String result = null;

        BufferedReader is = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ) );
        try
        {
            StringBuilder sb = new StringBuilder();
            String line = is.readLine();
            while ( line != null )
            {
                sb.append( line ).append( '\n' );
                line = is.readLine();
            }
            result = sb.toString();
        }
        finally
        {
            is.close();
        }
        return result;
    }

    public static class CompileResult
        implements JavascriptCompilationResult
    {
        private String js;

        private String minifiedJs;

        private List<File> dependencies;

        public CompileResult( String js, String minifiedJs, List<File> dependencies )
        {
            this.js = js;
            this.minifiedJs = minifiedJs;
            this.dependencies = dependencies;
        }

        @Override
        public String getJs()
        {
            return js;
        }

        @Override
        public String getMinifiedJs()
        {
            return minifiedJs;
        }

        @Override
        public List<File> getDependencies()
        {
            return dependencies;
        }

    }
}
