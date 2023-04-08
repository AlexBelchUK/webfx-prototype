package dev.webfx.prototype.old;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.lang.model.type.TypeKind;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
 
/**
 * Class uses the java language specification java parser
 * to obtain tokenised java statements and extract the
 * details we need from the statements
 */
public class NewJavaCodeParser {

	private int indent;
	
	/**
	 * Indent logging by 1 space
	 */
	private void logIndent() {
		indent++;
	}
	
	/**
	 * Outdent logging by 1 space
	 */
	private void logOutdent() {
		indent--;
	}
	
	/**
	 * Log text with indent spaces
	 * 
	 * @param text The text to log
	 */
	private void logText(final String text) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		sb.append(text);
		System.out.println (sb.toString());
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
	public NewCompilationDefinition parse (final List<File> files) throws IOException {
		 final NewCompilationDefinition compilationDefinition = new NewCompilationDefinition();
		
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
			 NewPackageDefinition packageDefinition = null;
			 final PackageTree packageTree = compilationUnitTree.getPackage();
			 if (packageTree != null) {
				 final String packageName = packageTree.getPackageName().toString();
		    	 logText ("parse: packageName=" + packageName);
		    	 
		    	 packageDefinition = compilationDefinition.getPackageLookup().computeIfAbsent(
		    		packageName, packageNameKey -> new NewPackageDefinition(packageNameKey)); 
			 }
			 
			 // Imports used by the class definition below
			 for (final ImportTree importTree : compilationUnitTree.getImports()) {
		    	 final String importStr = importTree.getQualifiedIdentifier().toString();
		    	 logText ("parse: import=" + importStr);
		    	 packageDefinition.addImport(importStr);
		     }
			 
			 // Defined classes
			 for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {		    	 		    	 
		    	 if (treeDecls instanceof ClassTree classTree) {
		    	     processClassTree(classTree, packageDefinition);		    	     
		    	 }
		    	 else {
					 logText ("parse: skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
				 }
		     }
		 }

		 return compilationDefinition;
	}
	
	/**
	 * Extract details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 * @param packageDefinition The package definition to populate
	 */
    private void processClassTree(final ClassTree classTree,
                                  final NewPackageDefinition packageDefinition) {
		logIndent();
		
    	final String className = classTree.getSimpleName().toString();
    	logText ("processClassTree: Add className=" + className);
		
    	final NewClassDefinition classDefinition = new NewClassDefinition(className);
		packageDefinition.addDefinedClass(className);
    	
    	for (final Tree tree : classTree.getMembers()) {        	
            if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree, packageDefinition);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree, packageDefinition);
            }
        	else {
        	    logText ("processClassTree: Skip=" + tree.getKind() + " [" + tree + "]");
        	}
		}

    	logOutdent();
   	}

    private void processVariableTree(final VariableTree variableTree, 
                                     final PackageDefinition packageDefinition) {
    	logIndent();
    	
    	final Tree tree = variableTree.getType();
    	if (tree instanceof IdentifierTree identifierTree) {
    		processIdentifierTree(identifierTree, packageDefinition);
    	}
    	else {
    		logText("processVariableTree: Skip kind=" + tree.getKind() + "[" + tree + "]");
    	}
    	
    	logOutdent();
    }
    
    /**
     * Process method within a class
     * 
     * @param methodTree The method tree holds methods for the class
     * @param packageDefinition The code class definition details
     */
	private void processMethodTree(final MethodTree methodTree, 
                                   final PackageDefinition packageDefinition) {
		logIndent();
		
		final String methodName = methodTree.getName().toString();
		logText ("processMethodTree: Processing methodName=" + methodName);
		 
		for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
			logText ("processMethodTree: Skip type parameter kind=" + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
		}
			
		// Get the return type class
		final Tree returnType = methodTree.getReturnType();
		if (returnType != null) {
		    if (returnType instanceof IdentifierTree identifierTree) {
		    	processIdentifierTree(identifierTree, packageDefinition);
		    }
		    else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
		    	processPrimitiveTypeTree(primitiveTypeTree, packageDefinition);
		    }
		    else {
		    	logText("processMethodTree: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
		    }
		}
		    
		// Get the parameters for the method
		for (final VariableTree variableTree : methodTree.getParameters()) {
		    final String variableName = variableTree.getName().toString();
		    logText ("processMethodTree: Processing variableName=" + variableName);
				    	
		    final Tree variableType = variableTree.getType();
		    if (variableType instanceof IdentifierTree identifierTree) {
		    	processIdentifierTree(identifierTree, packageDefinition);
		    }
		   	else {
		   		logText ("processMethodTree: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
		   	}
		}
		    
		// Get throws for the method
		for (final ExpressionTree expressionTree : methodTree.getThrows()) {
		    if (expressionTree instanceof IdentifierTree identifierTree) {
		    	processIdentifierTree(identifierTree, packageDefinition);
		    }
		    else {
		    	logText ("processMethodTree: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
		    }
		}
		    
		// Method body
		final BlockTree blockTree = methodTree.getBody();
		processBlockTree(blockTree, packageDefinition);
				
		logOutdent();
	}
	
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree, 
                                          final PackageDefinition packageDefinition) {
	    logIndent();
		
	    final TypeKind typeKind = primitiveTypeTree.getPrimitiveTypeKind();
	    logText("processPrimitiveTypeTree: Typekind=" + typeKind);

		logOutdent();
    }
	
	/**
	 * Store identifier parameter in referenced classes
	 * 
	 * @param identifierTree Identifier tree
	 * @param referencedClasses Referenced classes list
	 */
	private void processIdentifierTree(final IdentifierTree identifierTree,
			                           final PackageDefinition packageDefinition) {
		logIndent();
		
		final String className = identifierTree.getName().toString();
		

		logText("processIdentifierTree: --> kind=" + identifierTree.getKind());
		
		identifierTree.getKind();
		
		logText ("processIdentifierTree: Add className=" + className);
		
		referencedClasses.add(new ClassDefinition(className, 
				                                  null)); // Import list - not yet known
	    logOutdent();
	}
	
	/**
	 * Process the method block 
	 * 
	 * @param blockTree Block of code to process
	 * @param referencedClasses Referenced classes list
	 */
	private void processBlockTree(final BlockTree blockTree,
			                      final PackageDefinition packageDefinition) {
		logIndent();
		
		logText ("processBlockTree: [" + blockTree + "]");
		
		for (final StatementTree statementTree : blockTree.getStatements()) {
			
logText("processBlockTree: >>>> statementTree.kind=" + statementTree.getKind() + " [" + statementTree + "]");
			
			
			if (statementTree instanceof ReturnTree returnTree) {
				processReturnTree(returnTree, referencedClasses);
			}
			else if (statementTree instanceof ExpressionStatementTree expressionStatementTree) {
				processExpressionStatementTree(expressionStatementTree, referencedClasses);
			}
			else {
			    logText ("processBlockTree: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
			}
		}
		
		logOutdent();
	}
	
	private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
                                                final PackageDefinition packageDefinition) {
        logIndent();

        final ExpressionTree expressionTree = expressionStatementTree.getExpression();
        processExpressionTree(expressionTree, referencedClasses);
        
	    logOutdent();
	}
	
	/**
	 * Process return type
	 * 
	 * @param returnTree Return tree
	 * @param referencedClasses Referenced classes list
	 */
	private void processReturnTree(final ReturnTree returnTree,
	                               final PackageDefinition packageDefinition) {
		logIndent();
		
		logText ("processReturnTree: [" + returnTree + "]");

		final ExpressionTree expressionTree = returnTree.getExpression();		
		if (expressionTree instanceof NewClassTree newClassTree) {
            processNewClassTree(newClassTree, referencedClasses);			
		}
		else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
			processMethodInvocationTree(methodInvocationTree, referencedClasses);
		}
		else {
			logText ("processReturnTree: Skip expression kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
		}
		
		logOutdent();
	}

	/**
	 * Process new class tree
	 * 
	 * @param newClassTree The new class tree
	 * @param referencedClasses Referenced classes list
	 */
	private void processNewClassTree(final NewClassTree newClassTree,
	                                 final PackageDefinition packageDefinition) {
		logIndent();
		
		logText ("processNewClassTree: [" + newClassTree + "]");

		final ExpressionTree expressionTree = newClassTree.getIdentifier();
		processExpressionTree(expressionTree, referencedClasses);
		
		logOutdent();
	}
	
	/**
	 * Process the expression tree
	 * 
	 * @param expressionTree The expression tree
	 * @param referencedClasses Referenced classes list
	 */
	private void processExpressionTree(final ExpressionTree expressionTree, 
	                                   final PackageDefinition packageDefinition) {
		logIndent();
		
		if (expressionTree instanceof IdentifierTree identifierTree) {
			processIdentifierTree(identifierTree, referencedClasses);
		}
		else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
			processMemberSelectTree(memberSelectTree, referencedClasses);
		}
		else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
			processMethodInvocationTree(methodInvocationTree, referencedClasses);
		}
		else {
			logText("processExpressionTree: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
		}
		
		logOutdent();
	}
	
	private void processMemberSelectTree(final MemberSelectTree memberSelectTree,
	                                	 final PackageDefinition packageDefinition) {
        logIndent();
                             
        final ExpressionTree expressionTree = memberSelectTree.getExpression();
        processExpressionTree(expressionTree, referencedClasses);
        
        logOutdent();
	}
	
	private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
	                                         final PackageDefinition packageDefinition) {
        logIndent();

        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        processExpressionTree(expressionTree, referencedClasses);
        
        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
        	processExpressionTree(argExpressionTree, referencedClasses);	
        }
        	
	    logOutdent();
	}
	
	/**
	 * Main entry point
	 * 
	 * @param args Command line arguments - not used
	 * 
	 * @throws IOException Thrown on error
	 */
	public static void main(final String[] args) throws IOException {		
		final List<File> files = new ArrayList<>();
		
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\a\\A1Generic.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\a\\A2SamePackage.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\a\\A3VarArgs.java"));		
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\b\\B1.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\b\\B2Record.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\b\\B3Enum.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C1Implements.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C2Interface.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C3Annotation.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C4Abstract.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C5Extends.java"));
	//	files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\test\\c\\C6BasicClass.java"));
		
		
		logText("--------Parsing--------");
		final CompilationDefinition compilationDefinition = new NewJavaCodeParser();
		.parse(files);
		logText("-----------------------");
		
		logText("--------Results--------");
		for (final Entry<String, PackageDefinition> entry : compilationDefinition.getPackageLookup().entrySet()) {
		    final String packageName = entry.getKey();
		    final PackageDefinition packageDefinition = entry.getValue();
		    
		    logText ("----");
			logText("packageName:" + packageName);
			for (final ClassDefinition classDefinition : packageDefinition.getDefinedClasses()) {
				logText(" defined.className: " + classDefinition.getClassName());
				
				for (final String importStr : classDefinition.getImports()) {
					logText ("  defined.import: " + importStr);
				}
				
				for (final ClassDefinition refClassDefinition : classDefinition.getReferencedClasses()) {
					logText("    referenced.className: " + refClassDefinition.getClassName());
					
					if (refClassDefinition.getImports() != null) {
					    for (final String refImportStr : refClassDefinition.getImports()) {
						    logText ("     referenced.import: " + refImportStr);
					    }
					}
				}
			}
			logText ("----");
		}
		logText("-----------------------");
	}
}
