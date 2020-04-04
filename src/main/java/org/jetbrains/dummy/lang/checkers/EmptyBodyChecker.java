package org.jetbrains.dummy.lang.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.*;

import java.util.List;


public class EmptyBodyChecker extends AbstractChecker {

    private DiagnosticReporter reporter;

    public EmptyBodyChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    public void reportEmptyBlock(Element block) {
        StringBuilder b = new StringBuilder("Unnecessary empty block found on following line");

        reporter.report(block, b.toString());
    }

    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();

        for (FunctionDeclaration functionDeclaration : innerFunc) {
                visitCodeBlock(functionDeclaration.getBody());
        }
    }

    private void visitCodeBlock(Block block) {
        if (block.getStatements().size() == 0) {
            reportEmptyBlock(block);
        }
        else {
            for (Statement statement : block.getStatements()) {
                if (statement instanceof IfStatement) {
                    visitIf((IfStatement) statement);
                }
            }
        }
    }


    private void visitIf(IfStatement ifStatement) {
        if (ifStatement.getCondition() == null) {
            reportEmptyBlock(ifStatement.getCondition());
        }
        if (ifStatement.getCondition() != null) {
            visitCodeBlock(ifStatement.getThenBlock());
        }
        if (ifStatement.getElseBlock() != null) {
            visitCodeBlock(ifStatement.getElseBlock());
        }
    }

}
