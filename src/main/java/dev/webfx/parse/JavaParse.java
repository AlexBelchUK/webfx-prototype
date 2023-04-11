package dev.webfx.parse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

public class JavaParse {
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
	public List<ClassDefinitionData> parse(final List<File> pathFileList, 
                                           final List<String> excludedImportList) throws IOException {

	    final List<ClassDefinitionData> classDefinitionList = new ArrayList<>();
		
		final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		final StandardJavaFileManager standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);		 
		final Iterable<? extends JavaFileObject> javaFileObjects = standardJavaFileManager.getJavaFileObjectsFromFiles(pathFileList);
		final JavacTask javacTask = (JavacTask) javaCompiler.getTask(null, standardJavaFileManager, null, null, null, javaFileObjects);         
		final Iterable<? extends CompilationUnitTree> compilationUnitTrees = javacTask.parse();
		 
		for (final CompilationUnitTree compilationUnitTree : compilationUnitTrees) {
		 	
		    String packageName = null;	
			final PackageTree packageTree = compilationUnitTree.getPackage();
			if (packageTree != null) {
			    packageName = packageTree.getPackageName().toString();
		    	logText ("parse[01]: packageName=" + packageName);
		    }
			
			final List<ImportData> importList = new ArrayList<>();
			for (final ImportTree importTree : compilationUnitTree.getImports()) {
		    	String importStr = importTree.getQualifiedIdentifier().toString();
		    	logText ("parse[02]: import=" + importStr);
		    	
		    	int index = importStr.indexOf(".*");
		    	if (index >= 0) {
		    		importStr = importStr.substring(0, index);
		    		importList.add(new ImportData(importStr, ImportType.WILDCARD));
		    	}
		    	else {
		    	    importList.add(new ImportData(importStr, ImportType.CLASS_NAME));
		    	}
		    }
	
			final ClassDefinitionData classDefinitionData = new ClassDefinitionData(packageName, importList);
		    classDefinitionList.add(classDefinitionData);
			
		    for (final Tree treeDecls : compilationUnitTree.getTypeDecls()) {		    	 		    	 
		        if (treeDecls instanceof ClassTree classTree) {
		            processClassTree(classTree, classDefinitionData);
		    	}
		    	else {
				    logText ("parse[03]: skip=" + treeDecls.getKind() + " [" + treeDecls + "]");
				}
		    }
		}
		
		return classDefinitionList;
    }
	
	/**
	 * Extract class details from a tokenised class
	 * 
	 * @param classTree Class tree to navigate
	 * @param classDefinitionData Class definition data
	 */
    private void processClassTree(final ClassTree classTree,
                                  final ClassDefinitionData classDefinitionData) {
		logIndent();
		
    	final String className = classTree.getSimpleName().toString();

// @TODO - check if only class defined (or if several the public one)
    	classDefinitionData.setPrimaryClassName(className);
    	logText ("processClassTree[01]: Add className=" + className);
		
    	for (final Tree tree : classTree.getMembers()) {        	
            if (tree instanceof MethodTree methodTree) {
		  	    processMethodTree(methodTree, classDefinitionData);
			}
            else if (tree instanceof VariableTree variableTree) {
            	processVariableTree(variableTree, classDefinitionData);
            }
        	else {
        	    logText ("processClassTree[02]: Skip=" + tree.getKind() + " [" + tree + "]");
        	}
		}

    	logOutdent();
   	}
    
    /**
     * 
     * @param variableTree
     * @param classDefinitionData
     */
    private void processVariableTree(final VariableTree variableTree, 
                                     final ClassDefinitionData classDefinitionData) {
        logIndent();

        final Tree tree = variableTree.getType();
        if (tree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, classDefinitionData);
        }
        else {
            logText("processVariableTree[01]: Skip kind=" + tree.getKind() + "[" + tree + "]");
        }

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

        final String methodName = methodTree.getName().toString();
        logText ("processMethodTree[01]: Processing methodName=" + methodName);

        for (TypeParameterTree typeParameterTree : methodTree.getTypeParameters()) {
            logText ("processMethodTree[02]: Skip type parameter kind=" + typeParameterTree.getKind() + " [" + typeParameterTree + "]");
        }

        // Get the return type class
        final Tree returnType = methodTree.getReturnType();
        if (returnType != null) {
            if (returnType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else if (returnType instanceof PrimitiveTypeTree primitiveTypeTree) {
                processPrimitiveTypeTree(primitiveTypeTree, classDefinitionData);
            }
            else {
                logText("processMethodTree[03]: Skip return kind=" + returnType.getKind() + " [" + returnType + "}");
            }
        }

        // Get the parameters for the method
        for (final VariableTree variableTree : methodTree.getParameters()) {
            final String variableName = variableTree.getName().toString();
            logText ("processMethodTree[04]: Processing variableName=" + variableName);

            final Tree variableType = variableTree.getType();
            if (variableType instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else {
                logText ("processMethodTree[05]: Skip param kind=" + variableType.getKind() + " [" + variableType + "]");
            }
        }

        // Get throws for the method
        for (final ExpressionTree expressionTree : methodTree.getThrows()) {
            if (expressionTree instanceof IdentifierTree identifierTree) {
                processIdentifierTree(identifierTree, classDefinitionData);
            }
            else {
               logText ("processMethodTree[06]: Skip throws kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
            }
        }

        // Method body
        final BlockTree blockTree = methodTree.getBody();
        processBlockTree(blockTree, classDefinitionData);

        logOutdent();
    }

    /**
     * 
     * @param primitiveTypeTree
     * @param classDefinitionData
     */
    private void processPrimitiveTypeTree(final PrimitiveTypeTree primitiveTypeTree, 
                                          final ClassDefinitionData classDefinitionData) {
        logIndent();

        final TypeKind typeKind = primitiveTypeTree.getPrimitiveTypeKind();
        logText("processPrimitiveTypeTree[01]: Typekind=" + typeKind);

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

        final String className = identifierTree.getName().toString();

logText("processIdentifierTree: --> kind=" + identifierTree.getKind());
logText ("processIdentifierTree: Add className=" + className);

        classDefinitionData.getPackageClassList().add(new PackageClassData(null, className, ResolveState.UNKNOWN));
       
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

        logText ("processBlockTree[01]: [" + blockTree + "]");

        for (final StatementTree statementTree : blockTree.getStatements()) {
            logText("processBlockTree[02]: >>>> statementTree.kind=" + statementTree.getKind() + " [" + statementTree + "]");
   
            if (statementTree instanceof ReturnTree returnTree) {
                processReturnTree(returnTree, classDefinitionData);
            }
            else if (statementTree instanceof ExpressionStatementTree expressionStatementTree) {
                processExpressionStatementTree(expressionStatementTree, classDefinitionData);
            }
            else {
                logText ("processBlockTree[03]: Skip statement kind=" + statementTree.getKind() + " [" + statementTree + "]");
            }
        }

        logOutdent();
    }

    /**
     * 
     * @param expressionStatementTree
     * @param classDefinitionData
     */
    private void processExpressionStatementTree(final ExpressionStatementTree expressionStatementTree,
                                                final ClassDefinitionData classDefinitionData) {
        logIndent();

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

        logText ("processReturnTree[01]: [" + returnTree + "]");

        final ExpressionTree expressionTree = returnTree.getExpression();		
        if (expressionTree instanceof NewClassTree newClassTree) {
            processNewClassTree(newClassTree, classDefinitionData);			
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, classDefinitionData);
        }
        else {
            logText ("processReturnTree[02]: Skip expression kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
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

        logText ("processNewClassTree[01]: [" + newClassTree + "]");

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

        if (expressionTree instanceof IdentifierTree identifierTree) {
            processIdentifierTree(identifierTree, classDefinitionData);
        }
        else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
            processMemberSelectTree(memberSelectTree, classDefinitionData);
        }
        else if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
            processMethodInvocationTree(methodInvocationTree, classDefinitionData);
        }
        else {
            logText("processExpressionTree[01]: skip kind=" + expressionTree.getKind() + " [" + expressionTree + "]");
        }

        logOutdent();
    }

   /**
    * 
    * @param memberSelectTree
    * @param classDefinitionData
    */
    private void processMemberSelectTree(final MemberSelectTree memberSelectTree,
                                         final ClassDefinitionData classDefinitionData) {
        logIndent();

        final ExpressionTree expressionTree = memberSelectTree.getExpression();
        processExpressionTree(expressionTree, classDefinitionData);

        logOutdent();
    }

    /**
     *
     * @param methodInvocationTree
     * @param classDefinitionData
     */
    private void processMethodInvocationTree(final MethodInvocationTree methodInvocationTree,
                                             final ClassDefinitionData classDefinitionData) {
        logIndent();

        final ExpressionTree expressionTree = methodInvocationTree.getMethodSelect();
        processExpressionTree(expressionTree, classDefinitionData);

        for (final ExpressionTree argExpressionTree : methodInvocationTree.getArguments()) {
            processExpressionTree(argExpressionTree, classDefinitionData);	
        }

        logOutdent();
    }
}
