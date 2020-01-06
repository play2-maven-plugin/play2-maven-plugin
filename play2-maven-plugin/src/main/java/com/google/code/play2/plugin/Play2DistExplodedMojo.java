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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * Create exploded Play&#33; framework and Play&#33; application (standalone distribution).
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "dist-exploded", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class Play2DistExplodedMojo
    extends AbstractPlay2DistMojo
{

    /**
     * Skip dist exploded generation.
     *
     * @since 1.0.0
     */
    @Parameter( property = "play2.distExplodedSkip", defaultValue = "false" )
    private boolean distExplodedSkip;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( distExplodedSkip )
        {
            getLog().info( "Exploded dist generation skipped" );
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

        try
        {
            File distOutputDirectory = new File( project.getBuild().getDirectory(), "dist" );
            getLog().info( "Building dist directory: " + distOutputDirectory.getAbsolutePath() );

            Archiver archiver = getArchiver( "dir" );
            addArchiveContent( archiver, linuxStartFile, windowsStartFile );
            archiver.setDestFile( distOutputDirectory );

            archiver.createArchive();

            getLog().info( String.format( "%nYour application is ready in %s%n%n",
                                          distOutputDirectory.getCanonicalPath() ) );
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

}
