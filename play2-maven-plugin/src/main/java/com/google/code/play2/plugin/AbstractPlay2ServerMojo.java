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
//import java.net.ServerSocket;
//import java.util.ArrayList;
//import java.util.Collection;
import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Base class for Play&#33; server mojos.
 */
public abstract class AbstractPlay2ServerMojo
    extends AbstractAntJavaBasedPlay2Mojo
{
    /**
     * Alternative server port.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.httpPort", defaultValue = "" )
    private String httpPort;

    /**
     * Alternative server port for secure connection (https protocol).
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.httpsPort", defaultValue = "" )
    private String httpsPort;

    /**
     * Additional JVM arguments passed to Play! server's JVM
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.serverJvmArgs", defaultValue = "" )
    private String serverJvmArgs;

    protected Java prepareAntJavaTask( boolean fork )
        throws MojoExecutionException, IOException
    {
        File baseDir = project.getBasedir();

        Project antProject = createProject();
        Path classPath = getProjectClassPath( antProject );

        Java javaTask = new Java();
        javaTask.setTaskName( "play" );
        javaTask.setProject( antProject );
        javaTask.setClassname( "play.core.server.NettyServer" );
        javaTask.setClasspath( classPath );
        javaTask.setFork( fork );
        if ( fork )
        {
            javaTask.setDir( baseDir );

            String jvmArgs = getServerJvmArgs();
            if ( jvmArgs != null )
            {
                jvmArgs = jvmArgs.trim();
                if ( jvmArgs.length() > 0 )
                {
                    String[] args = jvmArgs.split( " " );
                    for ( String arg : args )
                    {
                        javaTask.createJvmarg().setValue( arg );
                        getLog().debug( "  Adding jvmarg '" + arg + "'" );
                    }
                }
            }

            if ( httpPort != null && httpPort.length() > 0 )
            {
                addSystemProperty( javaTask, "http.port", httpPort );
            }
            if ( httpsPort != null && httpsPort.length() > 0 )
            {
                addSystemProperty( javaTask, "https.port", httpsPort );
            }
        }
        else
        {
            // find and add all system properties in "serverJvmArgs"
            String jvmArgs = getServerJvmArgs();
            if ( jvmArgs != null )
            {
                jvmArgs = jvmArgs.trim();
                if ( jvmArgs.length() > 0 )
                {
                    String[] args = jvmArgs.split( " " );
                    for ( String arg : args )
                    {
                        if ( arg.startsWith( "-D" ) )
                        {
                            arg = arg.substring( 2 );
                            int p = arg.indexOf( '=' );
                            if ( p >= 0 )
                            {
                                String key = arg.substring( 0, p );
                                String value = arg.substring( p + 1 );
                                getLog().debug( "  Adding system property '" + arg + "'" );
                                addSystemProperty( javaTask, key, value );
                            }
                            else
                            {
                                // TODO - throw an exception
                            }
                        }
                    }
                }
            }
        }
        // addSystemProperty( javaTask, "play.home", playHome.getAbsolutePath() );
        // addSystemProperty( javaTask, "play.id", ( playId != null ? playId : "" ) );
        // addSystemProperty( javaTask, "application.path", baseDir.getAbsolutePath() );

        return javaTask;
    }

    protected Path getProjectClassPath( Project antProject )
        throws MojoExecutionException, IOException
    {
        Path classPath = new Path( antProject );

        Artifact projectArtifact = getProject().getArtifact();
        File projectArtifactFile = projectArtifact.getFile();
        if ( projectArtifactFile == null )
        {
            File classesDirectory = new File( getProject().getBuild().getOutputDirectory() );
            if ( !classesDirectory.isDirectory() )
            {
                throw new MojoExecutionException( "Project artifact file not available" ); //TODO improve message
            }
            projectArtifactFile = classesDirectory;
            //TODO - add warning, that classes may be not up to date
        }
        getLog().debug( String.format( "CP: %s:%s:%s (%s)", projectArtifact.getGroupId(),
                                       projectArtifact.getArtifactId(), projectArtifact.getType(),
                                       projectArtifact.getScope() ) );
        classPath.createPathElement().setLocation( projectArtifactFile );

        Set<?> classPathArtifacts = getProject().getArtifacts();
        for ( Iterator<?> iter = classPathArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            getLog().debug( String.format( "CP: %s:%s:%s (%s)", artifact.getGroupId(), artifact.getArtifactId(),
                                           artifact.getType(), artifact.getScope() ) );
            classPath.createPathElement().setLocation( artifact.getFile() );
        }
        return classPath;
    }

    protected String getHttpPort()
    {
        return httpPort;
    }

    protected String getHttpsPort()
    {
        return httpsPort;
    }

    protected String getServerJvmArgs()
    {
        return serverJvmArgs;
    }

    protected MavenProject getProject()
    {
        return project;
    }

}
