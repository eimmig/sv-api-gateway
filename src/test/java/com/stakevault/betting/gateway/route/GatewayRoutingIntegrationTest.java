package com.stakevault.betting.gateway.route;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.paseto4j.version4.Paseto;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingIntegrationTest {

	private static final String KEY_HEX = "93f681c1304df4c64f73a0df5c58c7eb097927246ad5b850247a97ef14774bc7";
	private static final SecretKey KEY = new SecretKey(HexFormat.of().parseHex(KEY_HEX), Version.V4);

	private static final AtomicReference<String> lastPath = new AtomicReference<>();
	private static final AtomicReference<String> lastUserIdHeader = new AtomicReference<>();
	private static final AtomicReference<String> lastTenantIdHeader = new AtomicReference<>();
	private static final AtomicReference<Boolean> lastAuthorizationPresent = new AtomicReference<>();
	private static final AtomicReference<Boolean> lastServiceKeyPresent = new AtomicReference<>();
	private static final AtomicReference<Boolean> lastTelegramUserIdPresent = new AtomicReference<>();
	private static final AtomicReference<String> lastCorrelationIdHeader = new AtomicReference<>();

	private static final HttpServer DOWNSTREAM = startDownstream();

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private static HttpServer startDownstream() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", exchange -> {
				lastPath.set(exchange.getRequestURI().getPath());
				lastUserIdHeader.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
				lastTenantIdHeader.set(exchange.getRequestHeaders().getFirst("X-Tenant-Id"));
				lastAuthorizationPresent.set(exchange.getRequestHeaders().containsKey("Authorization"));
				lastServiceKeyPresent.set(exchange.getRequestHeaders().containsKey("X-Service-Key"));
				lastTelegramUserIdPresent.set(exchange.getRequestHeaders().containsKey("X-Telegram-User-Id"));
				lastCorrelationIdHeader.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
				byte[] body;
				if (exchange.getRequestURI().getPath().equals("/api/v1/telegram-accounts/bot-user")) {
					body = "{\"userId\":\"resolved-user\",\"tenantId\":\"resolved-tenant\"}"
							.getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().add("Content-Type", "application/json");
				}
				else {
					body = "{}".getBytes(StandardCharsets.UTF_8);
				}
				exchange.sendResponseHeaders(200, body.length);
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

	@DynamicPropertySource
	static void gatewayProperties(DynamicPropertyRegistry registry) {
		String downstreamUrl = "http://localhost:" + DOWNSTREAM.getAddress().getPort();
		registry.add("gateway.auth-service-url", () -> downstreamUrl);
		registry.add("gateway.bets-service-url", () -> downstreamUrl);
		registry.add("gateway.stats-service-url", () -> downstreamUrl);
	}

	@AfterAll
	static void stopDownstream() {
		DOWNSTREAM.stop(0);
	}

	private static String validToken(String userId, String tenantId) {
		Instant now = Instant.now();
		String json = """
				{"userId":"%s","tenantId":"%s","iat":%d,"exp":%d}"""
				.formatted(userId, tenantId, now.getEpochSecond(), now.plusSeconds(3600).getEpochSecond());
		return Paseto.encrypt(KEY, json, "");
	}

	@Test
	void shouldRouteAuthenticatedRequestToAuthServiceAndInjectResolvedIdentity() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/users"))
				.header("Authorization", "Bearer " + validToken(userId, "acme"))
				.header("X-User-Id", "attacker-supplied")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/users");
		assertThat(lastUserIdHeader.get()).isEqualTo(userId);
		assertThat(lastTenantIdHeader.get()).isEqualTo("acme");
		assertThat(lastAuthorizationPresent.get()).isFalse();
		assertThat(lastCorrelationIdHeader.get()).isNotBlank();
		assertThat(response.headers().firstValue("X-Correlation-Id")).contains(lastCorrelationIdHeader.get());
	}

	@Test
	void shouldRejectUnauthenticatedRequestToProtectedRouteWithoutContactingDownstream() throws Exception {
		lastPath.set(null);
		HttpRequest request = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + "/api/v1/betting-houses")).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(lastPath.get()).isNull();
	}

	@Test
	void shouldRouteLoginRequestWithoutRequiringToken() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/auth/login");
	}

	@Test
	void shouldRouteStatisticsRequestToStatsService() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/statistics"))
				.header("Authorization", "Bearer " + validToken(userId, "acme")).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/statistics");
	}

	@Test
	void shouldRouteServiceKeyAuthenticatedRequestToBetsServiceWithResolvedIdentity() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/bets"))
				.header("X-Service-Key", "test-service-key")
				.header("X-Telegram-User-Id", "bot-user")
				.header("X-User-Id", "attacker-supplied")
				.header("X-Correlation-Id", "service-key-path-correlation-id")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/bets");
		assertThat(lastUserIdHeader.get()).isEqualTo("resolved-user");
		assertThat(lastTenantIdHeader.get()).isEqualTo("resolved-tenant");
		assertThat(lastServiceKeyPresent.get()).isFalse();
		assertThat(lastTelegramUserIdPresent.get()).isFalse();
		assertThat(lastCorrelationIdHeader.get()).isEqualTo("service-key-path-correlation-id");
		assertThat(response.headers().firstValue("X-Correlation-Id")).contains("service-key-path-correlation-id");
	}

	@Test
	void shouldRouteSportsRequestToBetsService() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sports"))
				.header("Authorization", "Bearer " + validToken(userId, "acme")).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/sports");
	}

	@Test
	void shouldRouteLeaguesRequestToBetsService() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/leagues"))
				.header("Authorization", "Bearer " + validToken(userId, "acme")).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/leagues");
	}

	@Test
	void shouldRouteMarketsRequestToBetsService() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/markets"))
				.header("Authorization", "Bearer " + validToken(userId, "acme")).GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/markets");
	}

	@Test
	void shouldRouteTelegramLinksRequestToAuthService() throws Exception {
		String userId = UUID.randomUUID().toString();
		HttpRequest request = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + "/api/v1/telegram-links"))
				.header("Authorization", "Bearer " + validToken(userId, "acme"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(lastPath.get()).isEqualTo("/api/v1/telegram-links");
	}

	@Test
	void shouldGenerateAndForwardCorrelationIdWhenClientDoesNotSendOne() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		String responseCorrelationId = response.headers().firstValue("X-Correlation-Id").orElse(null);
		assertThat(responseCorrelationId).isNotBlank();
		assertThat(lastCorrelationIdHeader.get()).isEqualTo(responseCorrelationId);
	}

	@Test
	void shouldPropagateClientSuppliedCorrelationIdUnchanged() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
				.header("X-Correlation-Id", "client-correlation-id")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.headers().firstValue("X-Correlation-Id")).contains("client-correlation-id");
		assertThat(lastCorrelationIdHeader.get()).isEqualTo("client-correlation-id");
	}
}
