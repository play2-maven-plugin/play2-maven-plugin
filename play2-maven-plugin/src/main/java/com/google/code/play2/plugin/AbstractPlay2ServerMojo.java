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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

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
     * Extra settings used only in production mode (like {@code devSettings}
     * for development mode).
     * <br>
     * <br>
     * Space-separated list of key=value pairs, e.g.
     * <br>
     * {@code play.server.http.port=9001 play.server.https.port=9443}
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.prodSettings", defaultValue = "" )
    private String prodSettings;

    /**
     * Additional JVM arguments passed to Play! server's JVM
     * <br>
     * <br>
     * Space-separated list of arguments, e.g.
     * <br>
     * {@code -Xmx1024m -Dconfig.resource=application-prod.conf -Dlogger.file=./conf/logback-prod.xml}
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.serverJvmArgs", defaultValue = "" )
    private String serverJvmArgs;

    protected Java prepareAntJavaTask( String mainClassName )
        throws MojoExecutionException
    {
        File baseDir = project.getBasedir();

        Project antProject = createProject();
        Path classPath = getProjectClassPath( antProject );

        Java javaTask = new Java();
        javaTask.setTaskName( "play" );
        javaTask.setProject( antProject );
        javaTask.setClassname( mainClassName );
        javaTask.setClasspath( classPath );
        javaTask.setFork( true );
        javaTask.setDir( baseDir );

        if ( prodSettings != null )
        {
            String trimmedProdSettings = prodSettings.trim();
            if ( trimmedProdSettings.length() > 0 )
            {
                String[] args = trimmedProdSettings.split( " " );
                for ( String arg : args )
                {
                    javaTask.createJvmarg().setValue( "-D" + arg );
                    getLog().debug( "  Adding jvmarg '-D" + arg + "'" );
                }
            }
        }

        if ( serverJvmArgs != null )
        {
            String jvmArgs = serverJvmArgs.trim();
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

        javaTask.createArg().setFile( baseDir );

        return javaTask;
    }

    protected Path getProjectClassPath( Project antProject )
        throws MojoExecutionException
    {
        Path classPath = new Path( antProject );

        File classesDirectory = new File( project.getBuild().getOutputDirectory() );
        if ( !classesDirectory.exists() )
        {
            throw new MojoExecutionException(
                                              String.format( "Project's classes directory \"%s\" does not exist. Run \"mvn process-classes\" first.",
                                                             classesDirectory.getAbsolutePath() ) );
        }
        if ( !classesDirectory.isDirectory() )
        {
            throw new MojoExecutionException( String.format( "Project's classes directory \"%s\" is not a directory",
                                                             classesDirectory.getAbsolutePath() ) );
        }

        getLog().debug( String.format( "CP: %s", classesDirectory.getAbsolutePath() ) );
        classPath.createPathElement().setLocation( classesDirectory );

        Set<Artifact> classPathArtifacts = project.getArtifacts();
        for ( Artifact artifact: classPathArtifacts )
        {
            getLog().debug( String.format( "CP: %s:%s:%s (%s)", artifact.getGroupId(), artifact.getArtifactId(),
                                           artifact.getType(), artifact.getScope() ) );
            classPath.createPathElement().setLocation( artifact.getFile() );
        }
        return classPath;
    }

}
