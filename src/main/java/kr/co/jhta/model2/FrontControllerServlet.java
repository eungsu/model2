package kr.co.jhta.model2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.jhta.model2.annotation.RequestMapping;
import kr.co.jhta.model2.constant.HttpMethod;
import kr.co.jhta.model2.utils.ClassScanner;
import kr.co.jhta.model2.vo.Mapping;

public class FrontControllerServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private Map<String, Object> objectMap = new HashMap<String, Object>();
	private Set<Mapping> mappings = new HashSet<Mapping>();
	private String directory;
	
	
	@Override
	public void init() throws ServletException {
		ServletConfig config = getServletConfig();
		directory = config.getInitParameter("jsp-directory"); 
		
		List<Class<?>> classes = getClasses();
		createInstance(classes);
		
		System.out.println();
		System.out.println("------------------------------------------------------------------------------------------------");
		for (Mapping mapping : mappings) {
			System.out.println("전체경로: " + mapping.getFullPath());
			System.out.println("\t요청경로(클래스): " + mapping.getPrefix());
			System.out.println("\t요청경로(메소드): " + mapping.getPath());
			System.out.println("\t요청방식: " + mapping.getMethod().toString());
			System.out.println("\t핸들러메소드: " + mapping.getHandler().getName() + "(request, response)");
		}
		System.out.println("------------------------------------------------------------------------------------------------");
	}
	
	private List<Class<?>> getClasses() throws ServletException {
		ServletConfig config = getServletConfig();
		String basePackage = config.getInitParameter("base-package");
		if (basePackage == null) {
			throw new ServletException("<init-param>의 base-package 초기화 파라미터값이 누락되었습니다.");
		}
		
		List<Class<?>> classes = ClassScanner.scan(basePackage);
		if (classes.isEmpty()) {
			System.err.print("등록된 컨트롤러가 하나도 없습니다.");
		}
		return classes;
	}
	
	private void createInstance(List<Class<?>> classes) throws ServletException {
		for (Class<?> clazz : classes) {
			try {
				Object instance = clazz.getDeclaredConstructor().newInstance();
				parseMapping(clazz, instance);
				objectMap.put(clazz.getName(), instance); 
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
	}
	
	private void parseMapping(Class<?> clazz, Object instance) throws ServletException {
		String prefix = null;
		RequestMapping prefixRequestMapping = clazz.getAnnotation(RequestMapping.class);
		if (prefixRequestMapping != null) {
			prefix = prefixRequestMapping.path();
			if (!prefix.startsWith("/")) {
				throw new ServletException("컨트롤러 클래스["+clazz.getName()+"]의 @RequestMapping에 정의된 경로는 \"/\"로 시작해야 합니다.");
			}
		}
		
		Method[] methods = clazz.getDeclaredMethods();
		for (Method handler : methods) {
			if (handler.isAnnotationPresent(RequestMapping.class)) {
				RequestMapping requestMapping = handler.getAnnotation(RequestMapping.class);
				String path = requestMapping.path();
				if (!path.startsWith("/")) {
					throw new ServletException("컨트롤러 클래스["+clazz.getName()+"] 요청핸들러 메소드["+handler.getName()+"()]의 @RequestMapping에 정의된 경로는 \"/\"로 시작해야 합니다.");
				}
				HttpMethod method = requestMapping.method();
				
				mappings.add(new Mapping(instance, handler, method, prefix, path));
			}
		}
	}
	
	private Mapping getMapping(String requestURI, String method) throws ServletException {
		if (mappings.isEmpty()) {
			throw new ServletException("등록된 매핑정보가 없습니다.");
		}
		for (Mapping mapping : mappings) {
			if (mapping.getFullPath().equals(requestURI) && mapping.getMethod().toString().equals(method)) {
				return mapping;
			}
		}
		throw new ServletException("[요청경로:"+requestURI+"][요청방식:"+method+"]에 맞는 매핑정보가 존재하지 않습니다.");		
	}
	

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestURI = req.getRequestURI();
		String method = req.getMethod();
		String contextPath = req.getContextPath();
		requestURI = requestURI.replace(contextPath, "");
		System.out.println("["+requestURI+"] 요청처리를 시작합니다.");
		
		Mapping mapping = getMapping(requestURI, method);
		Method handler = mapping.getHandler();
	
		try {
			String path = (String) handler.invoke(mapping.getTarget(), req, resp);
			if (path == null) {
				throw new ServletException("컨트롤러 클래스["+mapping.getTarget().getClass().getName()+"] 요청핸들러 메소드["+handler.getName()+"()]의 반환값이 null입니다.");
			}
			
			if (path.startsWith("redirect:")) {
				path = path.replace("redirect:", "");
				resp.sendRedirect(path);
			} else {
				if (directory != null) {
					path = directory + path;
				}
				req.getRequestDispatcher(path).forward(req, resp);
			}
			
		} catch (IllegalAccessException e) {
			throw new ServletException("컨트롤러 클래스["+mapping.getTarget().getClass().getName()+"] 요청핸들러 메소드["+handler.getName()+"()]를 실행할 수 없습니다.");
		} catch (IllegalArgumentException e) {
			throw new ServletException("컨트롤러 클래스["+mapping.getTarget().getClass().getName()+"] 요청핸들러 메소드["+handler.getName()+"()]의 매개변수가 올바르지 않습니다.");
		} catch (InvocationTargetException e) {
			throw new ServletException("컨트롤러 클래스["+mapping.getTarget().getClass().getName()+"] 요청핸들러 메소드["+handler.getName()+"()]를 실행할 수 없습니다.");
		}
		
		System.out.println("["+requestURI+"] 요청처리를 종료합니다.");
		System.out.println();
	}
}
