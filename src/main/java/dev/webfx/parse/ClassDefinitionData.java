package dev.webfx.parse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Belch
 */
public class ClassDefinitionData {

	private final String pathFile;
	
    private String packageName;
    private final List<ImportData> importList;
    
    private String primaryClassName;
    private final List<String> secondaryClassNameList;
    
    private final List<String> genericList;
    
    private final List<PackageClassData> packageClassList;
    
	/**
	 * Parameter constructor
	 *  
	 * @param pathFile
	 */
	public ClassDefinitionData(final String pathFile) {
		this.pathFile = pathFile;
		this.importList = new ArrayList<>();
		this.secondaryClassNameList = new ArrayList<>();
		this.genericList = new ArrayList<>();
		this.packageClassList = new ArrayList<>();
	}
	
	/**
	 * @return Java path and file
	 */
	public String getPathFile() {
		return pathFile;
	}
	
	/**
	 * @param packageName
	 */
	public void setPackageName(final String packageName) {
		this.packageName = packageName;
	}
	
	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return the importList
	 */
	public List<ImportData> getImportList() {
		return importList;
	}
	
	/**
	 * @return the generic name list
	 */
	public List<String> getGenericList() {
		return genericList;
	}
	
	/**
	 * Test if class name is actually a generic type <T> label
	 * 
	 * @param className Class name to test
	 * 
	 * @return True if in the generic list, false if not
	 */
	public boolean isGenericType(final String className) {
		return genericList.contains(className);
	}
	
	/**
	 * @return the primaryClassName
	 */
	public String getPrimaryClassName() {
		return primaryClassName;
	}

	/**
	 * @param primaryClassName the primaryClassName to set
	 */
	public void setPrimaryClassName(String primaryClassName) {
		this.primaryClassName = primaryClassName;
	}
	
	/**
	 * @return the secondaryClassNameList
	 */
	public List<String> getSecondaryClassNameList() {
		return secondaryClassNameList;
	}

	/**
	 * Add a non primary class name to the secondary class name list
	 * 
	 * @param secondaryClassName the secondaryClassName to add
	 */
	public void addClassNameToSecondaryClassNameList(String secondaryClassName) {
		secondaryClassNameList.add(secondaryClassName);
	}

	/**
	 * Add unique class name to the package class list
	 * 
	 * @param className Class name / can sometimes have package name in also
	 */
	public void addClassNameToPackageClassList(final String className) {
		for (final PackageClassData packageClassData : packageClassList) {	
			if (packageClassData.getClassName().equals(className)) {
				return;
			}	
		}
		
		packageClassList.add(new PackageClassData(null, className, false));
	}
	
	/**
	 * @return the packageClassList
	 */
	public List<PackageClassData> getPackageClassList() {
		return packageClassList;
	}
}
