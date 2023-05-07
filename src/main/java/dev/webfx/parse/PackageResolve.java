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
	
	private final Log log;
	
	private PackageResolveCallback cliPackageResolveCallback;
	
	public PackageResolve() {
		log = new Log();
		log.setLogLevel(LogType.WARN);
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
		
		log.verbose ("resolve: Called...");
		
		resolvePackagesForPackageAndClassNameUseExternalClasses(classDefinitionData,
				                                                this::resolvePackageOnClassPath,
				                                                pathFileList);
		
		resolvePackagesForClassNameUseClassNameImports(classDefinitionData);
		
		resolvePackagesForClassNameUseExternalWildcardImports(classDefinitionData,
				                                              this::resolvePackageOnClassPath,
				                                              pathFileList);
		
		resolvePackagesForPrimaryClassName(classDefinitionData);
		
		resolvePackagesForSecondaryClassNameList(classDefinitionData);
		
		resolvePackagesForClassNameUseDefaultPackage(classDefinitionData);
		
	    if (cliPackageResolveCallback != null) {
	    	resolvePackagesForPackageAndClassNameUseExternalClasses(classDefinitionData,
                                                                    cliPackageResolveCallback,
                                                                    pathFileList);

	    	resolvePackagesForClassNameUseExternalWildcardImports(classDefinitionData,
                                                                  cliPackageResolveCallback,
                                                                  pathFileList);
	    }
	    
	    log.verbose ("resolve: Done.");
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
	private void resolvePackagesForPackageAndClassNameUseExternalClasses(final ClassDefinitionData classDefinitionData, // NOSONAR
			                                                             final PackageResolveCallback packageResolveCallback,
			                                                             final List<String> pathFileList) {

		log.verbose ("resolvePackagesForPackageAndClassNameUseExternalClasses: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			final String packageClassName = packageClassData.getClassName();
		    
		    if (! packageClassData.isResolved() && packageClassName.contains(".")) {
		           
			    // Split into parts gradually build up string working
			   	// forwards until a class found - the primary class
			   	// ignore any inner class after that
			   	final int dotCount = getDotCountInString(packageClassName);
			   	for (int i = 1; i <= dotCount; i++) {
			   		final int index = getDotIndexFromEndOfString(packageClassName, i);
			    	if (index >= 0) {
		    		    final String packageName = packageClassName.substring(0, index);
		    		    final String classNamePart = packageClassName.substring(index + 1);
		    		    
		    		    final String classNameFile;
		    		    final String className;
		    		    
		    		    final int fileIndex = classNamePart.indexOf(".");
		    		    if (fileIndex >= 0) {
		    		    	classNameFile = classNamePart.substring(0, fileIndex);
		    		    	className = classNamePart.substring(index + 1);
		    		    }
		    		    else {
		    		    	classNameFile = classNamePart;
		    		    	className = classNamePart;
		    		    }
		    		    
		    		    log.verbose("resolvePackagesForPackageAndClassNameUseExternalClasses: try packageName=" + 
		    		                   packageName + ", classNameFile=" + classNameFile);
		    		    
		    		    final PackageResolveResult result = packageResolveCallback.onPackageReolveCallback(packageName, classNameFile);
		    		    if (result.isSuccess()) {    			
		    			    addUniquePathFileToList(result.getPathFile(), pathFileList);
		    			
			    		    packageClassData.setPackageName(packageName);
        	        	    packageClassData.setClassName(className);
			    		    packageClassData.setResolved(true);
			    		
			    		    log.verbose ("resolvePackagesForPackageAndClassNameUseExternalClasses: " + 
			    		                    "resolved=true, pathFile=" + result.getPathFile() + 
			    		                    ", packageName=" + packageName + ", className=" + className);
		    			
			    	    	break;
			   		    }
			    	}
			   	}
		    }
		}
		
		log.verbose ("resolvePackagesForPackageAndClassNameUseExternalClasses: Done.");
	}
	
	/**
	 * Search string count number of '.' in string
	 * 
	 * @param packageClassName The package and class name string
	 * 
	 * @return Number of dots in string
	 */
	private int getDotCountInString(final String packageClassName) {
		int dotCount = 0;
		for (int i = packageClassName.length() - 1; i >= 0; i--) {
			final char c = packageClassName.charAt(i);
			if (c == '.') {
				dotCount++;
			}
		}
		return dotCount;
	}
	
	/**
	 * Return the index for the N'th dot starting from the end of the string
	 * 
	 * @param packageClassName The package and class name string
	 * @param endOfStringDotCount The Nth dot count (1 to n) from the end of string
	 * 
	 * @return The index where the N'th dot was found
	 */
	private int getDotIndexFromEndOfString(final String packageClassName, 
			                               final int endOfStringDotCount) {
		
		int indexCount = 0;
		for (int i = packageClassName.length() - 1; i >= 0; i--) {
			final char c = packageClassName.charAt(i);
			if (c == '.') {
				indexCount++;
			    if (indexCount == endOfStringDotCount) {
				    return i;
			    }
			}
		}
		
		return -1;
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
			
			log.verbose ("addUniquePathFileToList: Adding pathFile=" + pathFile);
			pathFileList.add(pathFile);
		}
	}
	
	/**
	 * Resolve using import list with imported type of CLASS_NAME
	 * exact end of imported string class name to that referenced
	 * in the defined class.
	 *
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseClassNameImports(final ClassDefinitionData classDefinitionData) {
		log.verbose("resolvePackagesForClassNameUseClassNameImports: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {				
				final String className = packageClassData.getClassName();
				log.verbose("resolvePackagesForClassNameUseClassNameImports: try to match className=" + className);
		
		        for (final ImportData importData : classDefinitionData.getImportList()) {    			
			        if (importData.getImportType() == ImportType.CLASS_NAME) {
			        	log.verbose("resolvePackagesForClassNameUseClassNameImports: try packageClassName=" +
			        			       importData.getImportName());
					           		
			            if (importData.getImportName().endsWith("." + className)) {
				            final int index = importData.getImportName().indexOf("." + className);
				            final String packageName = importData.getImportName().substring(0,  index);
			
				            packageClassData.setPackageName(packageName);
				       	    packageClassData.setResolved(true);
				       	
				       	    log.verbose("resolvePackagesForClassNameUseClassNameImports: " + 
				       	                   "resolved=true, packageName=" + packageName + 
				       	                   ", className=" + className);
				       	    break;
				        }
			        }
		        }
			}
		}
		
		log.verbose("resolvePackagesForClassNameUseClassNameImports: Done.");
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
		log.verbose ("resolvePackagesForClassNameUseExternalWildcardImports: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
				log.verbose ("resolvePackagesForClassNameUseExternalWildcardImports: try to resolve className=" + className);
				
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImportType() == ImportType.WILDCARD) {
			        	final PackageResolveResult result = packageResolveCallback.onPackageReolveCallback(importData.getImportName(), className);
			    		if (result.isSuccess()) {	
			    			addUniquePathFileToList(result.getPathFile(), pathFileList);
                            final String packageName = importData.getImportName();			    		
			            	packageClassData.setPackageName(packageName);
				       	    packageClassData.setResolved(true);
				       	    
				       	    log.verbose ("resolvePackagesForClassNameUseExternalWildcardImports: " + 
				       	                    "resolved=true, packageName=" + packageName + 
				       	                    ", className=" + className);
				       	    break;
			    		}
			        }
		        }
			}
		}
		
		log.verbose ("resolvePackagesForClassNameUseExternalWildcardImports: Done.");
	}

	/**
	 * Resolve by testing primary class name and package name in the class definition
	 * 
	 * @param classDefinitionData
	 */
	private void resolvePackagesForPrimaryClassName(final ClassDefinitionData classDefinitionData) {
		log.verbose("resolvePackagesForPrimaryClassName: Called...");
		
		final String className = classDefinitionData.getPrimaryClassName();
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved() &&
		        packageClassData.getClassName().equals(className)) {
		        packageClassData.setPackageName(classDefinitionData.getPackageName());
		        packageClassData.setResolved(true);
		        
		        log.verbose("resolvePackagesForPrimaryClassName: " + 
		                       "resolved=true, packageName = " + classDefinitionData.getPackageName() +
		                       ", className=" + className);
			}
		}
		
		log.verbose("resolvePackagesForPrimaryClassName: Done.");
	}
	
	/**
	 * Resolve by testing main class name and package name in the class definition
	 * 
	 * @param classDefinitionData
	 */
	private void resolvePackagesForSecondaryClassNameList(final ClassDefinitionData classDefinitionData) {
		log.verbose("resolvePackagesForSecondaryClassNameList: Called...");
		
		final List<String> classNameList = classDefinitionData.getSecondaryClassNameList();
		for (final String className : classNameList) {
		    for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			    if (! packageClassData.isResolved() &&
		            packageClassData.getClassName().equals(className)) {
		            packageClassData.setPackageName(classDefinitionData.getPackageName());
		            packageClassData.setResolved(true);
		        
		            log.verbose("resolvePackagesForSecondaryClassNameList: " + 
		                        "resolved=true, packageName = " + classDefinitionData.getPackageName() +
		                        ", className=" + className);
			    }
		    }
		}

		log.verbose("resolvePackagesForSecondaryClassNameList: Done.");
	}

	/**
	 * Resolve by looking at java.lang default package and reflection.
     *
	 * @param classDefinitionData
	 */
	private void resolvePackagesForClassNameUseDefaultPackage(final ClassDefinitionData classDefinitionData) {
		log.verbose("resolvePackagesForClassNameUseDefaultPackage: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (! packageClassData.isResolved()) {
				final String className = packageClassData.getClassName();
				log.verbose("resolvePackagesForClassNameUseDefaultPackage: packageName=" + 
				               JAVA_LANG_PACKAGE_NAME + ". className=" + className);
				
				final PackageResolveResult result = resolvePackageOnClassPath(JAVA_LANG_PACKAGE_NAME, className);
	    		if (result.isSuccess()) {
	                packageClassData.setPackageName(JAVA_LANG_PACKAGE_NAME);
				    packageClassData.setResolved(true);
				    
				    log.verbose("resolvePackagesForClassNameUseDefaultPackage: " + 
				                   "resolved=true, packageName=" + JAVA_LANG_PACKAGE_NAME + 
				                   ", className=" + className);
		        }
			}
		}
		
		log.verbose("resolvePackagesForClassNameUseDefaultPackage: Done.");
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
		
		log.verbose("resolvePackageOnClassPath: packageName=" + packageName + ", className=" + className);
		
		final String packageClassName = packageName + "." + className;
		try {
			if (Class.forName(packageClassName, false, getClass().getClassLoader()) != null) {
				log.verbose("resolvePackageOnClassPath: " + 
			                   "resolved=true packageName=" + packageName + 
			                   ", className=" + className);
		        return new PackageResolveResult(true, null);
			}
		}
		catch (final ClassNotFoundException cnfe) {
			// Do nothing
		}
		
		log.verbose("resolvePackageOnClassPath: Done.");
		
		return new PackageResolveResult(false, null);
	}
}
