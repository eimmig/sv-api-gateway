package com.stakevault.betting.gateway.filter;

public class TelegramAccountNotFoundException extends RuntimeException implements LocalizedFilterException {

	public TelegramAccountNotFoundException() {
		super("no auth-service link for the provided telegramUserId");
	}

	@Override
	public String messageKey() {
		return "error.telegram-account-not-found";
	}

	@Override
	public int httpStatusCode() {
		return 404;
	}
}
