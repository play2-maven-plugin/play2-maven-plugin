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

package com.google.code.play2.spm.template;

import java.io.File;
import java.io.IOException;

import com.google.code.sbt.compiler.api.SourcePosition;
import com.google.code.play2.spm.AbstractPlay2SourcePositionMapper;

/**
 * Play&#33; Framework template source position mapper
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class Play2TemplateSourcePositionMapper extends AbstractPlay2SourcePositionMapper
{
    private static final String META_SEPARATOR = "-- GENERATED --";

    /**
     * Performs mapping from the position in generated source file to the position in Twirl template file it was
     * generated from.<br>
     * <br>
     * Returns:
     * <ul>
     * <li>position in Twirl template file</li>
     * <li>{@code null} value if file of the input position is not recognized as generated from Twirl template file</li>
     * </ul>
     *
     * @param p compilation error/warning position in generated file
     * @return position in {@code routes} file or {@code null} value if file of the input position is not recognized as
     *         generated from Twirl template file
     * @throws IOException I/O exception during generated or original source file reading
     */
    @Override
    public SourcePosition map( SourcePosition p ) throws IOException
    {
        SourcePosition result = null;

        File generatedFile = p.getFile();
        int generatedOffset = p.getOffset();
        if ( generatedFile != null && generatedFile.isFile() && generatedOffset >= 0 )
        {
            Play2TemplateGeneratedSource generated = getGeneratedSource( generatedFile );
            if ( generated != null )
            {
                String sourceFileName = generated.getSourceFileName();
                if ( sourceFileName != null )
                {
                    File sourceFile = new File( sourceFileName );
                    if ( sourceFile.isFile() )
                    {
                        int sourceOffset = generated.mapPosition( generatedOffset );
                        String sourceFileContent = readFileAsString( sourceFile );
                        if ( sourceFileContent.length() > sourceOffset )
                        {
                            String[] sourceFileLines = sourceFileContent.split( "\n" );
                            Play2TemplateLocation sourceLocation =
                                new Play2TemplateMapping( sourceFileLines ).location( sourceOffset );
                            //if ( sourceLocation != null )
                            //{
                                result = new Play2TemplateSourcePosition( sourceFile, sourceLocation );
                            //}
                        }
                    }
                }
            }
        }
        return result;
    }

    public Play2TemplateGeneratedSource getGeneratedSource( File generatedFile ) throws IOException
    {
        Play2TemplateGeneratedSource result = null;

        String generatedFileContent = readFileAsString( generatedFile );
        String[] fileSections = generatedFileContent.split( META_SEPARATOR );
        if ( fileSections.length > 1 )
        {
            String[] metaSectionLines = fileSections[1].trim().split( "\n" );
            if ( metaSectionLines.length > 0 )
            {
                result = new Play2TemplateGeneratedSource( metaSectionLines );
            }
        }
        return result;
    }

}
