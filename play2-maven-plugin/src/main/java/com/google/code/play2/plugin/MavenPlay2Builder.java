/*
 * Copyright 2013-2020 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.DirectoryScanner;

import com.google.code.play2.provider.api.AssetCompilationException;
import com.google.code.play2.provider.api.Play2BuildError;
import com.google.code.play2.provider.api.Play2BuildFailure;
import com.google.code.play2.provider.api.Play2Builder;
import com.google.code.play2.provider.api.Play2BuildException;
import com.google.code.play2.provider.api.RoutesCompilationException;
import com.google.code.play2.provider.api.TemplateCompilationException;

import com.google.code.play2.spm.template.Play2TemplateGeneratedSource;
import com.google.code.play2.spm.template.Play2TemplateSourcePositionMapper;

import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchException;
import com.google.code.play2.watcher.api.FileWatchService;
import com.google.code.play2.watcher.api.FileWatcher;

import com.google.code.sbt.compiler.api.Analysis;
import com.google.code.sbt.compiler.api.AnalysisProcessor;
import com.google.code.sbt.compiler.api.CompilerException;
import com.google.code.sbt.compiler.api.Compilers;

public class MavenPlay2Builder implements Play2Builder, FileWatchCallback
{
    private List<MavenProject> projects;

    private String sourceEncoding;

    private List<String> goals;

    private List<String> additionalGoals;

    private String assetsPrefix;

    private Log logger;

    private MavenSession session;

    private LifecycleExecutor lifecycleExecutor;

    private PlexusContainer container;

    private File templateCompilationOutputDirectory;

    private AnalysisProcessor sbtAnalysisProcessor;

    private FileWatchService playWatchService;

    private FileWatcher watcher = null; // created after first successful build

    // Flag to force a reload on the next request.
    // This is set if a compile error occurs, and also by the forceReload method on BuildLink, which is called for
    // example when evolutions have been applied.
//    @volatile private var forceReloadNextTime = false
    private boolean forceReloadNextTime = true;
    // Whether any source files have changed since the last request.
//    @volatile private var changed = false
//    private volatile boolean changed = false;
    private /*?volatile*/ Map<String, Long> changedFiles = new HashMap<String, Long>(); //TODO - moze od razu moduły, a nie pliki
    private Object changedFilesLock = new Object();

    private boolean afterFirstSuccessfulBuild = false;
    private Map<MavenProject, Map<String, File>> currentSourceMaps;
    private Map<MavenProject, Long> currentClasspathTimestamps;
    private Map<MavenProject, Set<String>> currentClasspathFilePaths;

    public MavenPlay2Builder( List<MavenProject> projects, String sourceEncoding, List<String> goals,
                              List<String> additionalGoals, String assetsPrefix, Log logger, MavenSession session,
                              LifecycleExecutor lifecycleExecutor, PlexusContainer container,
                              File templateCompilationOutputDirectory, AnalysisProcessor sbtAnalysisProcessor,
                              FileWatchService playWatchService )
    {
        this.projects = projects;
        this.sourceEncoding = sourceEncoding;
        this.goals = goals;
        this.additionalGoals = additionalGoals;
        this.assetsPrefix = assetsPrefix;
        this.logger = logger;
        this.session = session;
        this.lifecycleExecutor = lifecycleExecutor;
        this.container = container;
        this.templateCompilationOutputDirectory = templateCompilationOutputDirectory;
        this.sbtAnalysisProcessor = sbtAnalysisProcessor;
        this.playWatchService = playWatchService;

        currentSourceMaps = new HashMap<MavenProject, Map<String, File>>( projects.size() );
        currentClasspathTimestamps = new HashMap<MavenProject, Long>( projects.size() );
        currentClasspathFilePaths = new HashMap<MavenProject, Set<String>>( projects.size() );
        for ( MavenProject p: projects )
        {
            currentClasspathTimestamps.put( p, Long.valueOf( 0 ) );
            currentClasspathFilePaths.put( p, Collections.<String>emptySet() );
        }
    }

    @Override /* FileWatchCallback */
    public void close() throws IOException
    {
        if ( watcher != null )
        {
            watcher.close();
        }
    }

    @Override /* FileWatchCallback */
    public void onChange( File changedFile )
    {
        String path = changedFile.getAbsolutePath();
        Long currentTimestamp = Long.valueOf( changedFile.lastModified() );
        synchronized ( changedFilesLock )
        {
            Long prevTimestamp = changedFiles.get( path );
            if ( prevTimestamp == null || !prevTimestamp.equals( currentTimestamp ) )
            {
                logger.debug( "\"" + path + "\" file changed" );
                changedFiles.put( path, currentTimestamp );
            }
        }
    }

    @Override /* Play2Builder */
    public void forceReload()
    {
        forceReloadNextTime = true;
    }

    @Override /* Play2Builder */
    public Object[] findSource( String className, Integer line )
    {
        Object[] result = null;
        String topType = className.split( "\\$" )[0];
        for ( Map<String, File> sourceMap: currentSourceMaps.values() )
        {
            File sourceFile = sourceMap.get( topType );
            if ( sourceFile != null )
            {
                result = new Object[] { sourceFile, line };
                if ( sourceFile.getAbsolutePath().startsWith( templateCompilationOutputDirectory.getAbsolutePath() ) )
                {
                    try
                    {
                        Play2TemplateSourcePositionMapper mapper = new Play2TemplateSourcePositionMapper();
                        mapper.setCharsetName( sourceEncoding );
                        Play2TemplateGeneratedSource template = mapper.getGeneratedSource( sourceFile );
                        if ( template != null )
                        {
                            File originalSourceFile = new File( template.getSourceFileName() );
                            Integer originalLine = null;
                            if ( line != null )
                            {
                                originalLine = Integer.valueOf( template.mapLine( line.intValue() ) );
                            }
                            result = new Object[] { originalSourceFile, originalLine };
                        }
                    }
                    catch ( IOException e )
                    {
                        // ignore
                    }
                    catch ( RuntimeException e )
                    {
                        // ignore
                    }
                }
                break;
            }
        }
        return result;
    }

    @Override /* Play2Builder */
    public boolean build() throws Play2BuildFailure, Play2BuildError/*Play2BuildException*/
    {
        Set<String> changedFilePaths = null;
        Map<String, Long> prevChangedFiles = new HashMap<String, Long>();
        synchronized ( changedFilesLock )
        {
            if ( !changedFiles.isEmpty() )
            {
                changedFilePaths = changedFiles.keySet();
                prevChangedFiles = changedFiles;
                changedFiles = new HashMap<String, Long>();
            }
        //TEST - more code inside synchronized block
        }

        if ( !forceReloadNextTime && changedFilePaths == null /*&& afterFirstSuccessfulBuild*/ )
        {
            return false;
        }

        List<MavenProject> projectsToBuild = projects;
        // - !afterFirstSuccessfulBuild => first build or no previous successful builds, build all modules
        // - currentSourceMaps.isEmpty() => first build, build all modules
        // - projects.size() == 1 => one-module project, just build it
        // - else => not the first build in multimodule-project, calculate modules subset to build
        if ( afterFirstSuccessfulBuild /* !currentSourceMaps.isEmpty() *//* currentSourceMap != null */
            && !forceReloadNextTime && projects.size() > 1 )
        {
            projectsToBuild = calculateProjectsToBuild( changedFilePaths );
        }

        MavenExecutionResult result = executeBuild( projectsToBuild, goals );

        boolean shouldReload = forceReloadNextTime;
        forceReloadNextTime = result.hasExceptions();

        if ( !result.hasExceptions() && !additionalGoals.isEmpty() )
        {
            List<MavenProject> onlyMe = Arrays.asList( new MavenProject[] { session.getCurrentProject() } );

            result = executeBuild( onlyMe, additionalGoals );

            forceReloadNextTime = result.hasExceptions();
        }

        if ( result.hasExceptions() )
        {
            synchronized ( changedFilesLock )
            {
                changedFiles.putAll( prevChangedFiles ); // restore previously changed paths, required for next rebuild
            }
            Throwable firstException = result.getExceptions().get( 0 ); // LifecycleExecutionException
            Throwable t = firstException.getCause();
            if ( t instanceof MojoFailureException )
            {
                Throwable cause = t.getCause();
                if ( cause != null )
                {
                    try
                    {
                        Play2BuildException pbe = null;
                        String causeName = cause.getClass().getName();

                        // sbt-compiler exception
                        if ( CompilerException.class.getName().equals( causeName ) )
                        {
                            pbe = getSBTCompilerBuildException( cause );
                        }
                        else if ( AssetCompilationException.class.getName().equals( causeName )
                            || RoutesCompilationException.class.getName().equals( causeName )
                            || TemplateCompilationException.class.getName().equals( causeName ) )
                        {
                            pbe = getPlayBuildException( cause );
                        }

                        if ( pbe != null )
                        {
                            throw new Play2BuildFailure( pbe, sourceEncoding );
                        }
                        throw new Play2BuildError( t.getClass().getSimpleName() + ": " + t.getMessage(), t );
                    }
                    catch ( Play2BuildFailure e )
                    {
                        throw e;
                    }
                    catch ( Play2BuildError e )
                    {
                        throw e;
                    }
                    catch ( Exception e )
                    {
                        throw new Play2BuildError( ".... , check Maven console" );
                    }
                }
                throw new Play2BuildError( t.getClass().getSimpleName() + ": " + t.getMessage(), t );
            }

            if ( t instanceof PluginExecutionException )
            {
                Throwable cause = t.getCause();
                throw new Play2BuildError( cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause );
            }
            throw new Play2BuildError( t.getClass().getSimpleName() + ": " + t.getMessage(), t );
        }

        // no exceptions
        if ( !afterFirstSuccessfulBuild ) // this was first successful build
        {
            afterFirstSuccessfulBuild = true;

            if ( playWatchService != null )
            {
                // Monitor all existing, not generated (inside output directory) source and resource roots
                List<File> monitoredDirectories = new ArrayList<File>();
                for ( MavenProject p: projects )
                {
                    String targetDirectory = p.getBuild().getDirectory();
                    for ( String sourceRoot: p.getCompileSourceRoots() )
                    {
                        if ( !sourceRoot.startsWith( targetDirectory ) && new File( sourceRoot ).isDirectory() )
                        {
                            monitoredDirectories.add( new File( sourceRoot ) );
                        }
                    }
                    for ( Resource resource: p.getResources() )
                    {
                        String resourceRoot = resource.getDirectory();
                        if ( !resourceRoot.startsWith( targetDirectory ) && new File( resourceRoot ).isDirectory() )
                        {
                            monitoredDirectories.add( new File( resourceRoot ) );
                        }
                    }
                }
                //TODO - remove roots nested inside another roots (is it possible?)

                try
                {
                    watcher = playWatchService.watch( monitoredDirectories, this );
                }
                catch ( FileWatchException e )
                {
                    logger.warn( "File watcher initialization failed. Running without hot-reload functionality.", e );
                }
            }
        }

        Map<MavenProject, Map<String, File>> sourceMaps = new HashMap<MavenProject, Map<String, File>>( currentSourceMaps );
        for ( MavenProject p: projectsToBuild )
        {
            Map<String, File> sourceMap = new HashMap<String, File>();
            File classesDirectory = new File( p.getBuild().getOutputDirectory() );
            String classesDirectoryPath = classesDirectory.getAbsolutePath() + File.separator;
            File analysisCacheFile = defaultAnalysisCacheFile( p );
            Analysis analysis = sbtAnalysisProcessor.readFromFile( analysisCacheFile );
            for ( File sourceFile : analysis.getSourceFiles() )
            {
                Set<File> sourceFileProducts = analysis.getProducts( sourceFile );
                for ( File product : sourceFileProducts )
                {
                    String absolutePath = product.getAbsolutePath();
                    if ( absolutePath.contains( "$" ) )
                    {
                        continue; // skip inner and object classes
                    }
                    String relativePath = absolutePath.substring( classesDirectoryPath.length() );
//                    String name = product.getName();
                    String name = relativePath.substring( 0, relativePath.length() - ".class".length() );
                    /*if (name.indexOf( '$' ) > 0)
                    {
                        name = name.substring( 0, name.indexOf( '$' ) );
                    }*/
                    name = name.replace( File.separator, "." );
                    //System.out.println(sourceFile.getPath() + " -> " + name);
                    sourceMap.put( name, sourceFile );
                }
                /*String[] definitionNames = analysis.getDefinitionNames( sourceFile );
                Set<String> uniqueDefinitionNames = new HashSet<String>(definitionNames.length);
                for (String definitionName: definitionNames)
                {
                    if ( !uniqueDefinitionNames.contains( definitionName ) )
                    {
                        result.put( definitionName, sourceFile );
//                        System.out.println( "definitionName:'" + definitionName + "', source:'"
//                                        + sourceFile.getAbsolutePath() + "'" );
                        uniqueDefinitionNames.add( definitionName );
                    }
                }*/
            }
            sourceMaps.put( p, sourceMap );
        }
        this.currentSourceMaps = sourceMaps;

        for ( MavenProject p: projectsToBuild )
        {
            long lastModifiedTime = 0L;
            Set<String> outputFilePaths = new HashSet<String>();
            File outputDirectory = new File( p.getBuild().getOutputDirectory() );
            if ( outputDirectory.exists() && outputDirectory.isDirectory() )
            {
                DirectoryScanner classPathScanner = new DirectoryScanner();
                classPathScanner.setBasedir( outputDirectory );
                classPathScanner.setExcludes( new String[] { assetsPrefix + "**" } );
                classPathScanner.scan();
                String[] files = classPathScanner.getIncludedFiles();
                for ( String fileName: files )
                {
                    File f = new File( outputDirectory, fileName );
                    outputFilePaths.add( f.getAbsolutePath() );
                    long lmf = f.lastModified();
                    if ( lmf > lastModifiedTime )
                    {
                        lastModifiedTime = lmf;
                    }
                }
            }
            if ( !shouldReload
                && ( lastModifiedTime > currentClasspathTimestamps.get( p ).longValue() || !outputFilePaths.equals( currentClasspathFilePaths.get( p ) ) ) )
            {
                shouldReload = true;
            }
            currentClasspathTimestamps.put( p, Long.valueOf( lastModifiedTime ) );
            currentClasspathFilePaths.put( p, outputFilePaths );
        }

        return shouldReload;
    }

    private MavenExecutionResult executeBuild( List<MavenProject> projectsToBuild, List<String> goalsToExecute )
    {
        MavenExecutionRequest request = DefaultMavenExecutionRequest.copy( session.getRequest() );
        request.setStartTime( new Date() );
        request.setExecutionListener( new ExecutionEventLogger() );
        request.setGoals( goalsToExecute );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        final MavenSession newSession = new MavenSession( container, session.getRepositorySession(), request, result );
        newSession.setProjects( projectsToBuild );
        newSession.setCurrentProject( session.getCurrentProject() );
        newSession.setParallel( session.isParallel() );
        newSession.setProjectDependencyGraph( session.getProjectDependencyGraph() );

        Thread mavenBuildThread = new Thread( new MavenPlay2BuilderRunnable( lifecycleExecutor, newSession ) );
        mavenBuildThread.start();
        try
        {
            mavenBuildThread.join();
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException ( e );
        }

        return result;
    }

    private Play2BuildException getPlayBuildException( Throwable playException )
    {
        Play2BuildException result = null;
        try
        {
            String message = playException.getClass().getMethod( "getMessage" ).invoke( playException ).toString();
            Object file = playException.getClass().getMethod( "source" ).invoke( playException );
            String fileAbsolutePath = file.getClass().getMethod( "getAbsolutePath" ).invoke( file ).toString();
            Integer line = ( Integer ) playException.getClass().getMethod( "line" ).invoke( playException );
            Integer position = ( Integer ) playException.getClass().getMethod( "position" ).invoke( playException );
            result = new Play2BuildException( new File( fileAbsolutePath ), message, line.intValue(), position.intValue() );
        }
        catch ( IllegalAccessException e )
        {
            // ignore
        }
        catch ( InvocationTargetException e )
        {
            // ignore
        }
        catch ( NoSuchMethodException e )
        {
            // ignore
        }
        catch ( SecurityException e )
        {
            // ignore
        }
        return result;
    }

    private Play2BuildException getSBTCompilerBuildException( Throwable compilerException )
                    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        boolean hasGetProblemsMethod = false;
        for ( java.lang.reflect.Method m: compilerException.getClass().getMethods() )
        {
            if ( "getProblems".equals( m.getName() ) )
            {
                hasGetProblemsMethod = true;
                break;
            }
        }
        if ( !hasGetProblemsMethod )
        {
            //TODO - add warning about too old sbt-compiler-maven-plugin version
            return null; //TODO - how to display meaningful message on the screen?
        }

        Play2BuildException result = null;
        Object problems = compilerException.getClass().getMethod( "getProblems" ).invoke( compilerException );
        if ( problems.getClass().isArray() )
        {
            int length = Array.getLength( problems );
            for ( int i = 0; i < length; i ++ )
            {
                Object problem = Array.get( problems, i );
                String severity = problem.getClass().getMethod( "getSeverity"  ).invoke( problem ).toString();
                if ( "Error".equals( severity ) )
                {
                    //String category = problem.getClass().getMethod( "getCategory"  ).invoke( problem ).toString();
                    String message = problem.getClass().getMethod( "getMessage"  ).invoke( problem ).toString();
                    Object sourcePosition = problem.getClass().getMethod( "getPosition"  ).invoke( problem );
                    if ( sourcePosition != null )
                    {
                        Integer line = ( Integer ) sourcePosition.getClass().getMethod( "getLine" ).invoke( sourcePosition );
                        //String lineContent = sourcePosition.getClass().getMethod( "getLineContent" ).invoke( sourcePosition ).toString();
                        //Object offset = sourcePosition.getClass().getMethod( "getOffset" ).invoke( sourcePosition );
                        Integer pointer = ( Integer ) sourcePosition.getClass().getMethod( "getPointer" ).invoke( sourcePosition );
                        Object file = sourcePosition.getClass().getMethod( "getFile" ).invoke( sourcePosition ); //FIXME is it nullable?
                        String fileAbsolutePath = file.getClass().getMethod( "getAbsolutePath" ).invoke( file ).toString();
                        result = new Play2BuildException( new File( fileAbsolutePath ), message, line.intValue(), pointer.intValue() );
                    }
                    else
                    {
                        result = new Play2BuildException( null, message, 0, 0 );
                    }
                    break;
                }
            }
        }
        return result;
    }

    // === TEST ===================================================================================

    private File defaultAnalysisCacheFile( MavenProject p )
    {
        File classesDirectory = new File( p.getBuild().getOutputDirectory() );
        return new File( Compilers.getCacheDirectory( classesDirectory ), "compile" );
    }

    // === TEST ===================================================================================

    private List<MavenProject> calculateProjectsToBuild( Set<String> changedFilePaths )
    {
        Set<MavenProject> changedProjects = new HashSet<MavenProject>( projects.size() );
        for ( String path: changedFilePaths )
        {
            MavenProject p = findProjectFor( path );
            changedProjects.add( p );
        }

        Set<MavenProject> changedAndDependentProjects = new HashSet<MavenProject>( projects.size() );
        for ( MavenProject p: changedProjects )
        {
            changedAndDependentProjects.add( p );
            List<MavenProject> deps = session.getProjectDependencyGraph().getDownstreamProjects( p, true/*transitive*/ );
            for ( MavenProject depP: deps )
            {
                if ( projects.contains( depP ) )
                {
                    changedAndDependentProjects.addAll( deps );
                }
            }
        }

        List<MavenProject> result = new ArrayList<MavenProject>( changedAndDependentProjects.size() );
        for ( MavenProject p: projects )
        {
            if ( changedAndDependentProjects.contains( p ) )
            {
                result.add( p );
            }
        }
        return result;
    }

    private MavenProject findProjectFor( String filePath )
    {
        MavenProject result = null;
        search:
        for ( MavenProject p: projects )
        {
            for ( String sourceRoot: p.getCompileSourceRoots() )
            {
                if ( filePath.startsWith( sourceRoot ) )
                {
                    result = p;
                    break search;
                }
            }
            for ( Resource resource: p.getResources() )
            {
                if ( filePath.startsWith( resource.getDirectory() ) )
                {
                    result = p;
                    break search;
                }
            }
        }
        return result;
    }

}
