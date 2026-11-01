package com.stakevault.betting.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PasetoClaimsTest {

	private static final long NOW = 1_800_000_000L;

	@Test
	void shouldBeCompleteWhenUserIdTenantIdAndRoleArePresent() {
		PasetoClaims claims = new PasetoClaims("user-1", "tenant-1", "member", NOW, NOW + 3600);

		assertThat(claims.isComplete()).isTrue();
	}

	static Stream<Arguments> incompleteIdentityClaims() {
		return Stream.of(
				Arguments.of(null, "tenant-1", "member"),
				Arguments.of("", "tenant-1", "member"),
				Arguments.of("   ", "tenant-1", "member"),
				Arguments.of("user-1", null, "member"),
				Arguments.of("user-1", "", "member"),
				Arguments.of("user-1", "   ", "member"),
				Arguments.of("user-1", "tenant-1", null),
				Arguments.of("user-1", "tenant-1", ""),
				Arguments.of("user-1", "tenant-1", "   "));
	}

	@ParameterizedTest
	@MethodSource("incompleteIdentityClaims")
	void shouldBeIncompleteWhenAnIdentityClaimIsNullOrBlank(String userId, String tenantId, String role) {
		PasetoClaims claims = new PasetoClaims(userId, tenantId, role, NOW, NOW + 3600);

		assertThat(claims.isComplete()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "-1, true", "0, true", "1, false" })
	void shouldBeExpiredWhenExpIsNotAfterNow(long offsetFromNow, boolean expired) {
		PasetoClaims claims = new PasetoClaims("user-1", "tenant-1", "member", NOW - 3600, NOW + offsetFromNow);

		assertThat(claims.isExpired(NOW)).isEqualTo(expired);
	}
}
