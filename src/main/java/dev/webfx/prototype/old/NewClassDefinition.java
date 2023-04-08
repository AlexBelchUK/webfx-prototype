package dev.webfx.prototype.old;

public class NewClassDefinition {
	private String packageName;
	private String className;
	private NewResolvedType resolvedType;
	
	/**
	 * Un resolved class constructor
	 * 
	 * @param className The class name
	 */
	public NewClassDefinition(final String className) {
		this.className = className;
		resolvedType = NewResolvedType.UNRESOLVED;
	}
	
	/**
	 * Fully resolved class constructor
	 * 
	 * @param packageName the package name
	 * @param className the class name
	 */
	public NewClassDefinition(final String packageName,
			                  final String className) {
		this.packageName = packageName;
		this.className = className;
		resolvedType = NewResolvedType.RESOLVED;
	}

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @param packageName the packageName to set
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return the resolvedType
	 */
	public NewResolvedType getResolvedType() {
		return resolvedType;
	}

	/**
	 * @param resolvedType the resolvedType to set
	 */
	public void setResolvedType(NewResolvedType resolvedType) {
		this.resolvedType = resolvedType;
	}
}
