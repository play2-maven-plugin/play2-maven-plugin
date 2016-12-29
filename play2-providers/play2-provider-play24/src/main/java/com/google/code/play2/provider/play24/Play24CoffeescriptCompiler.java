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

package com.google.code.play2.provider.play24;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import com.google.code.play2.provider.api.AssetCompilationException;
import com.google.code.play2.provider.api.CoffeescriptCompilationResult;
import com.google.code.play2.provider.api.Play2CoffeescriptCompiler;

public class Play24CoffeescriptCompiler
    implements Play2CoffeescriptCompiler
{

    private List<String> compilerOptions = Collections.emptyList();

    @Override
    public void setCompilerOptions( List<String> compilerOptions )
    {
        this.compilerOptions = compilerOptions;
    }

    @Override
    public CoffeescriptCompilationResult compile( File source )
        throws AssetCompilationException, IOException
    {
        try
        {
            String js = compile( source, compilerOptions.contains( "bare" ) );
            return new CompileResult( js );
        }
        catch ( JavaScriptException e )
        {
            Scriptable error = (Scriptable) e.getValue();
            String msg = (String) ScriptableObject.getProperty( error, "message" );
            int line = 0;
            Matcher m = Pattern.compile( ".*on line ([0-9]+).*" ).matcher( msg );
            if ( m.find() )
            {
                line = Integer.parseInt( m.group( 1 ) );
            }
            throw new AssetCompilationException( source, msg, line, 0/*null*/ );
        }
    }

    private String compile( File source, boolean bare )
        throws IOException
    {
        Context ctx = Context.enter();
        ctx.setOptimizationLevel( -1 );
        Global global = new Global();
        global.init( ctx );
        Scriptable scope = ctx.initStandardObjects( global );

        Object wrappedCoffeescriptCompiler = Context.javaToJS( this, scope );
        ScriptableObject.putProperty( scope, "CoffeescriptCompiler", wrappedCoffeescriptCompiler );

        ctx.evaluateReader( scope,
                            new InputStreamReader(
                                                   this.getClass().getClassLoader().getResource( "coffee-script.js" ).openConnection().getInputStream(),
                                                   "UTF-8" ), "coffee-script.js", 1, null );

        NativeObject coffee = (NativeObject) scope.get( "CoffeeScript", scope );
        Function compilerFunction = (Function) coffee.get( "compile", scope );

        Context.exit();

        String coffeeCode = readFileContent( source ); // Path(source).string.replace("\r", "");
        Scriptable options = ctx.newObject( scope );
        options.put( "bare", options, Boolean.valueOf( bare ) );
        return (String) Context.call( null, compilerFunction, scope, scope, new Object[] { coffeeCode, options } );
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
        implements CoffeescriptCompilationResult
    {
        private String js;

        public CompileResult( String js )
        {
            this.js = js;
        }

        @Override
        public String getJs()
        {
            return js;
        }
    }

}
