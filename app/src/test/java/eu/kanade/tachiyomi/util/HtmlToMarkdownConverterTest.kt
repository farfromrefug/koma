package eu.kanade.tachiyomi.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HtmlToMarkdownConverterTest {

    @Test
    fun `convert null returns null`() {
        assertNull(HtmlToMarkdownConverter.convert(null))
    }

    @Test
    fun `convert empty string returns empty string`() {
        assertEquals("", HtmlToMarkdownConverter.convert(""))
    }

    @Test
    fun `convert plain text returns plain text`() {
        val plainText = "This is plain text without HTML"
        assertEquals(plainText, HtmlToMarkdownConverter.convert(plainText))
    }

    @Test
    fun `convert paragraph tags`() {
        val html = "<p>First paragraph</p><p>Second paragraph</p>"
        val expected = "First paragraph\n\nSecond paragraph"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert bold tags`() {
        val html = "<p>This is <b>bold</b> text</p>"
        val expected = "This is **bold** text"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert italic tags`() {
        val html = "<p>This is <i>italic</i> text</p>"
        val expected = "This is *italic* text"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert strong and em tags`() {
        val html = "<p>This is <strong>strong</strong> and <em>emphasized</em> text</p>"
        val expected = "This is **strong** and *emphasized* text"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert links`() {
        val html = "<p>Visit <a href=\"https://example.com\">this link</a></p>"
        val expected = "Visit [this link](https://example.com)"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert br tags`() {
        val html = "<p>Line one<br>Line two<br>Line three</p>"
        val expected = "Line one\nLine two\nLine three"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert headings`() {
        val html = "<h1>Heading 1</h1><h2>Heading 2</h2><h3>Heading 3</h3>"
        val result = HtmlToMarkdownConverter.convert(html)?.trim()
        assert(result?.contains("# Heading 1") == true)
        assert(result?.contains("## Heading 2") == true)
        assert(result?.contains("### Heading 3") == true)
    }

    @Test
    fun `convert unordered list`() {
        val html = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>"
        val result = HtmlToMarkdownConverter.convert(html)?.trim()
        assert(result?.contains("- Item 1") == true)
        assert(result?.contains("- Item 2") == true)
        assert(result?.contains("- Item 3") == true)
    }

    @Test
    fun `convert ordered list`() {
        val html = "<ol><li>Item 1</li><li>Item 2</li><li>Item 3</li></ol>"
        val result = HtmlToMarkdownConverter.convert(html)?.trim()
        assert(result?.contains("1. Item 1") == true)
        assert(result?.contains("2. Item 2") == true)
        assert(result?.contains("3. Item 3") == true)
    }

    @Test
    fun `convert complex manga description`() {
        val html = """
            <p><strong>Genre:</strong> Action, Fantasy</p>
            <p>This is a manga about a <i>hero</i> who fights <b>evil</b>.</p>
            <br>
            <p>Features:</p>
            <ul>
            <li>Epic battles</li>
            <li>Character development</li>
            </ul>
        """.trimIndent()
        
        val result = HtmlToMarkdownConverter.convert(html)
        assert(result != null)
        assert(result!!.contains("**Genre:**"))
        assert(result.contains("*hero*"))
        assert(result.contains("**evil**"))
        assert(result.contains("- Epic battles"))
        assert(result.contains("- Character development"))
    }

    @Test
    fun `convert div tags`() {
        val html = "<div>First div</div><div>Second div</div>"
        val expected = "First div\n\nSecond div"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }

    @Test
    fun `convert code tags`() {
        val html = "<p>Use <code>function()</code> to call</p>"
        val expected = "Use `function()` to call"
        assertEquals(expected, HtmlToMarkdownConverter.convert(html)?.trim())
    }
}
