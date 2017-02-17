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

package com.google.code.play2.provider.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Play2BuildFailure
    extends Exception
{
    private static final long serialVersionUID = 1L;

    private Play2BuildException e;

    private String charsetName;

    public Play2BuildFailure( Play2BuildException e, String charsetName )
    {
        //?super( e );
        this.e = e;
        this.charsetName = charsetName;
    }

    /**
     * Error message, if defined.
     * 
     * @return exception message
     */
    @Override
    public String getMessage()
    {
        return filterAnnoyingErrorMessages( e.getMessage() );
    }

    /**
     * Error line number, if defined.
     * 
     * @return exception line number
     */
    public int line()
    {
        return e.line();
    }

    /**
     * Column position, if defined.
     * 
     * @return exception column position
     */
    public int position()
    {
        return e.position();
    }

    /**
     * Source file.
     * 
     * @return exception source file
     */
    public File source()
    {
        return e.source();
    }

    public String input()
    {
        String result = null;
        File sourceFile = e.source();
        if ( sourceFile != null )
        {
            try
            {
                result = readFileAsString();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
        return result;
    }

    /**
     * Reads the content of the file to a string.
     * 
     * @param file a file to read the content of
     * @return the content of the file
     * @throws IOException I/O exception when reading from the file
     */
    // copied from AbstractPlay2SourcePositionMapper.java
    private String readFileAsString()
        throws IOException
    {
        FileInputStream is = new FileInputStream( e.source() );
        try
        {
            byte[] buffer = new byte[8192];
            int len = is.read( buffer );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ( len != -1 )
            {
                out.write( buffer, 0, len );
                len = is.read( buffer );
            }
            return charsetName != null ? new String( out.toByteArray(), charsetName ) : new String( out.toByteArray() );
        }
        finally
        {
            is.close();
        }
    }

    private String filterAnnoyingErrorMessages( String message )
    {
        String result = message;
        Pattern pattern =
            Pattern.compile( "(?s)overloaded method value (.*) with alternatives:(.*)cannot be applied to(.*)" );
        Matcher matcher = pattern.matcher( message );
        if ( matcher.find() )
        {
            String method = matcher.group( 1 );
            String signature = matcher.group( 3 );
            result = String.format( "Overloaded method value [%s] cannot be applied to %s", method, signature );
        }
        return result;
    }

}
