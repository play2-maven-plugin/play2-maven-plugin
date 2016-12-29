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

import java.net.URL;
import java.net.URLClassLoader;

public class NamedURLClassLoader
    extends URLClassLoader
{
    private String name;

    private URL[] urls; // for toString() only

    public NamedURLClassLoader( String name, URL[] urls, ClassLoader parent )
    {
        super( urls, parent );
        this.name = name;
        this.urls = urls; // for toString() only
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( name ).append( "{" ).append( urls[0].toString() );
        for ( int i = 1; i < urls.length; i++ )
        {
            sb.append( ", " ).append( urls[i].toString() );
        }
        sb.append( "}" );
        return sb.toString();
    }

}
