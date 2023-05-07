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
	
	private final Log log;
	
	private final JavaCompiler javaCompiler;
	private final StandardJavaFileManager standardJavaFileManager;
	
	/**
	 * Create compiler and file manager instance
	 */
	public JavaParse() {
		javaCompiler = ToolProvider.getSystemJavaCompiler();
		standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);
	    log = new Log();
	    log.setLogLevel(LogType.WARN);
	}
	
	/**
	 * Parse java file and extract the detail that we need
	 * 
	 * @param pathFile The start path and file to parse
	 * 
	 * @return The details extracted from the java files or null for invalid file
	 */
	public ClassDefinitionData parse(final String pathFile) { // NOSONAR
		log.indent();
		
		final Iterable<? extends JavaFileObject> javaFileObjects = 
			standardJavaFileManager.getJavaFileObjects(new File(pathFile));
		final JavacTask javacTask = (JavacTask) 
			javaCompiler.getTask(null, standardJavaFileManager, null, null, null, javaFileObjects);         
		
		Iterable<? extends CompilationUnitTree> compilationUnitTrees = null;
		try {
			compilationUnitTrees = javacTask.parse();
		}
		catch (final IOException ioe) {
			log.error ("parse: IOException " + ioe.getMessage());
			log.outdent();
			return null;
		}
		
		log.verbose ("parse: pathFile=" + pathFile);
		
		final Deque<Tree> treeStack = new ArrayDeque<>();
		
		final ClassDefinitionData classDefinitionData = new ClassDefinitionData(pathFile);
		for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
		 	treeStack.push(compilationUnitTree);
			
			final PackageTree packageTree = compilationUnitTree.getPackage();
			if (packageTree != null) {
				treeStack.push(packageTree);
			    
				final String packageName = packageTree.getPackageName().toString();
		    	log.verbose ("parse: packageName=" + packageName);
		    	classDefinitionData.setPackageName(packageName);
		    	
		    	treeStack.pop();
		    }

			for (final ImportTree importTree : compilationUnitTree.getImports()) {
				treeStack.push(importTree);
				
		    	String importStr = importTree.getQualifiedIdentifier().toString();
		    	log.verbose ("parse: import=" + importStr);
		    	
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
				    log.warn("parse: Skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
				}
		    	
		    	treeStack.pop();
		    }
		    
		    treeStack.pop();
		}
		
		log.outdent();
		
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
    	log.indent();
		log.verbose("processClassTree: " + classTree.getKind());
		
    	final String className = classTree.getSimpleName().toString();

    	// Primary class name is either the public class, however
    	// if then there is no public class use the package level class
    	// inner classes are ignored
    	final boolean nestedClass = isNestedClass(treeStack);
    	if (! nestedClass && 
    		classDefinitionData.getPrimaryClassName() == null ||
    		isPublic(classTree.getModifiers())) {	
    	    classDefinitionData.setPrimaryClassName(className);
        	log.verbose ("processClassTree: primaryClassName=" + className);
        }
    	else {
    		classDefinitionData.addClassNameToSecondaryClassNameList(className);
    		log.verbose ("processClassTree: Add secondaryClassName=" + className);
    	}

    	log.verbose ("processClassTree: Add className=" + className);
		
    	for (final Tree tree : classTree.getTypeParameters()) {
    		treeStack.push(tree);
    		if (tree instanceof TypeParameterTree typeParameterTree) {
    			processTypeParameterTree(typeParameterTree, treeStack, classDefinitionData);
    		}
    		else {
        	    log.warn ("processClassTree: [TypeParameter] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
    		treeStack.pop();
    	}
  
    	for (final Tree tree : classTree.getMembers()) {
    		treeStack.push(tree);
    		
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
        	    log.warn ("processClassTree: [Member] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
            
            treeStack.pop();
		}

    	log.outdent();
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
    	log.indent();
		log.verbose("processTypeParameterTree: " + typeParameterTree.getKind());
		
		final String typeName = typeParameterTree.getName().toString();
		classDefinitionData.getGenericList().add(typeName);
		
		log.outdent();
    }
    
    /**
     * Test if the class is a public class
     * 
     * @param modifiersTree The modifiers associated with element
     * 
     * @return True if public, false if not
     */
    private boolean isPublic (final ModifiersTree modifiersTree) {
    	log.indent();
    	
    	boolean result = false;
        if (modifiersTree != null) {
    	    final Set<Modifier> modifiers = modifiersTree.getFlags();
    	    result = modifiers.contains(Modifier.PUBLIC);
        }
        
        log.debug("isPublic: [" + modifiersTree + "]");
        log.outdent();
        
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
      
    	log.indent();
        log.verbose("processVariableTree: " + variableTree.getKind());
        
        final Tree tree = variableTree.getType();
        treeStack.push(tree);
        
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
            log.warn("processVariableTree: Skip kind=" + tree.getKind() + "[" + tree + "]");
        }
        treeStack.pop();

        log.outdent();
    }
    
    /**
     * @param parameterizedTypeTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processParameterizedTypeTree(final ParameterizedTypeTree parameterizedTypeTree,
    		                                  final Deque<Tree> treeStack,
    		                                  final ClassDefinitionData classDefinitionData) {
    	
    	log.indent();
    	log.verbose("processParameterizedTypeTree: " + parameterizedTypeTree.getKind());
    	
        final Tree tree = parameterizedTypeTree.getType();
        treeStack.push(tree);
        
        final String className =  tree.toString();
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processParameterizedTypeTree");
        
        treeStack.pop();
        
        log.outdent();
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
    	log.indent();
        log.verbose("processMethodTree: " + methodTree.getKind());
 
        final String methodName = methodTree.getName().toString();
        log.verbose ("processMethodTree: Processing methodName=" + methodName);

        for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
        	treeStack.push(typeParameterTree);
            log.warn ("processMethodTree: Skip type parameter kind=" + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
            treeStack.pop();
        }

        // Get the return type class
        final Tree returnType = methodTree.getReturnType();
        if (returnType != null) {
        	treeStack.push(returnType);
        	
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
                log.warn("processMethodTree: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
            }
            
            treeStack.pop();
        }
        else {
        	log.verbose("processMethodTree: No return type to process");
        }

        // Get the parameters for the method
        for (final VariableTree variableTree : methodTree.getParameters()) {
        	treeStack.push(variableTree);
        	
            final String variableName = variableTree.getName().toString();
            log.verbose("processMethodTree: Processing variableName=" + variableName);

            final Tree variableType = variableTree.getType();
            treeStack.push(variableType);
            if (variableType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, treeStack, classDefinitionData);
            }
            else {
                log.warn ("processMethodTree: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
            }
            treeStack.pop();
            
            treeStack.pop();
        }

        // Get throws for the method
        for (final ExpressionTree expressionTree : methodTree.getThrows()) {
        	treeStack.push(expressionTree);
        	
            if (expressionTree instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, treeStack, classDefinitionData);
            }
            else {
               log.warn ("processMethodTree: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
            }
            
            treeStack.pop();
        }

        // Method body
        final BlockTree blockTree = methodTree.getBody();
        treeStack.push(blockTree);
        processBlockTree(blockTree, treeStack, classDefinitionData);
        treeStack.pop();         
        
        log.outdent();
    }

    /**
     * @param primitiveTypeTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree,
    		                              final Deque<Tree> treeStack,
                                          final ClassDefinitionData classDefinitionData) {
    	
    	log.indent();
    	log.verbose("processPrimitiveTypeTree: " + primitiveTypeTree.getKind());
        log.outdent();
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
    	log.indent();
    	log.verbose("processIdentifierTree: " + identifierTree.getKind() + " [" + identifierTree + "]");
 
        final String className = identifierTree.getName().toString(); 
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processIdentifierTree");
       
        log.outdent();
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
    	
    	log.indent();
    	    	
    	boolean addClass = true;
    	if (isIdentifierAssignement(treeStack)) {
    		addClass = false;
        }
    	
    	if (addClass && isIdentifierReturn(treeStack)) {
    		addClass = false;
        }
    
    	logTreeStack(treeStack);
    	
        if (addClass) {
            if (! classDefinitionData.isGenericType(className)) {
            	log.verbose (methodName + "#: Add className=" + className);
                classDefinitionData.addClassNameToPackageClassList(className);
            }
            else {
            	log.verbose (methodName + "#: Genric type - not adding className=" + className);
            }
    	}
    	else {
    		log.verbose (methodName + "#: NOT adding className=" + className);
    	}
        
        log.outdent();
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
    	log.indent();
    	log.verbose ("processBlockTree: " + blockTree.getKind());

        for (final StatementTree statementTree : blockTree.getStatements()) {
        	treeStack.push(statementTree);
        	
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
                log.warn ("processBlockTree: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
            }
            
            treeStack.pop();
        }

        log.outdent();
    }

    /**
     * @param expressionStatementTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
    		                                    final Deque<Tree> treeStack,
    		                                    final ClassDefinitionData classDefinitionData) {
    	log.indent();
    	log.verbose("processExpressionStatementTree: " + expressionStatementTree.getKind());
        
        final ExpressionTree expressionTree = expressionStatementTree.getExpression();
        treeStack.push(expressionTree);
        processExpressionTree(expressionTree, treeStack, classDefinitionData);
        treeStack.pop();
        
        log.outdent();
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
    	log.indent();
    	log.verbose ("processReturnTree: " + returnTree.getKind());

        final ExpressionTree expressionTree = returnTree.getExpression();
        treeStack.push(expressionTree);
        
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
            log.warn("processReturnTree: Skip expression kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }
        treeStack.pop();
        
        log.outdent();
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
    	log.indent();
    	log.verbose ("processNewClassTree: " + newClassTree.getKind());

        final ExpressionTree expressionTree = newClassTree.getIdentifier();
        treeStack.push(expressionTree);
        processExpressionTree(expressionTree, treeStack, classDefinitionData);
        treeStack.pop();

        log.outdent();
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
    	log.indent();
    	log.verbose("processExpressionTree: " + expressionTree.getKind() + " [" + expressionTree + "]");
        
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
            log.warn("processExpressionTree: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        log.outdent();
    }

    /**
     * @param assignmentTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processAssignmentTree(final AssignmentTree assignmentTree,
    		                           final Deque<Tree> treeStack,
    		                           final ClassDefinitionData classDefinitionData) {
    	log.indent();
    	log.verbose("processAssignmentTree: " + assignmentTree);
    	
    	final ExpressionTree expressionTree = assignmentTree.getExpression();
    	treeStack.push(expressionTree);
        processExpressionTree(expressionTree, treeStack, classDefinitionData);
        treeStack.pop();
        
    	log.outdent();
    }
    
   /**
    * @param memberSelectTree
    * @param treeStack Stack of depth traversed classes up to this one
    * @param classDefinitionData
    */
    private void processMemberSelectTree(final MemberSelectTree memberSelectTree,
    		                             final Deque<Tree> treeStack,
                                         final ClassDefinitionData classDefinitionData) {  
    	log.indent();
    	log.verbose("processMemberSelectTree: " + memberSelectTree.getKind());

        final String className = memberSelectTree.toString();
        addClassNameToPackageClassList(className, treeStack, classDefinitionData, "processMemberSelectTree");
        
        log.outdent();
    }

    /**
     * @param methodInvocationTree
     * @param treeStack Stack of depth traversed classes up to this one
     * @param classDefinitionData
     */
    private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
    		                                 final Deque<Tree> treeStack,
                                             final ClassDefinitionData classDefinitionData) {
    	log.indent();
    	log.verbose("processMethodInvocationTree: " + methodInvocationTree.getKind() + " [" + methodInvocationTree + "]");
        
        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        treeStack.push(expressionTree);
        processExpressionTree(expressionTree, treeStack, classDefinitionData);
        treeStack.pop();
        
        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
            treeStack.push(argExpressionTree);
        	processExpressionTree(argExpressionTree, treeStack, classDefinitionData);
        	treeStack.pop();
        }

        log.outdent();
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
    private boolean isIdentifierReturn(final Deque<Tree> treeStack) {	
    	final Iterator<Tree> iterator = treeStack.iterator();
    	if (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    	
    		if (! (tree instanceof IdentifierTree)) {
    			return false;
    		}
    	}
    	
    	if (iterator.hasNext()) {
    		final Tree tree = iterator.next();
        	
    		if (tree instanceof ReturnTree) {
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
    private boolean isIdentifierAssignement(final Deque<Tree> treeStack) {
    	final Iterator<Tree> iterator = treeStack.iterator();
    	
    	if (iterator.hasNext()) {
    		final Tree tree = iterator.next();
        	if (! (tree instanceof IdentifierTree)) {
        		return false;
        	}
    	}
    	
    	if (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		if (tree instanceof AssignmentTree) {
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
     * @return True if a nested class, false if not
     */
    private boolean isNestedClass(final Deque<Tree> treeStack) {
    	int count = 0;
    	
    	final Iterator<Tree> iterator = treeStack.iterator();
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		if (tree instanceof ClassTree) {
    			count++;
    		}
    	}
    	
    	return count > 1;
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
    		log.verbose("logTreeStack: [" + tree.getKind() + "] ");
    	}
    }
}
