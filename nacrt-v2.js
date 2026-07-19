/**
 * SOV Nacrt renderer v2.
 * - keeps the original ZIP/SQL parser and export buttons
 * - collects TopoDroid cross-sections
 * - renders profile left, plan right, using real TDR station coordinates
 * - adds a detailed SOV title block and print-ready A4 SVG layout
 */
(() => {
  'use strict';

  if (typeof NacrtParser === 'undefined' || typeof NacrtRenderer === 'undefined') {
    throw new Error('Nacrt core nije učitan prije nacrt-v2.js');
  }

  const oldParseZip = NacrtParser.parseZip.bind(NacrtParser);
  NacrtParser.parseZip = async function parseZipV2(file) {
    const survey = await oldParseZip(file);
    survey.sections = [];

    try {
      const zip = await JSZip.loadAsync(file);
      for (const [path, entry] of Object.entries(zip.files)) {
        if (entry.dir || !/\.tdr$/i.test(path)) continue;
        const ab = await entry.async('arraybuffer');
        const parsed = TdrParser.parse(ab, path);
        if (parsed.plotType === 0) {
          parsed.filename = path;
          survey.sections.push(parsed);
        }
      }
    } catch (err) {
      console.warn('Nacrt v2: presjeci nisu učitani', err);
    }

    return survey;
  };

  const esc = value => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

  const domValue = (id, fallback = '') => {
    if (typeof document === 'undefined') return fallback;
    const el = document.getElementById(id);
    return el && typeof el.value === 'string' && el.value.trim() ? el.value.trim() : fallback;
  };

  const humanizeName = value => {
    const text = String(value || '').replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
    return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
  };

  const rounded = (value, mode = 'round') => {
    const n = Number(value) || 0;
    return mode === 'ceil' ? Math.ceil(n) : Math.round(n);
  };

  function getExtent(tdr, pad = 0) {
    if (!tdr) return null;
    let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
    const add = (x, y) => {
      if (!Number.isFinite(x) || !Number.isFinite(y)) return;
      xmin = Math.min(xmin, x); xmax = Math.max(xmax, x);
      ymin = Math.min(ymin, y); ymax = Math.max(ymax, y);
    };
    for (const line of tdr.lines || []) {
      for (const p of line.pts || []) {
        add(p.x, p.y);
        if (p.cp) {
          add(p.cp.cx1, p.cp.cy1); add(p.cp.cx2, p.cp.cy2);
        }
      }
    }
    for (const area of tdr.areas || []) for (const p of area.pts || []) add(p.x, p.y);
    for (const point of tdr.points || []) add(point.x, point.y);
    for (const station of tdr.stations || []) add(station.x, station.y);
    if (!Number.isFinite(xmin)) return null;
    return { xmin: xmin - pad, xmax: xmax + pad, ymin: ymin - pad, ymax: ymax + pad };
  }

  function makeTransform(ext, area) {
    if (!ext || !area) return Object.assign((x, y) => ({ x: 0, y: 0 }), { scale: 1, pixPerM: 20 });
    const sceneW = Math.max(1, ext.xmax - ext.xmin);
    const sceneH = Math.max(1, ext.ymax - ext.ymin);
    const scale = Math.min(area.width / sceneW, area.height / sceneH);
    const ox = area.left + (area.width - sceneW * scale) / 2;
    const oy = area.top + (area.height - sceneH * scale) / 2;
    const fn = (x, y) => ({
      x: ox + (x - ext.xmin) * scale,
      y: oy + (y - ext.ymin) * scale
    });
    fn.scale = scale;
    fn.pixPerM = scale * 20;
    fn.ext = ext;
    fn.area = area;
    return fn;
  }

  function pathData(points, tx) {
    if (!points || !points.length) return '';
    let d = '';
    points.forEach((point, index) => {
      const p = tx(point.x, point.y);
      if (index === 0) d += `M${p.x.toFixed(2)},${p.y.toFixed(2)}`;
      else if (point.cp) {
        const c1 = tx(point.cp.cx1, point.cp.cy1);
        const c2 = tx(point.cp.cx2, point.cp.cy2);
        d += ` C${c1.x.toFixed(2)},${c1.y.toFixed(2)} ${c2.x.toFixed(2)},${c2.y.toFixed(2)} ${p.x.toFixed(2)},${p.y.toFixed(2)}`;
      } else d += ` L${p.x.toFixed(2)},${p.y.toFixed(2)}`;
    });
    return d;
  }

  function renderLine(line, tx, underlay = false) {
    const d = pathData(line.pts, tx);
    if (!d) return '';
    if (underlay && line.type === 'wall') {
      return `<path d="${d}" fill="none" stroke="#b88a4b" stroke-opacity="0.14" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>`;
    }

    const style = {
      wall: { stroke: '#161616', width: 2.8 },
      pit: { stroke: '#232323', width: 1.5, dash: '2.5,3' },
      slope: { stroke: '#5b5147', width: 1.15 },
      user: { stroke: '#79644e', width: 1.1 },
      border: { stroke: '#222', width: 1.5 },
      'rock-border': { stroke: '#433b32', width: 1.25 },
      contour: { stroke: '#80766b', width: 0.9 },
      'floor-step': { stroke: '#4b433a', width: 1.2 },
      'ceiling-step': { stroke: '#4b433a', width: 1.2, dash: '4,3' }
    }[line.type] || { stroke: '#56504a', width: 1.05 };

    const close = line.closed ? ' Z' : '';
    return `<path d="${d}${close}" fill="none" stroke="${style.stroke}" stroke-width="${style.width}"${style.dash ? ` stroke-dasharray="${style.dash}"` : ''} stroke-linecap="round" stroke-linejoin="round"/>`;
  }

  function renderPoint(point, tx) {
    const p = tx(point.x, point.y);
    const x = p.x, y = p.y;
    const sizeMap = [5, 6.5, 8, 9.5, 11];
    const s = sizeMap[Math.max(0, Math.min(4, Number(point.scale) || 0))] || 7;
    const rotation = Number(point.orientation) || 0;

    switch (point.type) {
      case 'pillar':
        return `<rect x="${(x-s/2).toFixed(1)}" y="${(y-s/2).toFixed(1)}" width="${s.toFixed(1)}" height="${s.toFixed(1)}" fill="#5b554f"/>`;
      case 'debris':
        return `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${(s*0.48).toFixed(1)}" fill="#fff" stroke="#7e776f" stroke-width="1"/>`;
      case 'blocks':
        return `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)}) rotate(${rotation.toFixed(1)})"><polygon points="0,-${s} ${s*0.85},-${s*0.25} ${s*0.55},${s*0.75} -${s*0.55},${s*0.75} -${s*0.85},-${s*0.25}" fill="#d0c9bf" stroke="#47413b" stroke-width="1.2"/><path d="M-${s*0.55},${s*0.75} L${s*0.25},-${s*0.7} M-${s*0.85},-${s*0.25} L${s*0.55},${s*0.75}" stroke="#777067" stroke-width="0.8"/></g>`;
      case 'continuation':
        return `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)})"><rect x="-${s}" y="-${s}" width="${2*s}" height="${2*s}" rx="2" fill="#111"/><text x="0" y="${(s*0.52).toFixed(1)}" text-anchor="middle" font-size="${(s*1.65).toFixed(1)}" font-weight="700" fill="#fff">?</text></g>`;
      case 'entrance':
        return `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)}) rotate(${rotation.toFixed(1)})"><polygon points="0,-${s*1.3} ${s},${s*0.9} -${s},${s*0.9}" fill="#2b8d47" stroke="#1d6231" stroke-width="1"/></g>`;
      case 'section':
        return `<g transform="translate(${x.toFixed(1)} ${y.toFixed(1)})"><polygon points="0,-${s} ${s*0.9},${s*0.7} -${s*0.9},${s*0.7}" fill="#e7e7e7" stroke="#444" stroke-width="1"/><line x1="0" y1="-${s}" x2="0" y2="${s*0.7}" stroke="#777"/></g>`;
      case 'label':
        return point.text ? `<text x="${x.toFixed(1)}" y="${y.toFixed(1)}" font-size="11" fill="#252525">${esc(point.text)}</text>` : '';
      default:
        return `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${Math.max(2.5, s*0.35).toFixed(1)}" fill="#6b655f"/>`;
    }
  }

  function renderCenterline(tdr, sql, tx) {
    if (!tdr || !sql || !Array.isArray(sql.legs)) return '';
    const stationMap = new Map((tdr.stations || []).map(st => [String(st.name), st]));
    const parts = [];
    for (const leg of sql.legs) {
      const from = stationMap.get(String(leg.fStation));
      const to = stationMap.get(String(leg.tStation));
      if (!from || !to) continue;
      const a = tx(from.x, from.y), b = tx(to.x, to.y);
      parts.push(`<line x1="${a.x.toFixed(2)}" y1="${a.y.toFixed(2)}" x2="${b.x.toFixed(2)}" y2="${b.y.toFixed(2)}" stroke="#e12a22" stroke-width="1.45"/>`);
    }
    return parts.join('');
  }

  function renderStations(tdr, tx) {
    return (tdr.stations || []).map(st => {
      const p = tx(st.x, st.y);
      return `<g><circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="5.2" fill="#fff" stroke="#e12a22" stroke-width="2"/><circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="1.7" fill="#e12a22"/><text x="${(p.x+9).toFixed(1)}" y="${(p.y-7).toFixed(1)}" font-size="13" font-weight="600" fill="#e12a22">${esc(st.name)}</text></g>`;
    }).join('');
  }

  function renderDrawing(tdr, sql, tx, options = {}) {
    if (!tdr) return '';
    const clipId = options.clipId || `clip-${Math.random().toString(36).slice(2)}`;
    const a = tx.area;
    const parts = [`<clipPath id="${clipId}"><rect x="${a.left}" y="${a.top}" width="${a.width}" height="${a.height}"/></clipPath>`, `<g clip-path="url(#${clipId})">`];

    for (const area of tdr.areas || []) {
      const d = pathData(area.pts, tx);
      if (d) parts.push(`<path d="${d} Z" fill="#d9c19d" fill-opacity="0.55" stroke="#54493d" stroke-width="1"/>`);
    }
    for (const line of tdr.lines || []) parts.push(renderLine(line, tx, true));
    for (const line of tdr.lines || []) parts.push(renderLine(line, tx, false));
    parts.push(renderCenterline(tdr, sql, tx));
    for (const point of tdr.points || []) parts.push(renderPoint(point, tx));
    parts.push(renderStations(tdr, tx));
    parts.push('</g>');
    return parts.join('\n');
  }

  function renderDepthScale(profileTx, profileExtent, stats, x) {
    if (!profileTx || !profileExtent) return '';
    const metresByGeometry = Math.max(1, (profileExtent.ymax - profileExtent.ymin) / 20);
    const maxDepth = Math.max(10, Math.ceil(Math.max(Number(stats.dubina) || 0, metresByGeometry) / 10) * 10);
    const y0 = profileTx(0, profileExtent.ymin).y;
    const pxPerM = profileTx.pixPerM;
    const y1 = y0 + maxDepth * pxPerM;
    const parts = [`<g font-family="Arial, sans-serif" fill="#111">`];
    for (let m = 0; m < maxDepth; m += 5) {
      const ya = y0 + m * pxPerM;
      const yb = y0 + Math.min(maxDepth, m + 5) * pxPerM;
      parts.push(`<line x1="${x}" y1="${ya.toFixed(1)}" x2="${x}" y2="${yb.toFixed(1)}" stroke="${(m/5)%2===0 ? '#111' : '#fff'}" stroke-width="5"/>`);
      parts.push(`<line x1="${x}" y1="${ya.toFixed(1)}" x2="${x}" y2="${yb.toFixed(1)}" stroke="#111" stroke-opacity="0.35" stroke-width="0.7"/>`);
    }
    for (let m = 0; m <= maxDepth; m += 10) {
      const y = y0 + m * pxPerM;
      parts.push(`<line x1="${x-4}" y1="${y.toFixed(1)}" x2="${x+4}" y2="${y.toFixed(1)}" stroke="#111" stroke-width="1"/>`);
      parts.push(`<text x="${x+13}" y="${(y+5).toFixed(1)}" font-size="17">${m} m</text>`);
    }
    parts.push(`<line x1="${x}" y1="${y0.toFixed(1)}" x2="${x}" y2="${y1.toFixed(1)}" stroke="#111" stroke-width="1"/>`, '</g>');
    return parts.join('');
  }

  function renderScaleBar(tx, x, y) {
    if (!tx || !tx.pixPerM) return '';
    const choices = [1, 2, 5, 10, 20];
    let metres = 5;
    for (const choice of choices) {
      const px = choice * tx.pixPerM;
      if (px >= 70 && px <= 170) { metres = choice; break; }
    }
    const width = metres * tx.pixPerM;
    return `<g><line x1="${x}" y1="${y}" x2="${x+width}" y2="${y}" stroke="#111" stroke-width="3"/><line x1="${x}" y1="${y-7}" x2="${x}" y2="${y+7}" stroke="#111" stroke-width="2"/><line x1="${x+width}" y1="${y-7}" x2="${x+width}" y2="${y+7}" stroke="#111" stroke-width="2"/><text x="${x+width/2}" y="${y+25}" text-anchor="middle" font-size="16">${metres} m</text></g>`;
  }

  function renderNorthArrow(x, y) {
    return `<g transform="translate(${x} ${y})"><circle cx="0" cy="0" r="28" fill="#fff" stroke="#aaa" stroke-width="1"/><polygon points="0,-55 12,28 0,18 -12,28" fill="#111"/><text x="0" y="9" text-anchor="middle" font-size="24" font-weight="700" fill="#fff">N</text></g>`;
  }

  function fieldLabel(parts, x, y, label, value, options = {}) {
    const size = options.size || 14;
    parts.push(`<text x="${x}" y="${y}" font-size="${Math.max(10, size-3)}" font-weight="700" fill="#2673a6">${esc(label)}</text>`);
    parts.push(`<text x="${x}" y="${y+20}" font-size="${size}" fill="#171717">${esc(value || '—')}</text>`);
  }

  function renderHeader(parts, opt, stats, W, M) {
    const x = M, y = M, w = W - M*2, h = 282;
    const side = 165, centerX = x + side, centerW = w - side*2;
    const rightX = x + w - side;
    parts.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#fff" stroke="#222" stroke-width="2"/>`);
    parts.push(`<line x1="${centerX}" y1="${y}" x2="${centerX}" y2="${y+h}" stroke="#222"/><line x1="${rightX}" y1="${y}" x2="${rightX}" y2="${y+h}" stroke="#222"/>`);

    // Simple vector badges: no external assets, always printable/offline.
    parts.push(`<g transform="translate(${x+side/2} ${y+92})"><circle r="64" fill="#1689c5" stroke="#111" stroke-width="2"/><circle r="48" fill="#f4efe2" stroke="#111"/><path d="M-34,20 L-8,-30 9,-5 31,-40 42,26 Z" fill="#fff" stroke="#111" stroke-width="3"/><text x="0" y="82" text-anchor="middle" font-size="15" font-weight="700" fill="#111">SOV VELEBIT</text></g>`);
    parts.push(`<g transform="translate(${rightX+side/2} ${y+70})"><path d="M0,-56 L50,-34 39,50 0,65 -39,50 -50,-34 Z" fill="#fff" stroke="#d7352a" stroke-width="6"/><path d="M-32,-23 L28,-45 36,-2 3,-14 -19,26 Z" fill="#178bc5"/><text x="0" y="83" text-anchor="middle" font-size="13" font-weight="700" fill="#b4241e">PDS VELEBIT</text></g>`);

    const cy = y;
    const rows = [54, 46, 58, 58, 66];
    let ry = cy;
    rows.slice(0,-1).forEach(height => { ry += height; parts.push(`<line x1="${centerX}" y1="${ry}" x2="${rightX}" y2="${ry}" stroke="#555" stroke-width="0.8"/>`); });

    parts.push(`<text x="${centerX+12}" y="${y+18}" font-size="12" font-weight="700" fill="#2673a6">NAZIV</text>`);
    parts.push(`<text x="${centerX+centerW/2}" y="${y+39}" text-anchor="middle" font-size="27" fill="#171717">${esc(opt.title)}</text>`);
    parts.push(`<text x="${centerX+12}" y="${y+72}" font-size="12" font-weight="700" fill="#2673a6">LOKACIJA:</text>`);
    parts.push(`<text x="${centerX+104}" y="${y+72}" font-size="18" fill="#171717">${esc(opt.location || '—')}</text>`);

    const sy = y + 100;
    const sw = centerW/3;
    [centerX+sw, centerX+2*sw].forEach(xx => parts.push(`<line x1="${xx}" y1="${sy}" x2="${xx}" y2="${sy+58}" stroke="#555" stroke-width="0.8"/>`));
    fieldLabel(parts, centerX+12, sy+18, 'HORIZONTALNA DULJINA:', `${rounded(stats.horizontalna)} m`, {size:18});
    fieldLabel(parts, centerX+sw+12, sy+18, 'DULJINA:', `${rounded(stats.duljina)} m`, {size:18});
    fieldLabel(parts, centerX+2*sw+12, sy+18, 'DUBINA:', `${rounded(stats.dubina, 'ceil')} m`, {size:18});

    const ty = y + 158;
    parts.push(`<line x1="${centerX+centerW/2}" y1="${ty}" x2="${centerX+centerW/2}" y2="${ty+58}" stroke="#555" stroke-width="0.8"/>`);
    fieldLabel(parts, centerX+12, ty+18, 'TOPOGRAFSKI SNIMIO:', opt.topographer);
    fieldLabel(parts, centerX+centerW/2+12, ty+18, 'MJERIO:', opt.measuredBy);

    const by = y + 216;
    const bw = centerW/3;
    [centerX+bw, centerX+2*bw].forEach(xx => parts.push(`<line x1="${xx}" y1="${by}" x2="${xx}" y2="${y+h}" stroke="#555" stroke-width="0.8"/>`));
    fieldLabel(parts, centerX+12, by+18, 'KOORDINATE HTRS96/TM:', [opt.coordE, opt.coordN, opt.elevation].filter(Boolean).join(' / '));
    fieldLabel(parts, centerX+bw+12, by+18, 'DATUM / RAZDOBLJE:', opt.date);
    fieldLabel(parts, centerX+2*bw+12, by+18, 'DIGITALNA OBRADA:', opt.digital);

    parts.push(`<text x="${rightX+side/2}" y="${y+202}" text-anchor="middle" font-size="12" font-weight="700" fill="#2673a6">BROJ PLOČICE:</text>`);
    parts.push(`<text x="${rightX+side/2}" y="${y+241}" text-anchor="middle" font-size="27" fill="#171717">${esc(opt.plate || '—')}</text>`);
    parts.push(`<text x="${rightX+side/2}" y="${y+270}" text-anchor="middle" font-size="12" fill="#555">${esc(opt.club)}</text>`);
  }

  NacrtRenderer.render = function renderV2(survey, options = {}) {
    const sql = survey.sql || {};
    const stats = sql.stats || { duljina: 0, horizontalna: 0, dubina: 0 };
    const W = 1240, H = 1754, M = 34;

    const opt = {
      title: humanizeName(options.title || survey.name),
      date: options.date || survey.date || '',
      team: options.team || survey.team || '',
      club: options.club || 'SO Velebit',
      location: options.location || '',
      plate: options.cadastreNum || '',
      topographer: domValue('fTopographer', ''),
      measuredBy: domValue('fMeasuredBy', options.team || survey.team || ''),
      digital: domValue('fDigital', ''),
      members: domValue('fMembers', options.team || survey.team || ''),
      coordE: domValue('fCoordE', ''),
      coordN: domValue('fCoordN', ''),
      elevation: domValue('fElevation', '')
    };

    const profileArea = { left: 105, top: 352, width: 640, height: 1300 };
    const sectionArea = { left: 835, top: 500, width: 270, height: 300 };
    const planArea = { left: 760, top: 810, width: 430, height: 500 };
    const profileExtRaw = getExtent(survey.profile, 0);
    const planExtRaw = getExtent(survey.plan, 0);
    const profileTx = makeTransform(getExtent(survey.profile, 18), profileArea);
    const planTx = makeTransform(getExtent(survey.plan, 18), planArea);

    const parts = [`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">`, `<rect width="${W}" height="${H}" fill="#fff"/>`, `<style>text{font-family:Arial,'Segoe UI',sans-serif}.thin{shape-rendering:geometricPrecision}</style>`];
    renderHeader(parts, opt, stats, W, M);

    parts.push(`<text x="${profileArea.left+profileArea.width/2}" y="337" text-anchor="middle" font-size="16" font-weight="700" fill="#555">PROFIL</text>`);
    parts.push(`<text x="${planArea.left+planArea.width/2}" y="795" text-anchor="middle" font-size="16" font-weight="700" fill="#555">TLOCRT</text>`);

    if (survey.profile && profileExtRaw) {
      parts.push(renderDrawing(survey.profile, sql, profileTx, {clipId:'profile-clip'}));
      parts.push(renderDepthScale(profileTx, profileExtRaw, stats, 72));
    }

    const section = Array.isArray(survey.sections) ? survey.sections[0] : null;
    if (section) {
      const sectionTx = makeTransform(getExtent(section, 12), sectionArea);
      parts.push(`<text x="${sectionArea.left+sectionArea.width/2}" y="${sectionArea.top-12}" text-anchor="middle" font-size="14" font-weight="700" fill="#555">PRESJEK ${esc((section.stations && section.stations[0] && section.stations[0].name) || '')}</text>`);
      parts.push(renderDrawing(section, null, sectionTx, {clipId:'section-clip'}));
      const profileStation = survey.profile && (survey.profile.stations || []).find(st => section.stations && section.stations[0] && String(st.name) === String(section.stations[0].name));
      if (profileStation) {
        const from = profileTx(profileStation.x, profileStation.y);
        parts.push(`<line x1="${from.x.toFixed(1)}" y1="${from.y.toFixed(1)}" x2="${sectionArea.left}" y2="${(sectionArea.top+sectionArea.height/2).toFixed(1)}" stroke="#888" stroke-width="1.2" stroke-dasharray="16,12"/>`);
      }
    }

    if (survey.plan && planExtRaw) {
      parts.push(renderDrawing(survey.plan, sql, planTx, {clipId:'plan-clip'}));
      parts.push(renderScaleBar(planTx, planArea.left+25, planArea.top+planArea.height+45));
      parts.push(renderNorthArrow(planArea.left+planArea.width*0.55, planArea.top+planArea.height+155));
    }

    // Team row under header, like the original survey sheet.
    parts.push(`<rect x="${M}" y="${M+282}" width="${W-M*2}" height="42" fill="#fff" stroke="#222" stroke-width="1"/>`);
    parts.push(`<text x="${M+14}" y="${M+299}" font-size="12" font-weight="700" fill="#2673a6">ČLANOVI EKIPE I UDRUGE:</text>`);
    parts.push(`<text x="${M+230}" y="${M+299}" font-size="14" fill="#171717">${esc(opt.members || opt.team || '—')}</text>`);

    parts.push(`<text x="${M+12}" y="${H-22}" font-size="11" fill="#999">SOV Nacrt Generator v2</text>`);
    parts.push(`<text x="${W-M-12}" y="${H-22}" text-anchor="end" font-size="11" fill="#999">TopoDroid → SVG / PNG</text>`);
    parts.push('</svg>');
    return parts.join('\n');
  };

  function addField(grid, id, label, placeholder = '') {
    if (document.getElementById(id)) return;
    const group = document.createElement('div');
    group.className = 'form-group';
    const lab = document.createElement('label');
    lab.htmlFor = id;
    lab.textContent = label;
    const input = document.createElement('input');
    input.type = 'text'; input.id = id; input.placeholder = placeholder;
    group.append(lab, input); grid.appendChild(group);
  }

  function initUi() {
    const grid = document.querySelector('.form-grid');
    if (!grid) return;
    addField(grid, 'fTopographer', 'Topografski snimio', 'npr. Mislav Sajko');
    addField(grid, 'fMeasuredBy', 'Mjerio', 'npr. Vedran Ferenčak');
    addField(grid, 'fDigital', 'Digitalna obrada', 'npr. Mislav Sajko');
    addField(grid, 'fMembers', 'Članovi ekipe i udruge', 'imena i udruge');
    addField(grid, 'fCoordE', 'HTRS96/TM E', 'npr. 439058');
    addField(grid, 'fCoordN', 'HTRS96/TM N', 'npr. 4921061');
    addField(grid, 'fElevation', 'Nadmorska visina', 'npr. 779 mnv');

    const team = document.getElementById('fTeam');
    if (team) {
      const label = team.closest('.form-group')?.querySelector('label');
      if (label) label.textContent = 'Članovi / terenska ekipa';
      team.addEventListener('change', () => {
        const members = document.getElementById('fMembers');
        if (members && !members.value.trim()) members.value = team.value;
      });
    }
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initUi);
    else initUi();
  }
})();
