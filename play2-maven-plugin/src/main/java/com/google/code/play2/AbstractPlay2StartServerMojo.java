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
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.tools.ant.taskdefs.Java;

/**
 * Base class for Play&#33; server asynchronously starting ("start" and "start-server") mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2StartServerMojo
    extends AbstractPlay2ServerMojo
{
    protected Java getStartServerTask( /*ConfigurationParser configParser, String playId, */File logFile, boolean spawn )
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File baseDir = project.getBasedir();

        File pidFile = new File( baseDir, "RUNNING_PID"/*"server.pid"*/ );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        if ( logFile != null )
        {
            File logDirectory = logFile.getParentFile();
            if ( !logDirectory.exists() && !logDirectory.mkdirs() )
            {
                throw new MojoExecutionException( String.format( "Cannot create %s directory",
                                                                 logDirectory.getAbsolutePath() ) );
            }
        }

        Java javaTask = prepareAntJavaTask( /*configParser, playId, */true );
        if ( spawn )
        {
            javaTask.setSpawn( true );
        }
        else
        {
            javaTask.setFailonerror( true );
            PidFileDeleter.getInstance().add( pidFile );
        }

        //przeniesienie tu bylo bledem PidFileDeleter.getInstance().add( pidFile );
        //addSystemProperty( javaTask, "pidFile", pidFile.getAbsolutePath() );

        if ( logFile != null )
        {
            if ( spawn )
            {
                addSystemProperty( javaTask, "outFile", logFile.getAbsolutePath() );
            }
            else
            {
                javaTask.setOutput( logFile );
            }
        }

        return javaTask;
    }

    protected String getRootUrl( /*ConfigurationParser configParser*/ )
    {
        int serverPort = 9000;
        if ( getHttpPort() != null && getHttpPort().length() > 0 )
        {
            serverPort = Integer.parseInt( getHttpPort() );
        }
        /*else
        {
            String serverPortStr = configParser.getProperty( "http.port" );
            if ( serverPortStr != null )
            {
                serverPort = Integer.parseInt( serverPortStr );
            }
        }*/

        return String.format( "http://localhost:%d/", serverPort );
    }

    protected void waitForServerStarted( String rootUrl, JavaRunnable runner )
        throws MojoExecutionException, IOException
    {
        // boolean timedOut = false;

        /*
         * TimerTask timeoutTask = null; if (timeout > 0) { TimerTask task = new TimerTask() { public void run() {
         * timedOut = true; } }; timer.schedule( task, timeout * 1000 ); //timeoutTask = timer.runAfter(timeout * 1000,
         * { // timedOut = true; //}) }
         */

        boolean started = false;

        URL connectUrl = new URL( rootUrl );
        int verifyWaitDelay = 1000;
        while ( !started )
        {
            // if (timedOut) {
            // throw new
            // MojoExecutionException("Unable to verify if Play! Server was started in the given time ($timeout seconds)");
            // }

            Exception runnerException = runner.getException();
            if ( runnerException != null )
            {
                throw new MojoExecutionException( "Failed to start Play! Server", runnerException );
            }

            try
            {
                connectUrl.openConnection().getContent();
                started = true;
            }
            catch ( Exception e )
            {
                // return false;
            }

            if ( !started )
            {
                try
                {
                    Thread.sleep( verifyWaitDelay );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException( "?", e );
                }
            }
        }

        /*
         * if (timeoutTask != null) { timeoutTask.cancel(); }
         */
    }

}
