package com.github.common.encrypt.jwt;

/** see : https://github.com/auth0/java-jwt */
public enum Algorithm {
	HS256("HmacSHA256"), HS384("HmacSHA384"), HS512("HmacSHA512"), RS256("RS256"), RS384("RS384"), RS512("RS512");

	private String value;
	Algorithm(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
