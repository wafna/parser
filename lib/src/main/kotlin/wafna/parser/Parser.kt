package wafna.parser

import java.util.*

/**
 * The content of a parse node.
 */
abstract class Token {
    abstract val type: TokenType
    abstract val text: String?
}

class TerminalToken(override val type: Terminal, override val text: String) : Token() {
    override fun toString(): String = text
}

class NonTerminalToken(override val type: TokenType) : Token() {
    override val text = null
    override fun toString(): String = "$type"
}

/**
 * Pushback queue.
 */
internal class InputQueue(end: Terminal, val input: Iterator<TerminalToken>) {
    val eof = TerminalToken(end, "<EOF>")
    private val pushback = Stack<Token>()
    fun peek(): Token =
        if (pushback.isNotEmpty()) pushback.peek()
        else if (!input.hasNext()) eof
        else input.next().also { pushback.push(it) }

    fun pop(): Token =
        if (pushback.isNotEmpty()) pushback.pop()
        else if (!input.hasNext()) eof
        else input.next()

    fun push(node: Token) {
        pushback.push(node)
    }
}

interface ParseListener {
    fun shift(token: Token)
    fun reduce(token: NonTerminal, count: Int)
    fun accept()
}

fun runParser(parser: Parser, listener: ParseListener, input: Iterator<TerminalToken>) {
    val input = InputQueue(parser.end, input)
    // Prime the pump.
    val stack = Stack<Int>().apply {
        val state0 = parser.parseStates.first()
        push(state0.id)
    }

    // O(1) state lookup.
    val states = parser.parseStates.associateBy { it.id }.run {
        Array(size) { getValue(it) }
    }

    fun shift(shifts: Map<TokenType, Int>, state: ParseState) {
        val pop = input.pop()
        when (val shift = shifts[pop.type]) {
            null -> error("No shift found for ${pop.type} at ${state.show}")
            else -> {
                listener.shift(pop)
                stack.push(shift)
            }
        }
    }

    fun reduce(reductions: Map<Terminal, Reduction>, state: ParseState) {
        val peek = input.peek()
        when (val reduction = reductions[peek.type]) {
            null -> error("No reduction on $peek in ${state.show}")
            else -> {
                repeat(reduction.count) { stack.pop() }
                listener.reduce(reduction.to, reduction.count)
                input.push(NonTerminalToken(reduction.to))
            }
        }
    }

    var accepted = false
    while (!accepted) {
        val state = states[stack.peek()]
        when (val action = state.action) {
            is Shift -> shift(action.shifts, state)
            is Reduce -> reduce(action.reductions, state)
            is Resolve -> {
                val peek = input.peek()
                if (action.reductions.contains(peek.type))
                    reduce(action.reductions, state)
                else
                    shift(action.shifts, state)
            }

            is Accept -> {
                accepted = true
                listener.accept()
            }
        }
    }
}

val Config.show: String
    get() = buildString {
        append(production.lhs.name)
        append(" →")
        production.rhs.withIndex().forEach { (i, e) ->
            if (i == dot) append(" •")
            append(" $e")
        }
        if (production.rhs.size <= dot)
            append(" •")
        append("  ::  ${if (follows.isEmpty()) "∅" else follows.joinToString(" ")}")
    }

val ParseState.show: String
    get() = buildString {
        appendLine("STATE $id")
        configs.forEach { appendLine(it.show) }
        fun Map<Terminal, Reduction>.show(): String =
            toList().joinToString(", ") { "${it.first} → (${it.second.to}, ${it.second.count})" }

        fun Map<TokenType, Int>.show(): String =
            toList().joinToString(", ") { "${it.first} → ${it.second}" }
        when (val a = action) {
            is Accept -> appendLine("\tACCEPT: ${a.count}")
            is Reduce -> appendLine("\tREDUCE: ${a.reductions.show()}")
            is Shift -> appendLine("\tSHIFT: ${a.shifts.show()}")
            is Resolve -> appendLine("\tRESOLVE:\n\tSHIFT: ${a.shifts.show()}\n\tREDUCE: ${a.reductions.show()}")
        }
    }

