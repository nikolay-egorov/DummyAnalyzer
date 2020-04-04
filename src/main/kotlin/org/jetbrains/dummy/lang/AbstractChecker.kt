package org.jetbrains.dummy.lang

import org.jetbrains.dummy.lang.tree.DummyLangVisitor
import org.jetbrains.dummy.lang.tree.Element
import org.jetbrains.dummy.lang.tree.File

abstract class AbstractChecker  {
    abstract fun inspect(file: File)

//    abstract  fun <R, D> visitElement(visitor: DummyLangVisitor<R, D>, data: D): R
}