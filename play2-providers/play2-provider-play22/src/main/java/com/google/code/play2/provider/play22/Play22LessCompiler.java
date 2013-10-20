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

package com.google.code.play2.provider.play22;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import com.google.code.play2.provider.AssetCompilationException;
import com.google.code.play2.provider.LessCompilationResult;
import com.google.code.play2.provider.Play2LessCompiler;

// Based on Play! 2.2.0 framework/src/sbt-plugin/src/main/scala/less/LessCompiler.scala
public class Play22LessCompiler
    implements Play2LessCompiler
{
    public final static String LESS_SCRIPT = "less-1.4.2.js";
                    
    public LessCompilationResult compile( File source )
        throws AssetCompilationException, IOException
    {
        try
        {
            InternalCompileResult result1 = compile( source, false );
            InternalCompileResult result2 = compile( source, true );
            return new CompileResult( result1.css, result2.css, result1.dependencies );
        }
        catch ( JavaScriptException e )
        {
            Scriptable error = (Scriptable) e.getValue();
            String filename = ScriptableObject.getProperty( error, "filename" ).toString();
            //stare File file = ( filename == source.getAbsolutePath() ? source : resolve( source, filename ) );
            File file = new File( filename );
            throw new AssetCompilationException(
                                                 file,
                                                 ScriptableObject.getProperty( error, "message" ).toString(),
                                                 Integer.valueOf( ( (Double) ScriptableObject.getProperty( error,
                                                                                                           "line" ) ).intValue() ),
                                                 Integer.valueOf( ( (Double) ScriptableObject.getProperty( error,
                                                                                                           "column" ) ).intValue() ) );
        }
    }

    private String multiLineString( String[] lines )
    {
        StringBuilder sb = new StringBuilder();
        if ( lines != null && lines.length > 0 )
        {
            sb.append( lines[0] );
            for ( int i = 1; i < lines.length; i++ )
            {
                sb.append( "\n" );
                sb.append( lines[i] );
            }
        }
        return sb.toString();
    }

    public InternalCompileResult compile( File source, boolean minify )
        throws IOException
    {
        Context ctx = Context.enter();
        Global global = new Global();
        global.init( ctx );
        Scriptable scope = ctx.initStandardObjects( global );

        Object wrappedLessCompiler = Context.javaToJS( this, scope );
        ScriptableObject.putProperty( scope, "LessCompiler", wrappedLessCompiler );

        ctx.evaluateString( scope, multiLineString( new String[] {
            "",
            "                var timers = [],",
            "                    window = {",
            "                        document: {",
            "                            getElementById: function(id) { ",
            "                                return [];", "                            },",
            "                            getElementsByTagName: function(tagName) {",
            "                                return [];", "                            }",
            "                        },",
            "                        location: {",
            "                            protocol: 'file:', ", "                            hostname: 'localhost', ",
            "                            port: '80'", "                        },",
            "                        setInterval: function(fn, time) {",
            "                            var num = timers.length;",
            "                            timers[num] = fn.call(this, null);",
            "                            return num;", "                        }",
            "                    },",
            "                    document = window.document,",
            "                    location = window.location,",
            "                    setInterval = window.setInterval;",
            "",
            "            "
            } ), "browser.js", 1, null );
        ctx.evaluateReader( scope, new InputStreamReader(
                                                   getClass().getClassLoader().getResource( LESS_SCRIPT ).openConnection().getInputStream() ),
                                                   LESS_SCRIPT, 1, null );
        ctx.evaluateString( scope, multiLineString( new String[] {
            "                var compile = function(source) {",
            "",
            "                    var compiled;",
            "                    // Import tree context",
            "                    var context = [source];",
            "                    var dependencies = [source];",
            "",
            "                    window.less.Parser.importer = function(path, paths, fn, env) {",
            "",
            "                        var imported = LessCompiler.resolve(context[context.length - 1], path);",
            "                        var importedName = String(imported.getAbsolutePath());",
            "                        var input = String(LessCompiler.readContent(imported));",
            "",
            "                        // Store it in the contents, for error reporting",
            "                        env.contents[importedName] = input;",
            "",
            "                        context.push(imported);",
            "                        dependencies.push(imported)",
            "",
            "                        new(window.less.Parser)({",
            "                            optimization:3,",
            "                            filename:importedName,",
            "                            contents:env.contents,",
            "                            dumpLineNumbers:window.less.dumpLineNumbers",
            "                        }).parse(input, function (e, root) {",
            "                            fn(e, root, input);",
            "",
            "                            context.pop();",
            "                        });",
            "                    }",
            "",
            "                    new(window.less.Parser)({optimization:3, filename:String(source.getCanonicalPath())}).parse(String(LessCompiler.readContent(source)), function (e,root) {",
            "                        if (e) {",
            "                            throw e;",
            "                        }",
            "                        compiled = root.toCSS({compress: " + ( minify ? "true" : "false" ) + "})", "                    })",
            "",
            "                    return {css:compiled, dependencies:dependencies}",
            "                }",
            "            "
            } ), "compiler.js", 1, null );
        Function compilerFunction = (Function) scope.get( "compile", scope );

        Context.exit();

        Scriptable result = (Scriptable) Context.call( null, compilerFunction, scope, scope, new Object[] { source } );
        String css = ScriptableObject.getProperty( result, "css" ).toString();
        NativeArray dependencies = (NativeArray) ScriptableObject.getProperty( result, "dependencies" );

        // jak zwrocic te wyniki?
        List<File> deps = new ArrayList<File>();
        for ( int i = 0; i < dependencies.getLength(); i++ )
        {
            Object dependency = ScriptableObject.getProperty( dependencies, String.valueOf( i ) );
            if ( dependency instanceof File )
            {
                deps.add( ( (File) dependency ).getCanonicalFile() );
            }
            else if ( dependency instanceof NativeJavaObject )
            {
                Object x = ( (NativeJavaObject) dependency ).unwrap();
                deps.add( ( (File) x ).getCanonicalFile() );
            }
        }

        return new InternalCompileResult( css, deps );

    }

    // ??
    public static String readContent( File file )
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

    // ???
    public static File resolve( File originalSource, String imported )
    {
        return new File( originalSource.getParentFile(), imported );
    }

    private static class InternalCompileResult
    {
        private String css;

        private List<File> dependencies;

        public InternalCompileResult( String css, List<File> dependencies )
        {
            this.css = css;
            this.dependencies = dependencies;
        }

        public String getCss()
        {
            return css;
        }

        public List<File> getDependencies()
        {
            return dependencies;
        }
    }


    public static class CompileResult
        extends InternalCompileResult
        implements LessCompilationResult
    {
        private String minifiedCss;

        public CompileResult( String css, String minifiedCss, List<File> dependencies )
        {
            super( css, dependencies );
            this.minifiedCss = minifiedCss;
        }

        public String getMinifiedCss()
        {
            return minifiedCss;
        }
    }

}
