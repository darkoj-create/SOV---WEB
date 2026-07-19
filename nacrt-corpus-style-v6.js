/**
 * SOV Nacrt corpus-informed semantic style v6.
 * Derived from the 1,109-page SOV visual reference corpus plus paired
 * TopoDroid/PDF examples. Loaded after nacrt-semantic-layout.js.
 *
 * Principles:
 * - colour only when it carries semantic meaning
 * - cave void and generic rock remain white/black
 * - water blue, ice/snow cyan, vegetation green, wood brown
 * - sediment/materials use restrained print-safe patterns
 * - unknown objects keep the safe base-renderer fallback
 * - SVG only: no Canvas and no per-pixel processing
 */
(() => {
  'use strict';

  const C = window.SOVSemanticCore;
  const S = window.SOVSemanticStyles;
  if (!C || !S) throw new Error('Corpus style v6 traži semantic core i styles.');

  const PALETTE = Object.freeze({
    ink: '#171717',
    muted: '#666a6b',
    station: '#d32626',
    water: '#168fae',
    waterFill: '#dff4fa',
    ice: '#52abc2',
    iceFill: '#edfafe',
    snow: '#278eae',
    vegetation: '#397a43',
    vegetationDark: '#25542f',
    vegetationFill: '#dcebd7',
    wood: '#81552f',
    woodDark: '#4f341f',
    rock: '#cfd2d1',
    rockDark: '#55595a',
    debrisFill: '#ececea',
    sand: '#d8b979',
    sandFill: '#f7efd9',
    clay: '#b68b5e',
    clayFill: '#f0e1cf',
    soil: '#8e6c49',
    soilFill: '#eadcc8',
    calcite: '#c88c42',
    calciteFill: '#f6e5c9',
    guano: '#5b4634'
  });

  const originalDefs = S.defs.bind(S);
  const originalRenderLines = S.renderLines.bind(S);
  const originalRenderAreas = S.renderAreas.bind(S);
  const originalRenderPoints = S.renderPoints.bind(S);

  const norm = value => String(value || '').trim().toLowerCase().replace(/[_\s]+/g, '-');
  const isTree = type => /(?:^|:|-)(tree|conifer|fir|spruce|pine|broadleaf|deciduous|bush|shrub|vegetation|plant)(?:$|:|-)/.test(norm(type));

  function enhanceBaseDefs(svg) {
    return String(svg || '')
      .replace(/<pattern id="sov-sem-water"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-water" width="22" height="16" patternUnits="userSpaceOnUse">
          <rect width="22" height="16" fill="${PALETTE.waterFill}"/>
          <path d="M-2 4 Q3 0 8 4 T18 4 T28 4 M-2 11 Q3 7 8 11 T18 11 T28 11" fill="none" stroke="${PALETTE.water}" stroke-width="1.15"/>
        </pattern>`)
      .replace(/<pattern id="sov-sem-snow"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-snow" width="22" height="22" patternUnits="userSpaceOnUse">
          <rect width="22" height="22" fill="${PALETTE.iceFill}"/>
          <g stroke="${PALETTE.snow}" stroke-width="0.95">
            <path d="M6 3 V13 M2 6 L10 10 M10 6 L2 10"/>
            <path d="M17 10 V20 M13 13 L21 17 M21 13 L13 17"/>
          </g>
        </pattern>`)
      .replace(/<pattern id="sov-sem-sand"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-sand" width="12" height="12" patternUnits="userSpaceOnUse">
          <rect width="12" height="12" fill="${PALETTE.sandFill}"/>
          <circle cx="2" cy="2" r="0.7" fill="${PALETTE.sand}"/><circle cx="8" cy="5" r="0.6" fill="${PALETTE.sand}"/><circle cx="5" cy="10" r="0.55" fill="${PALETTE.sand}"/>
        </pattern>`)
      .replace(/<pattern id="sov-sem-clay"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-clay" width="16" height="14" patternUnits="userSpaceOnUse">
          <rect width="16" height="14" fill="${PALETTE.clayFill}"/>
          <path d="M-2 7 Q2 3 6 7 T14 7 T22 7" fill="none" stroke="${PALETTE.clay}" stroke-width="0.85"/>
        </pattern>`)
      .replace(/<pattern id="sov-sem-debris"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-debris" width="38" height="31" patternUnits="userSpaceOnUse">
          <rect width="38" height="31" fill="${PALETTE.debrisFill}"/>
          <path d="M3 20 L8 12 L15 16 L13 25 L6 27 Z M18 13 L25 7 L33 12 L30 21 L22 22 Z" fill="${PALETTE.rock}" stroke="${PALETTE.rockDark}" stroke-width="0.8"/>
          <circle cx="5" cy="5" r="0.9" fill="${PALETTE.muted}"/><circle cx="19" cy="27" r="0.75" fill="${PALETTE.muted}"/><circle cx="34" cy="27" r="0.8" fill="${PALETTE.muted}"/>
        </pattern>`)
      .replace(/<pattern id="sov-sem-blocks"[\s\S]*?<\/pattern>/,
        `<pattern id="sov-sem-blocks" width="50" height="43" patternUnits="userSpaceOnUse">
          <rect width="50" height="43" fill="#fff"/>
          <g fill="${PALETTE.rock}" stroke="${PALETTE.rockDark}" stroke-width="1">
            <path d="M3 15 L11 3 L23 6 L25 18 L16 26 L5 23 Z"/>
            <path d="M29 5 L41 2 L49 13 L45 25 L34 24 L26 16 Z"/>
            <path d="M17 31 L27 23 L40 29 L38 41 L25 43 L14 38 Z"/>
          </g>
          <g stroke="#8b8e8d" stroke-width="0.7"><path d="M10 8 L17 18 L8 20"/><path d="M34 7 L41 15 L31 20"/><path d="M23 29 L31 36"/></g>
        </pattern>`);
  }

  function extraDefs() {
    return `<defs id="sov-corpus-v6-defs">
      <pattern id="sov-v6-ice" width="24" height="22" patternUnits="userSpaceOnUse">
        <rect width="24" height="22" fill="${PALETTE.iceFill}"/>
        <path d="M3 18 L10 4 L15 14 L21 3" fill="none" stroke="${PALETTE.ice}" stroke-width="0.85"/>
        <path d="M6 5 L10 9 M16 6 L20 10" stroke="#8ed2e1" stroke-width="0.7"/>
      </pattern>
      <pattern id="sov-v6-gravel" width="18" height="16" patternUnits="userSpaceOnUse">
        <rect width="18" height="16" fill="#f4f4f1"/>
        <ellipse cx="4" cy="5" rx="2.2" ry="1.5" fill="#dadbd8" stroke="#696b6b" stroke-width="0.65"/>
        <ellipse cx="12" cy="4" rx="2.8" ry="1.7" fill="#e5e5e2" stroke="#696b6b" stroke-width="0.65"/>
        <ellipse cx="8" cy="12" rx="2.6" ry="1.6" fill="#d0d2d1" stroke="#696b6b" stroke-width="0.65"/>
        <ellipse cx="16" cy="12" rx="1.8" ry="1.2" fill="#ececea" stroke="#696b6b" stroke-width="0.6"/>
      </pattern>
      <pattern id="sov-v6-soil" width="16" height="16" patternUnits="userSpaceOnUse">
        <rect width="16" height="16" fill="${PALETTE.soilFill}"/>
        <circle cx="3" cy="4" r="0.8" fill="${PALETTE.soil}"/><circle cx="10" cy="3" r="0.65" fill="${PALETTE.soil}"/><circle cx="13" cy="11" r="0.75" fill="${PALETTE.soil}"/><circle cx="6" cy="13" r="0.6" fill="${PALETTE.soil}"/>
      </pattern>
      <pattern id="sov-v6-flowstone" width="20" height="17" patternUnits="userSpaceOnUse">
        <rect width="20" height="17" fill="${PALETTE.calciteFill}"/>
        <path d="M-2 5 Q3 1 8 5 T18 5 T28 5 M-2 12 Q3 8 8 12 T18 12 T28 12" fill="none" stroke="${PALETTE.calcite}" stroke-width="0.9"/>
      </pattern>
      <pattern id="sov-v6-vegetation" width="22" height="22" patternUnits="userSpaceOnUse">
        <rect width="22" height="22" fill="${PALETTE.vegetationFill}"/>
        <path d="M11 20 C9 14 5 11 3 6 C8 7 11 9 12 14 C13 9 16 5 20 3 C19 10 16 15 11 20Z" fill="none" stroke="${PALETTE.vegetation}" stroke-width="1"/>
      </pattern>
      <pattern id="sov-v6-wood" width="28" height="14" patternUnits="userSpaceOnUse">
        <rect width="28" height="14" fill="#b58a5c"/>
        <path d="M0 4 Q7 1 14 4 T28 4 M0 10 Q7 7 14 10 T28 10" fill="none" stroke="${PALETTE.woodDark}" stroke-width="0.8"/>
      </pattern>
      <pattern id="sov-v6-guano" width="14" height="14" patternUnits="userSpaceOnUse">
        <rect width="14" height="14" fill="#efe8de"/>
        <circle cx="3" cy="4" r="1" fill="${PALETTE.guano}"/><circle cx="10" cy="3" r="0.8" fill="${PALETTE.guano}"/><circle cx="7" cy="11" r="0.9" fill="${PALETTE.guano}"/>
      </pattern>
    </defs>`;
  }

  S.defs = function corpusDefs() {
    return enhanceBaseDefs(originalDefs()) + extraDefs();
  };

  function lineStyle(type) {
    const t = norm(type);
    if (/water|stream|river|flow/.test(t)) return { stroke: PALETTE.water, width: 1.55, arrow: /flow|arrow/.test(t) };
    if (/ice/.test(t)) return { stroke: PALETTE.ice, width: 1.65 };
    if (/wood|log|timber/.test(t)) return { stroke: PALETTE.wood, width: 3.2 };
    if (/rope/.test(t)) return { stroke: '#8a6a33', width: 1.25, dash: '3 3' };
    if (/slope|gradient/.test(t)) return { stroke: '#666', width: 1, dash: '5 4' };
    if (/crack|fissure/.test(t)) return { stroke: '#444', width: 1.05 };
    return null;
  }

  S.renderLines = function corpusLines(tdr, tx, all = false) {
    let svg = originalRenderLines(tdr, tx, all);
    if (!tdr || !tx) return svg;
    const extra = [];
    for (const line of tdr.lines || []) {
      const style = lineStyle(line.type);
      if (!style) continue;
      const d = C.pathData(line.pts, tx);
      if (!d) continue;
      extra.push(`<path d="${d}" fill="none" stroke="#fff" stroke-width="${style.width + 2.4}" stroke-linecap="round"/>`);
      extra.push(`<path d="${d}" fill="none" stroke="${style.stroke}" stroke-width="${style.width}"${style.dash ? ` stroke-dasharray="${style.dash}"` : ''} stroke-linecap="round" stroke-linejoin="round"/>`);
      if (style.arrow) {
        const p = C.arrowHead(line.pts, tx, 7);
        if (p) extra.push(`<polygon points="${p}" fill="${style.stroke}"/>`);
      }
    }
    return svg + (extra.length ? `<g class="sov-corpus-v6-lines">${extra.join('')}</g>` : '');
  };

  function areaCategory(type) {
    const t = norm(type);
    if (/^ice$|ice-floor|glacier/.test(t)) return 'ice';
    if (/gravel|pebble|cobble/.test(t)) return 'gravel';
    if (/soil|earth|loam|humus/.test(t)) return 'soil';
    if (/flowstone|calcite|moonmilk|formation/.test(t)) return 'flowstone';
    if (/vegetation|grass|plant|bush|shrub/.test(t)) return 'vegetation';
    if (/wood|log|timber|root/.test(t)) return 'wood';
    if (/guano|bat-dropping/.test(t)) return 'guano';
    if (/lake|pool|water|sump/.test(t)) return 'waterAlias';
    if (/snow/.test(t)) return 'snowAlias';
    if (/sand/.test(t)) return 'sandAlias';
    if (/clay|mud|silt/.test(t)) return 'clayAlias';
    if (/debris|rubble|scree/.test(t)) return 'debrisAlias';
    if (/block|boulder|rock/.test(t)) return 'blocksAlias';
    return '';
  }

  const areaPattern = category => ({
    ice: 'sov-v6-ice', gravel: 'sov-v6-gravel', soil: 'sov-v6-soil',
    flowstone: 'sov-v6-flowstone', vegetation: 'sov-v6-vegetation', wood: 'sov-v6-wood', guano: 'sov-v6-guano',
    waterAlias: 'sov-sem-water', snowAlias: 'sov-sem-snow', sandAlias: 'sov-sem-sand', clayAlias: 'sov-sem-clay',
    debrisAlias: 'sov-sem-debris', blocksAlias: 'sov-sem-blocks'
  })[category] || '';

  const exactAlreadyRendered = new Set(['debris','blocks','sand','clay','mud','water','snow']);

  S.renderAreas = function corpusAreas(tdr, tx) {
    let svg = originalRenderAreas(tdr, tx);
    if (!tdr || !tx) return svg;
    const extra = [];
    for (const area of tdr.areas || []) {
      const raw = norm(area.type);
      const category = areaCategory(raw);
      if (!category || exactAlreadyRendered.has(raw)) continue;
      const d = C.pathData(area.pts, tx);
      const pattern = areaPattern(category);
      if (d && pattern) extra.push(`<path d="${d} Z" fill="url(#${pattern})" stroke="${category === 'ice' ? PALETTE.ice : PALETTE.muted}" stroke-width="0.85"/>`);
    }
    return svg + (extra.length ? `<g class="sov-corpus-v6-areas">${extra.join('')}</g>` : '');
  };

  function conifer(x, y, width, height) {
    const w = Math.max(24, width), h = Math.max(64, height), cx = x + w / 2, base = y + h;
    const out = [`<g class="sov-v6-tree sov-v6-conifer">`,
      `<path d="M${cx-w*.04} ${y+h*.22} L${cx+w*.04} ${y+h*.22} L${cx+w*.07} ${base} L${cx-w*.07} ${base} Z" fill="${PALETTE.wood}" stroke="${PALETTE.woodDark}" stroke-width="0.9"/>`];
    const levels = [.16,.27,.39,.52,.66,.80,.92];
    levels.forEach((level, i) => {
      const top = y + h * (level - .14), bottom = y + h * level, half = w * (.12 + i * .052);
      const fill = i % 2 ? '#3d8249' : '#2f713e';
      out.push(`<path d="M${cx} ${top} L${cx-half*.55} ${bottom-h*.045} L${cx-half} ${bottom} L${cx} ${bottom+h*.025} L${cx+half} ${bottom} L${cx+half*.55} ${bottom-h*.045} Z" fill="${fill}" stroke="${PALETTE.vegetationDark}" stroke-width="0.7" stroke-linejoin="round"/>`);
    });
    out.push('</g>');
    return out.join('');
  }

  function broadleaf(x, y, width, height) {
    const w = Math.max(34, width), h = Math.max(58, height), cx = x + w / 2, base = y + h;
    return `<g class="sov-v6-tree sov-v6-broadleaf">
      <path d="M${cx-w*.045} ${y+h*.35} C${cx-w*.12} ${y+h*.57} ${cx-w*.09} ${y+h*.78} ${cx-w*.065} ${base} L${cx+w*.065} ${base} C${cx+w*.09} ${y+h*.78} ${cx+w*.12} ${y+h*.57} ${cx+w*.045} ${y+h*.35} Z" fill="${PALETTE.wood}" stroke="${PALETTE.woodDark}" stroke-width="0.9"/>
      <path d="M${cx} ${y+h*.48} C${cx-w*.08} ${y+h*.36} ${cx-w*.18} ${y+h*.31} ${cx-w*.26} ${y+h*.22} M${cx} ${y+h*.43} C${cx+w*.08} ${y+h*.32} ${cx+w*.18} ${y+h*.27} ${cx+w*.27} ${y+h*.18}" fill="none" stroke="${PALETTE.woodDark}" stroke-width="1"/>
      <path d="M${cx-w*.43} ${y+h*.25} C${cx-w*.46} ${y+h*.08} ${cx-w*.28} ${y-h*.01} ${cx-w*.12} ${y+h*.07} C${cx} ${y-h*.02} ${cx+w*.17} ${y+h*.01} ${cx+w*.22} ${y+h*.12} C${cx+w*.44} ${y+h*.10} ${cx+w*.5} ${y+h*.28} ${cx+w*.36} ${y+h*.38} C${cx+w*.2} ${y+h*.49} ${cx-w*.25} ${y+h*.48} ${cx-w*.43} ${y+h*.25} Z" fill="${PALETTE.vegetation}" stroke="${PALETTE.vegetationDark}" stroke-width="0.9"/>
    </g>`;
  }

  S.conifer = conifer;
  S.broadleaf = broadleaf;

  function pointGlyph(pt, p, size) {
    const t = norm(pt.type || pt.name);
    const s = size;
    if (/root/.test(t)) return `<g stroke="${PALETTE.wood}" stroke-width="1.3" fill="none"><path d="M${p.x} ${p.y-s} L${p.x} ${p.y+s*.2} M${p.x} ${p.y} L${p.x-s*.8} ${p.y+s} M${p.x} ${p.y+s*.1} L${p.x+s*.8} ${p.y+s} M${p.x-s*.25} ${p.y+s*.3} L${p.x-s} ${p.y+s*.65}"/></g>`;
    if (/wood|log|timber/.test(t)) return `<g transform="translate(${p.x} ${p.y}) rotate(${Number(pt.orientation||0)})"><rect x="${-s*1.2}" y="${-s*.28}" width="${s*2.4}" height="${s*.56}" rx="${s*.15}" fill="#b58a5c" stroke="${PALETTE.woodDark}"/><path d="M${-s*.8} 0 Q${-s*.3} ${-s*.18} ${s*.2} 0 T${s*.9} 0" fill="none" stroke="${PALETTE.woodDark}" stroke-width="0.7"/></g>`;
    if (/water-flow|spring|resurgence|stream/.test(t)) return `<g transform="translate(${p.x} ${p.y}) rotate(${Number(pt.orientation||0)})" stroke="${PALETTE.water}" fill="${PALETTE.water}"><path d="M0 ${s} L0 ${-s*.85}" stroke-width="1.4"/><path d="M0 ${-s} L${-s*.4} ${-s*.35} L${s*.4} ${-s*.35} Z"/><path d="M${-s*.8} ${s*.45} Q${-s*.4} ${s*.1} 0 ${s*.45} T${s*.8} ${s*.45}" fill="none" stroke-width="0.9"/></g>`;
    if (/stalactite/.test(t)) return `<path d="M${p.x-s} ${p.y-s*.65} L${p.x+s} ${p.y-s*.65} L${p.x} ${p.y+s} Z" fill="#fff" stroke="${PALETTE.ink}" stroke-width="1"/>`;
    if (/stalagmite/.test(t)) return `<path d="M${p.x-s} ${p.y+s*.65} L${p.x+s} ${p.y+s*.65} L${p.x} ${p.y-s} Z" fill="#fff" stroke="${PALETTE.ink}" stroke-width="1"/>`;
    if (/column|pillar-formation/.test(t)) return `<path d="M${p.x-s*.45} ${p.y-s} Q${p.x} ${p.y-s*.7} ${p.x+s*.45} ${p.y-s} L${p.x+s*.45} ${p.y+s} Q${p.x} ${p.y+s*.7} ${p.x-s*.45} ${p.y+s} Z" fill="${PALETTE.calciteFill}" stroke="${PALETTE.calcite}" stroke-width="1"/>`;
    if (/crystal/.test(t)) return `<path d="M${p.x} ${p.y-s} L${p.x+s*.65} ${p.y} L${p.x} ${p.y+s} L${p.x-s*.65} ${p.y} Z" fill="#fff" stroke="${PALETTE.calcite}" stroke-width="1"/>`;
    if (/guano/.test(t)) return `<g fill="${PALETTE.guano}"><circle cx="${p.x-s*.5}" cy="${p.y}" r="${s*.22}"/><circle cx="${p.x}" cy="${p.y-s*.25}" r="${s*.25}"/><circle cx="${p.x+s*.5}" cy="${p.y+s*.1}" r="${s*.2}"/></g>`;
    if (/danger|hazard/.test(t)) return `<g><path d="M${p.x} ${p.y-s} L${p.x+s*.9} ${p.y+s*.75} L${p.x-s*.9} ${p.y+s*.75} Z" fill="#fff" stroke="#111" stroke-width="1.2"/><text x="${p.x}" y="${p.y+s*.43}" text-anchor="middle" font-size="${s*1.25}" font-weight="700">!</text></g>`;
    return '';
  }

  function renderImprovedTreePoints(tdr, tx) {
    if (!tdr || !tx) return '';
    const out = [];
    for (const pt of tdr.points || []) {
      const type = norm(pt.type || pt.name);
      if (!isTree(type)) continue;
      const p = tx(pt.x, pt.y);
      const s = Math.max(6, 7 + (Number(pt.scale || 0) + 2) * 1.2);
      if (/broadleaf|deciduous|bush|shrub/.test(type)) out.push(broadleaf(p.x-s*1.7, p.y-s*4.7, s*3.4, s*5));
      else out.push(conifer(p.x-s*1.5, p.y-s*5.4, s*3, s*5.7));
    }
    return out.join('');
  }

  S.renderPoints = function corpusPoints(tdr, tx) {
    if (!tdr || !tx) return originalRenderPoints(tdr, tx);
    const filtered = { ...tdr, points: (tdr.points || []).filter(pt => !isTree(pt.type || pt.name)) };
    let svg = originalRenderPoints(filtered, tx);
    const extra = [renderImprovedTreePoints(tdr, tx)];
    for (const pt of tdr.points || []) {
      const type = norm(pt.type || pt.name);
      if (isTree(type)) continue;
      const p = tx(pt.x, pt.y);
      const s = Math.max(5, 6 + (Number(pt.scale || 0) + 2) * 1.05);
      const glyph = pointGlyph(pt, p, s);
      if (glyph) extra.push(glyph);
    }
    return svg + (extra.some(Boolean) ? `<g class="sov-corpus-v6-points">${extra.join('')}</g>` : '');
  };

  window.SOV_NACRT_CORPUS_STYLE = {
    version: '6.0',
    corpusPages: 1109,
    palette: PALETTE,
    rule: 'colour-is-semantic-only'
  };
})();
