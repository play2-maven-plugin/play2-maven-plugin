/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.spm.routes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Play2RoutesGeneratedSource
{
    static final String SOURCE_PREFIX = "// @SOURCE:";

    private static final Pattern LINE_MARKER = Pattern.compile( "\\s*// @LINE:\\s*(\\d+)\\s*" );

    private String[] lines; // generated file lines

    Play2RoutesGeneratedSource( String[] lines )
    {
        this.lines = lines;
    }

    public String getSourceFileName()
    {
        String result = null;
        for ( String line: lines )
        {
            if ( line.startsWith( SOURCE_PREFIX ) )
            {
                result = line.trim().substring( SOURCE_PREFIX.length() );
                break;
            }
        }
        return result;
    }

    public int mapLine( int generatedLine )
    {
        int result = -1;

        for ( int i = generatedLine/* - 1*/; i > 3; i-- )
        {
            String line = lines[ i ];
            Matcher matcher = LINE_MARKER.matcher( line );
            if ( matcher.find() )
            {
                result = Integer.parseInt( matcher.group( 1 ) );
                break;
            }
        }
        return result;
    }

}