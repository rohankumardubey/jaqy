/*
 * Copyright (c) 2017 Teradata
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

import com.teradata.jaqy.Globals;
import com.teradata.jaqy.JaqyInterpreter;
import com.teradata.jaqy.Session;

/**
 * @author	Heng Yuan
 */
public class CloseCommand extends JaqyCommandAdapter
{
	@Override
	public String getDescription ()
	{
		return "closes the current session.";
	}

	@Override
	public void execute (String[] args, Globals globals, JaqyInterpreter interpreter)
	{
		Session session = interpreter.getSession ();
		if (session == null)
		{
			interpreter.error ("Not in a session.");
		}
		if (session.isClosed ())
		{
			interpreter.println ("Session " + session.getId () + " is already closed.");
		}
		else
		{
			session.close (interpreter, false);
			interpreter.println ("Session " + session.getId () + " is now closed.");
		}
	}
}
