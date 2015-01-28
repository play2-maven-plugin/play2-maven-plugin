/*
 * Copyright 2013-2015 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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
import java.net.URL;
import java.net.URLConnection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Base class for Play&#33; server asynchronously starting ("start" and "start-server") mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2StartServerMojo
    extends AbstractPlay2ServerMojo
{
    protected Java getStartServerTask( boolean spawn )
        throws MojoExecutionException
    {
        File baseDir = project.getBasedir();

        File pidFile = new File( baseDir, "RUNNING_PID" );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        Java javaTask = prepareAntJavaTask( true );
        if ( spawn )
        {
            javaTask.setSpawn( true );
        }
        else
        {
            javaTask.setFailonerror( true );
            PidFileDeleter.getInstance().add( pidFile );
        }

        // moving here wasn't good PidFileDeleter.getInstance().add( pidFile );
        // addSystemProperty( javaTask, "pidFile", pidFile.getAbsolutePath() );

        /*if ( logFile != null )
        {
            if ( spawn )
            {
                addSystemProperty( javaTask, "outFile", logFile.getAbsolutePath() );
            }
            else
            {
                javaTask.setOutput( logFile );
            }
        }*/

        return javaTask;
    }

    protected String getRootUrl( String relativeUrl )
    {
        int serverPort = 9000;
        if ( httpPort != null && httpPort.length() > 0 )
        {
            serverPort = Integer.parseInt( httpPort );
        }
        /*
         * else { String serverPortStr = configParser.getProperty( "http.port" ); if ( serverPortStr != null ) {
         * serverPort = Integer.parseInt( serverPortStr ); } }
         */

        return String.format( "http://localhost:%d%s", Integer.valueOf( serverPort ), relativeUrl );
    }

    // startTimeout in milliseconds
    protected void waitForServerStarted( String rootUrl, JavaRunnable runner, int startTimeout, boolean spawned )
        throws MojoExecutionException, IOException
    {
        long endTimeMillis = startTimeout > 0  ? System.currentTimeMillis() + startTimeout : 0L;
        boolean started = false;

        URL connectUrl = new URL( rootUrl );
        int verifyWaitDelay = 1000;
        while ( !started )
        {
            if ( startTimeout > 0 && endTimeMillis - System.currentTimeMillis() < 0L )
            {
                if ( spawned )
                {
                    InternalPlay2StopMojo internalStop = new InternalPlay2StopMojo();
                    internalStop.project = project;
                    try
                    {
                        internalStop.execute();
                    }
                    catch ( MojoExecutionException e )
                    {
                        // just ignore
                    }
                    catch ( MojoFailureException e )
                    {
                        // just ignore
                    }
                }
                throw new MojoExecutionException( String.format( "Failed to start Play! server in %d ms",
                                                                 Integer.valueOf( startTimeout ) ) );
            }

            BuildException runnerException = runner.getException();
            if ( runnerException != null )
            {
                throw new MojoExecutionException( "Play! server start exception", runnerException );
            }

            try
            {
                URLConnection conn = connectUrl.openConnection();
                if ( startTimeout > 0 )
                {
                    int connectTimeOut =
                        Long.valueOf( Math.min( endTimeMillis - System.currentTimeMillis(),
                                                Integer.valueOf( Integer.MAX_VALUE ).longValue() ) ).intValue();
                    if ( connectTimeOut > 0 )
                    {
                        conn.setConnectTimeout( connectTimeOut );
                    }
                }
                connectUrl.openConnection().getContent();
                started = true;
            }
            catch ( Exception e )
            {
                // return false;
            }

            if ( !started )
            {
                long sleepTime = verifyWaitDelay;
                if ( startTimeout > 0 )
                {
                    sleepTime = Math.min( sleepTime, endTimeMillis - System.currentTimeMillis() );
                }
                if ( sleepTime > 0 )
                {
                    try
                    {
                        Thread.sleep( sleepTime );
                    }
                    catch ( InterruptedException e )
                    {
                        throw new MojoExecutionException( "?", e );
                    }
                }
            }
        }
    }

    private static class InternalPlay2StopMojo
        extends AbstractPlay2StopServerMojo
    {
        @Override
        protected void internalExecute()
            throws MojoExecutionException, MojoFailureException, IOException
        {
            stopServer();
        }
    }

}
