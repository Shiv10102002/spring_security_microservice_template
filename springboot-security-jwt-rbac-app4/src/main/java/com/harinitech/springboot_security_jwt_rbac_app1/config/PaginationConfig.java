package com.harinitech.springboot_security_jwt_rbac_app1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
@EnableSpringDataWebSupport
public class PaginationConfig {

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer paginationCustomizer() {

		return pageable -> {

			// Default:
			// page=0
			// size=10
			// sort=createdAt,desc
			pageable.setFallbackPageable(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
			// Prevent huge payload attacks
			pageable.setMaxPageSize(100);
			// page starts from 0
			pageable.setFallbackPageable(PageRequest.of(0, 10));

			pageable.setMaxPageSize(100);

			pageable.setOneIndexedParameters(false);
		};
	}
}