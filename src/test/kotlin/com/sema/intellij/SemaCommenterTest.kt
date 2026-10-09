package com.sema.intellij

import org.junit.Assert.*
import org.junit.Test

class SemaCommenterTest {
    private val commenter = SemaCommenter()

    @Test
    fun lineCommentPrefix() = assertEquals(";", commenter.lineCommentPrefix)

    @Test
    fun blockCommentPrefix() = assertNull(commenter.blockCommentPrefix)

    @Test
    fun blockCommentSuffix() = assertNull(commenter.blockCommentSuffix)

    @Test
    fun noCommentedBlockComment() {
        assertNull(commenter.commentedBlockCommentPrefix)
        assertNull(commenter.commentedBlockCommentSuffix)
    }
}
