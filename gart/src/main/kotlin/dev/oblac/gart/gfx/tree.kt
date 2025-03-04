package dev.oblac.gart.gfx

class TreeNode<T>(val value: T, val depth: Int) {
    private val nodes = mutableListOf<TreeNode<T>>()
    fun nodes(): List<TreeNode<T>> = nodes

    fun add(value: T): TreeNode<T> {
        val child = TreeNode(value, depth + 1)
        nodes.add(child)
        return child
    }

    companion object {
        fun <T> root(value: T): TreeNode<T> {
            return TreeNode(value, 0)
        }
    }


    fun allPaths(): List<List<T>> {
        return resolveAllPaths(this)
    }

    private fun <T> resolveAllPaths(node: TreeNode<T>, path: List<T> = listOf()): List<List<T>> {
        val newPath = path + node.value

        return if (node.nodes().isEmpty()) {
            listOf(newPath) // Leaf node, return the path
        } else {
            node.nodes().flatMap { resolveAllPaths(it, newPath) }
        }
    }

}
