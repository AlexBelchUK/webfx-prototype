package dev.webfx.parse;

import java.util.List;

public class PackageResolver {
	
	private static final String JAVA_LANG_PACKAGE_NAME = "java.lang";
	
	private ResolveCallback resolveCallback;
	
	public void setResolveCallback(final ResolveCallback resolveCallback) {
		this.resolveCallback = resolveCallback;
	}
	
	public void resolve(final ClassDefinitionData classDefinitionData,
			            final List<String> pathFileList) {
		
		String packageClassName;
		String className;
		
		// Case where the class name contains '.' as it is
	    // fully resolved to start with, need to determine the
	    // package and class name part allowing for nested inner classes
	    for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
		    if (packageClassData.getResolved() == ResolvedType.UNKNOWN &&
		    	packageClassData.getClassName().contains(".")) {
		           
			    // Split into parts gradually build up string working
			   	// forwards until a class found - the primary class
			   	// ignore any inner class after that
			   	final String[] packageClassParts = packageClassData.getClassName().split(".");
			   	for (int i = 0; i < packageClassParts.length; i++) {    
		    		String packageName = "";
		    		for (int j = 0; j < i; j++) {
			    	    packageClassName = packageClassParts[j];
			       	    if (j < i) {
			    	        packageName += ".";
			   		    }
			   	    }
			   		className = packageClassParts[i];
			   		
		    		if (isPackageAndClassValid(packageName, className)) {
			    		packageClassData.setPackageName(packageName);
			    		
			    		className = "";
        	        	for (int j = i; j < packageClassParts.length; j++) {
        	        		className += packageClassParts[j];
        	        		if (j < packageClassParts.length) {
        	        			className += ".";
        	        		}
        	        	}
        	        	
        	        	packageClassData.setClassName(className);
        	        	
			    		packageClassData.setResolved(ResolvedType.SUCCESS);
			    		break;
			   		}
			   	}
		    }
		}
	    
		// Resolve using import list with imported type of CLASS_NAME
		// exact end of imported string class name to that referenced
		// in the defined class.
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (packageClassData.getResolved() == ResolvedType.UNKNOWN) {				
				className = "." + packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImported() == ImportType.CLASS_NAME &&
			            importData.getImportName().endsWith(className)) {
				        final int index = importData.getImportName().indexOf(className);
				        packageClassData.setPackageName(importData.getImportName().substring(0,  index));
				       	packageClassData.setResolved(ResolvedType.SUCCESS);
				       	break;
				    }
		        }
			}
		}
		
		// Resolve using classes on class path i.e. reflection
		// using import list with imported type of WILDCARD, append
		// class name onto the end
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (packageClassData.getResolved() == ResolvedType.UNKNOWN) {
				className = packageClassData.getClassName();
		        for (final ImportData importData : classDefinitionData.getImportList()) {
			        if (importData.getImported() == ImportType.WILDCARD &&
			        	isPackageAndClassValid(importData.getImportName(), className)) {
			        	packageClassData.setPackageName(importData.getImportName());
				       	packageClassData.setResolved(ResolvedType.SUCCESS);
				       	break;
			        }
		        }
			}
		}
		
		// Resolve by testing main class name and package name
		// in the class definition
		className = classDefinitionData.getPrimaryClassName();
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (packageClassData.getResolved() == ResolvedType.UNKNOWN &&
		        packageClassData.getClassName().equals(className)) {
		        packageClassData.setPackageName(classDefinitionData.getPackageName());
		        packageClassData.setResolved(ResolvedType.SUCCESS);
			}
		}

		// Resolve by looking at java.lang default package and 
		// reflection.
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			if (packageClassData.getResolved() == ResolvedType.UNKNOWN) {
				className = packageClassData.getClassName();
		        if (isPackageAndClassValid(JAVA_LANG_PACKAGE_NAME, className)) {
			        packageClassData.setPackageName(JAVA_LANG_PACKAGE_NAME);
				    packageClassData.setResolved(ResolvedType.SUCCESS);
		        }
			}
		}
		
	    // Case where the class name contains '.' as it is
	    // fully resolved to start with, need to determine the
	    // package and class name part allowing for nested inner classes
	    if (resolveCallback != null) {
		    for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			    if (packageClassData.getResolved() == ResolvedType.UNKNOWN &&
			    	packageClassData.getClassName().contains(".")) {
		           
			    	// Split into parts gradually build up string working
			    	// forwards until a class found - the primary class
			    	// ignore any inner class after that
			    	final String packageClassParts[] = packageClassData.getClassName().split(".");
			    	for (int i = 0; i < packageClassParts.length; i++) {    
			    		String packageName = "";
			    		for (int j = 0; j < i; j++) {
			    		    packageClassName = packageClassParts[j];
			    		    if (j < i) {
			    		        packageName += ".";
			    		    }
			    	    }
			    		className = packageClassParts[i];
	
			    		packageClassName = packageName + "." + className;
		    	    	final String pathFile = resolveCallback.getPathFileForPackageClass(packageClassName); 	    
	        	        if (pathFile != null && ! pathFile.isEmpty()) {
	        	        	if (! pathFileList.contains(pathFile)) {
	        	        		pathFileList.add(pathFile);
	        	        	}
	        	        	packageClassData.setPackageName(packageName);
	        	        	
	        	        	className = "";
	        	        	for (int j = i; j < packageClassParts.length; j++) {
	        	        		className += packageClassParts[j];
	        	        		if (j < packageClassParts.length) {
	        	        			className += ".";
	        	        		}
	        	        	}
	        	        	
	        	        	packageClassData.setClassName(className);
	        	        	packageClassData.setResolved(ResolvedType.SUCCESS);
	        	        	break;
	        	        }
			    	}
			    }
		    }
	    }
		
		// Resolve using CLI callback to get path name from 
		// each item in import list with imported type of WILDCARD
		// Cache filename for next call - it can be parsed also.
	    if (resolveCallback != null) {
		    for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			    if (packageClassData.getResolved() == ResolvedType.UNKNOWN) {		
		            for (final ImportData importData : classDefinitionData.getImportList()) {
		        	    if (importData.getImported() == ImportType.WILDCARD) {
		        	        packageClassName = importData.getImportName() + "." + packageClassData.getClassName();
			    	    	final String pathFile = resolveCallback.getPathFileForPackageClass(packageClassName); 	    
		        	        if (pathFile != null && ! pathFile.isEmpty()) {
		        	        	if (! pathFileList.contains(pathFile)) {
		        	        		pathFileList.add(pathFile);
		        	        	}
		        	        	packageClassData.setPackageName(importData.getImportName());
		        	        	packageClassData.setResolved(ResolvedType.SUCCESS);
		        	        	break;
		        	        }
		        	    }
		        	}
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
	 * @return True if class exists, false if not
	 */
	private boolean isPackageAndClassValid(final String packageName,
			                               final String className) {
		
		final String packageClassName = packageName + "." + className;
		try {
			if (Class.forName(packageClassName, false, getClass().getClassLoader()) != null) {
				return true;
			}
		}
		catch (final ClassNotFoundException cnfe) {
			// Do nothing
		}
		return false;
	}
}
