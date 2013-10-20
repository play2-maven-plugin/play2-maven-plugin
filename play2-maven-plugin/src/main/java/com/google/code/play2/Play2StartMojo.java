/*
 * Copyright 2013 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play2;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
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
@Execute( phase = LifecyclePhase.PROCESS_CLASSES )
public class Play2StartMojo
    extends AbstractPlay2StartServerMojo
{
//    /**
//     * Play! id (profile) used when starting server without tests.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.id", defaultValue = "" )
//    private String playId;

//    /**
//     * Play! id (profile) used when starting server with tests.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.testId", defaultValue = "test" )
//    private String playTestId;

    /**
     * Allows the server startup to be skipped.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSkip", defaultValue = "false" )
    private boolean startSkip;

//    /**
//     * Start server with test profile.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.startWithTests", defaultValue = "false" )
//    private boolean startWithTests;

    /**
     * Spawns started JVM process. See <a href="http://ant.apache.org/manual/Tasks/java.html">Ant Java task documentation</a> for details.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSpawn", defaultValue = "true" )
    private boolean startSpawn;

    /**
     * After starting server wait for "http://localhost:${httpPort}/" URL to be available.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.startSynchro", defaultValue = "false" )
    private boolean startSynchro;

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
        
//        String startPlayId = ( startWithTests ? playTestId : playId );
        
        File baseDir = project.getBasedir();

        // Make separate method for checking conf file (use in "run" and "start" mojos)
        File confDir = new File(baseDir, "conf");
        if (!confDir.isDirectory())
        {
            getLog().info( "Skipping execution" );
            return;
        }
        if (!new File(confDir, "application.conf").isFile() && !new File(confDir, "application.json").isFile())
        {
            getLog().info( "Skipping execution" );
            return;
        }
        
//        ConfigurationParser configParser =  getConfiguration( startPlayId );

//        String sysOut = configParser.getProperty( "application.log.system.out" );
//        boolean redirectSysOutToFile = !( "false".equals( sysOut ) || "off".equals( sysOut ) );

        File logFile = null;
        //if ( redirectSysOutToFile )
        //{
            File logDirectory = new File( baseDir, "logs" );
            logFile = new File( logDirectory, "system.out" );
        //}

//        if ( redirectSysOutToFile )
//        {
            getLog().info( String.format( "Starting Play! Server, output is redirected to %s", logFile.getPath() ) );
//        }
//        else
//        {
//            getLog().info( "Starting Play! Server" );
//        }

        Java javaTask = getStartServerTask( /*configParser, startPlayId, */logFile, startSpawn );

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! Server runner" );
        t.start();
        try
        {
            t.join();
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        Exception startServerException = runner.getException();
        if ( startServerException != null )
        {
            throw new MojoExecutionException( "?", startServerException );
        }
        
        if ( startSynchro )
        {
            String rootUrl = getRootUrl( /*configParser*/ );

            getLog().info( String.format( "Waiting for %s", rootUrl ) );

            waitForServerStarted( rootUrl, runner );
        }
        
        getLog().info( "Play! Server started" );
    }

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }

}
