package org.jetbrains.dummy.lang.checkers;


import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;

import static util.ClassMatcher.match;

public class VariableInitializationCheckerN extends AbstractChecker {

    private DiagnosticReporter reporter;
    //TODO: perhaps don't need varDecl variable
    //
    private LinkedHashMap<String, Pair<VariableDeclaration, Expression>> varDeclOrder = new LinkedHashMap
        <String, Pair<VariableDeclaration, Expression>>();

    public VariableInitializationCheckerN(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    // Use this method for reporting errors
    private void reportAccessBeforeInitialization(@NotNull VariableAccess access) {
        StringBuilder b = new StringBuilder("Variable '");
        b.append(access.getName());
        b.append("' is accessed before initialization");

        reporter.report(access, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc =  file.getFunctions();

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            varDeclOrder.clear();
            Block func_block = functionDeclaration.getBody();
            visitCodeBlock(func_block);
        }
    }

    private void visitCodeBlock(Block block) {
        List<Statement> statements = block.getStatements();
        for (Statement statement : statements) {
            match().with(VariableDeclaration.class, this::visitDeclaration)
                    .with(Assignment.class, this::visitAssignment)
                    .with(IfStatement.class, this::visitIfStatement)
                    .with(FunctionCall.class, this::visitFunCall)
                    .with(VariableAccess.class, this::visitVariableAccess)
                    .exec(statement);
        }
    }


    private void visitDeclaration(VariableDeclaration varDecl) {
        varDeclOrder.put(varDecl.getName(), new Pair<>(varDecl,
                varDecl.getInitializer()));

    }

    private void visitAssignment(Assignment assignment) {
        if (varDeclOrder.containsKey(assignment.getVariable())) {

            varDeclOrder.put(assignment.getVariable(),
                    new Pair<>(varDeclOrder.get(assignment.getVariable()).getKey(),
                            assignment.getRhs()));
        }
    }

    private void visitIfStatement(IfStatement ifStatement){
        if (ifStatement.getCondition() instanceof VariableAccess) {
            visitVariableAccess((VariableAccess) ifStatement.getCondition());
        }
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }

    private void visitFunCall(FunctionCall functionCall) {
        List<Expression> arg = functionCall.getArguments();
        for (Expression expression : arg) {
            if (expression instanceof VariableAccess) {
                VariableAccess bias = (VariableAccess) expression;
                visitVariableAccess(bias);
            }
        }
    }

    private void visitVariableAccess(VariableAccess access) {
        if (varDeclOrder.containsKey(access.getName()) ) {
            if (varDeclOrder.get(access.getName()).getValue() == null) {
                reportAccessBeforeInitialization(access);
            }
        }
        else {
            reportAccessBeforeInitialization(access);
        }
    }

}
