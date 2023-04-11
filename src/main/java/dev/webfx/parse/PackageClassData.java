package dev.webfx.parse;

public class PackageClassData {
    private String packageName;
    private String className;
    private ResolveState resolveState; 
	
    /**
     * Parameter constructor
     * 
	 * @param packageName
	 * @param className
	 * @param resolveState
	 */
	public PackageClassData(final String packageName, 
			                final String className, 
			                final ResolveState resolveState) {
		this.packageName = packageName;
		this.className = className;
		this.resolveState = resolveState;
	}

	/**
	 * @return the resolveState
	 */
	public ResolveState getResolveState() {
		return resolveState;
	}

	/**
	 * @param resolveState the resolve state to set
	 */
	public void setResolveState(final ResolveState resolveState) {
		this.resolveState = resolveState;
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
}
