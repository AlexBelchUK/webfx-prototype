 package dev.webfx.parse;

import java.util.List;
import java.util.Set;

/**
 * @author Alexander Belch
 */
public class PackageResolve {
	
	private static final String JAVA_LANG_PACKAGE_NAME = "java.lang";
	
	private final Log log;
	
	private final PackageResolveOnClassPath packageResolveOnClassPath; 

	private PackageResolveCallback cliPackageResolveCallback;
	
	public PackageResolve() {
		log = new Log();
		log.setLogLevel(LogType.INFO);

		packageResolveOnClassPath = new PackageResolveOnClassPath();
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
	public void resolve(final ClassDefinitionData classDefinitionData, // NOSONAR
			            final List<String> pathFileList) {
		
		log.verbose ("resolve: Called...");
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
		
			boolean resolved = false;
			
		    // External CLI resolving is done first to get any
		    // source files for further parsing...
		    if (cliPackageResolveCallback != null) {
	    	    resolved = resolveUsePackageDotClassName(packageClassData,
                                                         cliPackageResolveCallback,
                                                         pathFileList);

	    	    if (! resolved) {
	    		    resolved = resolveUseClassNameImports(classDefinitionData.getImportList(),
	    		    		                              packageClassData, 
                                                          cliPackageResolveCallback,
                                                          pathFileList);
	    	    }
	    	    
	    	    if (! resolved) {
	    	        resolved = resolveUseWildCardImports(classDefinitionData.getImportList(),
	    	        		                             packageClassData,
                                                         cliPackageResolveCallback,
                                                         pathFileList);
	    	    }
	    	    
	    	    if (! resolved) {
	    	        resolved = resolveUseClassPackage(classDefinitionData.getPackageName(),
	    	        		                          packageClassData, 
	    		                                      cliPackageResolveCallback, 
                                                      pathFileList);
	    	    }
	        }
		
		    // Internal resolving is done later as there are no source files
		    // that can be added for resolving...
		
		    if (! resolved) {
	    	    resolved = resolveUsePackageDotClassName(packageClassData,
				                                         packageResolveOnClassPath,
		                                                 null);
		    }
		    
		    if (! resolved) {
		        resolved = resolveUseClassNameImports(classDefinitionData.getImportList(),
		        		                              packageClassData, 
				                                      packageResolveOnClassPath,
                                                      null);
		    }
		    
		    if (! resolved) {
		        resolved = resolveUseWildCardImports(classDefinitionData.getImportList(),
		        		                             packageClassData,
				                                     packageResolveOnClassPath,
				                                     null);
		    }
		    
		    if (! resolved) {
		        resolved = resolveUseClassPackage(classDefinitionData.getPackageName(),
		        		                          packageClassData, 
				                                  packageResolveOnClassPath, 
                                                  null);
		    }
		    
		    if (! resolved) {
		        resolved = resolveUsePrimaryClassName(classDefinitionData.getPackageName(),
		        		                              classDefinitionData.getPrimaryClassName(),
		        		                              packageClassData);
		    }
		    
		    if (! resolved) {
		        resolved = resolveUseSecondaryClassNameList(classDefinitionData.getPackageName(),
		        		                                    classDefinitionData.getSecondaryClassNameHashSet(),
		        		                                    packageClassData);
		    }
		    
		    if (! resolved) {
		        resolveUseJavaLangPackage(packageClassData);
		    }
		}
		
	    log.verbose ("resolve: Done.");
	}

	/**
	 * Case where the class name contains '.' as it is
     * fully resolved to start with, need to determine the
     * package and class name part allowing for nested inner classes
	 * 
	 * @param packageClassData
	 * @param packageResolveCallback
	 * @param pathFileList
	 * 
	 * @return true if resolved, false if not
	 */
	private boolean resolveUsePackageDotClassName(final PackageClassData packageClassData,
			                                      final PackageResolveCallback packageResolveCallback,
			                                      final List<String> pathFileList) {

		final String description = packageResolveCallback.onPackgeResolveDescription();
		log.verbose ("resolveUsePackageDotClassName: [" + description + "] Called...");
		
		final String packageClassName = packageClassData.getClassName();
		    
		if (packageClassName.contains(".")) {
		           
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
		   		    	className = classNamePart.substring(fileIndex +  1);	
		   		    }
		   		    else {
		   		    	classNameFile = classNamePart;
		   		    	className = classNamePart;
		   		    }
		   		    
		   		    log.verbose("resolveUsePackageDotClassName: [" + description + "] try packageName=" + 
		   		                packageName + ", classNameFile=" + classNameFile);
		   		    
		   		    final PackageResolveResult result = packageResolveCallback.onPackageResolveCallback(packageName, classNameFile);
		   		    if (result.isSuccess()) {    			
		   			    addUniquePathFileToList(result.getPathFile(), pathFileList);
		   			
		    		    packageClassData.setPackageName(packageName);
                	    packageClassData.setClassName(className);
		    		    packageClassData.setResolved(true);
		    		
		    		    log.info ("resolveUsePackageDotClassName: [" + description + "] " + 
		    		              "resolved=true, pathFile=" + result.getPathFile() + 
		    		              ", packageName=" + packageName + ", className=" + className +
		    		              " return true."); // NOSONAR
		   			
		    	    	return true;
			 		}
			    }
			}
		}
		
		log.verbose ("resolveUsePackageDotClassName: [" + description + "] return false.");
		return false;
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
		if (pathFileList != null && pathFile != null && ! pathFile.isBlank() &&
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
	 * @param importList
	 * @param packageClassData
	 * @param packageResolveCallback
	 * @param pathFileList
	 * 
	 * @return True if resolved, false if not
	 */
	private boolean resolveUseClassNameImports(final List<ImportData> importList,
			                                   final PackageClassData packageClassData,
			                                   final PackageResolveCallback packageResolveCallback,
                                               final List<String> pathFileList) {

		final String description = packageResolveCallback.onPackgeResolveDescription();		
		log.verbose("resolveUseClassNameImports: [" + description + "] Called...");
		
		final String className = packageClassData.getClassName();
		log.verbose("resolveUseClassNameImports: [" + description + "] try className=" + className);
		
	    for (final ImportData importData : importList) {    			
		    if (importData.getImportType() == ImportType.CLASS_NAME) {

			    if (importData.getImportName().endsWith("." + className)) {
				    final int index = importData.getImportName().indexOf("." + className);
				    final String packageName = importData.getImportName().substring(0,  index);

				    log.verbose("resolveUseClassNameImports: [" + description + "] try packageName=" +
			                    packageName);

				    final PackageResolveResult result = packageResolveCallback.onPackageResolveCallback(packageName, className);
				    if (result.isSuccess()) {
				    	addUniquePathFileToList(result.getPathFile(), pathFileList);
				       	packageClassData.setPackageName(packageName);
			    	    packageClassData.setResolved(true);

				        log.info("resolveUseClassNameImports: [" + description + "] " +
				                 "resolved=true, packageName=" + packageName + // NOSONAR
				                 ", className=" + className + " return true.");
				       	return true;
				    }
				}
			}
		}
		
		log.verbose("resolveUseClassNameImports: [" + description + "] return false.");
		
		return false;
	}

	/**
	 * Resolve using classes on class path i.e. reflection
	 * using import list with imported type of WILDCARD, append
	 * class name onto the end
	 * 
	 * @param importList
	 * @param packageClassData
	 * @param packageResolveCallback
	 * @param pathFileList
	 * 
	 * @return True if resolved, false if not
	 */
	private boolean resolveUseWildCardImports(final List<ImportData> importList,
			                                  final PackageClassData packageClassData,
	                                          final PackageResolveCallback packageResolveCallback,
	                                          final List<String> pathFileList) {

		final String description = packageResolveCallback.onPackgeResolveDescription();
		log.verbose ("resolveUseWildCardImports: [" + description + "] Called...");
				
		final String className = packageClassData.getClassName();
		log.verbose ("resolveUseWildCardImports: [" + description + "] try className=" + className);
				
		for (final ImportData importData : importList) {
			if (importData.getImportType() == ImportType.WILDCARD) {
			    final PackageResolveResult result = 
			    	packageResolveCallback.onPackageResolveCallback(importData.getImportName(), className);
			    
			    if (result.isSuccess()) {	
			    	addUniquePathFileToList(result.getPathFile(), pathFileList);
                            
			    	final String packageName = importData.getImportName();			    		
			        packageClassData.setPackageName(packageName);
				    packageClassData.setResolved(true);
				       	    
				    log.info ("resolveUseWildCardImports: [" + description + "] " + 
				              "resolved=true, packageName=" + packageName + 
				              ", className=" + className +" return true.");
				    return true;
			    }
			}
		}
		
		log.verbose ("resolveUseWildCardImports: [" + description + "] return false.");
		
		return false;
	}

	/**
	 * Resolve by testing primary class name and package name in the class definition
	 * 
	 * @param packageName
	 * @param primaryClassName
	 * @param packageClassData
	 * 
	 * @return true if resolved, false if not
	 */
	private boolean resolveUsePrimaryClassName(final String packageName,
			                                   final String primaryClassName,
			                                   final PackageClassData packageClassData) {
		log.verbose("resolveUsePrimaryClassName: Called...");
		
		if (packageClassData.getClassName().equals(primaryClassName)) {
		    packageClassData.setPackageName(packageName);
		    packageClassData.setResolved(true);
		        
		    log.info("resolveUsePrimaryClassName: " + 
		             "resolved=true, packageName = " + packageName +
		             ", className=" + primaryClassName + " return true.");
		    
		    return true;
		}

		log.verbose("resolveUsePrimaryClassName: return false.");
		
		return false;
	}
	
	/**
	 * Resolve by testing main class name and package name in the class definition
	 * 
	 * @param packageName
	 * @param packageClassData
	 * 
	 * @return True if resolved, false if not
	 */
	private boolean resolveUseSecondaryClassNameList(final String packageName,
			                                         final Set<String> secondaryClassNameHashSet,
			                                         final PackageClassData packageClassData) {
		log.verbose("resolveUseSecondaryClassNameList: Called...");
		
		if (secondaryClassNameHashSet.contains(packageClassData.getClassName())) {
			    	
		    packageClassData.setPackageName(packageName);
		    packageClassData.setResolved(true);
		        
		    log.info("resolveUseSecondaryClassNameList: " +
		             "resolved=true, packageName = " + packageName +
                     ", className=" + packageClassData.getClassName() +
                     " return true.");
		    
		    return true;
		}

		log.verbose("resolveUseSecondaryClassNameList: return false.");
		
		return false;
	}

	/**
	 * Resolve by looking at java.lang default package and reflection.
     *
	 * @param packageClassData
	 * 
	 * @return True if resolved, false if not
	 */
	private boolean resolveUseJavaLangPackage(final PackageClassData packageClassData) {
		log.verbose("resolveUseJavaLangPackage: Called...");
		
		final String className = packageClassData.getClassName();
		log.verbose("resolveUseJavaLangPackage: packageName=" + 
		            JAVA_LANG_PACKAGE_NAME + ". className=" + className);
				
		final PackageResolveResult result = 
			packageResolveOnClassPath.onPackageResolveCallback(JAVA_LANG_PACKAGE_NAME, className);
	    
		if (result.isSuccess()) {
	        packageClassData.setPackageName(JAVA_LANG_PACKAGE_NAME);
		    packageClassData.setResolved(true);
				    
		    log.info("resolveUseJavaLangPackage: " +
		             "resolved=true, packageName=" + JAVA_LANG_PACKAGE_NAME +
		             ", className=" + className + " return true.");
		    
		    return true;
		}
		
		log.verbose("resolveUseJavaLangPackage: return false.");
		
		return false;
	}
	
	/**
	 * Case where the class name contains '.' as it is
     * fully resolved to start with, need to determine the
     * package and class name part allowing for nested inner classes
	 * 
	 * @param defaultPackageName
	 * @param packageClassData
	 * @param packageResolveCallback
	 * @param pathFileList
	 * 
	 * @return true if resolved, false if not
	 */
	private boolean resolveUseClassPackage(final String defaultPackageName,
			                               final PackageClassData packageClassData,
			                               final PackageResolveCallback packageResolveCallback,
		    	                           final List<String> pathFileList) {
		
		final String description = packageResolveCallback.onPackgeResolveDescription();
		
		log.verbose ("resolveUseClassPackage: [" + description + "] defaultPackageName=" + defaultPackageName);

		final String packageClassName = packageClassData.getClassName();
		            
		String primaryClassName = packageClassName;
	    final int index = packageClassName.indexOf(".");
	   	if (index >= 0) {
	   		primaryClassName = packageClassName.substring(0, index);
	   	}
			   	
		log.verbose("resolveUseClassPackage: [" + description + "] defaultPackageName=" + 
		            defaultPackageName + ", primaryClassName=" + primaryClassName);
		    		    
		final PackageResolveResult result = 
			packageResolveCallback.onPackageResolveCallback(defaultPackageName, primaryClassName);
		 
		if (result.isSuccess()) {    			
		    addUniquePathFileToList(result.getPathFile(), pathFileList);
		    			
			packageClassData.setPackageName(defaultPackageName);
        	packageClassData.setClassName(primaryClassName);
			packageClassData.setResolved(true);
			    		
			log.info ("resolveUseClassPackage: [" + description + "] " +  
	                  "resolved=true, pathFile=" + result.getPathFile() + 
			          ", packageName=" + defaultPackageName + 
			          ", className=" + primaryClassName + " return true.");
			
			return true;
	    }
		
		log.verbose ("resolveUseClassPackage: [" + description + "] return false.");
		return false;
	}
}
