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

import org.apache.maven.plugins.annotations.Component;

import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Source generator base class for Play! mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlay2SourceGeneratorMojo
    extends AbstractPlay2Mojo
{

    /**
     * For M2E integration.
     */
    @Component
    protected BuildContext buildContext;

    protected static final String defaultTargetDirectoryName = "src_managed";

    protected void addSourceRoot( File generatedDirectory )
    {
        if ( !project.getCompileSourceRoots().contains( generatedDirectory.getAbsolutePath() ) )
        {
            project.addCompileSourceRoot( generatedDirectory.getAbsolutePath() );
            getLog().debug( "Added source directory: " + generatedDirectory.getAbsolutePath() );
        }
    }

    protected void configureSourcePositionMappers()
    {
        String sourcePositionMappersGAV = String.format( "%s:%s:%s", pluginGroupId, "play2-source-position-mappers", pluginVersion );
        project.getProperties().setProperty( "sbt._sourcePositionMappers", sourcePositionMappersGAV/*getSourcePositionMappersGAV()*/ );
    }

}
