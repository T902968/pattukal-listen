/* Pattukal Web extras: Music Galaxy and Buddy. Loaded only when opened, so they never slow the first paint or touch the audio engine. */
(function(){
"use strict";
const P=window.PK,$=s=>document.querySelector(s);

/* ---------- Music Galaxy: only real data (what you played, favourited and searched) ---------- */
let gOn=false,raf=0,nodes=[],cam={x:0,y:0,z:1},ptrs=new Map(),moved=0,pinch=0,inited=false;
const cv=()=>$('#gcv');
function hash(s){let h=0;for(const c of String(s))h=(h*31+c.charCodeAt(0))>>>0;return h}
function build(){
  nodes=[];const byArtist=new Map();
  [...P.hist,...P.likes,...P.seen.values()].forEach(s=>{if(!s||!s.artist)return;if(!byArtist.has(s.artist))byArtist.set(s.artist,new Map());byArtist.get(s.artist).set(s.id,s)});
  const mine=new Set(P.topArtists());
  const hubs=P.LANGS.slice(0,8);
  hubs.forEach((l,i)=>{const a=i/hubs.length*6.283;nodes.push({k:'lang',x:Math.cos(a)*380,y:Math.sin(a)*380,r:20,label:l[1],q:l[2],c:'#ffd24d'})});
  P.MOODS.forEach((m,i)=>{const a=i/P.MOODS.length*6.283+.3;nodes.push({k:'mood',x:Math.cos(a)*200,y:Math.sin(a)*200,r:12,label:m[0],q:m[1],c:'#4de0d0'})});
  [...byArtist.entries()].slice(0,60).forEach(([name,songs])=>{
    const h=hash(name),a=(h%3600)/3600*6.283,d=80+((h>>>8)%300);const ax=Math.cos(a)*d,ay=Math.sin(a)*d;
    nodes.push({k:'artist',x:ax,y:ay,r:mine.has(name)?13:9,label:name,songs:[...songs.values()],c:mine.has(name)?'#ff6fb1':'#b07bff'});
    [...songs.values()].slice(0,6).forEach((s,i)=>{const oa=i/6*6.283,od=26+(i%2)*8;nodes.push({k:'song',x:ax+Math.cos(oa)*od,y:ay+Math.sin(oa)*od,r:3.5,label:s.title,song:s,c:'#fff',parent:name,oa,od,ax,ay})})});
}
function draw(ts){
  const c=cv(),x=c.getContext('2d'),dpr=Math.min(devicePixelRatio||1,2),w=c.clientWidth,h=c.clientHeight;
  if(c.width!==w*dpr||c.height!==h*dpr){c.width=w*dpr;c.height=h*dpr}
  x.setTransform(dpr,0,0,dpr,0,0);x.fillStyle=getComputedStyle(document.documentElement).getPropertyValue('--bg')||'#000';x.fillRect(0,0,w,h);
  const t=P.still()?0:ts/1000;
  for(let i=0;i<70;i++){const hx=hash('s'+i);x.fillStyle='rgba(255,255,255,'+(.15+(hx%5)/20)+')';x.fillRect((hx%1000)/1000*w,((hx>>>10)%1000)/1000*h,1.4,1.4)}
  const sx=n=>w/2+(n.x-cam.x)*cam.z,sy=n=>h/2+(n.y-cam.y)*cam.z;
  nodes.forEach(n=>{if(n.k==='song'){const a=n.oa+t*.6;n.x=n.ax+Math.cos(a)*n.od;n.y=n.ay+Math.sin(a)*n.od}});
  x.lineWidth=1;
  nodes.forEach(n=>{if(n.k!=='song')return;const p=nodes.find(m=>m.k==='artist'&&m.label===n.parent);if(!p)return;x.strokeStyle='rgba(255,255,255,.08)';x.beginPath();x.moveTo(sx(p),sy(p));x.lineTo(sx(n),sy(n));x.stroke()});
  const cur=P.getCur();
  nodes.forEach(n=>{const px=sx(n),py=sy(n);if(px<-40||py<-40||px>w+40||py>h+40)return;const r=n.r*cam.z,on=n.song&&cur&&n.song.id===cur.id;
    const g=x.createRadialGradient(px,py,0,px,py,r*3.2);g.addColorStop(0,n.c+(n.c.length===7?'cc':''));g.addColorStop(1,'rgba(0,0,0,0)');x.fillStyle=g;x.beginPath();x.arc(px,py,r*(on?4:3),0,7);x.fill();
    x.fillStyle=n.c;x.beginPath();x.arc(px,py,Math.max(1.5,r*.7),0,7);x.fill();
    if(n.k!=='song'&&cam.z>.55||n.k==='lang'||cam.z>1.6&&n.k==='song'){x.fillStyle='rgba(255,255,255,.85)';x.font=(n.k==='lang'?13:11)+'px system-ui';x.textAlign='center';x.fillText(n.label.slice(0,22),px,py+r*1.6+12)}});
}
function loop(ts){if(!gOn)return;draw(ts);raf=requestAnimationFrame(loop)}
function pick(ex,ey){const c=cv(),r=c.getBoundingClientRect(),w=r.width,h=r.height;let best=null,bd=1e9;
  nodes.forEach(n=>{const px=w/2+(n.x-cam.x)*cam.z,py=h/2+(n.y-cam.y)*cam.z,d=Math.hypot(px-(ex-r.left),py-(ey-r.top));const lim=Math.max(18,n.r*cam.z*2);if(d<lim&&d<bd){bd=d;best=n}});return best}
function open(n){
  if(n.k==='song'){P.play(n.song,[n.song]);P.toast('Playing '+n.song.title);return}
  if(n.k==='artist'){const l=n.songs;P.sheet(`<h3>${P.esc(n.label)}</h3><div class="list">${l.map((s,i)=>`<button class="it" data-i="${i}"><img src="${P.thumb(s.id)}" data-id="${P.esc(s.id)}" referrerpolicy="no-referrer" alt=""><span class="t"><b>${P.esc(s.title)}</b><i>${P.esc(s.artist)}</i></span></button>`).join('')}</div><button class="opt" id="gmore"><span>More from this artist</span></button>`);
    $('#sheet').onclick=e=>{const b=e.target.closest('.it');if(b){P.play(l[+b.dataset.i],l);P.closeSheet();close()}else if(e.target.closest('#gmore')){P.closeSheet();close();P.openSearch(n.label)}};return}
  P.closeSheet();close();P.openSearch(n.q)}
function close(){gOn=false;cancelAnimationFrame(raf);$('#galaxy').classList.remove('on')}
P.galaxy=function(){
  build();if(!nodes.length){P.toast('Play some songs first');return}
  cam={x:0,y:0,z:P.S.lowEnd?.8:.9};gOn=true;$('#galaxy').classList.add('on');
  if(!inited){inited=true;const c=cv();
    c.addEventListener('pointerdown',e=>{c.setPointerCapture(e.pointerId);ptrs.set(e.pointerId,{x:e.clientX,y:e.clientY});moved=0;pinch=0});
    c.addEventListener('pointermove',e=>{const p=ptrs.get(e.pointerId);if(!p)return;
      if(ptrs.size===2){const [a,b]=[...ptrs.values()];const d0=Math.hypot(a.x-b.x,a.y-b.y);p.x=e.clientX;p.y=e.clientY;const [a2,b2]=[...ptrs.values()];const d1=Math.hypot(a2.x-b2.x,a2.y-b2.y);if(d0>0)cam.z=Math.max(.3,Math.min(4,cam.z*d1/d0));moved+=10;return}
      const dx=e.clientX-p.x,dy=e.clientY-p.y;p.x=e.clientX;p.y=e.clientY;moved+=Math.abs(dx)+Math.abs(dy);cam.x-=dx/cam.z;cam.y-=dy/cam.z});
    const up=e=>{const was=ptrs.has(e.pointerId);ptrs.delete(e.pointerId);if(was&&e.type==='pointerup'&&moved<8){const n=pick(e.clientX,e.clientY);if(n)open(n)}};
    c.addEventListener('pointerup',up);c.addEventListener('pointercancel',up);
    c.addEventListener('wheel',e=>{e.preventDefault();cam.z=Math.max(.3,Math.min(4,cam.z*(e.deltaY<0?1.1:.9)))},{passive:false});
    $('#gclose').onclick=close}
  raf=requestAnimationFrame(loop);
};

/* ---------- Buddy: a small helper that runs on simple rules on your device (no AI service) ---------- */
function say(h){const b=$('#bsay');if(b)b.innerHTML=h}
async function playQuery(q,label){say('Looking for '+P.esc(label||q)+'…');const r=await P.search(q);if(r===null){say("I can't reach a music server right now. Try again in a moment.");return}
  const v=P.visible(r);if(!v.length){say('I found nothing for that.');return}P.play(v[0],v);say('Playing <b>'+P.esc(v[0].title)+'</b> and more like it.')}
P.buddy=function(){
  const h=new Date().getHours(),greet=h<5?'Still up?':h<12?'Good morning!':h<17?'Good afternoon!':h<22?'Good evening!':'Late night vibes?';
  const lp=P.MOODQ();
  P.sheet(`<h3>${greet} I'm Buddy 🎧</h3><p class="sub" id="bsay">Tell me what to play, or tap one of these.</p>
  <form class="search" id="bf"><input id="bq" placeholder="Try: play chill songs · open library" autocomplete="off" aria-label="Ask Buddy"><button class="chip on">Go</button></form>
  <div class="chips" style="flex-wrap:wrap" id="bch">
   <button class="chip" data-a="time">Right for now</button><button class="chip" data-a="fav">My favourites</button><button class="chip" data-a="surprise">Surprise me</button>
   <button class="chip" data-a="galaxy">Open Galaxy</button><button class="chip" data-a="lib">Library</button><button class="chip" data-a="set">Settings</button></div>
  <div class="chips" style="flex-wrap:wrap">${P.MOODS.slice(0,6).map(m=>`<button class="chip" data-m="${P.esc(m[1])}" data-t="${P.esc(m[0])}">${m[0]}</button>`).join('')}</div>
  <p class="note">Buddy follows simple rules on your phone. It doesn't use an AI service and sends nothing anywhere except normal music searches.</p>`);
  const act=a=>{
    if(a==='time')playQuery(lp+' '+(h<11?'feel good morning':h<17?'focus chill':h<22?'evening relaxing':'late night slow')+' songs','something for now');
    else if(a==='fav'){if(!P.likes.length){say('You have no favourites yet. Tap ♡ in the player.');return}const l=P.shuffle(P.likes);P.play(l[0],l);say('Shuffling your favourites.')}
    else if(a==='surprise'){const t=P.topArtists();playQuery(t.length?'songs like '+P.shuffle(t)[0]:lp+' '+P.shuffle(['hits','new','popular'])[0],'a surprise')}
    else if(a==='galaxy'){P.closeSheet();PK.galaxy()}
    else if(a==='lib'){P.closeSheet();P.go('library')}else if(a==='set'){P.closeSheet();P.go('settings')}};
  $('#bch').onclick=e=>{const b=e.target.closest('[data-a]');if(b)act(b.dataset.a)};
  document.querySelectorAll('#sheet [data-m]').forEach(b=>b.onclick=()=>playQuery(lp+' '+b.dataset.m+' songs',b.dataset.t+' songs'));
  $('#bf').onsubmit=e=>{e.preventDefault();const q=$('#bq').value.trim().toLowerCase();if(!q)return;
    if(/^(open |go to |show )?(library|favou?rites)/.test(q)){P.closeSheet();P.go('library')}
    else if(/^(open |go to |show )?settings?/.test(q)){P.closeSheet();P.go('settings')}
    else if(/^(open |go to |show )?home/.test(q)){P.closeSheet();P.go('home')}
    else if(/galaxy/.test(q)){P.closeSheet();PK.galaxy()}
    else if(/^(search|find) /.test(q)){P.closeSheet();P.openSearch(q.replace(/^(search|find) /,''))}
    else if(/^shuffle/.test(q))act('fav');
    else playQuery(q.replace(/^play /,''))};
};
})();
