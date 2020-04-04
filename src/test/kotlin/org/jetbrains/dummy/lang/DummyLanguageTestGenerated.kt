package org.jetbrains.dummy.lang

import org.junit.Test

class DummyLanguageTestGenerated : AbstractDummyLanguageTest() {
    @Test
    fun testBad() {
        doTest("testData\noEffectTests\bad.dummy")
    }
    
    @Test
    fun testGood() {
        doTest("testData\noEffectTests\good.dummy")
    }
}
