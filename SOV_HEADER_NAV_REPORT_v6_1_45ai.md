# SOV v6.1.45ai — prije/poslije header navigacije

Zajednički novi blok koristi se na svim izmijenjenim članskim/radnim stranicama. Logo vodi na `dashboard.html`; `Odjava` koristi postojeći `data-logout` mehanizam iz `assets/auth.js`.

## `admin-client-errors.html`

**Prije:**
```html
<header class="top"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><small class="muted">Greške korisnika</small></div></a><nav class="nav"><a href="dashboard.html">Dashboard</a><a href="sync-status.html">Sync status</a><a href="audit-status.html">Audit</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `admin-notifications.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><small>Obavijesti</small></div></a><nav class="nav"><a href="dashboard.html">Dashboard</a><a href="admin-users.html">Korisnici</a><a href="news-editor.html">Vijesti</a><a data-logout="" href="login.html">Odjava</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `admin-users.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><small>Korisnici</small></div></a><nav class="nav"><a href="dashboard.html">Dashboard</a><a href="admin-notifications.html">Obavijesti</a><a data-logout="" href="login.html">Odjava</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `arhivar-izvoz.html`

**Prije:**
```html
<header class="as-top">
<a class="as-brand" href="arhivar-dashboard.html"><span class="as-mark">SOV</span><span><b>Arhivarski izvoz</b><small>CSV · XML · ZIP paketi</small></span></a>
<nav class="as-nav">
<a href="dashboard.html">Dashboard</a>
<a href="arhivar-dashboard.html">Arhivar</a>
<a href="arhivar.html">Uređivanje arhive</a>
<a href="arhivar-predane-jame.html">Predane jame</a>
<a class="active" href="arhivar-izvoz.html">Izvoz</a>
<a data-logout="" href="login.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `arhivar-predane-jame.html`

**Prije:**
```html
<header class="as-top">
<a class="as-brand" href="arhivar-dashboard.html"><span class="as-mark">SOV</span><span><b>Predane jame</b><small>review · falinke · approve u bazu</small></span></a>
<nav class="as-nav">
<a href="dashboard.html">Dashboard</a>
<a href="arhivar-dashboard.html">Arhivar</a>
<a href="arhivar.html">Uređivanje arhive</a>
<a class="active" href="arhivar-predane-jame.html">Predane jame</a>
<a href="arhivar-izvoz.html">Izvoz</a>
<a data-logout="" href="login.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `arhivar-zahvati.html`

**Prije:**
```html
<header class="az-top">
<a class="az-brand" href="dashboard.html"><span class="az-mark">SOV</span><span><b>Arhivar</b><small>Zahvati i radnje — izvještaj</small></span></a>
<nav class="az-nav">
<a href="dashboard.html">Dashboard</a>
<a href="pregled-baze.html">Baza</a>
<a href="karta.html">Karta</a>
<a class="active" href="arhivar-zahvati.html">Zahvati</a>
<a data-logout="" href="login.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `arhivar.html`

**Prije:**
```html
<header class="aw-top">
<a class="aw-brand" href="dashboard.html"><span class="aw-mark">SOV</span><span><b>Arhivar</b><small>objekti · nacrti · zapisnici · katastar readiness</small></span></a>
<nav class="aw-nav">
<a href="dashboard.html">Dashboard</a>
<a href="pregled-baze.html">Karta objekata</a>
<a href="arhivar-dashboard.html">Arhivar dashboard</a>
<a class="active" href="arhivar.html">Uređivanje arhive</a>
<a href="arhivar-predane-jame.html">Predane jame</a>
<a href="arhivar-izvoz.html">Izvoz</a>
<a href="arhivar-zahvati.html">Zapisnik/zahvat</a>
<a href="topodroid.html">Nacrti</a>
<a data-logout="" href="login.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `dashboard.html`

**Prije:**
```html
<header class="sov-top sov-dashboard-top" data-dashboard-header="daily">
<a class="sov-brand sov-dashboard-brand" href="index.html" aria-label="Otvori javnu naslovnicu SOV-a">
<img class="sov-brand-logo" src="assets/sov-logo.png" alt="Speleološki odsjek Velebit" loading="eager" decoding="async"/>
<div class="sov-brand-copy"><strong>SOV Cloud</strong><small>Članski dashboard</small></div>
</a>
<nav aria-label="Korisnički izbornik" class="sov-nav sov-dashboard-nav">
<a class="primary public-home" href="index.html">Javni sajt</a>
<a class="logout-link" data-logout="" href="index.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `dokumentacija.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="index.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>Zagreb</small></div></a><nav class="nav"><a href="dashboard.html">SOV Cloud</a><a href="karta.html">Karta objekata</a><a href="kalendar-izleta.html">Kalendar izleta</a><a href="index.html">Novosti</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `dokumenti.html`

**Prije:**
```html
<header class="sov-top">
<a class="sov-brand" href="dashboard.html"><div class="sov-mark">SOV</div><div><strong>Dokumenti</strong><small>SOV Cloud</small></div></a>
<nav class="sov-nav"><a href="dashboard.html">SOV Cloud</a><a href="karta.html">Karta</a><a href="oruzarstvo.html">Oprema</a><a class="primary" href="dokumenti.html">Dokumenti</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `izleti-cloud.html`

**Prije:**
```html
<header class="top"><a class="brand" href="dashboard.html"><span class="mark">SOV</span><span>Izleti<br/><small>Raspored</small></span></a><nav class="nav"><a class="btn" href="dashboard.html">Dashboard</a><a class="btn" href="kalendar-izleta.html">Kalendar</a><button class="btn" id="refreshBtn">Osvježi</button><button class="btn primary" id="topAddBtn">+ Dodaj novi izlet</button></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `kalendar-izleta.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>SOV Cloud</small></div></a>
<nav class="nav"><a href="dashboard.html">Dashboard</a><a href="karta.html">Karta objekata</a><a href="pregled-baze.html">Baza</a><a href="izleti.html">Izleti</a><a class="primary" href="kalendar-izleta.html">Kalendar izleta</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `karta.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div>Karta<br/><small>cijela baza · TK25 · predaja nove jame</small></div></a>
<div class="actions">
<a class="btn ghost" href="dashboard.html">Dashboard</a>
<button class="btn primary" onclick="location.href='predaj-novu-jamu.html'">+ Predaj novu jamu</button>
<button class="btn" onclick="exportVisibleKml()">KML</button>
<button class="btn" onclick="fitCurrent()">Prikaži sve</button>
</div>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `napisi-clanak.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><span class="mark">SOV</span><span><strong>SOV Cloud</strong><br/><small>Predaja članka</small></span></a>
<nav class="nav"><a href="dashboard.html">Dashboard</a><a href="vijesti.html">Vijesti</a><a data-role-editor="" href="news-editor.html">Urednik</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `novi-zapisnik.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="index.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>Zagreb</small></div></a><nav class="nav"><a href="dashboard.html">SOV Cloud</a><a href="dokumenti.html">Dokumenti</a><a href="pregled-zapisnika.html">Pregled zapisnika</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzar-master-inventar.html`

**Prije:**
```html
<header class="cm-top"><a class="cm-brand" href="oruzar-master.html"><span class="cm-logo">🧰</span><span><strong>Inventar</strong><small>Pretraga, kategorije i artikli. Ovdje možeš brzo dodati ili ispraviti opremu.</small></span></a><nav class="cm-nav"><a href="dashboard.html">Pregled</a><a href="izleti.html">Izleti</a><a href="karta.html">Karta</a><a href="oruzar-master.html">Pregled</a><a class="active" href="oruzar-master-inventar.html">Inventar</a><a href="oruzar-master-inventura.html">Inventura</a><a href="oruzar-master-posudbe.html">Posudbe</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzar-master-inventura.html`

**Prije:**
```html
<header class="cm-top"><a class="cm-brand" href="oruzar-master.html"><span class="cm-logo">🧰</span><span><strong>Inventura</strong><small>Jednostavan ekran za prebrojavanje opreme po kategorijama.</small></span></a><nav class="cm-nav"><a href="dashboard.html">Pregled</a><a href="izleti.html">Izleti</a><a href="karta.html">Karta</a><a href="oruzar-master.html">Pregled</a><a href="oruzar-master-inventar.html">Inventar</a><a class="active" href="oruzar-master-inventura.html">Inventura</a><a href="oruzar-master-posudbe.html">Posudbe</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzar-master-notes.html`

**Prije:**
```html
<header class="cm-top"><a class="cm-brand" href="oruzar-master.html"><span class="cm-logo">📝</span><span><strong>Bilješke i podsjetnici</strong><small>što treba obaviti, provjeriti i nabaviti</small></span></a><nav class="cm-nav"><a href="oruzar-master.html">Pregled</a><a href="oruzar-master-inventar.html">Inventar</a><a href="oruzar-master-inventura.html">Inventura</a><a href="oruzar-master-posudbe.html">Posudba</a><a class="active" href="oruzar-master-notes.html">Bilješke</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzar-master-posudbe.html`

**Prije:**
```html
<header class="cm-top"><a class="cm-brand" href="oruzar-master.html"><span class="cm-logo">🧰</span><span><strong>Posudbe</strong><small>Zahtjevi članova, izdavanje opreme i povrat.</small></span></a><nav class="cm-nav"><a href="dashboard.html">Pregled</a><a href="izleti.html">Izleti</a><a href="karta.html">Karta</a><a href="oruzar-master.html">Pregled</a><a href="oruzar-master-inventar.html">Inventar</a><a href="oruzar-master-inventura.html">Inventura</a><a class="active" href="oruzar-master-posudbe.html">Posudbe</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzar-master.html`

**Prije:**
```html
<header class="cm-top"><a class="cm-brand" href="oruzar-master.html"><span class="cm-logo">🧰</span><span><strong>Oružarstvo</strong><small>oružarski radni prostor</small></span></a><nav class="cm-nav"><a href="dashboard.html">Pregled</a><a href="izleti.html">Izleti</a><a href="karta.html">Karta</a><a class="active" href="oruzar-master.html">Oružarstvo</a><a href="oruzar-master-inventar.html">Inventar</a><a href="oruzar-master-inventura.html">Inventura</a><a href="oruzar-master-posudbe.html">Posudbe</a><a data-logout="" href="#">Odjava</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `oruzarstvo.html`

**Prije:**
```html
<header class="top">
    <a class="brand" href="dashboard.html"><span class="mark">SOV</span><span><b>SOV Cloud</b><small>Oprema</small></span></a>
    <nav class="nav"><a href="dashboard.html">Početna</a><a href="karta.html">Baza</a><a href="kalendar-izleta.html">Izleti</a><a class="primary" href="oruzarstvo.html">Oprema</a><a id="oruzarAdminLink" href="oruzar-master.html" hidden>Oružarski dio</a></nav>
  </header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `predaj-novu-jamu.html`

**Prije:**
```html
<header class="sf-top">
<a class="sf-brand" href="dashboard.html"><img alt="SOV" loading="lazy" src="assets/brand/sov-round-logo.png"/><span><b>SOV Cloud</b><small>Predaja nove jame</small></span></a>
<nav aria-label="SOV Cloud navigacija" class="sf-nav">
<a href="dashboard.html">Dashboard</a>
<a href="karta.html">Karta</a>
<a class="active" href="predaj-novu-jamu.html">Predaj novu jamu</a>
<a data-role-archive="" href="arhivar-predane-jame.html">Arhivarski inbox</a>
<a data-logout="" href="login.html">Odjava</a>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `pregled-baze.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>SOV Cloud</small></div></a>
<nav class="nav"><a href="dashboard.html">Dashboard</a><a href="karta.html">Karta objekata</a><a class="primary" href="pregled-baze.html">Baza</a><button class="btn primary syncTiny" onclick="syncDriveDrawings()">🔄 Sync nacrte</button><a href="izleti.html">Izleti</a><a href="index.html">Novosti</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `role-manager.html`

**Prije:**
```html
<header class="top wrap">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><small>Role manager · Webmaster</small></div></a>
<nav class="nav"><a href="dashboard.html">Dashboard</a><a href="admin-users.html">Korisnici</a><a href="sync-status.html">SOV Sync</a><a data-logout="" href="login.html">Odjava</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `sov-system-status.html`

**Prije:**
```html
<header class="sov-status-top" aria-label="SOV Cloud status header">
      <a class="sov-status-brand" href="/index.html" aria-label="SOV javna naslovnica">
        <img src="/assets/sov-logo.png" alt="SOV logo" />
        <span><b>Speleološki odsjek Velebit</b><span>SOV Cloud · admin status</span></span>
      </a>
      <nav class="sov-status-nav" aria-label="Navigacija statusa">
        <a class="sov-pill primary" href="/dashboard.html">Dashboard</a>
        <a class="sov-pill" href="/index.html">Javni sajt</a>
        <button class="sov-pill" id="refresh-status" type="button">Osvježi</button>
        <button class="sov-pill danger" type="button" onclick="window.SOVAuth ? window.SOVAuth.logout() : location.href='/index.html'">Odjava</button>
      </nav>
    </header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `status.html`

**Prije:**
```html
<header class="sov-status-top" aria-label="SOV Cloud status header">
      <a class="sov-status-brand" href="/index.html" aria-label="SOV javna naslovnica">
        <img src="/assets/sov-logo.png" alt="SOV logo" />
        <span><b>Speleološki odsjek Velebit</b><span>SOV Cloud · admin status</span></span>
      </a>
      <nav class="sov-status-nav" aria-label="Navigacija statusa">
        <a class="sov-pill primary" href="/dashboard.html">Dashboard</a>
        <a class="sov-pill" href="/index.html">Javni sajt</a>
        <button class="sov-pill" id="refresh-status" type="button">Osvježi</button>
        <button class="sov-pill danger" type="button" onclick="window.SOVAuth ? window.SOVAuth.logout() : location.href='/index.html'">Odjava</button>
      </nav>
    </header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `system-status.html`

**Prije:**
```html
<header class="sov-status-top" aria-label="SOV Cloud status header">
      <a class="sov-status-brand" href="/index.html" aria-label="SOV javna naslovnica">
        <img src="/assets/sov-logo.png" alt="SOV logo" />
        <span><b>Speleološki odsjek Velebit</b><span>SOV Cloud · admin status</span></span>
      </a>
      <nav class="sov-status-nav" aria-label="Navigacija statusa">
        <a class="sov-pill primary" href="/dashboard.html">Dashboard</a>
        <a class="sov-pill" href="/index.html">Javni sajt</a>
        <button class="sov-pill" id="refresh-status" type="button">Osvježi</button>
        <button class="sov-pill danger" type="button" onclick="window.SOVAuth ? window.SOVAuth.logout() : location.href='/index.html'">Odjava</button>
      </nav>
    </header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `sync-status.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Sync</strong><small>ekosustav status · v6.1.4</small></div></a>
<nav class="nav"><a href="dashboard.html">Dashboard</a><a href="karta.html">Karta</a><a href="arhivar-dashboard.html">Arhivar</a><a href="news-editor.html">Vijesti CMS</a><a href="role-manager.html">Role</a><a class="primary" href="sync-status.html">SOV Sync</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-compare.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><br/><span class="small">Live vs SQL compare</span></div></a>
<nav class="nav"><a class="btn" href="karta.html">Baza JSON</a><a class="btn" href="speleo-sql-edit-sandbox.html">SQL edit sandbox</a><a class="btn" href="speleo-sql-object-hub.html">Object hub v5.12</a><a class="btn" href="speleo-sql-safe.html">SQL preview/import</a><a class="btn" href="pregled-baze.html">Pregled baze</a><a class="btn" href="speleo-sql-promote.html">Promocija SQL</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-edit-sandbox.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><br/><span class="small">SQL edit sandbox</span></div></a>
<nav class="nav"><a class="btn" href="karta.html">Baza JSON</a><a class="btn" href="speleo-sql-safe.html">SQL preview/import</a><a class="btn" href="speleo-sql-object-hub.html">Object hub v5.12</a><a class="btn" href="pregled-baze.html">Pregled baze</a><a class="btn" href="speleo-sql-compare.html">Live vs SQL</a><a class="btn" href="speleo-sql-promote.html">Promocija SQL</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-object-hub.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><br/><span class="small">SQL Object Hub v5.12</span></div></a>
<nav class="nav"><a class="btn" href="karta.html">Baza JSON</a><a class="btn" href="speleo-sql-edit-sandbox.html">SQL sandbox</a><a class="btn" href="speleo-sql-compare.html">Compare</a><a class="btn" href="speleo-sql-safe.html">SQL import</a><a class="btn" href="speleo-sql-promote.html">Promocija SQL</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-promote.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><br/><span class="small">SQL Promotion v5.14</span></div></a>
<nav class="nav"><a class="btn" href="karta.html">Baza JSON</a><a class="btn" href="speleo-sql-edit-sandbox.html">SQL sandbox</a><a class="btn" href="speleo-sql-compare.html">Compare</a><a class="btn" href="speleo-sql-object-hub.html">Object hub</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-safe.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>SOV Cloud</strong><br/><span class="muted">sigurni SQL preview</span></div></a>
<nav class="nav"><a class="btn" href="karta.html">Baza</a><a class="btn" href="pregled-baze.html">Pregled baze</a><a class="btn primary" href="speleo-sql-edit-sandbox.html">Edit sandbox</a><a class="btn" href="dashboard.html">Dashboard</a><a class="btn" href="speleo-sql-promote.html">Promocija SQL</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-zapisnik.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>SOV Cloud</small></div></a><nav class="nav"><a href="dashboard.html">Dashboard</a><a href="karta.html">Karta objekata</a><a href="dokumenti.html">Dokumenti</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `topodroid-import.html`

**Prije:**
```html
<header class="top"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><b>TopoDroid import pipeline</b><br/><small>Web 5.38 · Admin/Arhivar</small></div></a><nav class="nav"><a class="btn" href="dashboard.html">Dashboard</a><a class="btn" href="topodroid.html">Arhiva/nacrti</a><a class="btn" href="karta.html">Baza</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `topodroid.html`

**Prije:**
```html
<header class="top"><a class="brand" href="dashboard.html"><div class="mark">SOV</div><div><b>Arhiva i nacrti</b><br/><small>Canonical 5.38</small></div></a><nav class="nav"><a class="btn" href="dashboard.html">Dashboard</a><a class="btn" href="karta.html">Karta objekata</a><a class="btn" href="pregled-baze.html">Baza</a><a class="btn primary" href="topodroid-import.html">Import pipeline</a><button class="btn" onclick="exportIndex()">Export index</button><button class="btn" data-archive-write="" id="syncBackendBtn" onclick="syncSelectedToBackend()">Spremi u arhivu</button></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `tracking.html`

**Prije:**
```html
<header class="tracking-top">
<a aria-label="Povratak na SOV dashboard" class="tracking-brand" href="dashboard.html">
<div class="tracking-mark">SOV</div>
<div><b>Field Tracking</b><small>izlet · team · zadnje pozicije</small></div>
</a>
<nav aria-label="Navigacija" class="tracking-actions">
<a class="tracking-btn ghost" href="dashboard.html">Dashboard</a>
<a class="tracking-btn ghost" href="izleti.html">Izleti</a>
<a class="tracking-btn ghost" href="karta.html">Karta</a>
<button class="tracking-btn primary" id="refreshBtn" type="button">Osvježi</button>
</nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `zapisnici-import.html`

**Prije:**
```html
<header class="sov-top"><a class="sov-brand" href="dashboard.html"><div class="sov-mark">SOV</div><div><strong>Import zapisnika</strong><small>SQL + Storage</small></div></a><nav class="sov-nav"><a href="dokumenti.html">Dokumenti</a><a href="zapisnici-cijela-arhiva.html">Cijela arhiva</a><a class="primary" href="zapisnici-import.html">Import</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `zapisnici-najave.html`

**Prije:**
```html
<header class="zn-top"><a href="dokumenti.html"><strong>SOV · Najave iz zapisnika</strong></a><nav class="zn-nav"><a href="dashboard.html">Početna</a><a href="dokumenti.html">Dokumenti</a><a href="izleti-cloud.html">Izleti</a><a data-logout="" href="#">Odjava</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `zapisnici-native.html`

**Prije:**
```html
<header class="sov-top">
<a class="sov-brand" href="dashboard.html"><div class="sov-mark">SOV</div><div><strong>Dokumenti</strong><small>Živi zapisnici</small></div></a>
<nav class="sov-nav"><a href="dashboard.html">SOV Cloud</a><a href="dokumenti.html">Dokumenti</a><a href="zapisnici-najave.html">Najave iz zapisnika</a><a class="primary" href="zapisnici-native.html">Živi zapisnici</a></nav>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `zapisnici-skupstine.html`

**Prije:**
```html
<header class="top wrap"><a class="brand" href="index.html"><div class="mark">SOV</div><div><strong>Speleološki odsjek Velebit</strong><small>Zagreb</small></div></a><nav class="nav"><a href="dashboard.html">SOV Cloud</a><a href="dokumenti.html">Dokumenti</a></nav></header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `arhivar-dashboard.html`

**Prije:**
```html
<nav class="sov-arhivar-nav" aria-label="Arhivar navigacija">
  <a class="brand active" href="arhivar-dashboard.html"><span>SOV Arhivar</span><small>jedan radni ekran</small></a>
  <a href="#predaje" data-work-tab="predaje">Predaje</a>
  <a href="#arhiva" data-work-tab="arhiva">Arhiva</a>
  <a href="#izvoz" data-work-tab="izvoz">Izvoz</a>
  <a href="arhivar-zahvati.html">Zahvati</a>
  <a href="topodroid.html">Nacrti</a>
  <a href="karta.html">Karta objekata</a>
  <span class="spacer"></span>
  <span class="muted">osnovno odmah · napredno po potrebi</span>
  <a href="dashboard.html">Dashboard</a>
  <a data-logout href="login.html">Odjava</a>
</nav>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `assets/arhivar-simplify-v6145ad.js`

**Prije:**
```html
function nav(){
 if(!isArhivar())return;
 const embedded=new URLSearchParams(location.search).has('embedded');
```

**Poslije:**
```html
function nav(){
 if(!isArhivar())return;
 if(document.querySelector('.sov-member-top'))return;
 const embedded=new URLSearchParams(location.search).has('embedded');
```

## `news-editor.html`

**Prije:**
```html
<header class="top">
<a class="brand" href="dashboard.html"><span class="brand-mark">SOV</span><span><strong>SOV Cloud</strong><br/><small>Urednik vijesti</small></span></a>
<div>
<h1>Vijesti i urednik</h1>
<p>Sve postojeće novosti i članski draftovi žive u bazi. Urednik može uređivati tekst, fotke, galerije i status objave.</p>
</div>
<div class="top-actions">
<a class="btn secondary" href="index.html">Javne novosti</a>
<a class="btn secondary" href="vijesti.html">Vijesti stranica</a>
<button class="btn" id="newBtn">+ Nova vijest</button>
</div>
</header>
```

**Poslije:**
```html
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
<section class="top news-editor-intro" aria-label="Urednik vijesti">
  <div>
    <h1>Vijesti i urednik</h1>
    <p>Sve postojeće novosti i članski draftovi žive u bazi. Urednik može uređivati tekst, fotke, galerije i status objave.</p>
  </div>
  <div class="top-actions">
    <a class="btn secondary" href="index.html">Javne novosti</a>
    <a class="btn secondary" href="vijesti.html">Vijesti stranica</a>
    <button class="btn" id="newBtn">+ Nova vijest</button>
  </div>
</section>
```

## `assets/arhivar-simplify-v6145ad.js` — fallback blok

**Prije:**
```js
const n=el(`<nav class="sov-arhivar-nav" aria-label="Arhivar navigacija"><a class="brand" href="arhivar-dashboard.html"><span>SOV Arhivar</span><small>jedan radni ekran</small></a><a data-tab="Predaje" href="arhivar-dashboard.html#predaje">Predaje</a><a data-tab="Arhiva" href="arhivar-dashboard.html#arhiva">Arhiva</a><a data-tab="Izvoz" href="arhivar-dashboard.html#izvoz">Izvoz</a><a href="arhivar-zahvati.html">Zahvati</a><a href="topodroid.html">Nacrti</a><a href="karta.html">Karta objekata</a><span class="spacer"></span><span class="muted">osnovno odmah · napredno po potrebi</span><a href="dashboard.html">Dashboard</a><a data-logout href="login.html">Odjava</a></nav>`);
```

**Poslije:**
```js
const n=el(`<header class="sov-member-top" data-sov-member-header><a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard"><img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async"><span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span></a><nav class="sov-member-nav" aria-label="Glavna članska navigacija"><a class="sov-member-link" href="index.html">Javni sajt</a><a class="sov-member-link" href="dashboard.html">Dashboard</a><a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a></nav></header>`);
```

## `oruzarstvo-import.html`

**Prije:**
```html
<body><main class="import-shell"><section class="import-card"><a class="btn" href="oruzarstvo.html">← Oružarstvo</a><p></p><h1>Uvoz oružarstva</h1><p>Ova stranica uzima pripremljeni popis opreme i sprema ga u evidenciju oružarstva. Pokreće je sam
```

**Poslije:**
```html
<body>
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```

## `speleo-sql-go-live.html`

**Prije:**
```html
<body><div class="wrap"><a class="pill" href="dashboard.html">← Dashboard</a><a class="pill" href="karta.html">Baza</a><a class="pill" href="pregled-baze.html">Pregled baze</a><div class="card"><h1>SQL Go Live</h1><p>Ovo prebacuje sve objekte iz
```

**Poslije:**
```html
<body>
<header class="sov-member-top" data-sov-member-header>
  <a class="sov-member-brand" href="dashboard.html" aria-label="Povratak na članski dashboard">
    <img class="sov-member-logo" src="assets/sov-logo.png" alt="SOV" loading="eager" decoding="async">
    <span class="sov-member-brand-text"><strong>SOV Cloud</strong><small>Članski prostor</small></span>
  </a>
  <nav class="sov-member-nav" aria-label="Glavna članska navigacija">
    <a class="sov-member-link" href="index.html">Javni sajt</a>
    <a class="sov-member-link" href="dashboard.html">Dashboard</a>
    <a class="sov-member-link sov-member-logout" href="index.html" data-logout>Odjava</a>
  </nav>
</header>
```
