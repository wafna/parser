package wafna.parser.lr1

import kotlin.test.Test

private operator fun SyntaxElementType.invoke(text: String? = null): SyntaxElement =
    SyntaxElement(this, text)

class TestLR1 {
    @Test
    fun `test LR(1)`() {
        val parser = generateParser(grammar)
        parser.states.forEach { print("--- "); print(it.show) }
    }

    private companion object {
        // Augmenting.
        object Start : NonTerminal("@")
        object End : Terminal("$")
        // Non-terminals.
        object Expr : NonTerminal("E")
        object TSum : NonTerminal("T")
        object TProd : NonTerminal("P")
        // Terminals.
        object Id : Terminal("id")
        object LParen : Terminal("(")
        object RParen : Terminal(")")
        object Plus : Terminal("+")
        object Times : Terminal("*")

        val rparen = RParen("(")
        val lparen = LParen("(")
        val plus = Plus("+")
        val x = Id("x")
        val y = Id("y")
        val z = Id("z")

        val grammar = listOf(
            Start(Expr, End),
            Expr(Expr, Plus, TSum),
            Expr(TSum),
            TSum(TSum, Times, TProd),
            TSum(TProd),
            TProd(Id),
            TProd(LParen, Expr, RParen)
        ).apply {
            println("--- Grammar")
            forEach { println(it.toString()) }
        }
    }
}

