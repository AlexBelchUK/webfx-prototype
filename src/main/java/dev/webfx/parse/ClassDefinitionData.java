package dev.webfx.parse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alexander Belch
 */
public class ClassDefinitionData {

	private final String pathFile;
	
    private String packageName;
    private final List<ImportData> importList;
    
    private String primaryClassName;
    private final Set<String> secondaryClassNameHashSet;
    
    private final Set<String> genericHashSet;
    
    private final List<PackageClassData> packageClassList;
    
	/**
	 * Parameter constructor
	 *  
	 * @param pathFile
	 */
	public ClassDefinitionData(final String pathFile) {
		this.pathFile = pathFile;
		this.importList = new ArrayList<>();
		this.secondaryClassNameHashSet = new HashSet<>();
		this.genericHashSet = new HashSet<>();
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
	 * Add a type name to the generic hash set
	 * 
	 * @param typeName The type name to store in the set
	 */
	public void addTypeNameToGenericHashSet(final String typeName) {
		genericHashSet.add(typeName);
	}

	/**
	 * Test if class name is actually a generic type <T> label
	 * 
	 * @param className Class name to test
	 * 
	 * @return True if in the generic hash set, false if not
	 */
	public boolean isGenericType(final String className) {
		return genericHashSet.contains(className);
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
	 * Return the secondary class name set collection
	 * 
	 * @return Secondary class name set collection
	 */
	public Set<String> getSecondaryClassNameHashSet() {
		return secondaryClassNameHashSet;
	}

	/**
	 * Add a secondary class name to the secondry class set collection
	 * 
	 * @param className The class name to add to the secondary class set collection
	 */
	public void addClassNameToSecondaryClassNameHashSet(final String className) {
		secondaryClassNameHashSet.add(className);
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
