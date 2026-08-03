/**
 * SOV Nacrt corpus-informed drawing style v7.
 *
 * Naučeno iz stvarnih, dovršenih nacrta ekspedicije "Sjeverni Velebit 2025/2026"
 * (74 nacrta, 14 Therion .th2 scrapova, 30 Therion SVG izvoza, 93 DXF-a).
 * Referentni objekti: 05-1210 Klekača, 05-1198 Utješna, 05-1199 Balov,
 * 05-1163 But, 05-1170 Zzzzzz.
 *
 * Učitava se POSLIJE nacrt-corpus-style-v6.js.
 *
 * Što v7 ispravlja u odnosu na v6 (sve provjereno na gornjim nacrtima):
 *  1. Sjena stijenki  — meki sivi pomak ispod zidova ("sjena"), potpis SOV/UIS crteža.
 *  2. Točke poligona  — mali crveni romb + sitan broj umjesto debelih crvenih kolutova.
 *  3. Poligonski vlak — tanka svijetlosiva linija umjesto jarko crvene.
 *  4. Ulaz            — puni crni trokut + oznaka ULAZ.
 *  5. Mjerilo         — graduirana crno-bijela ljestvica + "M 1:xxx" (izračunat omjer).
 *  6. Sjever          — pravi kompasni ružin simbol umjesto krug+strelica.
 *  7. Dubina          — kota najdublje točke ("-10 m") uz dno profila.
 *  8. Ježevi na skoku — gušće i kraće crtice (spacing 6.5) kao na Klekači/Utješnoj.
 *  9. Kosina          — izmjenične duge/kratke crtice (klasična oznaka kosine).
 * 10. Blokovi         — pojedinačni sivi poligoni s bridovima, ne generički raster.
 * 11. Težine linija   — zid 2.0, pretpostavljeni zid 1.5, granica 0.9.
 *
 * SVG only: bez Canvasa i bez per-pixel obrade.
 */
(() => {
  'use strict';

  const C = window.SOVSemanticCore;
  const S = window.SOVSemanticStyles;
  if (!C || !S) throw new Error('Corpus style v7 traži semantic core i styles.');
  if (typeof NacrtRenderer === 'undefined' || typeof NacrtRenderer.render !== 'function') {
    throw new Error('Corpus style v7 traži učitan nacrt-semantic-layout.js.');
  }

  /* ------------------------------------------------------------------ *
   * Konstante izvedene iz korpusa
   * ------------------------------------------------------------------ */

  // Platno je A4 portret: 1240 × 1754 px za 210 × 297 mm.
  const PX_PER_MM = 1240 / 210;
  // TopoDroid/Therion jedinica: 20 jedinica = 1 m (isto kao u nacrt-v2.js).
  const TDR_PER_M = 20;

  // Isti okviri kao u nacrt-semantic-layout.js — v7 ih samo čita.
  const L = {
    profile: { left: 105, top: 352, width: 640, height: 1300, pad: 18 },
    section: { left: 835, top: 500, width: 270, height: 300, pad: 12 },
    plan:    { left: 760, top: 810, width: 430, height: 500, pad: 18 }
  };

  const INK = Object.freeze({
    wall: '#141414',
    shadow: '#0d0d0d',
    station: '#e0231c',
    centerline: '#bcb9b5',
    note: '#1f1f1f',
    rule: '#111111',
    boulder: '#c9cac7',
    boulderEdge: '#1c1c1c',
    boulderFacet: '#8e908d',
    slope: '#5a5550'
  });

  // Led i snijeg: na 05-1187 Ledeno gnijezdo crtaju se raspršene cijan zvjezdice,
  // ne puna rasterska ispuna.
  const ICE = Object.freeze({ line: '#2aa8cc', mark: '#29a6ca' });

  // Mjerila koja SOV stvarno koristi na terenskim nacrtima.
  const SCALE_RATIOS = [50, 100, 125, 200, 250, 500, 1000, 2000];

  const norm = v => String(v || '').trim().toLowerCase().replace(/[_\s]+/g, '-');
  const isBoulder = t => /^(blocks|block|rock|boulder|boulders)$/.test(norm(t));
  const num = v => (Number.isFinite(Number(v)) ? Number(v) : 0);
  const hr = n => String(n).replace('.', ',');

  /* ------------------------------------------------------------------ *
   * 1. Težine linija — kalibrirano prema 05-1210 i 05-1198
   * ------------------------------------------------------------------ */

  // S.LINE je isti objekt koji base renderer čita iz svog closurea,
  // pa ga mutacijom stvarno mijenjamo (bez prepisivanja renderera).
  if (S.LINE) {
    Object.assign(S.LINE.wall || {}, { stroke: INK.wall, width: 2.0 });
    Object.assign(S.LINE['wall:presumed'] || {}, { width: 1.5, dash: '6 5' });
    Object.assign(S.LINE.border || {}, { stroke: '#8a8783', width: 0.9, dash: '3 3' });
    Object.assign(S.LINE.overhang || {}, { width: 1.4, dash: '9 4' });
    // Ježeve na skoku (pit) preuzima v7 — gušće su nego u baznom rendereru.
    if (S.LINE.pit) { S.LINE.pit.ticks = 0; S.LINE.pit.width = 1.6; }
    // Tipovi koje bazni registar nema — bez ovoga padaju na generički sivi default.
    S.LINE.contour = S.LINE.contour || { stroke: '#9b9894', width: 0.7 };
    S.LINE['floor-step'] = S.LINE['floor-step'] || { stroke: '#4b433a', width: 1.1 };
    S.LINE['ceiling-step'] = S.LINE['ceiling-step'] || { stroke: '#4b433a', width: 1.1, dash: '4 3' };
    S.LINE['wall:ice'] = S.LINE['wall:ice'] || { stroke: ICE.line, width: 1.9 };
    S.LINE['water-flow'] = S.LINE['water-flow'] || { stroke: '#168fae', width: 1.4, arrow: 1 };
  }

  /* ------------------------------------------------------------------ *
   * 2. Defs — filter za sjenu stijenki
   * ------------------------------------------------------------------ */

  // Bez SVG filtera: svg2pdf.js (vektorski PDF izvoz) filtere ne renderira,
  // pa se sjena radi slojevitim konturama koje prežive izvoz.
  const prevDefs = S.defs.bind(S);
  S.defs = function corpusDefsV7() {
    return prevDefs() + `<defs id="sov-v7-defs">
<pattern id="sov-v7-stipple" width="13" height="13" patternUnits="userSpaceOnUse">
<circle cx="2.5" cy="3" r="0.62" fill="#4a4744"/><circle cx="8.5" cy="1.5" r="0.5" fill="#4a4744"/>
<circle cx="11" cy="7" r="0.58" fill="#4a4744"/><circle cx="5" cy="8.5" r="0.55" fill="#4a4744"/>
<circle cx="1.5" cy="11" r="0.5" fill="#4a4744"/><circle cx="8" cy="11.5" r="0.6" fill="#4a4744"/>
</pattern>
</defs>`;
  };

  /* ------------------------------------------------------------------ *
   * 3. Linije — sjena zidova, gusti ježevi, kosina
   * ------------------------------------------------------------------ */

  function wallShadow(tdr, tx) {
    if (!tdr || !tx) return '';
    const paths = [];
    for (const line of tdr.lines || []) {
      const t = norm(line.type);
      if (t !== 'wall' && t !== 'wall:ice') continue;
      const d = C.pathData(line.pts, tx);
      if (d) paths.push(d);
    }
    if (!paths.length) return '';
    // Tri sloja opadajuće širine daju mekani rub bez feGaussianBlur-a.
    const body = paths.map(d => `<path d="${d}"/>`).join('');
    const layers = [[13, 0.05], [9.5, 0.07], [6.5, 0.10], [4, 0.13]];
    return `<g class="sov-v7-sjena" transform="translate(2.4,3.2)" fill="none" stroke="${INK.shadow}" stroke-linecap="round" stroke-linejoin="round">` +
      layers.map(([w, o]) => `<g stroke-width="${w}" stroke-opacity="${o}">${body}</g>`).join('') +
      '</g>';
  }

  function pitTicks(tdr, tx) {
    if (!tdr || !tx) return '';
    const parts = [];
    for (const line of tdr.lines || []) {
      if (norm(line.type) !== 'pit') continue;
      const side = (line.reversed ? -1 : 1) * (line.lside === 1 ? -1 : 1);
      const d = C.lineTicks(line.pts, tx, { side, spacing: 6.5, length: 6.5 });
      if (d) parts.push(`<path d="${d}" fill="none" stroke="${INK.wall}" stroke-width="1"/>`);
    }
    return parts.join('');
  }

  // Kosina: izmjenično duga i kratka crtica okomito na liniju pada.
  function slopeHachures(tdr, tx) {
    if (!tdr || !tx) return '';
    const parts = [];
    for (const line of tdr.lines || []) {
      const t = norm(line.type);
      if (!/^(slope|gradient)$/.test(t)) continue;
      const side = (line.reversed ? -1 : 1) * (line.lside === 1 ? -1 : 1);
      const long = C.lineTicks(line.pts, tx, { side, spacing: 13, length: 11 });
      const short = C.lineTicks(line.pts, tx, { side, spacing: 6.5, length: 4.5 });
      if (long) parts.push(`<path d="${long}" fill="none" stroke="${INK.slope}" stroke-width="0.95"/>`);
      if (short) parts.push(`<path d="${short}" fill="none" stroke="${INK.slope}" stroke-width="0.8"/>`);
    }
    return parts.join('');
  }

  const prevRenderLines = S.renderLines.bind(S);
  S.renderLines = function corpusLinesV7(tdr, tx, all = false) {
    const base = prevRenderLines(tdr, tx, all);
    if (!tdr || !tx) return base;
    const after = pitTicks(tdr, tx) + slopeHachures(tdr, tx);
    return wallShadow(tdr, tx) + base + (after ? `<g class="sov-v7-lines">${after}</g>` : '');
  };

  /* ------------------------------------------------------------------ *
   * 4. Blokovi — pojedinačni poligoni s bridovima
   * ------------------------------------------------------------------ */

  // Gromade na 05-1173 i 05-1198 su krupne, oštro bridaste i s vidljivim plohama.
  function boulder(x, y, s, seed) {
    const n = 5 + (seed % 2); // 5–6 vrhova: oštriji brid nego kod 7-kuta
    const pts = [];
    for (let i = 0; i < n; i++) {
      const a = (Math.PI * 2 * i) / n + ((seed % 13) / 13) * 1.2;
      const w = 0.74 + (((seed + 1) * 37 + i * 53) % 34) / 100;
      pts.push([x + Math.cos(a) * s * w, y + Math.sin(a) * s * w * 0.9]);
    }
    const poly = pts.map(p => `${p[0].toFixed(1)},${p[1].toFixed(1)}`).join(' ');
    // Unutarnja ploha: vrh -> težište pomaknuto -> drugi vrh.
    const gx = x + (pts[0][0] - x) * 0.18, gy = y + (pts[0][1] - y) * 0.18;
    const k = Math.min(2, n - 1), m = Math.min(n - 1, k + 2);
    const facets = `M${pts[0][0].toFixed(1)},${pts[0][1].toFixed(1)} L${gx.toFixed(1)},${gy.toFixed(1)} L${pts[k][0].toFixed(1)},${pts[k][1].toFixed(1)}` +
                   ` M${gx.toFixed(1)},${gy.toFixed(1)} L${pts[m][0].toFixed(1)},${pts[m][1].toFixed(1)}`;
    return `<g class="sov-v7-blok"><polygon points="${poly}" fill="${INK.boulder}" stroke="${INK.boulderEdge}" stroke-width="1.2" stroke-linejoin="miter"/><path d="${facets}" fill="none" stroke="${INK.boulderFacet}" stroke-width="0.85"/></g>`;
  }

  // Snijeg/led: zvjezdica sa šest krakova (UIS), cijan.
  function iceStar(x, y, s) {
    const d = [];
    for (let i = 0; i < 3; i++) {
      const a = (Math.PI * i) / 3, dx = Math.cos(a) * s, dy = Math.sin(a) * s;
      d.push(`M${(x - dx).toFixed(1)},${(y - dy).toFixed(1)} L${(x + dx).toFixed(1)},${(y + dy).toFixed(1)}`);
    }
    return `<path d="${d.join(' ')}" stroke="${ICE.mark}" stroke-width="1.15" stroke-linecap="round"/>`;
  }

  // Raspoređivanje zvjezdica po poligonu površine (rešetka + test unutarnjosti).
  function pointInPoly(px, py, poly) {
    let inside = false;
    for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
      const xi = poly[i][0], yi = poly[i][1], xj = poly[j][0], yj = poly[j][1];
      if ((yi > py) !== (yj > py) && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-9) + xi) inside = !inside;
    }
    return inside;
  }

  function scatterIce(pts, tx) {
    const poly = pts.map(p => { const q = tx(p.x, p.y); return [q.x, q.y]; });
    if (poly.length < 3) return '';
    let xmin = Infinity, xmax = -Infinity, ymin = Infinity, ymax = -Infinity;
    for (const [x, y] of poly) { xmin = Math.min(xmin, x); xmax = Math.max(xmax, x); ymin = Math.min(ymin, y); ymax = Math.max(ymax, y); }
    const step = 34, out = [];
    for (let y = ymin + step / 2; y < ymax; y += step) {
      for (let x = xmin + step / 2; x < xmax; x += step) {
        const jx = x + ((Math.round(x + y) % 11) - 5), jy = y + ((Math.round(x * 2 + y) % 11) - 5);
        if (pointInPoly(jx, jy, poly)) out.push(iceStar(jx, jy, 5));
      }
    }
    return out.join('');
  }

  /* ------------------------------------------------------------------ *
   * 5. Točke — blokove crtamo sami, ostalo prepuštamo v6 lancu
   * ------------------------------------------------------------------ */

  const isIce = t => /^(snow|ice|ice-floor|glacier|firn)$/.test(norm(t));

  const prevRenderPoints = S.renderPoints.bind(S);
  S.renderPoints = function corpusPointsV7(tdr, tx) {
    if (!tdr || !tx) return prevRenderPoints(tdr, tx);
    const mine = p => isBoulder(p.type || p.name) || isIce(p.type || p.name);
    const rest = { ...tdr, points: (tdr.points || []).filter(p => !mine(p)) };
    let svg = prevRenderPoints(rest, tx);
    const out = [];
    let seed = 0;
    for (const pt of tdr.points || []) {
      const p = tx(pt.x, pt.y);
      if (isBoulder(pt.type || pt.name)) {
        out.push(boulder(p.x, p.y, Math.max(6, 8 + (num(pt.scale) + 2) * 1.6), seed++));
      } else if (isIce(pt.type || pt.name)) {
        out.push(iceStar(p.x, p.y, Math.max(4, 5 + num(pt.scale) * 0.9)));
      }
    }
    return svg + (out.length ? `<g class="sov-v7-tocke">${out.join('')}</g>` : '');
  };

  /* ------------------------------------------------------------------ *
   * 5b. Površine — led/snijeg kao zvjezdice, sitni sediment kao stipple
   * ------------------------------------------------------------------ */

  // Crta se preko postojećeg crteža, pa je ispuna prozirna — samo rub i simboli.
  function iceAndStipple(tdr, tx) {
    if (!tdr || !tx) return '';
    const out = [];
    for (const area of tdr.areas || []) {
      const t = norm(area.type);
      const d = C.pathData(area.pts, tx);
      if (!d) continue;
      if (isIce(t)) {
        out.push(`<path d="${d} Z" fill="none" stroke="${ICE.line}" stroke-width="1.2"/>`);
        out.push(scatterIce(area.pts, tx));
      }
    }
    return out.join('');
  }

  const prevRenderAreas = S.renderAreas.bind(S);
  S.renderAreas = function corpusAreasV7(tdr, tx) {
    const extra = iceAndStipple(tdr, tx);
    return prevRenderAreas(tdr, tx) + (extra ? `<g class="sov-v7-povrsine">${extra}</g>` : '');
  };

  // nacrt-semantic-layout.js zove S.renderAreas samo za dodatne scrapove,
  // pa glavni tlocrt/profil/presjek moramo pokriti sami.
  function areaOverlay(survey) {
    if (!survey) return '';
    const targets = [
      [survey.profile, L.profile],
      [survey.plan, L.plan],
      [Array.isArray(survey.sections) ? survey.sections[0] : null, L.section]
    ];
    const out = [];
    for (const [tdr, box] of targets) {
      if (!tdr) continue;
      const tx = C.makeTransform(C.getExtent(tdr, box.pad), box);
      if (tx) out.push(iceAndStipple(tdr, tx));
    }
    const joined = out.filter(Boolean).join('');
    return joined ? `<g class="sov-v7-povrsine">${joined}</g>` : '';
  }

  /* ------------------------------------------------------------------ *
   * 6. Mjerilo i sjever — graduirano, s omjerom
   * ------------------------------------------------------------------ */

  function nearestRatio(raw) {
    let best = SCALE_RATIOS[0], bestDiff = Infinity;
    for (const r of SCALE_RATIOS) {
      const diff = Math.abs(Math.log(r / raw));
      if (diff < bestDiff) { bestDiff = diff; best = r; }
    }
    return best;
  }

  function graduatedScaleBar(x, y, metres, pixPerM) {
    const total = metres * pixPerM;
    const ratio = nearestRatio((1000 * PX_PER_MM) / pixPerM);
    // Podjela: 5 polja ako je moguće, inače 4.
    const segs = metres % 5 === 0 ? 5 : 4;
    const step = metres / segs;
    const h = 5.5;
    const parts = [`<g class="sov-v7-mjerilo" font-family="Arial, sans-serif" fill="${INK.rule}">`];
    for (let i = 0; i < segs; i++) {
      const x0 = x + (i * total) / segs;
      parts.push(`<rect x="${x0.toFixed(1)}" y="${y.toFixed(1)}" width="${(total / segs).toFixed(1)}" height="${h}" fill="${i % 2 === 0 ? INK.rule : '#fff'}" stroke="${INK.rule}" stroke-width="0.9"/>`);
    }
    for (let i = 0; i <= segs; i++) {
      const x0 = x + (i * total) / segs;
      const label = hr(Number((i * step).toFixed(2)));
      parts.push(`<line x1="${x0.toFixed(1)}" y1="${(y - 4).toFixed(1)}" x2="${x0.toFixed(1)}" y2="${y.toFixed(1)}" stroke="${INK.rule}" stroke-width="0.9"/>`);
      parts.push(`<text x="${x0.toFixed(1)}" y="${(y - 7).toFixed(1)}" text-anchor="middle" font-size="12">${label}</text>`);
    }
    parts.push(`<text x="${(x + total / 2).toFixed(1)}" y="${(y + h + 22).toFixed(1)}" text-anchor="middle" font-size="17">M 1:${ratio}</text>`);
    parts.push('</g>');
    return parts.join('');
  }

  function compassRose(x, y) {
    return `<g class="sov-v7-sjever" transform="translate(${x} ${y})" font-family="Arial, sans-serif">
<circle cx="0" cy="0" r="21" fill="#fff" stroke="${INK.rule}" stroke-width="1.1"/>
<circle cx="0" cy="0" r="14.5" fill="none" stroke="#9a9793" stroke-width="0.7"/>
<polygon points="0,-33 8.5,10 0,3.5" fill="${INK.rule}"/>
<polygon points="0,-33 -8.5,10 0,3.5" fill="#fff" stroke="${INK.rule}" stroke-width="1"/>
<polygon points="0,26 6,-4 0,1" fill="#c9c6c2" stroke="#9a9793" stroke-width="0.7"/>
<polygon points="0,26 -6,-4 0,1" fill="#fff" stroke="#9a9793" stroke-width="0.7"/>
<text x="0" y="-38" text-anchor="middle" font-size="18" font-weight="700" fill="${INK.rule}">N</text>
</g>`;
  }

  /* ------------------------------------------------------------------ *
   * 7. Post-obrada gotovog SVG-a
   * ------------------------------------------------------------------ */

  // Točke poligona: mali crveni romb + sitan broj (kao 05-1199 i 05-1210).
  const RE_STATION = /<g><circle cx="(-?[\d.]+)" cy="(-?[\d.]+)" r="5\.2"[^>]*\/><circle[^>]*\/><text x="(-?[\d.]+)" y="(-?[\d.]+)"[^>]*fill="#e12a22">([\s\S]*?)<\/text><\/g>/g;

  function slimStations(svg) {
    return svg.replace(RE_STATION, (_m, cx, cy, _tx, _ty, name) => {
      const x = Number(cx), y = Number(cy), r = 2.7;
      return `<g class="sov-v7-tocka"><path d="M${x.toFixed(1)},${(y - r).toFixed(1)} L${(x + r).toFixed(1)},${y.toFixed(1)} L${x.toFixed(1)},${(y + r).toFixed(1)} L${(x - r).toFixed(1)},${y.toFixed(1)} Z" fill="${INK.station}"/><text x="${(x + 4.6).toFixed(1)}" y="${(y - 3.4).toFixed(1)}" font-size="9.5" fill="${INK.station}">${name}</text></g>`;
    });
  }

  // Poligonski vlak: tanka svijetlosiva linija.
  function calmCenterline(svg) {
    return svg.replace(/stroke="#e12a22" stroke-width="1\.45"/g,
      `stroke="${INK.centerline}" stroke-width="0.8"`);
  }

  // Ulaz: puni crni trokut (bazni renderer ga preko brandinga ostavlja bijelim).
  function inkEntrance(svg) {
    return svg.replace(/<polygon points="0,-([\d.]+) ([\d.]+),([\d.]+) -([\d.]+),\3" fill="#fff" stroke="#111" stroke-width="1"\/>/g,
      (_m, a, b, c) => `<polygon points="0,-${a} ${b},${c} -${b},${c}" fill="${INK.rule}" stroke="${INK.rule}" stroke-width="1"/>`);
  }

  // Footer "SOV Nacrt Generator v2" ostaje na crtežu jer nacrt-branding.js traži
  // x="34", a nacrt-v2.js ga crta na x="46" (M+12). Ovdje ga mičemo pouzdano.
  // Urušenje: korpus (05-1173, 05-1198) crta sitan točkasti raster + zasebne
  // gromade, ne krupni kameni uzorak iz v6. Zamjena, ne dodavanje.
  function fineDebris(svg) {
    return svg.replace(/url\(#sov-sem-debris\)/g, 'url(#sov-v7-stipple)');
  }

  function dropGeneratorFooter(svg) {
    return svg
      .replace(/<text x="[\d.]+" y="1732"[^>]*>SOV Nacrt Generator v2<\/text>/g, '')
      .replace(/<text x="[\d.]+" y="1732"[^>]*>TopoDroid → SVG \/ PNG<\/text>/g, '');
  }

  // Mjerilo iz nacrt-v2.js zamjenjujemo graduiranim.
  const RE_SCALEBAR = /<g><line x1="([\d.]+)" y1="([\d.]+)" x2="([\d.]+)" y2="\2" stroke="#111" stroke-width="3"\/><line[^>]*\/><line[^>]*\/><text[^>]*>(\d+) m<\/text><\/g>/;

  function upgradeScaleBar(svg) {
    return svg.replace(RE_SCALEBAR, (_m, x1, y, x2, metres) => {
      const x = Number(x1), width = Number(x2) - x, m = Number(metres);
      if (!(width > 0) || !(m > 0)) return _m;
      return graduatedScaleBar(x, Number(y) + 6, m, width / m);
    });
  }

  // Sjever iz nacrt-v2.js zamjenjujemo kompasnom ružom.
  const RE_NORTH = /<g transform="translate\((-?[\d.]+) (-?[\d.]+)\)"><circle cx="0" cy="0" r="28"[^>]*\/><polygon points="0,-55 12,28 0,18 -12,28"[^>]*\/><text[^>]*>N<\/text><\/g>/;

  function upgradeNorth(svg) {
    return svg.replace(RE_NORTH, (_m, x, y) => compassRose(Number(x), Number(y)));
  }

  /* ------------------------------------------------------------------ *
   * 8. Kote: dubina dna i oznaka ULAZ
   * ------------------------------------------------------------------ */

  // Kota razine: kratka vodoravna crta + puni trokut + oznaka lijevo od nje.
  // Konvencija s 05-1248 Krumpirić (2021) i 05-1187 Ledeno gnijezdo.
  function datumMark(x, y, label) {
    const w = 26, t = 4.4;
    return `<g class="sov-v7-kota" font-family="Arial, sans-serif">` +
      `<line x1="${x.toFixed(1)}" y1="${y.toFixed(1)}" x2="${(x + w).toFixed(1)}" y2="${y.toFixed(1)}" stroke="${INK.note}" stroke-width="1"/>` +
      `<polygon points="${(x + w / 2 - t).toFixed(1)},${(y - t * 1.6).toFixed(1)} ${(x + w / 2 + t).toFixed(1)},${(y - t * 1.6).toFixed(1)} ${(x + w / 2).toFixed(1)},${y.toFixed(1)}" fill="${INK.note}"/>` +
      `<text x="${(x - 5).toFixed(1)}" y="${(y + 4).toFixed(1)}" text-anchor="end" font-size="12.5" fill="${INK.note}">${C.esc(label)}</text>` +
      `</g>`;
  }

  function depthAnnotations(survey) {
    if (!survey || !survey.profile) return '';
    const tx = C.makeTransform(C.getExtent(survey.profile, L.profile.pad), L.profile);
    if (!tx) return '';
    const parts = [];

    const stations = (survey.profile.stations || []).filter(s => Number.isFinite(s.x) && Number.isFinite(s.y));
    const depth = num(survey.sql && survey.sql.stats && survey.sql.stats.dubina);
    if (stations.length && depth > 0) {
      const deepest = stations.reduce((a, b) => (b.y > a.y ? b : a), stations[0]);
      const p = tx(deepest.x, deepest.y);
      const label = hr((Math.round(depth * 10) / 10).toFixed(1));
      parts.push(datumMark(p.x + 16, p.y, `-${label} m`));
    }

    const entrance = (survey.profile.points || []).find(pt => norm(pt.type || pt.name) === 'entrance');
    if (entrance) {
      const p = tx(entrance.x, entrance.y);
      // "kota ulaza" — ista razina s trokutom kao i kota dna, s nulom.
      // Potvrđeno na Špilji Veternici (CRO Speleo) i 05-1248 Krumpirić.
      parts.push(datumMark(p.x + 16, p.y, '0'));
      // Korpus piše "Ulaz" (05-1187, 05-1198), ne verzalom.
      parts.push(`<text class="sov-v7-ulaz" x="${(p.x + 12).toFixed(1)}" y="${(p.y - 13).toFixed(1)}" font-family="Arial, sans-serif" font-size="13" fill="${INK.note}">Ulaz</text>`);
    }

    return parts.join('');
  }

  /* ------------------------------------------------------------------ *
   * 9. Ulančavanje na postojeći renderer
   * ------------------------------------------------------------------ */

  const prevRender = NacrtRenderer.render.bind(NacrtRenderer);

  NacrtRenderer.render = function renderCorpusV7(survey, options = {}) {
    let svg = prevRender(survey, options);
    svg = slimStations(svg);
    svg = calmCenterline(svg);
    svg = inkEntrance(svg);
    svg = upgradeScaleBar(svg);
    svg = upgradeNorth(svg);
    svg = fineDebris(svg);
    svg = dropGeneratorFooter(svg);
    const tail = areaOverlay(survey);
    const notes = depthAnnotations(survey);
    if (tail || notes) {
      svg = svg.replace('</svg>', `${tail}<g class="sov-v7-kote">${notes}</g></svg>`);
    }
    return svg;
  };

  window.SOV_NACRT_CORPUS_STYLE_V7 = {
    version: '7.1',
    vectorSafe: true, // bez SVG filtera — preživi svg2pdf vektorski izvoz
    basedOn: 'Sjeverni Velebit 2025/2026 — 74 nacrta, 14 .th2, 30 Therion SVG, 93 DXF',
    references: ['05-1210 Klekača', '05-1198 Utješna', '05-1199 Balov', '05-1163 But', '05-1170 Zzzzzz'],
    pxPerMm: PX_PER_MM,
    tdrPerMetre: TDR_PER_M,
    scaleRatios: SCALE_RATIOS,
    rule: 'crtaj kako crta SOV, ne kako crta parser'
  };
})();
