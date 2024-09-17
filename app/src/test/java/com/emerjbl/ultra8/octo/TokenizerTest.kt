package com.emerjbl.ultra8.octo

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly

class TokenizerTest {

    @Test
    fun tokenizer_returnsExpectedToken() {
        val source = listOf(
            ":hello 13     this  \t is a test",
            "yep",
            "    the end # with comment",
            "   \t",
            "one more",
            "#start comment",
            "# comment with # is just one comment"
        )
        expectThat(
            tokenSequence(source.asSequence()).asIterable()
        ).containsExactly(
            Token(":hello", 0, 0),
            Token("13", 0, 7),
            Token("this", 0, 14),
            Token("is", 0, 22),
            Token("a", 0, 25),
            Token("test", 0, 27),
            Token("yep", 1, 0),
            Token("the", 2, 4),
            Token("end", 2, 8),
            Token("# with comment", 2, 12),
            Token("one", 4, 0),
            Token("more", 4, 4),
            Token("#start comment", 5, 0),
            Token("# comment with # is just one comment", 6, 0),
        )
    }
}
