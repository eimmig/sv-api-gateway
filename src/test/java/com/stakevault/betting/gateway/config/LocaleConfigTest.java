package com.stakevault.betting.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

class LocaleConfigTest {

	private final LocaleResolver resolver = new LocaleConfig().localeResolver();

	@Test
	void shouldResolvePtBrWithoutAcceptLanguage() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag("pt-BR"));
	}

	@Test
	void shouldResolveSameLocaleWhenSupported() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", "en-US");

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag("en-US"));
	}

	@Test
	void shouldFallBackToPtBrWhenUnsupported() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", "fr-FR");

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag("pt-BR"));
	}
}
