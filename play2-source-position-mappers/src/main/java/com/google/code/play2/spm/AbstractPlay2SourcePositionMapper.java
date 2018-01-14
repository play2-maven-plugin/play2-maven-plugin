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

package com.google.code.play2.spm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.google.code.sbt.compiler.api.SourcePositionMapper;

/**
 * Abstract Play&#33; Framework source position mapper
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2SourcePositionMapper implements SourcePositionMapper
{
    private String charsetName;

    /**
     * Sets source files character set name (encoding).
     * 
     * @param charsetName source files character set name
     */
    @Override
    public void setCharsetName( String charsetName )
    {
        this.charsetName = charsetName;
    }

    /**
     * Reads the content of the file to a string.
     * 
     * @param file a file to read the content of
     * @return the content of the file
     * @throws IOException I/O exception when reading from the file
     */
    protected String readFileAsString( File file )
        throws IOException
    {
        FileInputStream is = new FileInputStream( file );
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

}
