package wafna.parser

import java.util.*

/**
 * Reified vocabulary element.
 */
abstract class Token {
    abstract val type: TokenType
}

class TerminalToken(override val type: Terminal, val text: String) : Token() {
    override fun toString(): String = text
}

class NonTerminalToken(override val type: NonTerminal) : Token() {
    override fun toString(): String = type.toString()
}

/**
 * Pushback queue.
 * Reductions push tokens back into the input stream.
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

/**
 * Consumes the actions of the parser.
 */
abstract class ParserListener {
    // After a reduction the new node is pushed back to the input.
    // This remembers to ignore it when it gets shifted back.
    // Note: there are never two reductions in succession;
    // the new symbol precipitates a shift to a new reducing state.
    private var reduced = false
    abstract fun shift(token: Token)
    abstract fun reduce(token: NonTerminal, count: Int)
    abstract fun accept()
    internal fun shifted(token: Token) {
        if (reduced) reduced = false
        else shift(token)
    }

    internal fun reduced(token: NonTerminal, count: Int) {
        require(!reduced) { "Successive reductions." }
        reduced = true
        reduce(token, count)
    }

    open fun shiftAction(states: List<Int>, input: TokenType, shift: Int) {}
    open fun reduceAction(states: List<Int>, input: TokenType, count: Int, tokenType: TokenType) {}
}

fun runParser(parser: Parser, listener: ParserListener, input: Iterator<TerminalToken>) {
    // State table.
    val states = parser.states.associateBy { it.id }.run {
        Array(size) { getValue(it) }
    }
    // Parse stack.
    val stack = Stack<Int>()
    // Prime the pump.
    stack.push(states[0].id)
    // Queue the input.
    val input = InputQueue(parser.end, input)

    // Operations.
    fun shift(shifts: Map<TokenType, Int>, state: ParseState) {
        val pop = input.pop()
        when (val shift = shifts[pop.type]) {
            null -> error("No shift found for ${pop.type} at ${state.show}")
            else -> {
                listener.shiftAction(stack.toList(), pop.type, shift)
                listener.shifted(pop)
                stack.push(shift)
            }
        }
    }

    fun reduce(reductions: Map<Terminal, Reduction>, state: ParseState) {
        val peek = input.peek()
        when (val reduction = reductions[peek.type]) {
            null -> error("No reduction on $peek in ${state.show}")
            else -> {
                listener.reduceAction(stack.toList(), peek.type, reduction.count, reduction.to)
                repeat(reduction.count) { stack.pop() }
                listener.reduced(reduction.to, reduction.count)
                input.push(NonTerminalToken(reduction.to))
            }
        }
    }

    // Run the machine.
    var accepted = false
    while (!accepted) {
        val state = states[stack.peek()]
        when (val action = state.action) {
            is Shift -> shift(action.shifts, state)
            is Reduce -> reduce(action.reductions, state)
            is Resolve -> {
                val peek = input.peek()
                with(action) {
                    if (reductions.contains(peek.type)
                        && !(parser.conflictMode == ConflictMode.Shift && shifts.keys.contains(peek.type))
                    ) reduce(reductions, state)
                    else shift(shifts, state)
                }
            }

            is Accept -> {
                println("ACCEPT")
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
    get() = when (this) {
        is ParseState.Dbg -> show
        is ParseState.Opt -> show
    }

val ParseState.Dbg.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("> ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        fun Map<Terminal, Reduction>.show(): String =
            toList().joinToString(", ") { "${it.first} → (${it.second.to}, ${it.second.count})" }

        fun Map<TokenType, Int>.show(): String =
            toList().joinToString(", ") { "${it.first} → ${it.second}" }
        when (val a = action) {
            is Accept -> appendLine("\tACCEPT")
            is Reduce -> appendLine("\tREDUCE: ${a.reductions.show()}")
            is Shift -> appendLine("\tSHIFT: ${a.shifts.show()}")
            is Resolve -> {
                val conflicts = if (a.conflicts.isEmpty()) "" else a.conflicts.joinToString(" ")
                appendLine("\tCONFLICTS: $conflicts\n\tSHIFT: ${a.shifts.show()}\n\tREDUCE: ${a.reductions.show()}")
            }
        }
    }

val ParseState.Opt.show: String
    get() = buildString {
        appendLine("STATE $id")
        fun Map<Terminal, Reduction>.show(): String =
            toList().joinToString(", ") { "${it.first} → (${it.second.to}, ${it.second.count})" }

        fun Map<TokenType, Int>.show(): String =
            toList().joinToString(", ") { "${it.first} → ${it.second}" }
        when (val a = action) {
            is Accept -> appendLine("\tACCEPT")
            is Reduce -> appendLine("\tREDUCE: ${a.reductions.show()}")
            is Shift -> appendLine("\tSHIFT: ${a.shifts.show()}")
            is Resolve -> {
                val conflicts = if (a.conflicts.isEmpty()) "" else a.conflicts.joinToString(" ")
                appendLine("\tCONFLICTS: $conflicts\n\t    SHIFT: ${a.shifts.show()}\n\t   REDUCE: ${a.reductions.show()}")
            }
        }
    }

