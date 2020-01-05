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

package com.google.code.play2.provider.play28;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.util.Either;

import play.routes.compiler.InjectedRoutesGenerator$;
import play.routes.compiler.RoutesCompilationError;
import play.routes.compiler.RoutesCompiler;
import play.routes.compiler.RoutesGenerator;
//import play.routes.compiler.StaticRoutesGenerator$;

import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.RoutesCompilationException;

public class Play28RoutesCompiler
    implements Play2RoutesCompiler
{
    private static final String[] javaAdditionalImports = new String[] { "controllers.Assets.Asset", "play.libs.F" };

    private static final String[] scalaAdditionalImports = new String[] { "controllers.Assets.Asset" };

    private static final String[] supportedGenerators = new String[] { "injected", "static" };

    private File outputDirectory;

    private String generator;

    private List<String> additionalImports;

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
        if ( Arrays.asList( supportedGenerators ).contains( generator ) )
        {
            this.generator = generator;
        }
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
        RoutesGenerator routesGenerator = InjectedRoutesGenerator$.MODULE$;
        if ( "static".equals( generator ) )
        {
            //TODO warning, that static router is not supported anymore
            //routesGenerator = StaticRoutesGenerator$.MODULE$;
        }
        RoutesCompiler.RoutesCompilerTask routesCompilerTask =
            new RoutesCompiler.RoutesCompilerTask( routesFile, JavaConversions.asScalaBuffer( additionalImports ),
                                                   true, true, false ); // TODO - should be parametrizable in the future
        Either<Seq<RoutesCompilationError>, Seq<File>> result =
            RoutesCompiler.compile( routesCompilerTask, routesGenerator, outputDirectory );
        if ( result.isLeft() )
        {
            RoutesCompilationError e = result.left().get().apply( 0 );
            throw new RoutesCompilationException( e.source(), e.message(), (Integer) e.line().get(),
                                                  (Integer) e.column().get() );
        }
    }

}
