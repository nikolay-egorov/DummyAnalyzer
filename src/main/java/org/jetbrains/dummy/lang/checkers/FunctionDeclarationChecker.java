package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static util.ClassMatcher.match;

/**
 * Checks that function called was declared
 */
public class FunctionDeclarationChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private Set<Pair<String, Integer>> declaredFunc = new HashSet<>();

    public FunctionDeclarationChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }


    private void reportUndeclaredFunction(FunctionCall functionCall) {
        StringBuilder b = new StringBuilder("Function '");
        b.append(functionCall.getFunction());
        b.append("' called but was never declared");

        reporter.report(functionCall, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        innerFunc.forEach(i -> declaredFunc.add(new Pair<>(i.getName(), i.getParameters().size())));
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
        if (ifStatement.getThenBlock() != null){
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
        if (!declaredFunc.contains(new Pair<>(functionCall.getFunction(), functionCall.getArguments().size()))) {
            reportUndeclaredFunction(functionCall);
        }
    }

}
