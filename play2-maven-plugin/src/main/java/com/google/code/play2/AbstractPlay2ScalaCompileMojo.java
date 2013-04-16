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

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.typesafe.zinc.Compiler;
import com.typesafe.zinc.Inputs;
import com.typesafe.zinc.Setup;

import sbt.inc.Analysis;

/**
 * Abstract base class for Play! Mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2ScalaCompileMojo
    extends AbstractDependencyProcessingPlay2Mojo
{
    public static final String SCALA_GROUPID = "org.scala-lang";

    public static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";

    public static final String SCALA_COMPILER_ARTIFACTID = "scala-compiler";

    public static final String SCALA_REFLECT_ARTIFACTID = "scala-reflect";

    public static final String SBT_GROUP_ID = "com.typesafe.sbt";

    public static final String COMPILER_INTEGRATION_ARTIFACT_ID = "incremental-compiler";

    public static final String COMPILER_INTERFACE_ARTIFACT_ID = "compiler-interface";

    public static final String COMPILER_INTERFACE_CLASSIFIER = "sources";

    public static final String XSBTI_ARTIFACT_ID = "sbt-interface";

    public static final String COMPILE_ORDER = "mixed";

    /**
     * Contains the full list of projects in the reactor.
     * 
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    protected List<MavenProject> reactorProjects;

    /**
     * List of artifacts this plugin depends on.
     * 
     */
    @Parameter( property = "plugin.artifacts", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        List<String> compileSourceRoots = getCompileSourceRoots();

        if ( compileSourceRoots.isEmpty() )// ?
        {
            getLog().info( "No sources to compile" );

            return;
        }

        List<File> sourceRootDirs = new ArrayList<File>( compileSourceRoots.size() );
        for ( String compileSourceRoot : compileSourceRoots )
        {
            sourceRootDirs.add( new File( compileSourceRoot ) );
        }

        List<File> sources = getSourceFiles( sourceRootDirs );
        if ( sources.isEmpty() )
        {
            getLog().info( "No sources to compile" );

            return;
        }

        try
        {
            Artifact scalaLibraryArtifact =
                getDependencyArtifact( /*pluginArtifacts*/project.getArtifacts(), SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID, "jar" );
            if ( scalaLibraryArtifact == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:jar dependency not found", SCALA_GROUPID,
                                                                 SCALA_LIBRARY_ARTIFACTID ) );
            }
            
            Artifact scalaCompilerArtifact =
                getDependencyArtifact( /* pluginArtifacts */project.getArtifacts(), SCALA_GROUPID,
                                       SCALA_COMPILER_ARTIFACTID, "jar" );
            if ( scalaCompilerArtifact == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:jar dependency not found",
                                                                 SCALA_GROUPID, SCALA_COMPILER_ARTIFACTID ) );
            }

            Artifact scalaReflectArtifact =
                getDependencyArtifact( /* pluginArtifacts */project.getArtifacts(), SCALA_GROUPID,
                                       SCALA_REFLECT_ARTIFACTID, "jar" );
            if ( scalaReflectArtifact == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:jar dependency not found",
                                                                 SCALA_GROUPID, SCALA_REFLECT_ARTIFACTID ) );
            }

            List<File> scalaExtra = new ArrayList<File>();
            scalaExtra.add( scalaReflectArtifact.getFile() );

            Artifact xsbtiArtifact = getDependencyArtifact( pluginArtifacts, SBT_GROUP_ID, XSBTI_ARTIFACT_ID, "jar" );
            Artifact compilerInterfaceSrc =
                getDependencyArtifact( pluginArtifacts, SBT_GROUP_ID, COMPILER_INTERFACE_ARTIFACT_ID, "jar",
                                       COMPILER_INTERFACE_CLASSIFIER );

            SbtLogger sbtLogger = new SbtLogger( getLog() );
            Setup setup =
                Setup.create( scalaCompilerArtifact.getFile(), scalaLibraryArtifact.getFile(), scalaExtra,
                              xsbtiArtifact.getFile(), compilerInterfaceSrc.getFile(), null );
            if ( getLog().isDebugEnabled() )
            {
                Setup.debug( setup, sbtLogger );
            }
            Compiler compiler = Compiler.create( setup, sbtLogger );

            List<String> classpathElements = getClasspathElements();
            classpathElements.remove( getOutputDirectory().getAbsolutePath() );
            List<String> scalacOptions = Collections.emptyList();
            List<String> javacOptions = Collections.emptyList();
            Map<File, File> cacheMap = getAnalysisCacheMap();

            List<File> classpath = new ArrayList<File>( classpathElements.size() );
            for ( String path : classpathElements )
            {
                classpath.add( new File( path ) );
            }

            Inputs inputs =
                Inputs.create( classpath, sources, getOutputDirectory(), scalacOptions, javacOptions,
                               getAnalysisCacheFile(), cacheMap, COMPILE_ORDER );
            if ( getLog().isDebugEnabled() )
            {
                Inputs.debug( inputs, sbtLogger );
            }
            Analysis analysis = compiler.compile( inputs, sbtLogger );
            postCompile(classpath/*, sources*/, analysis);
            //System.out.println(analysis.relations());
            //System.out.println(analysis.stamps());
        }
        catch ( xsbti.CompileFailed e )
        {
            throw new MojoFailureException( "?", e );
        }
        /*catch ( DependencyTreeBuilderException e )
        {
            throw new MojoFailureException( "?", e );
        }*/
    }

    protected abstract List<String> getClasspathElements();

    protected abstract List<String> getCompileSourceRoots();

    protected abstract File getOutputDirectory();

    protected abstract File getAnalysisCacheFile();

    private List<File> getSourceFiles( List<File> sourceRootDirs )
    {
        List<File> sourceFiles = new ArrayList<File>();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes( new String[] { "**/*.java", "**/*.scala" } );
        scanner.addDefaultExcludes();

        for ( File dir : sourceRootDirs )
        {
            if ( dir.isDirectory() )
            {
                scanner.setBasedir( dir );
                scanner.scan();
                String[] includedFileNames = scanner.getIncludedFiles();
                for ( String includedFileName : includedFileNames )
                {
                    File tmpAbsFile = new File( dir, includedFileName ).getAbsoluteFile();// ?
                    sourceFiles.add( tmpAbsFile );
                }
            }
        }
        // scalac is sensible to scala file order, file system can't garanty file order => unreproductible build error
        // across platform
        // to garanty reproductible command line we order file by path (os dependend).
        // Collections.sort( sourceFiles );
        return sourceFiles;
    }

    private Map<File, File> getAnalysisCacheMap()
    {
        HashMap<File, File> map = new HashMap<File, File>();
        for ( MavenProject project : reactorProjects )
        {
            File analysisCacheFile = defaultAnalysisCacheFile( project );
            File classesDirectory = new File( project.getBuild().getOutputDirectory() );
            map.put( classesDirectory.getAbsoluteFile(), analysisCacheFile.getAbsoluteFile() );
            File testAnalysisCacheFile = defaultTestAnalysisCacheFile( project );
            File testClassesDirectory = new File( project.getBuild().getTestOutputDirectory() );
            map.put( testClassesDirectory.getAbsoluteFile(), testAnalysisCacheFile.getAbsoluteFile() );
        }
        return map;
    }

    private File defaultAnalysisDirectory( MavenProject p )
    {
        return new File( p.getBuild().getDirectory(), "analysis" );
    }

    protected File defaultAnalysisCacheFile( MavenProject p )
    {
        return new File( defaultAnalysisDirectory( p ), "compile" );
    }

    protected File defaultTestAnalysisCacheFile( MavenProject p )
    {
        return new File( defaultAnalysisDirectory( p ), "test-compile" );
    }

    protected void postCompile(List<File> classpathFiles, Analysis analysis) throws MojoExecutionException, IOException {
    }
    
}
