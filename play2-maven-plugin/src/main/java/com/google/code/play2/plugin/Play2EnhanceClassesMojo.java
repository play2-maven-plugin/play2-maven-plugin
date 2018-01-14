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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.codehaus.plexus.util.DirectoryScanner;

import com.google.code.play2.provider.api.Play2JavaEnhancer;
import com.google.code.play2.provider.api.Play2Provider;

import com.google.code.sbt.compiler.api.Analysis;
import com.google.code.sbt.compiler.api.AnalysisProcessor;

/**
 * Enhance Java classes
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2EnhanceClassesMojo
    extends AbstractPlay2EnhanceMojo
{
    private static final String DEFAULT_TEMPLATES_TARGET_DIRECTORY_NAME = "src_managed";

    /**
     * Project classpath.
     */
    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true, required = true )
    private List<String> classpathElements;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        File analysisCacheFile = getAnalysisCacheFile();
        if ( !analysisCacheFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Analysis cache file \"%s\" not found", analysisCacheFile.getAbsolutePath() ) );
        }
        if ( !analysisCacheFile.isFile() )
        {
            throw new MojoExecutionException( String.format( "Analysis cache \"%s\" is not a file", analysisCacheFile.getAbsolutePath() ) );
        }

        List<File> classpathFiles = new ArrayList<File>( classpathElements.size() );
        for ( String path : classpathElements )
        {
            classpathFiles.add( new File( path ) );
        }

        AnalysisProcessor sbtAnalysisProcessor = getSbtAnalysisProcessor();
        Analysis analysis = sbtAnalysisProcessor.readFromFile( analysisCacheFile );

        Play2Provider play2Provider = getProvider();
        Play2JavaEnhancer enhancer = play2Provider.getEnhancer();
        try
        {
            enhancer.setClasspathFiles( classpathFiles );

            File timestampFile = new File( getAnalysisCacheFile().getParentFile(), "play_instrumentation" );
            long lastEnhanced = 0L;
            if ( timestampFile.exists() )
            {
                String line = readFileFirstLine( timestampFile, "ASCII" );
                lastEnhanced = Long.parseLong( line );
            }

            List<String> compileSourceRoots = project.getCompileSourceRoots();

            Set<File> sourcesToGenerageAccessors = null;
            for ( String sourceRoot : compileSourceRoots )
            {
                if ( !sourceRoot.startsWith( project.getBuild().getDirectory() ) ) // unmanaged
                {
                    File scannerBaseDir = new File( sourceRoot );
                    if ( scannerBaseDir.isDirectory() )
                    {
                        DirectoryScanner scanner = new DirectoryScanner();
                        scanner.setBasedir( scannerBaseDir );
                        scanner.setIncludes( new String[] { "**/*.java" } );
                        scanner.addDefaultExcludes();
                        scanner.scan();
                        String[] javaSources = scanner.getIncludedFiles();
                        if ( sourcesToGenerageAccessors == null )
                        {
                            sourcesToGenerageAccessors = toFiles( scannerBaseDir, javaSources );
                        }
                        else
                        {
                            sourcesToGenerageAccessors.addAll( toFiles( scannerBaseDir, javaSources ) );
                        }
                    }
                }
            }
            if ( sourcesToGenerageAccessors == null || sourcesToGenerageAccessors.isEmpty() )
            {
                getLog().info( "No Java classes to enhance" );
                return;
            }

            Set<File> sourcesToRewriteAccess = null;
            if ( playVersion.compareTo( "2.4" ) < 0 )
            {
                // Play 2.1.x, 2.2.x, 2.3.x
                sourcesToRewriteAccess = new HashSet<File>( sourcesToGenerageAccessors );
                
                File targetDirectory = new File( project.getBuild().getDirectory() );
                String templatesOutputDirectoryName = play2Provider.getTemplatesCompiler().getCustomOutputDirectoryName();
                if ( templatesOutputDirectoryName == null )
                {
                    templatesOutputDirectoryName = DEFAULT_TEMPLATES_TARGET_DIRECTORY_NAME;
                }
                File scannerBaseDir = new File( targetDirectory, templatesOutputDirectoryName + "/main" );
                if ( scannerBaseDir.isDirectory() )
                {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir( scannerBaseDir );
                    scanner.setIncludes( new String[] { "**/*.template.scala" } );
                    scanner.addDefaultExcludes();
                    scanner.scan();
                    String[] scalaTemplateSources = scanner.getIncludedFiles();
                    sourcesToRewriteAccess.addAll( toFiles( scannerBaseDir, scalaTemplateSources ) );
                }
            }
            else
            {
                // Play 2.4.x
                for ( String sourceRoot : compileSourceRoots )
                {
                    File scannerBaseDir = new File( sourceRoot );
                    if ( scannerBaseDir.isDirectory() )
                    {
                        DirectoryScanner scanner = new DirectoryScanner();
                        scanner.setBasedir( scannerBaseDir );
                        scanner.setIncludes( new String[] { "**/*.java", "**/*.scala" } );
                        scanner.addDefaultExcludes();
                        scanner.scan();
                        String[] files = scanner.getIncludedFiles();
                        if ( sourcesToRewriteAccess == null )
                        {
                            sourcesToRewriteAccess = toFiles( scannerBaseDir, files );
                        }
                        else
                        {
                            sourcesToRewriteAccess.addAll( toFiles( scannerBaseDir, files ) );
                        }
                    }
                }
            }

            boolean accessorsGeneratedInJavaClasses = generateAccessors( sourcesToGenerageAccessors, lastEnhanced, analysis, enhancer );
            boolean accessRewritten = false;
            if ( sourcesToRewriteAccess != null )
            {
                accessRewritten = rewriteAccess( sourcesToRewriteAccess, lastEnhanced, analysis, enhancer );
            }

            if ( accessorsGeneratedInJavaClasses || accessRewritten )
            {
                if ( analysis != null )
                {
                    analysis.writeToFile( analysisCacheFile );
                }
                writeToFile( timestampFile, "ASCII", Long.toString( System.currentTimeMillis() ) );
            }
        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Enhancement exception", e );
        }
    }

    private Set<File> toFiles( File baseDir, String[] fileNames )
    {
        Set<File> result = new HashSet<File>( fileNames.length );
        for ( String fileName : fileNames )
        {
            result.add( new File( baseDir, fileName ) );
        }
        return result;
    }

    private boolean generateAccessors( Set<File> javaSources, long lastEnhanced, Analysis analysis, Play2JavaEnhancer enhancer ) throws Exception
    {
        int processedFiles = 0;
        int enhancedFiles = 0;

        if ( javaSources.size() > 0 )
        {
            for ( File sourceFile : javaSources )
            {
                if ( analysis.getCompilationTime( sourceFile ) > lastEnhanced )
                {
                    Set<File> javaClasses = analysis.getProducts( sourceFile );
                    for ( File classFile : javaClasses )
                    {
                        processedFiles++;
                        if ( enhancer.generateAccessors( classFile ) || enhancer.rewriteAccess( classFile ) )
                        {
                            enhancedFiles++;
                            getLog().debug( String.format( "\"%s\" enhanced", classFile.getPath() ) );
                            //if ( sbtAnalysisProcessor.areClassFileTimestampsSupported() )
                            //{
                                analysis.updateClassFileTimestamp( classFile );
                            //}
                        }
                        else
                        {
                            getLog().debug( String.format( "\"%s\" skipped", classFile.getPath() ) );
                        }
                    }
                }
            }
        }

        if ( processedFiles > 0 )
        {
            getLog().info( String.format( "Generate accessors - %d Java %s processed, %d enhanced",
                                          Integer.valueOf( processedFiles ), processedFiles > 1 ? "classes" : "class",
                                          Integer.valueOf( enhancedFiles ) ) );
        }
        else
        {
            getLog().info( "Generate accessors - no Java classes to process" );
        }

        return enhancedFiles > 0;
    }

    private boolean rewriteAccess( Set<File> sourceFilesToEnhance, long lastEnhanced, Analysis analysis, Play2JavaEnhancer enhancer ) throws Exception
    {
        int processedFiles = 0;
        int enhancedFiles = 0;

        if ( sourceFilesToEnhance.size() > 0 )
        {
            for ( File sourceFile : sourceFilesToEnhance )
            {
                if ( analysis.getCompilationTime( sourceFile ) > lastEnhanced )
                {
                    Set<File> templateClasses = analysis.getProducts( sourceFile );
                    for ( File classFile : templateClasses )
                    {
                        processedFiles++;
                        if ( enhancer.rewriteAccess( classFile ) )
                        {
                            enhancedFiles++;
                            getLog().debug( String.format( "\"%s\" enhanced", classFile.getPath() ) );
                            analysis.updateClassFileTimestamp( classFile );
                        }
                        else
                        {
                            getLog().debug( String.format( "\"%s\" skipped", classFile.getPath() ) );
                        }
                    }
                }
            }
        }

        if ( processedFiles > 0 )
        {
            getLog().info( String.format( "Rewrite access - %d %s processed, %d enhanced",
                                          Integer.valueOf( processedFiles ), processedFiles > 1 ? "classes" : "class",
                                          Integer.valueOf( enhancedFiles ) ) );
        }
        else
        {
            getLog().info( "Rewrite access - no classes to process" );
        }
        return enhancedFiles > 0;
    }

}
