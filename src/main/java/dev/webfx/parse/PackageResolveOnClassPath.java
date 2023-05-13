 package dev.webfx.parse;

/**
 * @author Alexander Belch
 */
public class PackageResolveOnClassPath implements PackageResolveCallback {

	private final Log log;

	/**
	 * Default constructor
	 */
	public PackageResolveOnClassPath() {
		log = new Log();
		log.setLogLevel(LogType.INFO);
	}

	/**
	 * Test if class exists on the class loader path
	 * 
	 * @param packageName The package name
	 * @param className The class name
	 * 
	 * @return Package resolve result
	 */
	@Override
	public PackageResolveResult onPackageResolveCallback(final String packageName,
	                                                     final String className) {

		log.verbose("PackageResolveOnClassPath.onPackageResolveCallback: packageName=" + packageName + ", className=" + className);

		final String packageClassName = packageName + "." + className;
		try {
			if (Class.forName(packageClassName, false, getClass().getClassLoader()) != null) {
				log.verbose("PackageResolveOnClassPath.onPackageResolveCallback: " + 
			                   "resolved=true packageName=" + packageName +
			                   ", className=" + className);
		        return new PackageResolveResult(true, null);
			}
		}
		catch (final ClassNotFoundException cnfe) {
			// Do nothing
		}

		log.verbose("PackageResolveOnClassPath.onPackageResolveCallback: Done.");
		
		return new PackageResolveResult(false, null);
	}

	/**
	 * @return description
	 */
	@Override
	public String onPackgeResolveDescription() {
		return "CLASSPATH";
	}
}
