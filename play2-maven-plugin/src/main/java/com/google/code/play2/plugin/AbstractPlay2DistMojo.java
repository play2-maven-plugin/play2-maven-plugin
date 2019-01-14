/*
 * Copyright 2013-2019 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;

import org.codehaus.plexus.archiver.Archiver;

import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2Runner;

/**
 * Base class for Play&#33; distribution packaging mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2DistMojo
    extends AbstractArchivingMojo
{
    /**
     * Identifier of the module to prepare the distribution for.
     * <br>
     * <br>
     * Important in multi-module projects with more than one {@code play2} modules
     * to select for which one to prepare the distribution.
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
     * If not specified, all reactor modules with {@code play2} packaging will be selected.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.mainModule", defaultValue = "" )
    private String mainModule;

    /**
     * Extra settings used only in production mode (like {@code devSettings}
     * for development mode).
     * <br>
     * <br>
     * Space-separated list of key=value pairs, e.g.
     * <br>
     * {@code play.server.http.port=9001 play.server.https.port=9443}
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.prodSettings", defaultValue = "" )
    private String prodSettings;

    /**
     * Additional JVM arguments passed to Play! server's JVM
     * <br>
     * <br>
     * Space-separated list of arguments, e.g.
     * <br>
     * {@code -Xmx1024m -Dconfig.resource=application-prod.conf -Dlogger.file=./conf/logback-prod.xml}
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.serverJvmArgs", defaultValue = "" )
    private String serverJvmArgs;

    /**
     * Distribution top level directory.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distTopLevelDirectory", defaultValue = "${project.build.finalName}" )
    private String distTopLevelDirectory;

    /**
     * Distribution additional project artifacts include filter.
     * 
     * Comma-separated list of the classifiers of project's additional artifacts
     * to include.
     * For example {@code assets} value means that <code>target/${artifactId}-${version}-assets.jar</code>
     * will be added to {@code lib} directory in distribution archive.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distClassifierIncludes", defaultValue = "" )
    private String distClassifierIncludes;

    /**
     * Distribution dependency include filter.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distDependencyIncludes", defaultValue = "" )
    private String distDependencyIncludes;

    /**
     * Distribution dependency exclude filter.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distDependencyExcludes", defaultValue = "" )
    private String distDependencyExcludes;

    protected void addArchiveContent( Archiver archiver, File linuxStartFile, File windowsStartFile )
        throws MojoExecutionException
    {
        File baseDir = project.getBasedir();
        File buildDirectory = new File( project.getBuild().getDirectory() );

        File projectArtifactFile = new File( buildDirectory, project.getBuild().getFinalName() + ".jar" ); // project.getArtifact().getFile();
        if ( !projectArtifactFile.isFile() )
        {
            throw new MojoExecutionException( String.format( "%s not present", projectArtifactFile.getAbsolutePath() ) );
            // TODO - add info about running "mvn package first"
        }

        String distPathPrefix = "";
        if ( distTopLevelDirectory != null && !"".equals( distTopLevelDirectory ) )
        {
            distPathPrefix = distTopLevelDirectory + '/';
        }
        String distLibPath = distPathPrefix + "lib/";

        String destinationFileName = distLibPath + projectArtifactFile.getName();
        archiver.addFile( projectArtifactFile, destinationFileName );

        if ( distClassifierIncludes != null && distClassifierIncludes.length() > 0 )
        {
            List<String> incl = Arrays.asList( distClassifierIncludes.split( "," ) );
            for ( String classifier: incl )
            {
                String projectAttachedArtifactFileName =
                    String.format( "%s-%s.jar", project.getBuild().getFinalName(), classifier.trim() );
                File projectAttachedArtifactFile = new File( buildDirectory, projectAttachedArtifactFileName );
                if ( !projectAttachedArtifactFile.isFile() )
                {
                    throw new MojoExecutionException( String.format( "%s not present", projectAttachedArtifactFile.getAbsolutePath() ) );
                }
                destinationFileName = distLibPath + projectAttachedArtifactFile.getName();
                archiver.addFile( projectAttachedArtifactFile, destinationFileName );
            }
        }

        // preparation
        Set<Artifact> projectArtifacts = project.getArtifacts();

        Set<Artifact> excludedArtifacts = new HashSet<Artifact>();

        AndArtifactFilter dependencyFilter = new AndArtifactFilter();
        if ( distDependencyIncludes != null && distDependencyIncludes.length() > 0 )
        {
            List<String> incl = Arrays.asList( distDependencyIncludes.split( "," ) );
            PatternIncludesArtifactFilter includeFilter =
                new PatternIncludesArtifactFilter( incl, true/* actTransitively */ );

            dependencyFilter.add( includeFilter );
        }
        if ( distDependencyExcludes != null && distDependencyExcludes.length() > 0 )
        {
            List<String> excl = Arrays.asList( distDependencyExcludes.split( "," ) );
            PatternExcludesArtifactFilter excludeFilter =
                new PatternExcludesArtifactFilter( excl, true/* actTransitively */ );

            dependencyFilter.add( excludeFilter );
        }

        Set<Artifact> filteredArtifacts = new HashSet<Artifact>(); // TODO-rename to filteredClassPathArtifacts
        for ( Artifact artifact: projectArtifacts )
        {
            if ( artifact.getArtifactHandler().isAddedToClasspath() && !excludedArtifacts.contains( artifact ) )
            {
                // TODO-add checkPotentialReactorProblem( artifact );
                if ( dependencyFilter.include( artifact ) )
                {
                    filteredArtifacts.add( artifact );
                }
                else
                {
                    getLog().debug( artifact.toString() + " excluded" );
                }
            }
        }

        // lib
        for ( Artifact artifact: filteredArtifacts )
        {
            File jarFile = artifact.getFile();
            StringBuilder dfnsb = new StringBuilder();
            dfnsb.append( artifact.getGroupId() ).append( '.' ).append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );
            if ( artifact.getClassifier() != null )
            {
                dfnsb.append( '-' ).append( artifact.getClassifier() );
            }
            dfnsb.append( '.' ).append( artifact.getType() );
            destinationFileName = dfnsb.toString();
            archiver.addFile( jarFile, distLibPath + destinationFileName );
        }

        if ( linuxStartFile != null && linuxStartFile.isFile() )
        {
            archiver.addFile( linuxStartFile, distPathPrefix + "start", 0755 /*permissions*/ );
        }

        if ( windowsStartFile != null && windowsStartFile.isFile() )
        {
            archiver.addFile( windowsStartFile, distPathPrefix + "start.bat" );
        }

        File readmeFile = new File( baseDir, "README" );
        if ( readmeFile.isFile() )
        {
            archiver.addFile( readmeFile, distPathPrefix + readmeFile.getName() );
        }

        checkArchiverForProblems( archiver );
    }

    protected File createLinuxStartFile( File buildDirectory, String prodServerMainClassName )
        throws IOException
    {
        File result = new File( buildDirectory, "start" );
        BufferedWriter writer = createBufferedFileWriter( result, "UTF-8" );
        try
        {
            writer.write( "#!/usr/bin/env sh" );
            writer.newLine();
            writer.write( "scriptdir=`dirname $0`" );
            writer.newLine();
            writer.write( "classpath=$scriptdir/lib/*" );
            writer.newLine();
            writer.write( "exec java $* -cp \"$classpath\"" );
            if ( prodSettings != null )
            {
                String trimmedProdSettings = prodSettings.trim();
                if ( trimmedProdSettings.length() > 0 )
                {
                    String[] args = trimmedProdSettings.split( " " );
                    for ( String arg : args )
                    {
                        writer.write( " -D" );
                        writer.write( arg );
                    }
                }
            }
            if ( serverJvmArgs != null )
            {
                String jvmArgs = serverJvmArgs.trim();
                if ( jvmArgs.length() > 0 )
                {
                    writer.write( " " );
                    writer.write( jvmArgs );
                }
            }
            writer.write( " " );
            writer.write( prodServerMainClassName );
            writer.write( " $scriptdir" );
            writer.newLine();
        }
        finally
        {
            writer.flush();
            writer.close();
        }
        return result;
    }

    protected File createWindowsStartFile( File buildDirectory, String prodServerMainClassName )
        throws IOException
    {
        File result = new File( buildDirectory, "start.bat" );
        BufferedWriter writer = createBufferedFileWriter( result, "UTF-8" );
        try
        {
            writer.write( "set scriptdir=%~dp0" );
            writer.newLine();
            writer.write( "set classpath=%scriptdir%/lib/*" );
            writer.newLine();
            writer.write( "java %* -cp \"%classpath%\"" );
            if ( prodSettings != null )
            {
                String trimmedProdSettings = prodSettings.trim();
                if ( trimmedProdSettings.length() > 0 )
                {
                    String[] args = trimmedProdSettings.split( " " );
                    for ( String arg : args )
                    {
                        writer.write( " -D" );
                        writer.write( arg );
                    }
                }
            }
            if ( serverJvmArgs != null )
            {
                String jvmArgs = serverJvmArgs.trim();
                if ( jvmArgs.length() > 0 )
                {
                    writer.write( " " );
                    writer.write( jvmArgs );
                }
            }
            writer.write( " " );
            writer.write( prodServerMainClassName );
            writer.write( " %scriptdir%" );
            writer.newLine();
        }
        finally
        {
            writer.flush();
            writer.close();
        }
        return result;
    }

    protected boolean isMainModule()
    {
        return mainModule == null || "".equals( mainModule ) || isMatchingProject( project, mainModule );
    }

    protected String getProdServerMainClassName() throws MojoExecutionException
    {
        Play2Provider play2Provider = getProvider();
        Play2Runner play2Runner = play2Provider.getRunner();

        return play2Runner.getServerMainClass();
    }

}
