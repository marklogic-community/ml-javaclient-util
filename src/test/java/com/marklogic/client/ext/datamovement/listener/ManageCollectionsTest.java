package com.marklogic.client.ext.datamovement.listener;

import com.marklogic.client.datamovement.DeleteListener;
import com.marklogic.client.datamovement.QueryBatch;
import com.marklogic.client.datamovement.QueryBatchListener;
import com.marklogic.client.ext.AbstractIntegrationTest;
import com.marklogic.client.ext.batch.RestBatchWriter;
import com.marklogic.client.ext.batch.SimpleDocumentWriteOperation;
import com.marklogic.client.ext.datamovement.QueryBatcherTemplate;
import com.marklogic.client.ext.datamovement.UrisQueryQueryBatcherBuilder;
import com.marklogic.client.ext.helper.ClientHelper;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ManageCollectionsTest extends AbstractIntegrationTest {

	private static final String COLLECTION = "modify-query-collections-test";

	@Test
	public void setThenAddThenRemove() {
		String firstUri = "dmsdk-test-1.xml";
		String secondUri = "dmsdk-test-2.xml";

		QueryBatcherTemplate qbt = new QueryBatcherTemplate(newClient("Documents"));
		qbt.setJobName("manage-collections-test");
		qbt.setBatchSize(1);
		qbt.setThreadCount(2);

		qbt.addUrisReadyListeners(new QueryBatchListener() {
			@Override
			public void processEvent(QueryBatch batch) {
				System.out.println("Testing, job batch number: " + batch.getJobBatchNumber() + "; " + batch.getJobTicket().getJobId());
			}
		});

		// Clear out the test documents
		qbt.applyOnDocumentUris(new DeleteListener(), firstUri, secondUri);

		// Insert documents
		RestBatchWriter writer = new RestBatchWriter(client, false);
		writer.write(Arrays.asList(
			new SimpleDocumentWriteOperation(firstUri, "<one/>", COLLECTION),
			new SimpleDocumentWriteOperation(secondUri, "<two/>", COLLECTION)
		));
		writer.waitForCompletion();

		// Set collections
		qbt.applyOnCollections(new SetCollectionsListener(COLLECTION, "red"), COLLECTION);
		assertUriInCollections(firstUri, COLLECTION, "red");
		assertUriInCollections(secondUri, COLLECTION, "red");

		// Add collections
		qbt.applyOnCollections(new AddCollectionsListener("blue", "green"), COLLECTION);
		assertUriInCollections(firstUri, COLLECTION, "red", "blue", "green");
		assertUriInCollections(secondUri, COLLECTION, "red", "blue", "green");

		// Remove collections
		qbt.applyOnCollections(new RemoveCollectionsListener("red", "blue", "green"), COLLECTION);
		assertUriInCollections(firstUri, COLLECTION);
		assertUriInCollections(secondUri, COLLECTION);

		// Set via URI pattern!
		qbt.applyOnUriPattern(new SetCollectionsListener(COLLECTION, "red"), "dmsdk-test-*.xml");
		assertUriInCollections(firstUri, COLLECTION, "red");
		assertUriInCollections(secondUri, COLLECTION, "red");

		qbt.applyOnUriPattern(new RemoveCollectionsListener("red"), "dmsdk-test-2*");
		assertUriInCollections(firstUri, COLLECTION, "red");
		assertUriNotInCollections(secondUri, "red");

		// Set via XQuery URIs query
		String xquery = String.format("cts:document-query(('%s', '%s'))", firstUri, secondUri);
		qbt.applyOnUrisQuery(new SetCollectionsListener(COLLECTION, "green"), xquery);
		assertUriInCollections(firstUri, COLLECTION, "green");
		assertUriInCollections(secondUri, COLLECTION, "green");

		// Set via Javascript URIs query
		String javascript = String.format("cts.documentQuery(['%s', '%s'])", firstUri, secondUri);
		qbt.applyOnUrisQuery(new SetCollectionsListener(COLLECTION, "blue"), javascript);
		assertUriInCollections(firstUri, COLLECTION, "blue");
		assertUriInCollections(secondUri, COLLECTION, "blue");

		// Set via full XQuery query
		xquery = String.format("cts:uris((), (), cts:document-query(('%s', '%s')))", firstUri, secondUri);
		qbt.applyOnUrisQuery(new SetCollectionsListener(COLLECTION, "green"), xquery);
		assertUriInCollections(firstUri, COLLECTION, "green");
		assertUriInCollections(secondUri, COLLECTION, "green");

		// Set via full Javascript query
		javascript = String.format("cts.uris('', null, cts.documentQuery(['%s', '%s']))", firstUri, secondUri);
		qbt.applyOnUrisQuery(new SetCollectionsListener(COLLECTION, "blue"), javascript);
		assertUriInCollections(firstUri, COLLECTION, "blue");
		assertUriInCollections(secondUri, COLLECTION, "blue");

		// Test out a failure listener
		xquery = String.format("cts:document-query(('%s', '%s'))", firstUri, secondUri);
		UrisQueryQueryBatcherBuilder builder = new UrisQueryQueryBatcherBuilder(xquery);
		builder.setWrapQueryIfAppropriate(false); // this will result in a bad query
		qbt.apply(new SetCollectionsListener(COLLECTION, "green"), builder);
		assertUriInCollections(firstUri, COLLECTION, "blue");
		assertUriInCollections(secondUri, COLLECTION, "blue");
	}

	private void assertUriInCollections(String uri, String... collections) {
		ClientHelper clientHelper = new ClientHelper(client);
		List<String> list = clientHelper.getCollections(uri);
		for (String coll : collections) {
			assertTrue(list.contains(coll));
		}
		assertEquals(collections.length, list.size());
	}

	private void assertUriNotInCollections(String uri, String... collections) {
		ClientHelper clientHelper = new ClientHelper(client);
		List<String> list = clientHelper.getCollections(uri);
		for (String coll : collections) {
			assertFalse(list.contains(coll));
		}
	}
}