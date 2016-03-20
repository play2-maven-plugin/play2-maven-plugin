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

package com.google.code.play2.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import com.google.code.sbt.compiler.api.AnalysisProcessor;
import com.google.code.sbt.compiler.api.Compiler; // required by JavaDoc
import com.google.code.sbt.compiler.api.Compilers;

public abstract class AbstractPlay2EnhanceMojo
    extends AbstractPlay2Mojo
{

    /**
     * Forced SBT version.<br>
     * <br>
     * Used to automatically select one of the "well known" SBT compilers if no compiler added explicitly as plugin's dependency.
     * There are three cases possible:
     * <ul>
     * <li>
     * If {@link #sbtVersion} is specified, compatible {@link Compiler} implementation
     * is selected and configured to use {@link #sbtVersion} SBT version for compilation.
     * </li>
     * <li>
     * If {@link #sbtVersion} is not specified, and {@link #playVersion} is specified
     * {@link #playVersion} parameter value is used to indirectly select compatible {@link Compiler} implementation
     * and it's {@link Compiler#getDefaultSbtVersion()} SBT version used for compilation.
     * </li>
     * <li>
     * If both {@link #sbtVersion} and {@link #playVersion} are not specified
     * the most recent {@link Compiler} implementation is selected
     * and it's {@link Compiler#getDefaultSbtVersion()} SBT version used for compilation.
     * </li>
     * </ul>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "sbt.version" )
    protected String sbtVersion;

    /**
     * List of artifacts this plugin depends on.
     */
    @Parameter( property = "plugin.artifacts", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    private static final String sbtCompilerPluginGroupId = "com.google.code.sbt-compiler-maven-plugin";

    private static final String sbtCompilerPluginApiArtifactId = "sbt-compiler-api";

    /**
     * Map of SBT compiler implementations. For now only zero or one allowed.
     */
    @Component( role = AnalysisProcessor.class )
    private Map<String, AnalysisProcessor> analysisProcessors;

    // SBT compiler resolution for Analysis cache processing

    // Cached classloaders
    private static final ConcurrentHashMap<String, ClassLoader> sbtCompilerCachedClassLoaders = new ConcurrentHashMap<String, ClassLoader>( 2 );

    private static ClassLoader getSbtCompilerCachedClassLoader( String compilerId )
    {
        return sbtCompilerCachedClassLoaders.get( compilerId );
    }

    private static void setSbtCompilerCachedClassLoader( String compilerId, ClassLoader classLoader )
    {
        sbtCompilerCachedClassLoaders.put( compilerId, classLoader );
    }

    protected AnalysisProcessor getSbtAnalysisProcessor()
        throws MojoExecutionException
    {
        AnalysisProcessor sbtAnalysisProcessor = null;
        if ( !analysisProcessors.isEmpty() )
        {
            sbtAnalysisProcessor = getDeclaredSbtAnalysisProcessor();
        }
        else
        {
            sbtAnalysisProcessor = getWellKnownSbtAnalysisProcessor();
        }
        return sbtAnalysisProcessor;
    }

    private AnalysisProcessor getDeclaredSbtAnalysisProcessor()
        throws MojoExecutionException
    {
        if ( analysisProcessors.size() > 1 )
        {
            throw new MojoExecutionException( "Too many compiles defined. A maximum of one allowed." );
        }

        Map.Entry<String, AnalysisProcessor> compilerEntry = analysisProcessors.entrySet().iterator().next();
        String compilerId = compilerEntry.getKey();
        AnalysisProcessor sbtAnalysisProcessor = compilerEntry.getValue();

        getLog().debug( String.format( "Using declared compiler \"%s\".", compilerId ) );

        return sbtAnalysisProcessor;
    }

    private AnalysisProcessor getWellKnownSbtAnalysisProcessor()
        throws MojoExecutionException
    {
        try
        {
            String compilerId = Compilers.getDefaultCompilerId( sbtVersion, playVersion );
            ClassLoader compilerClassLoader = getSbtCompilerCachedClassLoader( compilerId );
            if ( compilerClassLoader == null )
            {
                getLog().debug( String.format( "Cached classloader for compiler \"%s\" not available.", compilerId ) );
            }
            else
            {
                if ( compilerClassLoader.getParent() == Thread.currentThread().getContextClassLoader() )
                {
                    getLog().debug( String.format( "Using cached classloader for compiler \"%s\".", compilerId ) );
                }
                else
                {
                    getLog().debug( String.format( "Invalidated cached classloader for compiler \"%s\". Parent classloader changed from %d to %d.",
                                                   compilerId,
                                                   Integer.valueOf( compilerClassLoader.getParent().hashCode() ),
                                                   Integer.valueOf( Thread.currentThread().getContextClassLoader().hashCode() ) ) );
                    compilerClassLoader = null;
                }
            }
            if ( compilerClassLoader == null )
            {
                Artifact compilerArtifact =
                    getResolvedArtifact( sbtCompilerPluginGroupId, "sbt-compiler-" + compilerId,
                                         getSbtCompilerPluginVersion() );

                Set<Artifact> compilerDependencies = getAllDependencies( compilerArtifact );
                List<File> classPathFiles = new ArrayList<File>( compilerDependencies.size() + 2 );
                classPathFiles.add( compilerArtifact.getFile() );
                for ( Artifact dependencyArtifact : compilerDependencies )
                {
                    classPathFiles.add( dependencyArtifact.getFile() );
                }
//                String javaHome = System.getProperty( "java.home" );
//                classPathFiles.add( new File( javaHome, "../lib/tools.jar" ) );

                List<URL> classPathUrls = new ArrayList<URL>( classPathFiles.size() );
                for ( File classPathFile : classPathFiles )
                {
                    classPathUrls.add( new URL( classPathFile.toURI().toASCIIString() ) );
                }

                compilerClassLoader =
                    new URLClassLoader( classPathUrls.toArray( new URL[classPathUrls.size()] ),
                                        Thread.currentThread().getContextClassLoader() );
                getLog().debug( String.format( "Setting cached classloader for compiler \"%s\" with parent classloader %d",
                                               compilerId, Integer.valueOf( compilerClassLoader.getParent().hashCode() ) ) );
                setSbtCompilerCachedClassLoader( compilerId, compilerClassLoader );
            }

            ServiceLoader<AnalysisProcessor> analysisProcessorServiceLoader =
                ServiceLoader.load( AnalysisProcessor.class, compilerClassLoader );
            // get first (there should be exactly one)
            AnalysisProcessor sbtAnalysisProcessor = analysisProcessorServiceLoader.iterator().next();

            getLog().debug( String.format( "Using autodetected compiler \"%s\".", compilerId ) );

            return sbtAnalysisProcessor;
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Compiler autodetection failed", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Compiler autodetection failed", e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MojoExecutionException( "Compiler autodetection failed", e );
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Compiler autodetection failed", e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MojoExecutionException( "Compiler autodetection failed", e );
        }
    }

    private String getSbtCompilerPluginVersion() throws MojoExecutionException
    {
        return getPluginArtifact( sbtCompilerPluginGroupId, sbtCompilerPluginApiArtifactId, "jar" ).getBaseVersion();
    }

    private/*protected*/ Artifact getPluginArtifact( String groupId, String artifactId, String type )
        throws MojoExecutionException
    {
        Artifact result = null;
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( artifact.getGroupId().equals( groupId ) && artifact.getArtifactId().equals( artifactId )
                && type.equals( artifact.getType() ) )
            {
                result = artifact;
                break;
            }
        }
        if ( result == null )
        {
            throw new MojoExecutionException(
                                              String.format( "Unable to locate '%s:%s' in the list of plugin artifacts",
                                                             groupId, artifactId ) );
        }
        return result;
    }

    private File defaultAnalysisCacheFile( MavenProject p )
    {
        File classesDirectory = new File( p.getBuild().getOutputDirectory() );
        return new File( Compilers.getCacheDirectory( classesDirectory ), "compile" );
    }

    /**
     * Returns SBT incremental main compilation analysis cache file location for a project.
     * 
     * @return analysis cache file location
     */
    protected File getAnalysisCacheFile()
    {
        return defaultAnalysisCacheFile( project );
    }

}
