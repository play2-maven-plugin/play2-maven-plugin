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
import java.util.HashSet;
import java.util.Set;

/**
 * Shutdown hook responsible for deleting "server.pid" file.
 * 
 * Responsible for deleting "RUNNING_PID" (or any other) file,
 * if it should be deleted before Maven process ends,
 * but for some reason it wasn't (play:stop-server was not executed for example).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class PidFileDeleter
    extends Thread
{
    private static PidFileDeleter INSTANCE;

    private Set<File> pidFiles = new HashSet<File>( 1 );

    private PidFileDeleter()
    {
        super( "PidFileDeleter Shutdown Hook" );
    }

    public static synchronized PidFileDeleter getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new PidFileDeleter();
            Runtime.getRuntime().addShutdownHook( INSTANCE );
        }
        return INSTANCE;
    }

    public void add( File pidFile )
    {
        pidFiles.add( pidFile );
    }

    public void remove( File pidFile )
    {
        pidFiles.remove( pidFile );
    }

    public void run()
    {
        for ( File pidFile : pidFiles )
        {
            if ( pidFile != null && pidFile.isFile() )
            {
                if ( !pidFile.delete() )
                {
                    System.out.println( String.format( "Cannot delete %s file", pidFile.getAbsolutePath() ) );
                }
            }
        }
    }

}
