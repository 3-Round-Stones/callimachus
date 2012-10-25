package org.callimachusproject.server.process;

import org.callimachusproject.server.model.Request;

public abstract class ExchangeTask implements Comparable<ExchangeTask>,
		Runnable {
	private final Exchange exchange;

	public ExchangeTask(Exchange exchange) {
		this.exchange = exchange;
	}

	public Exchange getExchange() {
		return exchange;
	}

	@Override
	public int compareTo(ExchangeTask o2) {
		Request t1 = this.exchange.getRequest();
		Request t2 = o2.exchange.getRequest();
		if (t1.isStorable() && !t2.isStorable())
			return -1;
		if (!t1.isStorable() && t2.isStorable())
			return 1;
		if (t1.isSafe() && !t2.isSafe())
			return -1;
		if (!t1.isSafe() && t2.isSafe())
			return 1;
		if (t1.getReceivedOn() < t2.getReceivedOn())
			return -1;
		if (t1.getReceivedOn() > t2.getReceivedOn())
			return 1;
		return System.identityHashCode(t1) - System.identityHashCode(t2);
	}

}
