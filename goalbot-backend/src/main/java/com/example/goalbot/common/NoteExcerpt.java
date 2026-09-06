package com.example.goalbot.common;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.List;
import java.util.Locale;

/** Plain text only: snippets are never HTML, and never contain raw math/frontmatter. */
public final class NoteExcerpt {
    private static final Parser PARSER = Parser.builder().extensions(List.of(TablesExtension.create())).build();
    private NoteExcerpt() { }

    public static String extract(String content, String keyword) {
        if (content == null || content.isBlank()) return "";
        String source = content.replace("\r\n", "\n")
                .replaceFirst("(?s)\\A\\uFEFF?(---|\\+\\+\\+)\\n.*?\\n\\1(?:\\n|$)", "")
                .replaceAll("(?s)\\$\\$.*?\\$\\$|\\\\\\[.*?\\\\\\]|\\\\\\(.*?\\\\\\)", " ")
                .replaceAll("\\$(?:\\\\.|[^$\\n])+\\$", " ")
                .replaceAll("(?m)^(\\s*(?:>\\s*)*)(?:\\[![A-Za-z][\\w-]*\\][+-]?|![A-Za-z][\\w-]*)[ \\t]*", "$1");
        StringBuilder text = new StringBuilder();
        boolean searching = keyword != null && !keyword.isBlank();
        PARSER.parse(source).accept(new AbstractVisitor() {
            @Override public void visit(Text node) { text.append(node.getLiteral()); }
            @Override public void visit(Code node) { text.append(node.getLiteral()); }
            @Override public void visit(SoftLineBreak node) { text.append(' '); }
            @Override public void visit(HardLineBreak node) { text.append(' '); }
            @Override public void visit(Paragraph node) { visitChildren(node); text.append(' '); }
            @Override public void visit(Heading node) { if (searching) { visitChildren(node); text.append(' '); } }
            @Override public void visit(FencedCodeBlock node) { if (searching) text.append(node.getLiteral()).append(' '); }
            @Override public void visit(IndentedCodeBlock node) { if (searching) text.append(node.getLiteral()).append(' '); }
            @Override public void visit(HtmlBlock node) { }
            @Override public void visit(HtmlInline node) { }
            @Override public void visit(Image node) { }
            @Override public void visit(CustomNode node) { visitChildren(node); text.append(' '); }
            @Override public void visit(CustomBlock node) { visitChildren(node); text.append(' '); }
        });
        String plain = text.toString().replaceAll("\\[\\[([^]|]+)\\|([^]]+)]]", "$2")
                .replaceAll("\\[\\[([^]]+)]]", "$1").replaceAll("\\s+", " ").trim();
        int match = searching ? plain.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT)) : -1;
        int start = match >= 0 ? Math.max(0, match - 48) : 0;
        int end = Math.min(plain.length(), start + 180);
        // Do not split UTF-16 surrogate pairs when an excerpt meets an emoji.
        if (start > 0 && Character.isLowSurrogate(plain.charAt(start))) start--;
        if (end < plain.length() && Character.isLowSurrogate(plain.charAt(end))) end++;
        return (start > 0 ? "…" : "") + plain.substring(start, end).trim() + (end < plain.length() ? "…" : "");
    }
}
