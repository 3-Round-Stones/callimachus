// operations.js

importClass(Packages.calli.Creator);
importClass(Packages.calli.Creatable);
importClass(Packages.calli.Copyable);
importClass(Packages.calli.copy);
importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);
importClass(Packages.org.openrdf.http.object.exceptions.BadRequest);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getCopyPage() {
	return findTemplate(this, copy).calliConstruct(this, 'create');
}

function postCopy(msg) {
	var template = findTemplate(this, copy);
	var newCopy = template.calliCreateResource(this, msg.input, this.FindCopyUriSpaces());
	this.PropagatePermissions(newCopy);
	return newCopy;
}

function getCreatePage(msg) {
	var factory = msg.create ? msg.create : this;
	if (this instanceof Creator && !this.equals(factory) && !this.IsCreatable(factory))
		throw new BadRequest("Cannot create this class here: " + factory);
	if (!factory.calliCreate)
		throw new InternalServerError("No create template");
	return factory.calliCreate.calliConstruct(null, 'create');
}

function postCreate(msg) {
	var factory = msg.create ? msg.create : this;
	if (!(factory instanceof Creatable))
		throw new BadRequest("Cannot create " + factory);
	if (this.equals(factory)) {
		var template = this.calliCreate;
		if (!template)
			throw new InternalServerError("No create template");
		var newCopy = template.calliCreateResource(this, msg.input, this.calliUriSpace);
		newCopy = newCopy.objectConnection.addDesignation(newCopy, this.toString());
		this.PropagatePermissions(newCopy);
	} else {
		if (!(this instanceof Creator && this.IsCreatable(factory)))
			throw new BadRequest("Cannot create this class here");
		var newCopy = factory.PostCreate(factory, msg.input, false);
		newCopy.calliReaders.addAll(this.calliReaders);
		newCopy.calliEditors.addAll(this.calliEditors);
		newCopy.calliAdministrators.addAll(this.calliAdministrators);
		var statements = this.ConstructCreatorRelationship(newCopy).iterator();
		while (statements.hasNext()) {
			this.objectConnection.add(statements.next(), []);
		}
	}
	if (!msg.intermediate) {
		this.touchRevision();
	}
	return newCopy;
}

function getEditPage() {
	return findTemplate(this, edit).calliConstruct(this, 'edit');
}

function postEdit(msg) {
	var template = findTemplate(this, edit);
	template.calliEditResource(this, msg.input);
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


