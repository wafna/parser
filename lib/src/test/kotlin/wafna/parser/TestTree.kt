package wafna.parser

import kotlin.test.Test

class TreeNode(val name: String) {
    val children = mutableListOf<TreeNode>()
    fun addChild(node: TreeNode) = children.add(node)
}

fun TreeNode.show() = buildString {
    fun showTree(node: TreeNode, prefix: String = "", isLast: Boolean = true, isRoot: Boolean = true) {
        // 1. Determine the line marker for the current node
        val marker = if (isRoot) "" else if (isLast) "└── " else "├── "

        // 2. Print the node name with the current prefix and marker
        appendLine("$prefix$marker${node.name}")

        // 3. Update the prefix for the next level
        // If we're the root, we don't add to the prefix.
        // If we're the last child, children below us don't need a vertical bar.
        val newPrefix = prefix + if (isRoot) "" else if (isLast) "    " else "│   "

        // 4. Recursively print children
        node.children.forEachIndexed { index, child ->
            val childIsLast = index == node.children.size - 1
            showTree(child, newPrefix, childIsLast, false)
        }
    }
    showTree(this@show)
}

class TestTree {
    @Test
    fun testTree() {
        val root = TreeNode("Root")
        val folderA = TreeNode("Folder A")
        folderA.addChild(TreeNode("File 1"))
        folderA.addChild(TreeNode("File 2"))

        root.addChild(folderA)
        root.addChild(TreeNode("Folder B"))

        print(root.show())
    }
}