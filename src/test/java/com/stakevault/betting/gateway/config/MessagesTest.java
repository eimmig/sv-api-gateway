package com.stakevault.betting.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class MessagesTest {

	private final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

	MessagesTest() {
		messageSource.setBasename("messages");
		messageSource.setFallbackToSystemLocale(false);
		messageSource.setDefaultEncoding("UTF-8");
	}

	@Test
	void shouldResolveSmokeKeyInPtBr() {
		assertThat(messageSource.getMessage("smoke.test", null, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("Mensagem de teste do MessageSource");
	}

	@Test
	void shouldResolveSmokeKeyInEnUs() {
		assertThat(messageSource.getMessage("smoke.test", null, Locale.forLanguageTag("en-US")))
				.isEqualTo("MessageSource test message");
	}

	@Test
	void shouldResolveSmokeKeyInEs() {
		assertThat(messageSource.getMessage("smoke.test", null, Locale.forLanguageTag("es")))
				.isEqualTo("Mensaje de prueba de MessageSource");
	}
}
