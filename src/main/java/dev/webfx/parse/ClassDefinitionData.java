package dev.webfx.parse;

import java.util.ArrayList;
import java.util.List;

public class ClassDefinitionData {

	private final String pathFile;
	
    private String packageName;
    private List<ImportData> importList;
    
    private String primaryClassName;
    private List<PackageClassData> packageClassList;
    
	/**
	 * Parameter constructor
	 *  
	 * @param pathFile
	 */
	public ClassDefinitionData(final String pathFile) {
		this.pathFile = pathFile;
		this.importList = new ArrayList<>();
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
