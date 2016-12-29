/*
 * Copyright 2013-2016 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.watcher.polling;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.play2.watcher.api.AbstractFileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchLogger;

/**
 * Polling file watcher.
 */
public class PollingFileWatcher
    extends AbstractFileWatcher
    implements Runnable
{
    private final List<File> dirsToWatch;

    private final int pollDelayMillis;

    private boolean closed;

    private Map<String, Long> previousFileTimestamps = null;

    /**
     * Creates polling file watcher.
     * 
     * @param log a logger
     * @param dirsToWatch directories to watch
     * @param watchCallback watch callback
     * @param pollDelayMillis poll delay (milliseconds)
     */
    public PollingFileWatcher( FileWatchLogger log, List<File> dirsToWatch, final FileWatchCallback watchCallback,
                               int pollDelayMillis )
    {
        super( log, watchCallback );

        this.dirsToWatch = dirsToWatch;
        this.pollDelayMillis = pollDelayMillis;
    }

    @Override /* FileWatcher */
    public synchronized void close()
    {
        closed = true;
    }

    private synchronized boolean isClosed()
    {
        return closed;
    }

    @Override /* Runnable */
    public void run()
    {
        while ( !isClosed() )
        {
            watch();

            try
            {
                Thread.sleep( pollDelayMillis );
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    private void watch()
    {
        // Collect current file timestamps
        Map<String, Long> currentFileTimestamps = allFileTimestamps( dirsToWatch );

        // Compare with previous ones
        if ( previousFileTimestamps != null ) // not first run
        {
            Set<String> currentPaths = new HashSet<String>( currentFileTimestamps.keySet() );
            for ( Map.Entry<String, Long> previousEntry : previousFileTimestamps.entrySet() )
            {
                String path = previousEntry.getKey();
                Long prevTimeStamp = previousEntry.getValue();
                Long currentTimestamp = currentFileTimestamps.get( path );
                if ( currentTimestamp != null )
                {
                    if ( !currentTimestamp.equals( prevTimeStamp ) )
                    {
                        debug( "[polling] File modified \"%s\"", path );
                        watchCallback.onChange( new File( path ) );
                    }
                    currentPaths.remove( path ); // remove processed from map
                }
                else
                {
                    debug( "[polling] File deleted \"%s\"", path );
                    watchCallback.onChange( new File( path ) );
                }
            }

            for ( String path : currentPaths )
            {
                debug( "[polling] File created \"%s\"", path );
                watchCallback.onChange( new File( path ) );
            }
        }

        previousFileTimestamps = currentFileTimestamps;
    }

    private Map<String, Long> allFileTimestamps( List<File> dirs )
    {
        Map<String, Long> result = new HashMap<String, Long>();
        List<String> processedDirs = new ArrayList<String>();
        for ( File dir : dirs )
        {
            internalAllFileTimestamps( dir, processedDirs, result );
        }
        return result;

    }

    private void internalAllFileTimestamps( File dir, List<String> processedDirs, Map<String, Long> result )
    {
        if ( !processedDirs.contains( dir ) )
        {
            processedDirs.add( dir.getAbsolutePath() );
            for ( File file : dir.listFiles() )
            {
                if ( file.isFile() )
                {
                    result.put( file.getAbsolutePath(), Long.valueOf( file.lastModified() ) );
                }
                else if ( file.isDirectory() )
                {
                    internalAllFileTimestamps( file, processedDirs, result );
                }
            }
        }
    }

    private void debug( String message, Object... args )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( String.format( message, args ) );
        }
    }

}