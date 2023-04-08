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
public class JavaCodeParser {
 
	private static int indent;
	
	/**
	 * Constructor is private as all methods are static
	 */
	private JavaCodeParser() {
		// Do nothing
	}
	
	/**
	 * Indent logging by 1 space
	 */
	private static void logIndent() {
		indent++;
	}
	
	/**
	 * Outdent logging by 1 space
	 */
	private static void logOutdent() {
		indent--;
	}
	
	/**
	 * Log text with indent spaces
	 * 
	 * @param text The text to log
	 */
	private static void logText(final String text) {
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
				 final String packageName = packageTree.getPackageName().toString();
		    	 logText ("parse: packageName=" + packageName);
		    	 
		    	 packageDefinition = compilationDefinition.getPackageLookup().computeIfAbsent(
		    		packageName, packageNameKey -> new PackageDefinition(packageNameKey)); 
			 }
			 
			 // Imports used by the class definition below
			 final List<String> importsForClass = new ArrayList<>();
			 for (final ImportTree importTree : compilationUnitTree.getImports()) {
		    	 final String importStr = importTree.getQualifiedIdentifier().toString();
		    	 logText ("parse: import=" + importStr);
		    	 importsForClass.add(importStr);
		     }
			 
			 // Defined classes
			 for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {		    	 		    	 
		    	 if (treeDecls instanceof ClassTree classTree) {
		    	     processClassTree(classTree, importsForClass, packageDefinition);		    	     
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
	 * @param importsForClass Imports for the class defined
	 * @param packageDefinition The package definition to populate
	 */
    private static void processClassTree(final ClassTree classTree,
    		                             final List<String> importsForClass,
			                             final PackageDefinition packageDefinition) {
		logIndent();
		
    	final String className = classTree.getSimpleName().toString();
    	logText ("processClassTree: Add className=" + className);
		
    	final ClassDefinition classDefinition = new ClassDefinition(className, importsForClass);
		packageDefinition.getDefinedClasses().add(classDefinition);
    	
    	for (final Tree tree : classTree.getMembers()) {        	
            if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree, classDefinition);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree, classDefinition);
            }
        	else {
        	    logText ("processClassTree: Skip=" + tree.getKind() + " [" + tree + "]");
        	}
		}

    	logOutdent();
   	}

    private static void processVariableTree(final VariableTree variableTree, 
    		                                final ClassDefinition classDefinition) {
    	logIndent();
    	
    	final Tree tree = variableTree.getType();
    	if (tree instanceof IdentifierTree identifierTree) {
    		processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
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
     * @param classDefinition The code class definition details
     */
	private static void processMethodTree(final MethodTree methodTree, 
                                          final ClassDefinition classDefinition) {
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
		    	processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
		    }
		    else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
		    	processPrimitiveTypeTree(primitiveTypeTree, classDefinition.getReferencedClasses());
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
		    	processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
		    }
		   	else {
		   		logText ("processMethodTree: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
		   	}
		}
		    
		// Get throws for the method
		for (final ExpressionTree expressionTree : methodTree.getThrows()) {
		    if (expressionTree instanceof IdentifierTree identifierTree) {
		    	processIdentifierTree(identifierTree, classDefinition.getReferencedClasses());
		    }
		    else {
		    	logText ("processMethodTree: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
		    }
		}
		    
		// Method body
		final BlockTree blockTree = methodTree.getBody();
		processBlockTree(blockTree, classDefinition.getReferencedClasses());
				
		logOutdent();
	}
	
    private static void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree, 
                                                 final List<ClassDefinition> referencedClasses) {
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
	private static void processIdentifierTree(final IdentifierTree identifierTree,
			                                  final List<ClassDefinition> referencedClasses) {
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
	private static void processBlockTree(final BlockTree blockTree,
			                             final List<ClassDefinition> referencedClasses) {
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
	
	private static void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
                                                       final List<ClassDefinition> referencedClasses) {
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
	private static void processReturnTree(final ReturnTree returnTree,
			                              final List<ClassDefinition> referencedClasses) {
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
	private static void processNewClassTree(final NewClassTree newClassTree,
			                                final List<ClassDefinition> referencedClasses) {
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
	private static void processExpressionTree(final ExpressionTree expressionTree, 
		                                      final List<ClassDefinition> referencedClasses) {
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
	
	private static void processMemberSelectTree(final MemberSelectTree memberSelectTree,
                                                final List<ClassDefinition> referencedClasses) {
        logIndent();
                             
        final ExpressionTree expressionTree = memberSelectTree.getExpression();
        processExpressionTree(expressionTree, referencedClasses);
        
        logOutdent();
	}
	
	private static void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
                                                    final List<ClassDefinition> referencedClasses) {
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
		
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\A.java"));
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\B.java"));
		files.add(new File("C:\\ab_webfx\\webfx-prototype\\src\\main\\java\\dev\\webfx\\prototype\\parser\\code\\C.java"));
		
		logText("--------Parsing--------");
		final CompilationDefinition compilationDefinition = JavaCodeParser.parse(files);
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
