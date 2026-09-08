package com.stakevault.betting.gateway.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ServiceKeyAuthenticationFilter extends OncePerRequestFilter {

	static final String SERVICE_KEY_HEADER = "X-Service-Key";
	static final String TELEGRAM_USER_ID_HEADER = "X-Telegram-User-Id";

	private static final Logger log = LoggerFactory.getLogger(ServiceKeyAuthenticationFilter.class);

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

	private final String configuredServiceKey;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final ObjectMapper objectMapper;
	private final RestClient authServiceClient;

	public ServiceKeyAuthenticationFilter(@Value("${gateway.service-key}") String configuredServiceKey,
			@Value("${gateway.auth-service-url}") String authServiceUrl, MessageSource messageSource,
			LocaleResolver localeResolver, ObjectMapper objectMapper) {
		this.configuredServiceKey = configuredServiceKey;
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		this.authServiceClient = RestClient.builder().baseUrl(authServiceUrl).requestFactory(requestFactory).build();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getHeader(SERVICE_KEY_HEADER) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String providedKey = request.getHeader(SERVICE_KEY_HEADER);
		if (!constantTimeEquals(providedKey, configuredServiceKey)) {
			writeProblem(request, response, new InvalidServiceKeyException());
			return;
		}

		String telegramUserId = request.getHeader(TELEGRAM_USER_ID_HEADER);
		if (telegramUserId == null || telegramUserId.isBlank()) {
			writeProblem(request, response, new MissingTelegramUserIdException());
			return;
		}

		TelegramAccountLookupResponse link;
		try {
			link = authServiceClient.get()
					.uri("/api/v1/telegram-accounts/{telegramUserId}", telegramUserId)
					.retrieve()
					.body(TelegramAccountLookupResponse.class);
		}
		catch (HttpClientErrorException.NotFound e) {
			writeProblem(request, response, new TelegramAccountNotFoundException());
			return;
		}
		catch (RestClientException e) {
			log.warn("Failed to resolve telegramUserId via auth-service: {}", e.getClass().getSimpleName());
			writeProblem(request, response, new AuthServiceUnavailableException(e));
			return;
		}

		chain.doFilter(new ResolvedIdentityRequestWrapper(request, link.userId(), link.tenantId()), response);
	}

	private boolean constantTimeEquals(String provided, String configured) {
		if (provided == null) {
			return false;
		}
		return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
				configured.getBytes(StandardCharsets.UTF_8));
	}

	private void writeProblem(HttpServletRequest request, HttpServletResponse response,
			LocalizedFilterException exception) throws IOException {
		Locale locale = localeResolver.resolveLocale(request);
		String title = ProblemDetailMessages.title(exception, locale, messageSource);
		String detail = ProblemDetailMessages.detail(exception, locale, messageSource);
		FilterProblemWriter.write(response, request, objectMapper, exception.httpStatusCode(),
				ProblemDetailMessages.typeSlug(exception), title, detail);
	}

	private record TelegramAccountLookupResponse(String userId, String tenantId) {
	}
}
