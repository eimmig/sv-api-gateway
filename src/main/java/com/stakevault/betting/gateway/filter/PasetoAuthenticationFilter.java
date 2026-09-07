package com.stakevault.betting.gateway.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import org.springframework.web.util.UriUtils;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(PasetoAuthenticationFilter.class);

	private static final String BEARER_PREFIX = "Bearer ";

	private final SecretKey localKey;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final ObjectMapper objectMapper;
	private final String actuatorPathPrefix;

	public PasetoAuthenticationFilter(@Value("${paseto.local-key}") String localKeyHex, MessageSource messageSource,
			LocaleResolver localeResolver, ObjectMapper objectMapper,
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
		this.localKey = new SecretKey(HexFormat.of().parseHex(localKeyHex), Version.V4);
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
		this.objectMapper = objectMapper;
		this.actuatorPathPrefix = actuatorBasePath + "/";
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String decodedPath = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
		return decodedPath.startsWith(actuatorPathPrefix);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			writeUnauthorized(request, response);
			return;
		}

		PasetoClaims claims;
		try {
			String token = authorization.substring(BEARER_PREFIX.length());
			String claimsJson = Paseto.decrypt(localKey, token, "");
			claims = objectMapper.readValue(claimsJson, PasetoClaims.class);
		} catch (RuntimeException e) {
			log.warn("Rejecting PASETO token: {}", e.getClass().getSimpleName());
			writeUnauthorized(request, response);
			return;
		}

		if (claims.userId() == null || claims.userId().isBlank() || claims.tenantId() == null
				|| claims.tenantId().isBlank() || claims.exp() <= Instant.now().getEpochSecond()) {
			writeUnauthorized(request, response);
			return;
		}

		chain.doFilter(new ResolvedIdentityRequestWrapper(request, claims.userId(), claims.tenantId()), response);
	}

	private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		InvalidTokenException exception = new InvalidTokenException();
		Locale locale = localeResolver.resolveLocale(request);
		String title = ProblemDetailMessages.title(exception, locale, messageSource);
		String detail = ProblemDetailMessages.detail(exception, locale, messageSource);
		int status = exception.httpStatusCode();

		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		var body = new LinkedHashMap<String, Object>();
		body.put("type", "https://docs/errors/" + ProblemDetailMessages.typeSlug(exception));
		body.put("title", title);
		body.put("status", status);
		body.put("detail", detail);
		body.put("instance", request.getRequestURI());
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
