/*
 * Copyright 2013 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider;

import java.io.File;

public class SourceGenerationException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    private File source;

    private int line;

    private int position;

    public SourceGenerationException( File source, String message, int atLine, int column )
    {
        super( "Compilation error[" + message + "]" );
        this.source = source;
        this.line = atLine;
        this.position = column;
    }

    /**
     * Error line number, if defined.
     */
    public int line()
    {
        return line;
    }

    /**
     * Column position, if defined.
     */
    public int position()
    {
        return position;
    }

    /**
     * Source file.
     */
    public File source()
    {
        return source;
    }

}
