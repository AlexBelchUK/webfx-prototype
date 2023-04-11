package dev.webfx.parse;

public class ImportData {
    private final String importName;
    private final ImportType importType;
     
	/**
	 * Parameter constructor
	 * 
	 * @param importName
	 * @param importType
	 */
	public ImportData(final String importName, final ImportType importType) {
		this.importName = importName;
		this.importType = importType;
	}

	/**
	 * @return the importName
	 */
	public String getImportName() {
		return importName;
	}
	
	/**
	 * @return the import type
	 */
	public ImportType getImportType() {
		return importType;
	}
}
