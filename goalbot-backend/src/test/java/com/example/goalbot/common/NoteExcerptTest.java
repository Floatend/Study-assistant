package com.example.goalbot.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteExcerptTest {
    @Test void cleansFrontmatterMathCalloutAndMarkdown() {
        String source = "---\nsecret: hidden-metadata\n---\n# 文档标题\n\n> [!note]+ 推导提示\n> 使用 **牛顿定律** 与 [参考文献](https://example.com)。\n\n$$\\varepsilon_0=8.85 \\times 10^{-12}$$\n\n结果 $E=mc^2$ 可以验证。\n";
        assertThat(NoteExcerpt.extract(source, "")).contains("推导提示", "牛顿定律", "参考文献", "可以验证")
                .doesNotContain("hidden-metadata", "文档标题", "[!note]", "**", "https:", "$$", "varepsilon", "mc^2");
    }

    @Test void returnsLiteralCleanSnippetAroundLateBodyMatch() {
        String source = "普通正文。".repeat(100) + "\n\n关键字位于末尾 **Token_42**，不应只截取开头。";
        String result = NoteExcerpt.extract(source, "token_42");
        assertThat(result).startsWith("…").contains("Token_42").doesNotContain("**");
        assertThat(result.length()).isLessThanOrEqualTo(184);
    }

    @Test void supportsTableTextAndCodeSearchWithoutMarkdownMarkers() {
        String source = "| 名称 | 描述 |\n| --- | --- |\n| 光速 | 常量 |\n\n```java\nint searchToken = 1;\n```\n";
        assertThat(NoteExcerpt.extract(source, "光速")).contains("光速", "常量").doesNotContain("|", "---", "```");
        assertThat(NoteExcerpt.extract(source, "searchToken")).contains("searchToken").doesNotContain("```");
    }

    @Test void ignoresRawHtmlAndEmptyMathOnlyDocuments() {
        assertThat(NoteExcerpt.extract("<script>alert('x')</script>\n\n正文", "")).isEqualTo("正文");
        assertThat(NoteExcerpt.extract("$$a=b$$", "")).isEmpty();
        assertThat(NoteExcerpt.extract(null, "")).isEmpty();
    }
}
