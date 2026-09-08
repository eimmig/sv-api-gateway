package com.stakevault.betting.gateway.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import org.springframework.http.MediaType;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class FilterProblemWriter {

	private FilterProblemWriter() {
	}

	static void write(HttpServletResponse response, HttpServletRequest request, ObjectMapper objectMapper,
			int status, String typeSlug, String title, String detail) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		var body = new LinkedHashMap<String, Object>();
		body.put("type", "https://docs/errors/" + typeSlug);
		body.put("title", title);
		body.put("status", status);
		body.put("detail", detail);
		body.put("instance", request.getRequestURI());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
