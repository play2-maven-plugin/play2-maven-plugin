/*
 * Copyright 2013-2018 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;

/**
 * Package Play&#33; framework and Play&#33; application as one zip achive (standalone distribution).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "dist", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class Play2DistMojo
    extends AbstractPlay2DistMojo
{
    private static final String[] SUPPORTED_ARCHIVE_FORMATS = {
        "zip",
        "jar",
        "tar",
        "tar.bz2",
        "tar.gz",
        "tar.snappy",
        "tar.xz" };

    /**
     * Skip distribution file generation.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distSkip", defaultValue = "false" )
    private boolean distSkip;

    /**
     * The directory for the generated distribution file.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distOutputDirectory", defaultValue = "${project.build.directory}", required = true )
    private String distOutputDirectory;

    /**
     * The name of the generated distribution file.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distArchiveName", defaultValue = "${project.build.finalName}", required = true )
    private String distArchiveName;

    /**
     * Classifier to add to the generated distribution file.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distClassifier", defaultValue = "dist" )
    private String distClassifier;

    /**
     * Attach generated distribution file to project artifacts.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distAttach", defaultValue = "false" )
    private boolean distAttach;

    /**
     * Distribution archive formats.
     * <br>
     * <br>
     * Comma-separated list of archive formats to generate.
     * <br>
     * Supported formats:
     * <ul>
     * <li>{@code zip}</li>
     * <li>{@code jar}</li>
     * <li>{@code tar}</li>
     * <li>{@code tar.bz2}, {@code tbz2}</li>
     * <li>{@code tar.gz}, {@code tgz}</li>
     * <li>{@code tar.snappy}</li>
     * <li>{@code tar.xz}, {@code txz}</li>
     * </ul>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distFormats", defaultValue = "zip" )
    private String distFormats;

    /**
     * Sets the tar archiver behavior on file paths more than 100 characters long.
     * <br>
     * Valid values:
     * <ul>
     * <li>{@code warn} - GNU extensions are used with warning</li>
     * <li>{@code gnu} - GNU extensions are used without warning</li>
     * <li>{@code posix_warn} - POSIX extensions are used with warning</li>
     * <li>{@code posix} - POSIX extensions are used without warning</li>
     * <li>{@code truncate} - paths are truncated to the maximum length</li>
     * <li>{@code omit} - paths are omitted from the archive</li>
     * <li>{@code fail} - build fails</li>
     * </ul>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.distTarLongFileMode", defaultValue = "warn" )
    private String distTarLongFileMode;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( distSkip )
        {
            getLog().info( "Dist generation skipped" );
            return;
        }

        if ( !isMainModule() )
        {
            getLog().debug( "Not main module - skipping execution" );
            return;
        }

        File buildDirectory = new File( project.getBuild().getDirectory() );

        String prodServerMainClassName = getProdServerMainClassName();

        File linuxStartFile = createLinuxStartFile( buildDirectory, prodServerMainClassName );
        File windowsStartFile = createWindowsStartFile( buildDirectory, prodServerMainClassName );

        List<String> supportedFormatsAsList = Arrays.asList( SUPPORTED_ARCHIVE_FORMATS );

        try
        {
            for ( String format: distFormats.split( "," ) )
            {
                if ( !supportedFormatsAsList.contains( format ) )
                {
                    getLog().warn( String.format( "\"%s\" archive format not supported.", format ) );
                }
                else
                {
                    createArchive( format, linuxStartFile, windowsStartFile );
                }
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

    private void createArchive( String format, File linuxStartFile, File windowsStartFile )
        throws IOException, NoSuchArchiverException, MojoExecutionException
    {
        File destFile = new File( distOutputDirectory, getDestinationFileName( format ) );

        Archiver archiver = isTarArchiveFormat( format ) ? createTarArchiver( format ) : getArchiver( format );
        addArchiveContent( archiver, linuxStartFile, windowsStartFile );
        archiver.setDestFile( destFile );

        archiver.createArchive();

        if ( distAttach )
        {
            projectHelper.attachArtifact( project, format, distClassifier, destFile );
        }

        getLog().info( String.format( "%nYour application is ready in %s%n%n", destFile.getCanonicalPath() ) );
    }

    private boolean isTarArchiveFormat( String format )
    {
        return "txz".equals( format ) || "tgz".equals( format ) || "tbz2".equals( format ) || "tar".equals( format )
            || format.startsWith( "tar." );
    }

    private String getDestinationFileName( String extension )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( distArchiveName );
        if ( distClassifier != null && !"".equals( distClassifier ) )
        {
            if ( !distClassifier.startsWith( "-" ) )
            {
                buf.append( '-' );
            }
            buf.append( distClassifier );
        }
        buf.append( "." ).append( extension );
        return buf.toString();
    }

    private Archiver createTarArchiver( String format )
        throws NoSuchArchiverException
    {
        TarArchiver tarArchiver = (TarArchiver) getArchiver( "tar" );
        int index = format.indexOf( '.' );
        if ( index >= 0 )
        {
            TarArchiver.TarCompressionMethod tarCompressionMethod;
            String compression = format.substring( index + 1 );
            if ( "gz".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.gzip;
            }
            else if ( "bz2".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.bzip2;
            }
            else if ( "xz".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.xz;
            }
            else if ( "snappy".equals( compression ) )
            {
                tarCompressionMethod = TarArchiver.TarCompressionMethod.snappy;
            }
            else
            {
                throw new ArchiverException( "Unknown compression format: " + compression );
            }
            tarArchiver.setCompression( tarCompressionMethod );
        }
        else if ( "tgz".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.gzip );
        }
        else if ( "tbz2".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.bzip2 );
        }
        else if ( "txz".equals( format ) )
        {
            tarArchiver.setCompression( TarArchiver.TarCompressionMethod.xz );
        }

        tarArchiver.setLongfile( TarLongFileMode.valueOf( distTarLongFileMode ) );

        return tarArchiver;
    }

}
