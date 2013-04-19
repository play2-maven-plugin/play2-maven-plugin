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

/**
 * Base class for Play&#33; server stopping ("stop" and "stop-server") mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2StopServerMojo
    extends AbstractPlay2Mojo
{
    protected void stopServer()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File baseDir = project.getBasedir();

        File pidFile = new File( baseDir, "RUNNING_PID"/*"server.pid"*/ );
        if ( !pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server is not started (\"%s\" file not found)",
                                                             pidFile.getName() ) );
        }

        String pid = readFileFirstLine( pidFile ).trim();
        if ( "unknown".equals( pid ) )
        {
            throw new MojoExecutionException(
                                              String.format( "Cannot stop Play! Server (unknown process id in \"%s\" file",
                                                             pidFile.getAbsolutePath() ) );
        }

        try
        {
            kill( pid );
            if ( !pidFile.delete() )
            {
                throw new IOException( String.format( "Cannot delete %s file", pidFile.getAbsolutePath() ) );
            }
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        
        PidFileDeleter.getInstance().remove( pidFile );
    }
    
    // copied from Play! Framework's "play.utils.Utils" Java class
    protected void kill( String pid )
        throws IOException, InterruptedException
    {
        String os = System.getProperty( "os.name" );
        String command = ( os.startsWith( "Windows" ) ) ? "taskkill /F /PID " + pid : "kill " + pid;
        Runtime.getRuntime().exec( command ).waitFor();
    }

}
