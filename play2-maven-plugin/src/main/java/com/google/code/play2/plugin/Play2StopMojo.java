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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Stop Play&#33; server started in production mode ({@code sbt stop} equivalent).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "stop" )
public class Play2StopMojo
    extends AbstractPlay2StopServerMojo
{

    /**
     * Skip goal execution
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.stopSkip", defaultValue = "false" )
    private boolean stopSkip;

    /**
     * Identifier of the module to stop.
     * <br>
     * <br>
     * Important in multi-module projects with more than one {@code play2} modules
     * to choose which one should be stopped.
     * <br>
     * There are three supported formats:
     * <ul>
     * <li>
     * {@code artifactId} or {@code :artifactId} - find first module with given {@code artifactId}
     * </li>
     * <li>
     * {@code groupId:artifactId} - find module with given {@code groupId} and {@code artifactId}
     * </li>
     * </ul>
     * If not specified, all reactor modules with {@code play2} packaging will be selected.
     * <br>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.mainModule", defaultValue = "" )
    private String mainModule;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( stopSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        if ( !isMainModule() )
        {
            getLog().debug( "Not main module - skipping execution" );
            return;
        }

        // Make separate method for checking conf file (use in "run" and "start" mojos)
        File baseDir = project.getBasedir();

        File confDir = new File( baseDir, "conf" );
        if ( !confDir.isDirectory() )
        {
            getLog().info( "Skipping execution" );
            return;
        }
        if ( !new File( confDir, "application.conf" ).isFile() && !new File( confDir, "application.json" ).isFile() )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        getLog().info( "Stopping Play! server" );

        stopServer();

        getLog().info( "Play! server stopped" );
    }

    private boolean isMainModule()
    {
        return mainModule == null || "".equals( mainModule ) || isMatchingProject( project, mainModule );
    }

}
