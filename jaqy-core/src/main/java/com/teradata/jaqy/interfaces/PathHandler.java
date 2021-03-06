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

import java.io.IOException;

import com.teradata.jaqy.JaqyInterpreter;

/**
 * PathHandler is an abstraction used to deal with potential input / output
 * files / directories.
 *
 * @author  Heng Yuan
 */
public interface PathHandler
{
    /**
     * Get the Path object from the path string.
     *
     * @param   path
     *          the path string
     * @param   interpreter
     *          the interpreter.
     * @return  path object
     */
    public Path getPath (String path, JaqyInterpreter interpreter) throws IOException;
    /**
     * Check if the handler can handle a path string.
     *
     * @param   path
     *          path string
     * @return  true if the path can be handled.
     */
    public boolean canHandle (String path);
}
