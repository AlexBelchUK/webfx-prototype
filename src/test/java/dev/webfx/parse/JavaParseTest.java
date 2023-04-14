package dev.webfx.parse;
import java.io.IOException;

public class JavaParseTest {
	
	public void runTest () throws IOException {
		String file1 = "C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\a\\A1Generic.java";
	//	String file2 = "C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\b\\B.java";
	//	String file3 = "C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\c\\C.java";
		
		logText("--------Parsing--------");
		final JavaParse javaParse = new JavaParse();
		final ClassDefinitionData classDefinitionData = javaParse.parse(file1);
		logText("-----------------------");
		
		logText("--------Results--------");
		logText("packageName: " + classDefinitionData.getPackageName());
        logText("primaryClassName: " + classDefinitionData.getPrimaryClassName());
		 
	    for (final ImportData importData : classDefinitionData.getImportList()) {
	    	logText(" importType: " + importData.getImportType() + ", importName: " + importData.getImportName());
	    }

		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			logText("   packageName: " + packageClassData.getPackageName() + 
					", className: " + packageClassData.getClassName() +
			        ", resolvedState: " + packageClassData.getResolveState());
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
