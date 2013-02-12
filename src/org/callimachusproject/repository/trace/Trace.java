package org.callimachusproject.repository.trace;

import java.util.List;

public interface Trace {

	Trace getPreviousTrace();

	List<String> getAssignments();

	String getReturnVariable();

	String toString();

}