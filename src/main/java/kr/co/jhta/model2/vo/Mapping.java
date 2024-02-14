package kr.co.jhta.model2.vo;

import java.lang.reflect.Method;

import kr.co.jhta.model2.constant.HttpMethod;

public class Mapping {

	private Object target;
	private Method handler;
	private HttpMethod method;
	private String prefix;
	private String path;
	
	public Mapping(Object target, Method handler, HttpMethod method, String prefix, String path) {
		this.target = target;
		this.handler = handler;
		this.method = method;
		this.prefix = prefix;
		this.path = path;
	}

	public Object getTarget() {
		return target;
	}
	
	public Method getHandler() {
		return handler;
	}
	
	public HttpMethod getMethod() {
		return method;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getFullPath() {
		if (prefix != null) {
			return prefix + path;
		}
		return path;
	}

}
