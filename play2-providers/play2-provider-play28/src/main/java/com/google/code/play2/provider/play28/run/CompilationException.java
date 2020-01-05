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

package com.google.code.play2.provider.play28.run;

import play.api.PlayException;

public class CompilationException extends PlayException.ExceptionSource
{
    private static final long serialVersionUID = 1L;

    private int line;
    private int position;
    private String sourceName;
    private String sourceContent;

    public CompilationException( String message, int line, int position, String sourceName, String sourceContent )
    {
        super( "Compilation error", message );
        this.line = line;
        this.position = position;
        this.sourceName = sourceName;
        this.sourceContent = sourceContent;
    }

    @Override
    public Integer line()
    {
        return line > 0 ? Integer.valueOf( line ) : null;
    }

    @Override
    public Integer position()
    {
        return position > 0 ? Integer.valueOf( position ) : null;
    }

    @Override
    public String input()
    {
        return sourceContent;
    }

    @Override
    public String sourceName()
    {
        return sourceName;
    }

}