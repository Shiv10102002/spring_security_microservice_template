package com.shiv.springboot_security_jwt_rbac_app1.passwordreset;

import lombok.Data;

//Step 2: Verify OTP & Reset
@Data
public class ResetPasswordRequest {

	private String username;
	private String otp;
	private String newPassword;
}
