package com.harinitech.springboot_security_jwt_rbac_app1.model;


import java.util.Date;

import lombok.Data;

@Data
public class TokenInfo {
	
    private String token;
    private Date expiration;

}
