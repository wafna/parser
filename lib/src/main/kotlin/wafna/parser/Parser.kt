package wafna.parser

import java.util.*

abstract class Token {
    abstract val type: TokenType
    abstract val text: String?
}

class TerminalToken(override val type: TokenType, override val text: String) : Token() {
    override fun toString(): String = text
}

class NonTerminalToken(override val type: TokenType) : Token() {
    override val text = null
    override fun toString(): String = "$type"
}

data class PTNode(val token: Token, val children: List<PTNode> = emptyList())

fun Token.node(): PTNode = PTNode(this)

internal class InputQueue(end: TokenType, val input: Iterator<TerminalToken>) {
    val eof = TerminalToken(end, "<EOF>").node()
    private val pushback = Stack<PTNode>()
    fun peek(): PTNode =
        if (pushback.isNotEmpty()) pushback.peek()
        else if (!input.hasNext()) eof
        else input.next().node().also { pushback.push(it) }

    fun pop(): PTNode =
        if (pushback.isNotEmpty()) pushback.pop()
        else if (!input.hasNext()) eof
        else input.next().node()

    fun push(node: PTNode) {
        pushback.push(node)
    }
}

fun runParser(parser: Parser, input: Iterator<TerminalToken>): PTNode {
    val input = InputQueue(parser.end, input)
    // Initially, the parse stack has state 0 and the tree is empty.
    val stack = Stack<State>().apply {
        val state0 = parser.states.first()
        push(state0)
    }
    val tree = Stack<PTNode>()

    fun doShift(shifts: Map<TokenType, State>, state: State) {
        val pop = input.pop()
        when (val shift = shifts[pop.token.type]) {
            null ->
                error("No shift found for ${pop.token.type} at ${state.show}")

            else -> {
                tree.push(pop)
                stack.push(shift)
            }
        }
    }

    fun doReduce(reductions: Map<Terminal, Reduction>, state: State) {
        val peek = input.peek()
        when (val reduction = reductions[peek.token.type]) {
            null ->
                error("No reduction on $peek in ${state.show}")

            else -> {
                repeat(reduction.count) { stack.pop() }
                val ns = List(reduction.count) { tree.pop() }.reversed()
                input.push(PTNode(NonTerminalToken(reduction.to), ns))
            }
        }
    }

    var accepted = false
    tailrec fun next() {
        val state = stack.peek()
        when (val action = state.action!!) {
            is Shift -> doShift(action.shifts, state)
            is Reduce -> doReduce(action.reductions, state)
            is Resolve -> {
                val peek = input.peek()
                if (action.reductions.contains(peek.token.type))
                    doReduce(action.reductions, state)
                else
                    doShift(action.shifts, state)
            }

            is Accept -> {
                require(tree.size == 2)
                tree.pop().also {
                    require(it.token.type == parser.end) { "Internal error." }
                }
                accepted = true
            }
        }
        if (!accepted) next()
    }
    next()
    return tree.pop()
}

val Config.show: String
    get() = buildString {
        append(production.lhs.name)
        append(" → ")
        production.rhs.withIndex().forEach { (i, e) ->
            if (i == dot) append(" •")
            append(" $e")
        }
        if (production.rhs.size <= dot)
            append(" •")
        append("  ::  ${if (follows.isEmpty()) "∅" else follows.joinToString(" ")}")
    }
val State.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("> ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        fun Map<Terminal, Reduction>.show(): String =
            toList().joinToString(", ") { "${it.first} → (${it.second.to}, ${it.second.count})" }

        fun Map<TokenType, State>.show(): String =
            toList().joinToString(", ") { "${it.first} → ${it.second.id}" }
        when (val a = action) {
            null -> {} // appendLine("\t<no actions>")
            is Accept -> appendLine("\tACCEPT: ${a.count}")
            is Reduce -> appendLine("\tREDUCE: ${a.reductions.show()}")
            is Shift -> appendLine("\tSHIFT: ${a.shifts.show()}")
            is Resolve -> appendLine("\tRESOLVE:\n\tSHIFT: ${a.shifts.show()}\n\tREDUCE: ${a.reductions.show()}")
        }
    }

