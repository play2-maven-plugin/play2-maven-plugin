/*
 * Copyright 2013 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play2.provider.play22;

import java.io.File;
//?import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.play2.provider.Play2SBTCompiler;
import com.google.code.play2.provider.SBTCompilationException;
import com.google.code.play2.provider.SBTCompilationResult;

import com.typesafe.zinc.Compiler;
import com.typesafe.zinc.IncOptions;
import com.typesafe.zinc.Inputs;
import com.typesafe.zinc.Setup;

import sbt.inc.Analysis;
import scala.collection.JavaConversions;

import org.apache.maven.plugin.logging.Log;

public class Play22SBTCompiler implements Play2SBTCompiler
{
    private static final String COMPILE_ORDER = "mixed";//Tutaj???

//    private boolean playEnhance = false;
//    
//    public void setPlayEnhance(boolean playEnhance)
//    {
//        this.playEnhance = playEnhance;
//    }
    
    public String getDefaultScalaVersion()
    {
        return "2.10.2";
    }
    
    public String getDefaultSbtVersion()
    {
        return "0.13.0";
    }
    
    public SBTCompilationResult/*Analysis*/ compile( Log mavenLog, File scalaCompilerFile, File scalaLibraryFile, List<File> scalaExtra,
                  File xsbtiArtifactFile, File compilerInterfaceSrcFile, List<File> classpath,
                  List<File> sources, File outputDirectory,
                  List<String> scalacOptions, List<String> javacOptions,
                  File analysisCacheFile, Map<File, File> cacheMap ) throws SBTCompilationException
    {
        SbtLogger sbtLogger = new SbtLogger( mavenLog );
        Setup setup =
            Setup.create( scalaCompilerFile, scalaLibraryFile, scalaExtra, xsbtiArtifactFile, compilerInterfaceSrcFile,
                          null, false/* forkJava */ );
        if ( mavenLog.isDebugEnabled() )
        {
            Setup.debug( setup, sbtLogger );
        }
        Compiler compiler = Compiler.create( setup, sbtLogger );

        scala.Option<File> none = scala.Option.empty(); 
        Inputs inputs =
            Inputs.create( classpath, sources, outputDirectory, scalacOptions, javacOptions, analysisCacheFile,
                           cacheMap, COMPILE_ORDER, new IncOptions(3, 0.5d, false, false, 5, none, false, none) /*incOptions: IncOptions*/, false/*mirrorAnalysisCache*/ );
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
