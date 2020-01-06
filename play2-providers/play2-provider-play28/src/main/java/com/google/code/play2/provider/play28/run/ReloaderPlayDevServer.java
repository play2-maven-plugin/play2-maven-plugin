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

package com.google.code.play2.provider.play28.run;

import java.io.IOException;

import com.google.code.play2.provider.api.Play2DevServer;

import play.core.server.ReloadableServer;

public class ReloaderPlayDevServer
    implements Play2DevServer
{
    private ReloadableServer server;

    private Reloader reloader;

    public ReloaderPlayDevServer( ReloadableServer server, Reloader reloader )
    {
        this.server = server;
        this.reloader = reloader;
    }

    @Override
    public void close()
        throws IOException
    {
        server.stop();
        reloader.close();
    }

}
