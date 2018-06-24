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

package com.google.code.play2.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.provider.api.SourceGenerationException;

/**
 * Source generator base class for Play! mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2SourceGeneratorMojo
    extends AbstractPlay2Mojo
{
    /**
     * Source files encoding.
     * <br>
     * <br>
     * If not specified, the encoding value will be the value of the {@code file.encoding} system property.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "project.build.sourceEncoding" )
    protected String sourceEncoding;

    /**
     * For M2E integration.
     */
    @Component
    protected BuildContext buildContext;

    protected static final String DEFAULT_TARGET_DIRECTORY_NAME = "src_managed";

    protected void addSourceRoot( File generatedDirectory )
    {
        if ( !project.getCompileSourceRoots().contains( generatedDirectory.getAbsolutePath() ) )
        {
            project.addCompileSourceRoot( generatedDirectory.getAbsolutePath() );
            getLog().debug( "Added source directory: " + generatedDirectory.getAbsolutePath() );
        }
    }

    protected void configureSourcePositionMappers()
    {
        String sourcePositionMappersGAV = String.format( "%s:%s:%s", pluginGroupId, "play2-source-position-mappers", pluginVersion );
        project.getProperties().setProperty( "sbt._sourcePositionMappers", sourcePositionMappersGAV );
    }

    protected void reportCompilationProblems( File source, SourceGenerationException e )
    {
        if ( e.line() > 0 )
        {
            getLog().error( String.format( "%s:%d: %s", source.getAbsolutePath(), Integer.valueOf( e.line() ),
                                           e.getMessage() ) );
            String lineContent = readFileNthLine( source, e.line() - 1, "unknown" );
            if ( lineContent != null )
            {
                getLog().error( lineContent );
                if ( e.position() > 0 )
                {
                    int pointerSpaceLength = Math.min( e.position() - 1, lineContent.length() );
                    char[] pointerLine = new char[ pointerSpaceLength + 1 ];
                    for ( int i = 0; i < pointerSpaceLength; i++ )
                    {
                        pointerLine[ i ] = lineContent.charAt( i ) == '\t' ? '\t' : ' ';
                    }
                    pointerLine[ pointerSpaceLength ] = '^';
                    getLog().error( String.valueOf( pointerLine ) );
                }
            }
        }
        else
        {
            getLog().error( String.format( "%s: %s", source.getAbsolutePath(), e.getMessage() /* message */ ) );
        }
    }

    private String readFileNthLine( File file, int lineNo, String defaultValue )
    {
        String result = null;
        try
        {
            BufferedReader is = createBufferedFileReader( file, sourceEncoding );
            try
            {
                int i = 0;
                while ( i <= lineNo )
                {
                    result = is.readLine();
                    i++;
                }
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException e )
        {
            result = defaultValue;
        }
        return result;
    }

}
