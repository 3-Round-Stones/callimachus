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
package org.callimachusproject.repository.trace;

import java.util.List;

public class TraceAggregate implements Trace {
	private final TraceAnalyser analyser;
	private final TraceAggregate previous;
	private final CommonTrace trace;
	private final int revision;
	private long total;
	private int invocations;

	public TraceAggregate(TraceAnalyser analyser, TraceAggregate previous, CommonTrace trace, int revision) {
		this.analyser = analyser;
		this.previous = previous;
		this.trace = trace;
		this.revision = revision;
	}

	public CommonTrace getTrace() {
		return trace;
	}

	public TraceAggregate getPreviousTrace() {
		return previous;
	}

	public List<String> getAssignments() {
		return trace.getAssignments();
	}

	@Override
	public String getReturnVariable() {
		return trace.getReturnVariable();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(trace.toString()).append("; // ");
		sb.append(getInvocations()).append(" × ");
		sb.append((getAverage() / 1000000) / 1000.0).append("s = ");
		sb.append((getTotal() / 1000000) / 1000.0).append("s");
		return sb.toString();
	}

	public int getRevision() {
		return revision;
	}

	public synchronized long getTotal() {
		return total;
	}

	public synchronized long getAverage() {
		return getTotal() / getInvocations();
	}

	public synchronized int getInvocations() {
		return invocations;
	}

	public void spent(long duration) {
		increment(duration);
		analyser.update(this);
	}

	private synchronized void increment(long duration) {
		total += duration;
		invocations++;
	}

}
