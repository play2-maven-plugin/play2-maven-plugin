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

package com.google.code.play2.spm.template;

import java.util.ArrayList;
import java.util.List;

class Play2TemplateMapping
{
    private List<Play2TemplateLine> lines;

    Play2TemplateMapping( String[] sourceLines )
    {
        lines = new ArrayList<Play2TemplateLine>( sourceLines.length );
        int line = 0;
        int start = -1;
        int end = -1;
        for ( String content : sourceLines )
        {
            line++;
            start = end + 1;
            end = end + 1 + content.length();
            lines.add( new Play2TemplateLine( line, start, end, content ) );
        }
    }

    public Play2TemplateLocation location( int offset )
    {
        int index = -1;
        for ( int i = 0; i < lines.size(); i++ )
        {
            Play2TemplateLine line = lines.get( i );
            if ( line.start <= offset )
            {
                index = i;
            }
            else
            {
                break;
            }
        }
        index = Math.max( 0, index );
        return lines.get( index ).location( offset );
    }

}
