package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.*;

import static util.ClassMatcher.match;


/**
 * Checks if func call matching by name is valid but mismatches arguments number
 */
public class FunctionMissingArgsChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private Map<String, Integer> declaredFunc = new HashMap<>();
    private Map<Pair<String, Integer>, Boolean> ifReturnsAny = new HashMap<>();

    public FunctionMissingArgsChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    public void reportMissingArguments(FunctionCall functionCall) {
        StringBuilder b = new StringBuilder("Function '");
        b.append(functionCall.getFunction());
        b.append("' called with unexpected ").append(functionCall.getArguments().size());
        if (functionCall.getArguments().size() == 1) {
            b.append(" argument");
        }
        else b.append(" arguments");

        reporter.report(functionCall, b.toString());
    }

    public void inspectOnReturnValue(List<FunctionDeclaration> innerFunc, Boolean firstTime) {
        for (FunctionDeclaration functionDeclaration : innerFunc) {
            Pair<String, Integer> curFunc = new Pair<>(functionDeclaration.getName(),
                    functionDeclaration.getParameters().size());
            Boolean ifReturnsValue = false;

            for (Statement statement : functionDeclaration.getBody().getStatements()) {
                if (statement instanceof ReturnStatement && ((ReturnStatement) statement).getResult() != null) {
                    if (!firstTime) {
                        if (((ReturnStatement) statement).getResult() instanceof FunctionCall) {
                            Boolean ifChainedReturnValue =  ensureReturnValueForFuncCallAsArg(
                                                (FunctionCall) ((ReturnStatement) statement).getResult());
                            ifReturnsValue = ifChainedReturnValue;
                        }
                    }
                    else {
                        //TODO: think about recursive return
                        ifReturnsValue = true;
                    }
                }
            }
            ifReturnsAny.put(curFunc, ifReturnsValue);
        }
    }


    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        innerFunc.forEach(i -> declaredFunc.putIfAbsent(i.getName(), i.getParameters().size()));
        inspectOnReturnValue(innerFunc, false);
        inspectOnReturnValue(innerFunc, true);

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            visitCodeBlock(functionDeclaration.getBody());
        }
    }

    private void visitCodeBlock(Block block) {
        List<Statement> statements = block.getStatements();
        for (Statement statement : statements) {
            visitStatement(statement);
        }
    }


    private void visitStatement(Statement statement) {
        match().with(VariableDeclaration.class, this::visitVarDeclar)
                .with(IfStatement.class, this::visitIfStatement)
                .with(Assignment.class, this::visitAssignmentExpr)
                .with(ReturnStatement.class, this::visitReturnStatement)
                .with(FunctionCall.class, this::visitFuncCall)
                .exec(statement);
    }


    private void visitVarDeclar(VariableDeclaration variableDeclaration) {
        if (variableDeclaration.getInitializer() != null && variableDeclaration.getInitializer() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) variableDeclaration.getInitializer());
        }
    }

    private void visitReturnStatement(ReturnStatement returnStatement) {
        if (returnStatement.getResult() != null && returnStatement.getResult() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) returnStatement.getResult());
        }
    }

    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() != null && ifStatement.getCondition() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) ifStatement.getCondition());
        }
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }


    private void visitAssignmentExpr(Assignment assignment) {
        if (assignment.getRhs() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) assignment.getRhs());
        }
    }


    private void visitFuncCall(FunctionCall functionCall) {

        if (declaredFunc.get(functionCall.getFunction()) != functionCall.getArguments().size()) {
            reportMissingArguments(functionCall);
        }

        for (Expression argument : functionCall.getArguments()) {
            if (argument instanceof FunctionCall && !ensureReturnValueForFuncCallAsArg((FunctionCall) argument)) {
                reportMissingArguments(functionCall);
            }
        }
    }

    private Boolean ensureReturnValueForFuncCallAsArg(FunctionCall functionCall) {
        return ifReturnsAny.get(new Pair<>(functionCall.getFunction(), functionCall.getArguments().size()));
    }

}
