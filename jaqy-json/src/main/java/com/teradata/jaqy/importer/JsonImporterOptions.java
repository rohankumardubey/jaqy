/*
 * Copyright (c) 2021 Teradata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.jaqy.importer;

import java.nio.charset.Charset;

import com.teradata.jaqy.utils.JsonBinaryFormat;
import com.teradata.jaqy.utils.JsonFormat;
import com.teradata.jaqy.utils.JsonUtils;

/**
 * @author  Heng Yuan
 */
class JsonImporterOptions
{
    public Charset charset = JsonUtils.DEFAULT_CHARSET;
    public JsonBinaryFormat binaryFormat = JsonUtils.DEFAULT_BINARY_FORMAT;
    public JsonFormat format = JsonUtils.DEFAULT_FORMAT;
    public boolean rootAsArray = false;
    public String rowExp = null;
}
