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
import java.io.IOException;
import java.util.Arrays;

import com.google.code.sbt.compiler.api.SourcePosition;

import com.google.code.play2.spm.AbstractPlay2SourcePositionMapper;

/**
 * Play&#33; Framework routes file source position mapper
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class Play2RoutesSourcePositionMapper extends AbstractPlay2SourcePositionMapper
{
    private static final String GENERATOR_LINE = "// @GENERATOR:play-routes-compiler";

    /**
     * Performs mapping from the position in generated source file to the position in {@code routes} file it was
     * generated from.<br>
     * <br>
     * Returns:
     * <ul>
     * <li>position in {@code routes} file</li>
     * <li>{@code null} value if file of the input position is not recognized as generated from {@code routes} file</li>
     * </ul>
     *
     * @param p compilation error/warning position in generated file
     * @return position in {@code routes} file or {@code null} value if file of the input position is not recognized as
     *         generated from {@code routes} file
     * @throws IOException I/O exception during generated or original source file reading
     */
    @Override
    public SourcePosition map( SourcePosition p ) throws IOException
    {
        SourcePosition result = null;

        File generatedFile = p.getFile();
        if ( generatedFile != null && generatedFile.isFile() )
        {
            String[] generatedFileLines = readFileAsString( generatedFile ).split( "\n" );
            if ( generatedFileLines[0].startsWith( Play2RoutesGeneratedSource.SOURCE_PREFIX ) // play 2.2.x - 2.3.x
                || Arrays.asList( generatedFileLines ).contains( GENERATOR_LINE ) ) // play 2.4.x +
            {
                Play2RoutesGeneratedSource generatedSource = new Play2RoutesGeneratedSource( generatedFileLines );
                String sourceFileName = generatedSource.getSourceFileName();
                if ( sourceFileName != null )
                {
                    File sourceFile = new File( sourceFileName );
                    if ( sourceFile.isFile() )
                    {
                        int sourceLine = generatedSource.mapLine( p.getLine() );
                        String[] sourceFileLines = readFileAsString( sourceFile ).split( "\n" );
                        if ( sourceFileLines.length >= sourceLine )
                        {
                            String sourceLineContent = sourceFileLines[sourceLine - 1];
                            if ( sourceLineContent.endsWith( "\r" ) ) // remove trailing CR (on Windows)
                            {
                                sourceLineContent = sourceLineContent.substring( 0, sourceLineContent.length() - 1 );
                            }
                            result = new Play2RoutesSourcePosition( sourceLine, sourceLineContent, sourceFile );
                        }
                    }
                }
            }
        }
        return result;
    }

}
