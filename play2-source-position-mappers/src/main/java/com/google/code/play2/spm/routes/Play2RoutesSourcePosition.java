/*
 * Copyright 2013-2019 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import java.io.File;

import com.google.code.sbt.compiler.api.SourcePosition;

public class Play2RoutesSourcePosition implements SourcePosition
{
    private int line;
    private String lineContent;
    private File sourceFile;
    
    public Play2RoutesSourcePosition( int line, String lineContent, File sourceFile )
    {
        this.line = line;
        this.lineContent = lineContent;
        this.sourceFile = sourceFile;
    }

    /**
     * One-based line number.
     *
     * @return line number
     */
    @Override
    public int getLine()
    {
        return line;
    }
    
    /**
     * Line content.
     *
     * @return line content
     */
    @Override
    public String getLineContent()
    {
        return lineContent;
    }
    
    /**
     * Offset not specified.
     *
     * @return offset not specified
     */
    @Override
    public int getOffset()
    {
        return -1;
    }
    
    /**
     * Position not specified.
     *
     * @return position not specified
     */
    @Override
    public int getPointer()
    {
        return -1;
    }
    
    /**
     * File.
     *
     * @return file
     */
    @Override
    public File getFile()
    {
        return sourceFile;
    }

}
