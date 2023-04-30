package dev.webfx.parse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
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
	public ClassDefinitionData parse(final String pathFile) { // NOSONAR
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
		
		final Deque<Tree> treeStack = new ArrayDeque<>();
		
		final ClassDefinitionData classDefinitionData = new ClassDefinitionData(pathFile);
		for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
		 	treeStack.push(compilationUnitTree);
			
			final PackageTree packageTree = compilationUnitTree.getPackage();
			if (packageTree != null) {
				treeStack.push(packageTree);
			    
				final String packageName = packageTree.getPackageName().toString();
		    	logInfo ("parse: packageName=" + packageName);
		    	classDefinitionData.setPackageName(packageName);
		    	
		    	treeStack.pop();
		    }

			for (final ImportTree importTree : compilationUnitTree.getImports()) {
				treeStack.push(importTree);
				
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
		    	
		    	treeStack.pop();
		    }
		    
		    for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {
		    	treeStack.push(treeDecls);
		        
		    	if (treeDecls instanceof ClassTree classTree) {
		            processClassTree(classTree, treeStack, classDefinitionData);
		    	}
		    	else {
				    logInfo ("parse: Skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
				}
		    	
		        treeStack.pop();
		    }
		    
		    treeStack.pop();
		}
		
		logOutdent();
		
		return classDefinitionData;
    }
	
	/**
	 * Extract class details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 * @param treeStack Stack of depth traversed classes up to this one
	 * @param classDefinitionData Class definition data
	 */
    private void processClassTree(final ClassTree classTree,
    		                      final Deque<Tree> treeStack,
                                  final ClassDefinitionData classDefinitionData) {
		treeStack.push(classTree);
    	
    	logIndent();
		logInfo("processClassTree: " + classTree.getKind());
		
    	final String className = classTree.getSimpleName().toString();

    	// Primary class name is either the public class, however
    	// if then there is no public class use the package level class
    	// inner classes are ignored
    	final int nestedClassCount = getNestedClassCount(treeStack);
    	
    	if (nestedClassCount <= 1 && 
    		classDefinitionData.getPrimaryClassName() == null ||
    		isPublic(classTree.getModifiers())) {	
    	    classDefinitionData.setPrimaryClassName(className);
        	logInfo ("processClassTree: primaryClassName=" + className);
        }
    
    	logInfo ("processClassTree: Add className=" + className);
		
    	for (final Tree tree : classTree.getTypeParameters()) {
    		if (tree instanceof TypeParameterTree typeParameterTree) {
    			processTypeParameterTree(typeParameterTree, treeStack, classDefinitionData);
    		}
    		else {
        	    logInfo ("processClassTree: [TypeParameter] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
    	}
  
    	for (final Tree tree : classTree.getMembers()) {        	
            if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree, treeStack, classDefinitionData);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree, treeStack, classDefinitionData);
            }
            else if (tree instanceof ClassTree innerClassTree) {          	
            	processClassTree(innerClassTree, treeStack, classDefinitionData);
            }
        	else {
        	    logInfo ("processClassTree: [Member] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
		}

    	logOutdent();
    	
    	treeStack.pop();
    }

    /**
     * Get generic type name e.g. <T> and store in list
     * used to remove from class list any objects with type name T
     * 
     * @param typeParameterTree The type parameter tree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData The class definition data to update
     */
    private void processTypeParameterTree(final TypeParameterTree typeParameterTree, 
    		                              final Deque<Tree> treeStack,
    		                              final ClassDefinitionData classDefinitionData) {
        treeStack.push(typeParameterTree);
    	
    	logIndent();
		logInfo("processTypeParameterTree: " + typeParameterTree.getKind());
		
		final String typeName = typeParameterTree.getName().toString();
		classDefinitionData.getGenericList().add(typeName);
		
		treeStack.pop();
		
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
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processVariableTree(final VariableTree variableTree,
    		                         final Deque<Tree> treeStack,
                                     final ClassDefinitionData classDefinitionData) {
        treeStack.push(variableTree);
    	
    	logIndent();
        logInfo("processVariableTree: " + variableTree.getKind());
        
        final Tree tree = variableTree.getType();
        if (tree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, treeStack, classDefinitionData);
        }
        else if (tree instanceof MemberSelectTree memberSelectTree) {
        	processMemberSelectTree(memberSelectTree, treeStack, classDefinitionData);
        }
        else if (tree instanceof ParameterizedTypeTree parameterizedTypeTree) {
        	processParameterizedTypeTree(parameterizedTypeTree, treeStack, classDefinitionData);
        }
        else {
            logInfo("processVariableTree: Skip kind=" + tree.getKind() + "[" + tree + "]");
        }

        logOutdent();
        
        treeStack.pop();
    }
    
    /**
     * @param parameterizedTypeTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processParameterizedTypeTree(final ParameterizedTypeTree parameterizedTypeTree,
    		                                  final Deque<Tree> treeStack,
    		                                  final ClassDefinitionData classDefinitionData) {
    	treeStack.push(parameterizedTypeTree);
    	
    	logIndent();
    	logInfo("processParameterizedTypeTree: " + parameterizedTypeTree.getKind());
    	
        final Tree tree = parameterizedTypeTree.getType();
        final String className =  tree.toString();
        
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processParameterizedTypeTree");
        
        logOutdent();
        
        treeStack.pop();
    }

   /**
    * Process method within a class
    * 
    * @param methodTree The method tree holds methods for the class
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData The class definition details
    */
    private void processMethodTree(final MethodTree methodTree,
    		                       final Deque<Tree> treeStack,
                                   final ClassDefinitionData classDefinitionData) {
        treeStack.push(methodTree);
    	
    	logIndent();
        logInfo("processMethodTree: " + methodTree.getKind());
 
        final String methodName = methodTree.getName().toString();
        logInfo ("processMethodTree: Processing methodName=" + methodName);

        for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
            logInfo ("processMethodTree: Skip type parameter kind=" + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
        }

        // Get the return type class
        final Tree returnType = methodTree.getReturnType();
        if (returnType != null) {
            if (returnType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, treeStack, classDefinitionData);
            }
            else if (returnType instanceof MemberSelectTree memberSelectTree) {
                processMemberSelectTree(memberSelectTree, treeStack, classDefinitionData);
            }
            else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
                processPrimitiveTypeTree(primitiveTypeTree, treeStack, classDefinitionData);
            }
            else {
                logInfo("processMethodTree: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
            }
        }
        else {
        	logInfo("processMethodTree: No return type to process");
        }

        // Get the parameters for the method
        for (final VariableTree variableTree : methodTree.getParameters()) {
            final String variableName = variableTree.getName().toString();
            logInfo("processMethodTree: Processing variableName=" + variableName);

            final Tree variableType = variableTree.getType();
            if (variableType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, treeStack, classDefinitionData);
            }
            else {
                logInfo ("processMethodTree: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
            }
        }

        // Get throws for the method
        for (final ExpressionTree expressionTree : methodTree.getThrows()) {
            if (expressionTree instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, treeStack, classDefinitionData);
            }
            else {
               logInfo ("processMethodTree: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
            }
        }

        // Method body
        final BlockTree blockTree = methodTree.getBody();
        processBlockTree(blockTree, treeStack, classDefinitionData);

        logOutdent();
        
        treeStack.pop();
    }

    /**
     * @param primitiveTypeTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree,
    		                              final Deque<Tree> treeStack,
                                          final ClassDefinitionData classDefinitionData) {
        treeStack.push(primitiveTypeTree);
    	
    	logIndent();
        logInfo("processPrimitiveTypeTree: " + primitiveTypeTree.getKind());
        logOutdent();
        
        treeStack.pop();
    }

   /**
    * Store identifier parameter in referenced classes
    * 
    * @param identifierTree Identifier tree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData Class definition
    */
    private void processIdentifierTree(final IdentifierTree identifierTree,
    		                           final Deque<Tree> treeStack,
                                       final ClassDefinitionData classDefinitionData) {
        treeStack.push(identifierTree);
    	
    	logIndent();
        logInfo("processIdentifierTree: " + identifierTree.getKind() + " [" + identifierTree + "]");
 
        final String className = identifierTree.getName().toString(); 
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processIdentifierTree");
       
        logOutdent();
        
        treeStack.pop();
    }

    /**
     * Test if we can add the class name to the class and package list
     * - if it is a generic type skip it.
     * 
     * @param className The class to test and add
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData The class definition data
     * @param methodName The calling method name
     */
    private void addClassNameToPackageClassList(final String className,
    		                                    final Deque<Tree> treeStack,
    		                                    final ClassDefinitionData classDefinitionData,
    		                                    final String methodName) {
    	
    	logIndent();
    	    	
    	boolean addClass = true;
    	if (isAssignementIdentifier(treeStack)) {
    		addClass = false;
        }
    	
    	if (addClass && isReturnIdentifier(treeStack)) {
    		addClass = false;
        }
    
    	logTreeStack(treeStack);
    	
        if (addClass) {
            if (! classDefinitionData.isGenericType(className)) {
                logInfo (methodName + "#: Add className=" + className);
                classDefinitionData.addClassNameToPackageClassList(className);
            }
            else {
        	    logInfo (methodName + "#: Genric type - not adding className=" + className);
            }
    	}
    	else {
            logInfo (methodName + "#: NOT adding className=" + className);
    	}
        
        logOutdent();
    }
    
   /**
    * Process the method block 
    * 
    * @param blockTree Block of code to process
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData Class definition
    */
    private void processBlockTree(final BlockTree blockTree,
    		                      final Deque<Tree> treeStack,
                                  final ClassDefinitionData classDefinitionData) {
        treeStack.push(blockTree);
        
    	logIndent();
        logInfo ("processBlockTree: " + blockTree.getKind());

        for (final StatementTree statementTree : blockTree.getStatements()) {
            if (statementTree instanceof ExpressionStatementTree expressionStatementTree) {
                processExpressionStatementTree(expressionStatementTree, treeStack, classDefinitionData);
            }
            else if (statementTree instanceof ReturnTree returnTree) {
                processReturnTree(returnTree, treeStack, classDefinitionData);
            }
            else if (statementTree instanceof VariableTree variableTree) {
                processVariableTree(variableTree, treeStack, classDefinitionData);
            }
            else {
                logInfo ("processBlockTree: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
            }
        }

        logOutdent();
        
        treeStack.pop();
    }

    /**
     * @param expressionStatementTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
    		                                    final Deque<Tree> treeStack,
    		                                    final ClassDefinitionData classDefinitionData) {
        treeStack.push(expressionStatementTree);
    	
    	logIndent();
        logInfo("processExpressionStatementTree: " + expressionStatementTree.getKind());
        
        final ExpressionTree expressionTree = expressionStatementTree.getExpression();
        processExpressionTree(expressionTree, treeStack, classDefinitionData);

        logOutdent();
        
        treeStack.pop();
    }

   /**
    * Process return type
    * 
    * @param returnTree Return tree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData
    */
    private void processReturnTree(final ReturnTree returnTree,
    		                       final Deque<Tree> treeStack,
                                   final ClassDefinitionData classDefinitionData) {
        treeStack.push(returnTree);
    	
    	logIndent();
        logInfo ("processReturnTree: " + returnTree.getKind());

        final ExpressionTree expressionTree = returnTree.getExpression();	
        if (expressionTree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, treeStack, classDefinitionData);    
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof NewClassTree newClassTree) {
            processNewClassTree(newClassTree, treeStack, classDefinitionData);			
        }
        else {
            logInfo ("processReturnTree: Skip expression kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        logOutdent();
        
        treeStack.pop();
    }

   /**
    * Process new class tree
    * 
    * @param newClassTree The new class tree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData
    */
    private void processNewClassTree(final NewClassTree newClassTree,
    		                         final Deque<Tree> treeStack,
                                     final ClassDefinitionData classDefinitionData) {
        treeStack.push(newClassTree);
    	
    	logIndent();
        logInfo ("processNewClassTree: " + newClassTree.getKind());

        final ExpressionTree expressionTree = newClassTree.getIdentifier();
        processExpressionTree(expressionTree, treeStack, classDefinitionData);

        logOutdent();
        
        treeStack.pop();
    }

   /**
    * Process the expression tree
    * 
    * @param expressionTree The expression tree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData
    */
    private void processExpressionTree(final ExpressionTree expressionTree,
    		                           final Deque<Tree> treeStack,
                                       final ClassDefinitionData classDefinitionData) {
        treeStack.push(expressionTree);
    	
    	logIndent();
        logInfo("processExpressionTree: " + expressionTree.getKind() + " [" + expressionTree + "]");
        
        if (expressionTree instanceof AssignmentTree assignmentTree) {
        	processAssignmentTree(assignmentTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
            processMemberSelectTree(memberSelectTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof NewClassTree newClassTree) {
        	processNewClassTree(newClassTree, treeStack, classDefinitionData);
        }
        else if (expressionTree instanceof ParameterizedTypeTree parameterizedTypeTree) {
        	processParameterizedTypeTree(parameterizedTypeTree, treeStack, classDefinitionData);
        }
        else {
            logInfo("processExpressionTree: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        logOutdent();
        
        treeStack.pop();
    }

    /**
     * @param assignmentTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processAssignmentTree(final AssignmentTree assignmentTree,
    		                           final Deque<Tree> treeStack,
    		                           final ClassDefinitionData classDefinitionData) {
    	treeStack.push(assignmentTree);
    	
    	logIndent();
    	logInfo("processAssignmentTree: " + assignmentTree);
    	
    	final ExpressionTree expressionTree = assignmentTree.getExpression();
        processExpressionTree(expressionTree, treeStack, classDefinitionData);
    	
    	logOutdent();
    	
    	treeStack.pop();
    }
    
   /**
    * @param memberSelectTree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData
    */
    private void processMemberSelectTree(final MemberSelectTree memberSelectTree,
    		                             final Deque<Tree> treeStack,
                                         final ClassDefinitionData classDefinitionData) {
        treeStack.push(memberSelectTree);
        
    	logIndent();
        logInfo("processMemberSelectTree: " + memberSelectTree.getKind());

        final String className = memberSelectTree.toString();
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processMemberSelectTree");
        
        logOutdent();
        
        treeStack.pop();
    }

    /**
     * @param methodInvocationTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
    		                                 final Deque<Tree> treeStack,
                                             final ClassDefinitionData classDefinitionData) {
        treeStack.push(methodInvocationTree);
    	
    	logIndent();
        logInfo("processMethodInvocationTree: " + methodInvocationTree.getKind() + " [" + methodInvocationTree + "]");
        
        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        processExpressionTree(expressionTree, treeStack, classDefinitionData);

        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
            processExpressionTree(argExpressionTree, treeStack, classDefinitionData);	
        }

        logOutdent();
        
        treeStack.pop();
    }
    
    /**
     * If the last visited tree element was an IdentifierTree and the
     * previous was a ReturnTree then we have a return identifier
     * which is not a class object - return true.
     * 
     * @param treeStack Stack of depth traversed classes
     * 
     * @return true if a return identifier, false if not
     */
    private boolean isReturnIdentifier(final Deque<Tree> treeStack) {
    	boolean foundIdentifier = false;
    	boolean foundReturn = false;
    	
    	final Iterator<Tree> iterator = treeStack.descendingIterator();
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    	
    		if (tree instanceof IdentifierTree) {
    			foundIdentifier = true;
    		}
    		else if (tree instanceof ReturnTree) {
    			foundReturn = true;
    		}
    		else {
    			foundIdentifier = false;
    	    	foundReturn = false;		
    		}
    		
    		if (foundIdentifier && foundReturn) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * If the last visited tree element was an IdentifierTree and the
     * previous was an AssignmentTree then we have an assignment identifier
     * which is not a class object - return true.
     * 
     * @param treeStack Stack of depth traversed classes
     * 
     * @return true if an assignment identifier, false if not
     */
    private boolean isAssignementIdentifier(final Deque<Tree> treeStack) {
    	boolean foundIdentifier = false;
    	boolean foundAssignment = false;
    	
    	final Iterator<Tree> iterator = treeStack.iterator();
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    	
    		if (tree instanceof IdentifierTree) {
    			foundIdentifier = true;
    		}
    		else if (tree instanceof AssignmentTree) {
    			foundAssignment = true;
    		}
    		else {
    			foundIdentifier = false;
    	    	foundAssignment = false;
    		}
    		
    		if (foundIdentifier && foundAssignment) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Test to see how may nested class definitions there are
     * 
     * @param treeStack Stack of depth traversed classes
     * 
     * @return Number of nested class definitions
     */
    private int getNestedClassCount(final Deque<Tree> treeStack) {
    	int count = 0;
    	
    	final Iterator<Tree> iterator = treeStack.iterator();
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		if (tree instanceof ClassTree) {
    			count++;
    		}
    	}
    	
    	return count;
    }
    
    /**
     * Print out the tree stack in reverse traversed order
     * 
     * @param treeStack Tree stack to print
     */
    private void logTreeStack(final Deque<Tree> treeStack) {
    	final Iterator<Tree> iterator = treeStack.iterator();
    	
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		logInfo("logTreeStack: [" + tree.getKind() + "] ");
    	}
    }
}
