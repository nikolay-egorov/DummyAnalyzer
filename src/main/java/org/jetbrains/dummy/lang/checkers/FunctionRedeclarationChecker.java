package org.jetbrains.dummy.lang.checkers;

import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dummy.lang.AbstractChecker;
import org.jetbrains.dummy.lang.DiagnosticReporter;
import org.jetbrains.dummy.lang.tree.File;
import org.jetbrains.dummy.lang.tree.FunctionDeclaration;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class FunctionRedeclarationChecker extends AbstractChecker {

    private DiagnosticReporter reporter;
    private HashMap<Pair<String, Integer>, Integer> funcDecl = new HashMap<>();
    public FunctionRedeclarationChecker(DiagnosticReporter reporter) {
        this.reporter = reporter;
    }

    private void reportRedeclaredFunction(FunctionDeclaration functionDeclaration, Pair<String, Integer> seen) {
        StringBuilder b = new StringBuilder("Function '");
        b.append(functionDeclaration.getName());
        b.append("' was already declared on line ");
        b.append(funcDecl.get(seen));
        reporter.report(functionDeclaration, b.toString());
    }


    @Override
    public void inspect(@NotNull File file) {
        List<FunctionDeclaration> innerFunc = file.getFunctions();

        for (FunctionDeclaration functionDeclaration : innerFunc) {
            String name = functionDeclaration.getName();
            Integer paramsSize = functionDeclaration.getParameters().size();
            Pair<String, Integer> candFunc = new Pair<>(name, paramsSize);
            if (funcDecl.containsKey(candFunc)) {
                reportRedeclaredFunction(functionDeclaration, candFunc);
            }
            else {
                funcDecl.put(candFunc, functionDeclaration.getLine());
            }
        }
    }


}
