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
package com.teradata.jaqy.exporter;

import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;

import com.teradata.jaqy.utils.CSVExportInfo;
import com.teradata.jaqy.utils.CSVUtils;

/**
 * @author  Heng Yuan
 */
class CSVExporterOptions
{
    public Charset charset;
    public CSVFormat format;
    public HashMap<Integer, CSVExportInfo> fileInfoMap;

    public CSVExporterOptions ()
    {
        charset = CSVUtils.DEFAULT_CHARSET;
        format = CSVUtils.getDefaultFormat ();
        fileInfoMap = new HashMap<Integer, CSVExportInfo> ();
    }
}
