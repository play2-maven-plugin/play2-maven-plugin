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

import java.io.File;
//?import java.io.IOException;
import java.util.Collections;
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

public class Play22SBTCompiler
    implements Play2SBTCompiler
{
    private static final String COMPILE_ORDER = "mixed"; // Tutaj???

    private Log mavenLog;
    
    private File scalaLibraryFile;

    private File scalaCompilerFile;

    private List<File> scalaExtraFiles;

    private File xsbtiArtifactFile;

    private File compilerInterfaceSrcFile;

    private List<File> classpathFiles;

    private File outputDirectory;

    private List<String> scalacOptions = Collections.emptyList();

    private List<String> javacOptions = Collections.emptyList();

    private File analysisCacheFile;

    private Map<File, File> analysisCacheMap = Collections.emptyMap();

    public String getDefaultScalaVersion()
    {
        return "2.10.2";
    }

    public String getDefaultSbtVersion()
    {
        return "0.13.0";
    }

    public void setLog( Log mavenLog )
    {
        this.mavenLog = mavenLog;
    }

    public void setScalaLibraryFile( File scalaLibraryFile)
    {
        this.scalaLibraryFile = scalaLibraryFile;
    }

    public void setScalaCompilerFile( File scalaCompilerFile)
    {
        this.scalaCompilerFile = scalaCompilerFile;
    }

    public void setScalaExtraFiles( List<File> scalaExtraFiles )
    {
        this.scalaExtraFiles = scalaExtraFiles;
    }

    public void setXsbtiArtifactFile( File xsbtiArtifactFile )
    {
        this.xsbtiArtifactFile = xsbtiArtifactFile;
    }

    public void setCompilerInterfaceSrcFile( File compilerInterfaceSrcFile )
    {
        this.compilerInterfaceSrcFile = compilerInterfaceSrcFile;
    }

    public void setClassPathFiles( List<File> classpathFiles )
    {
        this.classpathFiles = classpathFiles;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setScalacOptions( List<String> scalacOptions )
    {
        this.scalacOptions = scalacOptions;
    }

    public void setJavacOptions( List<String> javacOptions )
    {
        this.javacOptions = javacOptions;
    }

    public void setAnalysisCacheFile( File analysisCacheFile )
    {
        this.analysisCacheFile = analysisCacheFile;
    }

    public void setAnalysisCacheMap( Map<File, File> analysisCacheMap )
    {
        this.analysisCacheMap = analysisCacheMap;
    }

    public SBTCompilationResult compile( List<File> sourceFiles )
        throws SBTCompilationException
    {
        SbtLogger sbtLogger = new SbtLogger( mavenLog );
        Setup setup =
            Setup.create( scalaCompilerFile, scalaLibraryFile, scalaExtraFiles, xsbtiArtifactFile, compilerInterfaceSrcFile,
                          null, false/* forkJava */ );
        if ( mavenLog.isDebugEnabled() )
        {
            Setup.debug( setup, sbtLogger );
        }
        Compiler compiler = Compiler.create( setup, sbtLogger );

        scala.Option<File> none = scala.Option.empty();
        IncOptions incOptions = new IncOptions( 3, 0.5d, false, false, 5, none, false, none );
        Inputs inputs =
            Inputs.create( classpathFiles, sourceFiles, outputDirectory, scalacOptions, javacOptions, analysisCacheFile,
                           analysisCacheMap, COMPILE_ORDER, incOptions, false/* mirrorAnalysisCache */ );
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
