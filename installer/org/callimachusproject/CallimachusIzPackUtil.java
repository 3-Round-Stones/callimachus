/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
 *
 */
package org.callimachusproject;

/**
 * A custom IzPack (see http://izpack.org) Panel to write Callimachus
 * configuration files.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusIzPackUtil {

  /**
   * Gets the file path separator.  We can't rely on java.io.File.pathSeparator
   * due to OSX weirdness (Unix '/' mixed with Apple ':' in IzPack dialog returns)
   */
  public String getPathSeparator() {
      String pathSep = "/";
      String OS = System.getProperty("os.name");
      if ( OS.startsWith("Windows") ) { pathSep = "\\"; }
      return pathSep;
  }

}