package org.jcoffeescript.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jcoffeescript.JCoffeeScriptCompileException;
import org.jcoffeescript.JCoffeeScriptCompiler;

public class CoffeeScriptFilter implements Filter {

	private static final int BUFFER_SIZE = 4096;
	private static final int MAX_COMPILED_JS = 100;
	

	@SuppressWarnings("serial")
	@Override
	public void init(FilterConfig config) throws ServletException {
		// coffeescript compiler
		JCoffeeScriptCompiler compiler = new JCoffeeScriptCompiler();
		config.getServletContext().setAttribute("compiler", compiler);
		
		// hashmap
		LinkedHashMap<String, String> previousBinaries = new LinkedHashMap<String, String>() {

			@Override
			protected boolean removeEldestEntry(Entry<String, String> eldest) {
				return size() > MAX_COMPILED_JS;
			}
		};
		config.getServletContext().setAttribute("previousBinaries", Collections.synchronizedMap(previousBinaries));
	}
	
	
	@Override
	public void destroy() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
//		HttpServletResponse response = (HttpServletResponse) resp;
		ServletContext servletContext = request.getSession().getServletContext();
		
		JCoffeeScriptCompiler compiler = (JCoffeeScriptCompiler) servletContext.getAttribute("compiler");

		String javascriptURI = getJavascriptURI(request);
		String coffeeFilename = discoverCoffeeFilename(javascriptURI);
		
		Map<String, String> previousBinaries = (Map<String, String>) servletContext.getAttribute("previousBinaries");
		
		String binary;
		if ((binary = previousBinaries.get(coffeeFilename)) == null) {
			
			InputStream coffeeStream = servletContext.getResourceAsStream(coffeeFilename);
			if (coffeeStream == null) {
				// pass just to show 404 default page
				chain.doFilter(req, resp);
				return;
			}
			
			String source = getContent(coffeeStream);

			try {
				synchronized (compiler) {
					binary = compiler.compile(source);
				}
			} catch (JCoffeeScriptCompileException e) {
				throw new ServletException("Compilation error on file: " + coffeeFilename + " " + e.getMessage(), e);
			}
		}
		
		// write in all cases to put value at the end of the linked list
		previousBinaries.put(coffeeFilename, binary);
		
		resp.setContentType("text/javascript");
		PrintWriter writer = resp.getWriter();
		writer.write(binary);
	}
	
	private String getJavascriptURI(HttpServletRequest request) {
		String javascriptURI = request.getServletPath();
		if (request.getPathInfo() != null) {
			javascriptURI += request.getPathInfo();
		}
		return javascriptURI;
	}

	private String discoverCoffeeFilename(String javascriptResource) {

		Pattern pattern = Pattern.compile("/js/(.*).js");
		Matcher matcher = pattern.matcher(javascriptResource);
		if (!matcher.matches()) {
			return null;
		}
		return "/WEB-INF/coffee/" + matcher.group(1) + ".coffee";
	}

	private String getContent(InputStream coffeeStream) throws IOException {
		InputStreamReader reader = new InputStreamReader(coffeeStream);
		try {
			StringWriter writer = new StringWriter();

			char[] buffer = new char[BUFFER_SIZE];
			int n = 0;
			while (-1 != (n = reader.read(buffer))) {
				writer.write(buffer, 0, n);
			}
			String source = writer.toString();
			return source;
		} finally {
			reader.close();
		}
	}

}
