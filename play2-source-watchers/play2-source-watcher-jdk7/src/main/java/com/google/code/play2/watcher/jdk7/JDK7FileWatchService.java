/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.watcher.jdk7;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.code.play2.watcher.api.AbstractFileWatchService;
import com.google.code.play2.watcher.api.FileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchException;
import com.google.code.play2.watcher.api.FileWatchService;

import org.codehaus.plexus.component.annotations.Component;

/**
 * JDK7 file watch service.
 */
@Component( role = FileWatchService.class, hint = "jdk7", description = "JDK7" )
public class JDK7FileWatchService
    extends AbstractFileWatchService
{
    /**
     * Creates JDK7 file watch service.
     */
    public JDK7FileWatchService()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileWatcher watch( List<File> filesToWatch, FileWatchCallback watchCallback )
        throws FileWatchException
    {
        List<File> dirsToWatch = new ArrayList<File>( filesToWatch.size() );
        for ( File file: filesToWatch )
        {
            if ( file.exists() )
            {
                if ( file.isDirectory() )
                {
                    dirsToWatch.add( file );
                }
                else
                {
                    if ( log != null && log.isWarnEnabled() )
                    {
                        log.warn( String.format( "[jdk7] \"%s\" is not a directory, will not be watched.",
                                                    file.getAbsolutePath() ) );
                    }
                }
            }
            else
            {
                if ( log != null && log.isWarnEnabled() )
                {
                    log.warn( String.format( "[jdk7] \"%s\" does not exist, will not be watched.",
                                                file.getAbsolutePath() ) );
                }
            }
        }

        try
        {
            JDK7FileWatcher result = new JDK7FileWatcher( log, dirsToWatch, watchCallback );

            Thread thread = new Thread( result, "jdk7-play-watch-service" );
            thread.setDaemon( true );
            thread.start();

            return result;
        }
        catch ( IOException e )
        {
            throw new FileWatchException( "JDK7FileWatcher initialization failed", e );
        }
    }

}
