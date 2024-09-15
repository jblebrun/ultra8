package com.emerjbl.ultra8.octo

data class Token(val text: String, val line: Int, val col: Int)

// Tokens are whitespace separated.
// Comments starting with # consume rest of line
private val tokenRegex = Regex("\\s*(#.*|\\S+)")

/** Generate a sequence of tokens for the provided sequence of program lines. */
fun tokenSequence(lines: Sequence<String>): Sequence<Token> =
    lines.flatMapIndexed() { lineIndex, line ->
        // Tokens are whitespace separated
        tokenRegex.findAll(line).mapNotNull { match ->
            match.groups[1]?.let {
                Token(
                    text = it.value,
                    line = lineIndex,
                    col = it.range.first
                )
            }
        }
    }
