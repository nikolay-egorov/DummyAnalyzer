package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.ClassMatcher.match;

public class VariableAssignmentChecker extends AbstractChecker {
    private DiagnosticReporter reporter;
    private Map<String, Expression> initVars = new HashMap<>();
    private Map<Pair<String, Integer>, Expression> funcReturns = new HashMap<>();
    private FunctionDeclaration currentFunc;

    public VariableAssignmentChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    public void reportBadAssignment(Expression assignment) {
        StringBuilder b = new StringBuilder("Right-side assignment '");
        if (assignment instanceof FunctionCall)
            b.append(((FunctionCall) assignment).getFunction());
        else if (assignment instanceof VariableAccess)
            b.append(((VariableAccess) assignment).getName());
        else b.append(assignment);
        b.append("' is invalid");

        reporter.report(assignment, b.toString());
    }


    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        for (FunctionDeclaration functionDeclaration : innerFunc) {
            Expression returnV = null;
            for (Statement statement : functionDeclaration.getBody().getStatements()) {
                if (statement instanceof ReturnStatement) {
                    //TODO: perhaps, chained call
                    returnV = ((ReturnStatement) statement).getResult();
                }
            }
            String name = functionDeclaration.getName();
            int argsS = functionDeclaration.getParameters().size();
            Pair<String, Integer> funcCandidate = new Pair<>(name, argsS);
            funcReturns.put(funcCandidate, returnV);
        }

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            currentFunc = functionDeclaration;
            visitCodeBlock(functionDeclaration.getBody());
            initVars.clear();
        }

    }

    private void visitCodeBlock(Block block) {
        for (Statement statement : block.getStatements()) {
            visitStatement(statement);
        }
    }

    private void visitStatement(Statement statement) {
        match().with(IfStatement.class, this::visitIfStatement)
                .with(VariableDeclaration.class, this::visitVarDecl)
                .with(Assignment.class, this::visitAssignment)
                .exec(statement);
    }

    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getThenBlock() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }

    private void visitVarDecl(VariableDeclaration variableDeclaration) {
        Expression init = variableDeclaration.getInitializer();
        if (init == null) {
            initVars.putIfAbsent(variableDeclaration.getName(), init);
            return;
        }

        if (init instanceof FunctionCall) {
            visitFuncCall((FunctionCall) init, variableDeclaration.getName());
        }
        else if (init instanceof VariableAccess) {
            List<String> currParams = currentFunc.getParameters();
            for (String currParam : currParams) {
                if (currParam.equals(((VariableAccess) init).getName())) {
                    initVars.putIfAbsent(variableDeclaration.getName(), init);
                    return;
                }
            }
            if (initVars.get(((VariableAccess) init).getName()) == null) {
                reportBadAssignment(init);
            }
            else
                initVars.putIfAbsent(variableDeclaration.getName(), init);
        }
        else  {
            initVars.putIfAbsent(variableDeclaration.getName(), init);
        }

    }



    private void visitAssignment(Assignment assignment) {
        String leftSide = assignment.getVariable();
        Expression rhs = assignment.getRhs();
        if (rhs != null) {
            if (rhs instanceof FunctionCall) {
                visitFuncCall((FunctionCall) rhs, leftSide);
                return;
            }
            // redundant a bit since we have VariableInitializationChecker
            if (rhs instanceof VariableAccess) {
                List<String> currParams = currentFunc.getParameters();
                for (String currParam : currParams) {
                    if (currParam.equals(((VariableAccess) rhs).getName())) {
                        initVars.putIfAbsent(leftSide, rhs);
                        return;
                    }
                }

                if (initVars.get(((VariableAccess) rhs).getName()) == null) {
                    reportBadAssignment(rhs);
                }
            }

//            else
//                initVars.putIfAbsent(leftSide, rhs);
        }
    }

    private void visitFuncCall(FunctionCall functionCall, String leftSideVar) {
        String name = functionCall.getFunction();
        int agsS = functionCall.getArguments().size();
        Pair<String, Integer> candFunc = new Pair<>(name, agsS);
        if (funcReturns.get(candFunc) == null) {
            reportBadAssignment(functionCall);
        }
        else {
            initVars.putIfAbsent(leftSideVar, funcReturns.get(candFunc) );
        }
    }


}
