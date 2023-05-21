package dev.webfx.parse;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexander Belch
 */
public class ProcessorTest {
	
	private final Log log;
	
	private final String userDir;

	private final Processor processor;
	private final PackageResolveDummyCli packageResolveDummyCli;

	/**
	 * Default constructor
	 */
	public ProcessorTest() {
	    log = new Log();
	    log.setLogLevel(LogType.INFO);
	    
		userDir = System.getProperty("user.dir");	
		log.info ("ProcessorTest: user.dir= " + userDir);
	
		packageResolveDummyCli = new PackageResolveDummyCli();		
        processor = new Processor();
		processor.setCliPackageResolveCallback(packageResolveDummyCli);
	}
	
	/**
	 * Run parse and resolve example
	 */
	public void runAllTests () {
		
		// --------Results--------
		// [Info]  packageName: dev.webfx.test1.a
		// [Info]  packageName: dev.webfx.test1.b
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.io
		// [Info]  packageName: java.lang
		// [Info]  packageName: java.util
		// [Info] -----------------------
		// Passes OK
		runTest("/src/test/java/dev/webfx/test1/a/A1Generic.java");
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.b
		// [Info]  packageName: dev.webfx.test1.c
		// [Info] -----------------------
		// Passes OK
		runTest("/src/test/java/dev/webfx/test1/b/B1.java");
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.lang
		// [Info] -----------------------
        // Passes OK
		runTest("/src/test/java/dev/webfx/test1/c/C1Implements.java");
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.a
		// [Info]  packageName: dev.webfx.test1.c
		// [Info] -----------------------
		// Passes OK
		runTest("/src/test/java/dev/webfx/test1/c/C5Extends.java");
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.lang
		// [Info] -----------------------
        // Passes OK
		runTest("/src/test/java/dev/webfx/test1/c/C6BasicClass.java");
				
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test2.a
		// [Info]  packageName: dev.webfx.test2.b
		// [Info]  packageName: dev.webfx.test2.c
		// [Info]  packageName: dev.webfx.test2.r
		// [Info] -----------------------
		// Passes OK
		runTest("/src/test/java/dev/webfx/test2/a/A.java");
	}
	
	/**
	 * Run a single test
	 * 
	 * @param pathFile Source path and file
	 */
	private void runTest(final String pathFile) {
		processor.clearFiles();
		
		final String fullPathFile = userDir + pathFile.replace('/', File.separatorChar);
		processor.addFile(fullPathFile);
		
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
		processorTest.runAllTests();
	}
}
