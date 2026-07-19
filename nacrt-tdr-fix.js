/**
 * Compatibility parser for TopoDroid TDR files.
 * Loaded after nacrt.js and replaces only TdrParser.parse.
 */
(() => {
  if (typeof TdrParser === 'undefined') {
    throw new Error('TdrParser nije učitan prije nacrt-tdr-fix.js');
  }

  function parse(arrayBuffer, sourceName = 'TDR datoteka') {
    const buf = new Uint8Array(arrayBuffer);
    const dv = new DataView(arrayBuffer);
    const decoder = new TextDecoder('utf-8');
    let off = 0;
    let currentTag = '?';
    let currentTagOffset = 0;

    const fail = message => {
      throw new Error(`${sourceName}: zapis ${currentTag} na offsetu ${currentTagOffset}: ${message}`);
    };
    const need = (count, label) => {
      if (!Number.isInteger(count) || count < 0 || off + count > buf.length) {
        fail(`nema dovoljno podataka za ${label} (treba ${count} B, preostalo ${Math.max(0, buf.length - off)} B)`);
      }
    };
    const readByte = (label = 'byte') => {
      need(1, label);
      return buf[off++];
    };
    const readInt = (label = 'int32') => {
      need(4, label);
      const value = dv.getInt32(off, false);
      off += 4;
      return value;
    };
    const readFloat = (label = 'float32') => {
      need(4, label);
      const value = dv.getFloat32(off, false);
      off += 4;
      return value;
    };
    const readUTF = (label = 'UTF tekst') => {
      need(2, `${label} duljina`);
      const len = dv.getUint16(off, false);
      off += 2;
      need(len, label);
      const value = decoder.decode(buf.subarray(off, off + len));
      off += len;
      return value;
    };
    const readPointList = (nPts, label) => {
      if (!Number.isInteger(nPts) || nPts < 0 || nPts > 1000000) {
        fail(`neispravan broj točaka za ${label}: ${nPts}`);
      }
      const pts = [];
      for (let i = 0; i < nPts; i++) {
        const x = readFloat(`${label} x[${i}]`);
        const y = readFloat(`${label} y[${i}]`);
        const hasCp = readByte(`${label} control-point flag[${i}]`);
        let cp = null;
        if (hasCp === 1) {
          cp = {
            cx1: readFloat(`${label} cx1[${i}]`),
            cy1: readFloat(`${label} cy1[${i}]`),
            cx2: readFloat(`${label} cx2[${i}]`),
            cy2: readFloat(`${label} cy2[${i}]`)
          };
        } else if (hasCp !== 0) {
          fail(`neispravan control-point flag ${hasCp} u točki ${i}`);
        }
        pts.push({ x, y, cp });
      }
      return pts;
    };

    currentTagOffset = off;
    currentTag = String.fromCharCode(readByte('V oznaka'));
    if (currentTag !== 'V') fail(`očekivana V oznaka, pronađeno ${JSON.stringify(currentTag)}`);
    const version = readInt('verzija');

    currentTagOffset = off;
    currentTag = String.fromCharCode(readByte('S oznaka'));
    if (currentTag !== 'S') fail(`očekivana S oznaka, pronađeno ${JSON.stringify(currentTag)}`);
    const scrapName = readUTF('naziv scrapa');
    const plotType = readInt('vrsta nacrta');
    readUTF('paleta točaka');
    readUTF('paleta linija');
    readUTF('paleta područja');

    const lines = [];
    const points = [];
    const stations = [];
    const areas = [];
    let bbox = null;

    while (off < buf.length) {
      currentTagOffset = off;
      currentTag = String.fromCharCode(readByte('oznaka zapisa'));

      if (currentTag === 'E') break;

      if (currentTag === 'I') {
        bbox = {
          xmin: readFloat('bbox xmin'),
          ymin: readFloat('bbox ymin'),
          xmax: readFloat('bbox xmax'),
          ymax: readFloat('bbox ymax')
        };
        const hasTransform = readInt('bbox transform flag');
        if (hasTransform === 1) {
          readFloat('bbox transform 1');
          readFloat('bbox transform 2');
          readFloat('bbox transform 3');
          readFloat('bbox transform 4');
        }
        continue;
      }

      if (currentTag === 'N') {
        readInt('scrap indeks');
        continue;
      }
      if (currentTag === 'F') continue;

      if (currentTag === 'D') {
        readFloat('D x1');
        readFloat('D y1');
        readFloat('D x2');
        readFloat('D y2');
        readFloat('D skala');
        readUTF('D početna stanica');
        need(6, 'D završni podaci');
        off += 6;
        continue;
      }

      if (currentTag === 'L') {
        const thType = readUTF('vrsta linije');
        const group = version >= 401147 ? readUTF('grupa linije') : '';
        const closed = readByte('zatvorena linija') === 1;
        const reversed = readByte('obrnuta linija') === 1;
        const outline = readInt('outline linije');
        const lside = version >= 602055 ? readInt('strana linije') : -1;
        const level = version >= 401090 ? readInt('razina linije') : 0;
        const scrap = version >= 401160 ? readInt('scrap linije') : 0;
        const options = readUTF('opcije linije');
        const nPts = readInt('broj točaka linije');
        const pts = readPointList(nPts, 'linija');
        lines.push({ type: thType, group, closed, reversed, outline, lside, level, scrap, options, pts });
        continue;
      }

      if (currentTag === 'A') {
        const thType = readUTF('vrsta područja');
        const group = version >= 401147 ? readUTF('grupa područja') : '';
        const prefix = readUTF('prefiks područja');
        const count = readInt('broj područja');
        const visible = readByte('vidljivost područja') === 1;
        const orientation = readFloat('orijentacija područja');
        const level = version >= 401090 ? readInt('razina područja') : 0;
        const scrap = version >= 401160 ? readInt('scrap područja') : 0;
        const nPts = readInt('broj točaka područja');
        const pts = readPointList(nPts, 'područje');
        areas.push({ type: thType, group, prefix, count, visible, orientation, level, scrap, closed: true, pts });
        continue;
      }

      if (currentTag === 'P') {
        // TopoDroid stores point coordinates before symbol name/group.
        const x = readFloat('x simbola');
        const y = readFloat('y simbola');
        const thType = readUTF('vrsta simbola');
        const group = version >= 401147 ? readUTF('grupa simbola') : '';
        const orientation = readFloat('orijentacija simbola');
        const scale = readInt('skala simbola');
        const level = version >= 401090 ? readInt('razina simbola') : 0;
        const scrap = version >= 401160 ? readInt('scrap simbola') : 0;
        const text = version >= 303066 ? readUTF('tekst simbola') : '';
        const options = readUTF('opcije simbola');
        points.push({ type: thType, group, x, y, orientation, scale, level, scrap, text, options });
        continue;
      }

      if (currentTag === 'U') {
        const x = readFloat('x korisničke stanice');
        const y = readFloat('y korisničke stanice');
        const scale = readInt('skala korisničke stanice');
        const level = version >= 401090 ? readInt('razina korisničke stanice') : 0;
        const scrap = version >= 401160 ? readInt('scrap korisničke stanice') : 0;
        const name = readUTF('naziv korisničke stanice');
        stations.push({ name, x, y, scale, level, scrap, isUser: true });
        continue;
      }

      if (currentTag === 'X') {
        const x = readFloat('x stanice');
        const y = readFloat('y stanice');
        const name = readUTF('naziv stanice');
        const level = version >= 401090 ? readInt('razina stanice') : 0;
        const scrap = version >= 401160 ? readInt('scrap stanice') : 0;
        let sectionType = -1;
        let azimuth = null;
        let clino = null;
        if (version >= 207038) {
          sectionType = readInt('vrsta presjeka stanice');
          if (sectionType !== -1) {
            azimuth = readFloat('azimut presjeka stanice');
            clino = readFloat('nagib presjeka stanice');
          }
        }
        stations.push({ name, x, y, level, scrap, sectionType, azimuth, clino, isUser: false });
        continue;
      }

      if (currentTag === 'T') {
        const x = readFloat('x oznake');
        const y = readFloat('y oznake');
        const orientation = version > 207043 ? readFloat('orijentacija oznake') : 0;
        const scale = readInt('skala oznake');
        const level = version > 401090 ? readInt('razina oznake') : 0;
        const scrap = version > 401160 ? readInt('scrap oznake') : 0;
        const text = readUTF('tekst oznake');
        const options = readUTF('opcije oznake');
        points.push({ type: 'label', group: '', x, y, orientation, scale, level, scrap, text, options });
        continue;
      }

      console.warn('TDR: nepodržana oznaka', currentTag, 'na offsetu', currentTagOffset, 'u', sourceName);
      break;
    }

    return { version, scrapName, plotType, lines, areas, points, stations, bbox };
  }

  TdrParser.parse = parse;
})();
