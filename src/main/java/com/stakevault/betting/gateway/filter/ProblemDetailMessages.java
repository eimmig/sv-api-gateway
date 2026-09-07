package com.stakevault.betting.gateway.filter;

import java.util.Locale;

import org.springframework.context.MessageSource;

final class ProblemDetailMessages {

	private ProblemDetailMessages() {
	}

	static String title(LocalizedFilterException exception, Locale locale, MessageSource messageSource) {
		return messageSource.getMessage(exception.messageKey() + ".title", exception.messageArgs(), locale);
	}

	static String detail(LocalizedFilterException exception, Locale locale, MessageSource messageSource) {
		return messageSource.getMessage(exception.messageKey() + ".detail", exception.messageArgs(), locale);
	}

	static String typeSlug(LocalizedFilterException exception) {
		return exception.messageKey().replaceFirst("^error\\.", "");
	}
}
