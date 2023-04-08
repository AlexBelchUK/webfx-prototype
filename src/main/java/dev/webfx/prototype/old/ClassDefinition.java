package dev.webfx.prototype.old;

import java.util.ArrayList;
import java.util.List;

public class ClassDefinition {
    private String className;
    private List<String> imports;
    private List<ClassDefinition> referencedClasses;
    
	/**
	 * @param className
	 * @param imports
	 */
	public ClassDefinition(final String className, 
			               final List<String> imports) {
		this.className = className;
		this.imports = imports;
		referencedClasses = new ArrayList<>();
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the imports
	 */
	public List<String> getImports() {
		return imports;
	}
	
	/**
	 * @return Referenced classes
	 */
	public List<ClassDefinition> getReferencedClasses() {
		return referencedClasses;
	}
}
