package dev.webfx.parse;
import java.io.File;
import java.io.IOException;

/**
 * @author Alexander Belch
 */
public class JavaParseTest {
	private final Log log;
	
	/**
	 * Constructor
	 */
	public JavaParseTest() {
		log = new Log();
		log.setLogLevel(LogType.INFO);
	}
	
	/**
	 * Test the java source parser
	 * 
	 * @throws IOException Thrown for invalid file
	 */
	public void runTest () throws IOException {
		final String userDir = System.getProperty("user.dir");
		final String fileA = userDir + "/src/test/java/dev/webfx/test1/a/A1Generic.java".replace('/', File.separatorChar);
		
		log.info("--------Parsing--------");
		final JavaParse javaParse = new JavaParse();
		final ClassDefinitionData classDefinitionData = javaParse.parse(fileA);
		log.info("-----------------------");
		
		log.info("--------Results--------");
		log.info("packageName: " + classDefinitionData.getPackageName());
		log.info("primaryClassName: " + classDefinitionData.getPrimaryClassName());
		 
	    for (final ImportData importData : classDefinitionData.getImportList()) {
	    	log.info(" importType: " + importData.getImportType() + ", importName: " + importData.getImportName());
	    }

		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			log.info("  className: " + packageClassData.getClassName());
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
		final JavaParseTest mainText = new JavaParseTest();
		mainText.runTest();
	}
}
