package dev.webfx.parse;

import java.util.List;

public class ClassDefinitionData {

    private final String packageName;
    private final List<ImportData> importList;
    
    private String primaryClassName;
    private List<PackageClassData> packageClassList;
    
	/**
	 * Parameter constructor
	 *  
	 * @param packageName
	 * @param importList
	 */
	public ClassDefinitionData(final String packageName, 
			                   final List<ImportData> importList) {
		this.packageName = packageName;
		this.importList = importList;
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
	 * @return the packageClassList
	 */
	public List<PackageClassData> getPackageClassList() {
		return packageClassList;
	}

	/**
	 * @param packageClassList the packageClassList to set
	 */
	public void setPackageClassList(List<PackageClassData> packageClassList) {
		this.packageClassList = packageClassList;
	}
}
