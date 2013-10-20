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
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
//import org.apache.maven.plugins.annotations.LifecyclePhase;
//import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import play.core.enhancers.PropertiesEnhancer;

//import org.codehaus.plexus.util.PathTool;

import scala.collection.JavaConversions;

//import com.typesafe.config.Config;
//import com.typesafe.config.ConfigException;
//import com.typesafe.config.ConfigFactory;
//import com.typesafe.config.ConfigValue;

//import sbt.IO;
import sbt.inc.Analysis;

//import com.avaje.ebean.enhance.agent.Transformer;
//import com.avaje.ebean.enhance.ant.OfflineFileTransform;

/**
 * Compile Scala and Java sources
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "compile"/*, defaultPhase = LifecyclePhase.COMPILE*/, requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2ScalaCompileMojo
    extends AbstractPlay2ScalaCompileMojo
{
    /**
     * The source directories containing the sources to be compiled.
     * 
     */
    @Parameter( defaultValue = "${project.compileSourceRoots}", readonly = true, required = true )
    private List<String> compileSourceRoots;

    /**
     * Project classpath.
     * 
     */
    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true, required = true )
    private List<String> classpathElements;

    /**
     * The directory for compiled classes.
     * 
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    private File outputDirectory;

    /**
     * Projects main artifact.
     * 
     */
    @Parameter( defaultValue = "${project.artifact}", readonly = true, required = true )
    private Artifact projectArtifact;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        super.internalExecute();

        if ( outputDirectory.isDirectory() )
        {
            projectArtifact.setFile( outputDirectory );
        }
    }

    @Override
    protected List<String> getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    @Override
    protected List<String> getClasspathElements()
    {
        return classpathElements;
    }

    @Override
    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    @Override
    protected File getAnalysisCacheFile()
    {
        //FIXME TEMP return defaultAnalysisCacheFile( project );
        return new File(project.getBuild().getDirectory(), "cache/" + project.getArtifactId() + "/compile/inc_compile");
    }
    
    @Override
    protected void postCompile(List<File> classpathFiles, Analysis analysis) throws MojoExecutionException, IOException {
        try
        {
            StringBuilder sb = new StringBuilder();
            for (File classpathFile: classpathFiles)
            {
                sb.append(classpathFile.getAbsolutePath()).append(java.io.File.pathSeparator);
            }
            sb.append(getOutputDirectory().getAbsolutePath());
            String classpath = sb.toString();
            
            File timestampFile = new File(getAnalysisCacheFile().getParentFile(), "play_instrumentation");
            long lastEnhanced = 0L;
            if ( timestampFile.exists() )
            {
                String line = readFileFirstLine( timestampFile );
                lastEnhanced = Long.parseLong( line );
            }

            File scannerBaseDir = new File(project.getBasedir(), "app" );//TODO-parametrize
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( scannerBaseDir );
            scanner.setIncludes( new String[] {"**/*.java"} );
            scanner.addDefaultExcludes();
            scanner.scan();
            String[] javaSources = scanner.getIncludedFiles();
            for ( String source : javaSources )
            {
                File sourceFile = new File(scannerBaseDir, source);
                // System.out.println( String.format( "'%s'", sourceFile.getAbsolutePath() ) );
                if ( analysis.apis().internalAPI( sourceFile ).compilation().startTime() > lastEnhanced )
                {
                    Set<File> javaClasses = JavaConversions.setAsJavaSet( analysis.relations().products( sourceFile ) );
                    for ( File classFile : javaClasses )
                    {
                        // System.out.println( String.format( "- '%s'", classFile.getAbsolutePath() ) );
                        PropertiesEnhancer.generateAccessors( classpath, classFile );
                        PropertiesEnhancer.rewriteAccess( classpath, classFile );
                    }
                }
            }
            
            scannerBaseDir = new File(project.getBuild().getDirectory(), "src_managed/main" );//TODO-parametrize
            if ( scannerBaseDir.isDirectory() )
            {
                scanner = new DirectoryScanner();
                scanner.setBasedir( scannerBaseDir );
                scanner.setIncludes( new String[] {"**/*.template.scala"} );
                scanner.scan();
                String[] scalaTemplateSources = scanner.getIncludedFiles();
                for ( String source : scalaTemplateSources )
                {
                    File sourceFile = new File(scannerBaseDir, source);
                    // System.out.println( String.format( "'%s'", sourceFile.getAbsolutePath() ) );
                    if ( analysis.apis().internalAPI( sourceFile ).compilation().startTime() > lastEnhanced )
                    {
                        Set<File> templateClasses = JavaConversions.setAsJavaSet( analysis.relations().products( sourceFile ) );
                        for ( File classFile : templateClasses )
                        {
                            // System.out.println( String.format( "- '%s'", classFile.getAbsolutePath() ) );
                            PropertiesEnhancer.rewriteAccess( classpath, classFile );
                        }
                    }
                }
            }
            
            writeToFile( timestampFile, Long.toString( System.currentTimeMillis()) );

// PlayCommands:352
            // EBean
/*            if (classpath.contains("play-java-ebean"))
            {
                ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
                try
                {
                    List<URL> classPathUrls = new ArrayList<URL>();
                    for (File classpathFile: classpathFiles)
                    {
                        classPathUrls.add(classpathFile.toURI().toURL());
                    }
                    classPathUrls.add(getOutputDirectory().toURI().toURL());
                    URL[] cp = classPathUrls.toArray( new URL[]{} );

                    Thread.currentThread().setContextClassLoader(new URLClassLoader(cp, ClassLoader.getSystemClassLoader()));

                    ClassLoader cl = ClassLoader.getSystemClassLoader();

                    Transformer t = new Transformer(cp, "debug=-1");

                    OfflineFileTransform ft = new OfflineFileTransform(t, cl, /*classes*//*getOutputDirectory().getAbsolutePath(), /*classes*//*getOutputDirectory().getAbsolutePath());

                    Config config = ConfigFactory.load(ConfigFactory.parseFileAnySyntax(new File("conf/application.conf")));

                    String models = null;
                    try
                    {
                        // see https://github.com/playframework/Play20/wiki/JavaEbean
                        Set<Map.Entry<String, ConfigValue>> entries = config.getConfig("ebean").entrySet();
                        for (Map.Entry<String, ConfigValue> entry: entries)
                        {
                            ConfigValue configValue = entry.getValue();
                            Object configValueUnwrapped = configValue.unwrapped();
                            //TODO-optimize
                            if (models == null)
                            {
                                models = configValueUnwrapped.toString();
                            }
                            else
                            {
                                models = models + "," + configValueUnwrapped.toString();
                            }
                        }
                    }
                    catch (ConfigException.Missing e)
                    {
                        models = "models.*";
                    }
                    
                    try
                    {
                        ft.process( models );
                    }
                    catch (Throwable/*?*//* e)
                    {
                        
                    }
                }
                finally
                {
                    Thread.currentThread().setContextClassLoader( originalContextClassLoader );
                }
            }*/
// PlayCommands:392
            File managedClassesDirectory = new File(getOutputDirectory().getParentFile(), getOutputDirectory().getName() + "_managed");
            Set<String> managedClassesSet = new HashSet<String>();
            if (managedClassesDirectory.isDirectory())
            {
                scanner = new DirectoryScanner();
                scanner.setBasedir( managedClassesDirectory );
                scanner.scan();
                String[] managedClasses = scanner.getIncludedFiles();
                managedClassesSet.addAll( Arrays.asList(managedClasses) );
            }
            
            scannerBaseDir = new File(project.getBuild().getDirectory(), "src_managed/main" );//TODO-parametrize
            if ( scannerBaseDir.isDirectory() )
            {
                scanner = new DirectoryScanner();
                scanner.setBasedir( scannerBaseDir );
                scanner.setIncludes( new String[] {"**/*.scala", "**/*.java"} );
                scanner.scan();
                String[] managedSources = scanner.getIncludedFiles();
                if (managedSources.length > 0)
                {
                    if (!managedClassesDirectory.exists())
                    {
                        if (!managedClassesDirectory.mkdirs())
                        {
                            //??
                        }
                    }
                }
                for ( String source : managedSources )
                {
                    File sourceFile = new File(scannerBaseDir, source);
                    Set<File> sourceProducts = JavaConversions.setAsJavaSet( analysis.relations().products( sourceFile ) );
                    //sbt.IO$.MODULE$.copy(ma byc Seq sourceProducts, sbt.IO$.MODULE$.copy$default$2(), sbt.IO$.MODULE$.copyDirectory$default$3());
                    //System.out.println("outdir: " + getOutputDirectory().getPath());
                    for (File file: sourceProducts)
                    {
                        //System.out.println("path: " + file.getPath());
                        //String relativePath = PathTool.getRelativePath( getOutputDirectory().getPath(), file.getPath() );
                        String relativePath = file.getPath().substring( getOutputDirectory().getPath().length() );
                        if (relativePath.startsWith( File.separator ))
                        {
                            relativePath = relativePath.substring( 1 );
                        }
                        //System.out.println("product: " + relativePath);
                        File destinationFile = new File(managedClassesDirectory, relativePath);
                        FileUtils.copyFile( file, destinationFile );
                        managedClassesSet.remove( relativePath );
                    }
                }
            }
            
            for (String managedClassPath: managedClassesSet)
            {
                //System.out.println('*' + managedClsssPath);
                File fileToDelete = new File(managedClassesDirectory, managedClassPath);
                if (!fileToDelete.delete())
                {
                    //??
                }
            }
        }
        catch (Exception e)//TODO-???
        {
            throw new MojoExecutionException( "?", e );
        }
    }
}
