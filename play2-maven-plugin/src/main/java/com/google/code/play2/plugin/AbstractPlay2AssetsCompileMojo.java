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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import org.codehaus.plexus.util.DirectoryScanner;

import com.google.code.play2.provider.api.AssetCompilationException;

public abstract class AbstractPlay2AssetsCompileMojo
    extends AbstractPlay2Mojo
{
    /**
     * The "assets" directory.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play2.assetsDirectory", readonly = true, defaultValue = "${project.basedir}/app/assets" )
    private File assetsDirectory;

    private static final String TARGET_DIRECTORY_NAME = "resource_managed/main";

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( !assetsDirectory.isDirectory() )
        {
            getLog().info( "No assets to compile" );
            return;
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( assetsDirectory );
        if ( getAssetsIncludes() != null )
        {
            scanner.setIncludes( getAssetsIncludes().split( "," ) );
        }
        if ( getAssetsExcludes() != null )
        {
            scanner.setExcludes( getAssetsExcludes().split( "," ) );
        }
        scanner.addDefaultExcludes();
        scanner.scan();
        String[] fileNames = scanner.getIncludedFiles();
        if ( fileNames.length > 0 )
        {
            File targetDirectory = new File( project.getBuild().getDirectory() );
            File generatedDirectory = new File( targetDirectory, TARGET_DIRECTORY_NAME );
            File outputDirectory = new File( generatedDirectory, "public" );

            try
            {
                compileAssets( assetsDirectory, fileNames, outputDirectory );
                addTargetDirectoryToResources();
            }
            catch ( AssetCompilationException e )
            {
                throw new MojoExecutionException( "Assets compilation failed", e );
            }
        }
        else
        {
            getLog().info( "No assets to compile" );
        }
    }

    protected abstract String getAssetsIncludes();

    protected abstract String getAssetsExcludes();

    protected abstract void compileAssets( File assetsSourceDirectory, String[] fileNames, File outputDirectory )
        throws AssetCompilationException, IOException, MojoExecutionException;

    private void addTargetDirectoryToResources()
    {
        File targetDirectory = new File( project.getBuild().getDirectory() );
        File generatedDirectory = new File( targetDirectory, TARGET_DIRECTORY_NAME );

        boolean resourceAlreadyAdded = false;
        for ( Resource res : project.getResources() )
        {
            if ( res.getDirectory().equals( generatedDirectory.getAbsolutePath() ) )
            {
                resourceAlreadyAdded = true;
                break;
            }
        }
        if ( !resourceAlreadyAdded )
        {
            Resource resource = new Resource();
            resource.setDirectory( generatedDirectory.getAbsolutePath() );
            project.addResource( resource );
            getLog().debug( "Added resource: " + resource.getDirectory() );
        }
    }

}
