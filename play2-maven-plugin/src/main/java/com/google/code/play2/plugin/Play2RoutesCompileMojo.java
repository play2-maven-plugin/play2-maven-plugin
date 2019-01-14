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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.codehaus.plexus.util.DirectoryScanner;

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
    extends AbstractPlay2SourceGeneratorMojo
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
     * @deprecated
     */
    @Parameter( property = "play2.confDirectory", readonly = true )
    private File confDirectory;

    /**
     * Routes generator type ("static" or "injected").
     * <br>
     * <br>
     * Supported by Play! 2.4.x and later. If not set, provider default generator type will be used.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.routesGenerator" )
    private String routesGenerator;

    /**
     * Additional imports for the router.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.routesAdditionalImports" )
    private String routesAdditionalImports;

    private static final String[] ROUTES_INCLUDES = new String[] { "*.routes", "routes" };

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( confDirectory != null )
        {
            getLog().warn( "\"confDirectory\" plugin configuration parameter is deprecated. "
                + "Plugin looks for routes files in all resource directories." );
        }

        if ( !"java".equals( mainLang ) && !"scala".equals( mainLang ) )
        {
            throw new MojoExecutionException(
                                              String.format( "Routes compilation failed  - unsupported <mainLang> configuration parameter value \"%s\"",
                                                             mainLang ) );
        }

        int routeFilesCount = 0;
        for ( Resource resource: project.getBuild().getResources() )
        {
            if ( !"public".equals( resource.getTargetPath() ) ) // exclude web assets
            {
                File directory = new File( resource.getDirectory() );
                if ( directory.isDirectory() )
                {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir( directory );
                    scanner.setIncludes( ROUTES_INCLUDES );
                    scanner.addDefaultExcludes();
                    scanner.scan();
                    String[] files = scanner.getIncludedFiles();
                    if ( files.length > 0 )
                    {
                        routeFilesCount += files.length;
                        compileRoutes( directory, files );
                    }
                }
            }
        }

        if ( routeFilesCount == 0 )
        {
            getLog().info( "No routers to compile" );
        }
    }

    private void compileRoutes( File resourceDirectory, String[] files )
            throws MojoExecutionException, MojoFailureException
    {
        Play2Provider play2Provider = getProvider();
        Play2RoutesCompiler compiler = play2Provider.getRoutesCompiler();

        File targetDirectory = new File( project.getBuild().getDirectory() );
        String outputDirectoryName = compiler.getCustomOutputDirectoryName();
        if ( outputDirectoryName == null )
        {
            outputDirectoryName = DEFAULT_TARGET_DIRECTORY_NAME;
        }
        File generatedDirectory = new File( targetDirectory, outputDirectoryName + "/main" );
        compiler.setOutputDirectory( generatedDirectory );

        String[] supportedGenerators = compiler.getSupportedGenerators();
        if ( routesGenerator != null && !routesGenerator.isEmpty() )
        {
            if ( Arrays.asList( supportedGenerators ).contains( routesGenerator ) )
            {
                compiler.setGenerator( routesGenerator );
                getLog().info( String.format( "Generating %s router", routesGenerator ) );
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                for ( String supportedGenerator : supportedGenerators )
                {
                    sb.append( ", \"" );
                    sb.append( supportedGenerator );
                    sb.append( '\"' );
                }
                String supportedGeneratorsStr = sb.substring( 2 );
                String msg =
                    String.format( "\"%s\" router generator not supported. Supported generators: %s.", routesGenerator,
                                   supportedGeneratorsStr );
                throw new MojoExecutionException( msg );
            }
        }
        else
        {
            getLog().info( String.format( "Generating %s router", supportedGenerators[0] ) ); // default
        }

        List<String> resolvedAdditionalImports = Collections.emptyList();
        if ( "java".equalsIgnoreCase( mainLang ) )
        {
            resolvedAdditionalImports = compiler.getDefaultJavaImports();
        }
        else if ( "scala".equalsIgnoreCase( mainLang ) )
        {
            resolvedAdditionalImports = compiler.getDefaultScalaImports();
        }
        if ( routesAdditionalImports != null && !"".equals( routesAdditionalImports ) )
        {
            resolvedAdditionalImports = new ArrayList<String>( resolvedAdditionalImports ); // mutable list
            String[] additionalImports = routesAdditionalImports.split( "[ \\r\\n]+" );
            resolvedAdditionalImports.addAll( Arrays.asList( additionalImports ) );
        }
        compiler.setAdditionalImports( resolvedAdditionalImports );

        String defaultNamespace = compiler.getDefaultNamespace();
        String mainRoutesFileName = compiler.getMainRoutesFileName();

        int compiledFiles = 0;
        RoutesCompilationException firstException = null;

        for ( String fileName : files )
        {
            File routesFile = new File( resourceDirectory, fileName );
            String generatedFileName = getGeneratedFileName( fileName, defaultNamespace, mainRoutesFileName );
            File generatedFile = new File( generatedDirectory, generatedFileName );
            boolean modified = true;
            if ( generatedFile.isFile() )
            {
                modified = generatedFile.lastModified() < routesFile.lastModified();
            }

            if ( modified )
            {
                try
                {
                    compiler.compile( routesFile );
                    compiledFiles++;
                    buildContextRefresh( generatedDirectory, generatedFileName );
                    getLog().debug( String.format( "\"%s\" processed", fileName ) );
                }
                catch ( RoutesCompilationException e )
                {
                    if ( firstException == null )
                    {
                        firstException = e;
                    }
                    reportCompilationProblems( routesFile, e );
                }
            }
            else
            {
                getLog().debug( String.format( "\"%s\" skipped - no changes", fileName ) );
            }
        }

        if ( firstException != null )
        {
            throw new MojoFailureException( "Routers compilation failed", firstException );
        }

        getLog().info( String.format( "%d router%s processed, %d compiled", Integer.valueOf( files.length ),
                                      files.length > 1 ? "s" : "", Integer.valueOf( compiledFiles ) ) );
        addSourceRoot( generatedDirectory );
        configureSourcePositionMappers();
    }

    private String getGeneratedFileName( String routesFileName, String defaultNamespace, String mainRoutesFileName )
    {
        String namespace = defaultNamespace;
        if ( routesFileName.endsWith( ".routes" ) )
        {
            namespace = routesFileName.substring( 0, routesFileName.length() - ".routes".length() );
        }

        String result = mainRoutesFileName;
        if ( namespace != null )
        {
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
