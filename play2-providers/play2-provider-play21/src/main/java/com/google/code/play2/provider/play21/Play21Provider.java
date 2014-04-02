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

package com.google.code.play2.provider.play21;

import com.google.code.play2.provider.api.Play2CoffeescriptCompiler;
import com.google.code.play2.provider.api.Play2EbeanEnhancer;
import com.google.code.play2.provider.api.Play2JavaEnhancer;
import com.google.code.play2.provider.api.Play2JavascriptCompiler;
import com.google.code.play2.provider.api.Play2LessCompiler;
import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.Play2TemplateCompiler;

/**
 * Plugin provider for Play&#33; 2.1.x
 * 
 * @plexus.component role="com.google.code.play2.provider.Play2Provider" role-hint="play22"
 */
public class Play21Provider
    implements Play2Provider
{
    public Play2LessCompiler getLessCompiler()
    {
        return new Play21LessCompiler();
    }

    public Play2CoffeescriptCompiler getCoffeescriptCompiler()
    {
        return new Play21CoffeescriptCompiler();
    }

    public Play2JavascriptCompiler getJavascriptCompiler()
    {
        return new Play21JavascriptCompiler();
    }

    public Play2RoutesCompiler getRoutesCompiler()
    {
        return new Play21RoutesCompiler();
    }

    public Play2TemplateCompiler getTemplatesCompiler()
    {
        return new Play21TemplateCompiler();
    }

    public Play2JavaEnhancer getEnhancer()
    {
        return new Play21JavaEnhancer();
    }

    public Play2EbeanEnhancer getEbeanEnhancer()
    {
        return new Play21EbeanEnhancer();
    }

}
