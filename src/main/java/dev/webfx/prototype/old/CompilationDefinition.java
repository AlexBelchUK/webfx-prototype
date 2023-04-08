package dev.webfx.prototype.old;

import java.util.HashMap;
import java.util.Map;

public class CompilationDefinition {
 
	private final Map<String, PackageDefinition> packageLookup;
	
	public CompilationDefinition() {
		packageLookup = new HashMap<>();
	}

	/**
	 * @return the packageLookup
	 */
	public Map<String, PackageDefinition> getPackageLookup() {
		return packageLookup;
	}
}
