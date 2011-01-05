/**
 * 
 */
package org.callimachusproject.helpers;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.ParameterDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.operation;
import org.openrdf.http.object.annotations.parameter;
import org.openrdf.http.object.annotations.type;
import org.openrdf.repository.object.annotations.iri;
import org.openrdf.repository.object.annotations.name;

public class ResourceInfo {
	public static class MethodInfo extends MethodDescriptor {
		private static ResourceInfo.MethodInfo[] wrap(
				Collection<MethodDescriptor> old) {
			ResourceInfo.MethodInfo[] result = new ResourceInfo.MethodInfo[old
					.size()];
			Iterator<MethodDescriptor> iter = old.iterator();
			for (int i = 0; i < result.length; i++) {
				result[i] = new MethodInfo(iter.next());
			}
			return result;
		}

		private MethodInfo(MethodDescriptor old) {
			super(old.getMethod(), old.getParameterDescriptors());
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return getMethod().getAnnotation(annotationClass);
		}

		public Annotation[] getAnnotations() {
			return getMethod().getAnnotations();
		}

		public Type getGenericReturnType() {
			return getMethod().getGenericReturnType();
		}

		public String getIRI() {
			if (isAnnotationPresent(iri.class)) {
				return getAnnotation(iri.class).value();
			}
			return null;
		}

		@Override
		public ResourceInfo.ParameterInfo[] getParameterDescriptors() {
			return ParameterInfo.wrap(this, super.getParameterDescriptors());
		}

		public String getRemoteName() {
			if (isAnnotationPresent(method.class)
					&& isAnnotationPresent(operation.class)) {
				return join(getAnnotation(method.class).value(), "/") + " ?"
						+ join(getAnnotation(operation.class).value(), "&");
			} else if (isAnnotationPresent(method.class)) {
				return join(getAnnotation(method.class).value(), "/");
			} else if (isAnnotationPresent(operation.class)) {
				return "?" + join(getAnnotation(operation.class).value(), "&");
			}
			return getName();
		}

		public String getType() {
			if (isAnnotationPresent(type.class)) {
				return join(getAnnotation(type.class).value(), ", ");
			}
			return null;
		}

		public boolean isAnnotationPresent(
				Class<? extends Annotation> annotationClass) {
			return getMethod().isAnnotationPresent(annotationClass);
		}

		private String join(String[] value, String delim) {
			if (value == null || value.length < 1)
				return "";
			StringBuilder b = new StringBuilder();
			for (int i = 0;; i++) {
				b.append(String.valueOf(value[i]));
				if (i == value.length - 1)
					return b.toString();
				b.append(delim);
			}
		}
	}

	public static class ParameterInfo extends ParameterDescriptor {
		private static ResourceInfo.ParameterInfo[] wrap(
				ResourceInfo.MethodInfo info, ParameterDescriptor[] old) {
			if (old == null) {
				ResourceInfo.ParameterInfo[] result = new ResourceInfo.ParameterInfo[info
						.getMethod().getParameterTypes().length];
				for (int i = 0; i < result.length; i++) {
					result[i] = new ParameterInfo(info, i);
				}
				return result;
			} else {
				ResourceInfo.ParameterInfo[] result = new ResourceInfo.ParameterInfo[old.length];
				for (int i = 0; i < result.length; i++) {
					result[i] = new ParameterInfo(info, i, old[i]);
				}
				return result;
			}
		}

		private int idx;
		private ResourceInfo.MethodInfo info;

		private ParameterInfo(ResourceInfo.MethodInfo info, int idx) {
			this.info = info;
			this.idx = idx;
		}

		private ParameterInfo(ResourceInfo.MethodInfo info, int idx,
				ParameterDescriptor old) {
			this.info = info;
			this.idx = idx;
			setExpert(old.isExpert());
			setHidden(old.isHidden());
			setPreferred(old.isPreferred());
			setName(old.getName());
			setShortDescription(old.getShortDescription());
			setDisplayName(old.getDisplayName());

			Enumeration<String> names = old.attributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				setValue(name, old.getValue(name));
			}
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation ann : getAnnotations()) {
				if (ann.annotationType().equals(annotationClass))
					return annotationClass.cast(ann);
			}
			return null;
		}

		public Annotation[] getAnnotations() {
			return info.getMethod().getParameterAnnotations()[idx];
		}

		public Type getGenericType() {
			return info.getMethod().getGenericParameterTypes()[idx];
		}

		public String getHeader() {
			if (isAnnotationPresent(header.class)) {
				return join(getAnnotation(header.class).value(), ", ");
			}
			return null;
		}

		public String getIRI() {
			if (isAnnotationPresent(iri.class)) {
				return getAnnotation(iri.class).value();
			}
			return null;
		}

		public String getName() {
			if (isAnnotationPresent(name.class)) {
				return join(getAnnotation(name.class).value(), ", ");
			} else if (super.getName() != null) {
				return super.getName();
			} else if (getIRI() != null) {
				return getIRI().replaceAll(".*[:/#](?=[^:/#]*$)", "");
			} else {
				return "arg" + (idx + 1);
			}
		}

		public String getQuery() {
			if (isAnnotationPresent(parameter.class)) {
				return join(getAnnotation(parameter.class).value(), ", ");
			}
			return null;
		}

		public String getType() {
			if (isAnnotationPresent(type.class)) {
				return join(getAnnotation(type.class).value(), ", ");
			}
			return null;
		}

		public boolean isAnnotationPresent(
				Class<? extends Annotation> annotationClass) {
			for (Annotation ann : getAnnotations()) {
				if (ann.annotationType().equals(annotationClass))
					return true;
			}
			return false;
		}

		private String join(String[] value, String delim) {
			if (value == null || value.length < 1)
				return "";
			StringBuilder b = new StringBuilder();
			for (int i = 0;; i++) {
				b.append(String.valueOf(value[i]));
				if (i == value.length - 1)
					return b.toString();
				b.append(delim);
			}
		}

	}

	public static class PropertyInfo extends PropertyDescriptor {
		private static ResourceInfo.PropertyInfo[] wrap(
				Collection<PropertyDescriptor> old) {
			ResourceInfo.PropertyInfo[] result = new ResourceInfo.PropertyInfo[old
					.size()];
			Iterator<PropertyDescriptor> iter = old.iterator();
			for (int i = 0; i < result.length; i++) {
				try {
					result[i] = new PropertyInfo(iter.next());
				} catch (IntrospectionException e) {
					throw new AssertionError(e);
				}
			}
			return result;
		}

		private PropertyInfo(PropertyDescriptor old)
				throws IntrospectionException {
			super(old.getName(), old.getReadMethod(), old.getWriteMethod());
		}

		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			return getReadMethod().getAnnotation(annotationClass);
		}

		public Annotation[] getAnnotations() {
			return getReadMethod().getAnnotations();
		}

		public Type getGenericType() {
			return getReadMethod().getGenericReturnType();
		}

		public String getIRI() {
			if (isAnnotationPresent(iri.class)) {
				return getAnnotation(iri.class).value();
			}
			return null;
		}

		public boolean isAnnotationPresent(
				Class<? extends Annotation> annotationClass) {
			return getReadMethod().isAnnotationPresent(annotationClass);
		}

	}

	private BeanInfo info;

	public ResourceInfo(Class<?> klass) throws IntrospectionException {
		info = Introspector.getBeanInfo(klass, Object.class);
	}

	public ResourceInfo.MethodInfo[] getMethodDescriptors() {
		Set<Method> accessors = new HashSet<Method>();
		for (PropertyDescriptor p : getPropertyDescriptors()) {
			accessors.add(p.getReadMethod());
			accessors.add(p.getWriteMethod());
		}
		for (MethodDescriptor o : getRemoteMethodDescriptors()) {
			accessors.add(o.getMethod());
		}
		TreeSet<MethodDescriptor> methods = new TreeSet<MethodDescriptor>(
				new Comparator<MethodDescriptor>() {
					public int compare(MethodDescriptor a, MethodDescriptor b) {
						return a.getName().compareTo(b.getName());
					}
				});
		methods.addAll(Arrays.asList(info.getMethodDescriptors()));
		Iterator<MethodDescriptor> iter = methods.iterator();
		while (iter.hasNext()) {
			MethodDescriptor m = iter.next();
			if (accessors.contains(m.getMethod())) {
				iter.remove();
			}
		}
		return MethodInfo.wrap(methods);
	}

	public ResourceInfo.MethodInfo[] getRemoteMethodDescriptors() {
		List<MethodDescriptor> operations = new ArrayList<MethodDescriptor>();
		operations.addAll(Arrays.asList(info.getMethodDescriptors()));
		Iterator<MethodDescriptor> iter = operations.iterator();
		while (iter.hasNext()) {
			Method m = iter.next().getMethod();
			if (!m.isAnnotationPresent(method.class)
					&& !m.isAnnotationPresent(operation.class)) {
				iter.remove();
			}
		}
		MethodInfo[] result = MethodInfo.wrap(operations);
		Arrays.sort(result, new Comparator<MethodInfo>() {
			public int compare(MethodInfo a, MethodInfo b) {
				return a.getRemoteName().compareTo(b.getRemoteName());
			}
		});
		return result;
	}

	public ResourceInfo.PropertyInfo[] getPropertyDescriptors() {
		TreeSet<PropertyDescriptor> properties = new TreeSet<PropertyDescriptor>(
				new Comparator<PropertyDescriptor>() {
					public int compare(PropertyDescriptor a,
							PropertyDescriptor b) {
						return a.getName().compareTo(b.getName());
					}
				});
		properties.addAll(Arrays.asList(info.getPropertyDescriptors()));
		Iterator<PropertyDescriptor> iter = properties.iterator();
		while (iter.hasNext()) {
			PropertyDescriptor p = iter.next();
			if (p.getReadMethod() == null || p.getWriteMethod() == null) {
				iter.remove();
			}
		}
		return PropertyInfo.wrap(properties);
	}
}