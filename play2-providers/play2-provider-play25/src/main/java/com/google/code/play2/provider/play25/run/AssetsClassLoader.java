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

package com.google.code.play2.provider.play25.run;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class loader for serving assets.
 *
 * Serves assets from the given directory, at the given prefix.
 */
public class AssetsClassLoader
    extends ClassLoader
{
    private String prefix;
    private File directory;

    /**
     * Creates assets class loader.
     * 
     * @param parent parent class loader.
     * @param prefix assets urls prefix.
     * @param directory assets directory.
     */
    public AssetsClassLoader( ClassLoader parent, String prefix, File directory )
    {
        super( parent );
        this.prefix = prefix;
        this.directory = directory;
    }

    @Override /* ClassLoader */
    public URL findResource( String name )
    {
        URL result = null;
        if ( name.startsWith( prefix ) && new File( directory, name.substring( prefix.length() ) ).isFile() )
        {
            try
            {
                result = new File( directory, name.substring( prefix.length() ) ).toURI().toURL();
            }
            catch ( MalformedURLException e )
            {
                // ignore, result = null;
                // maybe RuntimeException should be thrown here?
            }
        }
        return result;
    }

}
