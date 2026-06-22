package com.example.goalbot.integration.content;

import com.example.goalbot.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ContentSourceClient {

    private static final int MAX_CONTENT_LENGTH = 12000;

    private final RestTemplate restTemplate;

    public ContentArticle fetchLatest(String sourceName, String sourceUrl) {
        if (!StringUtils.hasText(sourceUrl)) {
            throw BusinessException.badRequest("AI briefing source URL is empty");
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(sourceUrl.trim(), String.class);
            String body = response.getBody();
            if (!StringUtils.hasText(body)) {
                throw BusinessException.external("Content source response is empty");
            }
            String normalizedSourceName = StringUtils.hasText(sourceName) ? sourceName.trim() : "AI 资讯源";
            if (looksLikeXml(body)) {
                return parseFeed(normalizedSourceName, sourceUrl.trim(), body);
            }
            return parseHtml(normalizedSourceName, sourceUrl.trim(), body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw BusinessException.external("Content source request failed: " + ex.getMessage());
        } catch (Exception ex) {
            throw BusinessException.external("Content source parse failed: " + ex.getMessage());
        }
    }

    private boolean looksLikeXml(String body) {
        String trimmed = body.stripLeading();
        return trimmed.startsWith("<?xml")
                || trimmed.startsWith("<rss")
                || trimmed.startsWith("<feed")
                || trimmed.startsWith("<rdf");
    }

    private ContentArticle parseFeed(String sourceName, String sourceUrl, String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
        Element item = firstElement(document, "item");
        if (item != null) {
            return new ContentArticle(
                    sourceName,
                    cleanText(textOf(item, "title")),
                    firstText(textOf(item, "link"), sourceUrl),
                    limitContent(firstText(textOf(item, "content:encoded"), textOf(item, "description"))),
                    firstText(textOf(item, "pubDate"), textOf(item, "dc:date"))
            );
        }

        Element entry = firstElement(document, "entry");
        if (entry != null) {
            return new ContentArticle(
                    sourceName,
                    cleanText(textOf(entry, "title")),
                    firstText(atomLink(entry), sourceUrl),
                    limitContent(firstText(textOf(entry, "content"), textOf(entry, "summary"))),
                    firstText(textOf(entry, "published"), textOf(entry, "updated"))
            );
        }
        throw BusinessException.external("No item or entry found in content feed");
    }

    private ContentArticle parseHtml(String sourceName, String sourceUrl, String body) {
        String title = matchFirst(body, "(?is)<title[^>]*>(.*?)</title>");
        String withoutScripts = body
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        String text = stripTags(withoutScripts);
        return new ContentArticle(
                sourceName,
                cleanText(firstText(title, sourceName + " 今日文章")),
                sourceUrl,
                limitContent(text),
                null
        );
    }

    private Element firstElement(Document document, String tagName) {
        Node node = document.getElementsByTagName(tagName).item(0);
        return node instanceof Element element ? element : null;
    }

    private String textOf(Element parent, String tagName) {
        Node node = parent.getElementsByTagName(tagName).item(0);
        return node == null ? null : node.getTextContent();
    }

    private String atomLink(Element entry) {
        Node node = entry.getElementsByTagName("link").item(0);
        if (node instanceof Element element) {
            return firstText(element.getAttribute("href"), element.getTextContent());
        }
        return null;
    }

    private String matchFirst(String value, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String stripTags(String value) {
        return cleanText(value
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", " "));
    }

    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String limitContent(String content) {
        String cleaned = cleanText(content);
        if (cleaned.length() <= MAX_CONTENT_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_CONTENT_LENGTH) + "\n\n[内容过长，已截断]";
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
