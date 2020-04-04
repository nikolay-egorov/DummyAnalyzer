package org.jetbrains.dummy.lang.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.io.PrintStream;
import java.util.*;

import static util.ClassMatcher.match;

/**
 * Proofs poor performance
 */
@Deprecated
public class InfiniteRecursionChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private HashMap<FunctionDeclaration, Set<List<String>>> perFuncReturnCont = new HashMap<>();
    private HashMap<FunctionCall, Set<List<Expression>>> callLog = new HashMap<>();
    private FunctionDeclaration currPointer;

    public InfiniteRecursionChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    public void reportInfiniteLoop(FunctionCall functionCall) {
        StringBuilder b = new StringBuilder("Function '");
        b.append(functionCall.getFunction());
        b.append("' call may result in an infinite loop");

        reporter.report(functionCall, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        for (FunctionDeclaration functionDeclaration : innerFunc) {
            perFuncReturnCont.put(functionDeclaration, new HashSet<>());
//            List<String>  args = functionDeclaration.getParameters();
//            perFuncReturnCont.get(functionDeclaration).add(args);
        }

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            visitFuncDecl(functionDeclaration);
            perFuncReturnCont.put(functionDeclaration, new HashSet<>());
        }
    }

    private void visitFuncDecl(FunctionDeclaration functionDeclaration) {
        currPointer = functionDeclaration;
        visitCodeBlock(functionDeclaration.getBody());
    }

    private void visitCodeBlock(Block codeBlock) {
        for (Statement statement : codeBlock.getStatements()) {
            if (statement instanceof FunctionCall) {
                visitFuncCall((FunctionCall) statement);
            }
            else {
                visitStatement(statement);
            }
        }
    }

    private void visitStatement(Statement statement) {
        match().with(IfStatement.class, this::visitIfStatement)
                .with(VariableDeclaration.class, this::visitVarDecl)
                .with(Assignment.class , this::visitVarAssignment)
                .with(FunctionCall.class, this::visitFuncCall)
                .with(ReturnStatement.class, this::visitReturnStat)
                .exec(statement);
    }

    private void visitIfStatement(IfStatement ifStatement){
        if (ifStatement.getCondition() != null) {
//            if (ifStatement.getCondition() instanceof FunctionCall) {
//                visitFuncCall((FunctionCall) ifStatement.getCondition());
//            }
            visitStatement(ifStatement.getCondition());
        }
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }

        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }


    private void visitVarDecl(VariableDeclaration variableDeclaration) {
        if (variableDeclaration.getInitializer() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) variableDeclaration.getInitializer());
        }
    }

    private void visitVarAssignment(Assignment assignment) {
        if (assignment.getRhs() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) assignment.getRhs());
        }
    }

    private void visitFuncCall(FunctionCall functionCall) {
        String name = functionCall.getFunction();
        List<Expression> args = functionCall.getArguments();
        List<String> stringArgs = new ArrayList<>();
        for (Expression arg : args) {
            if (arg instanceof VariableAccess) {
                stringArgs.add(((VariableAccess) arg).getName());
            }
            if (arg instanceof FunctionCall) {
                stringArgs.add(((FunctionCall) arg).getFunction());
            }
            else if (arg instanceof BooleanConst)  {
                stringArgs.add(String.valueOf(((BooleanConst) arg).getValue()));
            }
            else if (arg instanceof IntConst) {
                stringArgs.add(String.valueOf(((IntConst) arg).getValue()));
            }
        }
        // update curr pointer
        for (Map.Entry<FunctionDeclaration, Set<List<String>>> declarationSetEntry : perFuncReturnCont.entrySet()) {
            String fName = declarationSetEntry.getKey().getName();
            List<String> fArgs = null;
            for (List<String> expressions : declarationSetEntry.getValue()) {
                if (stringArgs.equals(expressions)) {
                    fArgs = expressions;
                }

            }
            if (name.equals(fName) && stringArgs.equals(fArgs)) {
                currPointer = declarationSetEntry.getKey();
            }
        }


        if (perFuncReturnCont.containsKey(currPointer)) {
            if (perFuncReturnCont.get(currPointer).contains(stringArgs)) {
                reportInfiniteLoop(functionCall);
            }
            else {
//                perFuncReturnCont.get(currPointer).add(functionCall.getArguments());
                perFuncReturnCont.get(currPointer).add(stringArgs);
                // visit func decl
                visitFuncDecl(currPointer);
            }
        }


        for (Statement statement : currPointer.getBody().getStatements()) {
            if (statement instanceof ReturnStatement) {
                if (((ReturnStatement) statement).getResult().equals(functionCall)) {
                    reportInfiniteLoop(functionCall);
                }
            }
        }

    }

    private void visitReturnStat(ReturnStatement statement) {

//        if (((ReturnStatement) statement).getResult()) {

//        }
    }


}
