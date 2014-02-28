/*
 * Copyright 2013-2014 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play22;

import java.io.File;
import java.util.Arrays;

import scala.collection.JavaConversions;

import play.router.RoutesCompiler$;
import play.router.RoutesCompiler.RoutesCompilationError;

import com.google.code.play2.provider.Play2RoutesCompiler;
import com.google.code.play2.provider.RoutesCompilationException;

public class Play22RoutesCompiler
    implements Play2RoutesCompiler
{
    private static final String[] javaAdditionalImports = new String[] { "play.libs.F" };

    private static final String[] scalaAdditionalImports = new String[] {};

    private String mainLang;

    private File outputDirectory;

    public void setMainLang( String mainLang )
    {
        this.mainLang = mainLang;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void compile( File routesFile )
        throws RoutesCompilationException
    {
        String[] additionalImports = {};
        if ( "java".equalsIgnoreCase( mainLang ) )
        {
            additionalImports = javaAdditionalImports;
        }
        else if ( "scala".equalsIgnoreCase( mainLang ) )
        {
            additionalImports = scalaAdditionalImports;
        }

        try
        {
            RoutesCompiler$.MODULE$.compile( routesFile, outputDirectory,
                                             JavaConversions.asScalaBuffer( Arrays.asList( additionalImports ) ), true,
                                             false );
        }
        catch ( RoutesCompilationError e )
        {
            throw new RoutesCompilationException( e.source(), e.message(), (Integer) e.line().get(),
                                                  (Integer) e.column().get() );
        }
    }

}
