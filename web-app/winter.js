/* Pattukal Web: Winter Edition (aurora Home, overlay effects, water fill), Gesture Music, Mystery Capsule, Time Fold, liquid song-open effect.
   Same features as the Android app. Everything here only draws or calls the app's own functions (P.play, P.like, ...);
   it never touches the audio engine. Loaded after first paint. */
(function(){
"use strict";
const P=window.PK,S=P.S,$=s=>document.querySelector(s),TAU=Math.PI*2;
const rnd=(a=1)=>Math.random()*a;
const dpr=()=>Math.min(window.devicePixelRatio||1,2);
const reduced=()=>S.hwReduce||P.still();
const visible=()=>document.visibilityState==='visible';
function seeded(n){let x=n*9301+49297;return()=>{x=(x*9301+49297)%233280;return x/233280}}

/* ------------------------------------------------------------------ Winter palettes (same as the Android app) */
const VARS={
 frozen:{seed:'#58b8ff',sky:['#030a1c','#0a2347','#14406e'],b:['#7fe3ff','#8ab6ff','#bff3ff']},
 aurora:{seed:'#35e0b5',sky:['#020b16','#062235','#0b3b3f'],b:['#3dffb4','#4db8ff','#b070ff']},
 midnight:{seed:'#4a63ff',sky:['#01030d','#060c27','#0e1745'],b:['#5b7bff','#8f6bff','#4dd0ff']},
 snowwhite:{seed:'#d6eeff',sky:['#0e1b33','#1d3358','#39608f'],b:['#ffffff','#d6eeff','#b9dcff']},
 christmas:{seed:'#e0394f',sky:['#07130f','#0f2a1e','#3a1018'],b:['#ff5468','#3fd07a','#ffe08a']},
 newyear:{seed:'#ffc94d',sky:['#05060f','#14123a','#2d1b4f'],b:['#ffd36b','#ff7bd0','#7bd8ff']}};
const V=()=>VARS[S.wvar]||VARS.midnight;
const rgba=(hex,a)=>{const n=parseInt(hex.slice(1),16);return `rgba(${n>>16},${n>>8&255},${n&255},${a})`};
const lite=()=>!!(S.lowEnd||S.battery||S.wlow);
const winterOn=()=>S.winterOn!==false;

/* ------------------------------------------------------------------ Winter background (aurora, stars, snowy hills) */
let bg=null,bctx=null,braf=0,bw=0,bh=0,last=0;
const stars=Array.from({length:80},()=>[rnd(),rnd()*.6,.5+rnd()*1.3,rnd()]);
const pines=Array.from({length:14},(_,i)=>[i/13+(rnd()-.5)*.05,.7+rnd()*.6]);
function sizeBg(){if(!bg)return;const d=Math.min(dpr(),1.5);bw=innerWidth;bh=innerHeight;bg.width=bw*d;bg.height=bh*d;bctx.setTransform(d,0,0,d,0,0);drawBg(performance.now())}
function glow(c,x,y,r,col,a){const g=c.createRadialGradient(x,y,0,x,y,r);g.addColorStop(0,col.replace('A',a));g.addColorStop(1,col.replace('A',0));c.fillStyle=g;c.beginPath();c.arc(x,y,r,0,TAU);c.fill()}
function drawBg(ts){
  const c=bctx,w=bw,h=bh,v=V(),t=lite()?14:ts/1000;
  let g=c.createLinearGradient(0,0,0,h);g.addColorStop(0,v.sky[0]);g.addColorStop(.55,v.sky[1]);g.addColorStop(1,v.sky[2]);c.fillStyle=g;c.fillRect(0,0,w,h);
  stars.forEach(s=>{const tw=.5+.5*Math.sin(t*(.6+s[3]*1.6)+s[3]*9);c.fillStyle=`rgba(235,243,255,${.12+.55*tw*s[3]})`;c.beginPath();c.arc(s[0]*w,s[1]*h,s[2],0,TAU);c.fill()});
  /* aurora ribbons */
  c.globalCompositeOperation='lighter';
  v.b.forEach((col,b)=>{
    const base=h*(.17+b*.085),amp=h*(.03+.012*b),k=(1.2+b*.55)/w*TAU,H=h*.4;
    const gr=c.createLinearGradient(0,0,0,H);gr.addColorStop(0,rgba(col,0));gr.addColorStop(.4,rgba(col,.2));gr.addColorStop(1,rgba(col,0));c.fillStyle=gr;
    for(let x=0;x<w+4;x+=4){const y=base+Math.sin(x*k+t*(.32+b*.1)+b*2)*amp+Math.sin(x*k*2.4-t*.21+b)*amp*.5-H*.3;c.save();c.translate(x,Math.round(y));c.fillRect(0,0,4,H);c.restore()}});
  c.globalCompositeOperation='source-over';
  /* moon */
  const mx=w*.78,my=h*.13,mr=Math.min(w,520)*.05;glow(c,mx,my,mr*5,'rgba(215,232,255,A)',.22);c.fillStyle='rgba(238,246,255,.82)';c.beginPath();c.arc(mx,my,mr,0,TAU);c.fill();
  /* snowy hills + pines */
  const hill=(base,amp,off,fill,cap)=>{c.beginPath();c.moveTo(0,h);for(let x=0;x<=w+10;x+=10){c.lineTo(x,h*base+Math.sin(x/w*TAU*1.3+off)*h*amp+Math.sin(x/w*TAU*3.1+off*2)*h*amp*.3)}c.lineTo(w,h);c.closePath();c.fillStyle=fill;c.fill();c.strokeStyle=cap;c.lineWidth=2;c.stroke()};
  hill(.8,.028,1.2,rgba(v.sky[1],.96),'rgba(220,235,255,.22)');
  const ph=h*.045;pines.forEach(p=>{const x=p[0]*w,y=h*.835+Math.sin(x/w*TAU*1.3+2.4)*h*.03,s=p[1];c.fillStyle='rgba(2,6,16,.9)';for(let i=0;i<3;i++){const yy=y-ph*s*i*.7;c.beginPath();c.moveTo(x,yy-ph*s*1.2);c.lineTo(x-ph*s*(.8-i*.13),yy);c.lineTo(x+ph*s*(.8-i*.13),yy);c.fill()}c.fillRect(x-1.5,y,3,ph*s*.3)});
  hill(.9,.022,3.4,rgba(v.sky[0],.98),'rgba(235,245,255,.3)');
  /* a warm cabin window glow */
  const wx=w*.2,wy=h*.915,fl=.8+.2*Math.sin(t*3);glow(c,wx,wy,26,'rgba(255,190,90,A)',.32*fl);c.fillStyle=`rgba(255,205,120,${.9*fl})`;c.fillRect(wx-5,wy-4,10,8);
}
function bgLoop(ts){braf=0;if(!bg||!visible())return;
  const gap=S.lowEnd||S.battery?66:40;if(ts-last>=gap){last=ts;drawBg(ts)}
  if(!reduced()&&!lite())braf=requestAnimationFrame(bgLoop)}
function bgStart(){if(!bg){bg=document.createElement('canvas');bg.id='hwbg';bg.setAttribute('aria-hidden','true');bg.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:0;pointer-events:none';document.body.prepend(bg);bctx=bg.getContext('2d');sizeBg();addEventListener('resize',sizeBg)}
  cancelAnimationFrame(braf);braf=0;drawBg(reduced()?14000:performance.now());if(!reduced()&&!lite())braf=requestAnimationFrame(bgLoop)}
function bgStop(){cancelAnimationFrame(braf);braf=0;if(bg){bg.remove();bg=null;removeEventListener('resize',sizeBg)}}
document.addEventListener('visibilitychange',()=>{if(visible()){if(winterOn()&&bg&&!braf&&!reduced()&&!lite())braf=requestAnimationFrame(bgLoop);fxKick();wkick()}});

/* ------------------------------------------------------------------ Overlay effects (snow, fire sparks, leaves, spin, water drops) */
let fx=null,fctx,fraf=0,fw=0,fh=0,flast=0,parts=[],ripples=[],px=-999,py=-999,fsig='';
const LEAF=['#c0562a','#e0902f','#a8412a','#d9b04a'];
function fxCount(){const max=lite()?70:210,k=Math.max(1,S.ovInt||6)/100;let n=Math.round(max*(.12+.88*k));if((S.ovFx||'snow')==='snow'&&S.ovMode==='storm')n=Math.round(n*1.8);return Math.max(4,n)}
function mkP(init){const k=S.ovFx||'snow',storm=k==='snow'&&S.ovMode==='storm';
  const p={x:rnd(fw),y:init?rnd(fh):-14,r:1,vx:0,vy:0,ph:rnd(TAU),a:.35+rnd(.55),rot:rnd(TAU),vr:(rnd()-.5)*.06,c:rnd()};
  if(k==='snow'){p.r=1+rnd(2.6);p.vy=(.3+rnd(.8))*(storm?2.4:1)*(.55+p.r/4);p.vx=storm?-1.1-rnd(1.4):0;if(storm)p.x=rnd(fw+200)}
  else if(k==='fire'){p.y=init?rnd(fh):fh+10;p.r=1+rnd(2.2);p.vy=-(.5+rnd(1.5));p.vx=(rnd()-.5)*.5}
  else if(k==='leaves'){p.r=5+rnd(5);p.vy=.5+rnd(.9);p.c=Math.floor(rnd(4))}
  else if(k==='spin'){p.r=4+rnd(7);p.vy=.25+rnd(.5);p.vr=(rnd()<.5?-1:1)*(.03+rnd(.07))}
  else{p.r=1.1+rnd(1.5);p.vy=3.2+rnd(4)}
  return p}
function fxSize(){if(!fx)return;const d=Math.min(dpr(),1.5);fw=innerWidth;fh=innerHeight;fx.width=fw*d;fx.height=fh*d;fctx.setTransform(d,0,0,d,0,0)}
function fxFrame(ts){fraf=0;if(!fx||!visible())return;
  if(reduced()||S.ovOn===false){fctx.clearRect(0,0,fw,fh);return}
  const gap=lite()?33:16;if(ts-flast<gap){fraf=requestAnimationFrame(fxFrame);return}flast=ts;
  const k=S.ovFx||'snow',v=V(),n=fxCount(),touch=S.wtouch!==false,sig=k+(S.ovMode||'')+n;
  if(sig!==fsig){fsig=sig;parts=Array.from({length:n},()=>mkP(true));ripples=[]}
  const c=fctx;c.clearRect(0,0,fw,fh);
  for(let i=0;i<parts.length;i++){const p=parts[i];
    if(touch&&px>-100){const dx=p.x-px,dy=p.y-py,d=Math.hypot(dx,dy);if(d<110&&d>.1){const f=(1-d/110)*3.4;p.x+=dx/d*f;p.y+=dy/d*f}}
    if(k==='snow'){p.ph+=.02;p.x+=p.vx+Math.sin(p.ph)*.4;p.y+=p.vy;if(p.y>fh+8||p.x<-20||p.x>fw+20){Object.assign(p,mkP(false));if(p.vx<0)p.x=rnd(fw+200)}
      c.fillStyle=`rgba(255,255,255,${p.a})`;c.beginPath();c.arc(p.x,p.y,p.r,0,TAU);c.fill()}
    else if(k==='fire'){p.ph+=.05;p.x+=p.vx+Math.sin(p.ph)*.5;p.y+=p.vy;p.a-=.0035;if(p.a<=0||p.y<-10)Object.assign(p,mkP(false),{y:fh+10,a:.4+rnd(.55)});
      const col=`255,${(130+p.c*110)|0},${(30+p.c*50)|0}`;c.fillStyle=`rgba(${col},${p.a*.22})`;c.beginPath();c.arc(p.x,p.y,p.r*3.2,0,TAU);c.fill();c.fillStyle=`rgba(${col},${Math.min(1,p.a+.2)})`;c.beginPath();c.arc(p.x,p.y,p.r,0,TAU);c.fill()}
    else if(k==='leaves'){p.ph+=.03;p.x+=Math.sin(p.ph)*1.1;p.y+=p.vy;p.rot+=.03;if(p.y>fh+12)Object.assign(p,mkP(false));
      c.save();c.translate(p.x,p.y);c.rotate(p.rot);c.fillStyle=LEAF[p.c];c.globalAlpha=.85;c.beginPath();c.ellipse(0,0,p.r,p.r*.45,0,0,TAU);c.fill();c.globalAlpha=1;c.restore()}
    else if(k==='spin'){p.ph+=.015;p.x+=Math.sin(p.ph)*.5;p.y+=p.vy;p.rot+=p.vr;if(p.y>fh+12)Object.assign(p,mkP(false));
      c.save();c.translate(p.x,p.y);c.rotate(p.rot);c.strokeStyle=rgba(v.b[(p.c*3|0)%3],p.a+.15);c.lineWidth=1.4;c.beginPath();for(let a=0;a<4;a++){c.moveTo(0,0);c.lineTo(Math.cos(a*Math.PI/2)*p.r,Math.sin(a*Math.PI/2)*p.r)}c.stroke();c.restore()}
    else{p.y+=p.vy;if(p.y>fh*(.86+p.c*.1)){if(ripples.length<14)ripples.push({x:p.x,y:p.y,r:0,a:.5});Object.assign(p,mkP(false))}
      c.strokeStyle=`rgba(150,210,255,${p.a*.7})`;c.lineWidth=p.r;c.lineCap='round';c.beginPath();c.moveTo(p.x,p.y-p.vy*2.6);c.lineTo(p.x,p.y);c.stroke()}}
  if(k==='water'){ripples=ripples.filter(r=>r.a>.02);ripples.forEach(r=>{r.r+=.9;r.a*=.94;c.strokeStyle=`rgba(170,220,255,${r.a})`;c.lineWidth=1.2;c.beginPath();c.ellipse(r.x,r.y,r.r*1.6,r.r*.5,0,0,TAU);c.stroke()})}
  fraf=requestAnimationFrame(fxFrame)}
function fxKick(){if(fx&&!fraf&&visible())fraf=requestAnimationFrame(fxFrame)}
function fxStart(){if(!fx){fx=document.createElement('canvas');fx.id='wfx';fx.setAttribute('aria-hidden','true');fx.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:9000;pointer-events:none';document.body.appendChild(fx);fctx=fx.getContext('2d');fxSize();addEventListener('resize',fxSize)}fxKick()}
function fxStop(){cancelAnimationFrame(fraf);fraf=0;if(fx){fx.remove();fx=null;fsig='';parts=[];removeEventListener('resize',fxSize)}}
addEventListener('pointermove',e=>{px=e.clientX;py=e.clientY},{passive:true});
['pointerup','pointercancel'].forEach(t=>addEventListener(t,e=>{if(e.pointerType!=='mouse'){px=-999;py=-999}},{passive:true}));
document.addEventListener('pointerleave',()=>{px=-999;py=-999});

/* Frost edges */
function frost(on){let f=$('#wfrost');if(on&&!f){f=document.createElement('div');f.id='wfrost';f.setAttribute('aria-hidden','true');document.body.appendChild(f)}else if(!on&&f)f.remove()}

/* ------------------------------------------------------------------ Water fill: pull the cord at the top of Home */
let wcv=null,wctx,wraf=0,wlevel=0,wtarget=0,wcord=null,wtimer=0;
const waterOK=()=>winterOn()&&S.water!==false&&P.getTab()==='home';
function wsize(){if(!wcv)return;const d=Math.min(dpr(),1.5);wcv.width=innerWidth*d;wcv.height=innerHeight*d;wctx.setTransform(d,0,0,d,0,0)}
function wdraw(ts){wraf=0;if(!wcv)return;
  const dt=wtarget>wlevel?.012:.02;wlevel+=(wtarget-wlevel)*dt*2.2+(wtarget>wlevel?.0008:-.0008);if(Math.abs(wtarget-wlevel)<.003)wlevel=wtarget;
  const W=innerWidth,H=innerHeight,c=wctx;c.clearRect(0,0,W,H);
  if(wlevel<=0&&wtarget<=0){wcv.remove();wcv=null;removeEventListener('resize',wsize);document.documentElement.classList.remove('wfill');return}
  const top=H*(1-wlevel*.62),t=ts/1000,still=reduced()||lite();
  const g=c.createLinearGradient(0,top,0,H);g.addColorStop(0,'rgba(70,170,255,.34)');g.addColorStop(1,'rgba(8,40,110,.62)');
  for(let l=0;l<2;l++){c.beginPath();c.moveTo(0,H);for(let x=0;x<=W+12;x+=12){c.lineTo(x,top+(l?8:0)+(still?0:Math.sin(x/W*TAU*(1.4+l*.6)+t*(1.6+l*.8)+l)*(10-l*3)))}c.lineTo(W,H);c.closePath();c.fillStyle=l?'rgba(40,120,220,.18)':g;c.fill()}
  c.strokeStyle='rgba(200,235,255,.45)';c.lineWidth=1.5;c.beginPath();for(let x=0;x<=W+12;x+=12){const y=top+(still?0:Math.sin(x/W*TAU*1.4+t*1.6)*10);x?c.lineTo(x,y):c.moveTo(x,y)}c.stroke();
  wraf=requestAnimationFrame(wdraw)}
function wkick(){if(wcv&&!wraf&&visible())wraf=requestAnimationFrame(wdraw)}
function setWater(on){wtarget=on?1:0;
  if(on&&!wcv){wcv=document.createElement('canvas');wcv.id='wwater';wcv.setAttribute('aria-hidden','true');wcv.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:8000;pointer-events:none';document.body.appendChild(wcv);wctx=wcv.getContext('2d');wsize();addEventListener('resize',wsize)}
  document.documentElement.classList.toggle('wfill',on);wkick()}
function cordSync(){
  const show=waterOK();
  if(show&&!wcord){wcord=document.createElement('button');wcord.id='wcord';wcord.type='button';wcord.setAttribute('aria-label','Pull the cord to fill the screen with water');wcord.innerHTML='<i></i><b></b>';document.body.appendChild(wcord);cordInit(wcord)}
  else if(!show&&wcord){wcord.remove();wcord=null;if(wtarget)setWater(false)}
  if(!winterOn()||S.water===false){if(wtarget)setWater(false)}}
function cordInit(el){let y0=0,dy=0,drag=false;const knob=el.querySelector('b'),line=el.querySelector('i');
  const set=d=>{line.style.height=(34+d)+'px';knob.style.transform=`translateY(${d}px)`};
  el.addEventListener('pointerdown',e=>{drag=true;y0=e.clientY;dy=0;el.setPointerCapture(e.pointerId);e.preventDefault()});
  el.addEventListener('pointermove',e=>{if(!drag)return;dy=Math.max(0,Math.min(80,e.clientY-y0));set(dy)});
  const end=()=>{if(!drag)return;drag=false;const pulled=dy>36||dy<4;set(0);if(pulled){setWater(wtarget<.5);if(S.hwSound)tone([392,523],.5,.05,'sine')}};
  el.addEventListener('pointerup',end);el.addEventListener('pointercancel',()=>{drag=false;set(0)})}
function wstart(){if(!wtimer)wtimer=setInterval(cordSync,500);cordSync()}
function wstop(){clearInterval(wtimer);wtimer=0;if(wcord){wcord.remove();wcord=null}if(wtarget)setWater(false)}

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
  if(winterOn())portal();
  if(S.liquid===false||reduced()||performance.now()-ptr.t>2500||!visible())return;
  const d=dpr(),W=innerWidth,H=innerHeight;if(!lq){lq=document.createElement('canvas');lq.setAttribute('aria-hidden','true');lq.style.cssText='position:fixed;inset:0;width:100%;height:100%;z-index:9998;pointer-events:none';document.body.appendChild(lq)}
  lq.width=W*d;lq.height=H*d;const c=lq.getContext('2d');c.setTransform(d,0,0,d,0,0);
  const o={x:ptr.x,y:ptr.y},maxR=Math.hypot(W,H)*.5,t0=performance.now(),dur=900;
  const cs=getComputedStyle(document.documentElement),c1=(cs.getPropertyValue('--acc')||V().b[0]).trim()||V().b[0],c2=(cs.getPropertyValue('--acc2')||V().b[1]).trim()||V().b[1];
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
  const accent=winterOn()?V().b[0]:'var(--acc)';
  const draw=()=>{
    const langs=P.LANGS.filter(l=>(S.langs||[]).includes(l[0]));
    box.innerHTML=`<div class="hwhead"><b style="color:${accent};letter-spacing:.14em;font-size:11px">MYSTERY SONG</b><span class="sub" style="margin:0">Mystery Capsule</span></div>
    <div class="chips"><button class="chip ${lang===null?'on':''}" data-l="">All languages</button>${langs.map(l=>`<button class="chip ${lang===l[0]?'on':''}" data-l="${l[0]}">${P.esc(l[1])}</button>`).join('')}</div>
    ${song?`<div class="hwrev"><img src="${P.thumb(song.id)}" referrerpolicy="no-referrer" alt=""><div><b>${P.esc(song.title)}</b><div class="sub" style="margin:2px 0 0">${P.esc(song.artist)}</div></div></div>
      <div class="hwbtns"><button class="chip on" data-a="play">Play</button><button class="chip" data-a="save">Save</button><button class="chip" data-a="more">Another</button></div>`
    :`<button class="hwcap" data-a="open" aria-label="Mystery capsule. Tap to open and reveal a song"><canvas width="160" height="160"></canvas><span>${busy?'Something stirs…':'Tap the capsule'}</span></button>`}`;
    const cv=box.querySelector('canvas');if(cv)capAnim(cv)};
  let craf=0,open=0;
  function capAnim(cv){cancelAnimationFrame(craf);const c=cv.getContext('2d');const t0=performance.now();
    const f=now=>{if(!cv.isConnected)return;const t=(now-t0)/1000,o=open,w=160,cx=80,cy=80,cw=70,ch=132,gap=o*18;c.clearRect(0,0,w,w);
      c.save();c.translate(cx,cy);c.rotate(reduced()?0:(busy?o*Math.PI:Math.sin(t*1.7)*.31));c.translate(-cx,-cy+(busy&&!reduced()?Math.sin(o*40)*2:0));
      const hw=winterOn(),fillTop=cy-ch*.18;
      [-1,1].forEach(side=>{c.save();c.translate(0,side*gap);c.beginPath();c.rect(0,side<0?0:cy,w,side<0?cy:w);c.clip();
        rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.fillStyle='rgba(255,255,255,.07)';c.fill();
        c.save();rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.clip();c.beginPath();c.moveTo(cx-cw/2,fillTop);for(let i=0;i<=14;i++)c.lineTo(cx-cw/2+cw*i/14,fillTop+Math.sin(i/14*TAU*1.3+t*2.4)*4);c.lineTo(cx+cw/2,cy+ch/2);c.lineTo(cx-cw/2,cy+ch/2);c.closePath();
        const g=c.createLinearGradient(0,fillTop,0,cy+ch/2);g.addColorStop(0,hw?rgba(V().b[1],.88):'rgba(139,123,255,.88)');g.addColorStop(1,hw?rgba(V().b[2],.8):'rgba(255,111,177,.8)');c.fillStyle=g;c.fill();
        for(let i=0;i<5;i++){const ph=(t*(.4+i%3*.3)+i*.21)%1;c.fillStyle=`rgba(255,255,255,${.55*(1-ph)})`;c.beginPath();c.arc(cx-cw*.25+(i%3)*cw*.22,cy+ch*.42-ph*ch*.5,1.6+i%3,0,TAU);c.fill()}c.restore();
        c.strokeStyle='rgba(255,255,255,.55)';c.lineWidth=2;rr(c,cx-cw/2,cy-ch/2,cw,ch,35);c.stroke();
        c.strokeStyle='rgba(255,255,255,.5)';c.lineWidth=3;c.lineCap='round';c.beginPath();c.moveTo(cx-cw*.28,cy-ch*.38);c.lineTo(cx-cw*.28,cy+ch*.1);c.stroke();c.restore()});
      c.strokeStyle=hw?V().b[0]:'#8b7bff';c.lineWidth=3+o*6;c.beginPath();c.moveTo(cx-cw/2-2,cy);c.lineTo(cx+cw/2+2,cy);c.stroke();c.restore();
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
    else{box.innerHTML=`<div class="hwhead"><b style="color:${accent};letter-spacing:.14em;font-size:11px">MYSTERY SONG</b></div><p class="note">${res===null?"The capsule can't reach the music servers. Check your connection and try again.":'No songs found right now. Try another language.'}</p><button class="chip on" data-a="open">Try again</button>`}}
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

/* ------------------------------------------------------------------ Winter collections */
const COLL=[['Winter Chill','winter chill songs'],['Snowfall Acoustic','acoustic calm snowfall'],['Cozy Cabin','cozy lofi'],['Frozen Lofi','lofi chill beats'],['Aurora Nights','ambient night calm'],['After Midnight','after midnight'],['New Year Party','new year party songs']];
async function collections(){
  const box=document.createElement('div');const lp=P.MOODQ();
  const pick=COLL.slice().sort(()=>Math.random()-.5).slice(0,4);
  const rows=await Promise.all(pick.map(async c=>[c[0],((await P.search(`${lp} ${c[1]}`))||[]).slice(0,14)]));
  const ok=rows.filter(r=>r[1].length>=4);if(!ok.length)return null;
  box.innerHTML=ok.map(r=>`<h2>${P.esc(r[0])}</h2><p class="sub">Winter collection</p><div class="row" data-c="${P.esc(r[0])}">${r[1].map(songCard).join('')}</div>`).join('');
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
const CSS=`html.hw{background:var(--wbg)}html.hw body{background:transparent}html.hw[data-theme]{--bg:var(--wbg);--bg2:var(--wbg2);--acc:var(--wseed);--acc2:var(--wb1);--fg:#f3f6ff;--mut:#a9b6d6;--onacc:#06101f;--glass:rgba(255,255,255,.08);--line:rgba(255,255,255,.14)}
html.hw #app{position:relative;z-index:1}
.hwcard{margin:12px 0;padding:14px;border-radius:22px;background:rgba(0,0,0,.45);border:1px solid var(--line);backdrop-filter:blur(var(--blur,18px))}
html.hw .hwcard{border-color:var(--wb0)}
#wfrost{position:fixed;inset:0;z-index:8990;pointer-events:none;box-shadow:inset 0 0 70px 6px rgba(190,225,255,.3),inset 0 0 170px rgba(120,180,255,.14)}
#wcord{position:fixed;top:0;left:50%;margin-left:-22px;width:44px;padding:0;border:0;background:none;z-index:8200;touch-action:none;cursor:grab;display:flex;flex-direction:column;align-items:center}
#wcord i{display:block;width:2px;height:34px;background:linear-gradient(#e8f3ff,#9cc4ff);border-radius:2px;transition:height .25s}#wcord b{width:20px;height:20px;border-radius:50%;margin-top:-1px;background:radial-gradient(circle at 35% 30%,#fff,#8fc2ff 60%,#4b83e0);box-shadow:0 0 14px rgba(120,190,255,.7);transition:transform .25s}
@keyframes wbob{50%{transform:translateY(-7px) rotate(1.2deg)}}
html.wfill .row .card img{animation:wbob 3.6s ease-in-out infinite}html.wfill .row .card:nth-child(2n) img{animation-delay:-1.3s}html.wfill .row .card:nth-child(3n) img{animation-delay:-2.4s}
html.still.wfill .row .card img{animation:none}
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
  const on=winterOn(),v=V(),r=document.documentElement;
  r.style.setProperty('--wbg',v.sky[0]);r.style.setProperty('--wbg2',v.sky[1]);r.style.setProperty('--wseed',v.seed);r.style.setProperty('--wb0',v.b[0]);r.style.setProperty('--wb1',v.b[1]);
  r.classList.toggle('hw',on);
  if(on)bgStart();else bgStop();
  if(S.ovOn!==false)fxStart();else fxStop();
  frost(on&&!!S.frost);
  if(on&&S.water!==false)wstart();else wstop();
  const tc=document.querySelector('meta[name=theme-color]');if(tc&&on)tc.content=v.sky[0];
  if(!S.hwSound)wind(false);
  if(S.gest===false&&gactive)gclear()}
let inited=false;
function init(){apply();if(!inited){inited=true;gestureInit();addEventListener('pointerdown',()=>{if(S.hwSound)wind(true)},{once:false,passive:true});
  setTimeout(()=>{if(S.gest!==false&&!S.gestSeen&&(S.langs&&S.langs.length))manual()},4000)}}
P.hw={init,apply,capsule,fold,collections,onOpen,manual,wind,writePrompt};
})();
