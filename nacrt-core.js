/**
 * nacrt.js — TopoDroid ZIP → cave survey parser & SVG renderer
 * SOV Nacrt Generator  (Speleološki odsjek Velebit)
 *
 * Expects JSZip to be loaded globally.
 * Usage:  const survey = await NacrtParser.parseZip(file);
 *         const svg    = NacrtRenderer.render(survey, options);
 */

// ─────────────────────────────────────────────────────────────
//  TDR Binary Parser
// ─────────────────────────────────────────────────────────────
const TdrParser = (() => {
  function parse(arrayBuffer) {
    const buf = new Uint8Array(arrayBuffer);
    const dv  = new DataView(arrayBuffer);
    let off = 0;

    const readByte  = () => buf[off++];
    const readInt   = () => { const v = dv.getInt32(off); off += 4; return v; };
    const readFloat = () => { const v = dv.getFloat32(off); off += 4; return v; };
    const readUTF   = () => {
      const len = dv.getUint16(off); off += 2;
      const s = new TextDecoder('utf-8').decode(buf.slice(off, off + len));
      off += len;
      return s;
    };

    // V tag — version
    const vTag = String.fromCharCode(readByte());
    if (vTag !== 'V') throw new Error('Expected V tag, got ' + vTag);
    const version = readInt();

    // S tag — scrap header
    const sTag = String.fromCharCode(readByte());
    if (sTag !== 'S') throw new Error('Expected S tag, got ' + sTag);
    const scrapName = readUTF();
    const plotType  = readInt(); // 1=plan, 2=profile
    readUTF(); readUTF(); readUTF(); // palettes

    const lines = [], points = [], stations = [], areas = [];
    let bbox = null;

    while (off < buf.length) {
      const tag = String.fromCharCode(readByte());

      if (tag === 'E') break;

      if (tag === 'I') {
        bbox = { xmin: readFloat(), ymin: readFloat(), xmax: readFloat(), ymax: readFloat() };
        readInt(); // extra
        continue;
      }

      if (tag === 'N') { readInt(); continue; }
      if (tag === 'F') continue;

      if (tag === 'D') {
        readFloat(); readFloat(); readFloat(); readFloat(); readFloat();
        readUTF(); // start station
        off += 6;  // skip 6 trailing bytes
        continue;
      }

      if (tag === 'L' || tag === 'A') {
        const thType = readUTF();
        const group  = readUTF();
        const closed = readInt();
        readByte(); readByte();
        const color = readInt();
        const level = readInt();
        const opt1 = readUTF(), opt2 = readUTF(), opt3 = readUTF();
        const nPts  = readInt();
        const pts = [];
        for (let i = 0; i < nPts; i++) {
          const x = readFloat(), y = readFloat();
          const hasCp = readByte();
          let cp = null;
          if (hasCp === 1) {
            cp = { cx1: readFloat(), cy1: readFloat(), cx2: readFloat(), cy2: readFloat() };
          }
          pts.push({ x, y, cp });
        }
        if (tag === 'A') readFloat(); // trailing orientation

        const obj = { type: thType, group, closed: closed !== 0, color, level, pts };
        if (tag === 'A') areas.push(obj); else lines.push(obj);
        continue;
      }

      if (tag === 'P') {
        const thType = readUTF();
        const group  = readUTF();
        const x = readFloat(), y = readFloat();
        const orientation = readFloat();
        const scale = readInt();
        const level = readInt();
        readUTF(); readUTF(); readUTF();
        points.push({ type: thType, group, x, y, orientation, scale, level });
        continue;
      }

      if (tag === 'U') {
        const name = readUTF();
        const x = readFloat(), y = readFloat();
        stations.push({ name, x, y, isUser: true });
        continue;
      }

      if (tag === 'X') {
        const x = readFloat(), y = readFloat();
        const name = readUTF();
        readInt(); readInt(); readInt();
        stations.push({ name, x, y, isUser: false });
        continue;
      }

      if (tag === 'T') {
        readUTF(); readFloat(); readFloat();
        continue;
      }

      console.warn('TDR: unknown tag', tag, 'at', off - 1);
      break;
    }

    return { version, scrapName, plotType, lines, areas, points, stations, bbox };
  }

  return { parse };
})();


// ─────────────────────────────────────────────────────────────
//  Survey SQL Parser
// ─────────────────────────────────────────────────────────────
const SurveySqlParser = (() => {
  function tokenize(valStr) {
    const tokens = [];
    let cur = '', inQ = false;
    for (let i = 0; i < valStr.length; i++) {
      const ch = valStr[i];
      if (ch === '"') { inQ = !inQ; continue; }
      if (ch === ',' && !inQ) { tokens.push(cur.trim()); cur = ''; continue; }
      cur += ch;
    }
    tokens.push(cur.trim());
    return tokens;
  }

  function parse(sqlText) {
    const lines = sqlText.split('\n');
    let survey = null;
    const shots = [];

    for (const line of lines) {
      const m = line.match(/^INSERT into (\w+) values\(\s*(.*?)\s*\);?\s*$/);
      if (!m) continue;
      const [, table, valStr] = m;
      const vals = tokenize(valStr);

      if (table === 'surveys') {
        survey = {
          id: vals[0], name: vals[1], date: vals[2], team: vals[3],
          declination: parseFloat(vals[4]), initStation: vals[6]
        };
      }
      if (table === 'shots') {
        shots.push({
          surveyId: vals[0], id: vals[1],
          fStation: vals[2], tStation: vals[3],
          distance: parseFloat(vals[4]),
          bearing:  parseFloat(vals[5]),
          clino:    parseFloat(vals[6]),
          extend: parseInt(vals[11]), flag: parseInt(vals[12]),
          leg: parseInt(vals[13]), status: parseInt(vals[14])
        });
      }
    }

    // Classify shots
    const legs    = shots.filter(s => s.fStation && s.tStation && s.status === 0 && s.leg === 0);
    const splays  = shots.filter(s => s.fStation && !s.tStation && s.status === 0);
    const deleted = shots.filter(s => s.status !== 0);

    // Declination — 1080 is TopoDroid sentinel for "not set"
    const decl = (survey && survey.declination === 1080) ? 0 : (survey ? survey.declination : 0);

    // Station coordinate reduction
    const deg2rad = d => d * Math.PI / 180;
    const stationCoords = {};
    if (survey) stationCoords[survey.initStation] = { e: 0, n: 0, z: 0 };

    let changed = true;
    while (changed) {
      changed = false;
      for (const leg of legs) {
        if (stationCoords[leg.fStation] && !stationCoords[leg.tStation]) {
          const from = stationCoords[leg.fStation];
          const d = leg.distance, b = deg2rad(leg.bearing + decl), c = deg2rad(leg.clino);
          stationCoords[leg.tStation] = {
            e: from.e + d * Math.cos(c) * Math.sin(b),
            n: from.n + d * Math.cos(c) * Math.cos(b),
            z: from.z + d * Math.sin(c)
          };
          changed = true;
        }
        if (stationCoords[leg.tStation] && !stationCoords[leg.fStation]) {
          const to = stationCoords[leg.tStation];
          const d = leg.distance, b = deg2rad(leg.bearing + decl), c = deg2rad(leg.clino);
          stationCoords[leg.fStation] = {
            e: to.e - d * Math.cos(c) * Math.sin(b),
            n: to.n - d * Math.cos(c) * Math.cos(b),
            z: to.z - d * Math.sin(c)
          };
          changed = true;
        }
      }
    }

    // Statistics
    const duljina = legs.reduce((s, l) => s + l.distance, 0);
    const horizontalna = legs.reduce((s, l) => s + l.distance * Math.cos(deg2rad(l.clino)), 0);
    const zValues = Object.values(stationCoords).map(c => c.z);
    const dubina = zValues.length >= 2 ? Math.max(...zValues) - Math.min(...zValues) : 0;

    return {
      survey, legs, splays, deleted, shots,
      declination: decl,
      stationCoords,
      stats: {
        duljina: Math.round(duljina * 10) / 10,
        horizontalna: Math.round(horizontalna * 10) / 10,
        dubina: Math.round(dubina * 10) / 10
      }
    };
  }

  return { parse };
})();


// ─────────────────────────────────────────────────────────────
//  Manifest Parser
// ─────────────────────────────────────────────────────────────
const ManifestParser = (() => {
  function parse(text) {
    const lines = text.trim().split('\n').map(l => l.trim()).filter(Boolean);
    // First line: survey name
    // Remaining lines: filename type pairs
    const name = lines[0] || '';
    const files = [];
    for (let i = 1; i < lines.length; i++) {
      const parts = lines[i].split(/\s+/);
      if (parts.length >= 2) {
        files.push({ filename: parts[0], type: parts[1] });
      }
    }
    return { name, files };
  }
  return { parse };
})();


// ─────────────────────────────────────────────────────────────
//  Main ZIP Parser
// ─────────────────────────────────────────────────────────────
const NacrtParser = (() => {
  async function parseZip(file) {
    const zip = await JSZip.loadAsync(file);

    // Find manifest
    const manifestEntry = zip.file(/manifest$/i)[0];
    if (!manifestEntry) throw new Error('manifest not found in ZIP');
    const manifestText = await manifestEntry.async('string');
    const manifest = ManifestParser.parse(manifestText);

    // Find survey.sql
    const sqlEntry = zip.file(/survey\.sql$/i)[0];
    if (!sqlEntry) throw new Error('survey.sql not found in ZIP');
    const sqlText = await sqlEntry.async('string');
    const sqlData = SurveySqlParser.parse(sqlText);

    // Parse TDR files
    let planTdr = null, profileTdr = null;
    for (const f of manifest.files) {
      const entry = zip.file(f.filename);
      if (!entry) continue;
      const ab = await entry.async('arraybuffer');
      const parsed = TdrParser.parse(ab);
      if (parsed.plotType === 1) planTdr = parsed;
      else if (parsed.plotType === 2) profileTdr = parsed;
    }

    // If manifest didn't identify them, try by filename pattern
    if (!planTdr || !profileTdr) {
      for (const [path, entry] of Object.entries(zip.files)) {
        if (entry.dir || !/\.tdr$/i.test(path)) continue;
        const ab = await entry.async('arraybuffer');
        const parsed = TdrParser.parse(ab);
        if (parsed.plotType === 1 && !planTdr) planTdr = parsed;
        if (parsed.plotType === 2 && !profileTdr) profileTdr = parsed;
      }
    }

    return {
      name: (sqlData.survey ? sqlData.survey.name : '') || manifest.name || 'Nepoznato',
      date: sqlData.survey ? sqlData.survey.date : '',
      team: sqlData.survey ? sqlData.survey.team : '',
      sql: sqlData,
      plan: planTdr,
      profile: profileTdr,
      manifest
    };
  }

  return { parseZip };
})();


// ─────────────────────────────────────────────────────────────
//  SVG Renderer
// ─────────────────────────────────────────────────────────────
const NacrtRenderer = (() => {
  // Scene: 20 units = 1 m, y grows DOWN in TDR

  // Line type → style mapping
  const LINE_STYLES = {
    'wall':    { stroke: '#333', width: 2.2, fill: 'none' },
    'pit':     { stroke: '#333', width: 1.5, fill: 'none', dash: '6,3' },
    'chimney': { stroke: '#555', width: 1.2, fill: 'none', dash: '4,4' },
    'slope':   { stroke: '#666', width: 1.0, fill: 'none' },
    'border':  { stroke: '#333', width: 1.5, fill: 'none' },
    'rock-border': { stroke: '#555', width: 1.2, fill: 'none' },
    'contour': { stroke: '#888', width: 0.8, fill: 'none' },
    'floor-step': { stroke: '#555', width: 1.2, fill: 'none' },
    'ceiling-step': { stroke: '#555', width: 1.2, fill: 'none', dash: '3,3' },
    '_default': { stroke: '#666', width: 1.0, fill: 'none' }
  };

  // Point type → symbol mapping
  const POINT_SYMBOLS = {
    'stalagmite': { shape: 'triangle', size: 4, fill: '#555' },
    'stalactite': { shape: 'triangleDown', size: 4, fill: '#555' },
    'pillar':     { shape: 'rect', size: 4, fill: '#555' },
    'flowstone':  { shape: 'circle', size: 3, fill: '#999' },
    'clay':       { shape: 'circle', size: 2.5, fill: '#b89060' },
    'sand':       { shape: 'dot', size: 1.5, fill: '#c0a060' },
    'blocks':     { shape: 'rect', size: 5, fill: 'none', stroke: '#555' },
    'pebbles':    { shape: 'circle', size: 2, fill: '#888' },
    'debris':     { shape: 'circle', size: 3, fill: 'none', stroke: '#777' },
    'water':      { shape: 'circle', size: 3, fill: '#4488cc' },
    'water-flow': { shape: 'circle', size: 3, fill: '#4488cc' },
    'entrance':   { shape: 'triangle', size: 6, fill: '#228833' },
    'air-draught': { shape: 'arrow', size: 5, fill: '#6688aa' },
    'label':      { shape: 'none' },
    'continuation': { shape: 'question', size: 5, fill: '#cc3333' },
    'danger':     { shape: 'triangle', size: 6, fill: '#cc3333' },
    'narrow-end': { shape: 'x', size: 4, stroke: '#cc3333' },
    '_default':   { shape: 'circle', size: 2.5, fill: '#777' }
  };

  function svgPointSymbol(pt, style, transform) {
    const { x, y } = transform(pt.x, pt.y);
    const s = style.size || 3;

    switch (style.shape) {
      case 'circle':
        return `<circle cx="${x}" cy="${y}" r="${s}" fill="${style.fill || '#777'}" stroke="${style.stroke || 'none'}" stroke-width="0.5"/>`;
      case 'dot':
        return `<circle cx="${x}" cy="${y}" r="${s}" fill="${style.fill || '#777'}"/>`;
      case 'triangle':
        return `<polygon points="${x},${y-s} ${x-s*0.87},${y+s*0.5} ${x+s*0.87},${y+s*0.5}" fill="${style.fill || '#555'}" stroke="none"/>`;
      case 'triangleDown':
        return `<polygon points="${x},${y+s} ${x-s*0.87},${y-s*0.5} ${x+s*0.87},${y-s*0.5}" fill="${style.fill || '#555'}" stroke="none"/>`;
      case 'rect':
        return `<rect x="${x-s/2}" y="${y-s/2}" width="${s}" height="${s}" fill="${style.fill || 'none'}" stroke="${style.stroke || '#555'}" stroke-width="0.5"/>`;
      case 'x':
        return `<g stroke="${style.stroke || '#cc3333'}" stroke-width="1.5"><line x1="${x-s}" y1="${y-s}" x2="${x+s}" y2="${y+s}"/><line x1="${x+s}" y1="${y-s}" x2="${x-s}" y2="${y+s}"/></g>`;
      case 'question':
        return `<text x="${x}" y="${y+s*0.4}" text-anchor="middle" font-size="${s*2.5}" fill="${style.fill || '#cc3333'}" font-weight="bold">?</text>`;
      case 'arrow':
        return `<polygon points="${x},${y-s} ${x-s*0.5},${y+s*0.3} ${x+s*0.5},${y+s*0.3}" fill="${style.fill || '#6688aa'}"/>`;
      case 'none':
        return '';
      default:
        return `<circle cx="${x}" cy="${y}" r="${s}" fill="${style.fill || '#777'}"/>`;
    }
  }

  function buildLinePath(pts, transform) {
    if (pts.length === 0) return '';
    let d = '';
    for (let i = 0; i < pts.length; i++) {
      const p = transform(pts[i].x, pts[i].y);
      if (i === 0) {
        d += `M${p.x.toFixed(1)},${p.y.toFixed(1)}`;
      } else if (pts[i].cp) {
        const cp = pts[i].cp;
        const c1 = transform(cp.cx1, cp.cy1);
        const c2 = transform(cp.cx2, cp.cy2);
        d += ` C${c1.x.toFixed(1)},${c1.y.toFixed(1)} ${c2.x.toFixed(1)},${c2.y.toFixed(1)} ${p.x.toFixed(1)},${p.y.toFixed(1)}`;
      } else {
        d += ` L${p.x.toFixed(1)},${p.y.toFixed(1)}`;
      }
    }
    return d;
  }

  function renderDrawing(tdr, sql, transform, label) {
    if (!tdr) return '';
    const parts = [];

    // Areas (filled)
    for (const area of tdr.areas || []) {
      const d = buildLinePath(area.pts, transform);
      if (d) parts.push(`<path d="${d} Z" fill="#e8e0d0" fill-opacity="0.3" stroke="none"/>`);
    }

    // Lines
    for (const line of tdr.lines) {
      const style = LINE_STYLES[line.type] || LINE_STYLES['_default'];
      const d = buildLinePath(line.pts, transform);
      if (!d) continue;
      let attrs = `stroke="${style.stroke}" stroke-width="${style.width}" fill="${style.fill}"`;
      if (style.dash) attrs += ` stroke-dasharray="${style.dash}"`;
      attrs += ' stroke-linecap="round" stroke-linejoin="round"';
      if (line.closed) {
        parts.push(`<path d="${d} Z" ${attrs}/>`);
      } else {
        parts.push(`<path d="${d}" ${attrs}/>`);
      }
    }

    // Points
    for (const pt of tdr.points) {
      const style = POINT_SYMBOLS[pt.type] || POINT_SYMBOLS['_default'];
      parts.push(svgPointSymbol(pt, style, transform));
    }

    // Stations
    for (const st of tdr.stations) {
      const { x, y } = transform(st.x, st.y);
      parts.push(`<circle cx="${x}" cy="${y}" r="3" fill="none" stroke="#c00" stroke-width="1.5"/>`);
      parts.push(`<circle cx="${x}" cy="${y}" r="1" fill="#c00"/>`);
      parts.push(`<text x="${x+6}" y="${y-4}" font-size="9" fill="#c00" font-family="sans-serif">${st.name}</text>`);
    }

    // Leg centerlines from SQL
    if (sql && sql.stationCoords) {
      const SCENE_SCALE = 20; // 20 TDR units = 1 m
      // Match TDR station 0 to SQL station 0 to compute offset
      const tdrSt0 = tdr.stations.find(s => s.name === (sql.survey ? sql.survey.initStation : '0'));
      if (tdrSt0) {
        for (const leg of sql.legs) {
          const from = sql.stationCoords[leg.fStation];
          const to   = sql.stationCoords[leg.tStation];
          if (!from || !to) continue;

          let fx, fy, tx, ty;
          if (tdr.plotType === 1) {
            // Plan: E→right, N→up → in TDR scene E→right, N→ negative-Y (Y grows down)
            fx = tdrSt0.x + from.e * SCENE_SCALE;
            fy = tdrSt0.y - from.n * SCENE_SCALE;
            tx = tdrSt0.x + to.e * SCENE_SCALE;
            ty = tdrSt0.y - to.n * SCENE_SCALE;
          } else {
            // Profile: horizontal distance → X, Z → negative-Y
            // We need the horizontal distance from init station
            // For simplicity, use E component for horizontal
            fx = tdrSt0.x + from.e * SCENE_SCALE;
            fy = tdrSt0.y - from.z * SCENE_SCALE;
            tx = tdrSt0.x + to.e * SCENE_SCALE;
            ty = tdrSt0.y - to.z * SCENE_SCALE;
          }
          const p1 = transform(fx, fy);
          const p2 = transform(tx, ty);
          parts.push(`<line x1="${p1.x.toFixed(1)}" y1="${p1.y.toFixed(1)}" x2="${p2.x.toFixed(1)}" y2="${p2.y.toFixed(1)}" stroke="#c00" stroke-width="1.2" stroke-dasharray="4,2"/>`);
        }
      }
    }

    return parts.join('\n');
  }

  function renderScaleBar(x, y, scalePixPerM) {
    // Choose a nice round bar length
    const candidates = [1, 2, 5, 10, 20, 50];
    let barM = 5;
    for (const c of candidates) {
      if (c * scalePixPerM >= 30 && c * scalePixPerM <= 150) { barM = c; break; }
    }
    const barPx = barM * scalePixPerM;
    return `
      <g transform="translate(${x},${y})">
        <line x1="0" y1="0" x2="${barPx}" y2="0" stroke="#333" stroke-width="2"/>
        <line x1="0" y1="-4" x2="0" y2="4" stroke="#333" stroke-width="1.5"/>
        <line x1="${barPx}" y1="-4" x2="${barPx}" y2="4" stroke="#333" stroke-width="1.5"/>
        <text x="${barPx/2}" y="14" text-anchor="middle" font-size="10" fill="#333" font-family="sans-serif">${barM} m</text>
      </g>`;
  }

  function renderNorthArrow(x, y) {
    return `
      <g transform="translate(${x},${y})">
        <line x1="0" y1="20" x2="0" y2="-20" stroke="#333" stroke-width="1.5"/>
        <polygon points="0,-20 -5,-12 5,-12" fill="#333"/>
        <text x="0" y="-24" text-anchor="middle" font-size="11" font-weight="bold" fill="#333" font-family="sans-serif">S</text>
      </g>`;
  }

  /**
   * Render full A4 survey drawing as SVG string.
   * @param {Object} survey — from NacrtParser.parseZip
   * @param {Object} options — { title, date, team, club, researcher, ... }
   * @returns {string} SVG markup
   */
  function render(survey, options = {}) {
    const opt = Object.assign({
      title: survey.name || '',
      date: survey.date || '',
      team: survey.team || '',
      club: 'SO Velebit',
      researcher: '',
      location: '',
      cadastreNum: '',
      width: 595,    // A4 portrait at 72dpi
      height: 842,
      margin: 25
    }, options);

    const W = opt.width, H = opt.height, M = opt.margin;
    const headerH = 90;
    const footerH = 30;
    const drawAreaTop = M + headerH + 10;
    const drawAreaBottom = H - M - footerH;
    const drawAreaH = drawAreaBottom - drawAreaTop;
    const drawAreaW = W - 2 * M;

    const hasPlan = !!survey.plan;
    const hasProfile = !!survey.profile;

    // Compute drawing extents
    function getExtent(tdr) {
      if (!tdr) return null;
      let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
      for (const line of tdr.lines) {
        for (const pt of line.pts) {
          xmin = Math.min(xmin, pt.x); xmax = Math.max(xmax, pt.x);
          ymin = Math.min(ymin, pt.y); ymax = Math.max(ymax, pt.y);
        }
      }
      for (const st of tdr.stations) {
        xmin = Math.min(xmin, st.x); xmax = Math.max(xmax, st.x);
        ymin = Math.min(ymin, st.y); ymax = Math.max(ymax, st.y);
      }
      if (!isFinite(xmin)) return null;
      const pad = 20; // scene units padding
      return { xmin: xmin - pad, xmax: xmax + pad, ymin: ymin - pad, ymax: ymax + pad };
    }

    const planExt    = getExtent(survey.plan);
    const profileExt = getExtent(survey.profile);

    // Layout: if both views, split horizontally (plan left, profile right)
    let planArea, profileArea;
    if (hasPlan && hasProfile) {
      const splitX = M + drawAreaW * 0.52;
      planArea    = { left: M, top: drawAreaTop, width: splitX - M - 5, height: drawAreaH };
      profileArea = { left: splitX + 5, top: drawAreaTop, width: W - M - splitX - 5, height: drawAreaH };
    } else if (hasPlan) {
      planArea = { left: M, top: drawAreaTop, width: drawAreaW, height: drawAreaH };
    } else if (hasProfile) {
      profileArea = { left: M, top: drawAreaTop, width: drawAreaW, height: drawAreaH };
    }

    // Create transform for a drawing to fit in a given area
    function makeTransform(ext, area) {
      if (!ext || !area) return (x, y) => ({ x: 0, y: 0 });
      const sceneW = ext.xmax - ext.xmin;
      const sceneH = ext.ymax - ext.ymin;
      if (sceneW <= 0 || sceneH <= 0) return (x, y) => ({ x: area.left, y: area.top });
      const scaleX = area.width / sceneW;
      const scaleY = area.height / sceneH;
      const scale = Math.min(scaleX, scaleY);
      const offsetX = area.left + (area.width - sceneW * scale) / 2;
      const offsetY = area.top + (area.height - sceneH * scale) / 2;
      const tx = (x, y) => ({
        x: offsetX + (x - ext.xmin) * scale,
        y: offsetY + (y - ext.ymin) * scale  // Y already grows down in TDR
      });
      tx.scale = scale;
      tx.pixPerM = scale * 20; // 20 scene units = 1 m
      return tx;
    }

    const planTx    = makeTransform(planExt, planArea);
    const profileTx = makeTransform(profileExt, profileArea);

    // Stats
    const stats = survey.sql ? survey.sql.stats : { duljina: 0, horizontalna: 0, dubina: 0 };

    // Build SVG
    const svgParts = [];
    svgParts.push(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}" style="background:#fff">`);
    svgParts.push(`<style>text { font-family: 'Segoe UI', system-ui, sans-serif; }</style>`);

    // ── Border ──
    svgParts.push(`<rect x="${M}" y="${M}" width="${W-2*M}" height="${H-2*M}" fill="none" stroke="#333" stroke-width="1.5"/>`);

    // ── Header table ──
    const hx = M, hy = M;
    const hw = W - 2 * M;
    svgParts.push(`<rect x="${hx}" y="${hy}" width="${hw}" height="${headerH}" fill="none" stroke="#333" stroke-width="1"/>`);
    // Title row
    svgParts.push(`<line x1="${hx}" y1="${hy+30}" x2="${hx+hw}" y2="${hy+30}" stroke="#333" stroke-width="0.8"/>`);
    svgParts.push(`<text x="${hx+hw/2}" y="${hy+21}" text-anchor="middle" font-size="16" font-weight="bold" fill="#222">${escXml(opt.title || survey.name)}</text>`);
    // Info rows
    const col2x = hx + hw * 0.45;
    svgParts.push(`<line x1="${col2x}" y1="${hy+30}" x2="${col2x}" y2="${hy+headerH}" stroke="#333" stroke-width="0.5"/>`);
    // Left column
    svgParts.push(`<text x="${hx+8}" y="${hy+46}" font-size="9" fill="#555">Datum:</text>`);
    svgParts.push(`<text x="${hx+50}" y="${hy+46}" font-size="10" fill="#222">${escXml(opt.date || survey.date)}</text>`);
    svgParts.push(`<text x="${hx+8}" y="${hy+60}" font-size="9" fill="#555">Mjerili:</text>`);
    svgParts.push(`<text x="${hx+50}" y="${hy+60}" font-size="10" fill="#222">${escXml(opt.team || survey.team)}</text>`);
    svgParts.push(`<text x="${hx+8}" y="${hy+74}" font-size="9" fill="#555">Klub:</text>`);
    svgParts.push(`<text x="${hx+50}" y="${hy+74}" font-size="10" fill="#222">${escXml(opt.club)}</text>`);
    // Right column
    svgParts.push(`<text x="${col2x+8}" y="${hy+46}" font-size="9" fill="#555">Duljina:</text>`);
    svgParts.push(`<text x="${col2x+65}" y="${hy+46}" font-size="10" fill="#222">${stats.duljina.toFixed(1).replace('.', ',')} m</text>`);
    svgParts.push(`<text x="${col2x+8}" y="${hy+60}" font-size="9" fill="#555">Horizontalna:</text>`);
    svgParts.push(`<text x="${col2x+80}" y="${hy+60}" font-size="10" fill="#222">${stats.horizontalna.toFixed(1).replace('.', ',')} m</text>`);
    svgParts.push(`<text x="${col2x+8}" y="${hy+74}" font-size="9" fill="#555">Dubina:</text>`);
    svgParts.push(`<text x="${col2x+65}" y="${hy+74}" font-size="10" fill="#222">${stats.dubina.toFixed(1).replace('.', ',')} m</text>`);

    if (opt.cadastreNum) {
      svgParts.push(`<text x="${hx+8}" y="${hy+88}" font-size="9" fill="#555">Katastarski br.:</text>`);
      svgParts.push(`<text x="${hx+85}" y="${hy+88}" font-size="10" fill="#222">${escXml(opt.cadastreNum)}</text>`);
    }

    // ── Drawings ──
    // Labels
    if (hasPlan) {
      svgParts.push(`<text x="${planArea.left + planArea.width/2}" y="${drawAreaTop - 2}" text-anchor="middle" font-size="11" font-weight="bold" fill="#444">TLOCRT</text>`);
    }
    if (hasProfile) {
      svgParts.push(`<text x="${profileArea.left + profileArea.width/2}" y="${drawAreaTop - 2}" text-anchor="middle" font-size="11" font-weight="bold" fill="#444">PROFIL</text>`);
    }

    // Separator line if both views
    if (hasPlan && hasProfile) {
      const sepX = (planArea.left + planArea.width + profileArea.left) / 2;
      svgParts.push(`<line x1="${sepX}" y1="${drawAreaTop}" x2="${sepX}" y2="${drawAreaBottom}" stroke="#ccc" stroke-width="0.5" stroke-dasharray="4,4"/>`);
    }

    // Render plan
    if (hasPlan && planExt) {
      svgParts.push(`<!-- PLAN -->`);
      svgParts.push(renderDrawing(survey.plan, survey.sql, planTx, 'TLOCRT'));
      // Scale bar
      if (planTx.pixPerM) {
        svgParts.push(renderScaleBar(planArea.left + 15, drawAreaBottom - 10, planTx.pixPerM));
      }
      // North arrow
      svgParts.push(renderNorthArrow(planArea.left + planArea.width - 20, drawAreaTop + 30));
    }

    // Render profile
    if (hasProfile && profileExt) {
      svgParts.push(`<!-- PROFILE -->`);
      svgParts.push(renderDrawing(survey.profile, survey.sql, profileTx, 'PROFIL'));
      if (profileTx.pixPerM) {
        svgParts.push(renderScaleBar(profileArea.left + 15, drawAreaBottom - 10, profileTx.pixPerM));
      }
    }

    // ── Footer ──
    svgParts.push(`<text x="${M+8}" y="${H-M-8}" font-size="8" fill="#999">SOV Nacrt Generator</text>`);
    svgParts.push(`<text x="${W-M-8}" y="${H-M-8}" text-anchor="end" font-size="8" fill="#999">TopoDroid → SVG</text>`);

    svgParts.push('</svg>');
    return svgParts.join('\n');
  }

  function escXml(s) {
    return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  return { render };
})();
