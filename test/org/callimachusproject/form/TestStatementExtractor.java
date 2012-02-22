package org.callimachusproject.form;

import org.callimachusproject.form.helpers.StatementExtractor;
import org.callimachusproject.server.exceptions.BadRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.algebra.InsertData;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.rio.helpers.StatementCollector;

public class TestStatementExtractor {
	private static final String PREFIX = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDeleteWhere() throws Exception {
		String input = PREFIX + "DELETE { <#you> a foaf:Person } INSERT { <#me> a foaf:Person } WHERE {}";
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, "http://example.com/");
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof Modify))
			throw new BadRequest("Not a DELETE/INSERT statement");
		Modify modify = (Modify) updateExpr;
		StatementCollector collector = new StatementCollector();
		modify.getDeleteExpr().visit(new StatementExtractor(collector, ValueFactoryImpl.getInstance()));
		Assert.assertEquals(1, collector.getStatements().size());
	}

	@Test
	public void testInsertWhere() throws Exception {
		String input = PREFIX + "DELETE { <#you> a foaf:Person } INSERT { <#me> a foaf:Person } WHERE {}";
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, "http://example.com/");
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof Modify))
			throw new BadRequest("Not a DELETE/INSERT statement");
		Modify modify = (Modify) updateExpr;
		StatementCollector collector = new StatementCollector();
		modify.getInsertExpr().visit(new StatementExtractor(collector, ValueFactoryImpl.getInstance()));
		Assert.assertEquals(1, collector.getStatements().size());
	}

	@Test
	public void testSingleInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person }";
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, "http://example.com/");
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof InsertData))
			throw new BadRequest("Not a DELETE/INSERT statement");
		InsertData insertData = (InsertData) updateExpr;
		StatementCollector collector = new StatementCollector();
		insertData.getInsertExpr().visit(new StatementExtractor(collector, ValueFactoryImpl.getInstance()));
		Assert.assertEquals(1, collector.getStatements().size());
	}

	@Test
	public void testInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:name \"me\" }";
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, "http://example.com/");
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof InsertData))
			throw new BadRequest("Not a DELETE/INSERT statement");
		InsertData insertData = (InsertData) updateExpr;
		StatementCollector collector = new StatementCollector();
		insertData.getInsertExpr().visit(new StatementExtractor(collector, ValueFactoryImpl.getInstance()));
		Assert.assertEquals(2, collector.getStatements().size());
	}

	@Test
	public void testBlankInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:knows [foaf:name \"you\"]}";
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, "http://example.com/");
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof InsertData))
			throw new BadRequest("Not a DELETE/INSERT statement");
		InsertData insertData = (InsertData) updateExpr;
		StatementCollector collector = new StatementCollector();
		insertData.getInsertExpr().visit(new StatementExtractor(collector, ValueFactoryImpl.getInstance()));
		Assert.assertEquals(3, collector.getStatements().size());
	}

}
