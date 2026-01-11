package eu.kanade.tachiyomi.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Converts HTML content to Markdown format.
 */
object HtmlToMarkdownConverter {
    /**
     * Converts HTML string to Markdown string.
     * @param html The HTML string to convert
     * @return The converted Markdown string, or the original string if it doesn't contain HTML
     */
    fun convert(html: String?): String? {
        if (html.isNullOrBlank()) return html
        
        // Check if the string contains HTML tags
        if (!html.contains("<")) return html
        
        return try {
            val doc: Document = Jsoup.parse(html)
            // If the parsed document body is empty or only contains text, return original
            if (doc.body().childNodeSize() == 0 || 
                (doc.body().childNodeSize() == 1 && doc.body().childNode(0) is TextNode)) {
                html
            } else {
                convertElement(doc.body()).trim()
            }
        } catch (e: Exception) {
            // If parsing fails, return the original string
            html
        }
    }

    private fun convertElement(element: Element): String {
        val result = StringBuilder()
        
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> result.append(node.text())
                is Element -> result.append(convertTag(node))
            }
        }
        
        return result.toString()
    }

    private fun convertTag(element: Element): String {
        return when (element.tagName().lowercase()) {
            "br" -> "\n"
            "p" -> "${convertElement(element)}\n\n"
            "div" -> "${convertElement(element)}\n\n"
            "h1" -> "# ${convertElement(element)}\n\n"
            "h2" -> "## ${convertElement(element)}\n\n"
            "h3" -> "### ${convertElement(element)}\n\n"
            "h4" -> "#### ${convertElement(element)}\n\n"
            "h5" -> "##### ${convertElement(element)}\n\n"
            "h6" -> "###### ${convertElement(element)}\n\n"
            "strong", "b" -> "**${convertElement(element)}**"
            "em", "i" -> "*${convertElement(element)}*"
            "u" -> "_${convertElement(element)}_"
            "code" -> "`${convertElement(element)}`"
            "pre" -> "```\n${element.text()}\n```\n\n"
            "blockquote" -> "> ${convertElement(element).replace("\n", "\n> ")}\n\n"
            "a" -> {
                val href = element.attr("href")
                val text = convertElement(element)
                if (href.isNotEmpty()) {
                    "[$text]($href)"
                } else {
                    text
                }
            }
            "img" -> {
                val src = element.attr("src")
                val alt = element.attr("alt").ifEmpty { "image" }
                if (src.isNotEmpty()) {
                    "![$alt]($src)"
                } else {
                    ""
                }
            }
            "ul" -> {
                val items = element.children()
                    .filter { it.tagName() == "li" }
                    .joinToString("\n") { "- ${convertElement(it)}" }
                "$items\n\n"
            }
            "ol" -> {
                val items = element.children()
                    .filter { it.tagName() == "li" }
                    .mapIndexed { index, li -> "${index + 1}. ${convertElement(li)}" }
                    .joinToString("\n")
                "$items\n\n"
            }
            "li" -> convertElement(element)
            "hr" -> "---\n\n"
            "span", "font" -> convertElement(element)
            else -> convertElement(element)
        }
    }
}
