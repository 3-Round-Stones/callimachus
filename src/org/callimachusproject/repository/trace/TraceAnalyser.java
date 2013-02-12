package org.callimachusproject.repository.trace;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class TraceAnalyser {
	private static final int MAX_TOP = 100;
	private static final int MAX_RECENT = 1024;

	private int revision;
	private long minTotalTime;
	private long minAverageTime;
	private final List<TraceAggregate> topTotalTime = new ArrayList<TraceAggregate>();
	private final List<TraceAggregate> topAverageTime = new ArrayList<TraceAggregate>();
	private final Map<CommonTrace, Reference<TraceAggregate>> weak = new WeakHashMap<CommonTrace, Reference<TraceAggregate>>();
	private final Map<CommonTrace, TraceAggregate> recent = new LinkedHashMap<CommonTrace, TraceAggregate>(
			MAX_RECENT, 0.75f, true) {
		private static final long serialVersionUID = -2803868296652610315L;

		protected boolean removeEldestEntry(
				java.util.Map.Entry<CommonTrace, TraceAggregate> eldest) {
			if (size() < MAX_RECENT)
				return false;
			weak.put(eldest.getKey(),
					new WeakReference<TraceAggregate>(eldest.getValue()));
			return true;
		}
	};
	private final Comparator<TraceAggregate> byTotal = new Comparator<TraceAggregate>() {
		public int compare(TraceAggregate o1, TraceAggregate o2) {
			long t1 = o1.getTotal();
			long t2 = o2.getTotal();
			TraceAggregate p1 = o1;
			while ((p1 = p1.getPreviousTrace()) != null) {
				t1 += p1.getTotal();
			}
			TraceAggregate p2 = o2;
			while ((p2 = p2.getPreviousTrace()) != null) {
				t2 += p2.getTotal();
			}
			if (t1 > t2)
				return -1;
			if (t1 == t2)
				return 0;
			return 1;
		}
	};
	private final Comparator<TraceAggregate> byAverage = new Comparator<TraceAggregate>() {
		public int compare(TraceAggregate o1, TraceAggregate o2) {
			long t1 = o1.getAverage();
			long t2 = o2.getAverage();
			TraceAggregate p1 = o1;
			while ((p1 = p1.getPreviousTrace()) != null) {
				t1 += p1.getAverage();
			}
			TraceAggregate p2 = o2;
			while ((p2 = p2.getPreviousTrace()) != null) {
				t2 += p2.getAverage();
			}
			if (t1 > t2)
				return -1;
			if (t1 == t2)
				return 0;
			return 1;
		}
	};

	public synchronized Trace[] getTracesByTotalTime() {
		return topTotalTime.toArray(new Trace[topTotalTime.size()]);
	}

	public synchronized Trace[] getTracesByAverageTime() {
		return topAverageTime.toArray(new Trace[topAverageTime.size()]);
	}

	public synchronized void reset() {
		minTotalTime = 0;
		minAverageTime = 0;
		topTotalTime.clear();
		topAverageTime.clear();
		weak.clear();
		recent.clear();
		revision++;
	}

	public TraceAggregate getAggregate(TraceAggregate previous,
			Class<?> returnType, String methodName, Class<?>[] types,
			Object... args) {
		CommonTrace common;
		CommonTrace trace = null;
		if (previous != null) {
			trace = previous.getTrace();
		}
		common = new CommonTrace(trace, returnType, methodName, types, args);
		return getAggregate(previous, common);
	}

	public void update(TraceAggregate aggregate) {
		long total = aggregate.getTotal();
		long average = aggregate.getAverage();
		recordTotal(aggregate, total);
		recordAverage(aggregate, average);
	}

	private synchronized TraceAggregate getAggregate(TraceAggregate previous,
			CommonTrace common) {
		TraceAggregate aggregate = recent.get(common);
		if (aggregate != null) {
			return aggregate;
		}
		Reference<TraceAggregate> ref = weak.get(common);
		if (ref != null) {
			aggregate = ref.get();
		}
		if (aggregate == null) {
			aggregate = new TraceAggregate(this, previous, common, revision);
		}
		recent.put(common, aggregate);
		if (ref != null) {
			weak.remove(common);
		}
		return aggregate;
	}

	private synchronized void recordTotal(TraceAggregate aggregate, long total) {
		if (total > minTotalTime && aggregate.getRevision() == revision) {
			if (!topTotalTime.contains(aggregate)) {
				topTotalTime.add(aggregate);
				Collections.sort(topTotalTime, byTotal);
				if (topTotalTime.size() > MAX_TOP) {
					TraceAggregate last = topTotalTime
							.remove(topTotalTime.size() - 1);
					minTotalTime = last.getTotal();
				}
			}
		}
	}

	private synchronized void recordAverage(TraceAggregate aggregate,
			long average) {
		if (average > minAverageTime && aggregate.getRevision() == revision) {
			if (!topAverageTime.contains(aggregate)) {
				topAverageTime.add(aggregate);
				Collections.sort(topAverageTime, byAverage);
				if (topAverageTime.size() > MAX_TOP) {
					TraceAggregate last = topAverageTime.remove(topAverageTime
							.size() - 1);
					minAverageTime = last.getTotal();
				}
			}
		}
	}

}
