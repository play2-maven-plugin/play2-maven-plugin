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

package com.google.code.play2.spm.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Play2TemplateGeneratedSource
{
    private static final String META_LINES = "LINES";
    private static final String META_MATRIX = "MATRIX";
    private static final String META_SOURCE = "SOURCE";

    private static final String META_UNDEFINED = "UNDEFINED";

    private static final Pattern META_PATTERN = Pattern.compile( "([A-Z]+): (.*)" );
    private static final Pattern UNDEFINED_META_PATTERN = Pattern.compile( "([A-Z]+):" );

    private Map<String, String> meta = Collections.emptyMap();

    public Play2TemplateGeneratedSource( String[] metaSectionLines )
    {
        meta = new HashMap<String, String>( metaSectionLines.length );
        for ( String line : metaSectionLines )
        {
            Matcher matcher = META_PATTERN.matcher( line );
            if ( matcher.find() )
            {
                meta.put( matcher.group( 1 ), matcher.group( 2 ) );
            }
            else
            {
                matcher = UNDEFINED_META_PATTERN.matcher( line );
                if ( matcher.find() )
                {
                    meta.put( matcher.group( 1 ), "" );
                }
                else
                {
                    meta.put( META_UNDEFINED, "" );
                }
            }
        }
    }

    public String getSourceFileName()
    {
        return meta.get( META_SOURCE );
    }

    public int mapPosition( int generatedPosition )
    {
        return mapPositionOrLine( META_MATRIX, generatedPosition );
    }

    public int mapLine( int generatedLine )
    {
        return mapPositionOrLine( META_LINES, generatedLine );
    }

    public int mapPositionOrLine( String metaSectionName, int generatedPositionOrLine )
    {
        List<Pair> l = metaData( metaSectionName );
        for ( int i = 0; i < l.size(); i++ )
        {
            Pair p = l.get( i );
            if ( p._1 > generatedPositionOrLine )
            {
                if ( i == 0 )
                {
                    return 0;
                }
                else
                {
                    Pair pos = l.get( i - 1 );
                    return pos._2 + ( generatedPositionOrLine - pos._1 );
                }
            }
        }
        // not found
        Pair pos = l.get( l.size() - 1 );
        return pos._2 + ( generatedPositionOrLine - pos._1 );
    }

    private List<Pair> metaData( String metaSectionName )
    {
        List<Pair> lines = null;

        String m = meta.get( metaSectionName );
        if ( m != null )
        {
            String[] pos = m.split( "\\|" );
            lines = new ArrayList<Pair>( pos.length );
            for ( String p : pos )
            {
                String[] c = p.split( "->" );
                try
                {
                    lines.add( new Pair( Integer.parseInt( c[0] ), Integer.parseInt( c[1] ) ) );
                }
                catch ( Exception e )
                {
                    lines.add( new Pair( 0, 0 ) ); // Skip if LINES meta is corrupted
                }
            }
        }
        else
        {
            lines = Collections.emptyList();
        }

        return lines;
    }

    private static class Pair
    {
        int _1, _2;

        Pair( int _1, int _2 )
        {
            this._1 = _1;
            this._2 = _2;
        }
    }

}
