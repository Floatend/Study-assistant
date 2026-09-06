import { test, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from 'typescript'

const source = await readFile(new URL('../src/utils/noteReading.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ES2022, target: ts.ScriptTarget.ES2022 } }).outputText
const { highlightMatches, loadReadingPosition, saveReadingPosition, READING_STORAGE_KEY } = await import('data:text/javascript;base64,' + Buffer.from(compiled).toString('base64'))

let data
beforeEach(() => {
  data = new Map()
  globalThis.localStorage = { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value) }
})

test('highlights literal punctuation, Chinese and mixed case without producing HTML', () => {
  const text = 'C++ 高数 <img onerror=bad> c++'
  const parts = highlightMatches(text, 'C++')
  assert.equal(parts.filter(p => p.match).length, 2)
  assert.equal(parts.map(p => p.text).join(''), text)
  assert.equal(highlightMatches(text, '高数').find(p => p.match).text, '高数')
  assert.deepEqual(highlightMatches('100%_!', '%_!').filter(p => p.match).map(p => p.text), ['%_!'])
  assert.deepEqual(highlightMatches(text, ''), [{ text, match: false }])
})
test('restores per-article progress only for the current revision', () => {
  saveReadingPosition(12, 'v1', .42)
  saveReadingPosition(13, 'v1', .61)
  assert.equal(loadReadingPosition(12, 'v1').progress, .42)
  assert.equal(loadReadingPosition(13, 'v1').progress, .61)
  assert.equal(loadReadingPosition(12, 'v2'), null)
})
test('start and end positions remove resume records', () => {
  saveReadingPosition(12, 'v1', .4)
  saveReadingPosition(12, 'v1', 1)
  assert.equal(loadReadingPosition(12, 'v1'), null)
  saveReadingPosition(12, 'v1', .4)
  saveReadingPosition(12, 'v1', 0)
  assert.equal(loadReadingPosition(12, 'v1'), null)
})
test('corrupt, expired, invalid, blocked and full storage are harmless', () => {
  data.set(READING_STORAGE_KEY, 'not json')
  assert.equal(loadReadingPosition(12, 'v1'), null)
  data.set(READING_STORAGE_KEY, JSON.stringify([{ noteId:12, revision:'v1', progress:.5, savedAt:0 }, { noteId:13, revision:'v1', progress:4, savedAt:Date.now() }]))
  assert.equal(loadReadingPosition(12, 'v1'), null)
  assert.equal(loadReadingPosition(13, 'v1'), null)
  globalThis.localStorage = { getItem() { throw Error('blocked') }, setItem() { throw Error('full') } }
  assert.equal(loadReadingPosition(12, 'v1'), null)
  assert.doesNotThrow(() => saveReadingPosition(12, 'v1', .3))
})
test('stores bounded metadata only, not article contents', () => {
  for (let i = 1; i <= 60; i++) saveReadingPosition(i, 'revision', .5)
  const records = JSON.parse(data.get(READING_STORAGE_KEY))
  assert.equal(records.length, 50)
  assert.deepEqual(Object.keys(records[0]).sort(), ['noteId', 'progress', 'revision', 'savedAt'])
})
