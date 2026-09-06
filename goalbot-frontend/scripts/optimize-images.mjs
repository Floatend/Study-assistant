import sharp from 'sharp'
import { mkdir, stat } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

const assets = new URL('../src/assets/', import.meta.url)
const output = new URL('optimized/', assets)
await mkdir(output, { recursive: true })

// Originals remain untouched. The portrait source removes only the horizontal
// area already outside object-fit: cover on narrow, portrait phone screens.
const variants = [
  { source: 'linge-sakura-hero.png', name: 'sakura-wide-1280.webp', width: 1280 },
  { source: 'linge-sakura-hero.png', name: 'sakura-wide-1774.webp', width: 1774 },
  { source: 'linge-sakura-hero.png', name: 'sakura-portrait-640.webp', width: 640, square: true },
  { source: 'linge-sakura-hero.png', name: 'sakura-portrait-887.webp', width: 887, square: true },
  { source: 'linge-workspace-hero.png', name: 'workspace-960.webp', width: 960 },
  { source: 'linge-workspace-hero.png', name: 'workspace-1672.webp', width: 1672 }
]

for (const variant of variants) {
  const input = fileURLToPath(new URL(variant.source, assets))
  const target = fileURLToPath(new URL(variant.name, output))
  let pipeline = sharp(input)
  if (variant.square) {
    const { width, height } = await pipeline.metadata()
    pipeline = pipeline.extract({ left: Math.floor((width - height) / 2), top: 0, width: height, height })
  }
  await pipeline.resize({ width: variant.width, withoutEnlargement: true }).webp({ quality: 84, effort: 6 }).toFile(target)
  console.log(`${variant.name}: ${(await stat(target)).size} bytes`)
}
