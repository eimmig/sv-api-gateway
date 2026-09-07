package com.stakevault.betting.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;

class PasetoAuthenticationFilterTest {

	private static final String KEY_HEX = "93f681c1304df4c64f73a0df5c58c7eb097927246ad5b850247a97ef14774bc7";
	private static final SecretKey KEY = new SecretKey(HexFormat.of().parseHex(KEY_HEX), Version.V4);

	private final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	private final LocaleResolver localeResolver = mock(LocaleResolver.class);
	private final PasetoAuthenticationFilter filter = new PasetoAuthenticationFilter(
			KEY_HEX, messageSource, localeResolver, new ObjectMapper());

	PasetoAuthenticationFilterTest() {
		messageSource.setBasename("messages");
		messageSource.setFallbackToSystemLocale(false);
		messageSource.setDefaultEncoding("UTF-8");
	}

	private static String validToken(String userId, String tenantId) {
		Instant now = Instant.now();
		String json = """
				{"userId":"%s","tenantId":"%s","iat":%d,"exp":%d}"""
				.formatted(userId, tenantId, now.getEpochSecond(), now.plusSeconds(3600).getEpochSecond());
		return Paseto.encrypt(KEY, json, "");
	}

	private static String expiredToken(String userId, String tenantId) {
		Instant now = Instant.now();
		String json = """
				{"userId":"%s","tenantId":"%s","iat":%d,"exp":%d}"""
				.formatted(userId, tenantId, now.minusSeconds(7200).getEpochSecond(), now.minusSeconds(3600).getEpochSecond());
		return Paseto.encrypt(KEY, json, "");
	}

	private static String tokenWithMissingTenant(String userId) {
		Instant now = Instant.now();
		String json = """
				{"userId":"%s","iat":%d,"exp":%d}"""
				.formatted(userId, now.getEpochSecond(), now.plusSeconds(3600).getEpochSecond());
		return Paseto.encrypt(KEY, json, "");
	}

	@Test
	void shouldPassThroughAndInjectResolvedIdentityForValidToken() throws Exception {
		String userId = UUID.randomUUID().toString();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + validToken(userId, "acme"));
		request.addHeader("X-User-Id", "attacker-supplied");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isInstanceOf(ResolvedIdentityRequestWrapper.class);
		ResolvedIdentityRequestWrapper forwarded = (ResolvedIdentityRequestWrapper) chain.getRequest();
		assertThat(forwarded.getHeader("X-User-Id")).isEqualTo(userId);
		assertThat(forwarded.getHeader("X-Tenant-Id")).isEqualTo("acme");
		assertThat(forwarded.getHeader("Authorization")).isNull();
	}

	@Test
	void shouldReturn401WhenTenantIdClaimIsMissing() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + tokenWithMissingTenant(UUID.randomUUID().toString()));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldPassThroughActuatorPathsWithoutRequiringToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	void shouldReturn401WhenAuthorizationHeaderMissing() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(response.getContentAsString()).contains("\"type\":\"https://docs/errors/invalid-token\"");
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn401WhenAuthorizationHeaderLacksBearerPrefix() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", validToken(UUID.randomUUID().toString(), "acme"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn401WhenTokenIsTampered() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		String tampered = validToken(UUID.randomUUID().toString(), "acme") + "x";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + tampered);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldReturn401WhenTokenIsExpired() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("pt-BR"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + expiredToken(UUID.randomUUID().toString(), "acme"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(chain.getRequest()).isNull();
	}

	@Test
	void shouldLocalizeErrorBodyPerAcceptLanguage() throws Exception {
		when(localeResolver.resolveLocale(any())).thenReturn(Locale.forLanguageTag("en-US"));
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getContentAsString()).contains("Invalid token");
	}
}
