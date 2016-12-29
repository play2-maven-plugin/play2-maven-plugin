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

package com.google.code.play2.watcher.api;

import java.util.Locale;

/**
 * Helper class.
 *
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class FileWatchServices
{
    /**
     * Returns default watch service identifier based on operating system.
     *
     * @return watch service id
     */
    public static String getDefaultWatchServiceId()
    {
        String result = "polling";
        String osName = System.getProperty( "os.name" );
        if ( osName != null )
        {
            osName = osName.toLowerCase( Locale.ENGLISH );
            if ( osName.contains( "windows" ) || osName.contains( "linux" ) )
            {
                result = isAtLeastJava7() ? "jdk7" : "jnotify";
            }
            else if ( osName.contains( "mac" ) )
            {
                result = "jnotify";
            }
        }
        return result;
    }

    private static boolean isAtLeastJava7()
    {
        boolean result = false;
        try
        {
            Class.forName( "java.nio.file.WatchService" );
            result = true;
        }
        catch ( Throwable t )
        {
            // ignore
        }
        return result;
    }

}
