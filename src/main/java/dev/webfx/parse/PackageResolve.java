package dev.webfx.parse;

import java.util.List;

// @TODO
//  a. List filter to fetch only unresolved items
//  b. Keep list of items still to resolve to reduce unnecessary loops
//  c. JUnit tests to test each method 

public class PackageResolve {
	
	private static final String JAVA_LANG_PACKAGE_NAME = "java.lang";
	
	private PackageResolveCallback cliPackageResolveCallback;
	
	/**
	 * Set resolve callback for CLI interface
	 * 
	 * @param cliPackageResolveCallback
	 */
	public void setCliPackageResolveCallback(final PackageResolveCallback cliPackageResolveCallback) {
		this.cliPackageResolveCallback = cliPackageResolveCallback;
	}
	
	/**
	 * Resolve a class definition, get package name for objects found 
	 * in the class definition
	 * 
	 * @param classDefinitionData
	 * @param pathFileList
	 */
	public void resolve(final ClassDefinitionData classDefinitionData,
			            final List<String> pathFileList) {
		
		resolvePackagesForPackageAndClassNameUseExternalClasses(classDefinitionData,
				                                                this::resolvePackageOnClassPath,
				                                                pathFileList);
		
		resolvePackagesForClassNameUseClassNameImports(classDefinitionData);
		
		resolvePackagesForClassNameUseExternalWildcardImports(classDefinitionData,
				                                      this::resolvePackageOnClassPath,
				                                      pathFileList);
		
		resolvePackagesForClassNameUseClassDefinition(classDefinitionData);
		
		resolvePackagesForClassNameUseDefaultPackage(classDefinitionData);
		
	    if (cliPackageResolveCallback != null) {
	    	resolvePackagesForPackageAndClassNameUseExternalClasses(classDefinitionData,
                                                                    cliPackageResolveCallback,
                                                                    pathFileList);
	    	
	    	resolvePackagesForClassNameUseExternalWildcardImports(classDefinitionData,
                                                                  cliPackageResolveCallback,
                                                                  pathFileList);
	    }	    
	}

	/**
	 * Case where the class name contains '.' as it is
     * fully resolved to start with, need to determine the
     * package and class name part allowing for nested inner classes
	 * 
	 * @param classDefinitionData
	 * @param packageResolveCallback
	 * @param pathFileList
	 */
	private void resolvePackagesForPackageAndClassNameUseExternalClasses(final ClassDefinitionData classDefinitionData,
			                                                             final PackageResolveCallback packageResolveCallback,
			                                                             final List<String> pathFileList) {

		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
		    if (! packageClassData.isResolved() &&
		    	packageClassData.getClassName().contains(".")) {
		           
			    // Split into parts gradually build up string working
			   	// forwards until a class found - the primary class
			   	// ignore any inner class after that
			   	final String[] packageClassParts = packageClassData.getClassName().split(".");
			   	for (int i = 0; i < packageClassParts.length; i++) {    
		    		final String packageName = buildPackageNameFromParts(packageClassParts, i);
		    		final PackageResolveResult result = packageResolveCallback.onPackageReolveCallback(packageName, packageClassParts[i]);
		    		if (result.isSuccess()) {
		    			addUniquePathFileToList(result.getPathFile(), pathFileList);
		    			
			    		packageClassData.setPackageName(packageName);
			    		
			    		final String className = buildClassNameFromParts(packageClassParts, i);
        	        	packageClassData.setClassName(className);

			    		packageClassData.setResolved(true);
			    		break;
			   		}
			   	}
		    }
		}
	}

	/**
	 * Append unique path and file name to list of paths and filenames
	 * 
	 * @param pathFile
	 * @param pathFileList
	 */
	private void addUniquePathFileToList(final String pathFile,
			                             final List<String> pathFileList) {
		if (pathFile != null && ! pathFile.isBlank() &&
		    ! pathFileList.contains(pathFile)) {
			pathFileList.add(pathFile);
		}
	}

	/**
	 * Build a class name taking package and classes separated by '.'
	 * e.g: com.abc.ClassName
	 * 
	 * @param packageClassParts
	 * @param i
	 * 
	 * @return
	 */
	private String buildClassNameFromParts(final String[] packageClassParts, int i) {
		StringBuilder className = new StringBuilder();
		for (int j = i; j < packageClassParts.length; j++) {
			className.append(packageClassParts[j]);
			if (j < packageClassParts.length) {
				className.append(".");
			}
		}
		return className.toString();
	}

	/**
	 * Build package name using package and class name parts e.g. com.package.ClassName
	 * 
	 * @param packageClassParts
	 * @param i
	 * 
	 * @return
	 */
	private String buildPackageNameFromParts(final String[] packageClassParts, int i) {
		StringBuilder packageName = new StringBuilder();
		for (int j = 0; j < i; j++) {
		    packageName.append(packageClassParts[j]);
		    if (j < (i - 1)) {
		        packageName.append(".");
		    }
		}
		return packageName.toString();
	}
	
	/**
	 * Resolve using import list with imported type of CLASS_NAME
	 * exact end of imported string class name to that referenced
	 * in the defined class.
	 *
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseClassNameImports(final ClassDefinitionData classDefinitionData) {
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {				
				final String className = "." + packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImportType() == ImportType.CLASS_NAME &&
			            importData.getImportName().endsWith(className)) {
				        final int index = importData.getImportName().indexOf(className);
				        packageClassData.setPackageName(importData.getImportName().substring(0,  index));
				       	packageClassData.setResolved(true);
				       	break;
				    }
		        }
			}
		}
	}

	/**
	 * Resolve using classes on class path i.e. reflection
	 * using import list with imported type of WILDCARD, append
	 * class name onto the end
	 * 
	 * @param classDefinitionData
	 * @param packageResolveCallback
	 * @param pathFileList
	 */
	private void resolvePackagesForClassNameUseExternalWildcardImports(final ClassDefinitionData classDefinitionData,
			                                                           final PackageResolveCallback packageResolveCallback,
			                                                           final List<String> pathFileList) {
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImportType() == ImportType.WILDCARD) {
			        	final PackageResolveResult result = packageResolveCallback.onPackageReolveCallback(importData.getImportName(), className);
			    		if (result.isSuccess()) {	
			    			addUniquePathFileToList(result.getPathFile(), pathFileList);
			    		
			            	packageClassData.setPackageName(importData.getImportName());
				       	    packageClassData.setResolved(true);
				       	    break;
			    		}
			        }
		        }
			}
		}
	}

	/**
	 * Resolve by testing main class name and package name in the class definition
	 * 
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseClassDefinition(final ClassDefinitionData classDefinitionData) {
		final String className = classDefinitionData.getPrimaryClassName();
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved() &&
		        packageClassData.getClassName().equals(className)) {
		        packageClassData.setPackageName(classDefinitionData.getPackageName());
		        packageClassData.setResolved(true);
			}
		}
	}

	/**
	 * Resolve by looking at java.lang default package and reflection.
     *
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseDefaultPackage(final ClassDefinitionData classDefinitionData) {
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
				final PackageResolveResult result = resolvePackageOnClassPath(JAVA_LANG_PACKAGE_NAME, className);
	    		if (result.isSuccess()) {
	                packageClassData.setPackageName(JAVA_LANG_PACKAGE_NAME);
				    packageClassData.setResolved(true);
		        }
			}
		}
	}

	/**
	 * Test if class exists on the class loader path
	 * 
	 * @param packageName The package name
	 * @param className The class name
	 * 
	 * @return Package resolve result
	 */
	private PackageResolveResult resolvePackageOnClassPath(final String packageName,
	                                         final String className) {
		
		final String packageClassName = packageName + "." + className;
		try {
			if (Class.forName(packageClassName, false, getClass().getClassLoader()) != null) {
				return new PackageResolveResult(true, null);
			}
		}
		catch (final ClassNotFoundException cnfe) {
			// Do nothing
		}
		return new PackageResolveResult(false, null);
	}
}
