package com.harinitech.springboot_security_jwt_rbac_app1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@ActiveProfiles("test")
class SpringbootSecurityJwtRbacApp1ApplicationTests {

	@MockBean
	private RedisConnectionFactory redisConnectionFactory;

	@Test
	void contextLoads() {
	}
}
