// operations.js

importClass(Packages.calli.Copyable);
importClass(Packages.calli.copy);
importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.apache.http.ProtocolVersion);
importClass(Packages.org.apache.http.message.BasicHttpResponse);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getCopyPage() {
	return findTemplate(this, copy).calliConstruct(this, 'create');
}

function postCopy(msg) {
	var template = findTemplate(this, copy);
	var newCopy = template.calliCreateResource(this, msg.input, this.FindCopyNamespaces());
	if (this.calliEditors.size()) {
		if (newCopy instanceof Copyable) {
			newCopy.calliEditors.addAll(this.calliEditors);
		}
		newCopy.calliAdministrators.addAll(this.calliEditors);
	} else {
		newCopy.calliAdministrators.addAll(this.FindCopyContributor(newCopy));
	}
	return newCopy;
}

function getCreatePage() {
	if (!this.calliCreate)
		throw new InternalServerError("No create template");
	return this.calliCreate.calliConstruct(this, 'create');
}

function postCreate(msg) {
	var template = this.calliCreate;
	if (!template)
		throw new InternalServerError("No create template");
	var newCopy = template.calliCreateResource(this, msg.input, this.FindCreateNamespaces());
	newCopy = newCopy.objectConnection.addDesignation(newCopy, this.toString());
	if (this.calliEditors.size()) {
		if (newCopy instanceof Copyable) {
			newCopy.calliEditors.addAll(this.calliEditors);
		}
		newCopy.calliAdministrators.addAll(this.calliEditors);
	} else {
		newCopy.calliAdministrators.addAll(this.FindCreateContributor(newCopy));
	}
	return newCopy;
}

function getEditPage() {
	return findTemplate(this, edit).calliConstruct(this, 'edit');
}

function postEdit(msg) {
	var template = findTemplate(this, edit);
	template.calliEditResource(this, msg.input);
	var ver = new ProtocolVersion("HTTP", 1, 1);
	var resp = new BasicHttpResponse(ver, 201, "Modified");
	resp.addHeader("Location", this + "?view");
	return resp;
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


