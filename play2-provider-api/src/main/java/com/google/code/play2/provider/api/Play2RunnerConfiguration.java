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

package com.google.code.play2.provider.api;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Play! runner configuration.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public class Play2RunnerConfiguration implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * ...
     */
    private File baseDirectory;

    /**
     * ...
     */
    private List<File> outputDirectories;

    /**
     * ...
     */
    private List<File> dependencyClasspath;

    /**
     * ...
     */
    private File docsFile;

    /**
     * ...
     */
    private List<File> docsClasspath;

    /**
     * ...
     */
    private Integer httpPort;

    /**
     * ...
     */
    private Integer httpsPort;

    /**
     * ...
     */
    private String httpAddress;

    /**
     * ...
     */
    private String assetsPrefix;

    /**
     * ...
     */
    private File assetsDirectory;

    /**
     * ...
     */
    private Map<String, String> devSettings;

    /**
     * ...
     */
    private transient Play2Builder buildLink;

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public File getBaseDirectory()
    {
        return baseDirectory;
    }

    /**
     * Sets ... .
     * 
     * @param baseDirectory ...
     */
    public void setBaseDirectory( File baseDirectory )
    {
        this.baseDirectory = baseDirectory;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public List<File> getOutputDirectories()
    {
        return outputDirectories;
    }

    /**
     * Sets ... .
     * 
     * @param outputDirectories ...
     */
    public void setOutputDirectories( List<File> outputDirectories )
    {
        this.outputDirectories = outputDirectories;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public List<File> getDependencyClasspath()
    {
        return dependencyClasspath;
    }

    /**
     * Sets ... .
     * 
     * @param dependencyClasspath ...
     */
    public void setDependencyClasspath( List<File> dependencyClasspath )
    {
        this.dependencyClasspath = dependencyClasspath;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public File getDocsFile()
    {
        return docsFile;
    }

    /**
     * Sets ... .
     * 
     * @param docsFile ...
     */
    public void setDocsFile( File docsFile )
    {
        this.docsFile = docsFile;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public List<File> getDocsClasspath()
    {
        return docsClasspath;
    }

    /**
     * Sets ... .
     * 
     * @param docsClasspath ...
     */
    public void setDocsClasspath( List<File> docsClasspath )
    {
        this.docsClasspath = docsClasspath;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public Integer getHttpPort()
    {
        return httpPort;
    }

    /**
     * Sets ... .
     * 
     * @param httpPort ...
     */
    public void setHttpPort( Integer httpPort )
    {
        this.httpPort = httpPort;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public Integer getHttpsPort()
    {
        return httpsPort;
    }

    /**
     * Sets ... .
     * 
     * @param httpsPort ...
     */
    public void setHttpsPort( Integer httpsPort )
    {
        this.httpsPort = httpsPort;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public String getHttpAddress()
    {
        return httpAddress;
    }

    /**
     * Sets ... .
     * 
     * @param httpAddress ...
     */
    public void setHttpAddress( String httpAddress )
    {
        this.httpAddress = httpAddress;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public String getAssetsPrefix()
    {
        return assetsPrefix;
    }

    /**
     * Sets ... .
     * 
     * @param assetsPrefix ...
     */
    public void setAssetsPrefix( String assetsPrefix )
    {
        this.assetsPrefix = assetsPrefix;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public File getAssetsDirectory()
    {
        return assetsDirectory;
    }

    /**
     * Sets ... .
     * 
     * @param assetsDirectory ...
     */
    public void setAssetsDirectory( File assetsDirectory )
    {
        this.assetsDirectory = assetsDirectory;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public Map<String, String> getDevSettings()
    {
        return devSettings;
    }

    /**
     * Sets ... .
     * 
     * @param devSettings ...
     */
    public void setDevSettings( Map<String, String> devSettings )
    {
        this.devSettings = devSettings;
    }

    /**
     * Returns ... .
     * 
     * @return ...
     */
    public Play2Builder getBuildLink()
    {
        return buildLink;
    }

    /**
     * Sets ... .
     * 
     * @param buildLink ...
     */
    public void setBuildLink( Play2Builder buildLink )
    {
        this.buildLink = buildLink;
    }

}
