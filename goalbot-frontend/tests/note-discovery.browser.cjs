// Isolated public API fixtures; never writes to the live backend.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const base=process.env.QA_BASE_URL || 'http://127.0.0.1:5173', out=process.env.QA_OUTPUT_DIR || path.join(os.tmpdir(), 'linge-discovery-qa');
const body='# 基础知识\n\n> [!note] 阅读提示\n> 这段内容仅用于隔离浏览器测试。\n\n$$E=mc^2$$\n\n'+ '这是一段用于检验阅读位置和长文布局的测试文字。\n\n'.repeat(28)+'## 推导过程\n\n```java\nint value = 1;\n```\n\n'+'这里用于验证不同屏幕尺寸下的续读位置。\n\n'.repeat(35)+'## 结论\n\n测试完成。';
const records=Array.from({length:137},(_,index)=>{const id=137-index;return {id,userId:1,title:'测试笔记 '+id,category:id<=50?'课程/物理':'技术/Java',summary:'测试文章摘要',content:body+(id===1?'\n末尾独有词 body_only_token。':''),tags:'测试',wordCount:1800,updatedAt:'2026-09-06T12:00:00',createdAt:'2026-09-06T12:00:00',published:true,official:true,authorName:'测试作者',excerpt:id===1?'末尾独有词 body_only_token。':'这是一段干净的搜索结果摘要。'}});
records.find(n=>n.id===2).title='<img src=x onerror=alert(1)> 示例';
records.find(n=>n.id===2).excerpt='搜索摘要 <svg onload=alert(2)> 不执行';
let searchDelay=0, articleDelay=0, linksFailure=false, listFailure=false, missing=0;
let errors=[];
const pass=[];
const pause=ms=>new Promise(r=>setTimeout(r,ms));
async function check(name,fn){await fn();pass.push(name);console.log('PASS '+name)}
async function title(page,id){await page.waitForFunction(t=>document.querySelector('#reader-article-title')?.textContent===t,records.find(n=>n.id===id).title)}
async function listReady(page,count){await page.waitForFunction(n=>document.querySelectorAll('.note-results .public-note-link').length===n,count)}
async function noOverflow(page){assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1))}
function scope(url){const q=(url.searchParams.get('keyword')||'').toLowerCase(), c=url.searchParams.get('category')||'';return records.filter(n=>(!c||n.category===c||n.category.startsWith(c+'/'))&&(!q||[n.title,n.content,n.category,n.excerpt].join(' ').toLowerCase().includes(q)))}
(async()=>{
  await fs.mkdir(out,{recursive:true});
  const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH ? {executablePath:process.env.CHROME_PATH} : {})});
  try{
    const context=await browser.newContext({viewport:{width:390,height:844},reducedMotion:'reduce'});
    const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
    await context.route('**/api/public/notes**',async route=>{
      const url=new URL(route.request().url()), path=url.pathname;
      const ok=data=>route.fulfill({json:{code:0,data}}), fail=()=>route.fulfill({status:503,json:{code:503,message:'QA failure'}});
      if(path.endsWith('/categories'))return ok([{name:'课程/物理',count:50},{name:'技术/Java',count:87}]);
      if(path.endsWith('/search')){
        if(searchDelay)await pause(searchDelay);
        if(listFailure)return fail();
        const items=scope(url), size=Number(url.searchParams.get('pageSize')||12),p=Math.min(Number(url.searchParams.get('page')||1),Math.max(1,Math.ceil(items.length/size)));
        return ok({items:items.slice((p-1)*size,p*size).map(({content,userId,...n})=>n),total:items.length,page:p,pageSize:size});
      }
      const id=Number(path.split('/')[4]), n=records.find(n=>n.id===id);
      if(!n||id===missing)return route.fulfill({json:{code:404,message:'missing'}});
      if(path.endsWith('/related')){if(linksFailure)return fail();return ok(records.filter(x=>x.category===n.category&&x.id!==id).slice(0,4))}
      if(path.endsWith('/navigation')){if(linksFailure)return fail();const items=scope(url),index=items.findIndex(x=>x.id===id);return ok({previous:index>0?items[index-1]:null,next:index>=0?items[index+1]||null:null,position:index+1})}
      if(articleDelay)await pause(articleDelay);
      return ok(n);
    });
    await check('Paged archive reaches notes beyond first 100 and supports page jumps',async()=>{
      await page.goto(base+'/notes');await listReady(page,12);
      assert(!new URL(page.url()).searchParams.has('note'));
      await page.locator('.note-results').getByRole('button',{name:'下一页',exact:true}).click();
      await page.waitForURL(/page=2/);await listReady(page,12);
      assert((await page.locator('.note-results .public-note-link').first().textContent()).includes('测试笔记 125'));
      const number=page.locator('.note-results').getByRole('spinbutton',{name:'页码'});
      await number.fill('12');await number.press('Enter');await number.blur();
      await page.waitForURL(/page=12/);await listReady(page,5);
      assert(await page.locator('.note-results').getByRole('button',{name:'下一页',exact:true}).isDisabled());
      await page.screenshot({path:out+'/archive-mobile.png'});
    });
    await check('Body search displays highlighted clean excerpts and no active article',async()=>{
      const input=page.locator('.note-results').getByRole('textbox',{name:'搜索学习笔记'});
      await input.fill('body_only_token');await input.press('Enter');await listReady(page,1);
      assert.equal(await page.locator('.note-results mark').textContent(),'body_only_token');
      assert.equal(await page.locator('.knowledge-article').count(),0);
      await page.locator('.note-results .public-note-link').click();await title(page,1);
      await page.getByRole('button',{name:'返回列表',exact:true}).click();await listReady(page,1);
      assert.equal(await input.inputValue(),'body_only_token');
      await page.goBack();await title(page,1);await page.goBack();await listReady(page,1);
    });
    await check('Unsafe title and excerpt text are escaped, not rendered as HTML',async()=>{
      await page.goto(base+'/notes?page=12');await listReady(page,5);
      assert.equal(await page.locator('.note-results img,.note-results svg[onload]').count(),0);
      assert(await page.locator('.note-results').getByText('<img src=x onerror=alert(1)> 示例',{exact:true}).isVisible());
    });
    await check('Mobile category drawer filters full server result and closes',async()=>{
      await page.getByRole('button',{name:'笔记',exact:true}).click();
      await page.getByRole('dialog').locator('.category-tree-label').getByText('课程',{exact:true}).click();
      await page.getByRole('dialog').waitFor({state:'hidden'});await listReady(page,12);
      assert.equal(new URL(page.url()).searchParams.get('category'),'课程');
      await page.locator('.note-results').getByText('共 50 篇').waitFor();
    });
    await check('Previous and next traverse page boundaries and preserve list context',async()=>{
      await page.goto(base+'/notes?note=126&page=1');await title(page,126);
      const next=page.locator('.article-pagination button').last();
      await page.waitForFunction(()=>document.querySelector('.article-pagination button:last-child')?.disabled===false);
      await next.click();await title(page,125);assert.equal(new URL(page.url()).searchParams.get('page'),'2');
      await page.locator('.article-pagination button').first().click();await title(page,126);
      assert.equal(new URL(page.url()).searchParams.get('page'),'1');
      await page.goBack();await title(page,125);
      await page.getByRole('button',{name:'返回列表',exact:true}).click();await listReady(page,12);
      assert.equal(new URL(page.url()).searchParams.get('page'),'2');
    });
    await check('Related notes exclude current article and change category browsing scope',async()=>{
      await page.goto(base+'/notes?note=1');await title(page,1);
      await page.locator('.related-note').first().waitFor();assert.equal(await page.locator('.related-note').count(),4);
      assert(!(await page.locator('.related-note').allTextContents()).some(t=>t.includes('测试笔记 1 ')));
      await page.locator('.related-note').first().click();await title(page,50);
      assert.equal(new URL(page.url()).searchParams.get('category'),'课程/物理');
    });
    await check('Resume saves actual reading, remains opt-in after reload and restores position',async()=>{
      await page.goto(base+'/notes?note=137');await title(page,137);
      assert.equal(await page.locator('.resume-reading').count(),0);
      await page.mouse.move(200,400);
      await page.mouse.wheel(0,900);await pause(200);
      await page.evaluate(()=>{const body=document.querySelector('.article-body');const top=body.getBoundingClientRect().top+scrollY-160;const range=body.offsetHeight-(innerHeight-160-80);scrollTo({top:top+range*.45,behavior:'instant'})});
      await page.waitForFunction(()=>{const p=JSON.parse(localStorage.getItem('linge-note-reading-v1')||'[]').find(x=>x.noteId===137);return p?.progress>.4&&p.progress<.5}).catch(async e=>{console.log(await page.evaluate(()=>({storage:localStorage.getItem('linge-note-reading-v1'),y:scrollY,body:document.querySelector('.article-body').getBoundingClientRect().toJSON(),height:document.querySelector('.article-body').offsetHeight,errors:document.body.innerText.slice(0,200)})));throw e});
      await page.reload();await title(page,137);await page.locator('.resume-reading').waitFor();
      const before=await page.evaluate(()=>scrollY);assert(before<550,String(before));
      const saved=await page.evaluate(()=>JSON.parse(localStorage.getItem('linge-note-reading-v1')).find(x=>x.noteId===137).progress);
      assert(saved>.4&&saved<.5);
      await page.locator('.resume-reading').click();
      await page.waitForFunction(()=>scrollY>1000);
      const progress=await page.evaluate(()=>{const b=document.querySelector('.article-body');return (scrollY-(b.getBoundingClientRect().top+scrollY-160))/(b.offsetHeight-(innerHeight-160-80))});
      assert(Math.abs(progress-.45)<.06,String(progress));
      await page.screenshot({path:out+'/resume-mobile.png'});
    });
    await check('Updated articles invalidate stored resume positions',async()=>{
      records[0].updatedAt='2026-09-07T12:00:00';
      await page.reload();await title(page,137);
      assert.equal(await page.locator('.resume-reading').count(),0);
      records[0].updatedAt='2026-09-06T12:00:00';
    });
    await check('Slow search does not block direct reading; stale search cannot replace current results',async()=>{
      searchDelay=1400;
      await page.goto(base+'/notes?note=5&page=1');await title(page,5);
      assert(await page.locator('.knowledge-article').isVisible());
      await page.goto(base+'/notes?q=body_only_token');
      await page.locator('.note-results').getByRole('textbox',{name:'搜索学习笔记'}).fill('does_not_exist');
      await page.locator('.note-results').getByRole('button',{name:'搜索',exact:true}).click();
      await page.locator('.note-results').getByText('没有找到对应笔记。').waitFor();
      await pause(1600);assert.equal(await page.locator('.note-results .public-note-link').count(),0);
      searchDelay=0;
    });
    await check('Rapid article selections ignore stale detail responses',async()=>{
      await page.goto(base+'/notes');await listReady(page,12);articleDelay=1200;
      await page.locator('.note-results .public-note-link').first().click();
      await page.waitForFunction(()=>new URLSearchParams(location.search).get('note')==='137');
      await page.getByRole('button',{name:'笔记',exact:true}).click();
      await page.getByRole('dialog').getByRole('button',{name:/测试笔记 136/}).click();
      await title(page,136);await pause(1400);
      assert.equal(await page.locator('#reader-article-title').textContent(),'测试笔记 136');articleDelay=0;
    });
    await check('Auxiliary API failure leaves article readable and supports retry',async()=>{
      linksFailure=true;await page.goto(base+'/notes?note=137');await title(page,137);
      await page.getByText('延伸阅读暂时不可用。').waitFor();
      assert(await page.locator('.article-body').isVisible());linksFailure=false;
      await page.locator('.reading-links-error').getByRole('button',{name:'重试',exact:true}).click();
      await page.locator('.related-note').first().waitFor();
      listFailure=true;await page.goto(base+'/notes');await page.locator('.note-results').getByText('文章列表暂时不可用，请重试。').waitFor();
      listFailure=false;await page.locator('.note-results').getByRole('button',{name:'重试',exact:true}).click();await listReady(page,12);
    });
    await check('Responsive archive and reader fit mobile, intermediate and wide layouts',async()=>{
      for(const width of [320,390,820,1024,1101,1279,1280,1440]){
        await page.setViewportSize({width,height:900});await page.goto(base+'/notes');await listReady(page,12);await noOverflow(page);
        await page.screenshot({path:out+'/results-'+width+'.png'});
        await page.locator('.note-results .public-note-link').first().click();await title(page,137);await noOverflow(page);
        await page.screenshot({path:out+'/article-'+width+'.png'});
      }
    });
    await check('Normal-motion outline closes drawer, clears header and returns keyboard focus',async()=>{
      await page.setViewportSize({width:390,height:844});await page.emulateMedia({reducedMotion:'no-preference'});
      await page.getByRole('button',{name:'目录',exact:true}).click();
      await page.getByRole('dialog').getByRole('button',{name:'推导过程',exact:true}).click();
      await page.getByRole('dialog').waitFor({state:'hidden'});
      await page.waitForFunction(()=>document.activeElement?.textContent==='推导过程'&&Math.abs(document.activeElement.getBoundingClientRect().top-160)<3);
      await page.getByRole('button',{name:'笔记',exact:true}).click();await page.keyboard.press('Escape');
      await page.getByRole('dialog').waitFor({state:'hidden'});
      await page.waitForFunction(()=>document.activeElement?.textContent==='笔记');
    });
    assert.deepEqual(errors,[]);console.log(JSON.stringify({passed:pass.length,checks:pass,screenshots:out},null,2));
  }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});

