package com.stakevault.betting.gateway.route;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class RouteConfig {

	@Bean
	public RouterFunction<ServerResponse> authServiceRoute(@Value("${gateway.auth-service-url}") String authServiceUrl) {
		return route("auth-service")
				.route(path("/api/v1/users/**").or(path("/api/v1/auth/**")).or(path("/api/v1/telegram-links/**")), http())
				.before(uri(authServiceUrl))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> betsServiceRoute(@Value("${gateway.bets-service-url}") String betsServiceUrl) {
		return route("bets-service")
				.route(path("/api/v1/betting-houses/**").or(path("/api/v1/bets/**")).or(path("/api/v1/transactions/**"))
						.or(path("/api/v1/sports/**")).or(path("/api/v1/leagues/**")).or(path("/api/v1/markets/**"))
					.or(path("/api/v1/tipsters/**")), http())
				.before(uri(betsServiceUrl))
				.build();
	}

	@Bean
	public RouterFunction<ServerResponse> statsServiceRoute(@Value("${gateway.stats-service-url}") String statsServiceUrl) {
		return route("stats-service")
				.route(path("/api/v1/statistics/**"), http())
				.before(uri(statsServiceUrl))
				.build();
	}
}
