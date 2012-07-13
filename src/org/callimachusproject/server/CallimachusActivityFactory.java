package org.callimachusproject.server;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.DatatypeConfigurationException;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.auditing.ActivityFactory;

public final class CallimachusActivityFactory implements ActivityFactory {
	private static final String ACTIVITY = "http://www.w3.org/ns/prov#Activity";
	private static final String INSERT_FOLDER = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "INSERT {\n"
			+ "$parent calli:hasComponent $folder .\n"
			+ "$folder a calli:Folder, $folderType;\n"
			+ "rdfs:label $label;\n"
			+ "calli:administrator ?administrator;\n"
			+ "calli:editor ?editor;\n"
			+ "calli:reader ?reader\n"
			+ "} WHERE {\n"
			+ "OPTIONAL {$parent calli:administrator ?administrator}\n"
			+ "OPTIONAL {$parent calli:editor ?editor}\n"
			+ "OPTIONAL {$parent calli:reader ?reader}\n" + "}";
	private static final String CALLI_HASCOMPONENT = "http://callimachusproject.org/rdf/2009/framework#hasComponent";
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final String uriSpace;
	private final String activityType;
	private final String folderType;
	private String namespace;
	private GregorianCalendar date;
	private String folder;

	public CallimachusActivityFactory(String uriSpace, String activityType, String folderType) throws DatatypeConfigurationException {
		assert uriSpace != null;
		assert activityType != null;
		assert folderType != null;
		this.uriSpace = uriSpace;
		this.activityType = activityType;
		this.folderType = folderType;
		assert uriSpace.endsWith("/");
	}

	public URI createActivityURI(ValueFactory vf) {
		String local = uid + seq.getAndIncrement();
		return vf.createURI(getNamespace(), local);
	}

	public void activityStarted(URI activity, RepositoryConnection con) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI(activity.getNamespace()), vf.createURI(CALLI_HASCOMPONENT), activity, activity);
		con.add(activity, RDF.TYPE, vf.createURI(activityType), activity);
		con.add(activity, RDF.TYPE, vf.createURI(ACTIVITY), activity);
	}

	public synchronized void activityEnded(URI activity, RepositoryConnection con) throws RepositoryException {
		if (folder == null || !folder.equals(activity.getNamespace())) {
			ValueFactory vf = con.getValueFactory();
			createFolder(vf.createURI(folder = activity.getNamespace()), con);
		}
	}

	private boolean createFolder(URI folder, RepositoryConnection con) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		if (con.hasStatement(folder, RDF.TYPE, null, true))
			return false;
		String str = folder.stringValue();
		assert str.endsWith("/");
		int split = str.lastIndexOf('/', str.length() - 2);
		if (split < 0)
			return false;
		String label = str.substring(split + 1, str.length() - 1).replace('-', ' ');
		URI parent = vf.createURI(str.substring(0, split + 1));
		createFolder(parent, con);
		try {
			Update update = con.prepareUpdate(QueryLanguage.SPARQL, INSERT_FOLDER);
			update.setBinding("folder", folder);
			update.setBinding("parent", parent);
			update.setBinding("folderType", vf.createURI(folderType));
			update.setBinding("label", vf.createLiteral(label));
			update.execute();
			return true;
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
	}

	private synchronized String getNamespace() {
		GregorianCalendar cal = new GregorianCalendar();
		if (date == null || date.get(Calendar.DATE) != cal.get(Calendar.DATE)
				|| date.get(Calendar.MONTH) != cal.get(Calendar.MONTH)
				|| date.get(Calendar.YEAR) != cal.get(Calendar.YEAR)) {
			date = cal;
			return namespace = uriSpace + date.get(Calendar.YEAR) + "/"
					+ zero(date.get(Calendar.MONTH) + 1) + "/"
					+ zero(date.get(Calendar.DATE)) + "/";
		}
		return namespace;
	}

	private String zero(int number) {
		if (number < 10)
			return "0" + number;
		return String.valueOf(number);
	}
}