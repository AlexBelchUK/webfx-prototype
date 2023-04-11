package dev.webfx.parse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainTest {
	
	public void runTest () throws IOException {
		final List<File> fileList = new ArrayList<>();
		fileList.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\a\\A.java"));
		fileList.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\b\\B.java"));
		fileList.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\test\\java\\dev\\webfx\\test\\c\\C.java"));
		
		final List<String> excludedImportList = new ArrayList<>();
		
		logText("--------Parsing--------");
		final JavaParse javaParse = new JavaParse();
		final List<ClassDefinitionData> classDefinitionList = javaParse.parse(fileList, excludedImportList);
		logText("-----------------------");
		
		logText("--------Results--------");
		for (final ClassDefinitionData classDefinitionData : classDefinitionList) {
		    logText("----");
		    logText("packageName: " + classDefinitionData.getPackageName());
		    logText("primaryClassName: " + classDefinitionData.getPrimaryClassName());
		 
		    for (final ImportData importData : classDefinitionData.getImportList()) {
				logText(" importName: " + importData.getImportName());
				logText(" importType: " + importData.getImportType());
		    }
		    
			for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
				logText("    packageName: " + packageClassData.getPackageName());
				logText("    className: " + packageClassData.getClassName());
				logText("    resolvedState: " + packageClassData.getResolveState());
			}
			
			logText ("----");
		}
		logText("-----------------------");
	}
	
	/**
	 * Log text to ouput
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
		final MainTest mainText = new MainTest();
		mainText.runTest();
	}
}
