/*
 * Copyright 2013-2020 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import org.apache.maven.plugins.annotations.Component;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

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
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * Returns preconfigured archiver
     * 
     * @param archiverName archiver name
     * @return archiver
     * @throws NoSuchArchiverException for unsupported archiver name
     */
    protected Archiver getArchiver( String archiverName )
        throws NoSuchArchiverException
    {
        Archiver result = archiverManager.getArchiver( archiverName );
        result.setDuplicateBehavior( Archiver.DUPLICATES_FAIL ); // Just in case
        return result;
    }

    /**
     * Check for potential "Duplicate file" exception before archive processing starts
     * 
     * @param archiver archiver to check
     */
    protected void checkArchiverForProblems( Archiver archiver )
    {
        for ( ResourceIterator iter = archiver.getResources(); iter.hasNext(); )
        {
            iter.next();
        }
    }

}
