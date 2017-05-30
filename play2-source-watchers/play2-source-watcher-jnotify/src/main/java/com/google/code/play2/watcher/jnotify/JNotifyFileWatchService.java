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

package com.google.code.play2.watcher.jnotify;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import com.google.code.play2.watcher.api.AbstractFileWatchService;
import com.google.code.play2.watcher.api.FileWatchLogger;
import com.google.code.play2.watcher.api.FileWatcher;
import com.google.code.play2.watcher.api.FileWatchCallback;
import com.google.code.play2.watcher.api.FileWatchException;
import com.google.code.play2.watcher.api.FileWatchService;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

import org.codehaus.plexus.component.annotations.Component;

/**
 * JNotify file watch service.
 */
@Component( role = FileWatchService.class, hint = "jnotify", description = "JNotify" )
public class JNotifyFileWatchService
    extends AbstractFileWatchService
{
    /**
     * Creates JNotify file watch service.
     */
    public JNotifyFileWatchService()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( FileWatchLogger log ) throws FileWatchException
    {
        super.initialize( log );

        File nativeLibsDirectory = new File( "." ); // maybe change to temporary directory?
        String nativeLibsDirectoryProp = System.getProperty( "play2.nativeLibsDirectory" );
        if ( nativeLibsDirectoryProp != null && !"".equals( nativeLibsDirectoryProp ) )
        {
            nativeLibsDirectory = new File( nativeLibsDirectoryProp );
        }
        else
        {
            String targetDirectoryProp = System.getProperty( "project.build.directory" );
            if ( targetDirectoryProp != null && !"".equals( targetDirectoryProp ) )
            {
                File targetDirectory = new File( targetDirectoryProp );
                nativeLibsDirectory = new File( targetDirectory, "native_libraries" );
            }
        }
        String nativeLibsPath = nativeLibsDirectory.getAbsolutePath();

        if ( !nativeLibsDirectory.exists() && !nativeLibsDirectory.mkdirs() )
        {
            throw new FileWatchException( String.format( "Cannot create \"%s\" directory", nativeLibsPath ) );
        }

        String libraryOS = null;
        String libraryName = "jnotify";
        String osName = System.getProperty( "os.name" );
        if ( osName != null )
        {
            osName = osName.toLowerCase( Locale.ENGLISH );
            String architecture = System.getProperty( "sun.arch.data.model" );
            if ( osName.startsWith( "windows" ) )
            {
                libraryOS = "windows" + architecture;
                if ( "amd64".equals( System.getProperty( "os.arch" ) ) )
                {
                    libraryName = "jnotify_64bit";
                }
            }
            else if ( osName.equals( "linux" ) )
            {
                libraryOS = "linux" + architecture;
            }
            else if ( osName.startsWith( "mac os x" ) )
            {
                libraryOS = "osx";
            }
        }

        if ( libraryOS == null )
        {
            throw new FileWatchException(
                                          String.format( "JNotifyFileWatchService initialization failed - unsupported OS \"%s\"",
                                                         osName ) );
        }

        String libraryResourceName = System.mapLibraryName( libraryName );
        libraryResourceName = libraryResourceName.replace( ".dylib", ".jnilib" ); // fix for JDK-7134701 bug
        File outputFile = new File( nativeLibsDirectory, libraryResourceName );
        if ( !outputFile.exists() )
        {
            try
            {
                copyResourceToFile( "META-INF/native/" + libraryOS, libraryResourceName, nativeLibsDirectory );
            }
            catch ( IOException e )
            {
                throw new FileWatchException( "JNotifyFileWatchService initialization failed", e );
            }
        }

        // hack to update java.library.path
        try
        {
            String javaLibraryPath = System.getProperty( "java.library.path" );
            javaLibraryPath =
                javaLibraryPath != null ? javaLibraryPath + File.pathSeparator + nativeLibsPath : nativeLibsPath;
            System.setProperty( "java.library.path", javaLibraryPath );

            Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
            fieldSysPath.setAccessible( true );
            fieldSysPath.set( null, null );
        }
        catch ( Exception e )
        {
            throw new FileWatchException( "JNotifyFileWatchService initialization failed", e );
        }

        // initialize JNotify
        try
        {
            JNotify.removeWatch( 0 );
        }
        catch ( JNotifyException e )
        {
            throw new FileWatchException( "JNotifyFileWatchService initialization failed", e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileWatcher watch( List<File> filesToWatch, FileWatchCallback watchCallback )
        throws FileWatchException
    {
        try
        {
            return new JNotifyFileWatcher( log, filesToWatch, watchCallback );
        }
        catch ( JNotifyException e )
        {
            throw new FileWatchException( "JNotifyFileWatcher initialization failed", e );
        }
    }

    private void copyResourceToFile( String resourcePath, String libraryName, File outputDirectory )
        throws IOException, FileWatchException
    {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream( resourcePath + "/" + libraryName );
        if ( is == null ) // should never happen
        {
            throw new FileWatchException( "JNotifyFileWatcherService initialization failed, native library resource \""
                + resourcePath + "/" + libraryName + "\" not found" );
        }

        try
        {
            byte[] buffer = new byte[8192];
            int len = is.read( buffer );

            File outputFile = new File( outputDirectory, libraryName );
            OutputStream os = new FileOutputStream( outputFile );
            try
            {
                while ( len != -1 )
                {
                    os.write( buffer, 0, len );
                    len = is.read( buffer );
                }
            }
            finally
            {
                os.flush();
                os.close();
            }
        }
        finally
        {
            is.close();
        }
    }

}
