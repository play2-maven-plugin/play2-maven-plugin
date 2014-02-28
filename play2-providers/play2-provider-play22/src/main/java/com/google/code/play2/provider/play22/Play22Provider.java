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

package com.google.code.play2.provider.play22;

import com.google.code.play2.provider.Play2CoffeescriptCompiler;
import com.google.code.play2.provider.Play2EbeanEnhancer;
import com.google.code.play2.provider.Play2JavaEnhancer;
import com.google.code.play2.provider.Play2JavascriptCompiler;
import com.google.code.play2.provider.Play2LessCompiler;
import com.google.code.play2.provider.Play2Provider;
import com.google.code.play2.provider.Play2RoutesCompiler;
import com.google.code.play2.provider.Play2TemplateCompiler;

/**
 * Plugin provider for Play&#33; 2.2.x
 * 
 * @plexus.component role="com.google.code.play2.provider.Play2Provider" role-hint="play22"
 */
public class Play22Provider
    implements Play2Provider
{
    public Play2LessCompiler getLessCompiler()
    {
        return new Play22LessCompiler();
    }

    public Play2CoffeescriptCompiler getCoffeescriptCompiler()
    {
        return new Play22CoffeescriptCompiler();
    }

    public Play2JavascriptCompiler getJavascriptCompiler()
    {
        return new Play22JavascriptCompiler();
    }

    public Play2RoutesCompiler getRoutesCompiler()
    {
        return new Play22RoutesCompiler();
    }

    public Play2TemplateCompiler getTemplatesCompiler()
    {
        return new Play22TemplateCompiler();
    }

    public Play2JavaEnhancer getEnhancer()
    {
        return new Play22JavaEnhancer();
    }

    public Play2EbeanEnhancer getEbeanEnhancer()
    {
        return new Play22EbeanEnhancer();
    }

}
