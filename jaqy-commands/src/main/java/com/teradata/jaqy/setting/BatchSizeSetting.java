/*
 * Copyright (c) 2017-2021 Teradata
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
package com.teradata.jaqy.setting;

import com.teradata.jaqy.CommandArgumentType;
import com.teradata.jaqy.JaqyInterpreter;

/**
 * @author  Heng Yuan
 */
public class BatchSizeSetting extends JaqySettingAdapter
{
    public BatchSizeSetting ()
    {
        super ("batchsize");
    }

    @Override
    public String getDescription ()
    {
        return "sets the batch execution size limit.";
    }

    @Override
    public Type getType ()
    {
        return Type.session;
    }

    @Override
    public CommandArgumentType getArgumentType ()
    {
        return CommandArgumentType.file;
    }

    @Override
    public Object get (JaqyInterpreter interpreter) throws Exception
    {
        return interpreter.getSession ().getConnection ().getBatchSize ();
    }

    @Override
    public void set (String[] args, boolean silent, JaqyInterpreter interpreter) throws Exception
    {
        int batchSize = Integer.parseInt (args[0]);
        if (batchSize < 0)
        {
            interpreter.error ("Batch size cannot be negative.");
        }
        interpreter.getSession ().getConnection ().setBatchSize (batchSize);
    }
}
