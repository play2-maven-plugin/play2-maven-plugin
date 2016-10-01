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

package com.google.code.play2.provider.play21;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import scala.collection.JavaConversions;

import play.router.RoutesCompiler;

import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.RoutesCompilationException;

public class Play21RoutesCompiler
    implements Play2RoutesCompiler
{
    private static final String[] javaAdditionalImports = new String[] { "play.libs.F" };

    private static final String[] scalaAdditionalImports = new String[] {};

    private static final String[] supportedGenerators = new String[] { "static" };

    private File outputDirectory;

    private List<String> additionalImports;

    @Override
    public String getCustomOutputDirectoryName()
    {
        return null;
    }

    @Override
    public String getDefaultNamespace()
    {
        return null;
    }

    @Override
    public String getMainRoutesFileName()
    {
        return "routes_routing.scala";
    }

    @Override
    public String[] getSupportedGenerators()
    {
        return supportedGenerators;
    }

    @Override
    public List<String> getDefaultJavaImports()
    {
        return Arrays.asList( javaAdditionalImports );
    }

    @Override
    public List<String> getDefaultScalaImports()
    {
        return Arrays.asList( scalaAdditionalImports );
    }

    @Override
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setGenerator( String generator )
    {
        // Not supported
    }

    @Override
    public void setAdditionalImports( List<String> additionalImports )
    {
        this.additionalImports = additionalImports;
    }

    @Override
    public void compile( File routesFile )
        throws RoutesCompilationException
    {
        try
        {
            RoutesCompiler.compile( routesFile, outputDirectory, JavaConversions.asScalaBuffer( additionalImports ) );
        }
        catch ( RoutesCompiler.RoutesCompilationError e )
        {
            throw new RoutesCompilationException( e.source(), e.message(), (Integer) e.line().get(),
                                                  (Integer) e.column().get() );
        }
    }

}
