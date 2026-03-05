package wafna.parser

fun <T> Iterator<T>.toList(): List<T> =
    buildList {
        while (hasNext()) {
            add(next())
        }
    }

val Token.show: String
    get() = text ?: type.toString()

val PTNode.show: String
    get() = buildString {
        fun showNode(node: PTNode, indent: Int) {
            repeat(indent) { append("  ") }
            append(node.token.show)
            appendLine(" [${node.children.size}]")
            node.children.forEach { showNode(it, indent + 1) }
        }
        showNode(this@show, 0)
    }