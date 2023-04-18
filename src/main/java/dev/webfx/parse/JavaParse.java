package dev.webfx.parse;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.AssignmentTree;
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
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;

/**
 * @author Alexander Belch
 */
public class JavaParse {
	
	private final JavaCompiler javaCompiler;
	private final StandardJavaFileManager standardJavaFileManager;
	
	private int indent;
	private boolean isDebug = false;
	
	/**
	 * Create compiler and file manager instance
	 */
	public JavaParse() {
		javaCompiler = ToolProvider.getSystemJavaCompiler();
		standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);
	}
	
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
	 * Log information
	 * 
	 * @param text
	 */
	private void logInfo(final String text) {
		logText(text);
	}
	
	/**
	 * Log debug
	 * 
	 * @param text
	 */
	private void logDebug(final String text) {
		if (isDebug) {
		    logText(text);
		}
	}
	
	/**
	 * Log text with indent spaces
	 * 
	 * @param text The text to log
	 */
	private void logText(final String text) {
		if (text.isBlank()) {
			return;
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		sb.append(text);
		System.out.println (sb.toString());
	}
	
	/**
	 * Parse java file and extract the detail that we need
	 * 
	 * @param pathFile The start path and file to parse
	 * 
	 * @return The details extracted from the java files or null for invalid file
	 */
	public ClassDefinitionData parse(final String pathFile) {
		logIndent();
		
		final Iterable<? extends JavaFileObject> javaFileObjects = 
			standardJavaFileManager.getJavaFileObjects(new File(pathFile));
		final JavacTask javacTask = (JavacTask) 
			javaCompiler.getTask(null, standardJavaFileManager, null, null, null, javaFileObjects);         
		
		Iterable<? extends CompilationUnitTree> compilationUnitTrees = null;
		try {
			compilationUnitTrees = javacTask.parse();
		}
		catch (final IOException ioe) {
			logInfo ("parse: IOException " + ioe.getMessage());
			logOutdent();
			return null;
		}
		
		logInfo ("parse: pathFile=" + pathFile);
		
		final ClassDefinitionData classDefinitionData = new ClassDefinitionData(pathFile);
		for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
		 	
			final PackageTree packageTree = compilationUnitTree.getPackage();
			if (packageTree != null) {
			    final String packageName = packageTree.getPackageName().toString();
		    	logInfo ("parse: packageName=" + packageName);
		    	classDefinitionData.setPackageName(packageName);
		    }

			for (final ImportTree importTree : compilationUnitTree.getImports()) {
		    	String importStr = importTree.getQualifiedIdentifier().toString();
		    	logInfo ("parse: import=" + importStr);
		    	
		    	int index = importStr.indexOf(".*");
		    	if (index >= 0) {
		    		importStr = importStr.substring(0, index);
		    		classDefinitionData.getImportList().add(new ImportData(importStr, ImportType.WILDCARD));
		    	}
		    	else {
		    		classDefinitionData.getImportList().add(new ImportData(importStr, ImportType.CLASS_NAME));
		    	}
		    }
		    
		    for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {		    	 		    	 
		        if (treeDecls instanceof ClassTree classTree) {
		            processClassTree(classTree, false,classDefinitionData);
		    	}
		    	else {
				    logInfo ("parse: Skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
				}
		    }
		}
		
		logOutdent();
		
		return classDefinitionData;
    }
	
	/**
	 * Extract class details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 * @param isInnerClass True if inner class
	 * @param classDefinitionData Class definition data
	 */
    private void processClassTree(final ClassTree classTree,
    		                      final boolean isInnerClass,
                                  final ClassDefinitionData classDefinitionData) {
		logIndent();
		
		logDebug("processClassTree: " + classTree.getKind() + " [" + classTree + "]");
		
    	final String className = classTree.getSimpleName().toString();

    	// Primary class name is either the public class, however
    	// if then there is no public class use the package level class
    	// inner classes are ignored
    	if (! isInnerClass && 
    		classDefinitionData.getPrimaryClassName() == null ||
    		isPublic(classTree.getModifiers())) {	
    	    classDefinitionData.setPrimaryClassName(className);
        	logInfo ("processClassTree: primaryClassName=" + className);
        }
    
    	logInfo ("processClassTree: Add className=" + className);
		
    	for (final Tree tree : classTree.getTypeParameters()) {
    		if (tree instanceof TypeParameterTree typeParameterTree) {
    			processTypeParameterTree(typeParameterTree, classDefinitionData);
    		}
    		else {
        	    logInfo ("processClassTree: [TypeParameter] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
    	}
  
    	for (final Tree tree : classTree.getMembers()) {        	
            if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree, classDefinitionData);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree, classDefinitionData);
            }
            else if (tree instanceof ClassTree innerClassTree) {          	
            	processClassTree(innerClassTree, true, classDefinitionData);
            }
        	else {
        	    logInfo ("processClassTree: [Member] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
		}

    	logOutdent();
   	}

    /**
     * Get generic type name e.g. <T> and store in list
     * used to remove from class list any objects with type name T
     * 
     * @param typeParameterTree The type parameter tree
     * @param classDefinitionData The class definition data to update
     */
    private void processTypeParameterTree(final TypeParameterTree typeParameterTree, 
    		                              final ClassDefinitionData classDefinitionData) {
        logIndent();
		
		logDebug("processTypeParameterTree: " + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
		
		final String typeName = typeParameterTree.getName().toString();
		classDefinitionData.getGenericList().add(typeName);
		
		logOutdent();
    }
    
    /**
     * Test if the class is a public class
     * 
     * @param modifiersTree The modifiers associated with element
     * 
     * @return True if public, false if not
     */
    private boolean isPublic (final ModifiersTree modifiersTree) {
    	logIndent();
    	
    	boolean result = false;
        if (modifiersTree != null) {
    	    final Set<Modifier> modifiers = modifiersTree.getFlags();
    	    result = modifiers.contains(Modifier.PUBLIC);
        }
        
        logDebug("isPublic: [" + modifiersTree + "]");
        
        logOutdent();
        return result;
    }

    /**
     * @param variableTree
     * @param classDefinitionData
     */
    private void processVariableTree(final VariableTree variableTree, 
                                     final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processVariableTree: " + variableTree.getKind() + " [" + variableTree + "]");
        
        final Tree tree = variableTree.getType();
        if (tree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, classDefinitionData);
        }
        else if (tree instanceof MemberSelectTree memberSelectTree) {
        	processMemberSelectTree(memberSelectTree, classDefinitionData);
        }
        else if (tree instanceof ParameterizedTypeTree parameterizedTypeTree) {
        	processParameterizedTypeTree(parameterizedTypeTree, classDefinitionData);
        }
        else {
            logInfo("processVariableTree: Skip kind=" + tree.getKind() + "[" + tree + "]");
        }

        logOutdent();
    }
    
    /**
     * @param parameterizedTypeTree
     * @param classDefinitionData
     */
    private void processParameterizedTypeTree(final ParameterizedTypeTree parameterizedTypeTree, 
    		                                  final ClassDefinitionData classDefinitionData) {
    	logIndent();

    	logDebug("processParameterizedTypeTree: " + parameterizedTypeTree.getKind() + " [" + parameterizedTypeTree + "]");
    	
        final Tree tree = parameterizedTypeTree.getType();
        final String className =  tree.toString();
        
        addClassNameToPackageClassList(className, classDefinitionData, "processParameterizedTypeTree");
        
        logOutdent();
    }

   /**
    * Process method within a class
    * 
    * @param methodTree The method tree holds methods for the class
    * @param classDefinitionData The class definition details
    */
    private void processMethodTree(final MethodTree methodTree, 
                                   final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processMethodTree: " + methodTree.getKind() + " [" + methodTree + "]");
 
        final String methodName = methodTree.getName().toString();
        logInfo ("processMethodTree: Processing methodName=" + methodName);

        for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
            logInfo ("processMethodTree: Skip type parameter kind=" + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
        }

        // Get the return type class
        final Tree returnType = methodTree.getReturnType();
        if (returnType != null) {
            if (returnType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else if (returnType instanceof MemberSelectTree memberSelectTree) {
                processMemberSelectTree(memberSelectTree, classDefinitionData);
            }
            else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
                processPrimitiveTypeTree(primitiveTypeTree, classDefinitionData);
            }
            else {
                logInfo("processMethodTree: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
            }
        }

        // Get the parameters for the method
        for (final VariableTree variableTree : methodTree.getParameters()) {
            final String variableName = variableTree.getName().toString();
            logDebug ("processMethodTree: Processing variableName=" + variableName);

            final Tree variableType = variableTree.getType();
            if (variableType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else {
                logInfo ("processMethodTree: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
            }
        }

        // Get throws for the method
        for (final ExpressionTree expressionTree : methodTree.getThrows()) {
            if (expressionTree instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else {
               logInfo ("processMethodTree: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
            }
        }

        // Method body
        final BlockTree blockTree = methodTree.getBody();
        processBlockTree(blockTree, classDefinitionData);

        logOutdent();
    }

    /**
     * @param primitiveTypeTree
     * @param classDefinitionData
     */
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree, 
                                          final ClassDefinitionData classDefinitionData) {
        logIndent();
        
        logDebug("processPrimitiveTypeTree: " + primitiveTypeTree.getKind() + " [" + primitiveTypeTree + "]");

        logOutdent();
    }

   /**
    * Store identifier parameter in referenced classes
    * 
    * @param identifierTree Identifier tree
    * @param classDefinitionData Class definition
    */
    private void processIdentifierTree(final IdentifierTree identifierTree,
                                       final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processIdentifierTree: " + identifierTree.getKind() + " [" + identifierTree + "]");
 
        final String className = identifierTree.getName().toString();
        addClassNameToPackageClassList(className, classDefinitionData, "processIdentifierTree");
        
        logOutdent();
    }

    /**
     * Test if we can add the class name to the class and package list
     * - if it is a generic type skip it.
     * 
     * @param className The class to test and add
     * @param classDefinitionData The class definition data
     * @param methodName The calling method name
     */
    private void addClassNameToPackageClassList(final String className,
    		                                    final ClassDefinitionData classDefinitionData,
    		                                    final String methodName) {

    	logIndent();
    	
        if (! classDefinitionData.isGenericType(className)) {
            logInfo (methodName + "#: Add className=" + className);
            classDefinitionData.addClassNameToPackageClassList(className);
        }
        else {
        	logInfo (methodName + "#: Genric type - not adding className=" + className);
        }
        
        logOutdent();
    }
    
   /**
    * Process the method block 
    * 
    * @param blockTree Block of code to process
    * @param classDefinitionData Class definition
    */
    private void processBlockTree(final BlockTree blockTree,
                                  final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug ("processBlockTree: " + blockTree.getKind() + " [" + blockTree + "]");

        for (final StatementTree statementTree : blockTree.getStatements()) {
            if (statementTree instanceof ExpressionStatementTree expressionStatementTree) {
                processExpressionStatementTree(expressionStatementTree, classDefinitionData);
            }
            else if (statementTree instanceof ReturnTree returnTree) {
                processReturnTree(returnTree, classDefinitionData);
            }
            else if (statementTree instanceof VariableTree variableTree) {
                processVariableTree(variableTree, classDefinitionData);
            }
            else {
                logInfo ("processBlockTree: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
            }
        }

        logOutdent();
    }

    /**
     * @param expressionStatementTree
     * @param classDefinitionData
     */
    private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
                                                final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processExpressionStatementTree: " + expressionStatementTree.getKind() + " [" + expressionStatementTree + "]");
        
        final ExpressionTree expressionTree = expressionStatementTree.getExpression();
        processExpressionTree(expressionTree, classDefinitionData);

        logOutdent();
    }

   /**
    * Process return type
    * 
    * @param returnTree Return tree
    * @param classDefinitionData
    */
    private void processReturnTree(final ReturnTree returnTree,
                                   final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug ("processReturnTree: " + returnTree.getKind() + " [" + returnTree + "]");

        final ExpressionTree expressionTree = returnTree.getExpression();	
        if (expressionTree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, classDefinitionData);    
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, classDefinitionData);
        }
        else if (expressionTree instanceof NewClassTree newClassTree) {
            processNewClassTree(newClassTree, classDefinitionData);			
        }
        else {
            logInfo ("processReturnTree: Skip expression kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        logOutdent();
    }

   /**
    * Process new class tree
    * 
    * @param newClassTree The new class tree
    * @param classDefinitionData
    */
    private void processNewClassTree(final NewClassTree newClassTree,
                                     final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug ("processNewClassTree: " + newClassTree.getKind() + " [" + newClassTree + "]");

        final ExpressionTree expressionTree = newClassTree.getIdentifier();
        processExpressionTree(expressionTree, classDefinitionData);

        logOutdent();
    }

   /**
    * Process the expression tree
    * 
    * @param expressionTree The expression tree
    * @param classDefinitionData
    */
    private void processExpressionTree(final ExpressionTree expressionTree, 
                                       final ClassDefinitionData classDefinitionData) {
        logIndent();

        logInfo("processExpressionTree: " + expressionTree.getKind() + " [" + expressionTree + "]");
        
        if (expressionTree instanceof AssignmentTree assignmentTree) {
        	processAssignmentTree(assignmentTree, classDefinitionData);
        }
        else if (expressionTree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, classDefinitionData);
        }
        else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
            processMemberSelectTree(memberSelectTree, classDefinitionData);
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, classDefinitionData);
        }
        else if (expressionTree instanceof NewClassTree newClassTree) {
        	processNewClassTree(newClassTree, classDefinitionData);
        }
        else if (expressionTree instanceof ParameterizedTypeTree parameterizedTypeTree) {
        	processParameterizedTypeTree(parameterizedTypeTree, classDefinitionData);
        }
        else {
            logInfo("processExpressionTree: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        logOutdent();
    }

    /**
     * @param assignmentTree
     * @param classDefinitionData
     */
    private void processAssignmentTree(final AssignmentTree assignmentTree,
    		                           final ClassDefinitionData classDefinitionData) {
    	logIndent();
    	
    	logDebug("processAssignmentTree: " + assignmentTree + " [" + assignmentTree + "]");
    	
    	final ExpressionTree expressionTree = assignmentTree.getExpression();
        processExpressionTree(expressionTree, classDefinitionData);
    	
    	logOutdent();
    }
    
   /**
    * @param memberSelectTree
    * @param classDefinitionData
    */
    private void processMemberSelectTree(final MemberSelectTree memberSelectTree,
                                         final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processMemberSelectTree: " + memberSelectTree.getKind() + " [" + memberSelectTree + "]");

        final String className = memberSelectTree.toString();
        addClassNameToPackageClassList(className, classDefinitionData, "processMemberSelectTree");
        
        logOutdent();
    }

    /**
     * @param methodInvocationTree
     * @param classDefinitionData
     */
    private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
                                             final ClassDefinitionData classDefinitionData) {
        logIndent();

        logDebug("processMethodInvocationTree: " + methodInvocationTree.getKind() + " [" + methodInvocationTree + "]");
        
        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        processExpressionTree(expressionTree, classDefinitionData);

        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
            processExpressionTree(argExpressionTree, classDefinitionData);	
        }

        logOutdent();
    }
}
