package wafna.parser.sexpr

import wafna.parser.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

val String.atom: SExpr.SAtom
    get() = SExpr.SAtom(toByteArray())

class TestSExpr {
    @Test
    fun test() {
//        testInput(listOf(lparen, rparen), SExpr.SList(emptyList()))
//        testInput(listOf(lparen, "a".token, rparen), SExpr.SList(listOf("a".atom)))
//        testInput(listOf(lparen, "a".token, "b".token, rparen), SExpr.SList(listOf("a".atom)))
//        testInput(listOf(lparen, "a".token, "b".token, "c".token, rparen), SExpr.SList(listOf("a".atom)))
//        testInput(listOf(lparen, "a".token, lparen, "b".token, "c".token, rparen, rparen), SExpr.SList(listOf("a".atom)))
        testInput(listOf(lparen, "a".token, lparen, "b".token, "c".token, rparen, "d".token, rparen), SExpr.SList(listOf("a".atom)))
    }

    companion object {
        val String.token: TerminalToken get() = TerminalToken(Atom, this)

        // Tokens
        object Start : NonTerminal("@")
        object End : Terminal("<$>")
        // Terminals
        object LParen : Terminal("[")
        object RParen : Terminal("]")
        object Atom : Terminal("a")
        // Non-terminals
        object Tree : NonTerminal("T")
        object Node : NonTerminal("N")
        object NodeList : NonTerminal("L")
        // Terminal tokens.
        val lparen = LParen.token("[")
        val rparen = RParen.token("]")

        val grammar = listOf(
            Start.produces(Tree, End),
            Tree.produces(LParen, NodeList, RParen),
            NodeList.produces(Node, NodeList),
            Node.produces(Atom),
            Node.produces(Tree),
            Node.produces(),
        ).apply {
            println("--- Grammar")
            forEach { println(it) }
        }
        val parser = generateParser(grammar) {
            configMode = ConfigMode.Dbg
            conflictMode = ConflictMode.Shift
        }.apply {
            println("--- Parser [${states.size}]")
            states.forEach { print("\uD80C\uDFF8 "); print(it.show) }
        }

        class SExprBuilder : ActionListener() {
            val lists = Stack<Int>()
            val items = Stack<SExpr>()
            override fun shift(token: Token) {
                when (token.type) {
                    Atom -> items.push(SExpr.SAtom(token.toString().toByteArray()))
                    LParen -> lists.push(items.size)
                    RParen -> {
                        val n = items.size - lists.pop()
                        val xs = List(n) { items.pop() }.reversed()
                        items.push(SExpr.SList(xs))
                    }

                    else -> {}
                }
            }

            override fun reduce(token: NonTerminal, count: Int) {}

            override fun accept() {}
        }

        fun testInput(input: List<TerminalToken>, expected: SExpr) {
            println("------------------------")
            println("- INPUT: ${input.joinToString(" ")}")
            val input = input.iterator()
            val builder = SExprBuilder()
            runParser(parser, builder, input) {
                stateListener = DebugStateListener
            }
            val actual = builder.items.pop().also {
                require(builder.items.isEmpty()) {
                    "WAT"
                }
            }
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show())
//            assertEquals(expected, actual)
        }
    }
}