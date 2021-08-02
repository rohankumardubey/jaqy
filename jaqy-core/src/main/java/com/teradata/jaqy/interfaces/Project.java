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
package com.teradata.jaqy.interfaces;

import java.sql.SQLException;

import com.teradata.jaqy.JaqyInterpreter;

/**
 * @author	Heng Yuan
 */
public interface Project
{
	/**
	 * Binds to a JaqyResultSet.
	 * @param	rs
	 * 			ResultSet object
	 * @param	interpreter
	 * 			interpreter object
	 * @throws	Exception
	 * 			in case of error
	 */
	public void bind (JaqyResultSet rs, JaqyInterpreter interpreter) throws Exception;
	/**
	 * Get the object at the specified column.
	 * @param	column
	 * 			The column index
	 * @return	the object at the specified column.
	 * @throws	SQLException
	 * 			in case of error
	 */
	public Object get (int column) throws Exception;
	/**
	 * Free any resources.  Should not throw any exceptions.
	 */
	public void close ();
}
