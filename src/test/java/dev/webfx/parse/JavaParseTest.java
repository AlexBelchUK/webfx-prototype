package dev.webfx.parse;
import java.io.File;
import java.io.IOException;

/**
 * @author Alexander Belch
 */
public class JavaParseTest {
	
	/**
	 * Test the java source parser
	 * 
	 * @throws IOException Thrown for invalid file
	 */
	public void runTest () throws IOException {
		final String userDir = System.getProperty("user.dir");
		final String fileA = userDir + "/src/test/java/dev/webfx/test1/a/A1Generic.java".replace('/', File.separatorChar);
		
		logText("--------Parsing--------");
		final JavaParse javaParse = new JavaParse();
		final ClassDefinitionData classDefinitionData = javaParse.parse(fileA);
		logText("-----------------------");
		
		logText("--------Results--------");
		logText("packageName: " + classDefinitionData.getPackageName());
        logText("primaryClassName: " + classDefinitionData.getPrimaryClassName());
		 
	    for (final ImportData importData : classDefinitionData.getImportList()) {
	    	logText(" importType: " + importData.getImportType() + ", importName: " + importData.getImportName());
	    }

		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			logText("  className: " + packageClassData.getClassName());
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
		final JavaParseTest mainText = new JavaParseTest();
		mainText.runTest();
	}
}
