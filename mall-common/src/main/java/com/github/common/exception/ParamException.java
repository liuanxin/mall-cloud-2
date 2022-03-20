package com.github.common.exception;

import com.github.common.util.A;
import com.google.common.base.Joiner;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/** 自定义参数校验异常 */
@Getter
public class ParamException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final Map<String, String> errorMap;

	public ParamException(String msg) {
		super(msg);
		this.errorMap = Collections.emptyMap();
	}

	public ParamException(String field, String message) {
		super(message);
		this.errorMap = A.maps(field, message);
	}

	public ParamException(Map<String, String> errorMap) {
		super(Joiner.on(",").join(errorMap.values()));
		this.errorMap = errorMap;
	}

	@Override
	public Throwable fillInStackTrace() { return this; }
}
