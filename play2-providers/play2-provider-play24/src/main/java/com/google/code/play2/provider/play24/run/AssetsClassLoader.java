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

package com.google.code.play2.provider.play24.run;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.code.play2.provider.api.Asset;

/**
 * A ClassLoader for serving assets.
 *
 * Serves assets from the given directories, at the given prefix.
 */
public class AssetsClassLoader
    extends ClassLoader
{
    private List<Asset> assets;

    /**
     * 
     * @param parent parent class loader.
     * @param assets a list of assets directories, paired with the prefix they should be served from.
     */
    public AssetsClassLoader( ClassLoader parent, List<Asset> assets )
    {
        super( parent );
        this.assets = assets;
    }

    @Override /* ClassLoader */
    public URL findResource( String name )
    {
        URL result = null;
        for ( Asset asset : assets )
        {
            if ( exists( name, asset.getPrefix(), asset.getDir() ) )
            {
                try
                {
                    result = new File( asset.getDir(), name.substring( asset.getPrefix().length() ) ).toURI().toURL();
                }
                catch ( MalformedURLException e )
                {
                    // ignore, result = null;
                }
                break;
            }
        }
        return result;
    }

    private boolean exists( String name, String prefix, File dir )
    {
        return name.startsWith( prefix ) && new File( dir, name.substring( prefix.length() ) ).isFile();
    }

}
