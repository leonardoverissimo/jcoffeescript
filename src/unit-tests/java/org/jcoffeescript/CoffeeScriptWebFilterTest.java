/*
 * Copyright 2011 Leonardo Verissimo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jcoffeescript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jcoffeescript.web.CoffeeScriptFilter;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoffeeScriptWebFilterTest {
	
	@BeforeClass
	public static void setUp() throws Exception {
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(9012);
        server.addConnector(connector);
		
		// setting context
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
		context.addFilter(CoffeeScriptFilter.class, "*.js", FilterMapping.DEFAULT);
		context.addServlet(DefaultServlet.class, "/*");
		context.setResourceBase("target/classes/unit-tests/app");
		
        server.setHandler(context);
        
		server.start();
	}
	
	@Test
	public void shouldReturnCompiledJSResource() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		
		GetMethod method = new GetMethod("http://localhost:9012/js/simple.js");
		int response = httpClient.executeMethod(method);
		
		assertEquals(200, response);
		assertTrue(method.getResponseBodyAsString().startsWith("(function() {\n"));
		assertTrue(method.getResponseBodyAsString().endsWith("\n}).call(this);\n"));
	}
	
	@Test
	public void shouldReturn404() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		
		GetMethod method = new GetMethod("http://localhost:9012/js/notfound.js");
		int response = httpClient.executeMethod(method);
		
		assertEquals(404, response);
	}
	
	@Test
	public void shouldReturnAlreadyExistingJavascript() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		
		GetMethod method = new GetMethod("http://localhost:9012/js/static.js");
		int response = httpClient.executeMethod(method);
		
		assertEquals(200, response);
	}
	
	@Test
	public void shouldReturnError() throws Exception {
		
		HttpClient httpClient = new HttpClient();
		
		GetMethod method = new GetMethod("http://localhost:9012/js/error.js");
		int response = httpClient.executeMethod(method);
		
		assertEquals(500, response);
	}
}
