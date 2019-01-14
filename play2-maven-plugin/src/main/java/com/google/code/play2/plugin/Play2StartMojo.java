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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Start Play&#33; server in production mode ({@code sbt start} equivalent).
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
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.startSkip", defaultValue = "false" )
    private boolean startSkip;

    /**
     * Identifier of the module to start.
     * <br>
     * <br>
     * Important in multi-module projects with more than one {@code play2} modules
     * to choose which one should be started.
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
     * Spawns started JVM process.
     * <br>
     * <br>
     * See <a href="http://ant.apache.org/manual/Tasks/java.html">Ant Java task
     * documentation</a> for details.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.startSpawn", defaultValue = "true" )
    private boolean startSpawn;

    /**
     * Start server synchronously.
     * <br>
     * <br>
     * After starting server wait for {@code startCheckUrl} URL to be available.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.startSynchro", defaultValue = "false" )
    private boolean startSynchro;

    /**
     * Server start timeout in milliseconds.
     * <br>
     * <br>
     * Used only if {@code startSynchro} is true.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.startTimeout", defaultValue = "0" )
    private int startTimeout;

    /**
     * Relative or absolute URL to check periodically when server is starting.
     * <br>
     * <br>
     * Server is started when connection to this URL returns any content
     * (does not throw IOException).
     * <br>
     * Relative URL is allowed only if absolute URL starts with
     * {@code http://0.0.0.0:9000} (protocol, host and port have
     * their default values).
     * <br>
     * If URL is relative, it has to start with a slash character
     * (like URLs in {@code conf/routes} file).
     * <br>
     * <br>
     * Used only if {@code startSynchro} is true.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.startCheckUrl", defaultValue = "/" )
    private String startCheckUrl;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( startSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        if ( !isMainModule() )
        {
            getLog().debug( "Not main module - skipping execution" );
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

        getLog().info( "Starting Play! server" );

        Java javaTask = getStartServerTask( startSpawn );

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
                throw new MojoExecutionException( "Play! server start interrupted", e );
            }
            BuildException startServerException = runner.getException();
            if ( startServerException != null )
            {
                throw new MojoExecutionException( "Play! server start exception", startServerException );
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

    private boolean isMainModule()
    {
        return mainModule == null || "".equals( mainModule ) || isMatchingProject( project, mainModule );
    }

}
