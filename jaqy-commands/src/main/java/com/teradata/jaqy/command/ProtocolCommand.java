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

import com.teradata.jaqy.CommandArgumentType;
import com.teradata.jaqy.JaqyDriverManager;
import com.teradata.jaqy.JaqyInterpreter;
import com.teradata.jaqy.PropertyTable;
import com.teradata.jaqy.utils.PropertyTableUtils;

/**
 * @author  Heng Yuan
 */
public class ProtocolCommand extends JaqyCommandAdapter
{
    public ProtocolCommand ()
    {
        super ("protocol");
    }

    @Override
    public String getDescription ()
    {
        return "displays / sets JDBC protocols and their associated JDBC drivers.";
    }

    @Override
    public String getLongDescription ()
    {
        return "usage: " + getCommand () + " [protocol name] [JDBC driver name]";
    }

    @Override
    public CommandArgumentType getArgumentType ()
    {
        return CommandArgumentType.file;
    }

    @Override
    public void execute (String[] args, boolean silent, boolean interactive, JaqyInterpreter interpreter) throws Exception
    {
        JaqyDriverManager driverManager = interpreter.getGlobals ().getDriverManager ();
        if (args.length == 0)
        {
            PropertyTable pt = PropertyTableUtils.createPropertyTable (driverManager.getDriverProtocolMap (), new String[] { "Protocol", "Driver" });
            interpreter.print (pt);
            return;
        }
        if (args.length != 2)
        {
            interpreter.error ("invalid command arguments.");
        }
        driverManager.addDriverProtocol (args[0], args[1]);
    }
}
