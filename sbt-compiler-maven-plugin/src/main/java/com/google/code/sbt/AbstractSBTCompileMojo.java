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

package com.google.code.sbt;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Abstract base class for SBT compilation mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractSBTCompileMojo
    extends AbstractMojo
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

    /**
     * SBT version
     * 
     * @since 1.0.0
     */
    @Parameter( property = "sbt.version", defaultValue = "0.13.0" )
    private String sbtVersion;

    /**
     * The -encoding argument for Scala and Java compilers.
     */
    @Parameter( property = "project.build.sourceEncoding" )
    protected String sourceEncoding;

    /**
     * Additional parameters for Java compiler.
     */
    @Parameter( property = "sbt.javacOptions", defaultValue = "-g" )
    protected String javacOptions;

    /**
     * Additional parameters for Scala compiler.
     */
    @Parameter( property = "sbt.scalacOptions", defaultValue = "-deprecation -unchecked" )
    protected String scalacOptions;

    /**
     * <i>Maven Internal</i>: Project to interact with.
     */
    @Component
    protected MavenProject project;

    /**
     * Artifact factory, needed to download source jars.
     */
    @Component
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    protected List<MavenProject> reactorProjects;

    /**
     * Used to look up artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory factory;

    /**
     * Used to resolve artifacts.
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     * Location of the local repository.
     */
    @Parameter( property = "localRepository", readonly = true, required = true )
    protected ArtifactRepository localRepo;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter( property = "project.remoteArtifactRepositories", readonly = true, required = true )
    protected List<?> remoteRepos;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            return;
        }

        try
        {
            long ts = System.currentTimeMillis();
            internalExecute();
            long te = System.currentTimeMillis();
            getLog().debug( String.format( "Mojo execution time: %d ms", te-ts ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

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

        List<File> sourceFiles = getSourceFiles( sourceRootDirs );
        if ( sourceFiles.isEmpty() )
        {
            getLog().info( "No sources to compile" );

            return;
        }

        try
        {
            SBTCompiler compiler = new SBTCompiler();

            Artifact scalaLibraryArtifact =
                getDependencyArtifact( project.getArtifacts(), SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID, "jar" );
            if ( scalaLibraryArtifact == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:jar dependency not found",
                                                                 SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID ) );
            }

            String scalaVersion = scalaLibraryArtifact.getVersion();
            Artifact scalaCompilerArtifact =
                getResolvedArtifact( SCALA_GROUPID, SCALA_COMPILER_ARTIFACTID, scalaVersion );
            if ( scalaCompilerArtifact == null )
            {
                throw new MojoExecutionException(
                                                  String.format( "Required %s:%s:%s:jar dependency not found",
                                                                 SCALA_GROUPID, SCALA_COMPILER_ARTIFACTID, scalaVersion ) );
            }

            List<File> scalaExtraJars = getCompilerDependencies( scalaCompilerArtifact );
            scalaExtraJars.remove( scalaLibraryArtifact.getFile() );

            Artifact xsbtiArtifact = getResolvedArtifact( SBT_GROUP_ID, XSBTI_ARTIFACT_ID, sbtVersion );
            if ( xsbtiArtifact == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:%s:jar dependency not found",
                                                                 SBT_GROUP_ID, XSBTI_ARTIFACT_ID, sbtVersion ) );
            }

            Artifact compilerInterfaceSrc =
                getResolvedArtifact( SBT_GROUP_ID, COMPILER_INTERFACE_ARTIFACT_ID, sbtVersion,
                                     COMPILER_INTERFACE_CLASSIFIER );
            if ( compilerInterfaceSrc == null )
            {
                throw new MojoExecutionException( String.format( "Required %s:%s:%s:%s:jar dependency not found",
                                                                 SBT_GROUP_ID, COMPILER_INTERFACE_ARTIFACT_ID,
                                                                 sbtVersion, COMPILER_INTERFACE_CLASSIFIER ) );
            }

            List<String> classpathElements = getClasspathElements();
            classpathElements.remove( getOutputDirectory().getAbsolutePath() );
            List<File> classpathFiles = new ArrayList<File>( classpathElements.size() );
            for ( String path : classpathElements )
            {
                classpathFiles.add( new File( path ) );
            }

            compiler.setLog( getLog() );
            compiler.setOutputDirectory( getOutputDirectory() );
            compiler.setScalaLibraryFile( scalaLibraryArtifact.getFile() );
            compiler.setScalaCompilerFile( scalaCompilerArtifact.getFile() );
            compiler.setScalaExtraFiles( scalaExtraJars );
            compiler.setXsbtiArtifactFile( xsbtiArtifact.getFile() );
            compiler.setCompilerInterfaceSrcFile( compilerInterfaceSrc.getFile() );
            compiler.setScalacOptions( getScalacOprions() );
            compiler.setJavacOptions( getJavacOprions() );
            compiler.setAnalysisCacheFile( getAnalysisCacheFile() );
            compiler.setAnalysisCacheMap( getAnalysisCacheMap() );
            compiler.setClassPathFiles( classpathFiles );

            /*SBTCompilationResult compileResult = */compiler.compile( sourceFiles );
        }
        /*??? catch ( SBTCompilationException e )
        {
            throw new MojoFailureException( "Scala compilation failed", e );
        }*/
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoFailureException( "Scala compilation failed", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoFailureException( "Scala compilation failed", e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MojoFailureException( "Scala compilation failed", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoFailureException( "Scala compilation failed", e );
        }
    }

    protected abstract List<String> getClasspathElements();

    protected abstract List<String> getCompileSourceRoots();

    protected abstract File getOutputDirectory();

    protected abstract File getAnalysisCacheFile();

    protected abstract Map<File, File> getAnalysisCacheMap();

    private Artifact getDependencyArtifact( Collection<?> classPathArtifacts, String groupId, String artifactId,
                                              String type )
    {
        Artifact result = null;
        for ( Iterator<?> iter = classPathArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() )
                && type.equals( artifact.getType() ) )
            {
                result = artifact;
                break;
            }
        }
        return result;
    }

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
                    File tmpAbsFile = new File( dir, includedFileName ).getAbsoluteFile(); // ?
                    sourceFiles.add( tmpAbsFile );
                }
            }
        }
        // scalac is sensible to scala file order, file system can't guarantee file order => unreproductible build error
        // across platform
        // to guarantee reproductible command line we order file by path (os dependend).
        // Collections.sort( sourceFiles );
        return sourceFiles;
    }

    private List<String> getScalacOprions()
    {
        List<String> result = new ArrayList<String>( Arrays.asList( scalacOptions.split( " " ) ) );
        if ( !result.contains( "-encoding" ) )
        {
            if ( sourceEncoding != null && sourceEncoding.length() > 0 )
            {
                result.add( "-encoding" );
                result.add( sourceEncoding );
            }
        }
        return result;
    }

    private List<String> getJavacOprions()
    {
        List<String> result = new ArrayList<String>( Arrays.asList( javacOptions.split( " " ) ) );
        if ( !result.contains( "-encoding" ) )
        {
            result.add( "-encoding" );
            result.add( sourceEncoding );
        }
        return result;
    }

    /*private Map<File, File> getAnalysisCacheMap()
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
    }*/

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

    // Private utility methods

    private Artifact getResolvedArtifact( String groupId, String artifactId, String version )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact artifact = factory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar" );
        resolver.resolve( artifact, remoteRepos, localRepo );
        return artifact;
    }

    private Artifact getResolvedArtifact( String groupId, String artifactId, String version, String classifier )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact artifact = factory.createArtifactWithClassifier( groupId, artifactId, version, "jar", classifier );
        resolver.resolve( artifact, remoteRepos, localRepo );
        return artifact;
    }

    private List<File> getCompilerDependencies( Artifact scalaCompilerArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException, InvalidDependencyVersionException,
        ProjectBuildingException
    {
        List<File> d = new ArrayList<File>();
        for ( Artifact artifact : getAllDependencies( scalaCompilerArtifact ) )
        {
            d.add( artifact.getFile() );
        }
        return d;
    }

    private Set<Artifact> getAllDependencies( Artifact artifact )
        throws ArtifactNotFoundException, ArtifactResolutionException, InvalidDependencyVersionException,
        ProjectBuildingException
    {
        Set<Artifact> result = new HashSet<Artifact>();
        MavenProject p = mavenProjectBuilder.buildFromRepository( artifact, remoteRepos, localRepo );
        Set<Artifact> d = resolveDependencyArtifacts( p );
        result.addAll( d );
        for ( Artifact dependency : d )
        {
            Set<Artifact> transitive = getAllDependencies( dependency );
            result.addAll( transitive );
        }
        return result;
    }

    /**
     * This method resolves the dependency artifacts from the project.
     * 
     * @param theProject The POM.
     * @return resolved set of dependency artifacts.
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */
    private Set<Artifact> resolveDependencyArtifacts( MavenProject theProject )
        throws ArtifactNotFoundException, ArtifactResolutionException, InvalidDependencyVersionException
    {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add( new ScopeArtifactFilter( Artifact.SCOPE_TEST ) );
        filter.add( new ArtifactFilter()
        {
            public boolean include( Artifact artifact )
            {
                return !artifact.isOptional();
            }
        } );
        // TODO follow the dependenciesManagement and override rules
        Set<Artifact> artifacts = theProject.createArtifacts( factory, Artifact.SCOPE_RUNTIME, filter );
        for ( Artifact artifact : artifacts )
        {
            resolver.resolve( artifact, remoteRepos, localRepo );
        }
        return artifacts;
    }

}
