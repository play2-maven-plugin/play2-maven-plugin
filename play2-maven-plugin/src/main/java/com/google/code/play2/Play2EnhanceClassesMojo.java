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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import com.google.code.play2.provider.Play2JavaEnhancer;

/**
 * Java classes enhance
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "enhance", requiresDependencyResolution = ResolutionScope.COMPILE )
public class Play2EnhanceClassesMojo
    extends AbstractPlay2Mojo
{
    private static final String appDirectoryName = "app";

    private static final String srcManagedDirectoryName = "src_managed/main";

    /**
     * Project classpath.
     */
    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true, required = true )
    private List<String> classpathElements;

    /**
     * The directory for compiled classes.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true, readonly = true )
    private File outputDirectory;

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        List<String> classpathElementsCopy = classpathElements;
        // ?classpathElementsCopy.remove( outputDirectory.getAbsolutePath() );

        List<File> classpath = new ArrayList<File>( classpathElementsCopy.size() );
        for ( String path : classpathElementsCopy )
        {
            classpath.add( new File( path ) );
        }

        enhanceClasses( classpath );
    }

    private File getAnalysisCacheFile()
    {
        return defaultAnalysisCacheFile( project );
    }

    // Copied from AbstractPlay2SBTCompileMojo
    private File defaultAnalysisDirectory( MavenProject p )
    {
        return new File( p.getBuild().getDirectory(), "analysis" );
    }

    protected File defaultAnalysisCacheFile( MavenProject p )
    {
        return new File( defaultAnalysisDirectory( p ), "compile" );
    }


    protected void enhanceClasses( List<File> classpathFiles )
        throws MojoExecutionException, IOException
    {
        Play2JavaEnhancer enhancer = play2Provider.getEnhancer();
        enhancer.setAnalysisCacheFile( getAnalysisCacheFile() );

        try
        {
            /*StringBuilder sb = new StringBuilder();
            for (File classpathFile: classpathFiles)
            {
                sb.append(classpathFile.getAbsolutePath()).append(java.io.File.pathSeparator);
            }
            sb.append(getOutputDirectory().getAbsolutePath());
            String classpath = sb.toString();*/

            // enhancer.setClasspath( classpath );//TODO Change type
            enhancer.setClasspathFiles( classpathFiles );

            File timestampFile = new File( getAnalysisCacheFile().getParentFile(), "play_instrumentation" );
            long lastEnhanced = 0L;
            if ( timestampFile.exists() )
            {
                String line = readFileFirstLine( timestampFile );
                lastEnhanced = Long.parseLong( line );
            }

            enhanceJavaClasses( lastEnhanced, enhancer );
            enhanceTemplateClasses( lastEnhanced, enhancer );

            writeToFile( timestampFile, Long.toString( System.currentTimeMillis() ) );
        }
        catch ( IOException e )
        {
            throw e;
        }
        catch ( Exception e )// TODO-???
        {
            throw new MojoExecutionException( "Enhancement exception", e );
        }

        synchronizeManagedClasses(enhancer);
    }

    private void enhanceJavaClasses( long lastEnhanced, Play2JavaEnhancer enhancer ) throws Exception
    {
        File scannerBaseDir = new File( project.getBasedir(), appDirectoryName );
        if ( !scannerBaseDir.isDirectory() )
        {
            return;
        }
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( scannerBaseDir );
        scanner.setIncludes( new String[] { "**/*.java" } );
        scanner.addDefaultExcludes();
        scanner.scan();
        String[] javaSources = scanner.getIncludedFiles();
        for ( String source : javaSources )
        {
            File sourceFile = new File( scannerBaseDir, source );
            // System.out.println( String.format( "'%s'", sourceFile.getAbsolutePath() ) );
            if ( enhancer.getCompilationTime( sourceFile ) > lastEnhanced )
            // if ( analysis.apis().internalAPI( sourceFile ).compilation().startTime() > lastEnhanced )
            {
                Set<File> javaClasses = enhancer.getProducts( sourceFile );
                // Set<File> javaClasses = JavaConversions.setAsJavaSet( analysis.relations().products( sourceFile )
                // );
                for ( File classFile : javaClasses )
                {
                    // System.out.println( String.format( "- '%s'", classFile.getAbsolutePath() ) );
                    enhancer.enhanceJavaClass( classFile );
                    // PropertiesEnhancer.generateAccessors( classpath, classFile );
                    // PropertiesEnhancer.rewriteAccess( classpath, classFile );
                }
            }
        }
    }

    private void enhanceTemplateClasses( long lastEnhanced, Play2JavaEnhancer enhancer ) throws Exception
    {

        File scannerBaseDir = new File( project.getBuild().getDirectory(), srcManagedDirectoryName ); // TODO-parametrize
        if ( scannerBaseDir.isDirectory() )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( scannerBaseDir );
            scanner.setIncludes( new String[] { "**/*.template.scala" } );
            scanner.scan();
            String[] scalaTemplateSources = scanner.getIncludedFiles();
            for ( String source : scalaTemplateSources )
            {
                File sourceFile = new File( scannerBaseDir, source );
                // System.out.println( String.format( "'%s'", sourceFile.getAbsolutePath() ) );
                if ( enhancer.getCompilationTime( sourceFile ) > lastEnhanced )
                // if ( analysis.apis().internalAPI( sourceFile ).compilation().startTime() > lastEnhanced )
                {
                    Set<File> templateClasses = enhancer.getProducts( sourceFile );
                    // Set<File> templateClasses = JavaConversions.setAsJavaSet( analysis.relations().products(
                    // sourceFile ) );
                    for ( File classFile : templateClasses )
                    {
                        // System.out.println( String.format( "- '%s'", classFile.getAbsolutePath() ) );
                        enhancer.enhanceTemplateClass( classFile );
                        // PropertiesEnhancer.rewriteAccess( classpath, classFile );
                    }
                }
            }
        }
    }

    private void synchronizeManagedClasses(Play2JavaEnhancer enhancer) throws IOException
    {
        File managedSrcDir = new File( project.getBuild().getDirectory(), srcManagedDirectoryName );
        if ( !managedSrcDir.isDirectory() )
        {
            return;
        }

        // Play 2.1.5 - PlayCommands.scala from line 371
        // Play 2.2.0 - PlayCommands.scala from line 160
        File managedClassesDir = new File( outputDirectory.getParentFile(), outputDirectory.getName() + "_managed" );
        Set<String> managedClassesSet = new HashSet<String>();
        if ( managedClassesDir.isDirectory() )
        {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( managedClassesDir );
            scanner.scan();
            String[] managedClasses = scanner.getIncludedFiles();
            managedClassesSet.addAll( Arrays.asList( managedClasses ) );
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( managedSrcDir );
        scanner.setIncludes( new String[] { "**/*.scala", "**/*.java" } );
        scanner.scan();
        String[] managedSources = scanner.getIncludedFiles();
        if ( managedSources.length > 0 )
        {
            if ( !managedClassesDir.exists() )
            {
                if ( !managedClassesDir.mkdirs() )
                {
                    // ??
                }
            }
        }
        for ( String source : managedSources )
        {
            File sourceFile = new File( managedSrcDir, source );
            Set<File> sourceProducts = enhancer.getProducts( sourceFile );
            // sbt.IO$.MODULE$.copy(ma byc Seq sourceProducts, sbt.IO$.MODULE$.copy$default$2(),
            // sbt.IO$.MODULE$.copyDirectory$default$3());
            // System.out.println("outdir: " + getOutputDirectory().getPath());
            for ( File file : sourceProducts )
            {
                // System.out.println("path: " + file.getPath());
                // String relativePath = PathTool.getRelativePath( getOutputDirectory().getPath(),
                // file.getPath() );
                String relativePath = file.getPath().substring( outputDirectory.getPath().length() );
                if ( relativePath.startsWith( File.separator ) )
                {
                    relativePath = relativePath.substring( 1 );
                }
                // System.out.println("product: " + relativePath);
                File destinationFile = new File( managedClassesDir, relativePath );
                FileUtils.copyFile( file, destinationFile );
                managedClassesSet.remove( relativePath );
            }
        }

        for ( String managedClassPath : managedClassesSet )
        {
            // System.out.println('*' + managedClsssPath);
            File fileToDelete = new File( managedClassesDir, managedClassPath );
            if ( !fileToDelete.delete() )
            {
                // ??
            }
        }
    }
}
