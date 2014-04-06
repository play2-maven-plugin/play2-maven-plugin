/*
 * Copyright 2013-2014 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.apache.tools.ant.taskdefs.Java;

/**
 * Start Play&#33; server ("play start" equivalent).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "start", requiresDependencyResolution = ResolutionScope.RUNTIME )
public class Play2StartMojo
    extends AbstractPlay2StartServerMojo
{
    /**
     * Allows the server startup to be skipped.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSkip", defaultValue = "false" )
    private boolean startSkip;

    /**
     * Spawns started JVM process. See <a href="http://ant.apache.org/manual/Tasks/java.html">Ant Java task
     * documentation</a> for details.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSpawn", defaultValue = "true" )
    private boolean startSpawn;

    /**
     * Start server synchronously.
     * 
     * After starting server wait for "http://localhost:${httpPort}${startCheckUrl}" URL
     * to be available.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSynchro", defaultValue = "false" )
    private boolean startSynchro;

    /**
     * Server start timeout in milliseconds.
     * 
     * Used only if startSynchro is true.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startTimeout", defaultValue = "0" )
    int startTimeout;

    /**
     * URL to check periodically when server is starting.
     * Server is started when connection to this URL returns any content
     * (does not throw IOException).
     * Has to starts with slash character (like URLs in "conf/routes" file).
     * 
     * Used only if startSynchro is true.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startCheckUrl", defaultValue = "/" )
    String startCheckUrl;

    /**
     * Get the executed project from the forked lifecycle.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "executedProject" )
    private MavenProject executedProject;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( startSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        File baseDir = project.getBasedir();

        // Make separate method for checking conf file (use in "run" and "start" mojos)
        File confDir = new File( baseDir, "conf" );
        if ( !confDir.isDirectory() )
        {
            getLog().info( "Skipping execution" );
            return;
        }
        if ( !new File( confDir, "application.conf" ).isFile() && !new File( confDir, "application.json" ).isFile() )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        File logFile = null;
        File logDirectory = new File( baseDir, "logs" );
        logFile = new File( logDirectory, "system.out" );

        getLog().info( String.format( "Starting Play! server, output is redirected to %s", logFile.getPath() ) );

        Java javaTask = getStartServerTask( logFile, startSpawn );

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! server runner" );
        t.start();

        if ( startSpawn )
        {
            try
            {
                t.join();
            }
            catch ( InterruptedException e )
            {
                t.interrupt();
                throw new MojoExecutionException( "?", e );
            }
            Exception startServerException = runner.getException();
            if ( startServerException != null )
            {
                throw new MojoExecutionException( "?", startServerException );
            }
        }
        // else don't invoke t.join(), it will lead to a deadlock

        if ( startSynchro )
        {
            String rootUrl = getRootUrl( startCheckUrl );

            getLog().info( String.format( "Waiting for %s", rootUrl ) );

            waitForServerStarted( rootUrl, runner, startTimeout, startSpawn );
        }

        getLog().info( "Play! server started" );
    }

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }

}
