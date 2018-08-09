package com.github.common.encrypt.jwt;

/** see : https://github.com/auth0/java-jwt */
public class JWTIssuerException extends JWTVerifyException {
    private final String issuer;

    public JWTIssuerException(String issuer) {
        this.issuer = issuer;
    }

    public JWTIssuerException(String message, String issuer) {
        super(message);
        this.issuer = issuer;
    }

    public String getIssuer() {
        return issuer;
    }
}
