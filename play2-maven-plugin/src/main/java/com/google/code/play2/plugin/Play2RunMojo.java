/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import com.google.code.play2.provider.api.Play2Builder;
import com.google.code.play2.provider.api.Play2DevServer;
import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2Runner;
import com.google.code.play2.provider.api.Play2RunnerConfiguration;
import com.google.code.play2.provider.api.Play2TemplateCompiler;

import com.google.code.sbt.compiler.api.AnalysisProcessor;

import com.google.code.play2.watcher.api.FileWatchService;
import com.google.code.play2.watcher.api.FileWatchServices;

/**
 * Run Play&#33; server in development mode ({@code sbt run} equivalent).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "run", requiresDependencyCollection = ResolutionScope.RUNTIME )
public class Play2RunMojo
    extends AbstractPlay2EnhanceMojo implements Contextualizable
{
    /**
     * Scala artifacts "groupId".
     */
    private static final String SCALA_GROUPID = "org.scala-lang";

    /**
     * Scala library "artifactId".
     */
    private static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";

    /**
     * Default HTTP port.
     */
    private static final int DEFAULT_HTTP_PORT = 9000;

    /**
     * Default HTTP address (not supported by all providers).
     */
    private static final String DEFAULT_HTTP_ADDRESS = "0.0.0.0";

    /**
     * Source files encoding.
     * <br>
     * <br>
     * If not specified, the encoding value will be the value of the {@code file.encoding} system property.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "project.build.sourceEncoding" )
    protected String sourceEncoding;

    /**
     * Allows the run to be skipped.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.runSkip", defaultValue = "false" )
    private boolean runSkip;

    /**
     * Directory containing all processed web assets.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.assetsOutputDirectory", defaultValue = "${project.build.outputDirectory}/public" ) //TODO  required or not?
    private File assetsOutputDirectory;

    /**
     * Web asset URLs prefix.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.assetsPrefix", defaultValue = "public/" ) //TODO  required or not?
    private String assetsPrefix;

    /**
     * Maven goals to execute during project rebuild.
     * <br>
     * <br>
     * In multi-module projects they are executed for all modules being rebuilt.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.runGoals", defaultValue = "process-classes", required = true )
    private String runGoals;

    /**
     * Additional Maven goals to execute during project rebuild.
     * <br>
     * <br>
     * In multi-module projects they are executed only for the main module.
     * <br>
     * <br>
     * It's required when calling SbtWeb plugin (via <a href="https://github.com/sbtrun-maven-plugin/sbtrun-maven-plugin">sbtrun-maven-plugin</a>)
     * in multi-module project.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.runAdditionalGoals", defaultValue = "" )
    private String runAdditionalGoals;

    /**
     * Additional JVM arguments passed to Play! server's JVM.
     * <br>
     * <br>
     * Because this goal does not fork JVM, only system properties are used, other arguments are ignored.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.serverJvmArgs", defaultValue = "" )
    private String serverJvmArgs;

    /**
     * Server port (HTTP protocol) or {@code disabled} to disable HTTP protocol.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.httpPort", defaultValue = "" )
    private String httpPort;

    /**
     * Server port for secure connection (HTTPS protocol).
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.httpsPort", defaultValue = "" )
    private String httpsPort;

    /**
     * Server address.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.httpAddress", defaultValue = "" )
    private String httpAddress;

    /**
     * Extra settings used only in development mode
     * (see <a href="https://www.playframework.com/documentation/2.5.x/ConfigFile#Extra-devSettings">Play! Framework documentation</a>)
     * <br>
     * <br>
     * Space-separated list of key=value pairs, e.g.
     * <br>
     * {@code play.server.http.port=9001 play.server.https.port=9443}
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.devSettings", defaultValue = "" )
    private String devSettings;

    /**
     * Identifier of the module to run.
     * <br>
     * <br>
     * Important in multi-module projects with more than one {@code play2} modules
     * to select which one should be run.
     * <br>
     * There are three supported formats:
     * <ul>
     * <li>
     * {@code artifactId} or {@code :artifactId} - find first module with given {@code artifactId}
     * </li>
     * <li>
     * {@code groupId:artifactId} - find module with given {@code groupId} and {@code artifactId}
     * </li>
     * </ul>
     * If not specified, first reactor module with {@code play2} packaging will be selected.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.mainModule", defaultValue = "" )
    private String mainModule;

    /**
     * Watch service used to watch for file changes.
     * <br>
     * <br>
     * Supported watch services:
     * <ul>
     * <li>
     * {@code jdk7}
     * </li>
     * <li>
     * {@code jnotify}
     * </li>
     * <li>
     * {@code polling}
     * </li>
     * </ul>
     * <br>
     * Default watch service is selected based on operating system and JDK version.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.fileWatchService", defaultValue = "" )
    private String fileWatchService;

    /**
     * The component used to execute the second Maven execution.
     */
    @Component
    private LifecycleExecutor lifecycleExecutor;

    /**
     * The plexus container.
     */
    private PlexusContainer container;

    /**
     * Map of file watch service implementations. For now only zero or one allowed.
     */
    @Component( role = FileWatchService.class )
    private Map<String, FileWatchService> watchServices;

    /**
     * Retrieves the Plexus container.
     * @param context the context
     * @throws ContextException if the container cannot be retrieved.
     */
    @Override
    public void contextualize( Context context ) throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( runSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        if ( mainModule != null && !"".equals( mainModule ) && !isMatchingProject( project, mainModule ) )
        {
            getLog().debug( "Not main module - skipping execution" );
            return;
        }

        File baseDir = project.getBasedir();

        // Make separate method for checking conf file (use in "run" and "start" mojos)
        /*???File confDir = new File( baseDir, "conf" );
        if ( !confDir.isDirectory() )
        {
            getLog().debug( "No \"conf\" directory - skipping execution" );
            return;
        }
        if ( !new File( confDir, "application.conf" ).isFile() && !new File( confDir, "application.json" ).isFile() )
        {
            getLog().debug( "No \"conf/application.conf\" or \"conf/application.json\" file - skipping execution" );
            return;
        }*/

        Play2Provider play2Provider = getProvider();
        Play2Runner play2Runner = play2Provider.getRunner();

        if ( !play2Runner.supportsRunInDevMode() )
        {
            getLog().warn( "Running in development mode not supported for this Play! Framework version" );
            return;
        }

        List<String> goals = Arrays.asList( runGoals.trim().split( " " ) );
        List<String> additionalGoals =
            runAdditionalGoals != null && !"".equals( runAdditionalGoals ) ? Arrays.asList( runAdditionalGoals.trim().split( " " ) )
                            : Collections.<String>emptyList();

        if ( !assetsPrefix.endsWith( "/" ) )
        {
            assetsPrefix = assetsPrefix + "/";
        }

        getLog().debug( "Required reactor modules:" );
        List<MavenProject> upstreamProjects = session.getProjectDependencyGraph().getUpstreamProjects( project, true );
        List<MavenProject> allRequiredReactorModules = new ArrayList<MavenProject>( 1 + upstreamProjects.size() );
        for ( MavenProject p: upstreamProjects )
        {
            allRequiredReactorModules.add( p );
            getLog().debug( "- " + p.getGroupId() + ":" + p.getArtifactId() );
        }
        allRequiredReactorModules.add( project );

        String scalaVersion = null;
        Set<Artifact> projectArtifacts = project.getArtifacts();
        List<File> dependencyClasspath = new ArrayList<File>( projectArtifacts.size() );
        for ( Artifact a: projectArtifacts )
        {
            if ( !isReactorProject( upstreamProjects, a ) )
            {
                if ( !a.isResolved() )
                {
                    try
                    {
                        getResolvedArtifact( a, false );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        throw new MojoExecutionException( "Artifact resolution failed", e );
                    }
                }
                dependencyClasspath.add( a.getFile() );
                if ( SCALA_GROUPID.equals( a.getGroupId() ) && SCALA_LIBRARY_ARTIFACTID.equals( a.getArtifactId() ) )
                {
                    scalaVersion = a.getVersion();
                }
            }
        }

        List<File> outputDirectories = new ArrayList<File>( allRequiredReactorModules.size() );
        for ( MavenProject p: allRequiredReactorModules )
        {
            outputDirectories.add( new File( p.getBuild().getOutputDirectory() ) );
        }

        File templateCompilationOutputDirectory = getTemplateCompilationOutputDirectory();
        
        AnalysisProcessor sbtAnalysisProcessor = getSbtAnalysisProcessor();

        Properties origProperties = System.getProperties();
        Properties newProperties = new Properties( project.getProperties() );
        newProperties.putAll( origProperties );
        if ( serverJvmArgs != null )
        {
            String trimmedServerJvmArgs = serverJvmArgs.trim();
            if ( trimmedServerJvmArgs.length() > 0 )
            {
                String[] args = trimmedServerJvmArgs.split( " " );
                for ( String arg : args )
                {
                    if ( arg.startsWith( "-D" ) )
                    {
                        getLog().debug( "  Setting system property '" + arg + "'" );
                        String[] prop = arg.substring( 2/*"-D".length()*/ ).split( "=", 2 );
                        newProperties.setProperty( prop[0], prop[1] );
                    }
                }
            }
        }
        newProperties.setProperty( "project.build.directory", project.getBuild().getDirectory() );

        Map<String, String> devSettingsMap = new HashMap<String, String>();
        if ( devSettings != null )
        {
            String trimmedDevSettings = devSettings.trim();
            if ( trimmedDevSettings.length() > 0 )
            {
                String[] args = trimmedDevSettings.split( " " );
                for ( String arg : args )
                {
                    String[] prop = arg.split( "=", 2 );
                    devSettingsMap.put( prop[0], prop[1] );
                }
            }
        }

        String httpPortString =
            httpPort != null && !"".equals( httpPort ) ? httpPort
                            : System.getProperty( "http.port", devSettingsMap.get( "play.server.http.port" ) );
        Integer resolvedHttpPort = parsePortValue( httpPortString, Integer.valueOf( DEFAULT_HTTP_PORT ) );

        String httpsPortString =
            httpsPort != null && !"".equals( httpsPort ) ? httpsPort
                            : System.getProperty( "https.port", devSettingsMap.get( "play.server.https.port" ) );
        Integer resolvedHttpsPort = parsePortValue( httpsPortString, null );

        if ( resolvedHttpPort == null && resolvedHttpsPort == null )
        {
            throw new MojoExecutionException( "HTTPS port must be specified when HTTP port is disabled" );
        }

        String httpAddressString =
            httpAddress != null && !"".equals( httpAddress ) ? httpAddress
                            : System.getProperty( "http.address", devSettingsMap.get( "play.server.http.address" ) );
        String resolvedHttpAddress = httpAddressString != null ? httpAddressString : DEFAULT_HTTP_ADDRESS;

        System.setProperties( newProperties );
        try
        {
            List<File> playDocsClasspath = new ArrayList<File>();
            File playDocsFile = null;
            if ( scalaVersion != null && !"".equals( scalaVersion ) )
            {
                String[] scalaVersionParts = scalaVersion.split( "\\." );
                String scalaBinaryVersion = scalaVersionParts[0] + "." + scalaVersionParts[1];
                String playDocsArtifactId = play2Runner.getPlayDocsModuleId( scalaBinaryVersion, playVersion );
                if ( playDocsArtifactId != null )
                {
                    String[] gav = playDocsArtifactId.split( ":" );
                    Set<Artifact> playDocsArtifacts =
                        getResolvedArtifact( gav[0], gav[1], gav[2] );
                    getLog().debug( "playDocsClasspath:" );
                    for ( Artifact dependencyArtifact : playDocsArtifacts )
                    {
                        File dependencyArtifactFile = dependencyArtifact.getFile();
                        if ( dependencyArtifact.getGroupId().equals( gav[0] )
                            && dependencyArtifact.getArtifactId().equals( gav[1] ) )
                        {
                            playDocsFile = dependencyArtifactFile;
                        }
                        playDocsClasspath.add( dependencyArtifactFile );
                        getLog().debug( "- " + dependencyArtifactFile.getAbsolutePath() );
                    }
                }
            }

            FileWatchService playWatchService = null;
            try
            {
                playWatchService = getWatchService();
                playWatchService.initialize( new MavenFileWatchLogger( getLog() ) );
            }
            catch ( Exception e )
            {
                getLog().warn( "File watch service initialization failed. Running without hot-reload functionality.", e );
                playWatchService = null;
            }

            Play2Builder buildLink =
                new MavenPlay2Builder( allRequiredReactorModules, sourceEncoding, goals, additionalGoals, assetsPrefix,
                                       getLog(), session, lifecycleExecutor, container,
                                       templateCompilationOutputDirectory, sbtAnalysisProcessor, playWatchService );

            Play2RunnerConfiguration configuration = new Play2RunnerConfiguration();
            configuration.setBaseDirectory( baseDir );
            configuration.setOutputDirectories( outputDirectories );
            configuration.setDependencyClasspath( dependencyClasspath );
            configuration.setDocsFile( playDocsFile );
            configuration.setDocsClasspath( playDocsClasspath );
            configuration.setHttpPort( resolvedHttpPort );
            configuration.setHttpsPort( resolvedHttpsPort );
            configuration.setHttpAddress( resolvedHttpAddress );
            configuration.setAssetsPrefix( assetsPrefix );
            configuration.setAssetsDirectory( assetsOutputDirectory );
            configuration.setDevSettings( devSettingsMap );
            configuration.setBuildLink( buildLink );

            try
            {
                System.out.println(); // for nicer console output

                Play2DevServer devModeServer = play2Runner.runInDevMode( configuration );
                try
                {
                    getLog().info( "" );
                    getLog().info( "(Server started, use [Enter] to stop...)" );
                    getLog().info( "" );
                    System.in.read(); // buffered read, waits for Enter
                }
                finally
                {
                    devModeServer.close();
                }
            }
            finally
            {
                buildLink.close();
            }
        }
        catch ( Throwable e )
        {
            throw new MojoExecutionException( "?", e );
        }
        finally
        {
            System.setProperties( origProperties );
        }
    }

    private File getTemplateCompilationOutputDirectory()
        throws MojoExecutionException
    {
        Play2Provider play2Provider = getProvider();
        Play2TemplateCompiler compiler = play2Provider.getTemplatesCompiler();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        String outputDirectoryName = compiler.getCustomOutputDirectoryName();
        if ( outputDirectoryName == null )
        {
            outputDirectoryName = AbstractPlay2SourceGeneratorMojo.DEFAULT_TARGET_DIRECTORY_NAME;
        }
        File generatedDirectory = new File( targetDirectory, outputDirectoryName + "/main" );
        return generatedDirectory;
    }

    private Integer parsePortValue( String portValue, Integer defaultValue )
    {
        Integer result = defaultValue;
        if ( portValue != null )
        {
            if ( "disabled".equals( portValue ) )
            {
                result = null;
            }
            else
            {
                try
                {
                    result = Integer.valueOf( portValue );
                }
                catch ( NumberFormatException e )
                {
                    throw new RuntimeException( "Invalid port argument: " + portValue, e );
                }
            }
        }
        return result;
    }

    private FileWatchService getWatchService()
        throws MojoExecutionException
    {
        FileWatchService watchService = null;
        if ( !watchServices.isEmpty() )
        {
            watchService = getDeclaredWatchService();
        }
        else
        {
            watchService = getWellKnownWatchService();
        }
        return watchService;
    }

    private FileWatchService getDeclaredWatchService()
        throws MojoExecutionException
    {
        if ( watchServices.size() > 1 )
        {
            throw new MojoExecutionException( "Too many file watch services defined. A maximum of one allowed." );
        }

        Map.Entry<String, FileWatchService> watchServiceEntry = watchServices.entrySet().iterator().next();
        String watchServiceId = watchServiceEntry.getKey();
        FileWatchService watchService = watchServiceEntry.getValue();

        getLog().debug( String.format( "Using declared file watch service \"%s\".", watchServiceId ) );

        return watchService;
    }

    private FileWatchService getWellKnownWatchService()
        throws MojoExecutionException
    {
        try
        {
            String watchServiceId =
                fileWatchService != null ? fileWatchService : FileWatchServices.getDefaultWatchServiceId();

            Set<Artifact> watcherArtifacts =
                getResolvedArtifact( pluginGroupId, "play2-source-watcher-" + watchServiceId, pluginVersion );

            List<URL> classPathUrls = new ArrayList<URL>( watcherArtifacts.size() );
            for ( Artifact dependencyArtifact : watcherArtifacts )
            {
                classPathUrls.add( new URL( dependencyArtifact.getFile().toURI().toASCIIString() ) );
            }

            ClassLoader watchServiceClassLoader =
                new URLClassLoader( classPathUrls.toArray( new URL[classPathUrls.size()] ),
                                    Thread.currentThread().getContextClassLoader() );

            ServiceLoader<FileWatchService> watchServiceLoader =
                ServiceLoader.load( FileWatchService.class, watchServiceClassLoader );
            // get first (there should be exactly one)
            FileWatchService watchService = watchServiceLoader.iterator().next();

            getLog().debug( String.format( "Using autodetected file watch service \"%s\".", watchServiceId ) );

            return watchService;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Provider autodetection failed", e );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Provider autodetection failed", e );
        }
    }

    private boolean isReactorProject( List<MavenProject> upstreamProjects, Artifact a )
    {
        boolean result = false;
        for ( MavenProject rp: upstreamProjects )
        {
            if ( rp.getGroupId().equals( a.getGroupId() ) && rp.getArtifactId().equals( a.getArtifactId() )
                && rp.getVersion().equals( a.getVersion() ) )
            {
                result = true;
                break;
            }
        }
        return result;
    }

}
