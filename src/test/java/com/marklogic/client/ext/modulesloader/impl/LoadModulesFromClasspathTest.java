package com.marklogic.client.ext.modulesloader.impl;

import com.marklogic.client.ext.AbstractIntegrationTest;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.ext.file.JarDocumentFileReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

import java.util.Set;

/**
 * Program for manually testing loading modules from the classpath instead of from the filesystem. This uses the test
 * jar at ./lib/modules.jar, which is expected to be on the classpath.
 */
public class LoadModulesFromClasspathTest extends AbstractIntegrationTest {

	private DatabaseClient modulesClient;

	@Before
	public void setup() {
		client = newClient("Modules");
		modulesClient = client;
		modulesClient.newServerEval().xquery("cts:uris((), (), cts:true-query()) ! xdmp:document-delete(.)").eval();
	}

	@Test
	public void testInsideJar() {
		assertEquals(0, getUriCountInModulesDatabase());

		AssetFileLoader assetFileLoader = new AssetFileLoader(modulesClient);
		assetFileLoader.setDocumentFileReader(new JarDocumentFileReader());
		DefaultModulesLoader l = new DefaultModulesLoader(assetFileLoader);

		/**
		 * A ModulesManager isn't yet useful because it's used for recording the last-loaded timestamp for files, which
		 * doesn't yet work for classpath resources.
		 */
		l.setModulesManager(null);

		/**
		 * Don't include "classpath:" on this! The method will do it for you. It needs to know the root path within
		 * the classpath that you expect to find your modules.
		 */
		Set<Resource> resources = l.loadModules("classpath*:/ml-modules", new DefaultModulesFinder(), client);

		assertEquals(26, resources.size());
		assertEquals(37, getUriCountInModulesDatabase());
	}

	@Test
	public void testInsideCp() {
		assertEquals(0, getUriCountInModulesDatabase());

		AssetFileLoader assetFileLoader = new AssetFileLoader(modulesClient);
		assetFileLoader.setDocumentFileReader(new JarDocumentFileReader());
		DefaultModulesLoader l = new DefaultModulesLoader(assetFileLoader);

		/**
		 * A ModulesManager isn't yet useful because it's used for recording the last-loaded timestamp for files, which
		 * doesn't yet work for classpath resources.
		 */
		l.setModulesManager(null);

		/**
		 * Don't include "classpath:" on this! The method will do it for you. It needs to know the root path within
		 * the classpath that you expect to find your modules.
		 */
		Set<Resource> resources = l.loadModules("classpath*:/sample-base-dir", new DefaultModulesFinder(), client);

		assertEquals(26, resources.size());
		assertEquals(37, getUriCountInModulesDatabase());
	}

	private int getUriCountInModulesDatabase() {
		return Integer.parseInt(modulesClient.newServerEval().xquery("count(cts:uris((), (), cts:true-query()))").evalAs(String.class));
	}
}
