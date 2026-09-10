package com.stakevault.betting.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;

class ServiceKeyAuthenticationFilterTest {

	private static final String CONFIGURED_KEY = "configured-service-key";

	private static final HttpServer AUTH_SERVICE_STUB = startAuthServiceStub();

	private final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	private final LocaleResolver localeResolver = mock(LocaleResolver.class);
	private final ServiceKeyAuthenticationFilter filter = new ServiceKeyAuthenticationFilter(CONFIGURED_KEY,
			"http://localhost:" + AUTH_SERVICE_STUB.getAddress().getPort(), messageSource, localeResolver,
			new ObjectMapper());

	ServiceKeyAuthenticationFilterTest() {
		messageSource.setBasename("messages");
		messageSource.setFallbackToSystemLocale(false);
		messageSource.setDefaultEncoding("UTF-8");
	}

	private static HttpServer startAuthServiceStub() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/api/v1/telegram-accounts/", exchange -> {
				String telegramUserId = exchange.getRequestURI().getPath()
						.substring("/api/v1/telegram-accounts/".length());
				byte[] body;
				int status;
				if ("linked-user".equals(telegramUserId)) {
					status = 200;
					body = "{\"userId\":\"u1\",\"tenantId\":\"acme\"}".getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().add("Content-Type", "application/json");
				}
				else if ("unlinked-user".equals(telegramUserId)) {
					status = 404;
					body = new byte[0];
				}
				else {
					status = 500;
					body = new byte[0];
				}
				exchange.sendResponseHeaders(status, body.length);
				exchange.getResponseBody().write(body);
				exchange.close();
			});
			server.start();
			return server;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@AfterAll
	static void stopAuthServiceStub() {
		AUTH_SERVICE_STUB.stop(0);
	}

	@Test
	void shouldNotFilterWhenServiceKeyHeaderAbsent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void shouldPassThroughAndInjectResolvedIdentityWhenServiceKeyAndTelegramUserIdValid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		request.addHeader("X-Service-Key", CONFIGURED_KEY);
		request.addHeader("X-Telegram-User-Id", "linked-user");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isInstanceOf(ResolvedIdentityRequestWrapper.class);
		ResolvedIdentityRequestWrapper forwarded = (ResolvedIdentityRequestWrapper) chain.getRequest();
		assertThat(forwarded.getHeader("X-User-Id")).isEqualTo("u1");
		assertThat(forwarded.getHeader("X-Tenant-Id")).isEqualTo("acme");
		// No role concept on this path (telegram-integration) - no admin route reachable via the bot.
		assertThat(forwarded.getHeader("X-User-Role")).isNull();
	}

	@Test
	void shouldReturn401WhenServiceKeyHeaderDoesNotMatch() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		request.addHeader("X-Service-Key", "wrong-key");
		request.addHeader("X-Telegram-User-Id", "linked-user");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(response.getContentAsString()).contains("\"type\":\"https://docs/errors/invalid-service-key\"");
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn401WhenTelegramUserIdHeaderMissing() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		request.addHeader("X-Service-Key", CONFIGURED_KEY);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString())
				.contains("\"type\":\"https://docs/errors/missing-telegram-user-id\"");
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn404WhenTelegramAccountNotLinked() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		request.addHeader("X-Service-Key", CONFIGURED_KEY);
		request.addHeader("X-Telegram-User-Id", "unlinked-user");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getContentAsString())
				.contains("\"type\":\"https://docs/errors/telegram-account-not-found\"");
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn503WhenAuthServiceCallFails() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bets");
		request.addHeader("X-Service-Key", CONFIGURED_KEY);
		request.addHeader("X-Telegram-User-Id", "broken-user");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(response.getContentAsString())
				.contains("\"type\":\"https://docs/errors/auth-service-unavailable\"");
		assertThat(chain.getRequest()).isNull();
	}
}
