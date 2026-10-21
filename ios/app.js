/* Pattukal Full Web Music App */
(function () {
  'use strict';

  // Demo catalog (public domain / free samples so it actually plays)
  const CATALOG = [
    { id: '1', title: 'Acoustic Breeze', artist: 'Benjamin Tissot', album: 'Bensound', duration: 154, url: 'https://www.bensound.com/bensound-music/bensound-acousticbreeze.mp3', art: '' },
    { id: '2', title: 'Sunny', artist: 'Benjamin Tissot', album: 'Bensound', duration: 142, url: 'https://www.bensound.com/bensound-music/bensound-sunny.mp3', art: '' },
    { id: '3', title: 'Creative Minds', artist: 'Benjamin Tissot', album: 'Bensound', duration: 146, url: 'https://www.bensound.com/bensound-music/bensound-creativeminds.mp3', art: '' },
    { id: '4', title: 'Energy', artist: 'Benjamin Tissot', album: 'Bensound', duration: 179, url: 'https://www.bensound.com/bensound-music/bensound-energy.mp3', art: '' },
    { id: '5', title: 'Tenderness', artist: 'Benjamin Tissot', album: 'Bensound', duration: 146, url: 'https://www.bensound.com/bensound-music/bensound-tenderness.mp3', art: '' },
    { id: '6', title: 'Once Again', artist: 'Benjamin Tissot', album: 'Bensound', duration: 152, url: 'https://www.bensound.com/bensound-music/bensound-onceagain.mp3', art: '' },
    { id: '7', title: 'Memories', artist: 'Benjamin Tissot', album: 'Bensound', duration: 213, url: 'https://www.bensound.com/bensound-music/bensound-memories.mp3', art: '' },
    { id: '8', title: 'Cute', artist: 'Benjamin Tissot', album: 'Bensound', duration: 149, url: 'https://www.bensound.com/bensound-music/bensound-cute.mp3', art: '' },
    { id: '9', title: 'Jazzy Frenchy', artist: 'Benjamin Tissot', album: 'Bensound', duration: 132, url: 'https://www.bensound.com/bensound-music/bensound-jazzyfrenchy.mp3', art: '' },
    { id: '10', title: 'Love', artist: 'Benjamin Tissot', album: 'Bensound', duration: 179, url: 'https://www.bensound.com/bensound-music/bensound-love.mp3', art: '' },
  ];

  // State
  let queue = [...CATALOG];
  let index = 0;
  let isPlaying = false;
  let shuffle = false;
  let repeat = false;
  let liked = JSON.parse(localStorage.getItem('pattukal_liked') || '[]');
  let recent = JSON.parse(localStorage.getItem('pattukal_recent') || '[]');

  const audio = document.getElementById('audio');
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => document.querySelectorAll(sel);

  // ---------- Helpers ----------
  function fmt(s) {
    if (!s || isNaN(s)) return '0:00';
    const m = Math.floor(s / 60);
    const sec = Math.floor(s % 60);
    return m + ':' + String(sec).padStart(2, '0');
  }

  function saveLiked() {
    localStorage.setItem('pattukal_liked', JSON.stringify(liked));
  }
  function saveRecent() {
    localStorage.setItem('pattukal_recent', JSON.stringify(recent.slice(0, 50)));
  }

  function current() {
    return queue[index] || null;
  }

  // ---------- Render ----------
  function songRow(song, extra = '') {
    const isLiked = liked.includes(song.id);
    const playing = current() && current().id === song.id && isPlaying;
    return `
      <button class="song-row ${playing ? 'playing' : ''}" data-id="${song.id}">
        <div class="song-art">${song.art ? `<img src="${song.art}" alt="">` : '♬'}</div>
        <div class="song-meta">
          <div class="song-title">${song.title}</div>
          <div class="song-artist">${song.artist}</div>
        </div>
        <span class="song-like ${isLiked ? 'liked' : ''}" data-like="${song.id}">${isLiked ? '♥' : '♡'}</span>
      </button>`;
  }

  function card(song) {
    return `
      <div class="card" data-id="${song.id}">
        <div class="card-art">♬</div>
        <div class="card-info">
          <div class="card-title">${song.title}</div>
          <div class="card-sub">${song.artist}</div>
        </div>
      </div>`;
  }

  function renderHome() {
    $('#quickMix').innerHTML = CATALOG.slice(0, 6).map(card).join('');
    $('#trendingList').innerHTML = CATALOG.slice(0, 5).map(s => songRow(s)).join('');
    $('#forYouList').innerHTML = CATALOG.slice(5).map(s => songRow(s)).join('');
  }

  function renderSearch(q) {
    const results = q
      ? CATALOG.filter(s =>
          s.title.toLowerCase().includes(q) ||
          s.artist.toLowerCase().includes(q) ||
          s.album.toLowerCase().includes(q))
      : [];
    $('#searchResults').innerHTML = results.map(s => songRow(s)).join('');
    $('#searchEmpty').style.display = results.length || !q ? 'none' : 'block';
    if (!q) $('#searchEmpty').style.display = 'block';
  }

  function renderLibrary(tab = 'liked') {
    let list = [];
    if (tab === 'liked') list = CATALOG.filter(s => liked.includes(s.id));
    else if (tab === 'recent') list = recent.map(id => CATALOG.find(s => s.id === id)).filter(Boolean);
    else list = []; // playlists placeholder

    $('#libraryList').innerHTML = list.map(s => songRow(s)).join('');
    $('#libraryEmpty').style.display = list.length ? 'none' : 'block';
  }

  function updatePlayerUI() {
    const song = current();
    if (!song) return;

    $('#playerTitle').textContent = song.title;
    $('#playerArtist').textContent = song.artist;
    $('#miniTitle').textContent = song.title;
    $('#miniArtist').textContent = song.artist;

    const art = song.art;
    if (art) {
      $('#playerArt').src = art;
      $('#playerArt').classList.add('show');
      $('#miniArt').src = art;
    } else {
      $('#playerArt').classList.remove('show');
      $('#miniArt').src = '';
    }

    $('#btnPlay').textContent = isPlaying ? '⏸' : '▶';
    $('#miniPlay').textContent = isPlaying ? '⏸' : '▶';
    $('#btnLike').textContent = liked.includes(song.id) ? '♥' : '♡';
    $('#btnLike').style.color = liked.includes(song.id) ? '#ff6b9d' : '';

    $('#miniPlayer').classList.toggle('hidden', !song);

    // Media Session
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: song.title,
        artist: song.artist,
        album: song.album || 'Pattukal',
      });
      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
    }
  }

  // ---------- Playback ----------
  function playSong(id, fromQueue = false) {
    const song = CATALOG.find(s => s.id === id);
    if (!song) return;

    if (!fromQueue) {
      queue = [...CATALOG];
      index = queue.findIndex(s => s.id === id);
    }

    audio.src = song.url;
    audio.play().then(() => {
      isPlaying = true;
      updatePlayerUI();
      // recent
      recent = [id, ...recent.filter(x => x !== id)];
      saveRecent();
    }).catch(err => {
      console.warn('Play failed', err);
      alert('Could not play this track. Check network or try another.');
    });
  }

  function togglePlay() {
    if (!current()) {
      playSong(CATALOG[0].id);
      return;
    }
    if (isPlaying) {
      audio.pause();
      isPlaying = false;
    } else {
      audio.play();
      isPlaying = true;
    }
    updatePlayerUI();
  }

  function next() {
    if (!queue.length) return;
    if (shuffle) {
      index = Math.floor(Math.random() * queue.length);
    } else {
      index = (index + 1) % queue.length;
    }
    playSong(queue[index].id, true);
  }

  function prev() {
    if (audio.currentTime > 3) {
      audio.currentTime = 0;
      return;
    }
    index = (index - 1 + queue.length) % queue.length;
    playSong(queue[index].id, true);
  }

  // ---------- Events ----------
  // Navigation
  $$('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const screen = btn.dataset.screen;
      $$('.nav-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      $$('.screen').forEach(s => s.classList.remove('active'));
      $('#' + screen).classList.add('active');
      if (screen === 'library') renderLibrary($('.lib-tab.active')?.dataset.tab || 'liked');
    });
  });

  // Song clicks (event delegation)
  document.addEventListener('click', (e) => {
    const row = e.target.closest('[data-id]');
    if (row && !e.target.closest('[data-like]')) {
      playSong(row.dataset.id);
      // open full player on mobile feel
      if (window.innerWidth < 600) {
        $$('.screen').forEach(s => s.classList.remove('active'));
        $('#player').classList.add('active');
      }
    }
    const likeBtn = e.target.closest('[data-like]');
    if (likeBtn) {
      e.stopPropagation();
      const id = likeBtn.dataset.like;
      if (liked.includes(id)) liked = liked.filter(x => x !== id);
      else liked.push(id);
      saveLiked();
      likeBtn.classList.toggle('liked');
      likeBtn.textContent = liked.includes(id) ? '♥' : '♡';
      updatePlayerUI();
    }
  });

  // Search
  $('#searchInput').addEventListener('input', (e) => {
    renderSearch(e.target.value.trim().toLowerCase());
  });

  // Library tabs
  $$('.lib-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      $$('.lib-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderLibrary(tab.dataset.tab);
    });
  });

  // Player controls
  $('#btnPlay').addEventListener('click', togglePlay);
  $('#miniPlay').addEventListener('click', (e) => { e.stopPropagation(); togglePlay(); });
  $('#btnNext').addEventListener('click', next);
  $('#miniNext').addEventListener('click', (e) => { e.stopPropagation(); next(); });
  $('#btnPrev').addEventListener('click', prev);
  $('#btnShuffle').addEventListener('click', () => {
    shuffle = !shuffle;
    $('#btnShuffle').style.color = shuffle ? 'var(--cyan)' : '';
  });
  $('#btnRepeat').addEventListener('click', () => {
    repeat = !repeat;
    $('#btnRepeat').style.color = repeat ? 'var(--cyan)' : '';
  });
  $('#btnLike').addEventListener('click', () => {
    const song = current();
    if (!song) return;
    if (liked.includes(song.id)) liked = liked.filter(x => x !== song.id);
    else liked.push(song.id);
    saveLiked();
    updatePlayerUI();
  });

  $('#miniPlayer').addEventListener('click', () => {
    $$('.screen').forEach(s => s.classList.remove('active'));
    $('#player').classList.add('active');
  });
  $('#closePlayer').addEventListener('click', () => {
    $('#player').classList.remove('active');
    $('#home').classList.add('active');
    $$('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.screen === 'home'));
  });

  // Progress
  audio.addEventListener('timeupdate', () => {
    if (!audio.duration) return;
    const pct = (audio.currentTime / audio.duration) * 1000;
    $('#progress').value = pct;
    $('#currentTime').textContent = fmt(audio.currentTime);
    $('#duration').textContent = fmt(audio.duration);
  });
  $('#progress').addEventListener('input', () => {
    if (!audio.duration) return;
    audio.currentTime = ($('#progress').value / 1000) * audio.duration;
  });

  audio.addEventListener('ended', () => {
    if (repeat) {
      audio.currentTime = 0;
      audio.play();
    } else next();
  });
  audio.addEventListener('play', () => { isPlaying = true; updatePlayerUI(); });
  audio.addEventListener('pause', () => { isPlaying = false; updatePlayerUI(); });

  // Media Session actions
  if ('mediaSession' in navigator) {
    navigator.mediaSession.setActionHandler('play', () => { audio.play(); });
    navigator.mediaSession.setActionHandler('pause', () => { audio.pause(); });
    navigator.mediaSession.setActionHandler('previoustrack', prev);
    navigator.mediaSession.setActionHandler('nexttrack', next);
  }

  // Service Worker
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }

  // Init
  renderHome();
  renderLibrary();
  updatePlayerUI();
})();
