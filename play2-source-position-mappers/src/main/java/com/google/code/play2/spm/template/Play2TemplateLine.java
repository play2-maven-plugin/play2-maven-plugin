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

package com.google.code.play2.spm.template;

class Play2TemplateLine
{
    public int line;

    public int start;

    public int end;

    public String content;

    Play2TemplateLine( int line, int start, int end, String content )
    {
        this.line = line;
        this.start = start;
        this.end = end;
        this.content = content;
    }

    public Play2TemplateLocation location( int o )
    {
        int offset = Math.min( Math.max( start, o ), end );
        int column = offset - start;
        String lineContent = content;
        if ( lineContent.endsWith( "\r" ) ) // remove trailing CR (on Windows)
        {
            lineContent = lineContent.substring( 0, lineContent.length() - 1 );
        }
        return new Play2TemplateLocation( line, column, offset, lineContent );
    }

}