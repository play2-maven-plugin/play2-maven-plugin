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

import java.io.File;

import com.google.code.sbt.compiler.api.SourcePosition;

public class Play2TemplateSourcePosition implements SourcePosition
{

    private File source; // not nullable
    private Play2TemplateLocation location; // not nullable
                    
    public Play2TemplateSourcePosition(File source, Play2TemplateLocation location)
    {
        this.source = source;
        this.location = location;
    }

    /**
     * One-based line number.
     *
     * @return line number
     */
    @Override
    public int getLine()
    {
        return location.line;
    }
    
    /**
     * Line content.
     *
     * @return line content
     */
    @Override
    public String getLineContent()
    {
        return location.content;
    }
    
    /**
     * Zero-based offset in characters.
     *
     * @return file offset
     */
    @Override
    public int getOffset()
    {
        return location.offset;
    }
    
    /**
     * One-based position in the line.
     *
     * @return position in the line
     */
    @Override
    public int getPointer()
    {
        return location.column;
    }
    
    /**
     * File.
     *
     * @return file
     */
    @Override
    public File getFile()
    {
        return source;
    }

}
