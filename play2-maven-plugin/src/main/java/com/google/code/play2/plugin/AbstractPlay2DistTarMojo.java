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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;

import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;

import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2Runner;

/**
 * Base class for Play&#33; distribution packaging mojos in tar.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2DistTarMojo
    extends AbstractArchivingMojo
{

    /**
     * Custom config file.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "config.file" )
    private File configFile;

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

    protected TarArchiver prepareArchiver()
        throws IOException, MojoExecutionException, NoSuchArchiverException
    {
        Play2Provider play2Provider = getProvider();
        Play2Runner play2Runner = play2Provider.getRunner();

        String prodServerMainClassName = play2Runner.getServerMainClass();

        TarArchiver tarArchiver = getTarArchiver();

        File baseDir = project.getBasedir();
        File buildDirectory = new File( project.getBuild().getDirectory() );

        File projectArtifactFile = new File( buildDirectory, project.getBuild().getFinalName() + ".jar" ); // project.getArtifact().getFile();
        if ( !projectArtifactFile.isFile() )
        {
            throw new MojoExecutionException( String.format( "%s not present", projectArtifactFile.getAbsolutePath() ) );
            // TODO - add info about running "mvn package first"
        }

        if ( configFile != null && !configFile.isFile() )
        {
            throw new MojoExecutionException( String.format( "%s not present", configFile.getAbsolutePath() ) );
        }

        String packageName = project.getArtifactId() + "-" + project.getVersion();

        String destinationFileName = packageName + "/lib/" + projectArtifactFile.getName();
        tarArchiver.addFile( projectArtifactFile, destinationFileName );

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
                destinationFileName = packageName + "/lib/" + projectAttachedArtifactFile.getName();
                tarArchiver.addFile( projectAttachedArtifactFile, destinationFileName );
            }
        }

        // preparation
        Set<?> projectArtifacts = project.getArtifacts();

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
        for ( Iterator<?> iter = projectArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
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
        for ( Iterator<?> iter = filteredArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            File jarFile = artifact.getFile();
            StringBuilder dfnsb = new StringBuilder();
            dfnsb.append( artifact.getGroupId() ).append( '.' ).append( artifact.getArtifactId() ).append( '-' ).append( artifact.getVersion() );
            if ( artifact.getClassifier() != null )
            {
                dfnsb.append( '-' ).append( artifact.getClassifier() );
            }
            dfnsb.append( ".jar" ); // TODO-get the real extension?
            String destFileName = dfnsb.toString();
            // destinationFileName = ;
            tarArchiver.addFile( jarFile, packageName + "/lib/" + destFileName/* jarFile.getName() */ );
        }

        File linuxStartFile = createLinuxStartFile( buildDirectory, prodServerMainClassName );
        tarArchiver.addFile( linuxStartFile, packageName + "/start" );

        File windowsStartFile = createWindowsStartFile( buildDirectory, prodServerMainClassName );
        tarArchiver.addFile( windowsStartFile, packageName + "/start.bat" );

        File readmeFile = new File( baseDir, "README" );
        if ( readmeFile.isFile() )
        {
            tarArchiver.addFile( readmeFile, packageName + "/" + readmeFile.getName() );
        }

        if ( configFile != null )
        {
            tarArchiver.addFile( configFile, packageName + "/" + configFile.getName() );
        }

        checkArchiverForProblems( tarArchiver );

        return tarArchiver;
    }

    private File createLinuxStartFile( File buildDirectory, String prodServerMainClassName )
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
            if ( configFile != null )
            {
                writer.write( " -Dconfig.file=`dirname $0`/" );
                writer.write( configFile.getName() );
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

    private File createWindowsStartFile( File buildDirectory, String prodServerMainClassName )
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
            if ( configFile != null )
            {
                writer.write( " -Dconfig.file=%scriptdir%/" );
                writer.write( configFile.getName() );
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
}
