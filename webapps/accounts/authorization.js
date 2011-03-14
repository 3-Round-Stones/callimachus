// authorization.js

importClass(Packages.org.openrdf.http.object.concepts.Transaction);
importClass(Packages.org.callimachusproject.annotations.reviewer);
importClass(Packages.org.callimachusproject.annotations.conservator);
importClass(Packages.org.openrdf.http.object.exceptions.InternalServerError);

function isConservator(msg) {
	if (isReviewing(msg.method, msg.query) || isEditing(msg.method, msg.query)) {
		var annotated = findAnnotatedClass(msg.object.getClass(), conservator);
		for (var a = annotated.length; a--;) {
			var uri = annotated[a].getAnnotation(conservator).value();
			for (var u = uri.length; u--;) {
				if (msg.credential.resource.stringValue().equals(uri[u]))
					return true;
				var group = this.objectConnection.getObject(uri[u]);
				if (group.calliMembers && group.calliMembers.contains(msg.credential))
					return true;
			}
		}
	}
	return msg.proceed();
}

function isReviewer(msg) {
	if (isReviewing(msg.method, msg.query)) {
		var annotated = findAnnotatedClass(msg.object.getClass(), reviewer);
		for (var a = annotated.length; a--;) {
			var uri = annotated[a].getAnnotation(reviewer).value();
			for (var u = uri.length; u--;) {
				if (msg.credential.resource.stringValue().equals(uri[u]))
					return true;
				var group = this.objectConnection.getObject(uri[u]);
				if (group.calliMembers && group.calliMembers.contains(credential))
					return true;
			}
		}
	}
	return msg.proceed();
}

function isViewingTransaction(msg) {
	if (msg.proceed()) {
		return true;
	}
	if (isReviewing(msg.method, msg.query)) {
		if (msg.object instanceof Transaction) {
			var iter = this.ListSubjectsOfTransaction(object).iterator();
			while (iter.hasNext()) {
				if (this.AuthorizeCredential(msg.credential, "GET", iter.next(), "describe"))
					return true; //# if they can view the RDF of the resource they can view its transactions
			}
		}
	}
	return false;
}

function isReviewing(method, query) {
	return (method == "GET" || method == "HEAD" || method == "POST" && query == "discussion")
		&& (query == null || query == "view" || query == "discussion" || query == "describe" || query == "history"
			|| query == "whatlinkshere" || query == "relatedchanges" || query == "introspect");
}

function isEditing(method, query) {
	return query == "edit" && (method == "GET" || method == "HEAD" || method == "POST");
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

