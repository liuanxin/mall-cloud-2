package com.github.common.encrypt.jwt;

/** see : https://github.com/auth0/java-jwt */
public class JWTVerifyException extends Exception {

	private static final long serialVersionUID = -4911506451239107610L;

	public JWTVerifyException() {}

    public JWTVerifyException(String message, Throwable cause) {
		super(message, cause);
	}
	public JWTVerifyException(String message) {
        super(message);
    }
}
