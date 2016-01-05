package org.callimachusproject.sail.optimistic.config;

import org.openrdf.sail.Sail;
import org.openrdf.sail.config.DelegatingSailImplConfigBase;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.SailFactory;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.helpers.SailWrapper;

/**
 * Dummy factory that just produces a do-nothing wrapper.
 * 
 * @author James Leigh
 * 
 */
public class OptimisticFactory implements SailFactory {

	@Override
	public String getSailType() {
		return "openrdf:OptimisticSail";
	}

	@Override
	public SailImplConfig getConfig() {
		return new DelegatingSailImplConfigBase(getSailType());
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		return new SailWrapper();
	}

}
