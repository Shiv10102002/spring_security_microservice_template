package com.shiv.springboot_security_jwt_rbac_app1.passwordreset;

import lombok.Data;

//Step 1: Request OTP
@Data
public class ForgotPasswordRequest {

	private String username;
}
