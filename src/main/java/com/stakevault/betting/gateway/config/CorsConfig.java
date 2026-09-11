package com.stakevault.betting.gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Roda como Filter de servlet puro (nao WebMvcConfigurer#addCorsMappings) porque as rotas do
 * Spring Cloud Gateway MVC sao RouterFunction, nao @Controller - nao passam pela
 * RequestMappingHandlerMapping que addCorsMappings decora. Precisa vir antes de
 * PasetoAuthenticationFilter/ServiceKeyAuthenticationFilter: o preflight OPTIONS nao carrega
 * Authorization/X-Service-Key, entao se qualquer um dos dois rodar primeiro rejeita o preflight
 * antes do CorsFilter conseguir responder. @Order direto no @Bean NAO basta pra isso (achado
 * real, provado pelo teste de integracao falhando com 401 antes desta correcao) - o registro de
 * Filter do Spring Boot so respeita ordem via FilterRegistrationBean.setOrder, nao via @Order na
 * classe/metodo do bean Filter em si.
 */
@Configuration
public class CorsConfig {

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		// Sem credentials: token PASETO vai no header Authorization (localStorage no web, ver
		// apps/web/src/app/core/auth.ts), nunca cookie - allowCredentials nao se aplica aqui.

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
