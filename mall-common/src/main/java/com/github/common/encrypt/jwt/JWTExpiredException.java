package com.github.common.encrypt.jwt;

/** see : https://github.com/auth0/java-jwt */
public class JWTExpiredException extends JWTVerifyException {
    private long expiration;

    public JWTExpiredException(long expiration) {
        this.expiration = expiration;
    }

    public JWTExpiredException(String message, long expiration) {
        super(message);
        this.expiration = expiration;
    }

    public long getExpiration() {
        return expiration;
    };
}
