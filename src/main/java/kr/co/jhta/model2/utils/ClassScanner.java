package kr.co.jhta.model2.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.ServletException;

public class ClassScanner {

	public static List<Class<?>> scan(String basePackage) throws ServletException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String path = basePackage.replace('.', '/');
		
		List<Class<?>> classes = new ArrayList<Class<?>>();
		try {
			List<File> files = new ArrayList<File>();
			Enumeration<URL> resources = classLoader.getResources(path);
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				files.add(new File(resource.getFile()));
			}
			for (File file : files) {
				if (file.isDirectory()) {
					classes.addAll(findClasses(file, basePackage));
				}
			}
		} catch (IOException e) {
			throw new ServletException(e);
		}
		return classes;
	}
	
	private static List<Class<?>> findClasses(File directory, String packageName) {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (!directory.exists()) {
			return classes;
		}
		
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				classes.addAll(findClasses(file,  packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				try {
					classes.add(Class.forName(className, false, classLoader));
				} catch (ClassNotFoundException e) {}
			}
		}
		return classes;
	}
}
