package com.stakevault.betting.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ResolvedIdentityRequestWrapperTest {

	@Test
	void shouldReturnResolvedUserIdRegardlessOfOriginalHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "client-supplied");

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(wrapper.getHeader("X-User-Id")).isEqualTo("resolved-user");
	}

	@Test
	void shouldReturnResolvedTenantIdWhenOriginalHeaderAbsent() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(wrapper.getHeader("X-Tenant-Id")).isEqualTo("resolved-tenant");
	}

	@Test
	void shouldPassThroughUnrelatedHeaders() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept-Language", "en-US");

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(wrapper.getHeader("Accept-Language")).isEqualTo("en-US");
	}

	@Test
	void shouldReflectResolvedValuesInGetHeadersAndHeaderNames() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "client-supplied");

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(Collections.list(wrapper.getHeaders("X-User-Id"))).containsExactly("resolved-user");
		List<String> names = Collections.list(wrapper.getHeaderNames());
		assertThat(names).contains("X-User-Id", "X-Tenant-Id");
	}

	@Test
	void shouldNeverForwardTheOriginalAuthorizationToken() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer v4.local.something");

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(wrapper.getHeader("Authorization")).isNull();
		assertThat(Collections.list(wrapper.getHeaders("Authorization"))).isEmpty();
		assertThat(Collections.list(wrapper.getHeaderNames())).doesNotContain("Authorization");
	}

	@Test
	void shouldNeverForwardTheServiceKeyOrTelegramUserIdHeaders() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Service-Key", "shared-secret");
		request.addHeader("X-Telegram-User-Id", "12345");

		ResolvedIdentityRequestWrapper wrapper = new ResolvedIdentityRequestWrapper(request, "resolved-user", "resolved-tenant");

		assertThat(wrapper.getHeader("X-Service-Key")).isNull();
		assertThat(wrapper.getHeader("X-Telegram-User-Id")).isNull();
		assertThat(Collections.list(wrapper.getHeaders("X-Service-Key"))).isEmpty();
		assertThat(Collections.list(wrapper.getHeaderNames())).doesNotContain("X-Service-Key", "X-Telegram-User-Id");
	}
}
