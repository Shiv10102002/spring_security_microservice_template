package com.harinitech.springboot_security_jwt_rbac_app1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

//No annotation needed — dependency on classpath auto-activates it in Spring Boot 3
@EnableFeignClients
@SpringBootApplication
@EnableScheduling
public class SpringbootSecurityJwtRbacApp1Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootSecurityJwtRbacApp1Application.class, args);
	}

}
