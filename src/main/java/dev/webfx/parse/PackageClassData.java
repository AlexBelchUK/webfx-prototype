package dev.webfx.parse;

/**
 * @author Alexander Belch
 */
public class PackageClassData {
    private String packageName;
    private String className;
    private boolean resolved; 
	
    /**
     * Parameter constructor
     * 
	 * @param packageName
	 * @param className
	 * @param resolved
	 */
	public PackageClassData(final String packageName, 
			                final String className, 
			                final boolean resolved) {
		this.packageName = packageName;
		this.className = className;
		this.resolved = resolved;
	}

	/**
	 * @return the resolved true of false state
	 */
	public boolean  isResolved() {
		return resolved;
	}

	/**
	 * @param resolved Set the resolved state true or false to set
	 */
	public void setResolved(final boolean resolved) {
		this.resolved = resolved;
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
