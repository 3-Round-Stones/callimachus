package org.callimachusproject.behaviours;

import java.io.IOException;
import java.util.Map;

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.rewrite.Substitution;
import org.callimachusproject.traits.CalliObject;

public abstract class PurlSupport implements CalliObject {

	public String processUriTemplate(String pattern, String queryString) throws IOException, FluidException {
		String base = this.toString();
		FluidFactory ff = FluidFactory.getInstance();
		FluidBuilder fb = ff.builder(getObjectConnection());
		Fluid fluid = fb.consume(queryString, base, String.class, "application/x-www-form-urlencoded");
		Map<String, ?> map = (Map) fluid.as(Map.class, "application/x-www-form-urlencoded");
		Substitution substitution = Substitution.compile(pattern);
        String result = substitution.replace(base, map);
        if (result != null && result.length() > 0) {
            int split = result.indexOf("\n");
            String location = result;
            if (split >= 0) {
            	location = result.substring(0, split);
            }
            int size = result.length();
            if (queryString != null) {
            	size += queryString.length() + 1;
            }
			StringBuilder sb = new StringBuilder(size);
            sb.append(location);
            if (queryString != null && location.indexOf('?') < 0) {
                sb.append('?').append(queryString);
            }
            if (split >= 0) {
            	sb.append(result.substring(split));
            }
        	return sb.toString();
        } else {
        	return null;
        }
	}
}
