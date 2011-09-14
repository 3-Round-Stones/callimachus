package org.callimachusproject.behaviours;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.traits.SelfAuthorizingTarget;
import org.openrdf.repository.object.annotations.name;
import org.openrdf.repository.object.annotations.sparql;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClassAuthorizationSupport implements SelfAuthorizingTarget, RDFObjectBehaviour {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private Logger logger = LoggerFactory.getLogger(ClassAuthorizationSupport.class);

	@Override
	public boolean calliIsAuthorized(Object credential, String method,
			String query) {
		assert credential != null;
		Class<?> klass = getBehaviourDelegate().getClass();
		Set<String> groups = new LinkedHashSet<String>();
		findAuthorizedGroups(klass, method, query, groups);
		if (groups.isEmpty())
			return false;
		if (groups.contains(credential.toString()))
			return true;
		groups.retainAll(selectMembership(credential));
		return !groups.isEmpty();
	}

	@sparql(PREFIX + "SELECT (str(?group) as ?gstring) { ?group calli:member $credential }")
	protected abstract Set<String> selectMembership(@name("credential") Object credential);

	private void findAuthorizedGroups(Class<?> klass, String method,
			String query, Set<String> groups) {
		if (isReading(method, query)) {
			addAnnotationValues(klass, "calli.reader", groups);
			addAnnotationValues(klass, "call.contributor", groups);
			addAnnotationValues(klass, "call.editor", groups);
		} else if (isCreating(method, query)) {
			addAnnotationValues(klass, "call.contributor", groups);
			addAnnotationValues(klass, "call.editor", groups);
		} else if (isEditing(method, query)) {
			addAnnotationValues(klass, "call.editor", groups);
		}
		addAnnotationValues(klass, "call.administrator", groups);
	}

	private boolean isReading(String method, String query) {
		return ("GET".equals(method) || "HEAD".equals(method) || "POST".equals(method) && "discussion".equals(query))
			&& (query == null || "view".equals(query) || "discussion".equals(query) || "history".equals(query)
				|| "whatlinkshere".equals(query) || "relatedchanges".equals(query));
	}

	private boolean isCreating(String method, String query) {
		if (query == null)
			return false;
		if ("GET".equals(method) || "HEAD".equals(method) || "POST".equals(method)) {
			if ("create".equals(query))
				return true;
			if (query.startsWith("create")) {
				char ch = query.charAt("create".length());
				return ch == '=' || ch == '&';
			}
		}
		return false;
	}

	private boolean isEditing(String method, String query) {
		if (query == null)
			return "PUT".equals(method) || "DELETE".equals(method);
		if ("edit".equals(query))
			return "GET".equals(method) || "HEAD".equals(method) || "POST".equals(method);
		return false;
	}

	private void addAnnotationValues(Class<?> klass, String name, Collection<String> groups) {
		Map<Class<?>,Annotation> annotated = new LinkedHashMap<Class<?>,Annotation>();
		findAnnotatedClass(klass, name, annotated);
		removeSuperClasses(annotated.keySet());
		for (Annotation ann : annotated.values()) {
			addAnnotationValue(ann, groups);
		}
	}

	private void findAnnotatedClass(Class<?> klass, String name,Map<Class<?>,Annotation> result) {
		for (Annotation ann : klass.getAnnotations()) {
			if (ann.annotationType().getName().equals(name)) {
				result.put(klass, ann);
				return;
			}
		}
		if (klass.getSuperclass() != null) {
			findAnnotatedClass(klass.getSuperclass(), name, result);
		}
		Class<?>[] interfaces = klass.getInterfaces();
		for (Class<?> iface : interfaces) {
			findAnnotatedClass(iface, name, result);
		}
	}

	private void removeSuperClasses(Set<Class<?>> set) {
		for (Class<?> f : new ArrayList<Class<?>>(set)) {
			Iterator<Class<?>> iter = set.iterator();
			while (iter.hasNext()) {
				if (iter.next().isAssignableFrom(f)) {
					iter.remove(); //# annotation overridden
				}
			}
		}
	}

	private void addAnnotationValue(Annotation ann, Collection<String> values) {
		try {
			Object ret = ann.annotationType().getMethod("value").invoke(ann);
			values.addAll(Arrays.asList((String[]) ret));
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

}
