package org.jetbrains.dummy.lang

import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.File

abstract class AbstractDummyLanguageTest {
    fun doTest(path: String) {
//        System.setProperty("line.separator", "\n")
        val expectedFile = File(path.replace(".dummy", ".txt"))
        if (!expectedFile.exists()) {
            expectedFile.createNewFile()
        }
        val expected = expectedFile.readText()

        val outputStream = ByteArrayOutputStream()
        val analyzer = DummyLanguageAnalyzer(outputStream)
        analyzer.analyze(path)
        val actual = outputStream.toString()//.replace("\r\n", "\n")

        Assert.assertEquals(expected, actual)
    }
}