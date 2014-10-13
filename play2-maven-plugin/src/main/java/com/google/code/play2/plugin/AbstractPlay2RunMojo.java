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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Base class for Play&#33; server synchronously starting ("run" and "test") mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2RunMojo
    extends AbstractPlay2ServerMojo
{

    /**
     * Allows the server startup to be skipped.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.runSkip", defaultValue = "false" )
    private boolean runSkip;

    /**
     * Run in forked Java process.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.runFork", defaultValue = "true" )
    private boolean runFork;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( runSkip )
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

        File pidFile = new File( baseDir, "RUNNING_PID" );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        Java javaTask = prepareAntJavaTask( runFork );
        javaTask.setFailonerror( true );
        PidFileDeleter.getInstance().add( pidFile );

        JavaRunnable runner = new JavaRunnable( javaTask );
        // maybe just like that:
        getLog().info( "Launching Play! server" );
        runner.run();
        /*Thread t = new Thread( runner, "Play! Server runner" );
        getLog().info( "Launching Play! Server" );
        t.start();
        try
        {
            t.join(); // waiting for Ctrl+C if forked, joins immediately if not forking
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "?", e );
        }*/
        BuildException runException = runner.getException();
        if ( runException != null )
        {
            throw new MojoExecutionException( "Play! server run exception", runException );
        }

        if ( !runFork )
        {
            while ( true ) // wait for Ctrl+C
            {
                try
                {
                    Thread.sleep( 10000 );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException( "?", e );
                }
            }
        }
    }

}
