/*
 * Copyright 2013 Grzegorz Slowikowski (gslowikowski at gmail dot com)
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

package com.google.code.play2.provider;

import java.io.File;
//?import java.io.IOException;
import java.util.List;
import java.util.Map;

//import sbt.inc.Analysis;

import org.apache.maven.plugin.logging.Log; //?

public interface Play2SBTCompiler
{
    // ? String getDefaultScalaVersion();

    String getDefaultSbtVersion();

    // TODO - add setters, compile() method should have less parameters
    SBTCompilationResult compile( Log mavenLog, File scalaCompilerFile, File scalaLibraryFile, List<File> scalaExtra,
                                  File xsbtiArtifactFile, File compilerInterfaceSrcFile, List<File> classpath,
                                  List<File> sources, File outputDirectory, List<String> scalacOptions,
                                  List<String> javacOptions, File analysisCacheFile, Map<File, File> cacheMap )
        throws SBTCompilationException;

}
