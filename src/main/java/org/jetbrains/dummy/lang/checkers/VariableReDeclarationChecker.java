package org.jetbrains.dummy.lang.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.HashMap;
import java.util.List;

import static util.ClassMatcher.match;


public class VariableReDeclarationChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private HashMap<String, Integer> perFuncDecl = new HashMap<>();

    public VariableReDeclarationChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private void reportVariableRedeclaration(@NotNull VariableDeclaration declaration) {
        StringBuilder b = new StringBuilder("Variable '");
        b.append(declaration.getName());
        b.append("' was already declared on line ");
        b.append(perFuncDecl.get(declaration.getName()));
        reporter.report(declaration, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            perFuncDecl.clear();
            Block func_block = functionDeclaration.getBody();
            visitCodeBlock(func_block);
        }
    }

    private void visitCodeBlock(Block codeBlock) {
        List<Statement> statements = codeBlock.getStatements();
        for (Statement statement : statements) {
            match().with(VariableDeclaration.class, this::visitDeclaration)
                    .with(IfStatement.class, this::visitIfStatement)
                    .exec(statement);
        }
    }

    private void visitDeclaration(VariableDeclaration variableDeclaration) {
        if (perFuncDecl.containsKey(variableDeclaration.getName())) {
            reportVariableRedeclaration(variableDeclaration);
        }
        else {
            perFuncDecl.put(variableDeclaration.getName(),variableDeclaration.getLine());
        }
    }


    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }

}
