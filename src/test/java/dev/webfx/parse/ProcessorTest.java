package dev.webfx.parse;
import java.io.IOException;
import java.util.List;

public class ProcessorTest implements PackageResolveCallback {
	
	public void runTest () {
		final Processor processor = new Processor();
		processor.setCliPackageResolveCallback(this);
		processor.addFile("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\a\\A1Generic.java");
		processor.addFile("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\a\\B1.java");
		processor.addFile("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\a\\C1Implements.java");
		

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
	 */
	@Override
	public PackageResolveResult onPackageReolveCallback(String packageName, String className) {
		logText("onPackageReolveCallback: packageName=" + packageName + ", className=" + className);
		
		// Not found the file
		return new PackageResolveResult(false, null);
	}
}
