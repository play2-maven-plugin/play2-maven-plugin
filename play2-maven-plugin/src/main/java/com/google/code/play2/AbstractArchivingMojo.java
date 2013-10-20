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
import java.io.InputStream;

import org.apache.maven.plugins.annotations.Component;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

/**
 * Base class for Play&#33; packaging mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractArchivingMojo
    extends AbstractPlay2Mojo
{

    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     */
    @Component
    private ArchiverManager archiverManager;

    protected void expandArchive( Archiver archiver, File destDirectory )
        throws IOException
    {
        for ( ResourceIterator iter = archiver.getResources(); iter.hasNext(); )
        {
            ArchiveEntry entry = iter.next();
            String name = entry.getName();
            name = name.replace( File.separatorChar, '/' );
            File destFile = new File( destDirectory, name );

            PlexusIoResource resource = entry.getResource();
            boolean skip = false;
            if ( destFile.exists() )
            {
                long resLastModified = resource.getLastModified();
                if ( resLastModified != PlexusIoResource.UNKNOWN_MODIFICATION_DATE )
                {
                    long destFileLastModified = destFile.lastModified(); // TODO-use this
                    if ( resLastModified <= destFileLastModified )
                    {
                        skip = true;
                    }
                }
            }

            if ( !skip )
            {
                switch ( entry.getType() )
                {
                    case ArchiveEntry.DIRECTORY:
                        destFile.mkdirs(); // TODO-change to PlexusUtils, check result
                        break;
                    case ArchiveEntry.FILE:
                        InputStream contents = resource.getContents();
                        RawInputStreamFacade facade = new RawInputStreamFacade( contents );
                        FileUtils.copyStreamToFile( facade, destFile );
                        break;
                    default:
                        throw new RuntimeException( "Unknown archive entry type: " + entry.getType() ); // TODO-polish, what exception class?
                }
                // System.out.println(entry.getName());
            }
        }
    }
    
    protected ZipArchiver getZipArchiver() throws NoSuchArchiverException
    {
        ZipArchiver result = (ZipArchiver) archiverManager.getArchiver( "zip" );
        result.setDuplicateBehavior( Archiver.DUPLICATES_FAIL ); // Just in case
        return result;
    }
    
    protected WarArchiver getWarArchiver() throws NoSuchArchiverException
    {
        WarArchiver result = (WarArchiver) archiverManager.getArchiver( "war" );
        result.setDuplicateBehavior( Archiver.DUPLICATES_FAIL ); // Just in case
        return result;
    }

    /**
     * Check for potential "Duplicate file" exception before archive processing starts
     * 
     * @param archiver
     */
    protected void checkArchiverForProblems( Archiver archiver )
    {
        for ( ResourceIterator iter = archiver.getResources(); iter.hasNext(); )
        {
            iter.next();
        }
    }

}
