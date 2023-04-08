package dev.webfx.prototype.old;

import java.util.ArrayList;
import java.util.List;

public class NewPackageDefinition {

	private final String packageName;
	private final List<String> imports;
	private final List<NewClassDefinition> definedClasses;
	private final List<NewClassDefinition> referencedClasses;
	
	public NewPackageDefinition(final String packageName) {
		this.packageName = packageName;
		
		imports = new ArrayList<>();
		definedClasses = new ArrayList<>();
		referencedClasses = new ArrayList<>();
	}

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Add import name to list if not present
	 * 
	 * @param importName The import string
	 */
	public void addImport(final String importName) {
		if (! imports.contains(importName)) {
			imports.add(importName);
		}
	}
	
	/**
	 * @return the imports
	 */
	public List<String> getImports() {
		return imports;
	}
	
	/**
	 * Add defined class if not present in the list
	 * automatically add the package name
	 * 
	 * @param className The defined class name
	 */
	public void addDefinedClass(final String className) {
		
		for (final NewClassDefinition classDefinition : definedClasses) {
			if (classDefinition.getClassName().equals(className)) {
				return;
			}
		}
		
		definedClasses.add(new NewClassDefinition(packageName, className));
	}

	/**
	 * @return the definedClasses
	 */
	public List<NewClassDefinition> getDefinedClasses() {
		return definedClasses;
	}
	
	/**
	 * Add referenced class to the list of referred classes
	 * 
	 * @param className The class name to add
	 */
	public void addReferencedClass(final String className) {
		for (final NewClassDefinition classDefinition : referencedClasses) {
			if (classDefinition.getClassName().equals(className)) {
				return;
			}
		}
		
		// If the class has '.'s assume it is resolved and split into parts
		// assume class names start with upper case letter
		String newPackageName = null;
		String newClassName = className;
		
		final int index = className.lastIndexOf('.');
		if (index >= 0) {
		    for (int i = 0; i < className.length(); i++) {
		    	if (className.charAt(i) == '.' && 
		    		Character.isUpperCase(className.charAt(i + 1))) {
		    		newPackageName = className.substring(0, i);
		    		newClassName = className.substring(i + 1);
		    		break;
		    	}
		    }
		}
		
		final NewClassDefinition classDefinition;
		if (packageName != null) {
			classDefinition = new NewClassDefinition(newPackageName, newClassName);
		}
		else {
			classDefinition = new NewClassDefinition(newClassName);
		}
		
		referencedClasses.add(classDefinition);
	}

	/**
	 * @return the referencedClasses
	 */
	public List<NewClassDefinition> getReferencedClasses() {
		return referencedClasses;
	}
}
