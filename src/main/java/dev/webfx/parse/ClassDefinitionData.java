package dev.webfx.parse;

import java.util.List;

public class ClassDefinitionData {
    private final String pathFile;
    private final String packageName;
    
    private String primaryClassName;
    
    private final List<ImportData> importList;
    private final List<PackageClassData> packageClassList;
    
	/**
	 * @param pathFile
	 * @param packageName
	 * @param primaryClassName
	 * @param importList
	 * @param packageClassList
	 */
	public ClassDefinitionData(final String pathFile, 
			                   final String packageName, 
			                   final String primaryClassName,
			                   final List<ImportData> importList, 
			                   final List<PackageClassData> packageClassList) {
		this.pathFile = pathFile;
		this.packageName = packageName;
		this.primaryClassName = primaryClassName;
		this.importList = importList;
		this.packageClassList = packageClassList;
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
	 * @return the pathFile
	 */
	public String getPathFile() {
		return pathFile;
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
	 * @return the packageClassList
	 */
	public List<PackageClassData> getPackageClassList() {
		return packageClassList;
	}
}
