/*
 * Copyright 2013 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.codehaus.plexus.util.DirectoryScanner;

import play.templates.ScalaTemplateCompiler;

/**
 * ...
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "template-compile", requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2ScalaTemplateCompileMojo
    extends AbstractPlay2Mojo
{

    private final static String appDirectoryName = "app";

    private final static String targetDirectoryName = "src_managed/main";

    private final static String[] scalaTemplatesIncludes = new String[] { "**/*.scala.*" };

    private final static String[] resultTypes = { "play.api.templates.Html", "play.api.templates.Txt",
        "play.api.templates.Xml" };

    private final static String[] formatterTypes = { "play.api.templates.HtmlFormat", "play.api.templates.TxtFormat",
        "play.api.templates.XmlFormat" };

    private final static String[] javaAdditionalImports = new String[] { "play.api.templates._",
        "play.api.templates.PlayMagic._",

        "models._", "controllers._",

        "java.lang._", "java.util._",

        "scala.collection.JavaConversions._", "scala.collection.JavaConverters._",

        "play.api.i18n._",
        // "play.api.templates.PlayMagicForJava._", // Play! 2.0.x
        "play.core.j.PlayMagicForJava._",

        "play.mvc._", "play.data._", "play.api.data.Field",
        // "com.avaje.ebean._", // Play! 2.0.x

        "play.mvc.Http.Context.Implicit._",

        "views.%format%._" };

    private final static String[] scalaAdditionalImports = new String[] { "play.api.templates._",
        "play.api.templates.PlayMagic._",

        "models._", "controllers._",

        "play.api.i18n._",

        "play.api.mvc._", "play.api.data._",

        "views.%format%._" };

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File basedir = project.getBasedir();
        File appDirectory = new File( basedir, appDirectoryName );

        File targetDirectory = new File( project.getBuild().getDirectory() );
        File generatedDirectory = new File( targetDirectory, targetDirectoryName );

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( appDirectory );
        scanner.setIncludes( scalaTemplatesIncludes );
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        if ( files.length > 0 )
        {
            for ( String fileName : files )
            {
                getLog().debug( String.format( "Processing template \"%s\"", fileName ) );
                File templateFile = new File( appDirectory, fileName );
                String ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
                String importsAsString = getImportsAsString( ext );
                int index = -1;
                if ( "html".equals( ext ) )
                {
                    index = 0;
                }
                else if ( "txt".equals( ext ) )
                {
                    index = 1;
                }
                if ( "xml".equals( ext ) )
                {
                    index = 2;
                }
                if ( index >= 0 )
                {
                    String resultType = resultTypes[index];
                    String formatterType = formatterTypes[index];
                    ScalaTemplateCompiler.compile( templateFile, appDirectory, generatedDirectory, resultType,
                                                   formatterType, importsAsString );
                }
            }

            if ( !project.getCompileSourceRoots().contains( generatedDirectory.getAbsolutePath() ) )
            {
                project.addCompileSourceRoot( generatedDirectory.getAbsolutePath() );
                getLog().debug( "Added source directory: " + generatedDirectory.getAbsolutePath() );
            }
        }
    }

    private String getImportsAsString( String format )
    {
        String mainLang = getMainLang();
        String[] additionalImports = {};
        if ( "java".equalsIgnoreCase( mainLang ) )
        {
            additionalImports = javaAdditionalImports;
        }
        else if ( "scala".equalsIgnoreCase( mainLang ) )
        {
            additionalImports = scalaAdditionalImports;
        }
        StringBuilder sb = new StringBuilder();
        for ( String additionalImport : additionalImports )
        {
            sb.append( "import " ).append( additionalImport.replace( "%format%", format ) ).append( ";\n" );
        }
        return sb.toString();
    }

}
