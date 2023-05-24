package dev.webfx.parse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * @author Alexander Belch
 */
public class Processor {
	private final Log log;

	private final JavaParse javaParse;
	private final PackageResolve packageResolve;
	
	private final Deque<String> pathFileDeque;
	private final List<String> pathFileProcessedList;
	
	/**
	 * Create new processor
	 */
	public Processor() {
		log = new Log();
		log.setLogLevel(LogType.INFO);

		javaParse = new JavaParse();
		packageResolve = new PackageResolve();
		
		pathFileDeque = new ArrayDeque<>();
		pathFileProcessedList = new ArrayList<>();
	}
	
	/**
	 * Add the CLI interface to be called to request path and
	 * file for a supplied package and class name
	 * 
	 * @param packageResolveCallback CLI callback
	 */
	public void setCliPackageResolveCallback(final PackageResolveCallback packageResolveCallback) {
		packageResolve.setCliPackageResolveCallback(packageResolveCallback);
	}
	
	/**
	 * Add path and file(s) to start processing
	 * 
	 * @param pathFile Java path and file 
	 */
	public void addFile(final String pathFile) {
		if (pathFileProcessedList.contains(pathFile)) {
			return;
		}
		
		pathFileProcessedList.add(pathFile);
		pathFileDeque.push(pathFile);
	}
	
	/**
	 * Reset files to be processed
	 */
	public void clearFiles() {
		pathFileDeque.clear();
		pathFileProcessedList.clear();
	}
	
	/**
	 * Process all files and request other files as needed
	 * 
	 * @return List of package names for all files supplied
	 */
	public List<String> process() { // NOSONAR
		final List<ClassDefinitionData> classDefinitionList = new ArrayList<>(); 
		
	    while (! pathFileDeque.isEmpty()) {
	    	final String pathFile = pathFileDeque.pop();
	    	final ClassDefinitionData classDefinitionData = javaParse.parse(pathFile);
	    	if (classDefinitionData != null) {
	    	    final List<String> newPathFilesToProcessList = new ArrayList<>();
	    		
	    	    packageResolve.resolve(classDefinitionData, newPathFilesToProcessList);
	    	    classDefinitionList.add(classDefinitionData);

	    	    printClassDefinition(classDefinitionData);
	    		
	    		for (final String newPathFileToProcess : newPathFilesToProcessList) {
	    			addFile(newPathFileToProcess);
	    		}
	    	}
	    }
	    
	    final List<String> packageNameList = new ArrayList<>();
	    
	    for (final ClassDefinitionData classDefinitionData : classDefinitionList) {
	    
	    	String packageName = classDefinitionData.getPackageName();
			if (! packageNameList.contains(packageName)) {
				packageNameList.add(packageName);
			}
			
	    	for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
	    		if (packageClassData.isResolved()) {
	    			
	    			packageName = packageClassData.getPackageName();
	    			if (! packageNameList.contains(packageName)) {
	    				packageNameList.add(packageName);
	    			}
	    		}
	    		else {
	    			log.warn ("process: Failed to resolve className= " + packageClassData.getClassName());
	    		}
	    	}
	    }
	    
	    // Sort in alphabetical order
	    Collections.sort(packageNameList);
	    
	    return packageNameList;
	}
	
	private void printClassDefinition(final ClassDefinitionData classDefinitionData) {
		
		log.info ("----------------------------------------------------------");
		log.info ("package: " + classDefinitionData.getPackageName());
		log.info ("primaryClass: " + classDefinitionData.getPrimaryClassName());
		
		for (final String secondaryClass : classDefinitionData.getSecondaryClassNameHashSet()) {
			log.info ("secondaryClass: " + secondaryClass);
		}
		
		for (final PackageClassData packageClassData : classDefinitionData.getPackageClassList()) {
			log.info ("packageClassData: package=" + 
	                  packageClassData.getPackageName() + ", class=" +
                      packageClassData.getClassName() + ", resolved=" + 
	                  packageClassData.isResolved());
		}
		log.info ("----------------------------------------------------------");
	}
}
