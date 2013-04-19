/*
 * Copyright 2013 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play2;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
//import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Run Play&#33; server ("play run" equivalent).
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME )
@Execute( phase = LifecyclePhase.PROCESS_CLASSES )
public class Play2RunMojo
    extends AbstractPlay2RunMojo
{
//    /**
//     * Play! id (profile) used.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.id", defaultValue = "" )
//    private String playId;

//    /**
//     * Play! id (profile) used when running server with tests.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.testId", defaultValue = "test" )
//    private String playTestId;

//    /**
//     * Run server with test profile.
//     * 
//     * @since 1.0.0
//     */
//    @Parameter( property = "play.runWithTests", defaultValue = "false" )
//    private boolean runWithTests;

//    @Override
//    protected String getPlayId()
//    {
//        return ( runWithTests ? playTestId : playId );
//    }

    /**
     * Get the executed project from the forked lifecycle.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "executedProject" )
    private MavenProject executedProject;

    @Override
    public MavenProject getProject()
    {
        return executedProject;
    }

}
