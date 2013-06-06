package org.callimachusproject.server.process;


public abstract class ExchangeTask implements Runnable {
	private final Exchange exchange;

	public ExchangeTask(Exchange exchange) {
		this.exchange = exchange;
	}

	public String toString() {
		return exchange.toString();
	}

	public Exchange getExchange() {
		return exchange;
	}

}
