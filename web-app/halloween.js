/* Pattukal Web: Halloween Edition, Gesture Music, Mystery Capsule, Time Fold, liquid song-open effect.
   Same features as the Android app. Everything here only draws or calls the app's own functions (P.play, P.like, ...);
   it never touches the audio engine. Loaded after first paint. */
(function(){
"use strict";
const P=window.PK,S=P.S,$=s=>document.querySelector(s),TAU=Math.PI*2;
const rnd=(a=1)=>Math.random()*a;
const dpr=()=>Math.min(window.devicePixelRatio||1,2);
const reduced=()=>S.hwReduce||P.still();
const visible=()=>document.visibilityState==='visible';
const ORANGE='#ff7a1a',VIOLET='#9b5cff',SILVER='#dde3f0';
function seeded(n){let x=n*9301+49297;return()=>{x=(x*9301+49297)%233280;return x/233280}}

/* ------------------------------------------------------------------ Haunted background */
let bg=null,bctx=null,braf=0,bw=0,bh=0,last=0;
const stars=Array.from({length:70},()=>[rnd(),rnd()*.55,.5+rnd()*1.4,rnd()]);
const embers=Array.from({length:30},()=>[rnd(),rnd(),.4+rnd(),rnd()]);
const wisps=Array.from({length:9},()=>[rnd(),.45+rnd()*.45,rnd(),1+Math.floor(rnd()*3)]);
function sizeBg(){if(!bg)return;const d=dpr();bw=innerWidth;bh=innerHeight;bg.width=bw*d;bg.height=bh*d;bctx.setTransform(d,0,0,d,0,0)}
function glow(c,x,y,r,col,a){const g=c.createRadialGradient(x,y,0,x,y,r);g.addColorStop(0,col.replace('A',a));g.addColorStop(1,col.replace('A',0));c.fillStyle=g;c.beginPath();c.arc(x,y,r,0,TAU);c.fill()}
function drawBg(ts){
  const c=bctx,w=bw,h=bh,t=(ts/1000)%60,cyc=(n,p=0)=>Math.sin(TAU*(t/60*n+p)),loop=(n,p=0)=>((t/60*n)+p)%1;
  const lite=S.lowEnd||S.battery;
  let g=c.createLinearGradient(0,0,0,h);g.addColorStop(0,'#000');g.addColorStop(.35,'#080414');g.addColorStop(.7,'#14081e');g.addColorStop(1,'#220c1a');c.fillStyle=g;c.fillRect(0,0,w,h);
  stars.forEach(s=>{const tw=.5+.5*Math.sin(TAU*(t/60*(2+Math.floor(s[3]*5))+s[3]));c.fillStyle=`rgba(221,227,240,${.15+.5*tw*s[3]})`;c.beginPath();c.arc(s[0]*w,s[1]*h,s[2],0,TAU);c.fill()});
  const mx=w*.72,my=h*.17,mr=Math.min(w,520)*.19,halo=1+.05*cyc(2);
  glow(c,mx,my,mr*3.4*halo,'rgba(200,190,255,A)',.3);
  g=c.createRadialGradient(mx-mr*.3,my-mr*.3,0,mx,my,mr*1.5);g.addColorStop(0,'#fff3d6');g.addColorStop(.5,'#e6e8f4');g.addColorStop(1,'#b7bed6');c.globalAlpha=.6;c.fillStyle=g;c.beginPath();c.arc(mx,my,mr,0,TAU);c.fill();c.globalAlpha=1;
  [[-.35,-.2,.2],[.3,.25,.14],[-.1,.4,.1],[.35,-.35,.08]].forEach(k=>{c.fillStyle='rgba(141,148,174,.32)';c.beginPath();c.arc(mx+mr*k[0],my+mr*k[1],mr*k[2],0,TAU);c.fill()});
  for(let i=0;i<3;i++){const x=-w*.6+loop(1,i*.33)*w*2,y=h*(.1+i*.07);const gg=c.createLinearGradient(x,0,x+w*.9,0);gg.addColorStop(0,'rgba(42,24,64,0)');gg.addColorStop(.5,'rgba(42,24,64,.55)');gg.addColorStop(1,'rgba(42,24,64,0)');c.fillStyle=gg;c.beginPath();c.ellipse(x+w*.45,y,w*.45,h*.02,0,0,TAU);c.fill()}
  for(let b=0;b<3;b++){const p=loop(2+b%2,b*.31),bx=-60+p*(w+120),by=h*(.08+.07*b)+Math.sin(p*TAU*3+b)*20,fl=Math.sin(t*14+b*2)*7,sc=.8+b*.25;
    c.fillStyle='rgba(0,0,0,.92)';c.beginPath();c.moveTo(bx,by);c.quadraticCurveTo(bx-14*sc,by-12*sc-fl,bx-30*sc,by+2*sc);c.quadraticCurveTo(bx-17*sc,by-2*sc,bx-8*sc,by+8*sc);c.lineTo(bx,by+4*sc);c.lineTo(bx+8*sc,by+8*sc);c.quadraticCurveTo(bx+17*sc,by-2*sc,bx+30*sc,by+2*sc);c.quadraticCurveTo(bx+14*sc,by-12*sc-fl,bx,by);c.fill()}
  const ground=h*.93;c.fillStyle='#0b0615';c.beginPath();c.moveTo(0,ground);c.lineTo(0,h*.78);c.quadraticCurveTo(w*.15,h*.7,w*.3,h*.78);c.quadraticCurveTo(w*.45,h*.72,w*.6,h*.8);c.quadraticCurveTo(w*.8,h*.7,w,h*.78);c.lineTo(w,ground);c.fill();
  const sil='#030108',cx=w*.2,base=h*.82;c.fillStyle=sil;c.beginPath();c.moveTo(cx-w*.2,ground);c.lineTo(cx-w*.2,base);c.quadraticCurveTo(cx-w*.1,base-h*.02,cx-w*.07,base-h*.03);c.lineTo(cx-w*.07,base-h*.1);c.lineTo(cx-w*.055,base-h*.13);c.lineTo(cx-w*.04,base-h*.1);c.lineTo(cx-w*.04,base-h*.06);
  for(let x=cx-w*.04;x<cx+w*.07;x+=w*.024){c.lineTo(x,base-h*.06);c.lineTo(x,base-h*.075);c.lineTo(x+w*.012,base-h*.075);c.lineTo(x+w*.012,base-h*.06)}
  c.lineTo(cx+w*.07,base-h*.15);c.lineTo(cx+w*.085,base-h*.205);c.lineTo(cx+w*.1,base-h*.15);c.lineTo(cx+w*.1,base-h*.04);c.lineTo(cx+w*.2,base);c.lineTo(cx+w*.2,ground);c.fill();
  [[cx-w*.055,base-h*.085],[cx+w*.085,base-h*.125],[cx+w*.085,base-h*.09],[cx,base-h*.04]].forEach((o,i)=>{const fl=.6+.4*Math.sin(TAU*(t/60*(6+i)+i*.2));glow(c,o[0],o[1],14,'rgba(255,122,26,A)',.22*fl);c.fillStyle=`rgba(255,179,71,${.95*fl})`;c.beginPath();c.arc(o[0],o[1],4.2,0,TAU);c.fill()});
  for(let k=0;k<4;k++){const bx=w*(.55+k*.14),th=h*(.13+(k%3)*.035),sw=cyc(3+k%2,k*.2)*7;c.fillStyle=sil;c.beginPath();c.moveTo(bx-7,ground);c.quadraticCurveTo(bx-3+sw*.3,ground-th*.5,bx+sw,ground-th);c.quadraticCurveTo(bx+3+sw*.3,ground-th*.5,bx+7,ground);c.fill();
    c.strokeStyle=sil;c.lineWidth=5;c.lineCap='round';c.beginPath();c.moveTo(bx+sw*.7,ground-th*.75);c.quadraticCurveTo(bx-w*.04+sw,ground-th*.85,bx-w*.07+sw*1.3,ground-th*.78-20);c.moveTo(bx+sw*.6,ground-th*.6);c.quadraticCurveTo(bx+w*.04+sw,ground-th*.72,bx+w*.075+sw*1.3,ground-th*.62-26);c.moveTo(bx+sw,ground-th);c.quadraticCurveTo(bx+sw*1.3-10,ground-th-26,bx+sw*1.5-22,ground-th-44);c.moveTo(bx+sw,ground-th);c.quadraticCurveTo(bx+sw*1.3+12,ground-th-24,bx+sw*1.5+26,ground-th-40);c.stroke()}
  c.fillStyle=sil;c.fillRect(0,ground,w,h-ground);
  [[.08,1],[.46,.75],[.88,1.2]].forEach((p,i)=>{const px=w*p[0],py=ground+(h-ground)*.42,rr=Math.min(w,520)*.045*p[1],fl=.75+.25*Math.sin(TAU*(t/60*(9+i*2)+i));
    glow(c,px,py,rr*3.4,'rgba(255,122,26,A)',.4*fl);g=c.createRadialGradient(px,py,0,px,py,rr*1.3);g.addColorStop(0,'#ff9a2e');g.addColorStop(1,'#e2570c');c.fillStyle=g;c.beginPath();c.ellipse(px,py,rr*1.15,rr,0,0,TAU);c.fill();
    c.fillStyle='rgba(42,10,0,.95)';[-.45,.45].forEach(dx=>{c.beginPath();c.moveTo(px+rr*dx,py-rr*.55);c.lineTo(px+rr*(dx-.2),py-rr*.1);c.lineTo(px+rr*(dx+.2),py-rr*.1);c.fill()});
    c.beginPath();c.moveTo(px-rr*.7,py+rr*.25);for(let z=0;z<=6;z++)c.lineTo(px-rr*.7+z*rr*.233,py+rr*(z%2?.62:.25));c.lineTo(px+rr*.7,py+rr*.8);c.lineTo(px-rr*.7,py+rr*.8);c.fill();c.fillStyle='#3b5a1e';c.fillRect(px-rr*.08,py-rr*1.25,rr*.16,rr*.28)});
  for(let i=0;i<4;i++){const x=-w*.6+(i%2?1-loop(1,i*.25):loop(1,i*.25))*w*1.6,y=ground-h*(.02+i*.045),gg=c.createLinearGradient(x,0,x+w,0);gg.addColorStop(0,'rgba(185,167,232,0)');gg.addColorStop(.5,`rgba(185,167,232,${.11-i*.012})`);gg.addColorStop(1,'rgba(185,167,232,0)');c.fillStyle=gg;c.beginPath();c.ellipse(x+w/2,y+h*.04,w/2,h*.04,0,0,TAU);c.fill()}
  if(!lite)wisps.forEach(ws=>{const wx=(ws[0]+.06*Math.sin(TAU*(t/60*ws[3]+ws[2])))*w,wy=ws[1]*h+14*Math.sin(TAU*(t/60*(ws[3]+1)+ws[2]*2)),a=.35+.35*Math.sin(TAU*(t/60*(3+ws[3])+ws[2]));glow(c,wx,wy,26,'rgba(157,255,184,A)',.3*a);c.fillStyle=`rgba(216,255,228,${.85*a})`;c.beginPath();c.arc(wx,wy,2.2,0,TAU);c.fill()});
  embers.forEach(e=>{const ph=(t/60*(4+Math.floor(e[2]*6))+e[3])%1,ex=(e[0]+Math.sin((ph+e[3])*TAU)*.02)*w,ey=ground-ph*h*.75;c.fillStyle=(e[3]>.5?'rgba(255,122,26,':'rgba(155,92,255,')+(1-ph)*.7+')';c.beginPath();c.arc(ex,ey,1.5+e[2]*1.8,0,TAU);c.fill()});
}
function bgLoop(ts){braf=0;if(!bg||!visible())return;
  const gap=S.lowEnd||S.battery?66:33;if(ts-last>=gap){last=ts;drawBg(ts)}
  if(!reduced())braf=requestAnimationFrame(bgLoop)}
function bgStart(){if(!bg){bg=document.createElement('canvas');bg.id='hwbg';bg.setAttribute('aria-hidden','true');bg.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:0;pointer-events:none';document.body.prepend(bg);bctx=bg.getContext('2d');sizeBg();addEventListener('resize',sizeBg)}
  cancelAnimationFrame(braf);drawBg(reduced()?12000:performance.now());if(!reduced())braf=requestAnimationFrame(bgLoop)}
function bgStop(){cancelAnimationFrame(braf);braf=0;if(bg){bg.remove();bg=null;removeEventListener('resize',sizeBg)}}
document.addEventListener('visibilitychange',()=>{if(S.hw!=='off'&&bg&&visible()&&!braf&&!reduced())braf=requestAnimationFrame(bgLoop)});

/* ------------------------------------------------------------------ Sounds (generated, off by default) */
let ac=null,windN=null;
function audio(){if(!ac){try{ac=new(window.AudioContext||window.webkitAudioContext)()}catch(e){}}if(ac&&ac.state==='suspended')ac.resume().catch(()=>{});return ac}
function tone(freqs,dur,vol,type){if(!S.hwSound)return;const a=audio();if(!a)return;const g=a.createGain();g.gain.setValueAtTime(vol,a.currentTime);g.gain.exponentialRampToValueAtTime(.0001,a.currentTime+dur);g.connect(a.destination);
  freqs.forEach(f=>{const o=a.createOscillator();o.type=type||'sine';o.frequency.value=f;o.connect(g);o.start();o.stop(a.currentTime+dur)})}
const chime=()=>tone([880,1320,1760],.9,.06);
const portal=()=>{if(!S.hwSound)return;const a=audio();if(!a)return;const o=a.createOscillator(),g=a.createGain();o.frequency.setValueAtTime(140,a.currentTime);o.frequency.linearRampToValueAtTime(560,a.currentTime+.7);g.gain.setValueAtTime(.0001,a.currentTime);g.gain.linearRampToValueAtTime(.05,a.currentTime+.3);g.gain.linearRampToValueAtTime(.0001,a.currentTime+.7);o.connect(g);g.connect(a.destination);o.start();o.stop(a.currentTime+.75)};
function wind(on){if(!on||!S.hwSound||!visible()||P.getTab()!=='home'){if(windN){try{windN.stop()}catch(e){}windN=null}return}
  if(windN)return;const a=audio();if(!a)return;const n=a.sampleRate*2,buf=a.createBuffer(1,n,a.sampleRate),d=buf.getChannelData(0);let lp=0;for(let i=0;i<n;i++){lp+=.04*((Math.random()*2-1)-lp);d[i]=lp*3}
  const src=a.createBufferSource();src.buffer=buf;src.loop=true;const f=a.createBiquadFilter();f.type='lowpass';f.frequency.value=500;const g=a.createGain();g.gain.value=.03;src.connect(f);f.connect(g);g.connect(a.destination);src.start();windN={stop(){try{src.stop()}catch(e){}}}}

/* ------------------------------------------------------------------ Liquid song-open effect */
let lq=null,lraf=0,ptr={x:innerWidth/2,y:innerHeight/2,t:0};
addEventListener('pointerdown',e=>{ptr={x:e.clientX,y:e.clientY,t:performance.now()}},true);
function onOpen(){
  if(S.hw!=='off')portal();
  if(S.liquid===false||reduced()||performance.now()-ptr.t>2500||!visible())return;
  const d=dpr(),W=innerWidth,H=innerHeight;if(!lq){lq=document.createElement('canvas');lq.setAttribute('aria-hidden','true');lq.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:9998;pointer-events:none';document.body.appendChild(lq)}
  lq.width=W*d;lq.height=H*d;const c=lq.getContext('2d');c.setTransform(d,0,0,d,0,0);
  const o={x:ptr.x,y:ptr.y},maxR=Math.hypot(W,H)*.5,t0=performance.now(),dur=900;
  const cs=getComputedStyle(document.documentElement),c1=(cs.getPropertyValue('--acc')||ORANGE).trim()||ORANGE,c2=(cs.getPropertyValue('--acc2')||VIOLET).trim()||VIOLET;
  const target={x:W/2,y:H*.88},drops=Array.from({length:9},()=>({a:rnd(TAU),cx:(rnd()-.5)*160,r:5+rnd()*8}));
  const ease=x=>x<.5?2*x*x:1-Math.pow(-2*x+2,2)/2,al=(col,a)=>{c.globalAlpha=Math.max(0,Math.min(1,a));c.fillStyle=col;c.strokeStyle=col};
  cancelAnimationFrame(lraf);
  const step=now=>{const p=Math.min(1,(now-t0)/dur);c.clearRect(0,0,W,H);
    const grow=ease(Math.min(1,p/.55)),fade=1-Math.max(0,Math.min(1,(p-.35)/.65)),r=maxR*grow,tt=p*11;
    if(r>2){c.beginPath();for(let i=0;i<=56;i++){const a=i*TAU/56,rr=r*(1+.045*Math.sin(5*a+tt)+.03*Math.sin(3*a-1.7*tt)+.02*Math.sin(8*a+2.3*tt));const x=o.x+Math.cos(a)*rr,y=o.y+Math.sin(a)*rr;i?c.lineTo(x,y):c.moveTo(x,y)}c.closePath();
      const g=c.createRadialGradient(o.x,o.y,0,o.x,o.y,r);g.addColorStop(0,'rgba(255,255,255,0)');g.addColorStop(.7,'rgba(255,255,255,'+.08*fade+')');g.addColorStop(1,'rgba(180,170,255,'+.34*fade+')');c.globalAlpha=1;c.fillStyle=g;c.fill();
      c.lineWidth=3;c.strokeStyle='rgba(255,255,255,'+.55*fade+')';c.stroke();c.lineWidth=12;c.strokeStyle='rgba(180,170,255,'+.3*fade+')';c.stroke();
      c.lineWidth=2;c.strokeStyle='rgba(255,255,255,'+.22*fade+')';c.beginPath();c.arc(o.x,o.y,r*.72,0,TAU);c.stroke();c.lineWidth=5;c.strokeStyle='rgba(255,255,255,'+.35*fade+')';c.beginPath();c.arc(o.x,o.y,r*.92,3.5,4.4);c.stroke()}
    const flow=Math.max(0,Math.min(1,(p-.3)/.65)),e=ease(flow);
    if(flow>0){drops.forEach(dr=>{const sx=o.x+Math.cos(dr.a)*maxR*.55*grow,sy=o.y+Math.sin(dr.a)*maxR*.55*grow,cx=(sx+target.x)/2+dr.cx,cy=(sy+target.y)/2-80,u=e,
        x=(1-u)*(1-u)*sx+2*(1-u)*u*cx+u*u*target.x,y=(1-u)*(1-u)*sy+2*(1-u)*u*cy+u*u*target.y,rad=dr.r*(1-.6*e);
        c.globalAlpha=.4*(1-flow*.6);c.fillStyle='rgba(180,170,255,1)';c.beginPath();c.arc(x,y,rad*1.8,0,TAU);c.fill();c.globalAlpha=.75*(1-flow*.5);c.fillStyle='#fff';c.beginPath();c.arc(x,y,rad,0,TAU);c.fill()});
      const land=Math.max(0,Math.min(1,(flow-.55)/.45));if(land>0){c.globalAlpha=.35*(1-land);c.strokeStyle='rgba(180,170,255,1)';c.lineWidth=4;c.beginPath();c.arc(target.x,target.y,20+70*land,0,TAU);c.stroke()}}
    c.globalAlpha=1;if(p<1)lraf=requestAnimationFrame(step);else{c.clearRect(0,0,W,H);lq.remove();lq=null}};
  lraf=requestAnimationFrame(step)}

/* ------------------------------------------------------------------ Mystery Capsule: Trick or Track */
function songCard(s){return`<button class="card" data-sid="${P.esc(s.id)}"><img src="${P.thumb(s.id)}" referrerpolicy="no-referrer" loading="lazy" alt=""><b>${P.esc(s.title)}</b><i>${P.esc(s.artist)}</i></button>`}
function capsule(){
  const box=document.createElement('div');box.className='hwcard';
  let lang=null,busy=false,song=null;const seenIds=new Set(),pool=[];
  const accent=S.hw!=='off'?ORANGE:'var(--acc)';
  const draw=()=>{
    const langs=P.LANGS.filter(l=>(S.langs||[]).includes(l[0]));
    box.innerHTML=`<div class="hwhead"><b style="color:${accent};letter-spacing:.14em;font-size:11px">TRICK OR TRACK</b><span class="sub" style="margin:0">Mystery Capsule</span></div>
    <div class="chips"><button class="chip ${lang===null?'on':''}" data-l="">All languages</button>${langs.map(l=>`<button class="chip ${lang===l[0]?'on':''}" data-l="${l[0]}">${P.esc(l[1])}</button>`).join('')}</div>
    ${song?`<div class="hwrev"><img src="${P.thumb(song.id)}" referrerpolicy="no-referrer" alt=""><div><b>${P.esc(song.title)}</b><div class="sub" style="margin:2px 0 0">${P.esc(song.artist)}</div></div></div>
      <div class="hwbtns"><button class="chip on" data-a="play">Play</button><button class="chip" data-a="save">Save</button><button class="chip" data-a="more">Another</button></div>`
    :`<button class="hwcap" data-a="open" aria-label="Mystery capsule. Tap to open and reveal a song"><canvas width="160" height="160"></canvas><span>${busy?'Something stirs…':'Tap the capsule'}</span></button>`}`;
    const cv=box.querySelector('canvas');if(cv)capAnim(cv)};
  let craf=0,open=0;
  function capAnim(cv){cancelAnimationFrame(craf);const c=cv.getContext('2d');const t0=performance.now();
    const f=now=>{if(!cv.isConnected)return;const t=(now-t0)/1000,o=open,w=160,cx=80,cy=80,cw=70,ch=132,gap=o*18;c.clearRect(0,0,w,w);
      c.save();c.translate(cx,cy);c.rotate(reduced()?0:(busy?o*Math.PI:Math.sin(t*1.7)*.31));c.translate(-cx,-cy+(busy&&!reduced()?Math.sin(o*40)*2:0));
      const hw=S.hw!=='off',fillTop=cy-ch*.18;
      [-1,1].forEach(side=>{c.save();c.translate(0,side*gap);c.beginPath();c.rect(0,side<0?0:cy,w,side<0?cy:w);c.clip();
        rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.fillStyle='rgba(255,255,255,.07)';c.fill();
        c.save();rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.clip();c.beginPath();c.moveTo(cx-cw/2,fillTop);for(let i=0;i<=14;i++)c.lineTo(cx-cw/2+cw*i/14,fillTop+Math.sin(i/14*TAU*1.3+t*2.4)*4);c.lineTo(cx+cw/2,cy+ch/2);c.lineTo(cx-cw/2,cy+ch/2);c.closePath();
        const g=c.createLinearGradient(0,fillTop,0,cy+ch/2);g.addColorStop(0,hw?'rgba(155,92,255,.88)':'rgba(139,123,255,.88)');g.addColorStop(1,hw?'rgba(255,122,26,.8)':'rgba(255,111,177,.8)');c.fillStyle=g;c.fill();
        for(let i=0;i<5;i++){const ph=(t*(.4+i%3*.3)+i*.21)%1;c.fillStyle=`rgba(255,255,255,${.55*(1-ph)})`;c.beginPath();c.arc(cx-cw*.25+(i%3)*cw*.22,cy+ch*.42-ph*ch*.5,1.6+i%3,0,TAU);c.fill()}c.restore();
        c.strokeStyle='rgba(255,255,255,.55)';c.lineWidth=2;rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.stroke();
        c.strokeStyle='rgba(255,255,255,.5)';c.lineWidth=3;c.lineCap='round';c.beginPath();c.moveTo(cx-cw*.28,cy-ch*.38);c.lineTo(cx-cw*.28,cy+ch*.1);c.stroke();c.restore()});
      c.strokeStyle=hw?ORANGE:'#8b7bff';c.lineWidth=3+o*6;c.beginPath();c.moveTo(cx-cw/2-2,cy);c.lineTo(cx+cw/2+2,cy);c.stroke();c.restore();
      if(o>.2)for(let i=0;i<12;i++){const a=i*.5236+o*2,d=o*80;c.fillStyle=`rgba(155,92,255,${.9-o*.6})`;c.beginPath();c.arc(cx+Math.cos(a)*d,cy+Math.sin(a)*d,3+i%3*1.5,0,TAU);c.fill()}
      if(visible())craf=requestAnimationFrame(f)};craf=requestAnimationFrame(f)}
  function rr(c,x,y,w,h,r){c.beginPath();c.moveTo(x+r,y);c.arcTo(x+w,y,x+w,y+h,r);c.arcTo(x+w,y+h,x,y+h,r);c.arcTo(x,y+h,x,y,r);c.arcTo(x,y,x+w,y,r);c.closePath()}
  async function reveal(){
    if(busy)return;busy=true;song=null;draw();chime();
    const anim=new Promise(res=>{const t0=performance.now(),d=reduced()?200:900,st=n=>{open=Math.min(1,(n-t0)/d);if(open<1)requestAnimationFrame(st);else res()};requestAnimationFrame(st)});
    let res=null;if(pool.filter(x=>!seenIds.has(x.id)).length<1){const q=['hits','popular songs','melody','party songs','romantic songs'][Math.floor(rnd(5))];const L=lang?P.LANGS.find(l=>l[0]===lang):null;res=await P.search(`${L?L[2]:''} ${q}`.trim());if(res)pool.push(...res.filter(x=>x&&x.id&&!pool.some(p=>p.id===x.id)))}
    await anim;open=0;
    const next=pool.find(x=>!seenIds.has(x.id));busy=false;
    if(next){seenIds.add(next.id);song=next;draw()}
    else{box.innerHTML=`<div class="hwhead"><b style="color:${accent};letter-spacing:.14em;font-size:11px">TRICK OR TRACK</b></div><p class="note">${res===null?"The capsule can't reach the music servers. Check your connection and try again.":'No songs found right now. Try another language.'}</p><button class="chip on" data-a="open">Try again</button>`}}
  box.onclick=e=>{const b=e.target.closest('[data-l],[data-a]');if(!b)return;
    if(b.dataset.l!==undefined){lang=b.dataset.l||null;pool.length=0;seenIds.clear();song=null;draw();return}
    const a=b.dataset.a;if(a==='open'||a==='more')reveal();
    else if(a==='play'&&song)P.play(song,[song]);
    else if(a==='save'&&song){if(!P.likes.some(x=>x.id===song.id))P.like(song);P.toast('Saved to favourites');b.textContent='Saved'}};
  draw();return box}

/* ------------------------------------------------------------------ Time Fold: real listening history by day */
const PERIODS=['Today','Yesterday','Earlier this week','Older'];
function periodOf(ts){const d=new Date(ts),n=new Date(),a=new Date(d.getFullYear(),d.getMonth(),d.getDate()),b=new Date(n.getFullYear(),n.getMonth(),n.getDate()),days=Math.round((b-a)/864e5);return days<=0?0:days===1?1:days<7?2:3}
function fold(){
  const box=document.createElement('div');box.className='hwcard';
  box.innerHTML=`<button class="hwfoldbtn"><span class="hwglass" aria-hidden="true">⌛</span><span><b>Time Fold</b><span class="sub" style="display:block;margin:0">Explore what you played, by day</span></span></button>`;
  box.querySelector('button').onclick=openFold;return box}
function openFold(){
  const groups=PERIODS.map(()=>[]);const seenSet=new Set();
  P.hist.slice().reverse().forEach(h=>{if(!h||!h.id||!h.t)return;const p=periodOf(h.t),k=p+h.id;if(seenSet.has(k)||groups[p].length>=30)return;seenSet.add(k);groups[p].push(h)});
  const have=groups.map((g,i)=>g.length?i:-1).filter(i=>i>=0);
  let sel=have[0]??0,lang=null;
  P.sheet(`<h2 style="margin:0 0 6px">Time Fold</h2><div id="tfc"></div>`);
  const root=$('#sheet #tfc');
  const render=async()=>{
    const L=(S.langs||[]).map(c=>P.LANGS.find(l=>l[0]===c)).filter(Boolean);
    let tabs,songs,label;
    if(lang){const eras=[['Latest','new'],['2010s','2010s hits'],['2000s','2000s hits'],['90s','90s hits']];tabs=eras;const e=eras[Math.min(sel,3)];label=e[0];root.innerHTML=head(L,tabs.map(x=>x[0]),Math.min(sel,3))+'<p class="note">Loading songs through time…</p>';
      const l=P.LANGS.find(x=>x[0]===lang);const r=await P.sx(`${l[2]} ${e[1]}`);songs=(r||[]).slice(0,14)}
    else{tabs=have.map(i=>PERIODS[i]);songs=groups[sel]||[];label=PERIODS[sel]}
    const idx=lang?Math.min(sel,3):have.indexOf(sel);
    root.innerHTML=head(L,tabs.map?tabs.map(x=>Array.isArray(x)?x[0]:x):[],idx)+(songs.length?`<p class="sub">${songs.length} songs · swipe the cards</p><div class="hwflow">${songs.map(s=>`<button class="hwc" data-id="${P.esc(s.id)}"><img src="${P.thumb(s.id)}" referrerpolicy="no-referrer" alt=""><b>${P.esc(s.title)}</b><i>${P.esc(s.artist)}</i></button>`).join('')}</div>`:`<p class="note">${lang?'No songs found right now.':'No listening history yet. Play some songs and they will appear here.'}</p>`);
    const fl=root.querySelector('.hwflow');if(fl){const cards=[...fl.children],upd=()=>{const m=fl.getBoundingClientRect(),cx=m.left+m.width/2;cards.forEach(c=>{const r=c.getBoundingClientRect(),d=((r.left+r.width/2)-cx)/r.width;c.style.transform=`perspective(700px) rotateY(${Math.max(-60,Math.min(60,-d*38))}deg) scale(${1-.14*Math.min(Math.abs(d),1.6)})`;c.style.opacity=1-.28*Math.min(Math.abs(d),2)})};fl.onscroll=upd;requestAnimationFrame(upd);
      fl.onclick=e=>{const b=e.target.closest('.hwc');if(!b)return;const s=songs.find(x=>x.id===b.dataset.id);if(s){P.closeSheet();P.play(s,songs)}}}
    root.onclick=e=>{const b=e.target.closest('[data-p],[data-lg]');if(!b)return;if(b.dataset.lg!==undefined){lang=b.dataset.lg||null;sel=lang?0:(have[0]??0)}else sel=lang?+b.dataset.p:have[+b.dataset.p];render()}};
  const head=(L,names,idx)=>`<div class="chips"><button class="chip ${!lang?'on':''}" data-lg="">My history</button>${L.map(l=>`<button class="chip ${lang===l[0]?'on':''}" data-lg="${l[0]}">${P.esc(l[1])}</button>`).join('')}</div><div class="chips">${names.map((n,i)=>`<button class="chip ${i===idx?'on':''}" data-p="${i}">${P.esc(n)}</button>`).join('')}</div>`;
  render()}

/* ------------------------------------------------------------------ Haunted collections */
const COLL=[['Midnight Drive','midnight drive'],['Haunted Frequencies','dark ambient eerie'],['Dark Romance','dark romantic'],['Moonlight Chill','moonlight chill'],['Ghostly Echoes','echo reverb slow'],['After Midnight','after midnight'],['Halloween Mix','halloween spooky']];
async function collections(){
  const box=document.createElement('div');const lp=P.MOODQ();
  const pick=COLL.slice().sort(()=>Math.random()-.5).slice(0,4);
  const rows=await Promise.all(pick.map(async c=>[c[0],((await P.search(`${lp} ${c[1]}`))||[]).slice(0,14)]));
  const ok=rows.filter(r=>r[1].length>=4);if(!ok.length)return null;
  box.innerHTML=ok.map(r=>`<h2>${P.esc(r[0])}</h2><p class="sub">Halloween collection</p><div class="row" data-c="${P.esc(r[0])}">${r[1].map(songCard).join('')}</div>`).join('');
  box.onclick=e=>{const b=e.target.closest('.card');if(!b)return;const row=b.parentElement,r=ok.find(x=>x[0]===row.dataset.c);const s=r&&r[1].find(x=>x.id===b.dataset.sid);if(s)P.play(s,r[1])};return box}

/* ------------------------------------------------------------------ Gesture Music */
let gcv=null,gstrokes=[],gcur=[],gactive=false,gwriting=false,gmsg=null,gfade=0,gtimer=0,graf=0,suppressClick=0;
function recognise(raw){
  const K=2.75,pts=raw.map(p=>[p[0]*K,p[1]*K]);if(pts.length<12)return[null,false];
  const xs=pts.map(p=>p[0]),ys=pts.map(p=>p[1]),minX=Math.min(...xs),maxX=Math.max(...xs),minY=Math.min(...ys),maxY=Math.max(...ys),w=maxX-minX,h=maxY-minY;
  if(Math.max(w,h)<90)return[null,false];
  let len=0;for(let i=1;i<pts.length;i++)len+=Math.hypot(pts[i][0]-pts[i-1][0],pts[i][1]-pts[i-1][1]);
  const gap=Math.hypot(pts[0][0]-pts[pts.length-1][0],pts[0][1]-pts[pts.length-1][1]),diag=Math.hypot(w,h),straight=gap/len,closed=gap<diag*.35;
  let turns=0,ls=0;for(let i=2;i<pts.length;i+=2){const dy=pts[i][1]-pts[i-2][1],sg=dy>3?1:dy<-3?-1:0;if(sg&&ls&&sg!==ls)turns++;if(sg)ls=sg}
  if(closed&&w>160&&h>160&&w/h>.75&&w/h<1.5){const cx=(minX+maxX)/2,ce=pts.filter(p=>Math.abs(p[0]-cx)<w*.1),le=pts.filter(p=>p[0]<cx-w*.12),ri=pts.filter(p=>p[0]>cx+w*.12);
    if(ce.length&&le.length&&ri.length){const topC=Math.min(...ce.map(p=>p[1])),lobe=Math.min(Math.min(...le.map(p=>p[1])),Math.min(...ri.map(p=>p[1]))),bot=pts.reduce((a,b)=>b[1]>a[1]?b:a);if((topC-lobe)/h>.07&&Math.abs(bot[0]-cx)<w*.22)return['heart',true]}}
  if(closed&&w>200&&h>200&&w/h>.6&&w/h<1.7&&len>diag*2.2)return['circle',true];
  if(straight>.93&&w>250&&h>250&&Math.abs(w/h-1)<.4)return['diagonal',true];
  if(turns>=3&&w>h*1.4&&len>w*1.5)return['wave',turns>=4];
  return[null,false]}
function gsize(){if(!gcv)return;const d=dpr();gcv.width=innerWidth*d;gcv.height=innerHeight*d;gcv.getContext('2d').setTransform(d,0,0,d,0,0)}
function gdraw(ts){
  graf=0;if(!gcv)return;const c=gcv.getContext('2d'),W=innerWidth,H=innerHeight;c.clearRect(0,0,W,H);
  const al=gfade?Math.max(0,1-(ts-gfade)/600):1;if(al<=0){gclear();return}
  const t=(ts/900)%1,jit=seeded(Math.floor(t*12)+3),all=[...gstrokes,gcur];
  all.forEach(pts=>{if(pts.length<2)return;
    const path=(dx,dy)=>{c.beginPath();c.moveTo(pts[0][0]+dx,pts[0][1]+dy);for(let i=1;i<pts.length;i++)c.lineTo(pts[i][0]+dx,pts[i][1]+dy)};
    c.lineCap='round';c.lineJoin='round';c.globalAlpha=al;
    c.strokeStyle='rgba(255,43,214,.16)';c.lineWidth=26;path(0,0);c.stroke();
    const dx=3+jit()*5;c.strokeStyle='rgba(40,240,255,.75)';c.lineWidth=4;path(-dx,jit()*2);c.stroke();c.strokeStyle='rgba(255,43,214,.75)';path(dx,-jit()*2);c.stroke();
    if(jit()>.35){const ys=pts.map(p=>p[1]),b=Math.min(...ys),e=Math.max(...ys);for(let k=0;k<2;k++){const y0=b+jit()*Math.max(1,e-b);c.save();c.beginPath();c.rect(0,y0,W,10+jit()*16);c.clip();c.strokeStyle='rgba(255,255,255,.85)';c.lineWidth=4;path((jit()-.5)*46,0);c.stroke();c.restore()}}
    c.strokeStyle='#fff';c.lineWidth=2.5;path(0,0);c.stroke();
    if(S.gestGlitter!==false)for(let i=0;i<pts.length;i+=2){const r=seeded(i*7919+pts.length%40),tw=Math.abs(Math.sin(t*TAU+i*.7)),px=pts[i][0]+(r()-.5)*30,py=pts[i][1]+(r()-.5)*30,s=(2+r()*6)*(.4+tw)/2.2;
      c.fillStyle=['#fff','#ffe082',i%2?'#28f0ff':'#ff2bd6'][i%3];c.globalAlpha=al*(.35+.65*tw);c.beginPath();c.moveTo(px,py-s*1.6);c.lineTo(px+s*.32,py-s*.32);c.lineTo(px+s*1.6,py);c.lineTo(px+s*.32,py+s*.32);c.lineTo(px,py+s*1.6);c.lineTo(px-s*.32,py+s*.32);c.lineTo(px-s*1.6,py);c.lineTo(px-s*.32,py-s*.32);c.closePath();c.fill()}});
  c.globalAlpha=1;graf=requestAnimationFrame(gdraw)}
function gstart(){if(!gcv){gcv=document.createElement('canvas');gcv.setAttribute('aria-hidden','true');gcv.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:9999;pointer-events:none';document.body.appendChild(gcv);gsize()}gfade=0;if(!graf)graf=requestAnimationFrame(gdraw)}
function gclear(){gstrokes=[];gcur=[];gactive=false;gwriting=false;gfade=0;cancelAnimationFrame(graf);graf=0;if(gcv){gcv.remove();gcv=null}hideMsg()}
let mb=null;function say(text,end){gmsg=text;if(!mb){mb=document.createElement('div');mb.setAttribute('role','status');mb.style.cssText='position:fixed;top:calc(env(safe-area-inset-top) + 70px);left:50%;transform:translateX(-50%);z-index:10000;background:rgba(0,0,0,.78);color:#fff;padding:10px 18px;border-radius:30px;font-weight:600;font-size:14px;pointer-events:none;max-width:86vw;text-align:center';document.body.appendChild(mb)}mb.textContent=text;
  clearTimeout(gtimer);if(end){gtimer=setTimeout(()=>{gfade=performance.now();setTimeout(gclear,650)},end)}}
function hideMsg(){if(mb){mb.remove();mb=null}gmsg=null}
function writePrompt(){
  hideMsg();P.sheet(`<h2 style="margin:0 0 6px">Play a song by name</h2><p class="note">Browsers can't read handwriting, so type the song name and it plays.</p><input id="gwin" class="in" style="width:100%;padding:12px;border-radius:14px;background:var(--glass);border:1px solid var(--line)" placeholder="Song name" autocomplete="off"><div class="hwbtns" style="margin-top:12px"><button class="chip on" id="gwgo">Play</button></div><p class="note" id="gwst"></p>`);
  const go=async()=>{const q=$('#gwin').value.trim();if(!q)return;$('#gwst').textContent=`Finding “${q}”…`;const r=await P.search(q);if(r===null){$('#gwst').textContent="Can't reach the music servers.";return}if(!r.length){$('#gwst').textContent='No song found.';return}P.closeSheet();P.play(r[0],[r[0]]);P.toast('Playing '+r[0].title)};
  $('#gwgo').onclick=go;$('#gwin').onkeydown=e=>{if(e.key==='Enter')go()};setTimeout(()=>$('#gwin')&&$('#gwin').focus(),50)}
function act(shape){
  if(shape==='circle'){P.go('library');say('Favorites',1300)}
  else if(shape==='wave'){P.go('home');say('Recently played',1300);setTimeout(()=>{const e=$('#h_recent');if(e)e.scrollIntoView({behavior:'smooth'})},400)}
  else if(shape==='diagonal'){P.setShuffle(!P.shuf());say(P.shuf()?'Shuffle on':'Shuffle off',1300)}
  else if(shape==='heart'){const c=P.getCur();if(!c)say('Nothing is playing',1300);else if(P.likes.some(x=>x.id===c.id))say('Already in Favorites ♥',1300);else{P.like(c);say('Liked ♥',1300)}}}
function endStroke(){
  gstrokes.push(gcur);const first=gstrokes.length===1;
  if(first){const[sh,sure]=recognise(gcur);if(sh&&sure){gcur=[];act(sh);return}}
  gcur=[];say('Not recognised. Try a circle, wave, diagonal or heart',1600);
  clearTimeout(gtimer);gtimer=setTimeout(()=>{gfade=performance.now();setTimeout(()=>{gclear();writePrompt()},650)},1500);gwriting=true;
  say('Pause to type a song name instead',1e9)}
/* double tap, then hold */
let d1=null,d2=null,holdT=0,tapUp=null;const SLOP=10;
function gestureInit(){
  const opt={capture:true,passive:true};
  addEventListener('pointerdown',e=>{
    if(S.gest===false||e.pointerType==='mouse'&&e.button!==0)return;
    if(gactive){e.preventDefault();return}
    if(gwriting){clearTimeout(gtimer);gwriting=false;begin(e,true);return}
    const now=performance.now();
    if(tapUp&&now-tapUp.t<280&&Math.hypot(e.clientX-tapUp.x,e.clientY-tapUp.y)<80){d2={x:e.clientX,y:e.clientY,id:e.pointerId};tapUp=null;clearTimeout(holdT);holdT=setTimeout(()=>{if(d2){begin(e,false)}},360)}
    else{d1={x:e.clientX,y:e.clientY,t:now,id:e.pointerId};tapUp=null}
  },opt);
  addEventListener('pointermove',e=>{
    if(gactive){gcur.push([e.clientX,e.clientY]);return}
    if(d2&&Math.hypot(e.clientX-d2.x,e.clientY-d2.y)>SLOP*1.2){d2=null;clearTimeout(holdT)}
    if(d1&&Math.hypot(e.clientX-d1.x,e.clientY-d1.y)>SLOP*2)d1=null;
  },opt);
  const up=e=>{
    if(gactive){if(gcur.length>1){suppressClick=performance.now();endStroke()}else gclear();gactive=false;return}
    if(d2){d2=null;clearTimeout(holdT);return}
    if(d1&&performance.now()-d1.t<220){tapUp={x:d1.x,y:d1.y,t:performance.now()}}d1=null};
  addEventListener('pointerup',up,opt);addEventListener('pointercancel',()=>{if(gactive)gclear();d1=d2=null;clearTimeout(holdT)},opt);
  addEventListener('touchmove',e=>{if(gactive)e.preventDefault()},{passive:false,capture:true});
  addEventListener('contextmenu',e=>{if(gactive||d2)e.preventDefault()},true);
  addEventListener('click',e=>{if(performance.now()-suppressClick<500){e.stopPropagation();e.preventDefault()}},true);
  function begin(e,more){
    d2=null;gactive=true;if(!more){gstrokes=[];gcur=[]}gcur=[[e.clientX,e.clientY]];
    try{navigator.vibrate&&navigator.vibrate(20)}catch(x){}
    try{document.activeElement&&document.activeElement.blur&&0}catch(x){}
    gstart();if(!more)say('Draw a circle, wave, diagonal, heart, or pause for typing',1e9);else say('Keep drawing…',1e9)}
}
function manual(){
  P.sheet(`<h2 style="margin:0 0 8px">✨ Gesture Music</h2>
  <p>1. <b>Double tap, and keep your finger down on the second tap.</b> You'll feel a buzz (where supported) and a glittering glitch trail follows your finger.</p>
  <p style="margin:8px 0 2px"><b>2. Draw:</b></p>
  <p style="margin:2px 0">○ Big circle: open Favorites</p><p style="margin:2px 0">〰 Wave, left to right: Recently played</p><p style="margin:2px 0">╱ Big diagonal line: shuffle on / off</p><p style="margin:2px 0">♥ Heart: like the song that is playing</p><p style="margin:2px 0">Aa Anything else: pause and type a song name to play it</p>
  <p class="note">Start the double tap on an empty spot, not on a song. Draw big. Normal taps and scrolling are never affected. Turn it off or change it in Settings.</p>
  <div class="hwbtns"><button class="chip on" id="gmok">Got it</button></div>`);
  const b=$('#gmok');if(b)b.onclick=()=>{S.gestSeen=true;P.save();P.closeSheet()}}

/* ------------------------------------------------------------------ apply / init */
const CSS=`html.hw{background:#000}html.hw body{background:transparent}html.hw[data-theme]{--bg:#000;--bg2:#0b0614}
html.hw #app{position:relative;z-index:1}
.hwcard{margin:12px 0;padding:14px;border-radius:22px;background:rgba(0,0,0,.45);border:1px solid var(--line);backdrop-filter:blur(var(--blur,18px))}
html.hw .hwcard{border-color:rgba(255,122,26,.45)}
.hwhead{display:flex;justify-content:space-between;align-items:center;margin-bottom:6px}
.hwcap{display:flex;flex-direction:column;align-items:center;gap:4px;width:100%;padding:6px 0;color:var(--mut);font-size:12px}.hwcap canvas{width:120px;height:120px}
.hwrev{display:flex;gap:12px;align-items:center;margin:10px 0}.hwrev img{width:92px;height:92px;border-radius:18px;object-fit:cover}
.hwbtns{display:flex;gap:8px;flex-wrap:wrap}
.hwfoldbtn{display:flex;align-items:center;gap:12px;width:100%;text-align:left}.hwglass{font-size:26px}
.hwflow{display:flex;overflow-x:auto;scroll-snap-type:x mandatory;padding:12px 70px 16px;gap:0;scrollbar-width:none;margin:0 -6px}
.hwc{flex:0 0 130px;margin-right:-14px;scroll-snap-align:center;text-align:left;transition:transform .12s linear;will-change:transform}
.hwc img{width:130px;height:130px;border-radius:18px;object-fit:cover;border:1px solid var(--line)}.hwc b,.hwc i{display:block;font-size:12px;font-style:normal;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:4px}.hwc i{color:var(--mut);font-size:11px;margin-top:0}
html.still .hwc{transition:none}`;
function apply(){
  if(!$('#hwcss')){const s=document.createElement('style');s.id='hwcss';s.textContent=CSS;document.head.appendChild(s)}
  const on=S.hw!=='off';document.documentElement.classList.toggle('hw',on);
  if(on)bgStart();else bgStop();
  if(!S.hwSound)wind(false);
  if(S.gest===false&&gactive)gclear()}
let inited=false;
function init(){apply();if(!inited){inited=true;gestureInit();addEventListener('pointerdown',()=>{if(S.hwSound)wind(true)},{once:false,passive:true});
  setTimeout(()=>{if(S.gest!==false&&!S.gestSeen&&(S.langs&&S.langs.length))manual()},4000)}}
P.hw={init,apply,capsule,fold,collections,onOpen,manual,wind,writePrompt};
})();
