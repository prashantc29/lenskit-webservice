package org.grouplens.lenskit.webapp.handler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.grouplens.common.dto.Dto;
import org.grouplens.common.dto.DtoContainer;
import org.grouplens.common.dto.JsonDtoContentHandler;
import org.grouplens.common.dto.XmlDtoContentHandler;
import org.grouplens.lenskit.webapp.ServerUtils.ParsedUrl;
import org.grouplens.lenskit.webapp.ServerUtils.SerializationFormat;
import org.grouplens.lenskit.webapp.RecEngine;
import org.grouplens.lenskit.webapp.RequestContentTypeException;
import org.grouplens.lenskit.webapp.ResponseContentTypeException;
import org.grouplens.lenskit.webapp.Session;

public abstract class RequestHandler {

	public static enum RequestMethod {
		PUT, POST, GET, DELETE
	}
	
	private ObjectArrayList<Resource> expectedResources;
	
	protected RequestHandler() {
		expectedResources = new ObjectArrayList<Resource>();
	}
		
	public abstract RequestMethod getMethod();

	public abstract void handle(Session session, ParsedUrl parsed, HttpServletRequest request, HttpServletResponse response) throws Exception;
	
	public boolean isCorrectHandler(Map<String, String> urlResources) {
		if (expectedResources.size() != urlResources.size()) {
			return false;
		}
		else {
			Iterator<Resource> expected = expectedResources.iterator();
			Iterator<Map.Entry<String, String>> parsed = urlResources.entrySet().iterator();
			while (expected.hasNext()) {
				Resource r = expected.next();
				Map.Entry<String, String> e = parsed.next();
				if (!r.getName().equals(e.getKey())) return false;
				else {
					if (r.requiresValue() && e.getValue() == null) return false;
					else if (!r.requiresValue() && e.getValue() != null) return false;
				}
			}
			return true;
		}
	}
	
	protected void addResource(String name, boolean expectsValue) {
		expectedResources.add(new Resource(name, expectsValue));
	}
	
	private class Resource {
		
		private String name;
		private boolean requiresValue;
		
		public Resource(String name, boolean requiresValue) {
			this.name = name;
			this.requiresValue = requiresValue;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean requiresValue() {
			return requiresValue;
		}
	}
	
	public List<String> getResourceList() {
		ObjectArrayList<String> resList = new ObjectArrayList<String>();
		for (Resource r : expectedResources) {
			resList.add(r.getName());
		}
		return resList;
	}
	
	protected void writeResponse(DtoContainer<? extends Dto> container, HttpServletResponse response, SerializationFormat format) throws IOException, ResponseContentTypeException {
		PrintWriter responseWriter = response.getWriter();
		if (format == SerializationFormat.XML) {
			response.setContentType("application/xml");
			XmlDtoContentHandler handler = new XmlDtoContentHandler();
			responseWriter.println(RecEngine.XML_HEADER);
			handler.toString(container, responseWriter);
		} else if (format == SerializationFormat.JSON || format == SerializationFormat.UNSPECIFIED) {
			response.setContentType("application/json");
			JsonDtoContentHandler handler = new JsonDtoContentHandler();
			handler.toString(container, responseWriter);
		} else {
			throw new ResponseContentTypeException("Client Must Accept JSON or XML");
		}
	}
	
	protected void readRequest(DtoContainer<? extends Dto> container, HttpServletRequest request, SerializationFormat format) throws IOException, RequestContentTypeException {
		BufferedReader requestReader = request.getReader();
		if (format == SerializationFormat.XML) {
			XmlDtoContentHandler handler = new XmlDtoContentHandler();
			handler.fromString(requestReader, container);
		} else if (format == SerializationFormat.JSON) {
			JsonDtoContentHandler handler = new JsonDtoContentHandler();
			handler.fromString(requestReader, container);
		} else {
			throw new RequestContentTypeException("Request Must be JSON or XML");
		}
	}
}
