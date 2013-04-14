/*
 * Copyright 2012 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import play.api.PlayException;

public class AssetCompilationException
    extends PlayException.ExceptionSource
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private File source;

    private Integer line;

    private Integer position;

    public AssetCompilationException( File source, String message, Integer atLine, Integer column )
    {
        super( "Compilation error", message );
        this.source = source;
        this.line = atLine;
        this.position = column;
    }

    /**
     * Error line number, if defined.
     */
    public Integer line()
    {
        return line;
    }

    /**
     * Column position, if defined.
     */
    public Integer position()
    {
        return position;
    }

    /**
     * Input stream used to read the source content.
     */
    public String input()
    {
        String result = null;

        if ( source != null && source.isFile() )
        {
            try
            {
                result = readFileContent( source );
            }
            catch ( IOException e )
            {
                result = null;
            }
        }
        return result;
    }

    /**
     * The source file name if defined.
     */
    public String sourceName()
    {
        return ( source != null ? source.getAbsolutePath() : null );
    }

    private String readFileContent( File file )
        throws IOException
    {
        String result = null;

        BufferedReader is = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ) );
        try
        {
            StringBuilder sb = new StringBuilder();
            String line = is.readLine();
            while ( line != null )
            {
                sb.append( line ).append( '\n' );
                line = is.readLine();
            }
            result = sb.toString();
        }
        finally
        {
            is.close();
        }
        return result;
    }
}
/*
 * case class AssetCompilationException(source: Option[File], message: String, atLine: Option[Int], column: Option[Int])
 * extends PlayException.ExceptionSource( "Compilation error", message) with FeedbackProvidedException { def line =
 * atLine.map(_.asInstanceOf[java.lang.Integer]).orNull def position =
 * column.map(_.asInstanceOf[java.lang.Integer]).orNull def input =
 * source.filter(_.exists()).map(scalax.file.Path(_).string).orNull def sourceName =
 * source.map(_.getAbsolutePath).orNull }
 */