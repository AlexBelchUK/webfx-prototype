package dev.webfx.prototype.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;

/**
 * Class uses the java language specification java parser
 * to obtain tokenised java statements and extract the
 * details we need from the statements
 */
public class JavaCodeParser {

	/**
	 * Constructor is private as all methods are static
	 */
	private JavaCodeParser() {
		// Do nothing
	}
		
	/**
	 * Parse java files and extract the details that we need
	 * 
	 * @param files The list of files to parse
	 * 
	 * @return The details extracted from the java files
	 * 
	 * @throws IOException If file is not found
	 */
	public static CompilationDefinition parse (final List<File> files) throws IOException {
		 final CompilationDefinition compilationDefinition = new CompilationDefinition();
		
		 final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		 final StandardJavaFileManager standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);		 
		 final Iterable<? extends JavaFileObject> javaFileObjects = standardJavaFileManager.getJavaFileObjectsFromFiles(files);
		 final JavacTask javacTask = (JavacTask) javaCompiler.getTask(null, standardJavaFileManager, null, null, null, javaFileObjects);         
		 final Iterable<? extends CompilationUnitTree> compilationUnitTrees = javacTask.parse();
		 
		 for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
			 // 
			 // 1.
			 // Get package and class definitions within the package
			 // Classes are public or package access level and are stored
			 // in the defined class list, also store assocatied imports
			 // for the defined class
			 //
			 
			 //
			 // 2. 
			 // Get and store public / package access class elements that are referenced
			 // in each defined class, elements can be:
			 //   a. interfaces
			 //   b. extends
			 //   c. fields
			 //   d. method return type
			 //   e. method parameters
			 //   f. variables defined in a method
			 // 
			 // Class elements can be:
			 //   a. record
			 //   b. class
			 //   c. abstract class
			 //   d. interface
			 //   e. enumeration
			 // but NOT primitives
			 //
			 
			 //
			 // 3.
			 // Go through all classes
			 //   - Move package prefixed classes to package name and class name
			 //   - The default package name is "", un-allocated package names are null
			 //
			 
			 // 4.
			 // Use rules to determine the package name of a null package
			 // e.g. is the class in the same package, is it in default package
			 // defined else where in the defined classes or a system package
			 // 
			 
			 // Package name
			 PackageDefinition packageDefinition = null;
			 final PackageTree packageTree = compilationUnitTree.getPackage();
			 if (packageTree != null) {
				 String packageName = packageTree.getPackageName().toString();
		    	 System.out.println ("parse: packageName:" + packageName);
		    	 
		    	 packageDefinition = compilationDefinition.getPackageLookup().computeIfAbsent(
		    		packageName, packageNameKey -> new PackageDefinition(packageNameKey)); 
			 }
			 
			 // Imports used by the class definition below
			 final List<String> importsForClass = new ArrayList<>();
			 for (final ImportTree importTree : compilationUnitTree.getImports()) {
		    	 String importStr = importTree.getQualifiedIdentifier().toString();
		    	 System.out.println ("parse: import:" + importStr);
		    	 importsForClass.add(importStr);
		     }
			 
			 // Defined classes
			 for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {		    	 		    	 
		    	 if (treeDecls instanceof ClassTree classTree) {
		    	     processClassTree(classTree, importsForClass, packageDefinition);		    	     
		    	 }
		    	 else {
					 System.out.println ("parse: skip:" + treeDecls.getKind());
				 }
		     }
		 }

		 return compilationDefinition;
	}
	
	/**
	 * Extract details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 * @param importsForClass Imports for the class defined
	 * @param packageDefinition The package definition to populate
	 */
    private static void processClassTree(final ClassTree classTree,
    		                             final List<String> importsForClass,
			                             final PackageDefinition packageDefinition) {
		
    	final String className = classTree.getSimpleName().toString();
    	System.out.println (" processClassTree: className:" + className);
		
    	final ModifiersTree modifiersTree = classTree.getModifiers();
		final AccessType accessType = getAccessType(modifiersTree);
    	
    	if (accessType != null) {
    		final ClassDefinition classDefinition = new ClassDefinition(accessType, className, importsForClass);
		    packageDefinition.getDefinedClasses().add(classDefinition);
		    System.out.println (" processClassTree:  Added defined class");
    	
    	    for (final Tree tree : classTree.getMembers()) {        	
                if (tree instanceof MethodTree methodTree) {
			  	    processMethodTree(methodTree, classDefinition);
			    }
        	    else {
        		    System.out.println (" processClassTree: Skip:" + tree.getKind());
        	    }
		    }
    	}
   	}

    /**
     * Convert modifiers tree to either package or public access enumeration
     * 
     * @param modifiersTree Modifiers tree
     * 
     * @return public, package or null access
     */
	private static AccessType getAccessType(final ModifiersTree modifiersTree) {
		final Set<Modifier> modifier = modifiersTree.getFlags();
    	
    	AccessType accessType = null;
    	if (modifier.contains(Modifier.PUBLIC)) {
    	    accessType = AccessType.PUBLIC_ACCESS;
    	}
    	else if (modifier.contains(Modifier.PROTECTED)) {
    		accessType = AccessType.PACKAGE_ACCESS;
    	}
		return accessType;
	}
	
    /**
     * Process method within a class
     * 
     * @param methodTree The method tree holds methods for the class
     * @param classDefinition The code class definition details
     */
	private static void processMethodTree(final MethodTree methodTree, 
                                          final ClassDefinition classDefinition) {
				
		final String methodName = methodTree.getName().toString();
		System.out.println ("  processMethodTree: Processing methodName: " + methodName);
		
		// Check method is package or public access
		final ModifiersTree modifiersTree = methodTree.getModifiers();
		final AccessType accessType = getAccessType(modifiersTree);
		if (accessType != null) { 
			// Get the return type class
		    final Tree returnType = methodTree.getReturnType();
		    if (returnType != null) {
		    	if (returnType instanceof IdentifierTree identifierTree) {
		    		System.out.println ("  processMethodTree: Processing return type");
		    		processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
		    	}
		    	else {
		    		System.out.println ("  processMethodTree: Skip return kind:" + returnType.getKind());
		    	}
		    }
		    
		    // Get the parameters for the method
		    for (final VariableTree variableTree : methodTree.getParameters()) {
		    	final String variableName = variableTree.getName().toString();
		    	System.out.println ("  processMethodTree: Processing variableName: " + variableName);
				    	
		    	final Tree variableType = variableTree.getType();
		    	if (variableType instanceof IdentifierTree identifierTree) {
		    		System.out.println ("  processMethodTree: Processing variable type");
		    		processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
		    	}
		    	else {
		    		System.out.println ("  processMethodTree: Skip param kind:" + variableType.getKind());
		    	}
		    }
		    
		    
		    // Get throws for the method
		    
		    
		    
		    
		    
		    
		}
	}
	
	/**
	 * Store identifier parameter in referenced classes
	 * 
	 * @param identifierTree Identifier tree
	 * @param referencedClasses Referenced classes list
	 */
	private static void processIdentifierTree(final IdentifierTree identifierTree,
			                                  final List<ClassDefinition> referencedClasses) {
		final String className = identifierTree.getName().toString();
		
		System.out.println ("   processIdentifierTree: className:" + className);
		
		referencedClasses.add(new ClassDefinition(null,   // Package name - not yet known 
				                                  className, 
				                                  null)); // Import list - not yet known
	}
	
	/**
	 * Main entry point
	 * 
	 * @param args Command line arguments - not used
	 * 
	 * @throws IOException Thrown on error
	 */
	public static void main(String[] args) throws IOException {		
		List<File> files = new ArrayList<>();
		
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\A.java"));
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\B.java"));
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\C.java"));
		
		final CompilationDefinition compilationDefinition = JavaCodeParser.parse(files);
		
		
	}
}
