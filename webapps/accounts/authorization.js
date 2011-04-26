// authorization.js

importClass(Packages.calli.reader);
importClass(Packages.calli.contributor);
importClass(Packages.calli.editor);
importClass(Packages.calli.administrator);
importClass(Packages.org.openrdf.http.object.annotations.realm);
importClass(Packages.org.openrdf.http.object.concepts.Transaction);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);

function isReader(msg) {
	if (isReading(msg.method, msg.query)) {
		var annotated = findAnnotatedClass(msg.object.getClass(), reader);
		for (var i = 0; i < annotated.length; i++) {
			var groups = annotated[i].getAnnotation(reader).value();
			if (isMember(msg.credential, groups))
				return true;
		}
	}
	return msg.proceed();
}

function isCreator(msg) {
	if (isReading(msg.method, msg.query) || isCreating(msg.method, msg.query)) {
		var annotated = findAnnotatedClass(msg.object.getClass(), contributor);
		for (var i = 0; i < annotated.length; i++) {
			var groups = annotated[i].getAnnotation(contributor).value();
			if (isMember(msg.credential, groups))
				return true;
		}
	}
	return msg.proceed();
}

function isEditor(msg) {
	if (isReading(msg.method, msg.query) || isCreating(msg.method, msg.query) || isEditing(msg.method, msg.query)) {
		var annotated = findAnnotatedClass(msg.object.getClass(), editor);
		for (var i = 0; i < annotated.length; i++) {
			var groups = annotated[i].getAnnotation(editor).value();
			if (isMember(msg.credential, groups))
				return true;
		}
	}
	return msg.proceed();
}

function isAdministrator(msg) {
	var annotated = findAnnotatedClass(msg.object.getClass(), administrator);
	for (var i = 0; i < annotated.length; i++) {
		var groups = annotated[i].getAnnotation(administrator).value();
		if (isMember(msg.credential, groups))
			return true;
	}
	return msg.proceed();
}

function authorizePart(msg) {
	if (msg.proceed()) {
		return true;
	}
	if (isReading(msg.method, msg.query)) {
		var iter = this.GetParentResources(msg.object).iterator();
		while (iter.hasNext()) {
			if (this.AuthorizeCredential(msg.credential, msg.method, iter.next(), msg.query))
				return true;
		}
	}
	return false;
}

function isViewingTransaction(msg) {
	if (msg.proceed()) {
		return true;
	}
	if (isReading(msg.method, msg.query) && msg.object instanceof Transaction) {
		var iter = this.ListSubjectsOfTransaction(msg.object).iterator();
		while (iter.hasNext()) {
			var subject = iter.next();
			if (!findAnnotatedClass(subject.getClass(), realm).length)
				continue; // class is not protected
			if (!this.AuthorizeCredential(msg.credential, "GET", subject, "describe"))
				return false;
		}
		// if they can view the RDF of all of the subjects they can view this transaction
		return true;
	}
	return false;
}

function isReading(method, query) {
	return (method == "GET" || method == "HEAD" || method == "POST" && query == "discussion")
		&& (query == null || query == "view" || query == "discussion" || query == "history"
			|| query == "whatlinkshere" || query == "relatedchanges");
}

function isCreating(method, query) {
	return (method == "GET" || method == "HEAD" || method == "POST")
		&& query != null && query.match(/^copy(=|&|$)|^create(=|&|$)/)
}

function isEditing(method, query) {
	if (query == "edit")
		return method == "GET" || method == "HEAD" || method == "POST";
	if (query == null)
		return method == "PUT" || method == "DELETE";
	return false;
}

function isMember(credential, groups) {
	for (var i = 0; i < groups.length; i++) {
		if (credential.resource.stringValue().equals(groups[i]))
			return true;
		var group = credential.objectConnection.getObject(groups[i]);
		if (group.calliMembers && group.calliMembers.contains(credential))
			return true;
	}
	return false;
}

function findAnnotatedClass(klass, ann) {
	if (klass.isAnnotationPresent(ann)) {
		return [klass];
	}
	var result = [];
	if (klass.getSuperclass()) {
		result = findAnnotatedClass(klass.getSuperclass(), ann);
	}
	var interfaces = klass.getInterfaces();
	for (var i = interfaces.length; i--;) {
		var face = findAnnotatedClass(interfaces[i], ann);
		for (var f = face.length; f--;) {
			for (var r = result.length; r--;) {
				if (result[r].isAssignableFrom(face[f])) {
					result.splice(r, 1); //# annotation overridden
				}
			}
			result.push(face[f]);
		}
	}
	return result;
}

