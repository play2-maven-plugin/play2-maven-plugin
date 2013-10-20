/*
 * Copyright 2012 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.FileUtils;

/**
 * Abstract base class for Play! Mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2Mojo
    extends AbstractMojo
{
    /**
     * <i>Maven Internal</i>: Project to interact with.
     * 
     */
    @Component
    protected MavenProject project;

    protected abstract void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !"play2".equals( project.getPackaging() ) )
        {
            return;
        }

        try
        {
            internalExecute();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

    protected String getMainLang()
    {
        String result = "scala";
        Set<?> classPathArtifacts = project.getDependencyArtifacts(); // only direct dependencies
        for ( Iterator<?> iter = classPathArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( artifact.getGroupId().equals( "play" ) && artifact.getArtifactId().startsWith( "play-java_" ) )
            {
                result = "java";
                break;
            }
        }
        return result;
    }

    protected final BufferedReader createBufferedFileReader( File file, String encoding )
        throws FileNotFoundException, UnsupportedEncodingException
    {
        return new BufferedReader( new InputStreamReader( new FileInputStream( file ), encoding ) );
    }

    protected final BufferedWriter createBufferedFileWriter( File file, String encoding )
        throws FileNotFoundException, UnsupportedEncodingException
    {
        return new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), encoding ) );
    }

    protected String readFileFirstLine( File file )
        throws IOException
    {
        String result = null;

        BufferedReader is = createBufferedFileReader( file, "UTF-8" );
        try
        {
            result = is.readLine();
        }
        finally
        {
            is.close();
        }
        return result;
    }

    protected void writeToFile( File file, String line )
        throws IOException
    {
        BufferedWriter writer = createBufferedFileWriter( file, "UTF-8" );
        try
        {
            writer.write( line );
        }
        finally
        {
            writer.flush();
            writer.close();
        }
    }

    protected void createDirectory( File directory, boolean overwrite )
        throws IOException
    {
        if ( directory.exists() )
        {
            if ( directory.isDirectory() )
            {
                if ( overwrite )
                {
                    FileUtils.cleanDirectory( directory );
                }
            }
            else
            // file if ( directory.isFile() )
            {
                getLog().info( String.format( "Deleting \"%s\" file", directory ) ); // TODO-more descriptive message
                if ( !directory.delete() )
                {
                    throw new IOException( String.format( "Cannot delete \"%s\" file",
                                                          directory.getCanonicalPath() ) );
                }
            }
        }

        if ( !directory.exists() )
        {
            if ( !directory.mkdirs() )
            {
                throw new IOException( String.format( "Cannot create \"%s\" directory",
                                                      directory.getCanonicalPath() ) );
            }
        }
    }

}
