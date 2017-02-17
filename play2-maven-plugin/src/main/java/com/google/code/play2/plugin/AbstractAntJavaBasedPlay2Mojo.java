/*
 * Copyright 2013-2017 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

import org.apache.tools.ant.BuildException;
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
     * Internal runnable wrapper for Ant Java task.
     */
    protected static class JavaRunnable
        implements Runnable
    {
        private Java java;

        private boolean executed;

        private BuildException exception;

        /**
         * Creates Runnable for given Ant Java task.
         * 
         * @param java Java task to be run
         */
        public JavaRunnable( Java java )
        {
            this.java = java;
        }

        /**
         * Returns information if execution has already finished.
         * 
         * @return true if execution finished
         */
        public boolean isExecuted()
        {
            boolean result;
            synchronized ( this )
            {
                result = executed;
            }
            return result;
        }

        /**
         * Returns execution exception if it has been thrown.
         * 
         * @return exception if it has been thrown
         */
        public BuildException getException()
        {
            BuildException result = null;
            synchronized ( this )
            {
                result = exception;
            }
            return result;
        }

        @Override
        public void run()
        {
            try
            {
                java.execute();
                synchronized ( this )
                {
                    this.executed = true;
                }
            }
            catch ( BuildException e )
            {
                synchronized ( this )
                {
                    this.executed = true;
                    this.exception = e;
                }
            }
        }
    }

    /**
     * Creates and configures Ant project for Java task.
     * 
     * @return Ant project for Java task
     */
    protected Project createProject()
    {
        final Project antProject = new Project();

        final ProjectHelper helper = ProjectHelper.getProjectHelper();
        antProject.addReference( ProjectHelper.PROJECTHELPER_REFERENCE, helper );
        helper.getImportStack().addElement( "AntBuilder" ); // import checks that stack is not empty

        final BuildLogger logger = new NoBannerLogger();

        logger.setMessageOutputLevel( Project.MSG_INFO );
        logger.setOutputPrintStream( System.out );
        logger.setErrorPrintStream( System.err );

        antProject.addBuildListener( logger );

        antProject.init();
        antProject.getBaseDir();
        return antProject;
    }

    /**
     * Adds string type system property to Ant Java task.
     * 
     * @param java Ant Java task
     * @param propertyName system property name
     * @param propertyValue system property value
     */
    protected void addSystemProperty( Java java, String propertyName, String propertyValue )
    {
        Environment.Variable sysPropPlayHome = new Environment.Variable();
        sysPropPlayHome.setKey( propertyName );
        sysPropPlayHome.setValue( propertyValue );
        java.addSysproperty( sysPropPlayHome );
    }

    /**
     * Adds file type system property to Ant Java task.
     * 
     * @param java Ant Java task
     * @param propertyName system property name
     * @param propertyValue system property value
     */
    protected void addSystemProperty( Java java, String propertyName, File propertyValue )
    {
        Environment.Variable sysPropPlayHome = new Environment.Variable();
        sysPropPlayHome.setKey( propertyName );
        sysPropPlayHome.setFile( propertyValue );
        java.addSysproperty( sysPropPlayHome );
    }

}
