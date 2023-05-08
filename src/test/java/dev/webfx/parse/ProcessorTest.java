package dev.webfx.parse;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexander Belch
 */
public class ProcessorTest implements PackageResolveCallback {
	
	private final Log log;
	
	private final String userDir;
	
	/**
	 * Default constructor
	 */
	public ProcessorTest() {
	    log = new Log();
	    log.setLogLevel(LogType.INFO);
		userDir = System.getProperty("user.dir");
		
		log.info ("user.dir= " + userDir);
	}
	
	/**
	 * Run parse and resolve example
	 */
	public void runTest () {
		final Processor processor = new Processor();
		processor.setCliPackageResolveCallback(this);

		// --------Results--------
		// [Info]  packageName: dev.webfx.test1.a
		// [Info]  packageName: dev.webfx.test1.b
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.io
		// [Info]  packageName: java.lang
		// [Info]  packageName: java.util
		// [Info] -----------------------
		// Passes OK
		//final String test1FileA = userDir + "/src/test/java/dev/webfx/test1/a/A1Generic.java".replace('/', File.separatorChar);
		//processor.addFile(test1FileA);
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.b
		// [Info]  packageName: dev.webfx.test1.c
		// [Info] -----------------------
		// Passes OK
		//final String test1FileB = userDir + "/src/test/java/dev/webfx/test1/b/B1.java".replace('/', File.separatorChar);
		//processor.addFile(test1FileB);
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.lang
		// [Info] -----------------------
        // Passes OK
		//final String test1FileC1 = userDir + "/src/test/java/dev/webfx/test1/c/C1Implements.java".replace('/', File.separatorChar);
		//processor.addFile(test1FileC1);
		
		//
		// Fails - NPE when using var
		//
		final String test1FileC5 = userDir + "/src/test/java/dev/webfx/test1/c/C5Extends.java".replace('/', File.separatorChar);
		processor.addFile(test1FileC5);
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test2.a
		// [Info]  packageName: dev.webfx.test2.b
		// [Info] -----------------------
		// Fails - [Warn] process: Failed to resolve className= b.getC
		//
		// final String test2FileA = userDir + "/src/test/java/dev/webfx/test2/a/A.java".replace('/', File.separatorChar);
	    // processor.addFile(test2FileA);
				
		final List<String> packageNameList = processor.process();
		
		log.info("--------Results--------");
		 
	    for (final String packageName : packageNameList) {
	    	log.info(" packageName: " + packageName);
	    }
	
	    log.info("-----------------------");
	}
	
	/**
	 * Main entry point
	 * 
	 * @param args Command line arguments - not used
	 * 
	 * @throws IOException Thrown on error
	 */
	public static void main(final String[] args) throws IOException {		
		final ProcessorTest processorTest = new ProcessorTest();
		processorTest.runTest();
	}

	/**
	 * Simulate CLI callback
	 * 
	 * The resolver will make a best attempt at package name, so it may
	 * be that the package and class file does not exist, on returning fail
	 * the resolve may make another call with adjusted package name which then
	 * may work.
	 * 
	 * @param packageName The package name e.g: com.somecompany.abc
	 * @param className The class name eg. SomeClass
	 */
	@Override
	public PackageResolveResult onPackageReolveCallback(String packageName, String className) {
		log.info("onPackageReolveCallback: packageName=" + packageName + ", className=" + className);
		
		final String basePath = userDir + "/src/test/java/".replace('/', File.separatorChar);
	    final String packagePath = packageName.replace('.', File.separatorChar);
	    final String fullPathFile = basePath + packagePath + File.separator + className + ".java";
        
	    final File file = new File(fullPathFile);
        
	    if (file.exists() && file.isFile()) {
	    	// File found - valid java source file
	    	return new PackageResolveResult(true, fullPathFile);
	    }
	    
		// File not found
		return new PackageResolveResult(false, null);
	}
}
