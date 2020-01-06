/*
 * Copyright 2013-2020 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider.play27;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import scala.Option;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.io.Codec;

import play.TemplateImports;

import play.twirl.compiler.TemplateCompilationError;
import play.twirl.compiler.TwirlCompiler;

import com.google.code.play2.provider.api.Play2TemplateCompiler;
import com.google.code.play2.provider.api.TemplateCompilationException;

public class Play27TemplateCompiler
    implements Play2TemplateCompiler
{
    private static final String[] templateExts = {
        "html",
        "txt",
        "xml",
        "js" };

    private static final String[] formatterTypes = {
        "play.twirl.api.HtmlFormat",
        "play.twirl.api.TxtFormat",
        "play.twirl.api.XmlFormat",
        "play.twirl.api.JavaScriptFormat" };

    private static final String[] constructorAnnotations = {
        "@javax.inject.Inject()"
    };

    private File sourceDirectory;

    private File outputDirectory;

    private List<String> additionalImports;

    @Override
    public String getCustomOutputDirectoryName()
    {
        return "twirl";
    }

    @Override
    public void setSourceDirectory( File sourceDirectory )
    {
        this.sourceDirectory = sourceDirectory;
    }

    @Override
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public List<String> getDefaultJavaImports()
    {
        return getDefaultImports( TemplateImports.minimalJavaTemplateImports );
    }

    @Override
    public List<String> getDefaultScalaImports()
    {
        return getDefaultImports( TemplateImports.defaultScalaTemplateImports );
    }

    private List<String> getDefaultImports( List<String> playImports )
    {
        List<String> twirlDefaultImports = JavaConverters.seqAsJavaList( TwirlCompiler.DefaultImports() );
        List<String> defaultImports = new ArrayList<String>( twirlDefaultImports.size() + playImports.size() );
        defaultImports.addAll( twirlDefaultImports );
        defaultImports.addAll( playImports );
        return Collections.unmodifiableList( defaultImports );
    }

    @Override
    public void setAdditionalImports( List<String> additionalImports )
    {
        this.additionalImports = additionalImports;
    }

    @Override
    public File compile( File templateFile )
        throws TemplateCompilationException
    {
        File result = null;

        String fileName = templateFile.getName();
        String ext = fileName.substring( fileName.lastIndexOf( '.' ) + 1 );
        int index = getTemplateExtIndex( ext );
        if ( index >= 0 )
        {
            String formatterType = formatterTypes[index];
            Seq<String> additionalImportsSeq = getAdditionalImports( ext );
            Seq<String> constructorAnnotationsSeq =
                JavaConverters.asScalaBuffer( Arrays.asList( constructorAnnotations ) ).toSeq();
            try
            {
                Option<File> resultOption =
                    TwirlCompiler.compile( templateFile, sourceDirectory, outputDirectory, formatterType,
                                           additionalImportsSeq, constructorAnnotationsSeq,
                                           Codec.apply( "UTF-8" )/* codec */, false/* inclusiveDot */ );
                result = resultOption.isDefined() ? resultOption.get() : null;
            }
            catch ( TemplateCompilationError e )
            {
                throw new TemplateCompilationException( e.source(), e.message(), e.line(), e.column() );
            }
        }
        return result;
    }

    private int getTemplateExtIndex( String ext )
    {
        int result = -1;
        for ( int i = 0; i < templateExts.length; i++ )
        {
            if ( templateExts[i].equals( ext ) )
            {
                result = i;
                break;
            }
        }
        return result;
    }

    private Seq<String> getAdditionalImports( String format )
    {
        List<String> formattedAdditionalImports = new ArrayList<String>( additionalImports.size() );
        for ( String additionalImport : additionalImports )
        {
            formattedAdditionalImports.add( additionalImport.replace( "%format%", format ) );
        }
        return JavaConverters.asScalaBuffer( formattedAdditionalImports ).toSeq();
    }

}
