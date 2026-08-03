/**
 * SOV Nacrt — PDF izvoz.
 * Učitava se POSLIJE nacrt-corpus-style-v7.js.
 *
 * Dodaje dva gumba uz postojeće "Spremi SVG" / "Spremi PNG":
 *   • PDF (vektor) — svg2pdf.js + jsPDF, pravi A4 vektor, oštro na svakom zoomu,
 *     spremno za tisak. Traži da crtež nema SVG filtere (v7.1 ih nema).
 *   • PDF (slika)  — canvas 3× → PNG → jsPDF, izgleda točno kao na ekranu.
 *
 * Biblioteke se učitavaju lijeno, tek na prvi klik, s istih CDN-ova
 * koje nacrt.html već koristi. Ako CDN ne prođe, gumb to jasno javi.
 */
(() => {
  'use strict';

  const A4 = { w: 210, h: 297 };          // mm
  const CANVAS_SCALE = 3;                  // za rasterski PDF

  const CDN = {
    jspdf: 'https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js',
    svg2pdf: 'https://cdn.jsdelivr.net/npm/svg2pdf.js@2.2.3/dist/svg2pdf.umd.min.js'
  };

  const loaded = new Map();
  function loadScript(url) {
    if (loaded.has(url)) return loaded.get(url);
    const p = new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = url;
      s.onload = () => resolve();
      s.onerror = () => reject(new Error('Ne mogu učitati ' + url));
      document.head.appendChild(s);
    });
    loaded.set(url, p);
    return p;
  }

  const jsPDFCtor = () => (window.jspdf && window.jspdf.jsPDF) || window.jsPDF;

  // svg2pdf.js 2.x UMD izvozi OBJEKT { svg2pdf }, ne funkciju, i uz to
  // registrira jsPDF.API.svg. Provjereno u pregledniku na so-velebit.hr.
  function svg2pdfFn() {
    const g = window.svg2pdf;
    if (typeof g === 'function') return g;
    if (g && typeof g.svg2pdf === 'function') return g.svg2pdf;
    return null;
  }

  // Logika u nacrt.html je unutar IIFE-a, pa currentSvg/currentSurvey/showStatus
  // NISU na window-u. Zato čitamo iz DOM-a: renderer upisuje SVG u #preview.
  function status(msg, kind) {
    const el = document.getElementById('status');
    if (el) { el.textContent = msg; el.className = 'status ' + (kind || 'info'); }
    else if (kind === 'error') console.error(msg);
  }

  function baseName() {
    const title = document.getElementById('fTitle');
    const raw = (title && title.value.trim()) || 'nacrt';
    return raw.replace(/[^a-zA-Z0-9_čćžšđČĆŽŠĐ -]/g, '_');
  }

  function currentSvgMarkup() {
    const node = document.querySelector('#preview svg');
    return node ? node.outerHTML : '';
  }

  function svgElementFromMarkup(markup) {
    const doc = new DOMParser().parseFromString(markup, 'image/svg+xml');
    const err = doc.querySelector('parsererror');
    if (err) throw new Error('SVG nije ispravan.');
    return doc.documentElement;
  }

  function save(pdf) {
    pdf.save(baseName() + '.pdf');
  }

  /* ---------------------------------------------------------------- *
   * Vektorski PDF
   * ---------------------------------------------------------------- */

  async function exportVector() {
    const markup = currentSvgMarkup();
    if (!markup) { status('Nema nacrta za izvoz. Prvo učitaj TopoDroid ZIP.', 'error'); return; }

    status('Pripremam vektorski PDF…');
    await loadScript(CDN.jspdf);
    await loadScript(CDN.svg2pdf);
    const JsPDF = jsPDFCtor();
    const convert = svg2pdfFn();
    if (!JsPDF || (!convert && typeof JsPDF.API.svg !== 'function')) {
      throw new Error('PDF biblioteke nisu dostupne.');
    }

    // svg2pdf traži element u dokumentu da bi mogao mjeriti tekst.
    const holder = document.createElement('div');
    holder.setAttribute('aria-hidden', 'true');
    holder.style.cssText = 'position:fixed;left:-10000px;top:0;width:1240px;height:1754px;';
    const svg = svgElementFromMarkup(markup);
    holder.appendChild(svg);
    document.body.appendChild(holder);

    try {
      const pdf = new JsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4', compress: true });
      const box = { x: 0, y: 0, width: A4.w, height: A4.h };
      if (typeof pdf.svg === 'function') await pdf.svg(svg, box);
      else await convert(svg, pdf, box);
      save(pdf);
      status('Vektorski PDF spremljen.', 'ok');
    } finally {
      holder.remove();
    }
  }

  /* ---------------------------------------------------------------- *
   * Rasterski PDF
   * ---------------------------------------------------------------- */

  // 1240×1754 px je A4; faktor 2 = 300 dpi, faktor 3 = 450 dpi.
  // Na 3× platno traži ~78 MB memorije, pa pri neuspjehu padamo na 2×.
  function rasterise(markup, scale) {
    return new Promise((resolve, reject) => {
      const blob = new Blob([markup], { type: 'image/svg+xml;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const img = new Image();
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas');
          canvas.width = (img.naturalWidth || 1240) * scale;
          canvas.height = (img.naturalHeight || 1754) * scale;
          const ctx = canvas.getContext('2d');
          ctx.fillStyle = '#fff';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          resolve(canvas.toDataURL('image/png'));
        } catch (e) { reject(e); } finally { URL.revokeObjectURL(url); }
      };
      img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Ne mogu rasterizirati SVG.')); };
      img.src = url;
    });
  }

  async function exportRaster() {
    const markup = currentSvgMarkup();
    if (!markup) { status('Nema nacrta za izvoz. Prvo učitaj TopoDroid ZIP.', 'error'); return; }

    status('Pripremam PDF…');
    await loadScript(CDN.jspdf);
    const JsPDF = jsPDFCtor();
    if (!JsPDF) throw new Error('jsPDF nije dostupan.');

    let dataUrl;
    try {
      dataUrl = await rasterise(markup, CANVAS_SCALE);
    } catch (e) {
      console.warn('Nacrt PDF: 3× rasterizacija nije uspjela, pokušavam 2×', e);
      dataUrl = await rasterise(markup, 2);
    }
    const pdf = new JsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4', compress: true });
    pdf.addImage(dataUrl, 'PNG', 0, 0, A4.w, A4.h, undefined, 'FAST');
    save(pdf);
    status('PDF spremljen.', 'ok');
  }

  /* ---------------------------------------------------------------- *
   * Gumbi
   * ---------------------------------------------------------------- */

  function wire(button, handler, busyLabel) {
    const original = button.textContent;
    button.addEventListener('click', async () => {
      if (button.disabled) return;
      button.disabled = true;
      button.textContent = busyLabel;
      try {
        await handler();
      } catch (err) {
        console.error('Nacrt PDF izvoz:', err);
        status('PDF izvoz nije uspio: ' + (err && err.message ? err.message : 'nepoznata greška'), 'error');
      } finally {
        button.disabled = false;
        button.textContent = original;
      }
    });
  }

  function addButtons() {
    const png = document.getElementById('btnPng');
    if (!png || document.getElementById('btnPdfVector')) return;

    const make = (id, label, title) => {
      const b = document.createElement('button');
      b.className = png.className || 'btn btn-secondary';
      b.id = id;
      b.textContent = label;
      b.title = title;
      return b;
    };

    const vector = make('btnPdfVector', 'Spremi PDF', 'Vektorski A4 PDF — oštro na svakom zoomu, za tisak');
    const raster = make('btnPdfImage', 'PDF (slika)', 'PDF sa slikom nacrta — izgleda točno kao na ekranu');

    png.insertAdjacentElement('afterend', raster);
    png.insertAdjacentElement('afterend', vector);

    wire(vector, exportVector, 'Izvozim…');
    wire(raster, exportRaster, 'Izvozim…');
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', addButtons);
  else addButtons();

  window.SOV_NACRT_PDF_EXPORT = { version: '1.0', format: 'A4 210×297 mm', modes: ['vektor', 'slika'] };
})();
