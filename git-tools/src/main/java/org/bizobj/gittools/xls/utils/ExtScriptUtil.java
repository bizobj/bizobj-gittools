package org.bizobj.gittools.xls.utils;

import java.io.File;
import java.io.IOException;

import org.bizobj.gittools.service.vo.CommitStatInfo;
import org.bizobj.gittools.xls.vo.StatDetailBean;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyObject;

public class ExtScriptUtil {
	/** The class name of extension groovy class */
	private static final String GIT_TOOLS_EXT_METHOD = "run"; 
	
	/**
	 * Build groovy class instance with {@link #GIT_TOOLS_EXT_METHOD} function defined
	 * @param groovySrc
	 * @return
	 */
	public static GroovyObject buildExtInstance(File groovySrc) {
		try {
            //FIXME CompilerConfiguration.DEFAULT use file.encoding overwrite CompilerConfiguration.DEFAULT_SOURCE_ENCODING(UTF-8)
            CompilerConfiguration cfg = new CompilerConfiguration();
            cfg.setSourceEncoding("UTF-8");
           
            GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), cfg);  
            Class<?> groovyClass = classLoader.parseClass(new GroovyCodeSource(groovySrc, "UTF-8"));  
			GroovyObject instance = (GroovyObject)groovyClass.getDeclaredConstructor().newInstance();
			return instance;
		} catch (CompilationFailedException | ReflectiveOperationException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
     * Call  {@link #GIT_TOOLS_EXT_METHOD} function with {@link CommitStatInfo} and {@link StatDetailBean},
     * to customize the extension fields in {@link StatDetailBean}.
	 * @param instance
	 * @param ci
	 * @param si
	 */
	public static void processExtension(GroovyObject instance, CommitStatInfo ci, StatDetailBean si) {
		instance.invokeMethod(GIT_TOOLS_EXT_METHOD, new Object[] {ci, si});
	}
}
