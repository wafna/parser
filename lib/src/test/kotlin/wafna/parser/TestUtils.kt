package wafna.parser

fun <T> Iterator<T>.toList(): List<T> =
    buildList {
        while (hasNext()) {
            add(next())
        }
    }

fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

val NonTerminal.token: NonTerminalToken
    get() = NonTerminalToken(this)

val Token.show: String
    get() = text ?: type.toString()

val ParseNode.show: String
    get() = buildString {
        fun showNode(node: ParseNode, indent: Int) {
            repeat(indent) { append("  ") }
            append(node.token.show)
            appendLine(" [${node.children.size}]")
            node.children.forEach { showNode(it, indent + 1) }
        }
        showNode(this@show, 0)
    }