---
name: monaka-editorial-web
description: Design or refine public personal websites, blogs, portfolios, public knowledge bases, and related editorial pages with calm negative-space storytelling, restrained motion, strong typography, and useful content navigation. Use when a user asks for a Monaka-inspired or editorial personal-site style without copying the original brand, assets, code, or prose; especially when public narrative surfaces must remain distinct from a private product workspace.
---

# Monaka Editorial Web

Create a calm, authored public experience. Borrow the design method of negative space and deliberate pacing, never the source site's visual assets, copy, Japanese concept, or implementation.

## Start With The Boundary

Classify every surface before designing it:

- **Public home:** identity, point of view, selected work, and clear next paths.
- **Public knowledge base:** find, choose, read, and continue reading.
- **Private workspace:** efficient repeated actions, dense information, predictable navigation.

Keep the public site expressive and the private workspace operational. Do not apply cinematic scroll locking, oversized decoration, or editorial navigation to a task-management screen.

## Build The Narrative

For a public home, use a sequence rather than a stack of generic cards:

```text
Identity hero with a real relevant image
  -> short statement of purpose
  -> 2-3 directions or bodies of work
  -> a visual pause / short belief
  -> two explicit destinations
  -> generous footer signature
```

Make the H1 the person, site, product, or literal offer. Keep the first viewport focused, but show a visible hint of the next section on both desktop and mobile.

For a knowledge base, use a reading layout:

```text
Site header
  -> archive title and one-sentence editorial policy
  -> search + category index + article list
  -> article header + rendered Markdown
  -> table of contents on desktop
  -> previous / next article
```

Put the long-form content on its own route or route query. Do not render article bodies on the public home by default.

## Use The Visual System

Use a small, deliberate palette:

- a warm paper base;
- a near-black ink for primary text;
- one deep structural color such as moss or ocean;
- one restrained contrasting accent such as brick red.

Use color to separate sections, not to decorate every element. Prefer borders, whitespace, type scale, and column alignment over card grids and shadows.

Choose a display face with character for site names and editorial headlines; use the project’s existing Chinese system stack for body text unless a licensed web font is explicitly available. Keep tracking at `0` or positive. Do not use a generic app logo tile when a typographic wordmark is more appropriate.

See [references/patterns.md](references/patterns.md) for page and interaction patterns.

## Motion Rules

Use motion to reveal hierarchy and convey pace:

- hero image: one slow scale or opacity entrance;
- sections: one small upward reveal once when entering view;
- links: small arrow translation on hover;
- long-page section changes: background or text-color shift only when it improves the narrative.

Avoid scroll hijacking, full-page lock-in, continuous particle effects, decorative orbs, and heavy WebGL unless the interaction itself is the product. Always implement a `prefers-reduced-motion` fallback that leaves every section fully visible and usable.

## Implement Carefully

1. Inspect the existing router, shared styles, data APIs, and mobile breakpoints before editing.
2. Preserve the existing design system inside private product routes.
3. Use real, inspectable images for the public hero; do not substitute an abstract SVG for a meaningful subject.
4. Build content navigation from real data: search, categories, stable article URLs, heading anchors, and adjacent-article links.
5. Sanitize rendered Markdown and generate unique heading IDs shared by the table of contents and rendered article.
6. Keep public publishing behind an explicit server-side approval flag. Never expose drafts or private user content merely because the frontend hides it.
7. Verify desktop and mobile text wrapping, no-overlap states, empty states, reduced motion, and public/private access boundaries.

## Delivery Checklist

- Confirm the public home does not call the full article-list API just to populate its first viewport.
- Confirm the knowledge base has search, category filtering, a readable empty state, a heading outline, and previous/next navigation.
- Confirm navigation and APIs do not expose the authoring interface to ordinary users.
- Confirm every user-facing route is reachable by direct URL and browser refresh.
- Run the frontend build and the backend tests for any API or schema change.
- If the project is deployed, provide migrations, targeted service rebuilds, and verification commands for the actual topology.
