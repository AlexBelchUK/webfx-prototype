package dev.webfx.parse;

import java.util.List;

// @TODO
//  a. List filter to fetch only unresolved items
//  b. Keep list of items still to resolve to reduce unnecessary loops
//  c. JUnit tests to test each method 

/**
 * @author Alexander Belch
 */
public class PackageResolve {
	
	private static final String JAVA_LANG_PACKAGE_NAME = "java.lang";
	
	private PackageResolveCallback cliPackageResolveCallback;
	
	/**
	 * Log info text
	 * 
	 * @param text The text to log
	 */
	private void logInfo(final String text) {
		if (text.isBlank()) {
			return;
		}
		System.out.println (text);
	}

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
		
		logInfo ("resolve: Called...");
		
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
	    
	    logInfo ("resolve: Done.");
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

		logInfo ("resolvePackagesForPackageAndClassNameUseExternalClasses: Called...");
		
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
			    		
			    		logInfo ("resolvePackagesForPackageAndClassNameUseExternalClasses: " + 
			    		         "resolved=true, pathFile=" + result.getPathFile() + 
			    		         ", packageName=" + packageName + ", className=" + className);
		    			
			    		break;
			   		}
			   	}
		    }
		}
		
		logInfo ("resolvePackagesForPackageAndClassNameUseExternalClasses: Done.");
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
			
			logInfo ("addUniquePathFileToList: Adding pathFile=" + pathFile);
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
		
		logInfo ("buildClassNameFromParts: i=" + i +", return=" + className.toString());
		
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
		
		logInfo("buildPackageNameFromParts: i=" + i + ", return=" + packageName.toString());
		
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
		logInfo("resolvePackagesForClassNameUseClassNameImports: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {				
				final String className = "." + packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImportType() == ImportType.CLASS_NAME &&
			            importData.getImportName().endsWith(className)) {
				        final int index = importData.getImportName().indexOf(className);
				        final String packageName = importData.getImportName().substring(0,  index);
			
				        packageClassData.setPackageName(packageName);
				       	packageClassData.setResolved(true);
				       	
				       	logInfo("resolvePackagesForClassNameUseClassNameImports: " + 
				       	        "resolved=true, packageName=" + packageName + 
				       	        ", className=" + className);
				       	break;
				    }
		        }
			}
		}
		
		logInfo("resolvePackagesForClassNameUseClassNameImports: Done.");
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
		logInfo ("resolvePackagesForClassNameUseExternalWildcardImports: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImportType() == ImportType.WILDCARD) {
			        	final PackageResolveResult result = packageResolveCallback.onPackageReolveCallback(importData.getImportName(), className);
			    		if (result.isSuccess()) {	
			    			addUniquePathFileToList(result.getPathFile(), pathFileList);
                            final String packageName = importData.getImportName();			    		
			            	packageClassData.setPackageName(packageName);
				       	    packageClassData.setResolved(true);
				       	    
				       	    logInfo ("resolvePackagesForClassNameUseExternalWildcardImports: " + 
				       	             "resolved=true, packageName=" + packageName + 
				       	             ", className=" + className);
				       	    break;
			    		}
			        }
		        }
			}
		}
		
		logInfo ("resolvePackagesForClassNameUseExternalWildcardImports: Done.");
	}

	/**
	 * Resolve by testing main class name and package name in the class definition
	 * 
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseClassDefinition(final ClassDefinitionData classDefinitionData) {
		logInfo("resolvePackagesForClassNameUseClassDefinition: Called...");
		
		final String className = classDefinitionData.getPrimaryClassName();
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved() &&
		        packageClassData.getClassName().equals(className)) {
		        packageClassData.setPackageName(classDefinitionData.getPackageName());
		        packageClassData.setResolved(true);
		        
		        logInfo("resolvePackagesForClassNameUseClassDefinition: " + 
		                "resolved=true, packageName = " + classDefinitionData.getPackageName() +
		                ", className=" + className);
			}
		}
		
		logInfo("resolvePackagesForClassNameUseClassDefinition: Done.");
	}

	/**
	 * Resolve by looking at java.lang default package and reflection.
     *
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseDefaultPackage(final ClassDefinitionData classDefinitionData) {
		logInfo("resolvePackagesForClassNameUseDefaultPackage: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
				final PackageResolveResult result = resolvePackageOnClassPath(JAVA_LANG_PACKAGE_NAME, className);
	    		if (result.isSuccess()) {
	                packageClassData.setPackageName(JAVA_LANG_PACKAGE_NAME);
				    packageClassData.setResolved(true);
				    
				    logInfo("resolvePackagesForClassNameUseDefaultPackage: " + 
				            "resolved=true, packageName=" + JAVA_LANG_PACKAGE_NAME + 
				            ", className=" + className);
		        }
			}
		}
		
		logInfo("resolvePackagesForClassNameUseDefaultPackage: Done.");
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
		
		logInfo("resolvePackageOnClassPath: Called...");
		
		final String packageClassName = packageName + "." + className;
		try {
			if (Class.forName(packageClassName, false, getClass().getClassLoader()) != null) {
				logInfo("resolvePackageOnClassPath: " + 
			            "resolved=true packageName=" + packageName + 
			            ", className=" + className);
		        return new PackageResolveResult(true, null);
			}
		}
		catch (final ClassNotFoundException cnfe) {
			// Do nothing
		}
		
		logInfo("resolvePackageOnClassPath: Done.");
		
		return new PackageResolveResult(false, null);
	}
}
