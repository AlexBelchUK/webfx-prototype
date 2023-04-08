package dev.webfx.parse;

public class PackageClassData {
    private final String packageName;
    private final String className;
    private ResolvedType resolved;
	
    /**
	 * @param packageName
	 * @param className
	 * @param resolved
	 */
	public PackageClassData(final String packageName, 
			                final String className, 
			                final ResolvedType resolved) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.resolved = resolved;
	}

	/**
	 * @return the resolved
	 */
	public ResolvedType getResolved() {
		return resolved;
	}

	/**
	 * @param resolved the resolved to set
	 */
	public void setResolved(final ResolvedType resolved) {
		this.resolved = resolved;
	}

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}
}
