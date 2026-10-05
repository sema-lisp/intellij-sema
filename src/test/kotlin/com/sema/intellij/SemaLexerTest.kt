package com.sema.intellij

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.*
import org.junit.Test

class SemaLexerTest {
    private fun tokensOf(source: String): List<Pair<IElementType?, String>> {
        val lexer = SemaLexer()
        lexer.start(source)
        val result = mutableListOf<Pair<IElementType?, String>>()
        while (lexer.tokenType != null) {
            result.add(lexer.tokenType to source.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    @Test
    fun emptyFile() {
        assertTrue(tokensOf("").isEmpty())
    }

    @Test
    fun whitespace() {
        val tokens = tokensOf("   \t\n  ")
        assertTrue(tokens.all { it.first == TokenType.WHITE_SPACE })
        assertEquals(1, tokens.size)
    }

    @Test
    fun delimiters() {
        val types = tokensOf("()[]{}").map { it.first }
        assertEquals(listOf(SemaTokenTypes.LPAREN, SemaTokenTypes.RPAREN,
            SemaTokenTypes.LBRACKET, SemaTokenTypes.RBRACKET,
            SemaTokenTypes.LBRACE, SemaTokenTypes.RBRACE), types)
    }

    @Test
    fun nestedDelimiters() {
        val types = tokensOf("([{}])").map { it.first }
        assertEquals(listOf(SemaTokenTypes.LPAREN, SemaTokenTypes.LBRACKET,
            SemaTokenTypes.LBRACE, SemaTokenTypes.RBRACE,
            SemaTokenTypes.RBRACKET, SemaTokenTypes.RPAREN), types)
    }

    @Test
    fun quoteFamily() {
        val types = tokensOf("' ` , ,@ @").filter { it.first != TokenType.WHITE_SPACE }.map { it.first }
        assertEquals(
            listOf(
                SemaTokenTypes.QUOTE,
                SemaTokenTypes.QUASIQUOTE,
                SemaTokenTypes.UNQUOTE,
                SemaTokenTypes.SPLICE,
                SemaTokenTypes.DEREF,
            ),
            types,
        )
    }

    @Test
    fun commaBeforeSymbolIsUnquote() {
        val types = tokensOf(",x").filter { it.first != TokenType.WHITE_SPACE }.map { it.first }
        assertEquals(listOf(SemaTokenTypes.UNQUOTE, SemaTokenTypes.SYMBOL), types)
    }

    @Test
    fun stringWithEscapes() {
        val strings = tokensOf("\"hello \\\"world\\\"\"").filter { it.first == SemaTokenTypes.STRING }
        assertEquals(1, strings.size)
        assertEquals("\"hello \\\"world\\\"\"", strings[0].second)
    }

    @Test
    fun unterminatedString() {
        assertEquals(SemaTokenTypes.STRING, tokensOf("\"unclosed").last().first)
    }

    @Test
    fun fString() {
        val strings = tokensOf("f\"hello {name}\"").filter { it.first == SemaTokenTypes.STRING }
        assertEquals(1, strings.size)
        assertEquals("f\"hello {name}\"", strings[0].second)
    }

    @Test
    fun regexString() {
        val regexes = tokensOf("#\"[a-z]+\"").filter { it.first == SemaTokenTypes.REGEX }
        assertEquals(1, regexes.size)
        assertEquals("#\"[a-z]+\"", regexes[0].second)
    }

    @Test
    fun regexLiteralFormsAreSingleTokens() {
        val literals = listOf(
            "#\"^\\d+$\"",
            "#\"(\\d+)-(\\w+)\"",
            "#\"\\\\\"",
            "#\"\\\"[^\\\"]+\\\"\"",
        )
        for (literal in literals) {
            assertEquals(listOf(SemaTokenTypes.REGEX to literal), tokensOf(literal))
        }
    }

    @Test
    fun regexLiteralStopsAtItsClosingQuote() {
        val source = "#\"\\d+\" \"text\" symbol) ; comment"
        val tokens = tokensOf(source).filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(
            listOf(
                SemaTokenTypes.REGEX to "#\"\\d+\"",
                SemaTokenTypes.STRING to "\"text\"",
                SemaTokenTypes.SYMBOL to "symbol",
                SemaTokenTypes.RPAREN to ")",
                SemaTokenTypes.LINE_COMMENT to "; comment",
            ),
            tokens,
        )
    }

    @Test
    fun unterminatedRegexLiteralIsStable() {
        assertEquals(listOf(SemaTokenTypes.REGEX to "#\"unclosed"), tokensOf("#\"unclosed"))
    }

    @Test
    fun regexBuiltinsAreSingleBuiltinTokens() {
        val names = listOf(
            "regex/match?", "regex/match", "regex/find-all",
            "regex/replace", "regex/replace-all", "regex/split",
        )
        for (name in names) {
            assertEquals(listOf(SemaTokenTypes.BUILTIN to name), tokensOf(name))
        }
    }

    @Test
    fun similarRegexNamesRemainSymbols() {
        for (name in listOf("regex/matches", "regex/split!", "my-regex/match")) {
            assertEquals(listOf(SemaTokenTypes.SYMBOL to name), tokensOf(name))
        }
    }

    @Test
    fun integers() {
        val numbers = tokensOf("0 42 -7 1000000").filter { it.first == SemaTokenTypes.NUMBER }
        assertEquals(4, numbers.size)
        assertEquals(listOf("0", "42", "-7", "1000000"), numbers.map { it.second })
    }

    @Test
    fun floats() {
        val numbers = tokensOf("3.14 -0.5 1.0 2e3 -4.5e-2").filter { it.first == SemaTokenTypes.NUMBER }
        assertEquals(listOf("3.14", "-0.5", "1.0", "2e3", "-4.5e-2"), numbers.map { it.second })
    }

    @Test
    fun numericTower() {
        val source = "1/2 3+4i +i -2i #xFF #b101 #e1.5 #e#xFF #x#e1F"
        val numbers = tokensOf(source).filter { it.first == SemaTokenTypes.NUMBER }
        assertEquals(source.split(" "), numbers.map { it.second })
    }

    @Test
    fun invalidNumberIsNotPartiallyHighlighted() {
        assertEquals(TokenType.BAD_CHARACTER, tokensOf("0x1F").single().first)
        assertEquals(TokenType.BAD_CHARACTER, tokensOf("#ei").single().first)
    }

    @Test
    fun standaloneMinusIsSymbol() {
        val types = tokensOf("(- x y)").filter { it.first != TokenType.WHITE_SPACE }.map { it.first }
        assertTrue(SemaTokenTypes.SYMBOL in types)
        assertFalse(SemaTokenTypes.NUMBER in types)
    }

    @Test
    fun plainSymbol() {
        val tokens = tokensOf("my-function")
        val sym = tokens.singleOrNull { it.first != TokenType.WHITE_SPACE }
        assertNotNull(sym)
        assertEquals(SemaTokenTypes.SYMBOL, sym!!.first)
        assertEquals("my-function", sym.second)
    }

    @Test
    fun specialForms() {
        val tokens = tokensOf("if when let lambda defun define defmacro import module")
        val specials = tokens.filter { it.first == SemaTokenTypes.SPECIAL_FORM }
        val defs = tokens.filter { it.first == SemaTokenTypes.DEFINITION_KEYWORD }
        assertEquals(6, specials.size)
        assertEquals(3, defs.size)
    }

    @Test
    fun workflowAndPolicyForms() {
        val tokens = tokensOf(
            "defworkflow defpolicy approval checkpoint parallel parallel-settled " +
                "phase pipeline pipeline-settled policy/without " +
                "settled-partition settled/err? settled/ok? step " +
                "tool/policy-subjects workflow/approval workflow/check " +
                "workflow/checkpoint workflow/phase workflow/policy-without " +
                "workflow/run workflow/run-form workflow/step " +
                "workflow/tool-call workflow/tool-result"
        )
            .filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(SemaTokenTypes.DEFINITION_KEYWORD, tokens[0].first)
        assertEquals(SemaTokenTypes.DEFINITION_KEYWORD, tokens[1].first)
        assertEquals(SemaTokenTypes.SPECIAL_FORM, tokens.first { it.second == "policy/without" }.first)
        for (token in tokens.drop(2).filter { it.second != "policy/without" }) {
            assertEquals(SemaTokenTypes.BUILTIN, token.first)
        }
    }

    @Test
    fun documentedBuiltins() {
        val source = "bytes/length async/with-timeout path/canonicalize db/open workflow/mcp-handle"
        val tokens = tokensOf(source).filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(source.split(" "), tokens.map { it.second })
        assertTrue(tokens.all { it.first == SemaTokenTypes.BUILTIN })
    }

    @Test
    fun dotToken() {
        val dot = tokensOf("(foo . bar)").find { it.first == SemaTokenTypes.DOT }
        assertNotNull(dot)
        assertEquals(".", dot!!.second)
    }

    @Test
    fun dotInSymbol() {
        val symbols = tokensOf("foo.bar").filter { it.first == SemaTokenTypes.SYMBOL }
        assertEquals(1, symbols.size)
        assertEquals("foo.bar", symbols[0].second)
    }

    @Test
    fun keywordColon() {
        val keywords = tokensOf(":my-keyword :another").filter { it.first == SemaTokenTypes.KEYWORD }
        assertEquals(2, keywords.size)
        assertEquals(listOf(":my-keyword", ":another"), keywords.map { it.second })
    }

    @Test
    fun booleansAndNil() {
        val tokens = tokensOf("#t #f true false nil")
        val booleans = tokens.filter { it.first == SemaTokenTypes.BOOLEAN }
        val nils = tokens.filter { it.first == SemaTokenTypes.NIL }
        assertEquals(4, booleans.size)
        assertEquals(1, nils.size)
    }

    @Test
    fun characterLiterals() {
        for (src in listOf("#\\a", "#\\space", "#\\newline", "#\\tab")) {
            assertEquals(SemaTokenTypes.CHARACTER, tokensOf(src)[0].first)
        }
    }

    @Test
    fun hashDispatch() {
        val dispatches = tokensOf("#(1 2 3) #u8(1 2 3)").filter { it.first == SemaTokenTypes.HASH_DISPATCH }
        assertEquals(2, dispatches.size)
        assertEquals("#(", dispatches[0].second)
        assertEquals("#u8(", dispatches[1].second)
    }

    @Test
    fun nestedBlockComments() {
        val comments = tokensOf("#|outer #|inner|# text|#").filter { it.first == SemaTokenTypes.BLOCK_COMMENT }
        assertEquals(1, comments.size)
        assertTrue(comments[0].second.contains("inner"))
    }

    @Test
    fun unterminatedBlockComment() {
        assertEquals(SemaTokenTypes.BLOCK_COMMENT, tokensOf("#|outer #|inner|#").last().first)
    }

    @Test
    fun lineComment() {
        val comments = tokensOf("; comment\nafter").filter { it.first == SemaTokenTypes.LINE_COMMENT }
        assertEquals(1, comments.size)
        assertEquals("; comment", comments[0].second)
    }

    @Test
    fun derefOperator() {
        assertEquals(SemaTokenTypes.DEREF, tokensOf("@value")[0].first)
    }

    @Test
    fun veryLongSymbol() {
        val longName = "x".repeat(1000)
        val tokens = tokensOf(longName)
        assertEquals(SemaTokenTypes.SYMBOL, tokens[0].first)
        assertEquals(longName, tokens[0].second)
    }

    @Test
    fun manyStrings() {
        val source = (1..100).joinToString(" ") { "\"str$it\"" }
        val strings = tokensOf(source).filter { it.first == SemaTokenTypes.STRING }
        assertEquals(100, strings.size)
    }

    @Test
    fun symbolChars() {
        val source = "foo&bar foo%bar foo^bar foo~bar foo.bar"
        val symbols = tokensOf(source).filter { it.first == SemaTokenTypes.SYMBOL }
        assertEquals(source.split(" "), symbols.map { it.second })
    }

    @Test
    fun hashMayEndASymbol() {
        assertEquals("guard-err#", tokensOf("guard-err#").single().second)
        assertEquals(SemaTokenTypes.SYMBOL, tokensOf("guard-err#").single().first)
    }
}
