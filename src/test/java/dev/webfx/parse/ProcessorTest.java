package dev.webfx.parse;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexander Belch
 */
public class ProcessorTest implements PackageResolveCallback {
	
	private final String userDir;
	
	/**
	 * Default constructor
	 */
	public ProcessorTest() {
		userDir = System.getProperty("user.dir");
		
		System.out.println ("user.dir= " + userDir);
	}
	
	/**
	 * Run parse and resolve example
	 */
	public void runTest () {
		final Processor processor = new Processor();
		processor.setCliPackageResolveCallback(this);

		final String fileA = userDir + "/src/test/java/dev/webfx/test1/a/A1Generic.java".replace('/', File.separatorChar);
		processor.addFile(fileA);
		
		//final String fileB = userDir + "/src/test/java/dev/webfx/test1/b/B1.java".replace('/', File.separatorChar);
		//processor.addFile(fileB);
		
		//final String fileC = userDir + "/src/test/java/dev/webfx/test1/c/C1Implements.java".replace('/', File.separatorChar);
		//processor.addFile(fileC);
		
		//final String fileZ = userDir + "/src/test/java/dev/webfx/test2/a/A.java".replace('/', File.separatorChar);
	    //processor.addFile(fileZ);
				
		final List<String> packageNameList = processor.process();
		
		logText("--------Results--------");
		 
	    for (final String packageName : packageNameList) {
			logText(" packageName: " + packageName);
	    }
	
		logText("-----------------------");
	}
	
	/**
	 * Log text to output
	 * 
	 * @param text Text to log
	 */
	private void logText(final String text) {
		System.out.println(text);
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
		logText("onPackageReolveCallback: packageName=" + packageName + ", className=" + className);
		
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
