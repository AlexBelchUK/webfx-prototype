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
	
	/**
	 * Default constructor
	 */
	public ProcessorTest() {
	    log = new Log();
	    log.setLogLevel(LogType.INFO);
		userDir = System.getProperty("user.dir");
		
		log.info ("ProcessorTest: user.dir= " + userDir);
	}
	
	/**
	 * Run parse and resolve example
	 */
	public void runTest () {
		final Processor processor = new Processor();

		final PackageResolveDummyCli packageResolveDummyCli = new PackageResolveDummyCli();		
		processor.setCliPackageResolveCallback(packageResolveDummyCli);

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
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.a
		// [Info]  packageName: dev.webfx.test1.c
		// [Info] -----------------------
		// Passes OK
		//final String test1FileC5 = userDir + "/src/test/java/dev/webfx/test1/c/C5Extends.java".replace('/', File.separatorChar);
		//processor.addFile(test1FileC5);
		
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test1.c
		// [Info]  packageName: java.lang
		// [Info] -----------------------
        // Passes OK
		// final String test1FileC6 = userDir + "/src/test/java/dev/webfx/test1/c/C6BasicClass.java".replace('/', File.separatorChar);
		//processor.addFile(test1FileC6);
				
		// [Info] --------Results--------
		// [Info]  packageName: dev.webfx.test2.a
		// [Info]  packageName: dev.webfx.test2.b
		// [Info]  packageName: dev.webfx.test2.c
		// [Info]  packageName: dev.webfx.test2.r
		// [Info] -----------------------
		// Passes OK
		final String test2FileA = userDir + "/src/test/java/dev/webfx/test2/a/A.java".replace('/', File.separatorChar);
	    processor.addFile(test2FileA);
	    		
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
}
