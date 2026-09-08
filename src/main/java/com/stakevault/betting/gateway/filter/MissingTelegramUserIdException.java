package com.stakevault.betting.gateway.filter;

public class MissingTelegramUserIdException extends RuntimeException implements LocalizedFilterException {

	public MissingTelegramUserIdException() {
		super("missing X-Telegram-User-Id header");
	}

	@Override
	public String messageKey() {
		return "error.missing-telegram-user-id";
	}

	@Override
	public int httpStatusCode() {
		return 401;
	}
}
