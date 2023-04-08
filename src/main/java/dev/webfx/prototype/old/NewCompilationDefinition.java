package dev.webfx.prototype.old;

import java.util.HashMap;
import java.util.Map;

public class NewCompilationDefinition {

	private final Map<String, NewPackageDefinition> packageLookup;
	
	public NewCompilationDefinition() {
		packageLookup = new HashMap<>();
	}

	/**
	 * @return the packageLookup
	 */
	public Map<String, NewPackageDefinition> getPackageLookup() {
		return packageLookup;
	}
}
