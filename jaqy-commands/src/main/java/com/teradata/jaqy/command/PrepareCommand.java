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
package com.teradata.jaqy.command;

import com.teradata.jaqy.JaqyInterpreter;
import com.teradata.jaqy.QueryMode;

/**
 * @author  Heng Yuan
 */
public class PrepareCommand extends JaqyCommandAdapter
{
    public PrepareCommand ()
    {
        super ("prepare", "prepare.txt");
    }

    @Override
    public String getDescription ()
    {
        return "prepares a sql statement.";
    }

    @Override
    public void execute (String[] args, boolean silent, boolean interactive, JaqyInterpreter interpreter)
    {
        interpreter.setQueryMode (QueryMode.Prepare);
    }
}
