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
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.NoBannerLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment;

/**
 * Base class for Ant Java task using mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractAntJavaBasedPlay2Mojo
    extends AbstractPlay2Mojo
{

    /**
     * List of artifacts this plugin depends on.
     */
    @Parameter( property = "plugin.artifacts", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    /**
     * Internal runnable wrapper for Ant Java task.
     */
    protected static class JavaRunnable
        implements Runnable
    {
        private Java java;

        private Exception exception;

        public JavaRunnable( Java java )
        {
            this.java = java;
        }

        public Exception getException()
        {
            Exception result = null;
            synchronized ( this )
            {
                result = exception;
            }
            return result;
        }

        public void run()
        {
            try
            {
                java.execute();
            }
            catch ( Exception e )
            {
                synchronized ( this )
                {
                    this.exception = e;
                }
            }
        }
    }

    protected Project createProject()
    {
        final Project project = new Project();

        final ProjectHelper helper = ProjectHelper.getProjectHelper();
        project.addReference( ProjectHelper.PROJECTHELPER_REFERENCE, helper );
        helper.getImportStack().addElement( "AntBuilder" ); // import checks that stack is not empty

        final BuildLogger logger = new NoBannerLogger();

        logger.setMessageOutputLevel( Project.MSG_INFO );
        logger.setOutputPrintStream( System.out );
        logger.setErrorPrintStream( System.err );

        project.addBuildListener( logger );

        project.init();
        project.getBaseDir();
        return project;
    }

    protected void addSystemProperty( Java java, String propertyName, String propertyValue )
    {
        Environment.Variable sysPropPlayHome = new Environment.Variable();
        sysPropPlayHome.setKey( propertyName );
        sysPropPlayHome.setValue( propertyValue );
        java.addSysproperty( sysPropPlayHome );
    }

    protected void addSystemProperty( Java java, String propertyName, File propertyValue )
    {
        Environment.Variable sysPropPlayHome = new Environment.Variable();
        sysPropPlayHome.setKey( propertyName );
        sysPropPlayHome.setFile( propertyValue );
        java.addSysproperty( sysPropPlayHome );
    }

    protected Artifact getPluginArtifact( String groupId, String artifactId, String type )
        throws MojoExecutionException
    {
        Artifact result = null;
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( artifact.getGroupId().equals( groupId ) && artifact.getArtifactId().equals( artifactId )
                && type.equals( artifact.getType() ) )
            {
                result = artifact;
                break;
            }
        }
        if ( result == null )
        {
            throw new MojoExecutionException(
                                              String.format( "Unable to locate '%s:%s' in the list of plugin artifacts",
                                                             groupId, artifactId ) );
        }
        return result;
    }

}
