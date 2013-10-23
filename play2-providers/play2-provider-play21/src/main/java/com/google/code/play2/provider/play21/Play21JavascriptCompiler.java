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

package com.google.code.play2.provider.play21;

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
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;

//import org.mozilla.javascript.Context;
//import org.mozilla.javascript.Function;
//import org.mozilla.javascript.NativeArray;
//import org.mozilla.javascript.NativeJavaObject;
//import org.mozilla.javascript.Scriptable;
//import org.mozilla.javascript.ScriptableObject;
//import org.mozilla.javascript.tools.shell.Global;

//import org.mozilla.javascript.*;
//import org.mozilla.javascript.tools.shell.*;

import com.google.code.play2.provider.AssetCompilationException;
import com.google.code.play2.provider.JavascriptCompilationResult;
import com.google.code.play2.provider.Play2JavascriptCompiler;

//Based on Play! 2.1.0 framework/src/sbt-plugin/src/main/scala/jscompile/JavascriptCompiler.scala
public class Play21JavascriptCompiler
    implements Play2JavascriptCompiler
{
    private List<String> simpleCompilerOptions = Collections.emptyList();

    // ???
    private List<String> fullCompilerOptions = Collections.emptyList();

    public void setSimpleCompilerOptions( List<String> simpleCompilerOptions )
    {
        this.simpleCompilerOptions = simpleCompilerOptions;
    }

    public void setFullCompilerOptions( List<String> fullCompilerOptions )
    {
        this.fullCompilerOptions = fullCompilerOptions;
    }

    // private String css = null;
    // //?private String minifiedCss = null;
    // private List<File> dependencies;

    // public String getCss()
    // {
    // return css;
    // }
    //
    // public List<File> getDependencies() {
    // return dependencies;
    // }

    public CompileResult compile( File source )
        throws AssetCompilationException, IOException
    {
        boolean simpleCheck = simpleCompilerOptions.contains( "rjs" );

        String origin = readFileContent( source );

        CompilerOptions options = null; // ????fullCompilerOptions;
        if ( options == null )
        {
            CompilerOptions defaultOptions = new CompilerOptions();
            defaultOptions.closurePass = true;
            if ( !simpleCheck )
            {
                defaultOptions.setProcessCommonJSModules( true );
                defaultOptions.setCommonJSModulePathPrefix( source.getParent() + File.separator );
                List<String> entryPoints = new ArrayList<String>( 1 );
                entryPoints.add( toModuleName( source.getName() ) );
                defaultOptions.setManageClosureDependencies( entryPoints );
            }
            for ( String opt : simpleCompilerOptions )
            {
                if ( "advancedOptimizations".equals( opt ) )
                {
                    CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel( defaultOptions );
                }
                else if ( "checkCaja".equals( opt ) )
                {
                    defaultOptions.setCheckCaja( true );
                }
                else if ( "checkControlStructures".equals( opt ) )
                {
                    defaultOptions.setCheckControlStructures( true );
                }
                else if ( "checkTypes".equals( opt ) )
                {
                    defaultOptions.setCheckTypes( true );
                }
                else if ( "checkSymbols".equals( opt ) )
                {
                    defaultOptions.setCheckSymbols( true );
                }
            }
            options = defaultOptions;
        }

        Compiler compiler = new Compiler();
        List<File> all = allSiblings( source );
        List<JSSourceFile> x = new ArrayList<JSSourceFile>();
        if ( !simpleCheck )
        {
            for ( File f : all )
            {
                x.add( JSSourceFile.fromFile( f ) );
            }
        }
        else
        {
            x.add( JSSourceFile.fromFile( source ) );
        }
        JSSourceFile[] input = x.toArray( new JSSourceFile[x.size()] );

        try
        {
            Result result = compiler.compile( new JSSourceFile[0], input, options );
            if ( result.success )
            {
                String minifiedJs = null;
                if ( !simpleCheck )
                {
                    minifiedJs = compiler.toSource();
                }
                return new CompileResult( origin, minifiedJs, null/* was: all */ );
            }
            else
            {
                JSError error = compiler.getErrors()[0];
                File errorFile = null; // FIXME
                // val errorFile = all.find(f => f.getAbsolutePath() == error.sourceName);
                throw new AssetCompilationException( errorFile, error.description, error.lineNumber, null );
            }
        }
        catch ( Exception e )
        {
            // e.printStackTrace();
            throw new AssetCompilationException( source, "Internal Closure Compiler error (see logs)", null, null );
            // throw new MojoFailureException( "Internal Closure Compiler error (see logs)" );
        }
    }

    public String minify( String source, String name )
        throws AssetCompilationException
    {
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();

        if ( name == null )
        {
            name = "unknown";
        }
        JSSourceFile[] input = new JSSourceFile[] { JSSourceFile.fromCode( name, source ) };
        // val input = Array[JSSourceFile](JSSourceFile.fromCode(name.getOrElse("unknown"), source))

        if ( compiler.compile( new JSSourceFile[] {}/* Array[JSSourceFile]() */, input, options ).success )
        {
            return compiler.toSource();
        }
        else
        {
            JSError error = compiler.getErrors()[0];
            throw new AssetCompilationException( null, error.description, error.lineNumber, null );
        }
        /*
         * compiler.compile(Array[JSSourceFile](), input, options).success match { case true => compiler.toSource() case
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

        public String getJs()
        {
            return js;
        }

        public String getMinifiedJs()
        {
            return minifiedJs;
        }

        public List<File> getDependencies()
        {
            return dependencies;
        }

    }
}
