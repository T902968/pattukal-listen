/* Pattukal – Real YouTube Music Web Player */
(function () {
  'use strict';

  // Public Piped instances (rotate if one is down)
  const PIPED_INSTANCES = [
    'https://pipedapi.kavin.rocks',
    'https://pipedapi.tokhmi.xyz',
    'https://pipedapi.syncpundit.io',
    'https://api-piped.mha.fi',
    'https://piped-api.privacy.com.de'
  ];
  let currentInstance = 0;

  // State
  let queue = [];
  let index = 0;
  let isPlaying = false;
  let shuffle = false;
  let repeat = false;
  let liked = JSON.parse(localStorage.getItem('pattukal_liked') || '[]');
  let recent = JSON.parse(localStorage.getItem('pattukal_recent') || '[]');
  let searchResults = [];
  let trending = [];

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
    localStorage.setItem('pattukal_recent', JSON.stringify(recent.slice(0, 40)));
  }

  function current() {
    return queue[index] || null;
  }

  function getInstance() {
    return PIPED_INSTANCES[currentInstance % PIPED_INSTANCES.length];
  }

  function nextInstance() {
    currentInstance = (currentInstance + 1) % PIPED_INSTANCES.length;
  }

  async function pipedFetch(path, retries = 3) {
    for (let i = 0; i < retries; i++) {
      try {
        const url = getInstance() + path;
        const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!res.ok) throw new Error(res.status);
        return await res.json();
      } catch (e) {
        console.warn('Piped instance failed, trying next…', e);
        nextInstance();
      }
    }
    throw new Error('All Piped instances failed');
  }

  // ---------- API ----------
  async function searchYoutube(query) {
    if (!query || query.length < 2) return [];
    try {
      const data = await pipedFetch('/search?q=' + encodeURIComponent(query) + '&filter=music_songs');
      // Piped returns items; filter to streams/songs
      const items = (data.items || data).filter(i => i.type === 'stream' || i.url);
      return items.slice(0, 30).map(normalizeSong);
    } catch (e) {
      console.error('Search failed', e);
      return [];
    }
  }

  async function getTrending() {
    try {
      // Use a popular music search as “trending” fallback
      const data = await pipedFetch('/search?q=top+music+hits&filter=music_songs');
      const items = (data.items || data).filter(i => i.type === 'stream' || i.url);
      return items.slice(0, 15).map(normalizeSong);
    } catch (e) {
      console.error('Trending failed', e);
      return [];
    }
  }

  async function getStreamUrl(videoId) {
    try {
      const data = await pipedFetch('/streams/' + videoId);
      // Prefer highest audio-only stream
      const audioStreams = (data.audioStreams || []).sort((a, b) => (b.bitrate || 0) - (a.bitrate || 0));
      if (audioStreams.length) return audioStreams[0].url;
      // Fallback to any stream
      if (data.videoStreams && data.videoStreams.length) {
        return data.videoStreams[0].url;
      }
      return null;
    } catch (e) {
      console.error('Stream fetch failed', e);
      return null;
    }
  }

  function normalizeSong(item) {
    const id = item.videoId || (item.url || '').replace(/.*v=/, '').replace(/.*\//, '') || item.id;
    return {
      id: id,
      title: item.title || item.name || 'Unknown',
      artist: item.uploaderName || item.uploader || item.artist || 'Unknown',
      album: item.album || '',
      duration: item.duration || 0,
      art: item.thumbnail || (item.thumbnails && item.thumbnails[0]?.url) || '',
      url: null // resolved on play
    };
  }

  // ---------- Render ----------
  function songRow(song) {
    const isLiked = liked.includes(song.id);
    const playing = current() && current().id === song.id && isPlaying;
    const artHtml = song.art
      ? `<img class="song-art" src="${song.art}" alt="" loading="lazy">`
      : `<div class="song-art">♬</div>`;
    return `
      <button class="song-row ${playing ? 'playing' : ''}" data-id="${song.id}">
        ${artHtml}
        <div class="song-meta">
          <div class="song-title">${escapeHtml(song.title)}</div>
          <div class="song-artist">${escapeHtml(song.artist)}</div>
        </div>
        <span class="song-like ${isLiked ? 'liked' : ''}" data-like="${song.id}">${isLiked ? '♥' : '♡'}</span>
      </button>`;
  }

  function card(song) {
    const artHtml = song.art
      ? `<img class="card-art-img" src="${song.art}" alt="" loading="lazy">`
      : '♬';
    return `
      <div class="card" data-id="${song.id}">
        <div class="card-art">${artHtml}</div>
        <div class="card-info">
          <div class="card-title">${escapeHtml(song.title)}</div>
          <div class="card-sub">${escapeHtml(song.artist)}</div>
        </div>
      </div>`;
  }

  function escapeHtml(str) {
    const d = document.createElement('div');
    d.textContent = str || '';
    return d.innerHTML;
  }

  function renderHome() {
    if (trending.length) {
      $('#quickMix').innerHTML = trending.slice(0, 8).map(card).join('');
      $('#trendingList').innerHTML = trending.slice(0, 8).map(s => songRow(s)).join('');
      $('#forYouList').innerHTML = trending.slice(8).map(s => songRow(s)).join('');
    } else {
      $('#trendingList').innerHTML = '<div class="empty-state">Loading music…</div>';
    }
  }

  function renderSearch(results) {
    searchResults = results;
    $('#searchResults').innerHTML = results.map(s => songRow(s)).join('');
    $('#searchEmpty').style.display = results.length ? 'none' : 'block';
    if (!results.length) $('#searchEmpty').textContent = 'No results. Try another search.';
  }

  function renderLibrary(tab = 'liked') {
    let list = [];
    if (tab === 'liked') {
      // We only store IDs; show recent liked that we still have in memory or just IDs
      list = recent.map(id => {
        const fromSearch = searchResults.find(s => s.id === id) || trending.find(s => s.id === id);
        return fromSearch || { id, title: 'Liked song', artist: '', art: '' };
      }).filter(s => liked.includes(s.id));
      // Also add pure liked IDs not in recent
      liked.forEach(id => {
        if (!list.find(s => s.id === id)) {
          list.push({ id, title: 'Liked song', artist: 'Open from search to restore', art: '' });
        }
      });
    } else if (tab === 'recent') {
      list = recent.map(id => {
        return searchResults.find(s => s.id === id) ||
               trending.find(s => s.id === id) ||
               queue.find(s => s.id === id) ||
               { id, title: 'Recently played', artist: '', art: '' };
      });
    }
    $('#libraryList').innerHTML = list.length ? list.map(s => songRow(s)).join('') : '';
    $('#libraryEmpty').style.display = list.length ? 'none' : 'block';
  }

  function updatePlayerUI() {
    const song = current();
    if (!song) {
      $('#miniPlayer').classList.add('hidden');
      return;
    }

    $('#playerTitle').textContent = song.title;
    $('#playerArtist').textContent = song.artist;
    $('#miniTitle').textContent = song.title;
    $('#miniArtist').textContent = song.artist;

    if (song.art) {
      $('#playerArt').src = song.art;
      $('#playerArt').classList.add('show');
      $('#miniArt').src = song.art;
      $('#artPlaceholder').style.display = 'none';
    } else {
      $('#playerArt').classList.remove('show');
      $('#miniArt').src = '';
      $('#artPlaceholder').style.display = 'grid';
    }

    $('#btnPlay').textContent = isPlaying ? '⏸' : '▶';
    $('#miniPlay').textContent = isPlaying ? '⏸' : '▶';
    $('#btnLike').textContent = liked.includes(song.id) ? '♥' : '♡';
    $('#btnLike').style.color = liked.includes(song.id) ? '#ff6b9d' : '';

    $('#miniPlayer').classList.remove('hidden');

    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: song.title,
        artist: song.artist,
        album: song.album || 'Pattukal',
        artwork: song.art ? [{ src: song.art, sizes: '512x512', type: 'image/jpeg' }] : []
      });
      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
    }
  }

  // ---------- Playback ----------
  async function playSong(id, fromQueue = false) {
    let song = queue.find(s => s.id === id) ||
               searchResults.find(s => s.id === id) ||
               trending.find(s => s.id === id);

    if (!song) {
      // minimal stub
      song = { id, title: 'Loading…', artist: '', art: '', url: null };
    }

    if (!fromQueue) {
      // Build queue from current context
      if (searchResults.length && searchResults.some(s => s.id === id)) {
        queue = [...searchResults];
      } else if (trending.length) {
        queue = [...trending];
      } else {
        queue = [song];
      }
      index = Math.max(0, queue.findIndex(s => s.id === id));
    } else {
      index = queue.findIndex(s => s.id === id);
      if (index < 0) {
        queue.push(song);
        index = queue.length - 1;
      }
    }

    // Show UI immediately
    isPlaying = false;
    updatePlayerUI();
    $('#playerTitle').textContent = song.title + ' (loading…)';

    // Resolve real stream URL
    const streamUrl = await getStreamUrl(id);
    if (!streamUrl) {
      alert('Could not get stream. Trying another server…');
      nextInstance();
      const retry = await getStreamUrl(id);
      if (!retry) {
        alert('Playback failed. Try another song or check network.');
        return;
      }
      song.url = retry;
    } else {
      song.url = streamUrl;
    }

    // Update queue entry
    queue[index] = { ...song };

    audio.src = song.url;
    try {
      await audio.play();
      isPlaying = true;
      recent = [id, ...recent.filter(x => x !== id)];
      saveRecent();
      updatePlayerUI();
    } catch (err) {
      console.warn('Play error', err);
      alert('Could not play this track. The stream may be blocked or the instance is down.');
      isPlaying = false;
      updatePlayerUI();
    }
  }

  function togglePlay() {
    if (!current()) return;
    if (isPlaying) {
      audio.pause();
      isPlaying = false;
    } else {
      audio.play().then(() => { isPlaying = true; updatePlayerUI(); });
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
    if (!queue.length) return;
    index = (index - 1 + queue.length) % queue.length;
    playSong(queue[index].id, true);
  }

  // ---------- Events ----------
  $$('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const screen = btn.dataset.screen;
      $$('.nav-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      $$('.screen').forEach(s => s.classList.remove('active'));
      $('#' + screen).classList.add('active');
      if (screen === 'library') renderLibrary($('.lib-tab.active')?.dataset.tab || 'liked');
      if (screen === 'search') $('#searchInput').focus();
    });
  });

  document.addEventListener('click', (e) => {
    const row = e.target.closest('[data-id]');
    if (row && !e.target.closest('[data-like]')) {
      playSong(row.dataset.id);
      // Open full player
      $$('.screen').forEach(s => s.classList.remove('active'));
      $('#player').classList.add('active');
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

  let searchTimeout;
  $('#searchInput').addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    const q = e.target.value.trim();
    if (q.length < 2) {
      renderSearch([]);
      $('#searchEmpty').textContent = 'Type at least 2 characters';
      $('#searchEmpty').style.display = 'block';
      return;
    }
    $('#searchEmpty').textContent = 'Searching…';
    $('#searchEmpty').style.display = 'block';
    searchTimeout = setTimeout(async () => {
      const results = await searchYoutube(q);
      renderSearch(results);
    }, 400);
  });

  $$('.lib-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      $$('.lib-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      renderLibrary(tab.dataset.tab);
    });
  });

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

  audio.addEventListener('timeupdate', () => {
    if (!audio.duration) return;
    $('#progress').value = (audio.currentTime / audio.duration) * 1000;
    $('#currentTime').textContent = fmt(audio.currentTime);
    $('#duration').textContent = fmt(audio.duration);
  });
  $('#progress').addEventListener('input', () => {
    if (!audio.duration) return;
    audio.currentTime = ($('#progress').value / 1000) * audio.duration;
  });
  audio.addEventListener('ended', () => {
    if (repeat) { audio.currentTime = 0; audio.play(); }
    else next();
  });
  audio.addEventListener('play', () => { isPlaying = true; updatePlayerUI(); });
  audio.addEventListener('pause', () => { isPlaying = false; updatePlayerUI(); });

  if ('mediaSession' in navigator) {
    navigator.mediaSession.setActionHandler('play', () => audio.play());
    navigator.mediaSession.setActionHandler('pause', () => audio.pause());
    navigator.mediaSession.setActionHandler('previoustrack', prev);
    navigator.mediaSession.setActionHandler('nexttrack', next);
  }

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }

  // Init – load trending
  (async () => {
    $('#trendingList').innerHTML = '<div class="empty-state">Loading real music…</div>';
    trending = await getTrending();
    renderHome();
    updatePlayerUI();
  })();
})();
