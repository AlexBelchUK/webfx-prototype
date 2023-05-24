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

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.DirectiveTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
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
	private static final boolean LOG_ADD_CLASS = false;
	
	private final Log log;
	
	private final JavaCompiler javaCompiler;
	private final StandardJavaFileManager standardJavaFileManager;
	
	private Deque<Tree> treeStack;
	private ClassDefinitionData classDefinitionData;
	
	/**
	 * Create compiler and file manager instance
	 */
	public JavaParse() {
		javaCompiler = ToolProvider.getSystemJavaCompiler();
		standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);
	    log = new Log();
	    log.setLogLevel(LogType.INFO);
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
		
		treeStack = new ArrayDeque<>();
		classDefinitionData = new ClassDefinitionData(pathFile);
		
		for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
		 	treeStack.push(compilationUnitTree);
			processCompilationUnitTree(compilationUnitTree);
		    treeStack.pop();
		}
		
		log.outdent();
		
		return classDefinitionData;
    }
	
	/**
     * Process annotation tree
     * 
     * @param annotationTree The annotation tree
     */
    private void processAnnotationTree(final AnnotationTree annotationTree) {	
    	log.indent();
    	log.verbose("processAnnotationTree: " + annotationTree.getKind() + " [" + annotationTree + "]");
        
        final Tree annotationType = annotationTree.getAnnotationType();
        if (annotationType != null) {
            treeStack.push(annotationType);      
            final String annotationClassName = annotationType.toString();
            classDefinitionData.addClassNameToPackageClassList(annotationClassName);
            treeStack.pop();
        }
        
        for (final ExpressionTree expressionTree : annotationTree.getArguments()) {
        	treeStack.push(expressionTree);
        	processExpressionTree(expressionTree);
        	treeStack.pop();
        }
    	
    	log.outdent();
    }
    
    /**
     * Process array type tree
     * 
     * @param arrayTypeTree Array type tree
     */
    private void processArrayTypeTree(final ArrayTypeTree arrayTypeTree) {
    	log.indent();
    	log.verbose("processArrayTypeTree: " + arrayTypeTree.getKind() + " [" + arrayTypeTree + "]");
        log.verbose("processArrayTypeTree: type=" + arrayTypeTree.getType());
        addClassNameToPackageClassList(arrayTypeTree.getType().toString(), "processArrayTypeTree");
    	log.outdent();
    }

    /**
     * Process assignment tree
     * 
     * @param assignmentTree The assignment tree
     */
    private void processAssignmentTree(final AssignmentTree assignmentTree) {
    	log.indent();
    	log.verbose("processAssignmentTree: " + assignmentTree);
    	
    	final ExpressionTree variableExpressionTree = assignmentTree.getVariable();
    	if (variableExpressionTree != null) {
    		treeStack.push(variableExpressionTree);
    		processExpressionTree(variableExpressionTree);
    		treeStack.pop();
    	}
    	
    	final ExpressionTree expressionTree = assignmentTree.getExpression();
    	if (expressionTree != null) {
    	    treeStack.push(expressionTree);
            processExpressionTree(expressionTree);
            treeStack.pop();
    	}
    	
    	log.outdent();
    }

    /**
     * Process the method block 
     * 
     * @param blockTree Block of code to process
     */
     private void processBlockTree(final BlockTree blockTree) {
     	log.indent();
     	log.verbose ("processBlockTree: " + blockTree.getKind());

         for (final StatementTree statementTree : blockTree.getStatements()) {
         	treeStack.push(statementTree);
             processStatementTree(statementTree);        	
             treeStack.pop();
         }

         log.outdent();
     }
    
	/**
	 * Extract class details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 */
    private void processClassTree(final ClassTree classTree) { // NOSONAR
    	log.indent();
		log.verbose("processClassTree: " + classTree.getKind());
		
    	final String className = classTree.getSimpleName().toString();
    	
    	final ModifiersTree modifiersTree = classTree.getModifiers();
    	if (modifiersTree != null) {
    	    treeStack.push(modifiersTree);
    	    processModifiersTree(modifiersTree);
    	    treeStack.pop();
    	}

    	// Primary class name is either the public class, however
    	// if then there is no public class use the package level class
    	// inner classes are ignored
    	final boolean nestedClass = treeStackCount(ClassTree.class) > 1;
    	if (! nestedClass && 
    		classDefinitionData.getPrimaryClassName() == null ||
    		isPublic(modifiersTree)) {	
    	    classDefinitionData.setPrimaryClassName(className);
        	log.verbose ("processClassTree: primaryClassName=" + className);
        }
    	else {
    		classDefinitionData.addClassNameToSecondaryClassNameHashSet(className);
    		log.verbose ("processClassTree: Add secondaryClassName=" + className);
    	}

    	log.verbose ("processClassTree: Add className=" + className);
    	
    	for (final TypeParameterTree typeParameterTree : classTree.getTypeParameters()) {
    		treeStack.push(typeParameterTree);
    		processTypeParameterTree(typeParameterTree);
    		treeStack.pop();
    	}
		
    	final Tree extendsTree = classTree.getExtendsClause();
    	if (extendsTree != null) {
    	    if (extendsTree instanceof IdentifierTree identifierTree) {
    		    treeStack.push(extendsTree);
    		    processIdentifierTree(identifierTree);    		
    	        treeStack.pop();
    	    }
    	    else {
    		    log.warn ("processClassTree: [ExtendsClause] Skip=" + extendsTree.getKind() + " [" + extendsTree + "]");
    	    }
    	}
    	
    	for (final Tree tree : classTree.getImplementsClause()) {
    		treeStack.push(tree);
    		if (tree instanceof IdentifierTree identifierTree) {
    	        processIdentifierTree(identifierTree);
    	    }
    		else {
        	    log.warn ("processClassTree: [ImplementsClause] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
    		treeStack.pop();
    	}

    	for (final Tree tree : classTree.getPermitsClause()) {
    		treeStack.push(tree);
            log.verbose("processClassTree: [Permits] Skip=" + tree.getKind() + ", " + tree);
    		treeStack.pop();
       	}
    	
    	for (final Tree tree : classTree.getMembers()) {
    		treeStack.push(tree);
    		
    		if (tree instanceof ClassTree innerClassTree) {          	
            	processClassTree(innerClassTree);
            }
    		else if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree);
            }
        	else {
        	    log.warn ("processClassTree: [Member] Skip=" + tree.getKind() + " [" + tree + "]");
        	}
            
            treeStack.pop();
		}

    	log.outdent();
    }

	/**
	 * Process compilation unit tree
	 * 
	 * @param compilationUnitTree The compilation unit tree
	 */
	private void processCompilationUnitTree(final CompilationUnitTree compilationUnitTree) {
		log.indent();
		log.verbose("processCompilationUnitTree: " + compilationUnitTree.getKind() + ", " + compilationUnitTree);
		
		final ModuleTree moduleTree = compilationUnitTree.getModule();
		if (moduleTree != null) {
			treeStack.push(moduleTree);
			processModuleTree(moduleTree);
			treeStack.pop();
		}
		
		for (final AnnotationTree annotationTree : compilationUnitTree.getPackageAnnotations()) {
		    treeStack.push(annotationTree);
			processAnnotationTree(annotationTree);
			treeStack.pop();
		}
		
		final ExpressionTree expressionTree = compilationUnitTree.getPackageName();
		if (expressionTree != null) {
			treeStack.push(expressionTree);
			processExpressionTree(expressionTree);
			treeStack.pop();
		}
		
		final PackageTree packageTree = compilationUnitTree.getPackage();
		if (packageTree != null) {
			treeStack.push(packageTree);
			processPackageTree(packageTree);
			treeStack.pop();
		}

		for (final ImportTree importTree : compilationUnitTree.getImports()) {
			treeStack.push(importTree);
			processImportTree(importTree);
			treeStack.pop();
		}
		
		for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {
			treeStack.push(treeDecls);	
			if (treeDecls instanceof ClassTree classTree) {
		        processClassTree(classTree);
			}
			else {
			    log.warn("processCompilationUnitTree: Skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
			}
			treeStack.pop();
		}
		
		log.outdent();
	}
	
	/**
	 * Process the directive tree
	 * 
	 * @param directiveTree The directive tree
	 */
	private void processDirectiveTree(final DirectiveTree directiveTree) {
		log.indent();
		log.verbose("processDirectiveTree: " + directiveTree.getKind() + ", " + directiveTree);
		log.outdent();
	}
	
	/**
     * Process expression statement tree
     * 
     * @param expressionStatementTree The expression statement tree
     */
    private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree) {
    	log.indent();
    	log.verbose("processExpressionStatementTree: " + expressionStatementTree.getKind());
        
        final ExpressionTree expressionTree = expressionStatementTree.getExpression();
        if (expressionTree != null) {
            treeStack.push(expressionTree);
            processExpressionTree(expressionTree);
            treeStack.pop();
        }
        
        log.outdent();
    }
    
    /**
     * Process the expression tree
     * 
     * @param expressionTree The expression tree
     */
     private void processExpressionTree(final ExpressionTree expressionTree) {    	
     	log.indent();
     	log.verbose("processExpressionTree: " + expressionTree.getKind() + " [" + expressionTree + "]");
         
        if (expressionTree instanceof AssignmentTree assignmentTree) {
        	processAssignmentTree(assignmentTree);
         }
         else if (expressionTree instanceof IdentifierTree identifierTree) {
             processIdentifierTree(identifierTree);
         }
         else if (expressionTree instanceof LambdaExpressionTree lambdaExpressionTree) {
             processLambdaExpressionTree(lambdaExpressionTree);
         }
         else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
             processMemberSelectTree(memberSelectTree);
         }
         else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
             processMethodInvocationTree(methodInvocationTree);
         }
         else if (expressionTree instanceof NewClassTree newClassTree) {
         	processNewClassTree(newClassTree);
         }
         else if (expressionTree instanceof ParameterizedTypeTree parameterizedTypeTree) {
         	processParameterizedTypeTree(parameterizedTypeTree);
         }
         else if (expressionTree instanceof LiteralTree literalTree) {
        	 processLiteralTree(literalTree);
         }
         else {
             log.warn("processExpressionTree: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
         }

         log.outdent();
    }
    
   /**
    * Store identifier parameter in referenced classes
    * 
    * @param identifierTree Identifier tree
    */
    private void processIdentifierTree(final IdentifierTree identifierTree) {
    	log.indent();
    	log.verbose("processIdentifierTree: " + identifierTree.getKind() + " [" + identifierTree + "]");
 
        final String className = identifierTree.getName().toString(); 
        addClassNameToPackageClassList(className, "processIdentifierTree");

        log.outdent();
    }

	/**
	 * Process import tree
	 * 
	 * @param importTree The import tree
	 */
	private void processImportTree(final ImportTree importTree) {
		log.indent();
		
		String importStr = importTree.getQualifiedIdentifier().toString();
		log.verbose ("processImportTree: import=" + importStr);
		
		int index = importStr.indexOf(".*");
		if (index >= 0) {
			importStr = importStr.substring(0, index);
			classDefinitionData.getImportList().add(new ImportData(importStr, ImportType.WILDCARD));
		}
		else {
			classDefinitionData.getImportList().add(new ImportData(importStr, ImportType.CLASS_NAME));
		}
		
		log.outdent();
	}
	
	/**
	 * Process lambda expression tree
	 * 
	 * @param lambdaExpressionTree The lambda expression tree
	 */
	private void processLambdaExpressionTree(final LambdaExpressionTree lambdaExpressionTree) {
		log.indent();
        log.verbose("processLambdaExpressionTree: " + lambdaExpressionTree.getKind() + " [" + lambdaExpressionTree + "]");
	 
    	for (final VariableTree variableTree : lambdaExpressionTree.getParameters()) {
    		treeStack.push(variableTree);
    		processVariableTree(variableTree);
    		treeStack.pop();
    	}
    	
    	final Tree tree = lambdaExpressionTree.getBody();
    	if (tree != null) {
    		treeStack.push(tree);
    	    if (tree instanceof BlockTree blockTree) {
                processBlockTree(blockTree);
            }
    	    else if (tree instanceof StatementTree statementTree) {
                processStatementTree(statementTree);
            }
    	    else {
    		    log.warn("processLambdaExpressionTree: skip kind=" + lambdaExpressionTree.getKind() + " [" + lambdaExpressionTree + "]");
    	    }
    	    treeStack.pop();
    	}
    	
    	log.outdent();
	}
	
	/**
	 * Process literal tree
	 * 
	 * @param literalTree The literal tree
	 */
	private void processLiteralTree(final LiteralTree literalTree) {
		log.indent();
        log.verbose("processLiteralTree: " + literalTree.getKind() + " [" + literalTree + "]");
        log.verbose("processLiteralTree: value=" + literalTree.getValue());
        log.outdent();
	}
	
   /**
    * Process member select tree
    * 
    * @param memberSelectTree The member select tree
    */
    private void processMemberSelectTree(final MemberSelectTree memberSelectTree) {  
    	log.indent();
    	log.verbose("processMemberSelectTree: " + memberSelectTree.getKind());

        final String className = memberSelectTree.toString();
        addClassNameToPackageClassList(className, "processMemberSelectTree");
        
        final ExpressionTree expressionTree = memberSelectTree.getExpression();
        if (expressionTree != null) {
        	treeStack.push(expressionTree);
        	processExpressionTree(expressionTree);
        	treeStack.pop();
        }
        
        log.outdent();
    }
	
   /**
    * Process method within a class
    * 
    * @param methodTree The method tree holds methods for the class
    */
    private void processMethodTree(final MethodTree methodTree) {
    	log.indent();
        log.verbose("processMethodTree: " + methodTree.getKind());
 
        final String methodName = methodTree.getName().toString();
        log.verbose ("processMethodTree: Processing methodName=" + methodName);
        
        final ModifiersTree modifiersTree = methodTree.getModifiers();
        if (modifiersTree != null) {
            treeStack.push(modifiersTree);
            processModifiersTree(modifiersTree);
            treeStack.pop();
        }
        
        // Get the return type class
        final Tree returnType = methodTree.getReturnType();
        if (returnType != null) {
        	treeStack.push(returnType);
        	
            if (returnType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree);
            }
            else if (returnType instanceof MemberSelectTree memberSelectTree) {
                processMemberSelectTree(memberSelectTree);
            }
            else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
                processPrimitiveTypeTree(primitiveTypeTree);
            }
            else {
                log.warn("processMethodTree: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
            }
            
            treeStack.pop();
        }
        
        for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
        	treeStack.push(typeParameterTree);
        	processTypeParameterTree(typeParameterTree);
            treeStack.pop();
        }

        // Get the parameters for the method
        for (final VariableTree variableTree : methodTree.getParameters()) {
        	treeStack.push(variableTree);
        	processVariableTree(variableTree);
            treeStack.pop();
        }

        final VariableTree receiverVariableTree = methodTree.getReceiverParameter();
        if (receiverVariableTree != null) {
        	treeStack.push(receiverVariableTree);
        	processVariableTree(receiverVariableTree);
            treeStack.pop();
        }
        
        // Get throws for the method
        for (final ExpressionTree expressionTree : methodTree.getThrows()) {
        	treeStack.push(expressionTree);
        	processExpressionTree(expressionTree);
            treeStack.pop();
        }

        // Method body
        final BlockTree blockTree = methodTree.getBody();
        if (blockTree != null) {
            treeStack.push(blockTree);
            processBlockTree(blockTree);
            treeStack.pop();
        }
        
        log.outdent();
    }

    /**
     * Process method invocation tree
     * 
     * @param methodInvocationTree The method invocation tree
     */
    private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree) {
    	log.indent();
    	log.verbose("processMethodInvocationTree: " + methodInvocationTree.getKind() + " [" + methodInvocationTree + "]");
        
    	for (final Tree tree : methodInvocationTree.getTypeArguments()) {
    		treeStack.push(tree);
    		log.verbose("processMethodInvocationTree: [TypeArguments] Skip=" + tree.getKind() + ", " + tree);
    		treeStack.pop();
    	}
    	
        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        if (expressionTree != null) {
            treeStack.push(expressionTree);
            processExpressionTree(expressionTree);
            treeStack.pop();
        }
        
        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
            treeStack.push(argExpressionTree);
        	processExpressionTree(argExpressionTree);
        	treeStack.pop();
        }

        log.outdent();
    }

    /**
     * Process modifiers tree
     * 
     * @param modifiersTree The modifiers tree
	 */
    private void processModifiersTree(final ModifiersTree modifiersTree) {
    	log.indent();
        log.verbose("processModifiersTree: " + modifiersTree.getKind());
        
    	for (final AnnotationTree annotationTree : modifiersTree.getAnnotations()) {
		    treeStack.push(annotationTree);
		    processAnnotationTree(annotationTree);
		    treeStack.pop();
	    }
    	log.outdent();
    }

	/**
	 * Process the module tree
	 * 
	 * @param moduleTree The module Tree
	 */
	private void processModuleTree(final ModuleTree moduleTree) {
		log.indent();
		log.verbose("processModuleTree: " + moduleTree.getKind() + ", " + moduleTree);
		
		for (final AnnotationTree annotationTree : moduleTree.getAnnotations()) {
			treeStack.push(annotationTree);
			processAnnotationTree(annotationTree);
			treeStack.pop();
		}
		
	    final ExpressionTree expressionTree = moduleTree.getName();
	    if (expressionTree != null) {
	        treeStack.push(expressionTree);
	    	processExpressionTree(expressionTree);
	    	treeStack.pop();
	    }
		
	    for (final DirectiveTree directiveTree : moduleTree.getDirectives()) {
	    	treeStack.push(directiveTree);
	    	processDirectiveTree(directiveTree);
	    	treeStack.pop();
	    }

		log.outdent();
	}
	
   /**
    * Process new class tree
    * 
    * @param newClassTree The new class tree
    */
    private void processNewClassTree(final NewClassTree newClassTree) {
    	log.indent();
    	log.verbose ("processNewClassTree: " + newClassTree.getKind());

    	final ExpressionTree enclosingExpressionTree = newClassTree.getEnclosingExpression();
    	if (enclosingExpressionTree != null) {
    		treeStack.push(enclosingExpressionTree);
    		processExpressionTree(enclosingExpressionTree);
    		treeStack.pop();
    	}
    	
    	for (final Tree tree : newClassTree.getTypeArguments()) {
    		treeStack.push(tree);
    		log.verbose ("processNewClassTree: [TypeArguments] Skip=" + tree.getKind() + ", " + tree);
    		treeStack.pop();
    	}
    	
        final ExpressionTree identifierExpressionTree = newClassTree.getIdentifier();
        if (identifierExpressionTree != null) {
        	treeStack.push(identifierExpressionTree);
            processExpressionTree(identifierExpressionTree);
            treeStack.pop();
        }
        
        for (final ExpressionTree expressionTree : newClassTree.getArguments()) {
        	treeStack.push(expressionTree);
        	processExpressionTree(expressionTree);
        	treeStack.pop();
        }
        
        final ClassTree classTree = newClassTree.getClassBody();
        if (classTree != null) {
        	treeStack.push(classTree);
        	processClassTree(classTree);
        	treeStack.pop();
        }
        
        log.outdent();
    }

	/**
	 * Process package tree
	 * 
	 * @param packageTree The package tree	 
	 */
	private void processPackageTree(final PackageTree packageTree) {
		log.indent();
		log.verbose("processPackageTree: " + packageTree.getKind());
		
		final String packageName = packageTree.getPackageName().toString();
    	log.verbose ("processPackageTree: packageName=" + packageName);
    	classDefinitionData.setPackageName(packageName);

    	for (final AnnotationTree annotationTree : packageTree.getAnnotations()) {
            treeStack.push(annotationTree);
        	processAnnotationTree(annotationTree);
        	treeStack.pop();
        }
    	
    	log.outdent();
	}

    /**
     * Process parameterized type tree
     * 
     * @param parameterizedTypeTree The oarameterized type tree    
	 */
    private void processParameterizedTypeTree(final ParameterizedTypeTree parameterizedTypeTree) {
    	log.indent();
    	log.verbose("processParameterizedTypeTree: " + parameterizedTypeTree.getKind());
    	
        final Tree typeTree = parameterizedTypeTree.getType();
        treeStack.push(typeTree);
        final String className = typeTree.toString();
        addClassNameToPackageClassList(className, "processParameterizedTypeTree");
        treeStack.pop();
        
        for (final Tree tree : parameterizedTypeTree.getTypeArguments()) {
        	treeStack.push(typeTree);
        	log.verbose("processParameterizedTypeTree: [TypeArguments] Skip=" + tree.getKind() + ", " + tree);
        	treeStack.pop();
        }
        
        log.outdent();
    }
    
    /**
     * Process primitive type tree
     * 
     * @param primitiveTypeTree The primitive type tree
     */
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree) {
    	log.indent();
    	log.verbose("processPrimitiveTypeTree: " + primitiveTypeTree.getKind() + ", " + primitiveTypeTree);
        log.outdent();
    }
    
    /**
     * Process return type
     * 
     * @param returnTree Return tree
     */
     private void processReturnTree(final ReturnTree returnTree) {
     	log.indent();
     	log.verbose ("processReturnTree: " + returnTree.getKind());

         final ExpressionTree expressionTree = returnTree.getExpression();
         if (expressionTree != null) {
             treeStack.push(expressionTree);
             processExpressionTree(expressionTree);
             treeStack.pop();
         }
         
         log.outdent();
     }

    /** 
     * Process the statement tree
     * 
     * @param statementTree The statement tree
     */
    private void processStatementTree(final StatementTree statementTree) {
    	log.indent();
    	log.verbose ("processStatementTree: " + statementTree.getKind()); 
    	
    	if (statementTree instanceof ExpressionStatementTree expressionStatementTree) {
            processExpressionStatementTree(expressionStatementTree);
        }
        else if (statementTree instanceof ReturnTree returnTree) {
            processReturnTree(returnTree);
        }
        else if (statementTree instanceof VariableTree variableTree) {
            processVariableTree(variableTree);
        }
        else {
            log.warn ("processStatementTree: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
        }
    	
    	log.outdent();
    }
    
    /**
     * Get generic type name e.g. <T> and store in list
     * used to remove from class list any objects with type name T
     * 
     * @param typeParameterTree The type parameter tree
     */
    private void processTypeParameterTree(final TypeParameterTree typeParameterTree) {
    	log.indent();
		log.verbose("processTypeParameterTree: " + typeParameterTree.getKind());
		
		final String typeName = typeParameterTree.getName().toString();
		classDefinitionData.addTypeNameToGenericHashSet(typeName);

		for (final Tree tree : typeParameterTree.getBounds()) { 
			treeStack.push(tree);
			log.verbose("processTypeParameterTree: [Bounds] Skip=" + tree.getKind() + ", " + tree);
			treeStack.pop();
		}
		
    	for (final AnnotationTree annotationTree : typeParameterTree.getAnnotations()) {
            treeStack.push(annotationTree);
        	processAnnotationTree(annotationTree);
        	treeStack.pop();
        }
    	
		log.outdent();
    }
    
    /**
     * Process variable tree
     * 
     * @param variableTree The variable tree
	 */
    private void processVariableTree(final VariableTree variableTree) {
    	log.indent();
        log.verbose("processVariableTree: " + variableTree.getKind());
        
        final ModifiersTree modifiersTree = variableTree.getModifiers();
        treeStack.push(modifiersTree);
        processModifiersTree(modifiersTree);
        treeStack.pop();
        
        final ExpressionTree nameExpressionTree = variableTree.getNameExpression();
        if (nameExpressionTree != null) {
        	treeStack.push(nameExpressionTree);
        	processExpressionTree(nameExpressionTree);
        	treeStack.pop();
        }
        
        final Tree tree = variableTree.getType();
        if (tree != null) {
            treeStack.push(tree);
        
            if (tree instanceof ArrayTypeTree arrayTypeTree) {
            	processArrayTypeTree(arrayTypeTree);
            }
            else if (tree instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree);
            }
            else if (tree instanceof MemberSelectTree memberSelectTree) {
        	    processMemberSelectTree(memberSelectTree);
            }
            else if (tree instanceof ParameterizedTypeTree parameterizedTypeTree) {
        	    processParameterizedTypeTree(parameterizedTypeTree);
            }
            else if (tree instanceof PrimitiveTypeTree primitiveTypeTree) {
            	processPrimitiveTypeTree(primitiveTypeTree);
            }
            else {
                log.warn("processVariableTree: Skip kind=" + tree.getKind() + "[" + tree + "]");
            }
            treeStack.pop();
        }

        final ExpressionTree initializerExpressionTree = variableTree.getInitializer();
        if (initializerExpressionTree != null) {
        	treeStack.push(initializerExpressionTree);
        	processExpressionTree(initializerExpressionTree);
        	treeStack.pop();
        }

        log.outdent();
    }
        
    /**
     * Test if we can add the class name to the class and package list
     * - if it is a generic type skip it.
     * 
     * @param className The class to test and add
     * @param methodName The calling method name
     */
    private void addClassNameToPackageClassList(final String className,
    		                                    final String methodName) {
    	log.indent();

    	if (LOG_ADD_CLASS) {
    	    treeStackLog();
    	}

    	final boolean addClass = doesTreeStackHoldClassName();
        if (addClass) {
            if (! classDefinitionData.isGenericType(className)) {
                if (LOG_ADD_CLASS) {
            	    log.info (methodName + "#: Add className=" + className);
                }
                classDefinitionData.addClassNameToPackageClassList(className);
            }
            else {
            	if (LOG_ADD_CLASS) {
            	    log.info (methodName + "#: Genric type - not adding className=" + className);
                }
            }
    	}
    	else {
    		if (LOG_ADD_CLASS) {
    		    log.info (methodName + "#: NOT adding className=" + className);
    		}
    	}
        
        log.outdent();
    }

	/**
	 * Test the tree stack to determine if the item is a class name
	 
	 * @return True if it is a class name, false if not
	 */
	private boolean doesTreeStackHoldClassName() {
		if (treeStackMatches(IdentifierTree.class, ClassTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(IdentifierTree.class, MethodTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(IdentifierTree.class, NewClassTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(IdentifierTree.class, ParameterizedTypeTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(IdentifierTree.class, VariableTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(MemberSelectTree.class, MethodTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(MemberSelectTree.class, NewClassTree.class)) {
    		return true;
        }
    	
    	if (treeStackMatches(MemberSelectTree.class, VariableTree.class)) {
    		return true;
        }
    	
		return false;
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
     * Start at the bottom of the tree stack, going up, compare each element
     * class type to the ones passed in. If each element matches the passed in
     * list up to the passed in list length then true is returned, else false.
     * 
     * @param matchers List of classes to match against
     * 
     * @return If matches map to the tree stack going upwards.
     */
    @SafeVarargs
	private boolean treeStackMatches(final Class<? extends Tree> ...matchers) {
    	final Iterator<Tree> iterator = treeStack.iterator();
    	
    	for (int i = 0; i < matchers.length && iterator.hasNext(); i++) {
    		final Tree tree = iterator.next();
    		if (! matchers[i].isInstance(tree)) {
    			return false;
    		}
    	}
    	
    	return true;
    }

    /**
     * Count number of occurrences of a next class
     * 
     * @param matcher The class to match on
     * 
     * @return CountTrue if a nested class, false if not
     */
    private int treeStackCount(final Class<? extends Tree> matcher) {
    	int count = 0;
    	
    	final Iterator<Tree> iterator = treeStack.iterator();
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		if (matcher.isInstance(tree)) {
    			count++;
    		}
    	}
    	
    	return count;
    }

    /**
     * Print out the tree stack in reverse traversed order
     */
    private void treeStackLog() {
    	final Iterator<Tree> iterator = treeStack.iterator();
    	
    	final StringBuilder sb = new StringBuilder();
    	sb.append("treeStackLog: ");
    	
    	while (iterator.hasNext()) {
    		final Tree tree = iterator.next();
    		sb.append(tree.getKind());
    		
    		if (iterator.hasNext()){
    			sb.append(" -> ");
    		}
    	}
    	
    	log.info(sb.toString());
    }
}
