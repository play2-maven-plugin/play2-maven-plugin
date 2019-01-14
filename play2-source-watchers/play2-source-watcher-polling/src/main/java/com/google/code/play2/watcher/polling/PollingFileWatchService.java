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

package com.google.code.play2.watcher.polling;

import java.io.File;
import java.util.List;

import com.google.code.play2.watcher.api.AbstractFileWatchService;
import com.google.code.play2.watcher.api.FileWatchException;
import com.google.code.play2.watcher.api.FileWatchLogger;
import com.google.code.play2.watcher.api.FileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchService;

import org.codehaus.plexus.component.annotations.Component;

/**
 * Polling file watch service.
 */
@Component( role = FileWatchService.class, hint = "polling", description = "Polling" )
public class PollingFileWatchService
    extends AbstractFileWatchService
{

    /**
     * Default poll interval in milliseconds.
     */
    private final int DEFAULT_POLL_INTERVAL = 1000;

    /**
     * Poll interval in milliseconds.
     */
    private int pollInterval;

    /**
     * Creates polling file watch service.
     */
    public PollingFileWatchService()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( FileWatchLogger log ) throws FileWatchException
    {
        super.initialize( log );

        pollInterval = DEFAULT_POLL_INTERVAL;
        String pollIntervalProp = System.getProperty( "play2.pollInterval" );
        if ( pollIntervalProp != null )
        {
            try
            {
                pollInterval = Integer.parseInt( pollIntervalProp );
            }
            catch ( NumberFormatException e )
            {
                log.warn( String.format( "Unparsable property value \"%s\", using default poll interval %d ms",
                                         pollIntervalProp, Integer.valueOf( DEFAULT_POLL_INTERVAL ) ) );
                // throw new FileWatchException( "PollingFileWatcher initialization failed", e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileWatcher watch( List<File> filesToWatch, FileWatchCallback watchCallback )
    {
        PollingFileWatcher result = new PollingFileWatcher( log, filesToWatch, watchCallback, pollInterval );

        Thread thread = new Thread( result, "polling-play-watch-service" );
        thread.setDaemon( true );
        thread.start();

        return result;
    }

}
