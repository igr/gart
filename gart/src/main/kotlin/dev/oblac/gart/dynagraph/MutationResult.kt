package dev.oblac.gart.dynagraph

/**
 * Result of a mutating [DynaGraph] operation. Mutations can succeed, optionally
 * producing new vertex IDs, or fail silently.
 */
sealed class MutationResult {
    abstract val newVerts: IntArray

    data object Failure : MutationResult() {
        override val newVerts: IntArray get() = EMPTY_INTS
    }

    class Ok(override val newVerts: IntArray = EMPTY_INTS) : MutationResult() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Ok && newVerts.contentEquals(other.newVerts))

        override fun hashCode(): Int = newVerts.contentHashCode()
    }

    val isOk: Boolean get() = this is Ok

    val newVert: Int? get() = newVerts.firstOrNull()

    companion object {
        private val EMPTY_INTS = IntArray(0)
        internal fun ok(): MutationResult = Ok(EMPTY_INTS)
        internal fun ok(v: Int): MutationResult = Ok(intArrayOf(v))
        internal fun ok(a: Int, b: Int): MutationResult = Ok(intArrayOf(a, b))
    }
}
