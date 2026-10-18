/* Design-only simulation. No model, native task repository, or personal data access. */
const $ = (s) => document.querySelector(s);
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const icon = name => `<svg aria-hidden="true"><use href="#i-${name}"/></svg>`;
const logo = '<span class="lifi-logo" aria-hidden="true"></span>';
const stamp = () => '今天 · ' + new Intl.DateTimeFormat('zh-CN', {hour:'2-digit',minute:'2-digit',timeZone:'Asia/Shanghai'}).format(new Date());
const state = {
  messages: [], running:false, waiting:false, tab:'Lifi', viewed:null, lastQuery:'', timer:null, runToken:0,
  memories:[{text:'每周安排 3 次运动，每次 40 分钟',source:'来自运动习惯对话'},{text:'复杂任务更喜欢拆成几个小步骤',source:'来自计划整理对话'}],
  history:[{id:'sample-habit',title:'给运动留一点时间',time:'昨天 · 20:18',messages:[{type:'user',text:'帮我安排每周 3 次力量训练，每次 40 分钟。'},{type:'text',text:'可以从周一、周三、周五开始，每次留出 40 分钟。循序渐进，比一次练很久更容易坚持。'},{type:'created',kind:'habit',title:'力量训练',detail:'每周 3 次 · 每次 40 分钟'}]},
  {id:'sample-task',title:'准备产品评审',time:'10 月 11 日 · 14:32',messages:[{type:'user',text:'明天下午 3 点和产品开评审会。'},{type:'text',text:'已经为你整理好这项任务。'},{type:'created',kind:'task',title:'产品评审会',detail:'10 月 12 日 · 15:00'}]}]
};
let toastTimer, opener;
const input = $('#input');
const prompts = [
  {icon:'task',title:'安排一个任务',example:'明天下午 3 点和产品开评审会'},
  {icon:'habit',title:'建立一个习惯',example:'每周 3 次力量训练，每次 40 分钟'},
  {icon:'plan',title:'帮我理清计划',example:'把这周的任务排一下优先级'}
];
function welcome() {
  return `<section class="welcome"><div class="hero-symbol">${logo}</div><p class="overline">你好，欢迎回来</p><h2>把想法告诉我，<br>我们一起往前走。</h2><p class="description">我是 Lifi。帮你安排任务、建立习惯，<br>也把复杂的计划，变成下一步。</p><div class="quick-label">从这里开始，也可以随意聊聊</div><div class="quick-list">${prompts.map((p,i)=>`<button class="quick-row" data-prompt="${i}"><span class="quick-icon">${icon(p.icon)}</span><span class="quick-text"><strong>${p.title}</strong><small>${p.example}</small></span>${icon('chevron')}</button>`).join('')}</div></section>`;
}
function assistantLabel() { return `<div class="assistant-label">${logo} Lifi</div>`; }
function approvalCard(m, index, readonly) {
  const reading = m.kind === 'read';
  const pending = m.status === 'pending';
  const settled = m.status === 'approved' ? (reading ? '已允许一次' : '已确认创建') : m.status === 'rejected' ? '已拒绝 · 未执行' : '等待确认';
  const title = reading ? '读取本周的任务' : m.kind === 'habit' ? m.title : m.title;
  const scope = reading ? '<div><span>时间范围</span><strong>未来 7 天</strong></div><div><span>任务状态</span><strong>未完成</strong></div><div><span>使用目的</span><strong>整理优先级与安排</strong></div>' : m.kind === 'habit' ? `<div><span>重复</span><strong>${esc(m.frequency)}</strong></div><div><span>每次目标</span><strong>${esc(m.goal)}</strong></div>` : `<div><span>截止时间</span><strong>${esc(m.due.replace('T',' · '))}</strong></div><div><span>备注</span><strong>${esc(m.note || '未填写')}</strong></div>`;
  return `<article class="action-card"><div class="card-eyebrow">${icon(reading ? 'lock' : m.kind === 'habit' ? 'habit' : 'task')} ${reading ? '读取任务' : m.kind === 'habit' ? '创建习惯' : '创建任务'}<span class="badge">${settled}</span></div><h3>${esc(title)}</h3><p>${reading ? '了解已有安排后，我可以帮你找到更合适的先后顺序。' : '我整理好了这些信息，确认后添加到 Deadliner。'}</p><div class="scope-box">${scope}</div>${pending && !readonly ? `<div class="card-buttons"><button class="secondary" data-reject="${index}">拒绝</button><button class="primary" data-approve="${index}">${reading ? '允许一次' : '确认添加'}${icon(reading ? 'chevron' : 'plus')}</button></div>${!reading ? `<button class="edit-inline" data-edit="${index}">${icon('edit')}编辑内容</button>` : ''}<p class="tool-footnote">${reading ? '本次确认只用于这一次读取。' : '仅创建上面展示的内容。'}</p>` : `<div class="result-status">${icon(m.status === 'rejected' ? 'close' : 'check')}${settled}</div>`}</article>`;
}
function messageHTML(m,i,readonly=false) {
  if(m.type === 'user') return `<div class="user-message">${esc(m.text)}</div>`;
  if(m.type === 'text') return `${assistantLabel()}<p class="ai-text">${esc(m.text)}</p>`;
  if(m.type === 'process') return `<details class="process" ${m.open ? 'open' : ''} data-process="${i}"><summary><span class="completed-dot"></span>${esc(m.title)}${icon('chevron')}</summary><ol>${m.steps.map(s=>`<li>${esc(s)}</li>`).join('')}</ol></details>`;
  if(m.type === 'approval') return approvalCard(m,i,readonly);
  if(m.type === 'plan') return `<article class="action-card"><div class="card-eyebrow">${icon('plan')}安排建议<span class="badge">3 个重点</span></div><h3>先做最接近截止的事</h3><div class="plan-row"><span class="plan-time">今天<br>优先</span><div><strong>完成系统论作业</strong><small>明天 19:00 截止 · 建议预留 90 分钟</small></div></div><div class="plan-row"><span class="plan-time">明天<br>其次</span><div><strong>准备产品评审材料</strong><small>周四 15:00 评审 · 先梳理问题与方案</small></div></div><div class="plan-row"><span class="plan-time">周五<br>之后</span><div><strong>整理本周项目进展</strong><small>周五 18:00 截止 · 汇总已有记录</small></div></div></article>${!readonly ? '<div class="follow-ups"><button data-follow="把产品评审材料拆成几个小任务">拆解评审准备</button><button data-follow="记住：复杂任务我喜欢分成小步骤">记住我的偏好</button></div>' : ''}`;
  if(m.type === 'steps') return `<article class="action-card"><div class="card-eyebrow">${icon('plan')}目标拆解<span class="badge">3 个步骤</span></div><h3>准备产品评审材料</h3><div class="plan-row"><span class="plan-time">01</span><div><strong>列出需要评审的问题</strong><small>收集反馈，明确这次要做的决定。</small></div></div><div class="plan-row"><span class="plan-time">02</span><div><strong>整理方案与取舍</strong><small>把主要方案、限制与待确认项放在一起。</small></div></div><div class="plan-row"><span class="plan-time">03</span><div><strong>提前发送评审材料</strong><small>留出阅读时间，让会议聚焦讨论。</small></div></div></article>`;
  if(m.type === 'created') return `<article class="action-card"><div class="card-eyebrow">${icon(m.kind === 'habit' ? 'habit' : 'task')} ${m.kind === 'habit' ? '习惯' : '任务'}<span class="badge">已添加</span></div><h3>${esc(m.title)}</h3><div class="task-detail">${icon(m.kind === 'habit' ? 'habit' : 'history')}${esc(m.detail)}</div><div class="result-status">${icon('check')}已添加到${m.kind === 'habit' ? '习惯' : '任务'}列表</div></article>`;
  if(m.type === 'memory') return `<div class="memory-receipt">${icon('memory')}<span>记住了：${esc(m.text)}</span>${!readonly ? '<button data-action="memory">管理</button>' : ''}</div>`;
  if(m.type === 'error') return `${assistantLabel()}<article class="action-card error-card"><div class="card-eyebrow">${icon('close')}连接未完成</div><h3>刚才没能连接上</h3><p>这次请求没有完成。你的内容还在，可以稍后再试。</p>${!readonly ? '<button class="primary" data-action="retry">重试这次请求</button>' : ''}</article>`;
  return '';
}
function render(scroll=false) {
  const record = state.viewed ? state.history.find(h=>h.id===state.viewed) : null;
  if(!record) state.viewed=null;
  const messages = record ? record.messages : state.messages;
  const content = $('#content');
  const position=content.scrollTop;
  if(state.tab !== 'Lifi') content.innerHTML=`<section class="away-view"><div class="quick-icon">${icon(state.tab === '习惯' ? 'habit' : state.tab === '概览' ? 'grid' : 'task')}</div><h2>${esc(state.tab)}</h2><p>这里展示切换主导航后的页面占位。</p><div class="away-card"><strong>对话会一直留在 Lifi</strong><p>${state.running ? 'Lifi 正在继续处理。你可以随时回去查看进度。' : state.waiting ? 'Lifi 等待你的确认。回到对话后可以继续操作。' : '切换页面不会清空当前对话。'}</p><button class="primary" data-action="current">返回 Lifi</button></div></section>`;
  else if(!messages.length) content.innerHTML=welcome();
  else content.innerHTML=`<div class="chat-date">${record ? esc(record.time)+' · 历史对话' : '今天'}</div>${messages.map((m,i)=>messageHTML(m,i,!!record)).join('')}${state.running && !record ? '<div class="loading-message"><span class="pulsing-dot"></span>Lifi 正在整理你的请求…</div>' : ''}`;
  content.scrollTop=scroll ? content.scrollHeight : position;
  $('#composer').hidden=state.tab!=='Lifi' || !!record;
  $('#context-bar').innerHTML=record ? '<button class="memory-link" data-action="current">历史对话仅供查看 · 返回当前对话 '+icon('chevron')+'</button>' : `<button class="memory-link" data-action="memory">${icon('memory')}记忆与偏好 · ${state.memories.length} 条${icon('chevron')}</button>`;
  const banner=$('#run-banner');
  banner.hidden=!(state.running || state.waiting);
  banner.innerHTML=`${state.running ? '<span class="pulsing-dot"></span>' : icon('lock')}<span>${state.running ? '正在处理 · 离开页面也会继续' : '等待确认 · 还未执行这次操作'}</span>${state.tab !== 'Lifi' || record ? '<button data-action="current">回到对话</button>' : ''}`;
  $('.current-title').textContent=state.messages.find(m=>m.type==='user')?.text || '从一句话开始';
  $('.current-meta').textContent=state.running ? '正在处理' : state.waiting ? '等待确认' : state.messages.length ? '继续当前对话' : '尚未开始';
  $('#rail-history').innerHTML=state.history.length ? state.history.map(h=>`<button class="rail-history-item" data-history="${esc(h.id)}">${esc(h.title)}<small>${esc(h.time)}</small></button>`).join('') : '<p class="empty-note">还没有历史对话</p>';
  document.querySelectorAll('[data-tab]').forEach(b=>{b.classList.toggle('selected',b.dataset.tab===state.tab); if(b.dataset.tab===state.tab)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});
  updateComposer();
}
function updateComposer() {
  const send=$('#send');
  send.disabled=state.waiting || (!state.running && !input.value.trim());
  send.innerHTML=state.running ? '<span class="stop-square"></span>' : icon('arrow');
  send.setAttribute('aria-label',state.running ? '停止处理' : '发送');
  input.disabled=state.running || state.waiting;
  input.placeholder=state.waiting ? '先确认上面的操作，再继续对话' : state.running ? '正在处理你的请求…' : '告诉我，你想做什么…';
  $('#composer-hint').textContent=state.running ? '点击方块停止本次处理' : state.waiting ? '操作范围与内容见上方卡片' : '任务、习惯，或一个还没理清的想法';
}
function toast(text) {clearTimeout(toastTimer);$('#toast').textContent=text;$('#toast').hidden=false;toastTimer=setTimeout(()=>$('#toast').hidden=true,2600);}
function fill(text) {state.tab='Lifi';state.viewed=null;render();if(state.running||state.waiting){toast('先完成或停止当前请求');return;} input.value=text;input.style.height='auto';input.style.height=Math.min(input.scrollHeight,90)+'px';updateComposer();input.focus();}
function schedule(ms,callback) {clearTimeout(state.timer);const token=++state.runToken;state.timer=setTimeout(()=>{if(token===state.runToken)callback();},ms);}
function endRun() {clearTimeout(state.timer);state.runToken++;state.running=false;}
function makeProposal(kind,query) {
  if(kind==='habit') return {type:'approval',kind,status:'pending',title:query.includes('跑步')?'跑步':'力量训练',frequency:'每周 3 次',goal:query.includes('30')?'30 分钟':'40 分钟'};
  return {type:'approval',kind:'task',status:'pending',title:query.includes('作业')?'完成系统论作业':query.includes('评审')?'产品评审会':query.replace(/^(创建|添加|帮我安排|帮我创建)[：:]?/,'').slice(0,50),due:'2026-10-14T15:00',note:query.includes('评审')?'和产品一起评审方案':''};
}
function decide(query) {
  const isMemory=/记住|偏好/.test(query);
  if(isMemory) {
    const text=query.replace(/^记住[：:]?\s*/,'');state.memories.push({text,source:'来自当前对话'});state.messages.push({type:'text',text:'好的，以后整理计划时，我会参考这个偏好。'},{type:'memory',text});
  } else if(/习惯|训练|跑步|运动/.test(query)) {
    state.messages.push({type:'text',text:'从一个容易坚持的节奏开始。我为你整理了这个习惯：'},makeProposal('habit',query));state.waiting=true;
  } else if(/优先|这周|本周|计划|拆|安排.*任务/.test(query)) {
    state.messages.push({type:'process',title:'已理解请求 · 需要任务上下文',steps:['理解你想整理本周的安排','准备读取未来 7 天的未完成任务']},{type:'text',text:'我想先看看你已有的任务，再根据截止时间帮你排一个更从容的顺序。'},{type:'approval',kind:'read',status:'pending',intent:/拆/.test(query)?'steps':'plan'});state.waiting=true;
  } else if(/明天|任务|评审|作业|创建|添加/.test(query)) {
    state.messages.push({type:'text',text:'收到，我把这件事整理成了一项任务。看看信息是否合适：'},makeProposal('task',query));state.waiting=true;
  } else {
    state.messages.push({type:'text',text:'我们可以先从一件事开始：你现在最想完成什么？告诉我大概的截止时间，我帮你把它整理成任务；也可以一起理清这周的安排。'});
  }
  state.running=false;render(state.tab==='Lifi'&&!state.viewed);
}
function send(query,delay=1500) {
  if(state.running||state.waiting)return;
  state.lastQuery=query;state.viewed=null;state.tab='Lifi';state.messages.push({type:'user',text:query});state.running=true;
  input.value='';input.style.height='auto';render(true);schedule(delay,()=>decide(query));
}
function approve(index) {
  const m=state.messages[index];if(!m || m.status!=='pending' || state.viewed)return;
  m.status='approved';state.waiting=false;state.running=true;render(true);
  schedule(1400,()=>{
    state.running=false;
    if(m.kind==='read' && m.intent==='steps')state.messages.push({type:'process',title:'已理解任务上下文 · 拆解完成',steps:['读取当前任务','识别评审准备中的主要步骤','生成拆解建议，未创建额外任务']},{type:'text',text:'把评审准备分成三个小步骤，每一步都会更容易开始。'},{type:'steps'});
    else if(m.kind==='read')state.messages.push({type:'process',title:'已读取 3 项任务 · 处理完成',steps:['读取未来 7 天内的未完成任务','按截止时间与准备工作整理','生成建议，未修改已有任务']},{type:'text',text:'这周有 3 件事值得优先关注。先给最近截止的任务留出完整时间，再准备评审材料。'}, {type:'plan'});
    else state.messages.push({type:'created',kind:m.kind,title:m.title,detail:m.kind==='habit'?`${m.frequency} · 每次 ${m.goal}`:m.due.replace('T',' · ')},{type:'text',text:m.kind==='habit'?'习惯已经添加好了。给自己留一点弹性，持续做下去。':'任务已经添加好了。接下来想做什么，继续告诉我。'});
    render(state.tab==='Lifi'&&!state.viewed);
  });
}
function reject(index) {
  const m=state.messages[index];if(!m || m.status!=='pending' || state.viewed)return;
  m.status='rejected';state.waiting=false;
  state.messages.push({type:'text',text:m.kind==='read'?'好的，这次不读取任务。你可以直接告诉我几件要做的事，我继续帮你整理。':'好的，这次没有添加。你可以调整想法后再告诉我。'});render(true);
}
function stop() {
  if(!state.running)return;endRun();state.messages.push({type:'text',text:'已停止这次处理。对话内容会保留，你可以继续补充。'});render(true);toast('已停止处理');
}
function newChat() {
  if(state.running||state.waiting){toast('当前请求尚未结束，请先停止或完成确认');return;}
  if(state.messages.length)state.history.unshift({id:'history-'+Date.now(),title:state.messages.find(m=>m.type==='user')?.text.slice(0,20)||'新对话',time:stamp(),messages:JSON.parse(JSON.stringify(state.messages))});
  state.messages=[];state.viewed=null;state.tab='Lifi';input.value='';input.style.height='auto';closeSheet();render();$('#content').scrollTop=0;
}
function current() {state.viewed=null;state.tab='Lifi';closeSheet();render(true);}
function showSheet(title,subtitle,html) {
  opener=document.activeElement;
  $('#overlay').innerHTML=`<section class="sheet" role="dialog" aria-modal="true" aria-labelledby="sheet-title"><div class="sheet-handle"></div><div class="sheet-title"><h2 id="sheet-title">${title}</h2><button class="icon-button" data-action="close" aria-label="关闭">${icon('close')}</button></div><p class="sheet-intro">${subtitle}</p>${html}</section>`;$('#overlay').hidden=false;$('.sheet [data-action="close"]').focus();
}
function closeSheet() {const wasOpen=!$('#overlay').hidden;$('#overlay').hidden=true;$('#overlay').innerHTML='';if(wasOpen && opener?.isConnected)opener.focus();}
function historySheet() {
  showSheet('你的对话','从上次的想法接着走。历史记录仅供查看。',`<div class="sheet-section">当前对话</div><div class="history-item"><button class="history-open" data-action="current"><strong>${esc(state.messages.find(m=>m.type==='user')?.text || '从一句话开始')}</strong><small>${state.running?'正在处理':state.waiting?'等待确认':'点击继续当前对话'}</small></button><span class="tiny-dot"></span></div><div class="sheet-section">历史对话 · ${state.history.length}</div>${state.history.length ? state.history.map(h=>`<div class="history-item"><button class="history-open" data-history="${esc(h.id)}"><strong>${esc(h.title)}</strong><small>${esc(h.time)} · ${h.messages.length} 条</small></button><button class="delete-button" data-delete-history="${esc(h.id)}" aria-label="删除对话 ${esc(h.title)}">删除</button></div>`).join('') : '<div class="empty-note">还没有历史对话<br>开始新对话时，上一段会留在这里。</div>'}`);
}
function memorySheet() {
  showSheet('记忆与偏好','让之后的建议更贴近你。你可以随时删除。',`<div class="memory-note">这些信息用来理解你的偏好。<br>输入“记住：…”可以试着添加一条记忆。</div><div class="sheet-section">已记住 · ${state.memories.length} 条</div>${state.memories.length ? state.memories.map((m,i)=>`<div class="memory-item"><div>${esc(m.text)}<small>${esc(m.source)}</small></div><button class="delete-button" data-delete-memory="${i}" aria-label="删除记忆 ${esc(m.text)}">删除</button></div>`).join('') : '<div class="empty-note">还没有记忆<br>你可以在对话中告诉 Lifi 你的偏好。</div>'}`);
}
function editSheet(index) {
  const m=state.messages[index];if(!m || m.status!=='pending')return;
  const habit=m.kind==='habit';
  showSheet(habit?'调整习惯':'调整任务','保存后回到预览卡片，确认时再添加。',`<form class="edit-form" id="edit-form" data-index="${index}"><label for="edit-title">名称</label><input id="edit-title" name="title" value="${esc(m.title)}" required maxlength="80">${habit ? `<label for="edit-frequency">重复频次</label><input id="edit-frequency" name="frequency" value="${esc(m.frequency)}" required><label for="edit-goal">每次目标</label><input id="edit-goal" name="goal" value="${esc(m.goal)}" required>` : `<label for="edit-due">截止时间</label><input id="edit-due" name="due" type="datetime-local" value="${esc(m.due)}" required><label for="edit-note">备注</label><input id="edit-note" name="note" value="${esc(m.note)}">`}<button class="primary" type="submit">保存修改</button></form>`);
}
function scene(name) {
  // Lab controls reset only simulated demo state; these are outside the app UI.
  endRun();state.waiting=false;state.messages=[];state.viewed=null;state.tab='Lifi';input.value='';input.style.height='auto';closeSheet();
  document.querySelectorAll('[data-scene]').forEach(b=>b.classList.toggle('active',b.dataset.scene===name));
  if(name==='home'){render();$('#content').scrollTop=0;return;}
  const query=name==='task'?'明天下午 3 点和产品开评审会':name==='habit'?'每周 3 次力量训练，每次 40 分钟':'把这周的任务排一下优先级';state.lastQuery=query;
  if(name==='running'){send(query,18000);return;}
  state.messages.push({type:'user',text:query});
  if(name==='error')state.messages.push({type:'error'});else decide(query);
  render(true);
}
$('#composer').addEventListener('submit',event=>{event.preventDefault();if(state.running){stop();return;}const text=input.value.trim();if(text)send(text);});
input.addEventListener('input',()=>{input.style.height='auto';input.style.height=Math.min(input.scrollHeight,90)+'px';updateComposer();});
input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();$('#composer').requestSubmit();}});
document.addEventListener('click',event=>{
  const b=event.target.closest('button');if(!b)return;
  if(b.dataset.prompt!==undefined)fill(prompts[Number(b.dataset.prompt)].example);
  else if(b.dataset.scene)scene(b.dataset.scene);
  else if(b.dataset.approve!==undefined)approve(Number(b.dataset.approve));
  else if(b.dataset.reject!==undefined)reject(Number(b.dataset.reject));
  else if(b.dataset.edit!==undefined)editSheet(Number(b.dataset.edit));
  else if(b.dataset.follow)fill(b.dataset.follow);
  else if(b.dataset.tab){state.tab=b.dataset.tab;state.viewed=null;render();}
  else if(b.dataset.history){state.viewed=b.dataset.history;state.tab='Lifi';closeSheet();render();$('#content').scrollTop=0;}
  else if(b.dataset.deleteHistory){state.history=state.history.filter(h=>h.id!==b.dataset.deleteHistory);if(state.viewed===b.dataset.deleteHistory)state.viewed=null;render();historySheet();toast('已删除这段示例对话');}
  else if(b.dataset.deleteMemory!==undefined){state.memories.splice(Number(b.dataset.deleteMemory),1);render();memorySheet();toast('已删除这条示例记忆');}
  else if(b.dataset.action){({new:newChat,current,history:historySheet,memory:memorySheet,close:closeSheet,retry:()=>{if(state.running||state.waiting)return;state.messages=state.messages.filter(m=>m.type!=='error');state.running=true;render(true);schedule(1400,()=>decide(state.lastQuery));}})[b.dataset.action]?.();}
});
document.addEventListener('submit',event=>{
  if(event.target.id!=='edit-form')return;event.preventDefault();const form=event.target, m=state.messages[Number(form.dataset.index)];if(!m || m.status!=='pending')return;
  const values=new FormData(form);if(!String(values.get('title')).trim()){toast('请填写名称');return;}for(const [key,value] of values)m[key]=String(value).trim();closeSheet();render();toast('已更新，确认后再添加');
});
document.addEventListener('toggle',event=>{const el=event.target;if(el.matches?.('[data-process]')&&!state.viewed){const m=state.messages[Number(el.dataset.process)];if(m?.type==='process')m.open=el.open;}},true);
$('#overlay').addEventListener('click',event=>{if(event.target===$('#overlay'))closeSheet();});
document.addEventListener('keydown',event=>{
  if($('#overlay').hidden)return;
  if(event.key==='Escape'){closeSheet();return;}
  if(event.key==='Tab'){
    const nodes=[...$('#overlay').querySelectorAll('button,input,[tabindex="0"]')].filter(el=>!el.disabled);
    const first=nodes[0],last=nodes[nodes.length-1];
    if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
  }
});
$('#theme-toggle').addEventListener('click',()=>{const dark=document.body.classList.toggle('dark');$('#theme-toggle span').textContent=dark?'浅色':'深色';$('#theme-toggle').setAttribute('aria-pressed',dark);});
$('#width-toggle').addEventListener('click',()=>{const wide=document.body.classList.toggle('wide');$('#width-toggle span').textContent=wide?'手机':'宽屏';$('#width-toggle').setAttribute('aria-pressed',wide);});
render();
