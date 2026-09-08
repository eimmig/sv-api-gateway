package com.stakevault.betting.gateway.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CorrelationIdRequestWrapper extends HttpServletRequestWrapper {

	private final String correlationId;

	public CorrelationIdRequestWrapper(HttpServletRequest request, String correlationId) {
		super(request);
		this.correlationId = correlationId;
	}

	@Override
	public String getHeader(String name) {
		if (CorrelationIdFilter.CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
			return correlationId;
		}
		return super.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (CorrelationIdFilter.CORRELATION_ID_HEADER.equalsIgnoreCase(name)) {
			return Collections.enumeration(Set.of(correlationId));
		}
		return super.getHeaders(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
		names.removeIf(CorrelationIdFilter.CORRELATION_ID_HEADER::equalsIgnoreCase);
		names.add(CorrelationIdFilter.CORRELATION_ID_HEADER);
		return Collections.enumeration(names);
	}
}
