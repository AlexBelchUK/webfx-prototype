package dev.webfx.parse;

public class ImportData {
    private final String importName;
    private final ImportType imported;
     
	/**
	 * @param importName
	 * @param imported
	 */
	public ImportData(final String importName, final ImportType imported) {
		super();
		this.importName = importName;
		this.imported = imported;
	}

	/**
	 * @return the importName
	 */
	public String getImportName() {
		return importName;
	}
	
	/**
	 * @return the imported
	 */
	public ImportType getImported() {
		return imported;
	}
}
