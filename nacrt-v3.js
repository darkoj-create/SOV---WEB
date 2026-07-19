/**
 * SOV Nacrt renderer v3 visual layer.
 * Loaded after nacrt-v2.js. Keeps v2 geometry/layout and adds:
 * - scanline-inferred cave fills for drawings without TopoDroid areas
 * - depth-aware colour, grain and relief shading
 * - richer blocks, debris, pillar and entrance symbols
 * - print-safe SVG filters and gradients
 */
(() => {
  'use strict';

  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('Nacrt renderer v2 nije učitan prije nacrt-v3.js');
  }

  const renderV2 = NacrtRenderer.render.bind(NacrtRenderer);
  const LAYOUT = {
    profile: { left: 105, top: 352, width: 640, height: 1300, pad: 18 },
    section: { left: 835, top: 500, width: 270, height: 300, pad: 12 },
    plan: { left: 760, top: 810, width: 430, height: 500, pad: 18 }
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
          add(p.cp.cx1, p.cp.cy1);
          add(p.cp.cx2, p.cp.cy2);
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
    if (!ext || !area) return null;
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
    fn.area = area;
    return fn;
  }

  function traceLine(ctx, line, tx, area, factor) {
    const pts = line && line.pts;
    if (!pts || pts.length < 2) return;
    const local = p => {
      const q = tx(p.x, p.y);
      return { x: (q.x - area.left) * factor, y: (q.y - area.top) * factor };
    };
    const first = local(pts[0]);
    ctx.beginPath();
    ctx.moveTo(first.x, first.y);
    for (let i = 1; i < pts.length; i++) {
      const p = pts[i];
      const q = local(p);
      if (p.cp) {
        const c1Abs = tx(p.cp.cx1, p.cp.cy1);
        const c2Abs = tx(p.cp.cx2, p.cp.cy2);
        ctx.bezierCurveTo(
          (c1Abs.x - area.left) * factor,
          (c1Abs.y - area.top) * factor,
          (c2Abs.x - area.left) * factor,
          (c2Abs.y - area.top) * factor,
          q.x,
          q.y
        );
      } else ctx.lineTo(q.x, q.y);
    }
    if (line.closed) ctx.closePath();
    ctx.stroke();
  }

  function colourFor(kind, x, y, width, height) {
    const nx = width > 1 ? x / (width - 1) : 0;
    const ny = height > 1 ? y / (height - 1) : 0;
    let top, bottom;
    if (kind === 'profile') {
      top = [228, 216, 193];
      bottom = [139, 91, 43];
    } else if (kind === 'section') {
      top = [222, 207, 180];
      bottom = [171, 129, 78];
    } else {
      top = [229, 217, 194];
      bottom = [183, 145, 94];
    }
    const vertical = kind === 'profile' ? Math.pow(ny, 0.82) : 0.32 + ny * 0.42;
    const sideLight = Math.sin((nx + 0.15) * Math.PI) * 0.12;
    const t = Math.max(0, Math.min(1, vertical - sideLight));
    return [
      Math.round(top[0] + (bottom[0] - top[0]) * t),
      Math.round(top[1] + (bottom[1] - top[1]) * t),
      Math.round(top[2] + (bottom[2] - top[2]) * t)
    ];
  }

  function noiseAt(x, y, seed) {
    let n = (x * 374761393 + y * 668265263 + seed * 69069) | 0;
    n = (n ^ (n >>> 13)) * 1274126177;
    return ((n ^ (n >>> 16)) >>> 0) / 4294967295;
  }

  function buildFillImage(tdr, tx, kind) {
    if (!tdr || !tx || typeof document === 'undefined') return '';
    let walls = (tdr.lines || []).filter(line => line.type === 'wall');
    // Cross-sections can be stored as a single pit outline instead of wall lines.
    if (!walls.length) walls = (tdr.lines || []).filter(line => (line.pts || []).length > 2);
    if (!walls.length) return '';

    const area = tx.area;
    const factor = 2;
    const width = Math.max(2, Math.ceil(area.width * factor));
    const height = Math.max(2, Math.ceil(area.height * factor));
    const maskCanvas = document.createElement('canvas');
    maskCanvas.width = width; maskCanvas.height = height;
    const maskCtx = maskCanvas.getContext('2d', { willReadFrequently: true });
    if (!maskCtx) return '';

    maskCtx.clearRect(0, 0, width, height);
    maskCtx.strokeStyle = '#000';
    maskCtx.lineWidth = Math.max(5, 3.2 * factor);
    maskCtx.lineCap = 'round';
    maskCtx.lineJoin = 'round';
    for (const wall of walls) traceLine(maskCtx, wall, tx, area, factor);

    const mask = maskCtx.getImageData(0, 0, width, height);
    const alpha = mask.data;
    const inside = new Uint8Array(width * height);

    // Pair wall crossings row-by-row. This works for shafts and branched plans,
    // and still leaves genuinely open passages unfilled instead of flooding the page.
    for (let y = 0; y < height; y++) {
      const runs = [];
      let x = 0;
      while (x < width) {
        while (x < width && alpha[(y * width + x) * 4 + 3] < 40) x++;
        if (x >= width) break;
        const start = x;
        while (x < width && alpha[(y * width + x) * 4 + 3] >= 40) x++;
        runs.push({ start, end: x - 1 });
      }
      if (runs.length < 2) continue;
      for (let i = 0; i + 1 < runs.length; i += 2) {
        const from = Math.min(width - 1, runs[i].end + 1);
        const to = Math.max(0, runs[i + 1].start - 1);
        if (to - from < 3) continue;
        for (let xx = from; xx <= to; xx++) inside[y * width + xx] = 1;
      }
    }

    // Horizontal wall segments can briefly hide a scanline crossing and create
    // a thin transparent stripe. Close only short vertical gaps; large real
    // openings and separated passages stay untouched.
    const maxVerticalGap = 12 * factor;
    for (let x = 0; x < width; x++) {
      let y = 0;
      while (y < height) {
        while (y < height && !inside[y * width + x]) y++;
        if (y >= height) break;
        while (y < height && inside[y * width + x]) y++;
        const gapStart = y;
        while (y < height && !inside[y * width + x] && y - gapStart <= maxVerticalGap) y++;
        if (y < height && inside[y * width + x] && y - gapStart <= maxVerticalGap) {
          for (let yy = gapStart; yy < y; yy++) inside[yy * width + x] = 1;
        }
      }
    }

    const outCanvas = document.createElement('canvas');
    outCanvas.width = width; outCanvas.height = height;
    const outCtx = outCanvas.getContext('2d');
    if (!outCtx) return '';
    const image = outCtx.createImageData(width, height);
    const data = image.data;
    const seed = kind === 'profile' ? 17 : kind === 'section' ? 31 : 47;

    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const index = y * width + x;
        if (!inside[index]) continue;
        const p = index * 4;
        const base = colourFor(kind, x, y, width, height);
        const fine = (noiseAt(x, y, seed) - 0.5) * 17;
        const grain = noiseAt(Math.floor(x / 5), Math.floor(y / 5), seed + 9) > 0.86 ? -13 : 0;
        data[p] = Math.max(0, Math.min(255, base[0] + fine + grain));
        data[p + 1] = Math.max(0, Math.min(255, base[1] + fine + grain));
        data[p + 2] = Math.max(0, Math.min(255, base[2] + fine + grain));
        data[p + 3] = 226;
      }
    }
    outCtx.putImageData(image, 0, 0);

    // Subtle mineral flecks, deterministic and restricted to filled pixels.
    outCtx.save();
    outCtx.globalAlpha = 0.12;
    for (let i = 0; i < Math.floor(width * height / 9000); i++) {
      const x = Math.floor(noiseAt(i, seed, 3) * width);
      const y = Math.floor(noiseAt(i, seed, 7) * height);
      if (!inside[y * width + x]) continue;
      outCtx.fillStyle = i % 3 === 0 ? '#fff4d8' : '#5d3d24';
      outCtx.beginPath();
      outCtx.ellipse(x, y, 1 + (i % 3), 0.7 + (i % 2), (i * 0.77) % Math.PI, 0, Math.PI * 2);
      outCtx.fill();
    }
    outCtx.restore();

    const href = outCanvas.toDataURL('image/png');
    return `<image class="sov-v3-cave-fill sov-v3-${kind}" x="${area.left}" y="${area.top}" width="${area.width}" height="${area.height}" href="${href}" preserveAspectRatio="none" filter="url(#sov-v3-relief)"/>`;
  }

  function irregularRock(x, y, size, rotation, index) {
    const points = [];
    const count = 7;
    for (let i = 0; i < count; i++) {
      const a = (Math.PI * 2 * i / count) + rotation;
      const wobble = 0.72 + noiseAt(i, index, 91) * 0.38;
      points.push(`${(x + Math.cos(a) * size * wobble).toFixed(1)},${(y + Math.sin(a) * size * wobble).toFixed(1)}`);
    }
    return `<polygon points="${points.join(' ')}" fill="url(#sov-v3-rock)" stroke="#4a4037" stroke-width="1.1" filter="url(#sov-v3-stone-shadow)"/>`;
  }

  function renderMaterialOverlay(tdr, tx, kind) {
    if (!tdr || !tx) return '';
    const parts = [`<g class="sov-v3-materials sov-v3-materials-${kind}">`];
    let index = 0;
    for (const point of tdr.points || []) {
      const p = tx(point.x, point.y);
      const scale = 4.5 + Math.max(0, Math.min(4, Number(point.scale) || 0)) * 1.25;
      const rotation = (Number(point.orientation) || 0) * Math.PI / 180;
      if (point.type === 'blocks') {
        parts.push('<g>');
        for (let i = 0; i < 5; i++) {
          const a = rotation + i * 1.31;
          const radius = i === 0 ? 0 : scale * (0.55 + (i % 2) * 0.18);
          parts.push(irregularRock(p.x + Math.cos(a) * radius, p.y + Math.sin(a) * radius, scale * (i === 0 ? 1.05 : 0.62), a, index * 11 + i));
        }
        parts.push('</g>');
      } else if (point.type === 'debris') {
        parts.push('<g>');
        for (let i = 0; i < 8; i++) {
          const a = i * 2.17 + rotation;
          const r = scale * (0.35 + (i % 4) * 0.18);
          const rr = 1.7 + (i % 3) * 0.9;
          parts.push(irregularRock(p.x + Math.cos(a) * r, p.y + Math.sin(a) * r * 0.68, rr, a, index * 13 + i));
        }
        parts.push('</g>');
      } else if (point.type === 'pillar') {
        const s = scale * 0.8;
        parts.push(`<g transform="translate(${p.x.toFixed(1)} ${p.y.toFixed(1)}) rotate(${(Number(point.orientation)||0).toFixed(1)})" filter="url(#sov-v3-stone-shadow)"><path d="M-${(s*0.75).toFixed(1)},-${s.toFixed(1)} Q0,-${(s*1.25).toFixed(1)} ${(s*0.72).toFixed(1)},-${(s*0.85).toFixed(1)} L${(s*0.62).toFixed(1)},${(s*0.9).toFixed(1)} Q0,${(s*1.18).toFixed(1)} -${(s*0.7).toFixed(1)},${(s*0.8).toFixed(1)} Z" fill="url(#sov-v3-pillar)" stroke="#4a4037" stroke-width="1"/><path d="M-${(s*0.35).toFixed(1)},-${(s*0.75).toFixed(1)} Q0,0 ${(s*0.25).toFixed(1)},${(s*0.72).toFixed(1)}" fill="none" stroke="#efe2c9" stroke-opacity="0.45" stroke-width="1"/></g>`);
      } else if (point.type === 'entrance') {
        const s = scale * 1.25;
        parts.push(`<g transform="translate(${p.x.toFixed(1)} ${p.y.toFixed(1)}) rotate(${(Number(point.orientation)||0).toFixed(1)})"><path d="M-${s.toFixed(1)},${(s*0.65).toFixed(1)} Q0,-${(s*0.8).toFixed(1)} ${s.toFixed(1)},${(s*0.65).toFixed(1)} Z" fill="url(#sov-v3-earth)" stroke="#4a4a2a" stroke-width="1.1"/><path d="M-${(s*0.75).toFixed(1)},${(s*0.5).toFixed(1)} Q0,-${(s*0.48).toFixed(1)} ${(s*0.75).toFixed(1)},${(s*0.5).toFixed(1)}" fill="none" stroke="#2e783d" stroke-width="2"/></g>`);
      }
      index++;
    }
    parts.push('</g>');
    return parts.join('');
  }

  function definitions() {
    return `<defs id="sov-v3-defs">
      <linearGradient id="sov-v3-rock" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#f0e6d4"/><stop offset="0.45" stop-color="#b7a58e"/><stop offset="1" stop-color="#756453"/></linearGradient>
      <linearGradient id="sov-v3-pillar" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#66584c"/><stop offset="0.48" stop-color="#a18c72"/><stop offset="0.7" stop-color="#5a4e43"/><stop offset="1" stop-color="#3f3832"/></linearGradient>
      <linearGradient id="sov-v3-earth" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8e6b3b"/><stop offset="1" stop-color="#4d3824"/></linearGradient>
      <filter id="sov-v3-relief" x="-15%" y="-15%" width="130%" height="130%"><feGaussianBlur in="SourceAlpha" stdDeviation="1.2" result="blur"/><feOffset dx="1.8" dy="2.4" result="off"/><feColorMatrix in="off" type="matrix" values="0 0 0 0 0.17 0 0 0 0 0.11 0 0 0 0 0.06 0 0 0 .42 0" result="shadow"/><feMerge><feMergeNode in="shadow"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
      <filter id="sov-v3-stone-shadow" x="-35%" y="-35%" width="170%" height="170%"><feDropShadow dx="1.1" dy="1.6" stdDeviation="1.1" flood-color="#2d241d" flood-opacity="0.5"/></filter>
    </defs>`;
  }

  function patchBaseSvg(svg) {
    return svg
      .replace(/SOV Nacrt Generator v2/g, 'SOV Nacrt Generator v3')
      .replace(/stroke="#b88a4b" stroke-opacity="0\.14" stroke-width="10"/g, 'stroke="#7d542e" stroke-opacity="0.06" stroke-width="6"')
      .replace(/fill="#d9c19d" fill-opacity="0\.55"/g, 'fill="#c8aa7d" fill-opacity="0.70"')
      .replace(/fill="#d0c9bf" stroke="#47413b"/g, 'fill="url(#sov-v3-rock)" stroke="#47413b"')
      .replace(/fill="#5b554f"/g, 'fill="url(#sov-v3-pillar)"')
      .replace(/stroke="#7e776f" stroke-width="1"/g, 'stroke="#655a50" stroke-width="1.1"');
  }

  NacrtRenderer.render = function renderV3(survey, options = {}) {
    let svg = patchBaseSvg(renderV2(survey, options));
    const profileTx = survey.profile ? makeTransform(getExtent(survey.profile, LAYOUT.profile.pad), LAYOUT.profile) : null;
    const planTx = survey.plan ? makeTransform(getExtent(survey.plan, LAYOUT.plan.pad), LAYOUT.plan) : null;
    const section = Array.isArray(survey.sections) ? survey.sections[0] : null;
    const sectionTx = section ? makeTransform(getExtent(section, LAYOUT.section.pad), LAYOUT.section) : null;

    const fills = [
      buildFillImage(survey.profile, profileTx, 'profile'),
      buildFillImage(section, sectionTx, 'section'),
      buildFillImage(survey.plan, planTx, 'plan')
    ].filter(Boolean).join('');

    // Put raster relief behind the v2 vector geometry, then place material symbols
    // above it. Header and red station graphics remain fully vector and crisp.
    const injectBehind = `${definitions()}${fills}`;
    svg = svg.replace(/<style>([\s\S]*?)<\/style>/, match => `${match}${injectBehind}`);

    const overlays = [
      renderMaterialOverlay(survey.profile, profileTx, 'profile'),
      renderMaterialOverlay(section, sectionTx, 'section'),
      renderMaterialOverlay(survey.plan, planTx, 'plan')
    ].filter(Boolean).join('');
    svg = svg.replace('</svg>', `${overlays}</svg>`);
    return svg;
  };

  if (typeof window !== 'undefined') {
    window.SOV_NACRT_V3 = { version: '3.0', getExtent, makeTransform, buildFillImage };
  }
})();