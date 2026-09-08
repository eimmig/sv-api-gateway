package com.stakevault.betting.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void shouldGenerateCorrelationIdWhenAbsent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		String generated = response.getHeader("X-Correlation-Id");
		assertThat(generated).isNotBlank();
		assertThat(chain.getRequest()).isInstanceOf(CorrelationIdRequestWrapper.class);
		assertThat(((CorrelationIdRequestWrapper) chain.getRequest()).getHeader("X-Correlation-Id"))
				.isEqualTo(generated);
	}

	@Test
	void shouldPropagateClientSuppliedCorrelationId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Correlation-Id", "client-supplied-id");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("client-supplied-id");
		assertThat(((CorrelationIdRequestWrapper) chain.getRequest()).getHeader("X-Correlation-Id"))
				.isEqualTo("client-supplied-id");
	}

	@Test
	void shouldGenerateNewCorrelationIdWhenHeaderIsBlank() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Correlation-Id", "   ");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Correlation-Id")).isNotBlank().isNotEqualTo("   ");
	}

	@Test
	void shouldPutCorrelationIdInMdcDuringChainAndRemoveItAfter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
					throws IOException, ServletException {
				assertThat(MDC.get("correlationId")).isNotBlank();
				super.doFilter(servletRequest, servletResponse);
			}
		};

		filter.doFilter(request, response, chain);

		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	void shouldRunForActuatorPaths() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
		assertThat(chain.getRequest()).isInstanceOf(CorrelationIdRequestWrapper.class);
	}
}
