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

package com.google.code.play2.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import com.typesafe.config.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.code.play2.provider.api.Play2EbeanEnhancer;
import com.google.code.play2.provider.api.Play2Provider;

import com.google.code.sbt.compiler.api.Analysis;
import com.google.code.sbt.compiler.api.AnalysisProcessor;

/**
 * Ebean enhance
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "ebean-enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2EbeanEnhanceMojo
    extends AbstractPlay2EnhanceMojo
{
    /**
     * Project classpath.
     */
    @Parameter( property = "project.compileClasspathElements", readonly = true, required = true )
    private List<String> classpathElements;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        Config config = getPlayConfiguration().resolve();
        String models  = getEBeanModelsToEnhance( config );

        File outputDirectory = new File( project.getBuild().getOutputDirectory() );

        File timestampFile = new File( getAnalysisCacheFile().getParentFile(), "play_ebean_instrumentation" );
        long lastEnhanced = 0L;
        if ( timestampFile.exists() )
        {
            String line = readFileFirstLine( timestampFile, "ASCII" );
            lastEnhanced = Long.parseLong( line );
        }

        int processedFiles = 0;
        int enhancedFiles = 0;

        List<File> modelClassesToEnhance = collectClassFilesToEnhance( lastEnhanced, outputDirectory, models );
        if ( !modelClassesToEnhance.isEmpty() )
        {
            classpathElements.remove( outputDirectory.getAbsolutePath() );
            List<File> classpathFiles = new ArrayList<File>( classpathElements.size() );
            for ( String path : classpathElements )
            {
                classpathFiles.add( new File( path ) );
            }

            List<URL> classPathUrls = new ArrayList<URL>( classpathFiles.size() + 1 );
            for ( File classpathFile : classpathFiles )
            {
                classPathUrls.add( classpathFile.toURI().toURL() );
            }
            classPathUrls.add( outputDirectory.toURI().toURL() );

            Play2Provider play2Provider = getProvider();
            Play2EbeanEnhancer enhancer = play2Provider.getEbeanEnhancer();
            enhancer.setOutputDirectory( outputDirectory );
            enhancer.setClassPathUrls( classPathUrls );

            File analysisCacheFile = getAnalysisCacheFile();
            if ( !analysisCacheFile.exists() )
            {
                throw new MojoExecutionException( String.format( "Analysis cache file \"%s\" not found", analysisCacheFile.getAbsolutePath() ) );
            }
            if ( !analysisCacheFile.isFile() )
            {
                throw new MojoExecutionException( String.format( "Analysis cache \"%s\" is not a file", analysisCacheFile.getAbsolutePath() ) );
            }
            Analysis analysis = null;
            AnalysisProcessor sbtAnalysisProcessor = getSbtAnalysisProcessor();
            if ( sbtAnalysisProcessor.areClassFileTimestampsSupported() )
            {
                analysis = sbtAnalysisProcessor.readFromFile( analysisCacheFile );
            }

            for ( File classFile: modelClassesToEnhance )
            {
                processedFiles++;
                try
                {
                    if ( enhancer.enhanceModel( classFile ) )
                    {
                        enhancedFiles++;
                        getLog().debug( String.format( "\"%s\" enhanced", classFile.getPath() ) );
                        if ( analysis != null )
                        {
                            analysis.updateClassFileTimestamp( classFile );
                        }
                    }
                    else
                    {
                        getLog().debug( String.format( "\"%s\" skipped", classFile.getPath() ) );
                    }
                }
                catch ( Exception e )
                {
                    //??
                }
            }

            if ( enhancedFiles > 0 )
            {
                if ( analysis != null )
                {
                    analysis.writeToFile( analysisCacheFile );
                }
                writeToFile( timestampFile, "ASCII", Long.toString( System.currentTimeMillis() ) );
            }
        }

        if ( processedFiles > 0 )
        {
            getLog().info( String.format( "%d Ebean %s processed, %d enhanced", Integer.valueOf( processedFiles ),
                                          processedFiles > 1 ? "classes" : "class", Integer.valueOf( enhancedFiles ) ) );
        }
        else
        {
            getLog().info( "No Ebean classes to enhance" );
        }
    }

    private Config getPlayConfiguration() {
        String configResource = System.getProperty( "config.resource" );
        if ( configResource != null )
        {
            return ConfigFactory.parseResources( configResource );
        } else
        {
            String configFileName = System.getProperty( "config.file", "conf/application.conf" );
            File applicationConfFile = new File( project.getBasedir(), configFileName );
            return ConfigFactory.parseFileAnySyntax( applicationConfFile );
        }
    }

    private String getEBeanModelsToEnhance(Config config)
    {
        try
        {
            StringBuilder collector = new StringBuilder();

            Set<Map.Entry<String, ConfigValue>> entries = config.getConfig( "ebean" ).entrySet();
            for ( Map.Entry<String, ConfigValue> entry : entries )
            {
                ConfigValue configValue = entry.getValue();
                if ( configValue.valueType() == ConfigValueType.STRING )
                {
                    collector.append( ',' ).append( configValue.unwrapped().toString() );
                } else
                {
                    String configKey = "ebean." + entry.getKey();
                    List<String> tmpModels = config.getStringList( configKey );
                    for ( String tmpModel : tmpModels )
                    {
                        collector.append( ',' ).append( tmpModel );
                    }
                }
            }
            return collector.length() != 0 ? collector.substring( 1 ) : null;
        } catch ( ConfigException.Missing e )
        {
            return "models.*";
        }
    }

    /**
     * Process all the comma delimited list of packages.
     * <p>
     * Package names are effectively converted into a directory on the file
     * system, and the class files are found and processed.
     * </p>
     * 
     * @param lastEnhanced previous enhancement timestamp
     * @param outputDirectory directory containing classes to enhance
     * @param packageNames package names include filter
     * @return list of files to enhance
     */
    public List<File> collectClassFilesToEnhance( long lastEnhanced, File outputDirectory, String packageNames )
    {
        if ( packageNames == null || packageNames.isEmpty())
        {
            return collectClassFilesToEnhanceFromPackage( lastEnhanced, outputDirectory, "", true );
            // return;
        }

        List<File> result = new ArrayList<File>();
        
        String[] pkgs = packageNames.split( "," );
        for ( int i = 0; i < pkgs.length; i++ )
        {

            String pkg = pkgs[i].trim().replace( '.', '/' );

            boolean recurse = false;
            if ( pkg.endsWith( "**" ) )
            {
                recurse = true;
                pkg = pkg.substring( 0, pkg.length() - 2 );
            }
            else if ( pkg.endsWith( "*" ) )
            {
                recurse = true;
                pkg = pkg.substring( 0, pkg.length() - 1 );
            }

            pkg = trimSlash( pkg );

            result.addAll( collectClassFilesToEnhanceFromPackage( lastEnhanced, outputDirectory, pkg, recurse ) );
        }
        return result;
    }

    private List<File> collectClassFilesToEnhanceFromPackage( long lastEnhanced, File outputDirectory, String dir, boolean recurse )
    {
        List<File> result = new ArrayList<File>();

        File d = new File( outputDirectory.getAbsolutePath(), dir );
        if ( !d.exists() )
        {
            getLog().warn( String.format( "\"%s\" directory does not exist", d.getPath() ) );
        }
        else if ( !d.isDirectory() )
        {
            getLog().warn( String.format( "\"%s\" is not a directory", d.getPath() ) );
        }
        else
        {
            for ( File file: d.listFiles() )
            {
                if ( file.isDirectory() )
                {
                    if ( recurse )
                    {
                        String subdir = dir + "/" + file.getName();
                        result.addAll( collectClassFilesToEnhanceFromPackage( lastEnhanced, outputDirectory, subdir, recurse ) );
                    }
                }
                else
                {
                    if ( file.getName().endsWith( ".class" ) && ( file.lastModified() > lastEnhanced ) )
                    {
                        result.add( file );
                        // transformFile(file);
                    }
                }
            }
        }
        return result;
    }

    private String trimSlash( String dir )
    {
        String result = dir;
        if ( dir.endsWith( "/" ) )
        {
            result = dir.substring( 0, dir.length() - 1 );
        }
        return result;
    }

}
