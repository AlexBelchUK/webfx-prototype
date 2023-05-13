package dev.webfx.parse;

/**
 * @author Alexander Belch
 */
public interface PackageResolveCallback {
	 
	/**
	 * Return absolute path string for specified package and class name
	 * Return null if not found
	 * 
	 * @param packageName package name
	 * @param className Class name
	 * 
	 * @return result of resolving
	 */
	public PackageResolveResult onPackageResolveCallback(final String packageName, 
			                                             final String className);

	/**
	 * Return description of the resolver e.g. class name
	 * 
	 * @return String description
	 */
	public String onPackgeResolveDescription();
}
