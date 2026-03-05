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
 * Every shift and reduce produces a new parse node.
 */
data class ParseNode(val token: Token, val children: List<ParseNode> = emptyList())

/**
 * Pushback queue.
 */
internal class InputQueue(end: Terminal, val input: Iterator<TerminalToken>) {
    val eof = ParseNode(TerminalToken(end, "<EOF>"))
    private val pushback = Stack<ParseNode>()
    fun peek(): ParseNode =
        if (pushback.isNotEmpty()) pushback.peek()
        else if (!input.hasNext()) eof
        else ParseNode(input.next()).also { pushback.push(it) }

    fun pop(): ParseNode =
        if (pushback.isNotEmpty()) pushback.pop()
        else if (!input.hasNext()) eof
        else ParseNode(input.next())

    fun push(node: ParseNode) {
        pushback.push(node)
    }
}

fun runParser(parser: Parser, input: Iterator<TerminalToken>): ParseNode {
    val input = InputQueue(parser.end, input)
    // Initially, the parse stack has state 0 and the tree is empty.
    val stack = Stack<Int>().apply {
        val state0 = parser.parseStates.first()
        push(state0.id)
    }
    val tree = Stack<ParseNode>()

    // O(1) state lookup.
    val states = parser.parseStates.associateBy { it.id }.let { states ->
        Array(states.size) { states.getValue(it) }
    }

    fun doShift(shifts: Map<TokenType, Int>, parseState: ParseState) {
        val pop = input.pop()
        when (val shift = shifts[pop.token.type]) {
            null ->
                error("No shift found for ${pop.token.type} at ${parseState.show}")

            else -> {
                tree.push(pop)
                stack.push(shift)
            }
        }
    }

    fun doReduce(reductions: Map<Terminal, Reduction>, parseState: ParseState) {
        val peek = input.peek()
        when (val reduction = reductions[peek.token.type]) {
            null ->
                error("No reduction on $peek in ${parseState.show}")

            else -> {
                repeat(reduction.count) { stack.pop() }
                val children = List(reduction.count) { tree.pop() }.reversed()
                input.push(ParseNode(NonTerminalToken(reduction.to), children))
            }
        }
    }

    var accepted = false
    tailrec fun next() {
        val state = states[stack.peek()]
        when (val action = state.action) {
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
                accepted = true
                require(tree.size == 2)
                tree.pop().also {
                    require(it.token.type == parser.end) { "Internal error." }
                }
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

