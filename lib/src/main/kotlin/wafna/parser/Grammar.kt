package wafna.parser

sealed class TokenType(val name: String? = null) {
    override fun toString(): String = name ?: this::class.java.simpleName
    abstract val terminal: Boolean
}

open class Terminal(name: String? = null) : TokenType(name) {
    override val terminal: Boolean = true
}

open class NonTerminal(name: String? = null) : TokenType(name) {
    override val terminal: Boolean = false
}

data class Production(val lhs: NonTerminal, val rhs: List<TokenType>) {
    override fun toString(): String = "$lhs → ${rhs.joinToString(" ")}"
}

fun NonTerminal.produces(vararg rhs: TokenType): Production =
    Production(this, rhs.asList())

