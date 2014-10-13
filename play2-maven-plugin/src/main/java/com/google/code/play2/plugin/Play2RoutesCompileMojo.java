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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.codehaus.plexus.util.DirectoryScanner;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.RoutesCompilationException;

/**
 * Compile routes
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "routes-compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class Play2RoutesCompileMojo
    extends AbstractPlay2Mojo
{

    /**
     * Main language ("scala" or "java").
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.mainLang", required = true, defaultValue = "scala" )
    private String mainLang;

    /**
     * The "conf" directory.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.confDirectory", readonly = true, defaultValue = "${project.basedir}/conf" )
    private File confDirectory;

    /**
     * For M2E integration.
     */
    @Component
    private BuildContext buildContext;

    private static final String targetDirectoryName = "src_managed/main";

    private static final String[] routesIncludes = new String[] { "*.routes", "routes" };

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( !"java".equals( mainLang ) && !"scala".equals( mainLang ) )
        {
            throw new MojoExecutionException(
                                              String.format( "Routes compilation failed  - unsupported <mainLang> configuration parameter value \"%s\"",
                                                             mainLang ) );
        }

        if ( !confDirectory.isDirectory() )
        {
            getLog().info( "No routes to compile" );
            return;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( confDirectory );
        scanner.setIncludes( routesIncludes );
        scanner.addDefaultExcludes();
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        if ( files.length == 0 )
        {
            getLog().debug( "No routes to compile" );
            return;
        }

        File targetDirectory = new File( project.getBuild().getDirectory() );
        File generatedDirectory = new File( targetDirectory, targetDirectoryName );

        Play2Provider play2Provider = getProvider();
        Play2RoutesCompiler compiler = play2Provider.getRoutesCompiler();
        compiler.setMainLang( mainLang );
        compiler.setOutputDirectory( generatedDirectory );

        for ( String fileName : files )
        {
            File routesFile = new File( confDirectory, fileName );
            String generatedFileName = getGeneratedFileName( fileName );
            File generatedFile = new File( generatedDirectory, generatedFileName );
            boolean modified = true;
            if ( generatedFile.isFile() )
            {
                modified = ( generatedFile.lastModified() < routesFile.lastModified() );
            }

            if ( modified )
            {
                try
                {
                    compiler.compile( routesFile );
                    buildContextRefresh( generatedDirectory, generatedFileName );
                    getLog().debug( String.format( "\"%s\" processed", fileName ) );
                }
                catch ( RoutesCompilationException e )
                {
                    throw new MojoExecutionException( String.format( "Routes compilation failed (%s)",
                                                                     routesFile.getPath() ), e );
                }
            }
            else
            {
                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
            }
        }

        if ( !project.getCompileSourceRoots().contains( generatedDirectory.getAbsolutePath() ) )
        {
            project.addCompileSourceRoot( generatedDirectory.getAbsolutePath() );
            getLog().debug( "Added source directory: " + generatedDirectory.getAbsolutePath() );
        }
    }

    private String getGeneratedFileName( String routesFileName )
    {
        String result = "routes_routing.scala";
        if ( routesFileName.endsWith( ".routes" ) )
        {
            String namespace = routesFileName.substring( 0, routesFileName.length() - ".routes".length() );
            String packageDir = namespace.replace( '.', File.separatorChar );
            result = packageDir + File.separatorChar + result;
        }

        return result;
    }

    private void buildContextRefresh( File generatedDirectory, String generatedFileName )
    {
        File fileTargetDir = new File( generatedDirectory, generatedFileName ).getParentFile();
        File generatedFile = new File( fileTargetDir, "routes_routing.scala" );
        buildContext.refresh( generatedFile );
        generatedFile = new File( fileTargetDir, "routes_reverseRouting.scala" );
        if ( generatedFile.exists() )
        {
            buildContext.refresh( generatedFile );
        }
        generatedFile = new File( generatedDirectory, "controllers/routes.java" );
        buildContext.refresh( generatedFile );
    }

}
