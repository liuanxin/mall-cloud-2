package com.github.common.exception;

import com.google.common.base.Joiner;

import java.util.Collections;
import java.util.Map;

/** 参数校验 */
public class ParamException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final Map<String, String> errorMap;

	public ParamException(String msg) {
		super(msg);
		this.errorMap = Collections.emptyMap();
	}

	public ParamException(Map<String, String> errorMap) {
		super(Joiner.on(",").join(errorMap.values()));
		this.errorMap = errorMap;
	}

	public Map<String, String> getErrorMap() {
		return errorMap;
	}

	@Override
	public Throwable fillInStackTrace() { return this; }
}
