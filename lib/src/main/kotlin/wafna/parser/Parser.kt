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
    val eof = TerminalToken(end, "<$>")
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
 * For visibility into the parser.
 */
interface StateListener {
    fun begin()
    fun shift(stack: List<Int>, state: ParseState, input: Token, shift: Int)
    fun reduce(stack: List<Int>, state: ParseState, input: Token, count: Int, tokenType: TokenType)
    fun accept(stack: List<Int>, state: ParseState)
}

object StateListenerNOOP : StateListener {
    override fun begin() {}
    override fun shift(stack: List<Int>, state: ParseState, input: Token, shift: Int) {}
    override fun reduce(stack: List<Int>, state: ParseState, input: Token, count: Int, tokenType: TokenType) {}
    override fun accept(stack: List<Int>, state: ParseState) {}
}
/**
 * Consumes the actions of the parser.
 */
abstract class ActionListener {
    abstract fun shift(token: Token)
    abstract fun reduce(token: NonTerminal, count: Int)
    abstract fun accept()
    // After a reduction the new node is pushed back to the input.
    // This remembers to ignore it when it gets shifted back.
    // Note: there are never two reductions in succession;
    // the new symbol precipitates a shift to a new reducing state.
    private var reduced = false
    internal fun shifted(token: Token) {
        if (reduced) reduced = false
        else shift(token)
    }

    internal fun reduced(token: NonTerminal, count: Int) {
        require(!reduced)
        reduced = true
        reduce(token, count)
    }
}

class RunConfig {
    var stateListener: StateListener = StateListenerNOOP
}

fun runParser(
    parser: Parser,
    builder: ActionListener,
    input: Iterator<TerminalToken>,
    configure: RunConfig.() -> Unit = {}
) {
    val config = RunConfig().apply { configure() }
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
                config.stateListener.shift(stack.toList(), state, pop, shift)
                stack.push(shift)
                builder.shifted(pop)
            }
        }
    }

    fun reduce(reductions: Map<Terminal, Reduction>, state: ParseState) {
        val peek = input.peek()
        when (val reduction = reductions[peek.type]) {
            null -> error("No reduction on $peek in ${state.show}")
            else -> {
                config.stateListener.reduce(stack.toList(), state, peek, reduction.count, reduction.to)
                repeat(reduction.count) { stack.pop() }
                input.push(NonTerminalToken(reduction.to))
                builder.reduced(reduction.to, reduction.count)
            }
        }
    }
    // Run the machine.
    config.stateListener.begin()
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
                        && !(parser.conflictMode == ConflictMode.Shift && shifts.contains(peek.type))
                    ) reduce(reductions, state)
                    else shift(shifts, state)
                }
            }

            is Accept -> {
                config.stateListener.accept(stack.toList(), state)
                accepted = true
                builder.accept()
            }
        }
    }
}

val Config.show: String
    get() = buildString {
        append(production.lhs.toString())
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

private fun Map<Terminal, Reduction>.showReductions(): String =
    toList().joinToString(", ") { "${it.first} → ${it.second.to}(${it.second.count})" }

private fun Map<TokenType, Int>.showShifts(): String =
    toList().joinToString(", ") { "${it.first} → ${it.second}" }

val ParseState.Dbg.show: String
    get() = buildString {
        appendLine("STATE $id")
        basis.forEach { appendLine("> ${it.show}") }
        extension.forEach { appendLine("  ${it.show}") }
        when (val a = action) {
            is Accept -> appendLine("  ACCEPT")
            is Reduce -> appendLine("  REDUCE: ${a.reductions.showReductions()}")
            is Shift -> appendLine("  SHIFT: ${a.shifts.showShifts()}")
            is Resolve -> {
                val conflicts = if (a.conflicts.isEmpty()) "" else a.conflicts.joinToString(" ")
                appendLine("  CONFLICTS: $conflicts\n      SHIFT: ${a.shifts.showShifts()}\n     REDUCE: ${a.reductions.showReductions()}")
            }
        }
    }
val ParseState.Opt.show: String
    get() = buildString {
        appendLine("STATE $id")
        when (val a = action) {
            is Accept -> appendLine("ACCEPT")
            is Reduce -> appendLine("REDUCE: ${a.reductions.showReductions()}")
            is Shift -> appendLine("SHIFT: ${a.shifts.showShifts()}")
            is Resolve -> {
                val conflicts = if (a.conflicts.isEmpty()) "" else a.conflicts.joinToString(" ")
                appendLine("CONFLICTS: $conflicts\n    SHIFT: ${a.shifts.showShifts()}\n   REDUCE: ${a.reductions.showReductions()}")
            }
        }
    }

