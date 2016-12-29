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

package com.google.code.play2.watcher.jnotify;

import java.io.File;
import java.util.List;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;

import com.google.code.play2.watcher.api.AbstractFileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchLogger;

/**
 * JNotify file watcher.
 */
public class JNotifyFileWatcher
    extends AbstractFileWatcher
    implements JNotifyListener
{
    private int[] registeredWatchIds;

    /**
     * Creates JNotify file watcher.
     * 
     * @param log a logger
     * @param dirsToWatch directories to watch
     * @param watchCallback watch callback
     * @throws JNotifyException in case of 
     */
    public JNotifyFileWatcher( FileWatchLogger log, List<File> dirsToWatch, FileWatchCallback watchCallback )
        throws JNotifyException
    {
        super( log, watchCallback );

        registeredWatchIds = new int[ dirsToWatch.size() ];
        for ( int i = 0; i < dirsToWatch.size(); i++ )
        {
            File file = dirsToWatch.get( i );
            int watchId =
                JNotify.addWatch( file.getAbsolutePath(), JNotify.FILE_ANY, true/* watchSubtree */, this/* listener */ );
            registeredWatchIds[ i ] = watchId;
            debug( "[jnotify] Watch %d added for \"%s\"", Integer.valueOf( watchId ), file.getAbsolutePath() );
        }
    }

    @Override /* JNotifyListener */
    public void fileCreated( int wd, String rootPath, String name )
    {
        debug( "[jnotify] File created \"%s\", \"%s\"", rootPath, name );
        watchCallback.onChange( new File( rootPath, name ) );
    }

    @Override /* JNotifyListener */
    public void fileDeleted( int wd, String rootPath, String name )
    {
        debug( "[jnotify] File deleted \"%s\", \"%s\"", rootPath, name );
        watchCallback.onChange( new File( rootPath, name ) );
    }

    @Override /* JNotifyListener */
    public void fileModified( int wd, String rootPath, String name )
    {
        debug( "[jnotify] File modified \"%s\", \"%s\"", rootPath, name );
        watchCallback.onChange( new File( rootPath, name ) );
    }

    @Override /* JNotifyListener */
    public void fileRenamed( int wd, String rootPath, String oldName, String newName )
    {
        debug( "[jnotify] File renamed \"%s\", \"%s\" -> \"%s\"", rootPath, oldName, newName );
        watchCallback.onChange( new File( rootPath, oldName ) );
        watchCallback.onChange( new File( rootPath, newName ) );
    }

    @Override /* FileWatcher */
    public void close()
    {
        for ( int i = 0; i < registeredWatchIds.length; i++ )
        {
            int watchId = registeredWatchIds[ i ];
            if ( watchId > 0 ) // if watch was successfully created
            {
                try
                {
                    JNotify.removeWatch( watchId );
                    debug( "[jnotify] Watch %d removed", Integer.valueOf( watchId ) );
                }
                catch ( JNotifyException e/*Throwable t*/ )
                {
                    debug( "[jnotify] Cannot remove watch %d, ignored.", registeredWatchIds[ i ] );
                    // Ignore, if we fail to remove a watch it's not the end of the world.
                    // http://sourceforge.net/p/jnotify/bugs/12/
                    // We match on Throwable because matching on an IOException didn't work.
                    // http://sourceforge.net/p/jnotify/bugs/5/
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
