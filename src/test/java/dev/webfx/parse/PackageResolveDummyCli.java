package dev.webfx.parse;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Belch
 */
public class PackageResolveDummyCli implements PackageResolveCallback {

	private final Log log;

	private final String userDir;

	private final Map<String, PackageResolveResult> packageClassResolveResultLookup;

	/**
	 * Default constructor
	 */
	public PackageResolveDummyCli() {
	    log = new Log();
	    log.setLogLevel(LogType.INFO);
		userDir = System.getProperty("user.dir");
		packageClassResolveResultLookup = new HashMap<>();

		log.info ("user.dir= " + userDir);
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
	public PackageResolveResult onPackageResolveCallback(String packageName, String className) {
		log.verbose("PackageResolveDummyCli.onPackageReolveCallback: packageName=" + packageName + ", className=" + className);

		final String basePath = userDir + "/src/test/java/".replace('/', File.separatorChar);
	    final String packagePath = packageName.replace('.', File.separatorChar);
	    final String fullPathFile = basePath + packagePath + File.separator + className + ".java";        
	    log.verbose("PackageResolveDummyCli.onPackageReolveCallback: fullPathFile=" + fullPathFile);

	    PackageResolveResult packageResolveResult = packageClassResolveResultLookup.get (packageName + ":" + className);
	    if (packageResolveResult != null) {
	    	return packageResolveResult;
	    }

	    final File file = new File(fullPathFile);

	    if (file.exists() && file.isFile()) {
	    	// File found - valid java source file
	    	log.info("PackageResolveDummyCli.onPackageReolveCallback: resolved file=" + file.getAbsolutePath());

	    	packageResolveResult = new PackageResolveResult(true, fullPathFile);
	    	packageClassResolveResultLookup.put (packageName + ":" + className, packageResolveResult);

	    	return packageResolveResult;
	    }

		// File not found
	    log.verbose("PackageResolveDummyCli.onPackageReolveCallback: Not resolved");

	    packageResolveResult = new PackageResolveResult(false, null);
	    packageClassResolveResultLookup.put (packageName + ":" + className, packageResolveResult);

		return packageResolveResult;
	}

	/**
	 * Simulate client description
	 */
	@Override
	public String onPackgeResolveDescription() {		
		return "CLI";
	}
}
