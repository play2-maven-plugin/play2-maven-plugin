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

package com.google.code.play2.provider.play25;

import java.io.File;
import java.util.Arrays;

import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.util.Either;

import play.routes.compiler.InjectedRoutesGenerator$;
import play.routes.compiler.RoutesCompilationError;
import play.routes.compiler.RoutesCompiler;
import play.routes.compiler.RoutesCompiler$;
import play.routes.compiler.RoutesGenerator;
import play.routes.compiler.StaticRoutesGenerator$;

import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.RoutesCompilationException;

public class Play25RoutesCompiler
    implements Play2RoutesCompiler
{
    private static final String[] javaAdditionalImports = new String[] { "controllers.Assets.Asset", "play.libs.F" };

    private static final String[] scalaAdditionalImports = new String[] { "controllers.Assets.Asset" };

    private static final String[] supportedGenerators = new String[] { "injected", "static" };

    private String mainLang;

    private File outputDirectory;

    private String generator;

    @Override
    public String getCustomOutputDirectoryName()
    {
        return "routes";
    }

    @Override
    public String getDefaultNamespace()
    {
        return "router";
    }

    @Override
    public String getMainRoutesFileName()
    {
        return "Routes.scala";
    }

    @Override
    public String[] getSupportedGenerators()
    {
        return supportedGenerators;
    }

    @Override
    public void setMainLang( String mainLang )
    {
        this.mainLang = mainLang;
    }

    @Override
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setGenerator( String generator )
    {
        if ( Arrays.asList( supportedGenerators ).contains( generator ) )
        {
            this.generator = generator;
        }
    }

    @Override
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

        RoutesGenerator routesGenerator = InjectedRoutesGenerator$.MODULE$;
        if ( "static".equals( generator ) )
        {
            routesGenerator = StaticRoutesGenerator$.MODULE$;
        }
        RoutesCompiler.RoutesCompilerTask routesCompilerTask =
            new RoutesCompiler.RoutesCompilerTask( routesFile,
                                                   JavaConversions.asScalaBuffer( Arrays.asList( additionalImports ) ),
                                                   true, true, false ); // TODO - should be parametrizable in the future
        Either<Seq<RoutesCompilationError>, Seq<File>> result =
            RoutesCompiler$.MODULE$.compile( routesCompilerTask, routesGenerator, outputDirectory );
        if ( result.isLeft() )
        {
            RoutesCompilationError e = result.left().get().apply( 0 );
            throw new RoutesCompilationException( e.source(), e.message(), (Integer) e.line().get(),
                                                  (Integer) e.column().get() );
        }
    }

}
