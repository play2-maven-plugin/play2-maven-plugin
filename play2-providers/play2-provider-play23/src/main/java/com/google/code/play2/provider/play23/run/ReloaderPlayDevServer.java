/*
 * Copyright 2013-2018 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play23.run;

import java.io.IOException;
import java.util.jar.JarFile;

import com.google.code.play2.provider.api.Play2DevServer;

import play.core.server.ServerWithStop;

public class ReloaderPlayDevServer
    implements Play2DevServer
{
    private ServerWithStop server;

    private JarFile docsJarFile; // optional

    private Reloader reloader;

    public ReloaderPlayDevServer( ServerWithStop server, JarFile docsJarFile, Reloader reloader )
    {
        this.server = server;
        this.docsJarFile = docsJarFile;
        this.reloader = reloader;
    }

    @Override
    public void close()
        throws IOException
    {
        server.stop();
        if ( docsJarFile != null )
        {
            docsJarFile.close();
        }
        reloader.close();
    }

}
