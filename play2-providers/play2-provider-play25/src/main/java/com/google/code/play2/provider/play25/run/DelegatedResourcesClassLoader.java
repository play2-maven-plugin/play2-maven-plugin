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

package com.google.code.play2.provider.play25.run;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A ClassLoader that only uses resources from its parent
 */
public class DelegatedResourcesClassLoader
    extends NamedURLClassLoader
{
    public DelegatedResourcesClassLoader( String name, URL[] urls, ClassLoader parent )
    {
        super( name, urls, parent );
    }

    @Override
    public Enumeration<URL> getResources( String name )
        throws IOException
    {
        return getParent().getResources( name );
    }

}
