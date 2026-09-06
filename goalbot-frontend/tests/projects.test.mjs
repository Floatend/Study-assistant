import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from 'typescript'

function asModule(source) {
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ES2022, target: ts.ScriptTarget.ES2022 } }).outputText
  return 'data:text/javascript;base64,' + Buffer.from(compiled).toString('base64')
}
const timelineModule = asModule(await readFile(new URL('../src/data/timeline.ts', import.meta.url), 'utf8'))
const source = await readFile(new URL('../src/data/projects.ts', import.meta.url), 'utf8')
const { projects, projectIds, findProject, adjacentProjects } = await import(asModule(source.replace("'./timeline'", JSON.stringify(timelineModule))))
const { timelineItems, achievements } = await import(timelineModule)

test('three curated projects reuse the original resume timeline facts', () => {
  assert.equal(projects.length, 3)
  assert.equal(new Set(projectIds).size, 3)
  for (const project of projects) {
    const source = timelineItems.find(item => item.id === project.id)
    for (const key of ['title', 'period', 'role', 'description', 'details', 'tags', 'start', 'end', 'current']) {
      assert.deepEqual(project[key], source[key], project.id + ': ' + key)
    }
    assert.equal(source.link.to, '/projects/' + project.id)
    assert(project.background.length > 0 && project.flow.length > 1 && project.focus.length > 0)
  }
})
test('unknown, case-changed and non-string slugs never select another project', () => {
  for (const slug of [null, undefined, [], {}, '', 'Cloud-edge-capture', 'linge-site', '../cloud-edge-capture', 'toString']) {
    assert.equal(findProject(slug), undefined)
  }
  for (const project of projects) assert.equal(findProject(project.id), project)
})
test('previous and next navigation has stable, non-circular boundaries', () => {
  assert.equal(adjacentProjects(projectIds[0]).previous, undefined)
  assert.equal(adjacentProjects(projectIds[0]).next, projects[1])
  assert.equal(adjacentProjects(projectIds[1]).previous, projects[0])
  assert.equal(adjacentProjects(projectIds[1]).next, projects[2])
  assert.equal(adjacentProjects(projectIds[2]).next, undefined)
  assert.deepEqual(adjacentProjects('unknown'), { previous: undefined, next: undefined })
})
test('note topics fit the existing public search contract', () => {
  for (const project of projects) {
    assert(project.noteTopics.length > 0)
    assert.equal(new Set(project.noteTopics).size, project.noteTopics.length)
    for (const topic of project.noteTopics) assert(topic.trim().length > 0 && topic.length <= 100)
  }
})
test('previously verified award names and levels remain unchanged', () => {
  assert.equal(achievements.length, 8)
  assert.equal(achievements.find(item => item.id === 'huawei-ict').result, '省级三等奖')
  assert.equal(achievements.find(item => item.id === 'innovation').title, '中国国际大学生创新竞赛')
  assert.equal(achievements.find(item => item.id === 'innovation').result, '校级二等奖')
})
