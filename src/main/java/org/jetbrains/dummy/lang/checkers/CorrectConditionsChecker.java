package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partially duplicated by @link NoEffectFunChecker
 * @see NoEffectFuncCallChecker
 */
public class CorrectConditionsChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private Map<Pair<String, Integer>,FunctionDeclaration> functionsSet = new HashMap<>();

    public CorrectConditionsChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private void reportIllegalCondition(@NotNull Expression expression) {
        StringBuilder b = new StringBuilder("Condition '");
        if (expression instanceof FunctionCall)
            b.append(((FunctionCall) expression).getFunction());
        else b.append(expression.toString());
        b.append("' is not suited for a conditional expression");

        reporter.report(expression, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();
        innerFunc.forEach(i -> functionsSet.put(
                new Pair<>(i.getName(),i.getParameters().size()),
                                                                    i));

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            Block funcBlock = functionDeclaration.getBody();
            visitCodeBlock(funcBlock);
        }
    }

    private void visitCodeBlock(Block codeBlock) {
        List<Statement> statements = codeBlock.getStatements();
        for (Statement statement : statements) {
            if (statement instanceof  IfStatement) {
                visitIfStatement((IfStatement) statement);
            }
        }
    }

    private void visitIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() == null) {
            reportIllegalCondition(ifStatement.getCondition());
        }
        else {
            visitCondExpression(ifStatement.getCondition());
            if (ifStatement.getThenBlock() != null) {
                visitCodeBlock(ifStatement.getThenBlock());
            }
            if (ifStatement.getElseBlock() != null) {
                visitCodeBlock(ifStatement.getElseBlock());
            }
        }
    }


    private void visitCondExpression(Expression expression) {
        if (expression instanceof IntConst || expression instanceof BooleanConst ||
                expression instanceof VariableAccess) {
            return;
        }
        else if (expression instanceof FunctionCall) {
            String name = ((FunctionCall) expression).getFunction();
            int argsSize = ((FunctionCall) expression).getArguments().size();
            Pair<String, Integer> possibleFunc = new Pair<>(name, argsSize);
            boolean seenReturn = false;
            if (functionsSet.containsKey(possibleFunc)) {
                for (Statement statement : functionsSet.get(possibleFunc).getBody().getStatements()) {
                    if (statement instanceof ReturnStatement) {seenReturn = true;}
                }
                if (!seenReturn) {
                    reportIllegalCondition(expression);
                }
            }
            else {
                reportIllegalCondition(expression);
            }
        }
    }



}
