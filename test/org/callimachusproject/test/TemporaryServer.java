/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.test;

import java.util.concurrent.Callable;

import org.callimachusproject.repository.CalliRepository;

public interface TemporaryServer {

	void start() throws InterruptedException, Exception;

	void pause() throws Exception;

	void resume() throws Exception;

	void stop() throws Exception;

	void destroy() throws Exception;

	String getOrigin();

	String getUsername();

	char[] getPassword();

	CalliRepository getRepository();

	<T> T waitUntilReCompiled(Callable<T> callable) throws InterruptedException, Exception;
}
