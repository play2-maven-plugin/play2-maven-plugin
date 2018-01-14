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

package com.google.code.play2.provider.play21;

import com.google.code.play2.provider.api.Play2DevServer;
import com.google.code.play2.provider.api.Play2Runner;
import com.google.code.play2.provider.api.Play2RunnerConfiguration;

public class Play21Runner
    implements Play2Runner
{
    @Override
    public String getServerMainClass()
    {
        return "play.core.server.NettyServer";
    }

    @Override
    public boolean supportsRunInDevMode()
    {
        return false;
    }

    @Override
    public String getPlayDocsModuleId( String scalaBinaryVersion, String playVersion )
    {
        return null; // feature not supported
    }

    @Override
    public Play2DevServer runInDevMode( Play2RunnerConfiguration configuration )
        throws Throwable
    {
        return null; // feature not supported
    }

}
