package com.stakevault.betting.gateway.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every other filter (PasetoAuthenticationFilter/ServiceKeyAuthenticationFilter have
 * no explicit @Order and default to LOWEST_PRECEDENCE) so the correlation id is already in the
 * MDC when those filters log a rejection.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	private static final String CORRELATION_ID_MDC_KEY = "correlationId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}

		response.setHeader(CORRELATION_ID_HEADER, correlationId);
		MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
		try {
			chain.doFilter(new CorrelationIdRequestWrapper(request, correlationId), response);
		} finally {
			MDC.remove(CORRELATION_ID_MDC_KEY);
		}
	}
}
