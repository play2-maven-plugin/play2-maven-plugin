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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.play2.provider.Play2SBTCompiler;
import com.google.code.play2.provider.SBTCompilationException;
import com.google.code.play2.provider.SBTCompilationResult;

import com.typesafe.zinc.Compiler;
import com.typesafe.zinc.Inputs;
import com.typesafe.zinc.Setup;

import sbt.inc.Analysis;

import org.apache.maven.plugin.logging.Log;

import scala.collection.JavaConversions;

public class Play21SBTCompiler
    implements Play2SBTCompiler
{
    private static final String COMPILE_ORDER = "mixed"; // here?

    public String getDefaultScalaVersion()
    {
        return "2.10.0";
    }

    public String getDefaultSbtVersion()
    {
        return "0.12.2";
    }

    public SBTCompilationResult compile( Log mavenLog, File scalaCompilerFile, File scalaLibraryFile,
                                         List<File> scalaExtra, File xsbtiArtifactFile, File compilerInterfaceSrcFile,
                                         List<File> classpath, List<File> sources, File outputDirectory,
                                         List<String> scalacOptions, List<String> javacOptions, File analysisCacheFile,
                                         Map<File, File> cacheMap )
        throws SBTCompilationException
    {
        SbtLogger sbtLogger = new SbtLogger( mavenLog );
        Setup setup =
            Setup.create( scalaCompilerFile, scalaLibraryFile, scalaExtra, xsbtiArtifactFile, compilerInterfaceSrcFile,
                          null );
        if ( mavenLog.isDebugEnabled() )
        {
            Setup.debug( setup, sbtLogger );
        }
        Compiler compiler = Compiler.create( setup, sbtLogger );

        Inputs inputs =
            Inputs.create( classpath, sources, outputDirectory, scalacOptions, javacOptions, analysisCacheFile,
                           cacheMap, COMPILE_ORDER );
        if ( mavenLog.isDebugEnabled() )
        {
            Inputs.debug( inputs, sbtLogger );
        }

        try
        {
            Analysis analysis = compiler.compile( inputs, sbtLogger );
            return new CompileResult( analysis );
        }
        catch ( xsbti.CompileFailed e )
        {
            throw new SBTCompilationException( e );
        }
    }

    public static class CompileResult
        implements SBTCompilationResult
    {
        private Analysis analysis;

        public CompileResult( Analysis analysis )
        {
            this.analysis = analysis;
        }

        public Set<File> getProducts( File sourceFile )
        {
            return JavaConversions.setAsJavaSet( analysis.relations().products( sourceFile ) );
        }

    }

}
