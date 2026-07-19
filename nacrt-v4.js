/**
 * SOV Nacrt renderer v4 material logic.
 * Loaded after nacrt-v3.js.
 * Keeps v2/v3 parsing and layout, but fixes geology semantics:
 * - cave void stays white
 * - surrounding terrain is rendered as an outer halo
 * - the old v3 interior fill raster is removed
 * - footer/debug labels are removed for cleaner official output
 */
(() => {
  'use strict';

  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('Nacrt renderer nije učitan prije nacrt-v4.js');
  }

  const renderPrev = NacrtRenderer.render.bind(NacrtRenderer);
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
        if (p.cp) { add(p.cp.cx1, p.cp.cy1); add(p.cp.cx2, p.cp.cy2); }
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

  function noiseAt(x, y, seed) {
    let n = (x * 374761393 + y * 668265263 + seed * 69069) | 0;
    n = (n ^ (n >>> 13)) * 1274126177;
    return ((n ^ (n >>> 16)) >>> 0) / 4294967295;
  }

  function buildMasks(tdr, tx) {
    if (!tdr || !tx || typeof document === 'undefined') return null;
    let walls = (tdr.lines || []).filter(line => line.type === 'wall');
    if (!walls.length) walls = (tdr.lines || []).filter(line => (line.pts || []).length > 2);
    if (!walls.length) return null;
    const area = tx.area;
    const factor = 2;
    const width = Math.max(2, Math.ceil(area.width * factor));
    const height = Math.max(2, Math.ceil(area.height * factor));
    const canvas = document.createElement('canvas');
    canvas.width = width; canvas.height = height;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return null;
    ctx.clearRect(0, 0, width, height);
    ctx.strokeStyle = '#000';
    ctx.lineWidth = Math.max(5, 3.2 * factor);
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    for (const wall of walls) traceLine(ctx, wall, tx, area, factor);
    const alpha = ctx.getImageData(0, 0, width, height).data;
    const inside = new Uint8Array(width * height);
    const wallMask = new Uint8Array(width * height);
    for (let i = 0; i < width * height; i++) wallMask[i] = alpha[i * 4 + 3] >= 40 ? 1 : 0;

    for (let y = 0; y < height; y++) {
      const runs = [];
      let x = 0;
      while (x < width) {
        while (x < width && !wallMask[y * width + x]) x++;
        if (x >= width) break;
        const start = x;
        while (x < width && wallMask[y * width + x]) x++;
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
    return { inside, wallMask, width, height, factor, area };
  }

  function buildTerrainHaloImage(tdr, tx, kind) {
    const masks = buildMasks(tdr, tx);
    if (!masks || typeof document === 'undefined') return '';
    const { inside, wallMask, width, height, area } = masks;
    const outCanvas = document.createElement('canvas');
    outCanvas.width = width; outCanvas.height = height;
    const outCtx = outCanvas.getContext('2d');
    if (!outCtx) return '';
    const image = outCtx.createImageData(width, height);
    const data = image.data;

    const haloRadius = kind === 'profile' ? 44 : kind === 'section' ? 28 : 32;
    const grainSeed = kind === 'profile' ? 77 : kind === 'section' ? 91 : 103;

    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const idx = y * width + x;
        if (inside[idx]) continue;

        let nearWall = false;
        let minD2 = haloRadius * haloRadius + 1;
        for (let dy = -haloRadius; dy <= haloRadius; dy++) {
          const yy = y + dy;
          if (yy < 0 || yy >= height) continue;
          const rem = Math.floor(Math.sqrt(Math.max(0, haloRadius * haloRadius - dy * dy)));
          for (let dx = -rem; dx <= rem; dx++) {
            const xx = x + dx;
            if (xx < 0 || xx >= width) continue;
            if (!wallMask[yy * width + xx]) continue;
            nearWall = true;
            const d2 = dx * dx + dy * dy;
            if (d2 < minD2) minD2 = d2;
          }
        }
        if (!nearWall) continue;

        const p = idx * 4;
        const d = Math.sqrt(minD2);
        const fade = Math.max(0, 1 - d / haloRadius);
        const ny = height > 1 ? y / (height - 1) : 0;
        const baseLight = kind === 'profile' ? [232, 225, 207] : [229, 222, 204];
        const baseDark  = kind === 'profile' ? [203, 191, 162] : [210, 198, 168];
        const t = Math.max(0, Math.min(1, 0.18 + ny * 0.32));
        const n = (noiseAt(x, y, grainSeed) - 0.5) * 10;
        const r = Math.round((baseLight[0] + (baseDark[0] - baseLight[0]) * t) + n);
        const g = Math.round((baseLight[1] + (baseDark[1] - baseLight[1]) * t) + n);
        const b = Math.round((baseLight[2] + (baseDark[2] - baseLight[2]) * t) + n);
        data[p] = Math.max(0, Math.min(255, r));
        data[p+1] = Math.max(0, Math.min(255, g));
        data[p+2] = Math.max(0, Math.min(255, b));
        data[p+3] = Math.round(150 + fade * 70);
      }
    }

    outCtx.putImageData(image, 0, 0);
    outCtx.save();
    outCtx.globalAlpha = 0.14;
    for (let i = 0; i < Math.floor(width * height / 12000); i++) {
      const x = Math.floor(noiseAt(i, grainSeed, 3) * width);
      const y = Math.floor(noiseAt(i, grainSeed, 7) * height);
      const idx = y * width + x;
      if (inside[idx]) continue;
      outCtx.fillStyle = i % 2 ? '#f7f0e2' : '#a98958';
      outCtx.beginPath();
      outCtx.ellipse(x, y, 1 + (i % 3), 0.8 + (i % 2), (i * 0.63) % Math.PI, 0, Math.PI * 2);
      outCtx.fill();
    }
    outCtx.restore();

    const href = outCanvas.toDataURL('image/png');
    return `<image class="sov-v4-terrain sov-v4-${kind}" x="${area.left}" y="${area.top}" width="${area.width}" height="${area.height}" href="${href}" preserveAspectRatio="none"/>`;
  }

  function patchHeaderAndFooter(svg, survey) {
    const stats = (survey && survey.sql && survey.sql.stats) || {};
    const depth = Math.round(Number(stats.dubina) || 0);
    if (depth > 0) {
      svg = svg.replace(/(<text[^>]*>DUBINA:<\/text>[\s\S]*?<text[^>]*>)(\d+) m(<\/text>)/, `$1${depth} m$3`);
    }
    svg = svg.replace(/<text x="34" y="1732"[^<]*<\/text>/g, '');
    svg = svg.replace(/<text x="1194" y="1732"[^<]*<\/text>/g, '');
    svg = svg.replace(/<text x="425" y="337"[^<]*PROFIL<\/text>/g, '');
    svg = svg.replace(/<text x="975" y="795"[^<]*TLOCRT<\/text>/g, '');
    return svg;
  }

  NacrtRenderer.render = function renderV4(survey, options = {}) {
    let svg = renderPrev(survey, options);
    svg = svg.replace(/<image class="sov-v3-cave-fill[\s\S]*?\/>(?:\n)?/g, '');
    svg = patchHeaderAndFooter(svg, survey);

    const profileTx = survey.profile ? makeTransform(getExtent(survey.profile, LAYOUT.profile.pad), LAYOUT.profile) : null;
    const planTx = survey.plan ? makeTransform(getExtent(survey.plan, LAYOUT.plan.pad), LAYOUT.plan) : null;
    const section = Array.isArray(survey.sections) ? survey.sections[0] : null;
    const sectionTx = section ? makeTransform(getExtent(section, LAYOUT.section.pad), LAYOUT.section) : null;

    const halos = [
      buildTerrainHaloImage(survey.profile, profileTx, 'profile'),
      buildTerrainHaloImage(section, sectionTx, 'section'),
      buildTerrainHaloImage(survey.plan, planTx, 'plan')
    ].filter(Boolean).join('');

    svg = svg.replace(/(<defs id="sov-v3-defs">[\s\S]*?<\/defs>)/, `$1${halos}`);
    svg = svg.replace(/SOV Nacrt Generator v3/g, '');
    return svg;
  };

  if (typeof window !== 'undefined') window.SOV_NACRT_V4 = { version: '4.0' };
})();
