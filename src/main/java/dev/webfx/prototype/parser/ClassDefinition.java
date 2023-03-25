package dev.webfx.prototype.parser;

import java.util.ArrayList;
import java.util.List;

public class ClassDefinition {
	private AccessType accessType;
    private String className;
    private List<String> imports;
    private List<ClassDefinition> referencedClasses;
    
	/**
	 * @param accessType
	 * @param className
	 * @param imports
	 */
	public ClassDefinition(final AccessType accessType,
			               final String className, 
			               final List<String> imports) {
		this.className = className;
		this.accessType = accessType;
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
	 * @return the accessType
	 */
	public AccessType getAccessType() {
		return accessType;
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
