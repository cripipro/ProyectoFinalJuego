// ———————— Clase principal ————————

class Game {
  constructor() {
    this.gameId = null;
    this.boardSize = 5;
    this.board = document.getElementById('board');
    this.statusEl = document.getElementById('game-status');
    this.movesEl  = document.getElementById('moves-count');
    this.startBtn = document.getElementById('btn-new-game');
    this.nameInput = document.getElementById('player-name');
    this.showScoresBtn = document.getElementById('show-scores');
    this.closeScoresBtn = document.getElementById('close-scores');
    this.tabButtons = document.querySelectorAll('.tab-button');

    this.bindEvents();
  }

  bindEvents() {
    this.startBtn.addEventListener('click', () => this.startNewGame());
    this.showScoresBtn.addEventListener('click', showHighScores);
    this.closeScoresBtn.addEventListener('click', hideHighScores);
    this.tabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        showScoreTab(tab);
      });
    });
  }

  apiCall(url, opts = {}) {
    return async () => {
      const res = await fetch(url, {
        headers: { 'Accept': 'application/json' },
        ...opts
      });
      const body = await res.json();
      if (!res.ok) throw new Error(body.error || res.statusText);
      return body;
    };
  }

  async startNewGame() {
    try {
      const diff = +document.getElementById('difficulty').value;
      const gs = await this.apiCall(`/api/game/start?difficulty=${diff}`)();
      this.gameId = gs.gameId;
      this.movesEl.textContent  = 0;
      this.statusEl.textContent = 'En progreso';
      this.renderBoard(gs);
    } catch (e) {
      console.error(e);
      alert('Error al iniciar el juego: ' + e.message);
    }
  }

  async makeMove(q, r) {
    if (!this.gameId) return;
    try {
      const gs = await this.apiCall(
        `/api/game/block?gameId=${this.gameId}&q=${q}&r=${r}`,
        { method: 'POST' }
      )();
      this.renderBoard(gs);
      this.updateStatus(gs.status);
      this.updateMovesCount(gs.movesCount || 0);
      if (['PLAYER_LOST','PLAYER_WON'].includes(gs.status)) {
        this.showGameOver(gs.status);
      }
    } catch (e) {
      console.error(e);
      alert('Error al realizar el movimiento');
    }
  }

  updateMovesCount(n) {
    this.movesEl.textContent = n;
  }

  renderBoard(gs) {
    this.board.innerHTML = '';
    const cfg = { hexSize:25, centerX:375, centerY:375, w:40, h:46 };
    for (let q = -this.boardSize; q <= this.boardSize; q++) {
      for (let r = -this.boardSize; r <= this.boardSize; r++) {
        const s = -q - r;
        if (Math.abs(s) > this.boardSize) continue;
        const x = cfg.centerX + cfg.hexSize * (1.5 * q);
        const y = cfg.centerY + cfg.hexSize * ((Math.sqrt(3)/2) * q + Math.sqrt(3) * r);
        const cell = document.createElement('div');
        const isBorder = [q,r,s].some(c => Math.abs(c) === this.boardSize);
        cell.className = isBorder ? 'hex-cell border-cell' : 'hex-cell';
        cell.style.left = `${x - cfg.w/2}px`;
        cell.style.top  = `${y - cfg.h/2}px`;
        if (isBorder) {
          cell.style.opacity = '0.3';
          cell.style.pointerEvents = 'none';
        } else {
          if (q === gs.catPosition.q && r === gs.catPosition.r) {
            cell.classList.add('cat');
          } else if (gs.blockedCells.some(b => b.q===q && b.r===r)) {
            cell.classList.add('blocked');
          } else {
            cell.addEventListener('click', () => this.makeMove(q, r));
          }
        }
        this.board.appendChild(cell);
      }
    }
  }

  updateStatus(st) {
    const msgs = {
      IN_PROGRESS: 'En progreso',
      PLAYER_LOST: '¡El gato escapó!',
      PLAYER_WON:   '¡Atrapaste al gato!'
    };
    this.statusEl.textContent = msgs[st] || 'Estado desconocido';
  }

  showGameOver(st) {
    this.removeDialogs();
    const msgs = {
      PLAYER_LOST: '¡UPS!¡El gato se escapó! Intenta otra vez.',
      PLAYER_WON:   '¡Felicidades! ¡Ganaste!'
    };
    const div = document.createElement('div');
    div.className = 'game-over';
    const name  = this.nameInput.value.trim() || 'Anónimo';
    const moves = this.movesEl.textContent;
    div.innerHTML = `
      <h2>${msgs[st]}</h2>
      <p>Jugador: ${name}</p>
      <p>Movimientos: ${moves}</p>
      <div>
        <button id="btn-save">Guardar Puntuación</button>
        <button id="btn-close">Cerrar</button>
        <button id="btn-restart">Nuevo Juego</button>
      </div>`;
    document.body.appendChild(div);

    // Atamos aquí los botones del diálogo
    div.querySelector('#btn-save')
       .addEventListener('click', async () => {
         await saveScore(this.gameId, name);
         this.closeGameOverDialog();
       });
    div.querySelector('#btn-close')
       .addEventListener('click', () => this.closeGameOverDialog());
    div.querySelector('#btn-restart')
        .addEventListener('click', () => {
        this.closeGameOverDialog();
        this.startNewGame();
        });
  }

  closeGameOverDialog() {
    this.removeDialogs();
  }

  removeDialogs() {
    document.querySelectorAll('.game-over').forEach(d => d.remove());
  }
}

// ———————— Global: gestión de puntuaciones ————————

const createScoreFetcher = endpoint => async () => {
  try {
    const res = await fetch(endpoint, { headers:{'Accept':'application/json'} });
    if (!res.ok) throw new Error(res.statusText);
    return await res.json();
  } catch (e) {
    console.error('Error fetching scores:', e);
    return [];
  }
};

const scoreDisplayFunctions = {
  top:     createScoreFetcher('/api/game/high-scores?limit=10'),
  winning: createScoreFetcher('/api/game/winning-scores?limit=10'),
  recent:  createScoreFetcher('/api/game/recent-scores?limit=10')
};


const formatScore = score => {
  const winIcon = score.playerWon ? '🏆' : '❌';
  return {
    playerName: score.playerName,
    details:    `${winIcon} ${score.movesCount} movimientos`,
    score:      score.movesCount // o tu fórmula
  };
};

const createScoreRenderer = containerId => scores => {
  const ul = document.getElementById(containerId);
  if (!ul) return;
  ul.innerHTML = scores.map(s => {
    const f = formatScore(s);
    return `<li>
      <strong>${f.playerName}</strong>: ${f.details}
    </li>`;
  }).join('');
};

async function showHighScores() {
  document.getElementById('high-score-section').style.display = 'block';
  await showScoreTab('top');
}

function hideHighScores() {
  document.getElementById('high-score-section').style.display = 'none';
}

async function showScoreTab(tab) {
  document.querySelectorAll('.tab-button').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tab);
  });
  const fetcher = scoreDisplayFunctions[tab];
  const renderer = createScoreRenderer('score-list');
  const scores = await fetcher();
  renderer(scores);
}

const createScoreSaver = (gameId, playerName) => async () => {
  try {
    const res = await fetch(
      `/api/game/save-score?gameId=${gameId}&playerName=${encodeURIComponent(playerName)}`,
      { method:'POST', headers:{'Accept':'application/json'} }
    );
    if (!res.ok) throw new Error(res.statusText);
    alert('¡Puntuación guardada!');
  } catch (e) {
    console.error('Error saving score:', e);
    alert('Error al guardar la puntuación');
  }
};

async function saveScore(gameId, playerName) {
    await createScoreSaver(gameId, playerName)();
    // Una vez guardado, cerramos cualquier diálogo y mostramos el ranking actualizado
    showHighScores();
}

// ———————— Inicialización ————————

window.addEventListener('load', () => {
  window.game = new Game();
});
