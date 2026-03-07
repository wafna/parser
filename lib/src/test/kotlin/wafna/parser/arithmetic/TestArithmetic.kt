package wafna.parser.arithmetic

import wafna.parser.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArithmetic {
    @Test
    fun testPlus() {
        testInput(listOf(x), AST.Id(x))
        testInput(listOf(x, plus, y), AST.Plus(AST.Id(x), AST.Id(y)))
        testInput(listOf(x, plus, y, plus, z), AST.Plus(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(lparen, x, plus, y, rparen, plus, z), AST.Plus(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(x, plus, lparen, y, plus, z, rparen), AST.Plus(AST.Id(x), AST.Plus(AST.Id(y), AST.Id(z))))
    }
    @Test
    fun testTimes() {
        testInput(listOf(x), AST.Id(x))
        testInput(listOf(x, times, y), AST.Times(AST.Id(x), AST.Id(y)))
        testInput(listOf(x, times, y, times, z), AST.Times(AST.Times(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(lparen, x, times, y, rparen, times, z), AST.Times(AST.Times(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(x, times, lparen, y, times, z, rparen), AST.Times(AST.Id(x), AST.Times(AST.Id(y), AST.Id(z))))
    }
    @Test
    fun testPlusTimes() {
//        testInput(listOf(x, plus, y, times, z), AST.Plus(AST.Id(x), AST.Times(AST.Id(y), AST.Id(z))))
//        testInput(listOf(lparen, x, plus, y, rparen, times, z), AST.Times(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)))
//        testInput(listOf(x, divide, y, times, z), AST.Times(AST.Divide(AST.Id(x), AST.Id(y)), AST.Id(z)))
//        testInput(listOf(x, minus, lparen, y, times, z, rparen, plus, w), AST.Plus(AST.Minus(AST.Id(x), AST.Times(AST.Id(y), AST.Id(z))), AST.Id(w)))
        testInput(listOf(lparen, x, plus, y, rparen, times, z, minus, w), AST.Minus(AST.Times(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)), AST.Id(w)))
    }

    companion object {
        val grammar = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Term),
            Expr.produces(Expr, Minus, Term),
            Expr.produces(Term),
            Term.produces(Term, Times, Prod),
            Term.produces(Term, Divide, Prod),
            Term.produces(Prod),
            Prod.produces(Id),
            Prod.produces(LParen, Expr, RParen)
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

        fun testInput(input: List<TerminalToken>, expected: AST) {
            println("------------------------")
            println("- INPUT: ${input.joinToString(" ")}")
            val input = input.iterator()
            val builder = ASTBuilder()
            runParser(parser, builder, input) {
                stateListener = DebugStateListener
            }
            val actual = builder.tree.pop().also {
                require(builder.tree.isEmpty())
            }
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show())
            assertEquals(expected, actual)
        }

        class ASTBuilder : ActionListener() {
            val tree = Stack<AST>()
            val ops = Stack<Token>()
            override fun shift(token: Token) {
                when (token.type) {
                    Id -> tree.push(AST.Id(token))
                    Plus -> ops.push(token)
                    Minus -> ops.push(token)
                    Times -> ops.push(token)
                    Divide -> ops.push(token)
                    else -> {}
                }
            }

            fun reduceOp(count: Int) {
                if (count == 3) {
                    val children = List(2) { tree.pop() }
                    val op = ops.pop()
                    val n = when (op.type) {
                        Plus -> AST.Plus(children[1], children[0])
                        Minus -> AST.Minus(children[1], children[0])
                        Times -> AST.Times(children[1], children[0])
                        Divide -> AST.Divide(children[1], children[0])
                        else -> error(op.type.toString())
                    }
                    tree.push(n)
                }
            }

            override fun reduce(token: NonTerminal, count: Int) {
                when (token) {
                    Expr -> reduceOp(count)
                    Term -> reduceOp(count)
                    else -> {}
                }
            }

            override fun accept() {
                require(tree.size == 1) {
                    "TOO MUCH TREE ${tree.size}\n${List(tree.size) { i -> "[$i]\n${tree.pop()}" }.joinToString("\n")}"
                }
            }
        }
    }
}