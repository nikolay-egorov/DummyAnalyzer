package org.jetbrains.dummy.lang.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;


import java.util.*;

import static util.ClassMatcher.match;

/**
 * Checks whatever variable was declared in func scope
 */
public class VariableDeclarationChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private Map<String, Integer> varDecl = new HashMap<>();
    public VariableDeclarationChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }
    private static Set<String> keyWords = new HashSet<>(Arrays.asList( "var", "fun", "return", "if", "else", "="));

    private void reportUndefinedVariable(Statement variableAccess) {
        StringBuilder b = new StringBuilder("Variable '");
        if (variableAccess instanceof VariableAccess)
            b.append(((VariableAccess) variableAccess).getName());
        else if (variableAccess instanceof Assignment)
            b.append(((Assignment) variableAccess).getVariable());
        else b.append(variableAccess.toString());
        b.append("' was referenced but never declared");

        reporter.report(variableAccess, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        varDecl.clear();
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
                .with(FunctionCall.class, this::visitFunCall)
                .with(VariableDeclaration.class, this::visitVarDeclaration)
                .with(Assignment.class, this::visitVarAssign)
                .with(VariableAccess.class, this::visitVariableAccess)
                .exec(statement);
    }

    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() != null) {
            visitStatement(ifStatement.getCondition());
        }
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }


    private void visitFunCall(FunctionCall functionCall) {
        for (Expression argument : functionCall.getArguments()) {
            if (argument instanceof VariableAccess) {
                visitVariableAccess((VariableAccess) argument);
            }
        }
    }

    private void visitVarDeclaration(VariableDeclaration variableDeclaration) {
        if (keyWords.contains(variableDeclaration.getName())) {
            reportUndefinedVariable(variableDeclaration);
        }
        if (!varDecl.containsKey(variableDeclaration.getName())) {
            varDecl.put(variableDeclaration.getName(), variableDeclaration.getLine());
        }
    }

    private void visitVarAssign(Assignment assignment) {
        if (!varDecl.containsKey(assignment.getVariable())) {
            reportUndefinedVariable(assignment);
        }
    }

    private void visitVariableAccess(VariableAccess variableAccess) {
        if (!varDecl.containsKey(variableAccess.getName())) {
            reportUndefinedVariable(variableAccess);
        }
    }



}
