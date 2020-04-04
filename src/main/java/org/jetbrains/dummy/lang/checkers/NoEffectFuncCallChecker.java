package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.ClassMatcher.match;

/**
 * Finds func call having no effect
 * Checks only in if conditions, return statements and in-func call statements
 */
public class NoEffectFuncCallChecker extends AbstractChecker {

    private DiagnosticReporter reporter;

    private Map<Pair<String, Integer>, Expression> funcReturns = new HashMap<>();

    public NoEffectFuncCallChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    public void reportPointlessCall(FunctionCall functionCall) {
        StringBuilder b = new StringBuilder("Function call '");
        b.append(functionCall.getFunction());
        b.append("' seems to have no effect");

        reporter.report(functionCall, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        for (FunctionDeclaration functionDeclaration : innerFunc) {
            Expression returnV = null;
            for (Statement statement : functionDeclaration.getBody().getStatements()) {
                if (statement instanceof ReturnStatement) {
                    returnV = ((ReturnStatement) statement).getResult();
                }
            }
            String name = functionDeclaration.getName();
            int argsS = functionDeclaration.getParameters().size();
            Pair<String, Integer> funcCandidate = new Pair<>(name, argsS);
            funcReturns.put(funcCandidate, returnV);
        }

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            visitCodeBlock(functionDeclaration.getBody());
        }

    }


    private void visitCodeBlock(Block block) {
        for (Statement statement : block.getStatements()) {
            visitStatement(statement);
        }
    }

    private void visitStatement(Statement statement) {
        match().with(IfStatement.class, this::visitIfStatement)
                .with(FunctionCall.class, this::visitFuncCall)
                .with(ReturnStatement.class, this::visitReturnStatement)
                .exec(statement);
    }

    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) ifStatement.getCondition());
        }

        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }

    private void visitFuncCall(FunctionCall functionCall) {
        String name = functionCall.getFunction();
        int agsS = functionCall.getArguments().size();
        Pair<String, Integer> candFunc = new Pair<>(name, agsS);
        if (funcReturns.get(candFunc) == null) {
            reportPointlessCall(functionCall);
        }
    }

    private void visitReturnStatement(ReturnStatement returnStatement) {
        if (returnStatement.getResult() instanceof FunctionCall) {
            visitFuncCall((FunctionCall) returnStatement.getResult());
        }
    }


}
