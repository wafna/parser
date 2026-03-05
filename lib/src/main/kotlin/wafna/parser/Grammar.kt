package wafna.parser

/**
 * Distinguishing token types is useful in keeping order.
 */
sealed class TokenType(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
    abstract val terminal: Boolean
}

/**
 * A piece of text, e.g. id, keyword, punctuation, etc.
 */
open class Terminal(name: String? = null) : TokenType(name) {
    override val terminal: Boolean = true
}

/**
 * Aggregates of terminals and other non-terminals.
 */
open class NonTerminal(name: String? = null) : TokenType(name) {
    override val terminal: Boolean = false
}

/**
 * Generative grammar rule.
 */
data class Production(val lhs: NonTerminal, val rhs: List<TokenType>) {
    override fun toString(): String = "$lhs → ${rhs.joinToString(" ")}"
}

/**
 * Notation.
 */
fun NonTerminal.produces(vararg rhs: TokenType): Production =
    Production(this, rhs.asList())

