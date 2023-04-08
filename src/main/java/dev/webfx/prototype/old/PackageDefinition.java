package dev.webfx.prototype.old;

import java.util.ArrayList;
import java.util.List;
 
public class PackageDefinition {
	private final String packageName;
	private final List<ClassDefinition> definedClasses;
	
	/**
	 * @param packageName
	 */
	public PackageDefinition(String packageName) {
		this.packageName = packageName;
		definedClasses = new ArrayList<>();
	}

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}
	
	/**
	 * @return the definedClasses
	 */
	public List<ClassDefinition> getDefinedClasses() {
		return definedClasses;
	}
}
