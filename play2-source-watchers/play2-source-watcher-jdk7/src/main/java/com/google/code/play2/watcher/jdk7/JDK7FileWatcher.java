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

package com.google.code.play2.watcher.jdk7;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import com.google.code.play2.watcher.api.AbstractFileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchLogger;

/**
 * JDK7 file watcher.
 */
public class JDK7FileWatcher
    extends AbstractFileWatcher
    implements Runnable
{
    private final static WatchEvent.Kind<?>[] EVENTS = new WatchEvent.Kind<?>[] { StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW };

    @SuppressWarnings( "restriction" )
    private final static WatchEvent.Modifier[] MODIFIERS =
        new WatchEvent.Modifier[] { com.sun.nio.file.SensitivityWatchEventModifier.HIGH };

    private WatchService watcher;

    private boolean closed;

    /**
     * Creates JDK7 file watcher.
     * 
     * @param log a logger
     * @param dirsToWatch directories to watch
     * @param watchCallback watch callback
     * @throws IOException if I/O exception occurs
     */
    public JDK7FileWatcher( FileWatchLogger log, List<File> dirsToWatch, FileWatchCallback watchCallback )
        throws IOException
    {
        super( log, watchCallback );

        watcher = FileSystems.getDefault().newWatchService();
        List<String> allPathsToWatch = allSubDirectories( dirsToWatch );
        for ( String path : allPathsToWatch )
        {
            watchDir( new File( path ) );
        }
    }

    @Override /* FileWatcher */
    public synchronized void close()
    {
        try
        {
            watcher.close();
        }
        catch ( IOException e )
        {
            log.warn( String.format( "IOException (%s) thrown while closing watcher", e.getMessage() ) );
        }
        closed = true;
    }

    private synchronized boolean isClosed()
    {
        return closed;
    }

    @Override /* Runnable */
    public void run()
    {
        try
        {
            while ( !isClosed() )
            {
                watch();
            }
        }
        catch ( ClosedWatchServiceException e )
        {
            // expected - ignore
        }
        catch ( StackOverflowError e )
        {
            // non-fatal - ignore
            log.warn( "StackOverflowError occured, ignoring" );
        }
        catch ( InterruptedException e )
        {
            // fatal
            throw new RuntimeException( e );
        }
        catch ( VirtualMachineError | ThreadDeath | LinkageError e )
        {
            // fatal
            throw e;
        }
        catch ( Throwable t )
        {
            // non-fatal - ignore
            log.warn( String.format( "Throwable %s (%s) occured, ignoring", t.getClass().getName(), t.getMessage() ) );
        }
        finally
        {
            // Just in case it wasn't closed.
            close();
        }
    }

    private void watch()
        throws InterruptedException, IOException
    {
        WatchKey watchKey = watcher.take();

        List<WatchEvent<?>> events = watchKey.pollEvents();

        for ( WatchEvent<?> event : events )
        {
            if ( event.kind() == StandardWatchEventKinds.OVERFLOW )
            {
                log.warn( "Overflow event occured, some change events were lost" );
            }
            else
            {
                File file = getEventFile( watchKey, (WatchEvent<Path>) event );
                String path = file.getAbsolutePath();
                if ( event.kind() == StandardWatchEventKinds.ENTRY_CREATE )
                {
                    debug( "[jdk7] File created \"%s\"", path );
                }
                else if ( event.kind() == StandardWatchEventKinds.ENTRY_MODIFY )
                {
                    debug( "[jdk7] File modified \"%s\"", path );
                }
                else if ( event.kind() == StandardWatchEventKinds.ENTRY_DELETE )
                {
                    debug( "[jdk7] File deleted \"%s\"", path );
                }
                watchCallback.onChange( file );
                if ( event.kind() == StandardWatchEventKinds.ENTRY_CREATE )
                {
                    if ( file.isDirectory() )
                    {
                        for ( String subdir: allSubDirectories( file ) )
                        {
                            watchDir( new File( subdir ) );
                        }
                    }
                }
            }
        }

        boolean valid = watchKey.reset();
        if ( !valid && log.isErrorEnabled() )
        {
            log.error( "Cannot reset key " + watchKey.toString() );
        }
    }

    private List<String> allSubDirectories( List<File> dirs )
    {
        List<String> result = new ArrayList<String>();
        for ( File dir : dirs )
        {
            internalAllSubDirs( dir, result );
        }
        return result;

    }

    private List<String> allSubDirectories( File dir )
    {
        List<String> result = new ArrayList<String>();
        internalAllSubDirs( dir, result );
        return result;
    }

    private void internalAllSubDirs( File dir, List<String> result )
    {
        if ( !result.contains( dir ) )
        {
            result.add( dir.getAbsolutePath() );
            for ( File file : dir.listFiles() )
            {
                if ( file.isDirectory() )
                {
                    internalAllSubDirs( file, result );
                }
            }
        }
    }

    private WatchKey watchDir( File dir )
        throws IOException
    {
        return dir.toPath().register( watcher, EVENTS, MODIFIERS );
    }

    private File getEventFile( WatchKey watchKey, WatchEvent<Path> watchEvent )
    {
        Path childPath = watchEvent.context();
        Path parentPath = (Path) watchKey.watchable();
        Path path = parentPath.resolve( childPath );
        return path.toFile();
    }

    private void debug( String message, Object... args )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( String.format( message, args ) );
        }
    }

}
