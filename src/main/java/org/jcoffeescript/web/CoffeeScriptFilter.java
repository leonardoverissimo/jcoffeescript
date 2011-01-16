package org.jcoffeescript.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jcoffeescript.JCoffeeScriptCompileException;
import org.jcoffeescript.JCoffeeScriptCompiler;

public class CoffeeScriptFilter implements Filter {

	private static final int BUFFER_SIZE = 4096;
	

	@Override
	public void init(FilterConfig config) throws ServletException {
		JCoffeeScriptCompiler compiler = new JCoffeeScriptCompiler();
		config.getServletContext().setAttribute("compiler", compiler);
	}
	
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
//		HttpServletResponse response = (HttpServletResponse) resp;
		
		JCoffeeScriptCompiler compiler = (JCoffeeScriptCompiler) request.getSession()
				.getServletContext().getAttribute("compiler");

		String coffeeFilename = discoverCoffeeFilename(request.getPathInfo());
		InputStream coffeeStream = request.getSession().getServletContext().getResourceAsStream(coffeeFilename);
		if (coffeeStream == null) {
			// pass just to show 404 default page
			chain.doFilter(req, resp);
			return;
		}
		
		String source = getContent(coffeeStream);

		try {
			String binary = compiler.compile(source);

			resp.setContentType("text/javascript");
			PrintWriter writer = resp.getWriter();
			writer.write(binary);
			
		} catch (JCoffeeScriptCompileException e) {
			throw new ServletException("Compilation error on file: " + coffeeFilename + " " + e.getMessage(), e);
		}
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
