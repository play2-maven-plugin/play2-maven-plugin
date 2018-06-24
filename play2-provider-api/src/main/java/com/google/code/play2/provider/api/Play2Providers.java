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

package com.google.code.play2.provider.api;

public class Play2Providers
{
    private Play2Providers()
    {
    }

    public static String getDefaultProviderId( String playVersion )
    {
        String result = null;
        if ( playVersion != null && !playVersion.isEmpty() )
        {
            if ( playVersion.startsWith( "2.7." ) || playVersion.startsWith( "2.7-" ) )
            {
                result = "play27";
            }
            else if ( playVersion.startsWith( "2.6." ) || playVersion.startsWith( "2.6-" ) )
            {
                result = "play26";
            }
            else if ( playVersion.startsWith( "2.5." ) || playVersion.startsWith( "2.5-" ) )
            {
                result = "play25";
            }
            else if ( playVersion.startsWith( "2.4." ) || playVersion.startsWith( "2.4-" ) )
            {
                result = "play24";
            }
            else if ( playVersion.startsWith( "2.3." ) || playVersion.startsWith( "2.3-" ) )
            {
                result = "play23";
            }
            else if ( playVersion.startsWith( "2.2." ) || playVersion.startsWith( "2.2-" ) )
            {
                result = "play22";
            }
            else if ( playVersion.startsWith( "2.1." ) || playVersion.startsWith( "2.1-" ) )
            {
                result = "play21";
            }
        }
        if ( result == null )
        {
            result = "play26";
        }
        return result;
    }

}
