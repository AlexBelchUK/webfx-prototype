package dev.webfx.parse;

public interface ResolveCallback {
	 
	/**
	 * Return absolute path string for specified package and class name
	 * Return null if not found
	 * 
	 * @param packageClassName Not notation package and class name
	 * 
	 * @return Path and filename
	 */
	public String getPathFileForPackageClass(final String packageClassName);
	
}
