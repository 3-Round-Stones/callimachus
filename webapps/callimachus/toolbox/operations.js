// operations.js

importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.callimachusproject.server.exceptions.InternalServerError);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getEditPage() {
	return findTemplate(this, edit).calliConstruct(this, 'edit');
}

function postEdit(msg) {
	var template = findTemplate(this, edit);
	template.calliEditResource(this, msg.input);
	var parent = this.SelectParentComposite();
	if (parent) {
		parent.touchRevision();
	}
	return this;
}

function findTemplate(obj, ann) {
	var annotated = findAnnotatedClass(obj.getClass(), ann);
	if (annotated) {
		var uri = annotated.getAnnotation(ann).value();
		if (uri.length != 1)
			throw new InternalServerError("Multiple templates for " + annotated.simpleName);
		var template = obj.objectConnection.getObject(uri[0]);
		if (template.calliConstruct)
			return template;
		throw new InternalServerError("Missing template");
	}
	throw new InternalServerError("No template");
}

function findAnnotatedClass(klass, ann) {
	if (klass.isAnnotationPresent(ann)) {
		return klass;
	}
	var result;
	if (klass.getSuperclass()) {
		result = findAnnotatedClass(klass.getSuperclass(), ann);
	}
	var interfaces = klass.getInterfaces();
	for (var i = interfaces.length; i--;) {
		var face = findAnnotatedClass(interfaces[i], ann);
		if (face) {
			if (!result || result.isAssignableFrom(face)) {
				result = face;
			} else if (!face.isAssignableFrom(result)) {
				throw new InternalServerError("Conflicting templates for "
					+ result.simpleName +  " and " + face.simpleName);
			}
		}
	}
	return result;
}


