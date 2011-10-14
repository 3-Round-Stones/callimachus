// operations.js

importClass(Packages.calli.Composite);
importClass(Packages.calli.Creatable);
importClass(Packages.calli.edit);
importClass(Packages.calli.view);
importClass(Packages.org.callimachusproject.util.MultipartParser);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);
importClass(Packages.org.openrdf.http.object.exceptions.BadRequest);
importClass(Packages.org.openrdf.http.object.exceptions.Forbidden);

function getViewPage() {
	return findTemplate(this, view).calliConstruct(this, 'view');
}

function getCreatePage(msg) {
	if (!this.calliCreate || this.calliCreate.isEmpty())
		throw new InternalServerError("No create template");
	if (this.calliCreate.size() != 1)
		throw new InternalServerError("Multiple create templates");
	return this.calliCreate.iterator().next().calliConstruct(null, 'create');
}

function getCreatorPage(msg) {
	if (!(msg.create instanceof Creatable))
		throw new BadRequest("Cannot create this class here: " + msg.create);
	if (!msg.create.calliCreate || msg.create.calliCreate.isEmpty())
		throw new InternalServerError("No create template");
	if (msg.create.calliCreate.size() != 1)
		throw new InternalServerError("Multiple create templates");
	return msg.create.calliCreate.iterator().next().calliConstruct(null, 'create');
}

function postFactoryCreate(msg) {
	if (!(msg.create instanceof Creatable))
		throw new BadRequest("Cannot create: " + msg.create);
	if (!msg.location)
		throw new BadRequest("No location provided");
	var creatorUri = this.toString();
	var createdUri = msg.location.toString();
	var dest = createdUri.substring(0, createdUri.lastIndexOf('/', createdUri.length - 2) + 1);
	if (creatorUri != dest && creatorUri != dest.substring(0, dest.length - 1))
		throw new BadRequest("Location URI must be nested");
	if (createdUri.search(/[\s\#\?]/) >= 0 || createdUri.search(/^\w+:\/\/\S+/) != 0)
		throw new BadRequest("Fragement or name resources are not supported");
	var iter = this.FindCreator(msg.location).iterator();
	while (iter.hasNext()) {
		var user = iter.next();
		if (!msg.create.calliIsAuthorized(user, "POST", "create"))
			throw new Forbidden(user + " is not permitted to create " + msg.create + " resources");
	}
	var newCopy = null;
	var bio = new java.io.BufferedInputStream(msg.body, 65536);
	if (msg.type.indexOf("multipart/form-data") == 0) {
		bio.mark(1024);
		var parser = new MultipartParser(bio);
		var file = parser.next();
		var headers = parser.getHeaders();
		var disposition = headers.get("content-disposition");
		if (disposition && disposition.indexOf("filename=") >= 0) {
			var type = headers.get("content-type");
			if (type == "application/octet-stream" || type.indexOf("application/x-") == 0) {
				var fileName = disposition.replace(/.*filename="/g, '').replace(/".*/g, '');
				var mimetypes = new javax.activation.MimetypesFileTypeMap();
				if (!"application/octet-stream".equals(mimetypes.getContentType(fileName))) {
					type = mimetypes.getContentType(fileName);
				}
			}
			newCopy = msg.create.PostCreate(file, msg.location, type);
		} else { // not a file upload
			bio.reset();
			newCopy = msg.create.PostCreate(bio, msg.location, msg.type);
		}
	} else {
		newCopy = msg.create.PostCreate(bio, msg.location, msg.type);
	}
	newCopy.calliEditor.addAll(this.FindContributor(newCopy));
	newCopy.calliReader.addAll(this.calliReader);
	newCopy.calliEditor.addAll(this.calliEditor);
	newCopy.calliAdministrator.addAll(this.calliAdministrator);
	msg.create.touchRevision(); // Update class index
	if (msg.intermediate) {
		var revision = this.auditRevision;
		this.calliHasComponent.add(newCopy);
		this.auditRevision = revision; // restore the previous revision
	} else {
		this.calliHasComponent.add(newCopy);
	}
	return newCopy;
}

function postCreate(msg) {
	var template = this.calliCreate.iterator().next();
	if (!template)
		throw new InternalServerError("Cannot create " + this.toString() + " with " + msg.type);
	if (msg.type != "application/rdf+xml" && msg.type.indexOf("application/rdf+xml;") != 0)
		throw new BadRequest("File format is not recognized: " + msg.type);
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


