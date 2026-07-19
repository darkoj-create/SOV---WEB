/**
 * SOV Nacrt symbols polish.
 * Loaded after nacrt-branding.js.
 * Adds lightweight, object-specific SVG symbols only:
 * - irregular rocks / debris (grey, print-safe)
 * - conifer trees for explicit vegetation points or a clearly detached tall user-line tree cluster
 * No cave/terrain colouring, Canvas or per-pixel processing.
 */
(() => {
  'use strict';

  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('Nacrt symbols traži učitan nacrt-branding.js');
  }

  const renderPrevious = NacrtRenderer.render.bind(NacrtRenderer);
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
    return Number.isFinite(xmin) ? { xmin: xmin - pad, xmax: xmax + pad, ymin: ymin - pad, ymax: ymax + pad } : null;
  }

  function makeTransform(ext, area) {
    if (!ext || !area) return null;
    const w = Math.max(1, ext.xmax - ext.xmin), h = Math.max(1, ext.ymax - ext.ymin);
    const scale = Math.min(area.width / w, area.height / h);
    const ox = area.left + (area.width - w * scale) / 2;
    const oy = area.top + (area.height - h * scale) / 2;
    const tx = (x, y) => ({ x: ox + (x - ext.xmin) * scale, y: oy + (y - ext.ymin) * scale });
    tx.scale = scale; tx.area = area;
    return tx;
  }

  function bboxOfLines(lines) {
    let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
    for (const line of lines || []) for (const p of line.pts || []) {
      if (!Number.isFinite(p.x) || !Number.isFinite(p.y)) continue;
      xmin = Math.min(xmin, p.x); xmax = Math.max(xmax, p.x);
      ymin = Math.min(ymin, p.y); ymax = Math.max(ymax, p.y);
    }
    return Number.isFinite(xmin) ? { xmin, xmax, ymin, ymax, width: xmax - xmin, height: ymax - ymin } : null;
  }

  function overlapRatio(a, b) {
    if (!a || !b) return 0;
    const w = Math.max(0, Math.min(a.xmax, b.xmax) - Math.max(a.xmin, b.xmin));
    const h = Math.max(0, Math.min(a.ymax, b.ymax) - Math.max(a.ymin, b.ymin));
    const area = Math.max(1, a.width * a.height);
    return (w * h) / area;
  }

  function detectDetachedTree(tdr) {
    const userLines = (tdr && tdr.lines || []).filter(line => String(line.type || '').toLowerCase() === 'user' && (line.pts || []).length >= 2);
    if (userLines.length < 3) return null;
    const userBox = bboxOfLines(userLines);
    const wallBox = bboxOfLines((tdr.lines || []).filter(line => String(line.type || '').toLowerCase() === 'wall'));
    if (!userBox || userBox.height < userBox.width * 1.55 || userBox.height < 35) return null;
    if (wallBox && overlapRatio(userBox, wallBox) > 0.18) return null;
    return userBox;
  }

  function coniferSvg(x, y, width, height, opacity = 1) {
    const w = Math.max(22, width), h = Math.max(70, height);
    const cx = x + w / 2;
    const trunkW = Math.max(3, w * 0.085);
    const trunkTop = y + h * 0.22;
    const ground = y + h * 0.98;
    const layers = [
      [0.08, 0.16, 0.22], [0.16, 0.27, 0.36], [0.25, 0.39, 0.50],
      [0.35, 0.52, 0.64], [0.47, 0.65, 0.77], [0.60, 0.77, 0.88]
    ];
    const parts = [`<g class="sov-symbol-tree" opacity="${opacity}">`];
    parts.push(`<path d="M${(cx-trunkW/2).toFixed(1)} ${trunkTop.toFixed(1)} L${(cx+trunkW/2).toFixed(1)} ${trunkTop.toFixed(1)} L${(cx+trunkW*0.72).toFixed(1)} ${ground.toFixed(1)} L${(cx-trunkW*0.72).toFixed(1)} ${ground.toFixed(1)} Z" fill="#76502f" stroke="#2f241b" stroke-width="1"/>`);
    parts.push(`<path d="M${cx.toFixed(1)} ${(y+h*0.12).toFixed(1)} L${cx.toFixed(1)} ${(ground-h*0.06).toFixed(1)}" stroke="#3d2d20" stroke-width="1.2"/>`);
    layers.forEach((layer, index) => {
      const top = y + h * layer[0];
      const middle = y + h * layer[1];
      const bottom = y + h * layer[2];
      const half = w * (0.12 + index * 0.062);
      const colour = index % 3 === 0 ? '#2f6f3e' : index % 3 === 1 ? '#3f8448' : '#527f43';
      const d = [
        `M${cx.toFixed(1)} ${top.toFixed(1)}`,
        `L${(cx-half*0.55).toFixed(1)} ${middle.toFixed(1)}`,
        `L${(cx-half*0.27).toFixed(1)} ${(middle+h*0.018).toFixed(1)}`,
        `L${(cx-half).toFixed(1)} ${bottom.toFixed(1)}`,
        `L${(cx-half*0.34).toFixed(1)} ${(bottom-h*0.005).toFixed(1)}`,
        `L${cx.toFixed(1)} ${(bottom+h*0.025).toFixed(1)}`,
        `L${(cx+half*0.34).toFixed(1)} ${(bottom-h*0.005).toFixed(1)}`,
        `L${(cx+half).toFixed(1)} ${bottom.toFixed(1)}`,
        `L${(cx+half*0.27).toFixed(1)} ${(middle+h*0.018).toFixed(1)}`,
        `L${(cx+half*0.55).toFixed(1)} ${middle.toFixed(1)} Z`
      ].join(' ');
      parts.push(`<path d="${d}" fill="${colour}" stroke="#24482d" stroke-width="0.8" stroke-linejoin="round"/>`);
    });
    parts.push(`<path d="M${(cx-w*0.28).toFixed(1)} ${ground.toFixed(1)} Q${cx.toFixed(1)} ${(ground+h*0.025).toFixed(1)} ${(cx+w*0.28).toFixed(1)} ${ground.toFixed(1)}" fill="none" stroke="#222" stroke-width="1.1"/>`);
    parts.push('</g>');
    return parts.join('');
  }

  function rockPolygon(cx, cy, radius, seed) {
    const points = [];
    const count = 6 + (seed % 3);
    for (let i = 0; i < count; i++) {
      const a = Math.PI * 2 * i / count;
      const wobble = 0.72 + (((seed * 19 + i * 37) % 29) / 100);
      points.push(`${(cx + Math.cos(a) * radius * wobble).toFixed(1)},${(cy + Math.sin(a) * radius * wobble).toFixed(1)}`);
    }
    const fills = ['#ececea', '#d9d9d5', '#c8cac8', '#f3f3f0'];
    return `<polygon points="${points.join(' ')}" fill="${fills[seed % fills.length]}" stroke="#555" stroke-width="0.9" stroke-linejoin="round"/>`;
  }

  function renderPointSymbols(tdr, tx) {
    if (!tdr || !tx) return '';
    const out = [];
    let index = 0;
    for (const point of tdr.points || []) {
      const type = String(point.type || point.name || '').toLowerCase();
      const p = tx(point.x, point.y);
      const base = Math.max(4.5, 5.5 + Number(point.scale || 0) * 0.8);
      if (/tree|bush|plant|vegetation|conifer/.test(type)) {
        out.push(coniferSvg(p.x - base * 1.3, p.y - base * 4.8, base * 2.6, base * 5.2));
      } else if (/block|rock|boulder/.test(type)) {
        out.push('<g class="sov-symbol-rocks">');
        [[0,0,1.25],[-0.9,0.55,0.72],[0.88,0.5,0.78],[0.18,-0.82,0.68]].forEach((r,j)=>{
          out.push(rockPolygon(p.x+r[0]*base,p.y+r[1]*base,base*r[2],index*7+j));
        });
        out.push('</g>');
      } else if (/debris|pebble|rubble|scree/.test(type)) {
        out.push('<g class="sov-symbol-debris">');
        for (let i=0;i<8;i++) {
          const a=i*2.07, dist=base*(0.28+(i%4)*0.19), rr=base*(0.18+(i%3)*0.07);
          out.push(rockPolygon(p.x+Math.cos(a)*dist,p.y+Math.sin(a)*dist*0.65,rr,index*11+i));
        }
        out.push('</g>');
      }
      index++;
    }
    return out.join('');
  }

  function renderDetectedTree(tdr, tx) {
    const box = detectDetachedTree(tdr);
    if (!box || !tx) return '';
    const p1 = tx(box.xmin, box.ymin), p2 = tx(box.xmax, box.ymax);
    const x = Math.min(p1.x, p2.x), y = Math.min(p1.y, p2.y);
    const w = Math.abs(p2.x - p1.x), h = Math.abs(p2.y - p1.y);
    return coniferSvg(x, y, w, h, 0.98);
  }

  function improvedPatterns(svg) {
    const debris = `<pattern id="sov-pattern-debris" width="42" height="34" patternUnits="userSpaceOnUse">
      <path d="M2 12 L8 4 L16 7 L15 17 L7 20 Z" fill="#d8d9d6" stroke="#555" stroke-width="0.9"/>
      <path d="M20 6 L28 2 L35 8 L32 16 L23 15 Z" fill="#eeeeeb" stroke="#5c5c5c" stroke-width="0.9"/>
      <path d="M14 24 L21 17 L29 21 L28 31 L18 33 Z" fill="#c9cbc8" stroke="#555" stroke-width="0.9"/>
      <path d="M31 22 L38 18 L42 26 L37 33 L29 31 Z" fill="#e4e4e1" stroke="#666" stroke-width="0.8"/>
      <path d="M4 26 L8 23 L11 28 L7 32 L2 30 Z" fill="#f2f2ef" stroke="#666" stroke-width="0.7"/>
      <path d="M5 12 L11 9 M22 7 L29 10 M17 26 L24 23 M32 25 L37 28" stroke="#9a9a96" stroke-width="0.55"/>
    </pattern>`;
    const blocks = `<pattern id="sov-pattern-blocks" width="50" height="42" patternUnits="userSpaceOnUse">
      <path d="M2 15 L10 3 L22 6 L24 18 L15 25 L5 23 Z" fill="#e2e3e0" stroke="#4f4f4f" stroke-width="1.1"/>
      <path d="M28 5 L40 2 L49 12 L45 24 L34 23 L26 16 Z" fill="#caced0" stroke="#4f4f4f" stroke-width="1.1"/>
      <path d="M18 29 L28 21 L40 27 L38 40 L25 42 L14 37 Z" fill="#f0f0ed" stroke="#575757" stroke-width="1"/>
      <path d="M8 9 L16 16 M32 8 L41 15 M22 31 L32 36" stroke="#8b8b88" stroke-width="0.7"/>
    </pattern>`;
    return svg
      .replace(/<pattern id="sov-pattern-debris"[\s\S]*?<\/pattern>/, debris)
      .replace(/<pattern id="sov-pattern-blocks"[\s\S]*?<\/pattern>/, blocks);
  }

  NacrtRenderer.render = function renderSymbolsPolish(survey, options = {}) {
    let svg = improvedPatterns(renderPrevious(survey, options));
    const profileTx = survey.profile ? makeTransform(getExtent(survey.profile, LAYOUT.profile.pad), LAYOUT.profile) : null;
    const planTx = survey.plan ? makeTransform(getExtent(survey.plan, LAYOUT.plan.pad), LAYOUT.plan) : null;
    const section = Array.isArray(survey.sections) ? survey.sections[0] : null;
    const sectionTx = section ? makeTransform(getExtent(section, LAYOUT.section.pad), LAYOUT.section) : null;
    const overlays = [
      renderPointSymbols(survey.profile, profileTx),
      renderPointSymbols(section, sectionTx),
      renderPointSymbols(survey.plan, planTx),
      renderDetectedTree(survey.profile, profileTx)
    ].filter(Boolean).join('');
    return svg.replace('</svg>', `${overlays}</svg>`);
  };

  if (typeof window !== 'undefined') window.SOV_NACRT_SYMBOLS = { version: '1.0' };
})();
