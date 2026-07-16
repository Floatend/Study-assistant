# Editorial Patterns

## Page Patterns

### Public home

- Use one real hero image, full bleed or nearly full bleed, with a solid translucent contrast layer only when needed for text legibility.
- Place a small metadata row at the hero edge. Keep the core statement to one thought and two destinations.
- Follow with unframed bands. A three-direction section can use vertical dividers and lightly different background colors instead of separate floating cards.
- End with a deep-color invitation section and a typographic footer signature.

### Knowledge base

- Desktop: left library rail, central article column, right table-of-contents rail.
- Mobile: category/search rail first, a short scrollable article list, then a single article column. Hide the desktop outline rail because heading links remain available through browser search and document flow.
- Article headers should include category, update date, author/site name, word count, summary, and tags only when real data exists.
- Article navigation should use the same active filtered list. Do not invent related posts.

### Private authoring desk

- Restrict authoring routes and APIs server-side to the actual owner/admin role.
- Use a library rail for search, categories, and draft/public states.
- Use a large two-pane Markdown editor with source and preview modes. Include direct upload of `.md`, `.markdown`, and `.txt` where the backend supports it.
- Keep publication state explicit: draft, organized, public. Publishing should be a deliberate action and public endpoints must return only approved public notes.

## Style Tokens

Use values as starting points, not brand colors to copy:

```css
--paper: #f4f0e7;
--ink: #1f2721;
--muted: #68736a;
--moss: #1f4e42;
--brick: #a54837;
--line: #d6d1c7;
```

Use 1px dividers, radii at or below 8px, and restrained shadows. Prefer article-width text columns around 65-80 characters.

## Interaction Budget

- Allow one entrance animation per meaningful unit.
- Use 180-320ms for hover feedback and 550-850ms for section reveal.
- Keep animations transform/opacity based.
- Do not rely on animation to reveal required text or controls.
- Use IntersectionObserver for optional one-time content reveal; disconnect it on unmount.

## Do Not Copy

- Never reuse a reference site's exact logo, copy, illustrations, raw HTML/CSS/JS, proprietary imagery, or brand concept.
- Do not use Japanese typography merely as “decoration.” Only use it when actual Japanese content and an appropriate language system exist.
- Do not turn a functional dashboard into an art-directed landing page.
