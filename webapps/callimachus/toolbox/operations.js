// operations.js

importClass(Packages.calli.Creator);
importClass(Packages.calli.Creatable);
importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);
importClass(Packages.org.openrdf.http.object.exceptions.BadRequest);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getCreatePage(msg) {
	var factory = msg.create ? msg.create : this;
	if (this instanceof Creator && !this.equals(factory) && !this.IsCreatable(factory))
		throw new BadRequest("Cannot create this class here: " + factory);
	if (!factory.calliCreate)
		throw new InternalServerError("No create template");
	return factory.calliCreate.calliConstruct(null, 'create');
}

function postFactoryCreate(msg) {
	if (!(msg.create instanceof Creatable))
		throw new BadRequest("Cannot create " + msg.create);
	if (!(this instanceof Creator && this.IsCreatable(msg.create)))
		throw new BadRequest("Cannot create this class here");
	var creatorUri = this.toString();
	if (!msg.location)
		throw new BadRequest("No location provided");
	var createdUri = msg.location.toString();
	var dest = createdUri.substring(0, createdUri.lastIndexOf('/', createdUri.length - 2) + 1);
	if (creatorUri != dest && creatorUri != dest.substring(0, dest.length - 1))
		throw new BadRequest("Resource URI must be nested");
	var newCopy = msg.create.PostCreate(msg.body, msg.location);
	this.PropagatePermissions(newCopy);
	var statements = this.ConstructCreatorRelationship(newCopy).iterator();
	while (statements.hasNext()) {
		this.objectConnection.add(statements.next(), []);
	}
	msg.create.touchRevision();
	if (!msg.intermediate) {
		this.touchRevision();
	}
	return newCopy;
}

function postCreate(msg) {
	var template = this.calliCreate;
	if (!template)
		throw new InternalServerError("No create template");
	if (!msg.location)
		throw new BadRequest("Missing Location header");
	if (!this.IsCreateInUriSpace(msg.location))
		throw new BadRequest("Incorrect Subject Namespace");
	var newCopy = template.calliCreateResource(msg.body, this.toString(), msg.location);
	newCopy = newCopy.objectConnection.addDesignation(newCopy, this.toString());
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


