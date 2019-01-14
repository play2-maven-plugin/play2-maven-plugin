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

package com.google.code.play2.provider.play22;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import scala.Option;

import play.templates.ScalaTemplateCompiler;
import play.templates.TemplateCompilationError;

import com.google.code.play2.provider.api.Play2TemplateCompiler;
import com.google.code.play2.provider.api.TemplateCompilationException;

public class Play22TemplateCompiler
    implements Play2TemplateCompiler
{
    private static final String[] templateExts = {
        "html",
        "txt",
        "xml",
        "js" };

    private static final String[] formatterTypes = {
        "play.api.templates.HtmlFormat",
        "play.api.templates.TxtFormat",
        "play.api.templates.XmlFormat",
        "play.api.templates.JavaScriptFormat" };

    private static final String[] javaAdditionalImports = new String[] {
        "play.api.templates._",
        "play.api.templates.PlayMagic._",
        "models._",
        "controllers._",
        "java.lang._",
        "java.util._",
        "scala.collection.JavaConversions._",
        "scala.collection.JavaConverters._",
        "play.api.i18n._",
        "play.core.j.PlayMagicForJava._",
        "play.mvc._",
        "play.data._",
        "play.api.data.Field",
        "play.mvc.Http.Context.Implicit._",
        "views.%format%._" };

    private static final String[] scalaAdditionalImports = new String[] {
        "play.api.templates._",
        "play.api.templates.PlayMagic._",
        "models._",
        "controllers._",
        "play.api.i18n._",
        "play.api.mvc._",
        "play.api.data._",
        "views.%format%._" };

    private File sourceDirectory;

    private File outputDirectory;

    private List<String> additionalImports;

    @Override
    public String getCustomOutputDirectoryName()
    {
        return null;
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
            String importsAsString = getImportsAsString( ext );
            try
            {
                Option<File> resultOption =
                    ScalaTemplateCompiler.compile( templateFile, sourceDirectory, outputDirectory, formatterType,
                                                   importsAsString );
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

    private String getImportsAsString( String format )
    {
        StringBuilder sb = new StringBuilder();
        for ( String additionalImport : additionalImports )
        {
            sb.append( "import " ).append( additionalImport.replace( "%format%", format ) ).append( "\n" );
        }
        return sb.substring( 0, sb.length() - 1 );
    }

}
