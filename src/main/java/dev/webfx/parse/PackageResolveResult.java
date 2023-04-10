package dev.webfx.parse;

public class PackageResolveResult {
	 
	private final boolean success;
	private final String pathFile;
	
	public PackageResolveResult(final boolean success,
			                    final String pathFile) {
		this.success = success;
		this.pathFile = pathFile;
	}

	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @return the pathFile
	 */
	public String getPathFile() {
		return pathFile;
	}
}
