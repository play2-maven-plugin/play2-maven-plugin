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

package com.google.code.play2.provider.play28;

import com.google.code.play2.provider.api.Play2CoffeescriptCompiler;
import com.google.code.play2.provider.api.Play2EbeanEnhancer;
import com.google.code.play2.provider.api.Play2JavaEnhancer;
import com.google.code.play2.provider.api.Play2JavascriptCompiler;
import com.google.code.play2.provider.api.Play2LessCompiler;
import com.google.code.play2.provider.api.Play2Provider;
import com.google.code.play2.provider.api.Play2RoutesCompiler;
import com.google.code.play2.provider.api.Play2Runner;
import com.google.code.play2.provider.api.Play2TemplateCompiler;

import org.codehaus.plexus.component.annotations.Component;

/**
 * Plugin provider for Play&#33; 2.7.x
 */
@Component( role = Play2Provider.class, hint = "play28", description = "Play! 2.7.x" )
public class Play28Provider
    implements Play2Provider
{
    @Override
    public Play2LessCompiler getLessCompiler()
    {
        return new Play28LessCompiler();
    }

    @Override
    public Play2CoffeescriptCompiler getCoffeescriptCompiler()
    {
        return new Play28CoffeescriptCompiler();
    }

    @Override
    public Play2JavascriptCompiler getJavascriptCompiler()
    {
        return new Play28JavascriptCompiler();
    }

    @Override
    public Play2RoutesCompiler getRoutesCompiler()
    {
        return new Play28RoutesCompiler();
    }

    @Override
    public Play2TemplateCompiler getTemplatesCompiler()
    {
        return new Play28TemplateCompiler();
    }

    @Override
    public Play2JavaEnhancer getEnhancer()
    {
        return new Play28JavaEnhancer();
    }

    @Override
    public Play2EbeanEnhancer getEbeanEnhancer()
    {
        return new Play28EbeanEnhancer();
    }

    @Override
    public Play2Runner getRunner()
    {
        return new Play28Runner();
    }

}
