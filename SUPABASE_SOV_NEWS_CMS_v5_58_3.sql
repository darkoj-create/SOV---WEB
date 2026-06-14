-- SOV web v5.58.3 — News CMS / Urednik DB-first
-- Pokreni nakon v5.58.2. Ovo NE dira Arhivar.

create extension if not exists pgcrypto;
create extension if not exists unaccent;

create table if not exists public.sov_news (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  summary text,
  body text,
  image_url text,
  pdf_url text,
  cta_label text,
  cta_url text,
  published boolean not null default true,
  pinned boolean not null default false,
  published_at timestamptz default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by uuid,
  updated_by uuid
);

alter table public.sov_news add column if not exists slug text;
alter table public.sov_news add column if not exists category text default 'Novosti';
alter table public.sov_news add column if not exists author_name text;
alter table public.sov_news add column if not exists image_alt text;
alter table public.sov_news add column if not exists gallery_urls text[] not null default '{}';
alter table public.sov_news add column if not exists attachment_urls text[] not null default '{}';
alter table public.sov_news add column if not exists content_html text;
alter table public.sov_news add column if not exists featured boolean not null default false;
alter table public.sov_news add column if not exists source text default 'sov-web';
alter table public.sov_news add column if not exists legacy_url text;
alter table public.sov_news add column if not exists tags text[] not null default '{}';

-- Fill missing slugs from existing titles.
update public.sov_news
set slug = lower(regexp_replace(regexp_replace(unaccent(coalesce(title,'')), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'))
where slug is null or slug = '';

create unique index if not exists sov_news_slug_key on public.sov_news (slug);
create index if not exists sov_news_published_idx on public.sov_news (published, pinned desc, featured desc, published_at desc);
create index if not exists sov_news_category_idx on public.sov_news (category);

create or replace function public.sov_can_edit_news()
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_role text;
  v_can boolean;
begin
  if auth.uid() is null then
    return false;
  end if;

  if to_regclass('public.sov_current_user_permissions') is not null then
    begin
      execute 'select coalesce(can_edit_news,false) or coalesce(can_manage_users,false) from public.sov_current_user_permissions limit 1'
      into v_can;
      if coalesce(v_can,false) then return true; end if;
    exception when others then
      -- fallback below
    end;
  end if;

  if to_regclass('public.profiles') is not null then
    begin
      execute 'select lower(coalesce(role::text,'''')) from public.profiles where id = $1 limit 1'
      into v_role using auth.uid();
      if v_role in ('admin','editor','urednik') then return true; end if;
    exception when others then
      -- fallback below
    end;
  end if;

  return false;
end $$;

grant execute on function public.sov_can_edit_news() to authenticated, anon;

alter table public.sov_news enable row level security;

drop policy if exists "sov_news_public_read_published" on public.sov_news;
create policy "sov_news_public_read_published"
  on public.sov_news for select
  using (published = true or public.sov_can_edit_news());

drop policy if exists "sov_news_editor_insert" on public.sov_news;
create policy "sov_news_editor_insert"
  on public.sov_news for insert
  with check (public.sov_can_edit_news());

drop policy if exists "sov_news_editor_update" on public.sov_news;
create policy "sov_news_editor_update"
  on public.sov_news for update
  using (public.sov_can_edit_news())
  with check (public.sov_can_edit_news());

drop policy if exists "sov_news_editor_delete" on public.sov_news;
create policy "sov_news_editor_delete"
  on public.sov_news for delete
  using (public.sov_can_edit_news());

create or replace function public.set_sov_news_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  new.updated_by = auth.uid();
  if new.created_by is null then new.created_by = auth.uid(); end if;
  if new.slug is null or new.slug = '' then
    new.slug = lower(regexp_replace(regexp_replace(unaccent(coalesce(new.title,'')), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'));
  end if;
  return new;
end $$;

drop trigger if exists trg_sov_news_updated_at on public.sov_news;
create trigger trg_sov_news_updated_at
before insert or update on public.sov_news
for each row execute function public.set_sov_news_updated_at();

-- Storage bucket for editor uploads (cover photos, galleries, PDFs). Public read, editor/admin write.
do $$
begin
  if to_regclass('storage.buckets') is not null then
    insert into storage.buckets (id, name, public)
    values ('sov-news', 'sov-news', true)
    on conflict (id) do update set public = true;
  end if;
exception when others then
  raise notice 'Storage bucket creation skipped: %', sqlerrm;
end $$;

do $$
begin
  if to_regclass('storage.objects') is not null then
    drop policy if exists "sov_news_storage_public_read" on storage.objects;
    create policy "sov_news_storage_public_read" on storage.objects
      for select using (bucket_id = 'sov-news');

    drop policy if exists "sov_news_storage_editor_insert" on storage.objects;
    create policy "sov_news_storage_editor_insert" on storage.objects
      for insert with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    drop policy if exists "sov_news_storage_editor_update" on storage.objects;
    create policy "sov_news_storage_editor_update" on storage.objects
      for update using (bucket_id = 'sov-news' and public.sov_can_edit_news())
      with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    drop policy if exists "sov_news_storage_editor_delete" on storage.objects;
    create policy "sov_news_storage_editor_delete" on storage.objects
      for delete using (bucket_id = 'sov-news' and public.sov_can_edit_news());
  end if;
exception when others then
  raise notice 'Storage policies skipped: %', sqlerrm;
end $$;

-- Seed/migrate all existing static news pages into the DB. Existing edited rows are NOT overwritten.

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$56-zagrebacka-speleoskola$slug$,
  $title$56. ZAGREBAČKA SPELEOŠKOLA$title$,
  $summary$Eto nas opet! Ne gubi vrijeme, pročitaj što piše i prijavi se prije nego li se mjesta popune!$summary$,
  $body$Sveopći puče, počuj:
U terminu od 25.3. do 3.5.2026. godine održat će se 56. zagrebačka speleološka škola u organizaciji SO Velebit.
Teorijska predavanja održavat će se srijedom od 18 do 20:30 h u prostorijama društva u Klaićevoj 42/1, a praktična nastava na jednodnevnim i dvodnevnim izletima tijekom 5 vikenda.
Detaljan program škole bit će objavljen uskoro.
Cijena pohađanja škole iznosi 150 € za zaposlene, odnosno 120 € za studente. U cijenu je uključeno osiguranje od nezgode i sva potrebna speleološka oprema. Plaćanje je moguće jednokratno ili u dvije rate.
Prijave su moguće putem obrasca na
LINKU
. Nakon popunjene prijavnice kontaktirat će vas voditeljica škole Gorana Perić s daljnjim informacijama i terminom za intervju.
Broj polaznika je ograničen!
Sve upite šaljite na: speleoskola.sov@gmail.com$body$,
  $html$<p class="wp-block-paragraph">Sveopći puče, počuj:</p>
<p class="wp-block-paragraph">U terminu od 25.3. do 3.5.2026. godine održat će se 56. zagrebačka speleološka škola u organizaciji SO Velebit.<br/>Teorijska predavanja održavat će se srijedom od 18 do 20:30 h u prostorijama društva u Klaićevoj 42/1, a praktična nastava na jednodnevnim i dvodnevnim izletima tijekom 5 vikenda.</p>
<p class="wp-block-paragraph">Detaljan program škole bit će objavljen uskoro.<br/><br/>Cijena pohađanja škole iznosi 150 € za zaposlene, odnosno 120 € za studente. U cijenu je uključeno osiguranje od nezgode i sva potrebna speleološka oprema. Plaćanje je moguće jednokratno ili u dvije rate.</p>
<p class="wp-block-paragraph">Prijave su moguće putem obrasca na <a href="https://forms.gle/sVP52PE6AwnjvpKbA">LINKU</a>. Nakon popunjene prijavnice kontaktirat će vas voditeljica škole Gorana Perić s daljnjim informacijama i terminom za intervju.</p>
<p class="wp-block-paragraph">Broj polaznika je ograničen!</p>
<p class="wp-block-paragraph">Sve upite šaljite na: speleoskola.sov@gmail.com</p>
<p class="wp-block-paragraph"></p>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/04/ponorac-na-stijeni-mikic-t.jpg?fit=1200%2C800&ssl=1$img$,
  $legacy$novosti/56-zagrebacka-speleoskola.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$56-zagrebacka-speleoskola$slug2$,
  true,
  false,
  false,
  $dt$2026-01-20T14:24:01+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$duman-obecana-zemlja$slug$,
  $title$Duman, obećana zemlja$title$,
  $summary$Uoči pisanja ovog teksta podsjetila sam se svojih Suza u Munižabi i primijetila jedan uzorak: pojmovi “Crnopac”, “Olga” i “obećanje” u kombinaciji uglavnom ne daju ništa jednostavno.$summary$,
  $body$Dumaniti,
gl. nesvrš. neprijel.
; romantično šetati blagim padinama bukove šume
Tekst: Petra Jagodić
Fotografije: Petra Jagodić, Dino Grozić
Ekipa: Dino Grozić, Olga Jerković Perić, Petra Jagodić, Noa Balen, Ana Horvat
Uvod
Ideja o odlasku na Duman u ambicioznu akciju rekognosciranja došla je na dnevni red sastanka u SOV-u tamo negdje sredinom listopada, iako se zapravo rađala u Dinotovoj glavi preko godinu dana. Kombinacijom statistike, procesa eliminacije i pustih želja Dino je zaključio da bi područje Dumana na Crnopcu moglo skrivati nove ulaze u još neistražene dijelove prve etaže Jamskog sustava Crnopac. Analizom dostupnih LiDAR podataka pronašao je oko 140 potencijalnih ulaza u objekte, a još su mu samo trebali nadobudni istomišljenici koji će s njim iste i provjeriti.
Prepoznavši potencijalne saveznike u Velebitašima, stigla je prva najava za pohod: 25.-26.10. Isprva se činilo da će izlet biti dobro posjećen i WhatsApp grupa “Crnopac ovaj vikend” je brzo narasla na preko deset članova. No, kiša je imala druge planove pa je tako “ovaj vikend” brzo postao “onaj vikend”. Uz to, i članovi ekipe su u međuvremenu našli druge planove pa su tako otpadali jedan po jedan zbog temperatura, migrena i života općenito.
Ipak, na temelju same činjenice da ovaj tekst postoji, mogli biste pretpostaviti da se dogodilo nešto vrijedno spomena – i bili biste u pravu.
Dan prvi
Šačica entuzijasta željnih avanture – Dino, Olga i ja – odlučno je krenula u petak (31.10.2025.) navečer prema Gračacu. Jesenski dani su kratki, pa smo dogovorili s Centrom izvrsnosti Cerovačke špilje da tamo prespavamo i ujutro, odmorni, krenemo u istraživanje. Prije spavanja još smo provjerili rade li nam offline karte i KML-ovi te skovali okvirni plan “napada”.
Unatoč suncu na prognozi, jutro nas je dočekalo maglovito i tmurno – kako već i priliči blagdanu Svih svetih. Jednako tmurni bili su ostaci ostataka (tzv. napoj) za doručak. No vijest o pojačanju podigla je moral: Noa i Ana su oko šest ujutro krenuli iz Zagreba da nam se pridruže barem na jedan dan.
Nakon posljednjeg kruga eliminacije “luksuza” iz ruksaka i nešto čekanja na parkingu, uputili smo se svi zajedno do Gornje Cerovačke pećine i dalje markiranom planinarskom stazom koja vodi prema Jabukovcu. Put je na početku vrlo ugodan, a uskoro se i najavljeno sunce počelo probijati kroz oblake i zlatno jesensko lišće. Lagano smo se uspinjali i povremeno zastajali da provjerimo koordinate koje su bile vrlo blizu staze. Glavni cilj akcije bio je zapravo dalje prema jugozapadu pa nismo htjeli ovdje gubiti vrijeme.
Nakon dva sata planinarenja, stigli smo na osunčano sedlo – mjesto gdje počinje prava zabava. Tu smo se trebali odvojiti s planinarske staze i uhvatiti jedan od neucrtanih lovačkih puteljaka koji bi nas, prema LiDAR snimci, trebao odvesti na zapad. Teren je u međuvremenu prerastao u nešto sasvim drugo: mješavinu bodljikavog graba i razbacanog krškog kamenja koja je vješto skrivala svaki mogući prolaz kroz šikaru.
Dok su neki hvatali predah i sunce, a drugi uporno gledali u sve karte i izohipse na raspolaganju, Olga je jednostavno – nestala u grmlju! Nakon nekoliko minuta šuškanja, začuli smo njen glas: “Ekipa, ovdje! Našla sam stazu!” Iako ju je Dino pokušao uvjeriti da to nije ta koju tražimo jer ide u krivom smjeru i previše gubi na visini, na kraju smo se pomirili s time da za bolju ne znamo pa smo i mi nestali skupa s njom u grmlju.
Probijali smo se dalje nečime što je nalikovalo na životinjsku stazu (znate onu koja izgleda prohodno pri tlu, ali ta prohodnost seže samo do struka) dok nismo napokon izbili na širi utabani put kojeg smo tražili. U daljini su se čuli lovački psi koji kao da su se teleportirali s brda na brdo, rugajući nam se iz svih smjerova.
Sljedeća točka na popisu bila je jedna usamljena koordinata tik uz put. I dok smo mi raspravljali o tome kolike su šanse da jedan ozbiljan objekt tako blizu prilično popularne rute već nije istražen ili bar poznat, pred nama se ukazao impresivan ulaz u špilju visine 6-7 metara. Iako objekt očito staje nakon petnaestak metara zarušenjem, obradovala nas je morfologija koja upućuje na to da je nekad bio dio većeg horizontalnog kanala – upravo onakvog kakve tražimo.
Nakon što smo gurnuli glavu u sve rupe, pregledali sve kosti i svo perje, došao je trenutak da Noa i Ana krenu natrag. Usput će provjeriti još četiri obližnje koordinate i pokušati dokučiti gdje se zapravo naša lovačka staza spaja na planinarsku. Mi ostali nastavljamo dalje, susrećući putem zaigrane pse rugalice i njihove vlasnike koji nam pričaju o lovu na divlje svinje i broju medvjeda u porastu koji sve češće silaze na niže nadmorske visine.
U sada smanjenom sastavu uskoro izbijamo na prekrasan travnati proplanak i odlučujemo tu napraviti dužu pauzu za ručak, znajući da ono što slijedi neće biti ni blizu tako gostoljubivo. Čeka nas još dosta koordinata koje se nalaze na sve kamenitijem terenu, ali glavna motivacija je približavanje poznatim kanalima Munižabe i potajna nada da ćemo pronaći novi ulaz u nju.
Iako nismo utvrdili postojanje novog ulaza, uslikali smo vrlo pristojan grupni selfie iznad Munižabe (što se, poznavajući sudionike ove priče, čini kao teži poduhvat nego ovo prvo) i prije mraka pronašli bar dva jako zanimljiva, perspektivna objekta. Jednu jamu iz koje snažno puše hladan zrak, i objekt koji bi, inspiriran obližnjom Munižabom, mogao nositi ime
Slabo ste gledali 2
.
Olga je naime, nabasala na špiljicu (jednu od onih usputnih), ušla unutra samo s mobitelom i zaključila da, iako tehnički jest objekt, ne ide to nigdje. Kad smo već tamo, gonjeni čistom znatiželjom, Dino i ja smo ušli za njom vidjeti kamo ide to nigdje. I bogme smo, uz svjetlo čeone rasvjete, imali što vidjeti – u zidu malene špilje otvara se kanal veličine bolničkog hodnika!
Kanal se spaja s drugim, jamskim ulazom, kojeg smo prepoznali kao prethodno provjerenu obližnju koordinatu. Osim toga, vrlo brzo poprima značajke freatske tube sa toboganom koji vodi na početak vertikale od dvadesetak metara začinjene zaostalom granatom. Budući da sa sobom nismo imali speleo opremu, a i da smo silno željeli stići doma u jednom komadu, brzo smo napustili objekt i uzbuđeno odlučili: ovdje se moramo vratiti!
Noć smo proveli u mirnom dolcu kojeg smo otkrili usput – topao, suh, idealan za bivak. Umorni od cijelog dana, sjedili smo u tišini i hipnotizirano gledali u malo ognjište koje smo sagradili pored bivka. Tada se Olga sjetila da na mobitelu ima snimljene epizode
Povijesti četvrtkom
koje smo slušali i u autu. Na sveopću radost, pustila ih je i večer je završila pričom o propasti civilizacija kroz prošlost. “Super je ovaj izlet,” izjavila je Olga. “Pa s kim bi drugim mogla ovako slušati
Povijest četvrtkom
?” Ne znam, Olga. Zaista ne znam.
Dan drugi
Kratki dani i puno posla pred nama znače samo jedno: rano spavanje, rano buđenje. Dogovor da nas sutradan budi Olga koja ionako ustaje u cik zore Dino i ja smo malo požalili, no nakon nekoliko “još samo pet minuta” svi smo već slagali bivak i spremali ruksake. Doručak smo odlučili odgoditi do trenutka kad nam se i želuci probude, pa smo tako već oko 7 sati bili u pokretu.
Ostatak prijepodneva protekao je u znaku oštrih škrapa, nemilosrdne vegetacije i manjka kofeina. Uglavnom smo radili podijeljeni u dvije ekipe – Dino kao solo igrač, te Olga i ja u tandemu – da budemo efikasniji
i ova muka što prije završi
. Često smo se sjetili blagih padina bukove šume koje nam je obećao Dino i slatko se nasmijali svojoj naivnosti.
Dok sam po stoti put tog dana birala između borbe sa raslinjem koje me šamaralo i pentranja po stijenama, zaključila sam da smo ovdje valjda bar sigurni od medvjeda i divljih svinja. Oni vjerojatno nisu toliko ludi da idu kuda i mi, unatoč pogonu na sva četiri.
No, takav je surovi krš ipak s razlogom omiljeno speleološko igralište, pa smo se drugi dan osjećali možda i produktivnije nego prvi. Zvijezde dana svakako su dvije pedesetmetarske jame koje smo našli (jednu od njih sasvim slučajno, usput) te činjenica da su skoro sve danas provjerene koordinate sada potvrđeni objekti.
Oko podneva smo završili krug i izbili natrag na planinarsku stazu od jučer. Još sat vremena hodanja nizbrdo činilo se kao šetnja Maksimirom, a zadnji metri prošli su u maštanju o hrani.
Nakon pizze koja je poslana iz raja, ubacili smo zadnje podatke u tablicu, napravili kratak rezime i zadovoljno zaključili da je izlet ispunio očekivanja. Više je obećavajućih objekata zbog kojih je zatitralo naše špiljarsko srce, unatoč nepristupačnom terenu i granati postavljenoj na kritičnom mjestu.
S mislima o toplom tušu i mogućoj višednevnoj ekspediciji na Dumanu, potrpali smo se u auto i krenuli starom cestom za Zagreb – sad već skoro tradicionalno – uz još koju epizodu
Povijesti četvrtkom
.
Epilog
Uoči pisanja ovog teksta podsjetila sam se svojih
Suza u Munižabi
i primijetila jedan uzorak: pojmovi “Crnopac”, “Olga” i “obećanje” u kombinaciji uglavnom ne daju ništa jednostavno.
Sada, s vremenskim odmakom, upala mišića popušta i preuzima osjećaj optimizma – napravili smo baš dobar posao. No, hoću li išta naučiti iz toga? Možda. Možda i ne. Vidjet ćemo sljedeći put hoću li i ja slučajno dobiti migrenu. Ili život.
Konačna statistika:
Ukupno obrađeno: 35 koordinata, od toga 29 prema LiDAR-u i 6 slučajno pronađenih
Potvrđenih objekata: 24$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph"><strong>Dumaniti,</strong> <em>gl. nesvrš. neprijel.</em>; romantično šetati blagim padinama bukove šume</p>
<p class="wp-block-paragraph"><em>Tekst: Petra Jagodić</em><br/><em>Fotografije: Petra Jagodić, Dino Grozić</em><br/><em>Ekipa: Dino Grozić, Olga Jerković Perić, Petra Jagodić, Noa Balen, Ana Horvat</em></p>
<p class="wp-block-paragraph"><strong>Uvod</strong></p>
<p class="wp-block-paragraph">Ideja o odlasku na Duman u ambicioznu akciju rekognosciranja došla je na dnevni red sastanka u SOV-u tamo negdje sredinom listopada, iako se zapravo rađala u Dinotovoj glavi preko godinu dana. Kombinacijom statistike, procesa eliminacije i pustih želja Dino je zaključio da bi područje Dumana na Crnopcu moglo skrivati nove ulaze u još neistražene dijelove prve etaže Jamskog sustava Crnopac. Analizom dostupnih LiDAR podataka pronašao je oko 140 potencijalnih ulaza u objekte, a još su mu samo trebali nadobudni istomišljenici koji će s njim iste i provjeriti.<br/>Prepoznavši potencijalne saveznike u Velebitašima, stigla je prva najava za pohod: 25.-26.10. Isprva se činilo da će izlet biti dobro posjećen i WhatsApp grupa “Crnopac ovaj vikend” je brzo narasla na preko deset članova. No, kiša je imala druge planove pa je tako “ovaj vikend” brzo postao “onaj vikend”. Uz to, i članovi ekipe su u međuvremenu našli druge planove pa su tako otpadali jedan po jedan zbog temperatura, migrena i života općenito.</p>
<p class="wp-block-paragraph">Ipak, na temelju same činjenice da ovaj tekst postoji, mogli biste pretpostaviti da se dogodilo nešto vrijedno spomena – i bili biste u pravu.</p>
<p class="wp-block-paragraph"><strong>Dan prvi</strong></p>
<p class="wp-block-paragraph">Šačica entuzijasta željnih avanture – Dino, Olga i ja – odlučno je krenula u petak (31.10.2025.) navečer prema Gračacu. Jesenski dani su kratki, pa smo dogovorili s Centrom izvrsnosti Cerovačke špilje da tamo prespavamo i ujutro, odmorni, krenemo u istraživanje. Prije spavanja još smo provjerili rade li nam offline karte i KML-ovi te skovali okvirni plan “napada”.<br/>Unatoč suncu na prognozi, jutro nas je dočekalo maglovito i tmurno – kako već i priliči blagdanu Svih svetih. Jednako tmurni bili su ostaci ostataka (tzv. napoj) za doručak. No vijest o pojačanju podigla je moral: Noa i Ana su oko šest ujutro krenuli iz Zagreba da nam se pridruže barem na jedan dan.<br/>Nakon posljednjeg kruga eliminacije “luksuza” iz ruksaka i nešto čekanja na parkingu, uputili smo se svi zajedno do Gornje Cerovačke pećine i dalje markiranom planinarskom stazom koja vodi prema Jabukovcu. Put je na početku vrlo ugodan, a uskoro se i najavljeno sunce počelo probijati kroz oblake i zlatno jesensko lišće. Lagano smo se uspinjali i povremeno zastajali da provjerimo koordinate koje su bile vrlo blizu staze. Glavni cilj akcije bio je zapravo dalje prema jugozapadu pa nismo htjeli ovdje gubiti vrijeme.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6667" data-id="6667" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_1.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6668" data-id="6668" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_2.jpg" width="813"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6669" data-id="6669" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_3.jpg" width="813"/></figure>
</figure>
<p class="wp-block-paragraph">Nakon dva sata planinarenja, stigli smo na osunčano sedlo – mjesto gdje počinje prava zabava. Tu smo se trebali odvojiti s planinarske staze i uhvatiti jedan od neucrtanih lovačkih puteljaka koji bi nas, prema LiDAR snimci, trebao odvesti na zapad. Teren je u međuvremenu prerastao u nešto sasvim drugo: mješavinu bodljikavog graba i razbacanog krškog kamenja koja je vješto skrivala svaki mogući prolaz kroz šikaru.<br/>Dok su neki hvatali predah i sunce, a drugi uporno gledali u sve karte i izohipse na raspolaganju, Olga je jednostavno – nestala u grmlju! Nakon nekoliko minuta šuškanja, začuli smo njen glas: “Ekipa, ovdje! Našla sam stazu!” Iako ju je Dino pokušao uvjeriti da to nije ta koju tražimo jer ide u krivom smjeru i previše gubi na visini, na kraju smo se pomirili s time da za bolju ne znamo pa smo i mi nestali skupa s njom u grmlju.<br/>Probijali smo se dalje nečime što je nalikovalo na životinjsku stazu (znate onu koja izgleda prohodno pri tlu, ali ta prohodnost seže samo do struka) dok nismo napokon izbili na širi utabani put kojeg smo tražili. U daljini su se čuli lovački psi koji kao da su se teleportirali s brda na brdo, rugajući nam se iz svih smjerova.<br/>Sljedeća točka na popisu bila je jedna usamljena koordinata tik uz put. I dok smo mi raspravljali o tome kolike su šanse da jedan ozbiljan objekt tako blizu prilično popularne rute već nije istražen ili bar poznat, pred nama se ukazao impresivan ulaz u špilju visine 6-7 metara. Iako objekt očito staje nakon petnaestak metara zarušenjem, obradovala nas je morfologija koja upućuje na to da je nekad bio dio većeg horizontalnog kanala – upravo onakvog kakve tražimo.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-2 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6672" data-id="6672" data-orig-size="2048,1536" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_4.jpg" width="1080"/></figure>
</figure>
<p class="wp-block-paragraph">Nakon što smo gurnuli glavu u sve rupe, pregledali sve kosti i svo perje, došao je trenutak da Noa i Ana krenu natrag. Usput će provjeriti još četiri obližnje koordinate i pokušati dokučiti gdje se zapravo naša lovačka staza spaja na planinarsku. Mi ostali nastavljamo dalje, susrećući putem zaigrane pse rugalice i njihove vlasnike koji nam pričaju o lovu na divlje svinje i broju medvjeda u porastu koji sve češće silaze na niže nadmorske visine.<br/>U sada smanjenom sastavu uskoro izbijamo na prekrasan travnati proplanak i odlučujemo tu napraviti dužu pauzu za ručak, znajući da ono što slijedi neće biti ni blizu tako gostoljubivo. Čeka nas još dosta koordinata koje se nalaze na sve kamenitijem terenu, ali glavna motivacija je približavanje poznatim kanalima Munižabe i potajna nada da ćemo pronaći novi ulaz u nju.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-3 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6674" data-id="6674" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_5.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6675" data-id="6675" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_6.jpg" width="1080"/></figure>
</figure>
<p class="wp-block-paragraph">Iako nismo utvrdili postojanje novog ulaza, uslikali smo vrlo pristojan grupni selfie iznad Munižabe (što se, poznavajući sudionike ove priče, čini kao teži poduhvat nego ovo prvo) i prije mraka pronašli bar dva jako zanimljiva, perspektivna objekta. Jednu jamu iz koje snažno puše hladan zrak, i objekt koji bi, inspiriran obližnjom Munižabom, mogao nositi ime <em>Slabo ste gledali 2</em>.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-4 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6677" data-id="6677" data-orig-size="2048,1536" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_7.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6678" data-id="6678" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_8.jpg" width="1080"/></figure>
</figure>
<p class="wp-block-paragraph">Olga je naime, nabasala na špiljicu (jednu od onih usputnih), ušla unutra samo s mobitelom i zaključila da, iako tehnički jest objekt, ne ide to nigdje. Kad smo već tamo, gonjeni čistom znatiželjom, Dino i ja smo ušli za njom vidjeti kamo ide to nigdje. I bogme smo, uz svjetlo čeone rasvjete, imali što vidjeti – u zidu malene špilje otvara se kanal veličine bolničkog hodnika!<br/>Kanal se spaja s drugim, jamskim ulazom, kojeg smo prepoznali kao prethodno provjerenu obližnju koordinatu. Osim toga, vrlo brzo poprima značajke freatske tube sa toboganom koji vodi na početak vertikale od dvadesetak metara začinjene zaostalom granatom. Budući da sa sobom nismo imali speleo opremu, a i da smo silno željeli stići doma u jednom komadu, brzo smo napustili objekt i uzbuđeno odlučili: ovdje se moramo vratiti!</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-5 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6681" data-id="6681" data-orig-size="1536,2048" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_9.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6680" data-id="6680" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_10.jpg" width="813"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6682" data-id="6682" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_11.jpg" width="813"/></figure>
</figure>
<p class="wp-block-paragraph">Noć smo proveli u mirnom dolcu kojeg smo otkrili usput – topao, suh, idealan za bivak. Umorni od cijelog dana, sjedili smo u tišini i hipnotizirano gledali u malo ognjište koje smo sagradili pored bivka. Tada se Olga sjetila da na mobitelu ima snimljene epizode <em>Povijesti četvrtkom</em> koje smo slušali i u autu. Na sveopću radost, pustila ih je i večer je završila pričom o propasti civilizacija kroz prošlost. “Super je ovaj izlet,” izjavila je Olga. “Pa s kim bi drugim mogla ovako slušati <em>Povijest četvrtkom</em>?” Ne znam, Olga. Zaista ne znam.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-6 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6684" data-id="6684" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_12.jpg" width="1080"/></figure>
</figure>
<p class="wp-block-paragraph"><strong>Dan drugi</strong></p>
<p class="wp-block-paragraph">Kratki dani i puno posla pred nama znače samo jedno: rano spavanje, rano buđenje. Dogovor da nas sutradan budi Olga koja ionako ustaje u cik zore Dino i ja smo malo požalili, no nakon nekoliko “još samo pet minuta” svi smo već slagali bivak i spremali ruksake. Doručak smo odlučili odgoditi do trenutka kad nam se i želuci probude, pa smo tako već oko 7 sati bili u pokretu.</p>
<p class="wp-block-paragraph">Ostatak prijepodneva protekao je u znaku oštrih škrapa, nemilosrdne vegetacije i manjka kofeina. Uglavnom smo radili podijeljeni u dvije ekipe – Dino kao solo igrač, te Olga i ja u tandemu – da budemo efikasniji <s>i ova muka što prije završi</s>. Često smo se sjetili blagih padina bukove šume koje nam je obećao Dino i slatko se nasmijali svojoj naivnosti.<br/>Dok sam po stoti put tog dana birala između borbe sa raslinjem koje me šamaralo i pentranja po stijenama, zaključila sam da smo ovdje valjda bar sigurni od medvjeda i divljih svinja. Oni vjerojatno nisu toliko ludi da idu kuda i mi, unatoč pogonu na sva četiri.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-7 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6686" data-id="6686" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_13.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6687" data-id="6687" data-orig-size="2048,1542" height="813" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_14.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6688" data-id="6688" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_15.jpg" width="813"/></figure>
</figure>
<p class="wp-block-paragraph">No, takav je surovi krš ipak s razlogom omiljeno speleološko igralište, pa smo se drugi dan osjećali možda i produktivnije nego prvi. Zvijezde dana svakako su dvije pedesetmetarske jame koje smo našli (jednu od njih sasvim slučajno, usput) te činjenica da su skoro sve danas provjerene koordinate sada potvrđeni objekti.<br/>Oko podneva smo završili krug i izbili natrag na planinarsku stazu od jučer. Još sat vremena hodanja nizbrdo činilo se kao šetnja Maksimirom, a zadnji metri prošli su u maštanju o hrani.<br/>Nakon pizze koja je poslana iz raja, ubacili smo zadnje podatke u tablicu, napravili kratak rezime i zadovoljno zaključili da je izlet ispunio očekivanja. Više je obećavajućih objekata zbog kojih je zatitralo naše špiljarsko srce, unatoč nepristupačnom terenu i granati postavljenoj na kritičnom mjestu.<br/>S mislima o toplom tušu i mogućoj višednevnoj ekspediciji na Dumanu, potrpali smo se u auto i krenuli starom cestom za Zagreb – sad već skoro tradicionalno – uz još koju epizodu <em>Povijesti četvrtkom</em>.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-8 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/11/06/duman-obecana-zemlja/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6690" data-id="6690" data-orig-size="1542,2048" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_16.jpg" width="813"/></figure>
</figure>
<p class="wp-block-paragraph"><strong>Epilog</strong></p>
<p class="wp-block-paragraph">Uoči pisanja ovog teksta podsjetila sam se svojih <em>Suza u Munižabi</em> i primijetila jedan uzorak: pojmovi “Crnopac”, “Olga” i “obećanje” u kombinaciji uglavnom ne daju ništa jednostavno.<br/>Sada, s vremenskim odmakom, upala mišića popušta i preuzima osjećaj optimizma – napravili smo baš dobar posao. No, hoću li išta naučiti iz toga? Možda. Možda i ne. Vidjet ćemo sljedeći put hoću li i ja slučajno dobiti migrenu. Ili život.</p>
<p class="wp-block-paragraph"><em>Konačna statistika:</em><br/><em>Ukupno obrađeno: 35 koordinata, od toga 29 prema LiDAR-u i 6 slučajno pronađenih<br/>Potvrđenih objekata: 24</em></p>$html$,
  $cat$Istraživanja$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/11/duman_13.jpg?fit=1200%2C904&ssl=1$img$,
  $legacy$novosti/duman-obecana-zemlja.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$duman-obecana-zemlja$slug2$,
  true,
  false,
  false,
  $dt$2025-11-06T16:25:21+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$fugro-qgis-orux$slug$,
  $title$Fugro, QGIS, Orux!$title$,
  $summary$Godišnja speleološka ekspedicija "Sjeverni Velebit 2025."uspješno je privedena kraju, pa evo neki suma-sumarum kad već i naslov zvuči onako, latinski... jel'te.$summary$,
  $body$Piše: Vedran Ferenčak
Fotografije: arhiva ekspedicije
; Dalibor Paar, Čedo Josipović, Paul Karoshi, Paula Skelin, Dragana Rajković
I ove godine, u Velebitaškoj tradiciji, održala se speleološka ekspedicija u NP Sjeverni Velebit. U periodu od 26. 7. do 10. 8. okupilo se šarenog svijeta iz svih krajeva Hrvatske i šire. Ekspedicija je maštovito nazvana “Sjeverni Velebit 2025”, a nadimak joj je bio “Fugro, QGIS, Orux!” – što nije krilatica neke rimske legije, nego tri programa pomoću kojih smo koristili LIDAR snimke za rekognosciranje. LIDAR je i ranije bio poznat, ali ovo je bila prva SOV-ova ekspedicija na kojoj se ova tehnologija sustavno primjenjivala i uvelike olakšala pronalaženje terena. Kako je jedan član dobro primijetio – na Sjevernom Velebitu nije teško istražiti objekt, nego do njega pristupiti.
Za ljubitelje statistike: na ekspediciji je sudjelovalo 47 članova i 3 gosta. Zastupljeno je bilo 10 speleoloških društava: SOV, SOL, SOM, SUE, VfHH, SKOL, SDK, ASAK, HBSD i HPD Zolj. Od toga: 7 instruktora speleologije, 10 speleologa, 30 pripravnika i 3 suradnika. Najmlađi sudionik imao je 12 godina. Po spolu, bilo je 17 žena i 33 muškarca, a društvo su činili i 5 pasa, djece rođene i još nerođene – pa nove snage svakako stižu, a tko zna što će biti na proljeće?
E sad, što se napravilo? U 16 dana pregledane su 52 LIDAR točke od ukupno 192 zabilježene. Od toga je 29 potvrđeno kao speleološki objekti, a 23 kao ne-objekti – što je u skladu s dosadašnjim postotkom. Od potvrđenih 29, njih 19 čine novi fond istraženih objekata, dok su 3 već postojeća, ali s preklapanjem zbog nedovoljno preciznih koordinata. Istraženo je i 11 ranije poznatih objekata te otkriveno 8 novih, pronađenih usput tijekom obilazaka. Sve skupa, brojka staje na 38 istraženih objekata – a 39. je posebna priča: jama Nedam.
Ove godine nastavili smo istraživati paralelni krak u Nedam, gdje se prošle ekspedicije stalo na suženju na -600 m, na dijelu zvanom Usisavač. Radilo se u dva smjera: jedna ekipa penjala je dimnjak, a druga nastavila spuštanje. Ispenjan je dimnjak od 25 m do stropa meandra, a na suprotnoj strani vidi se okno koje tek treba istražiti. Spuštanje prema dolje nastavlja se kroz suženja, pa je nacrtano novih 40 m. Od posljednje dosegnute točke kamen pada još 30-ak metara – dakle, perspektive za nastavak itekako ima.
Uz istraživanja nastavljena su i znanstvena mjerenja. Skupljani su stari i postavljani novi uređaji za praćenje kemijsko-fizikalnih svojstava jama i meteoroloških uvjeta na ulazima. U objektima s prostranim ulazima (Lukina jama, Patkov gušt, G6) zabilježeno je spuštanje razine leda za oko 30 metara.
Posebno veselje izazvao je pronalazak Prvog spita. Kombinacijom mudrosti, iskustva i upornosti, jama Prvi spit je napokon pronađena – na opću radost svih!
Vremenske prilike bile su većinom povoljne: omjer sunčanih i kišnih dana bio je uvjerljivo na strani sunca. Za kišne dane imali smo stabilan i dobro opremljen kamp. Naša plava polu-kupola od cerada dva tjedna nas je čuvala i od kiše i od vjetra.
Kroz cijelu ekspediciju bilo je i pregršt prilika za brušenje raznih vještina – ne samo speleoloških. Osim postavljanja i crtanja, pilila su se i cijepala drva, mijenjale gume na autima, kuhala jela na vatri. To su sitnice koje se podrazumijevaju, ali ako se ne prenose dalje, lako izblijede. Budući da je bilo puno mlađih članova, stalno se učilo i prenosilo znanje. Jer, kako kaže stara poslovica: nije znanje znati, nego znanje znanje predati – a upravo to stvara našu speleološku kulturu.$body$,
  $html$<p class="has-black-color has-pale-cyan-blue-background-color has-text-color has-background has-link-color wp-elements-db27cb67a15399a8e4dd6bc9ee6e806c wp-block-paragraph"><em>Piše: Vedran Ferenčak<br/>Fotografije: arhiva ekspedicije</em>; Dalibor Paar, Čedo Josipović, Paul Karoshi, Paula Skelin, Dragana Rajković</p>
<p class="wp-block-paragraph"><br/>I ove godine, u Velebitaškoj tradiciji, održala se speleološka ekspedicija u NP Sjeverni Velebit. U periodu od 26. 7. do 10. 8. okupilo se šarenog svijeta iz svih krajeva Hrvatske i šire. Ekspedicija je maštovito nazvana “Sjeverni Velebit 2025”, a nadimak joj je bio “Fugro, QGIS, Orux!” – što nije krilatica neke rimske legije, nego tri programa pomoću kojih smo koristili LIDAR snimke za rekognosciranje. LIDAR je i ranije bio poznat, ali ovo je bila prva SOV-ova ekspedicija na kojoj se ova tehnologija sustavno primjenjivala i uvelike olakšala pronalaženje terena. Kako je jedan član dobro primijetio – na Sjevernom Velebitu nije teško istražiti objekt, nego do njega pristupiti.</p>
<p class="wp-block-paragraph">Za ljubitelje statistike: na ekspediciji je sudjelovalo 47 članova i 3 gosta. Zastupljeno je bilo 10 speleoloških društava: SOV, SOL, SOM, SUE, VfHH, SKOL, SDK, ASAK, HBSD i HPD Zolj. Od toga: 7 instruktora speleologije, 10 speleologa, 30 pripravnika i 3 suradnika. Najmlađi sudionik imao je 12 godina. Po spolu, bilo je 17 žena i 33 muškarca, a društvo su činili i 5 pasa, djece rođene i još nerođene – pa nove snage svakako stižu, a tko zna što će biti na proljeće?<br/></p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/08/26/fugro-qgis-orux/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6529" data-id="6529" data-orig-size="3000,648" height="233" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/paul-karoshi.jpg" width="1080"/></figure>
</figure>
<p class="wp-block-paragraph">E sad, što se napravilo? U 16 dana pregledane su 52 LIDAR točke od ukupno 192 zabilježene. Od toga je 29 potvrđeno kao speleološki objekti, a 23 kao ne-objekti – što je u skladu s dosadašnjim postotkom. Od potvrđenih 29, njih 19 čine novi fond istraženih objekata, dok su 3 već postojeća, ali s preklapanjem zbog nedovoljno preciznih koordinata. Istraženo je i 11 ranije poznatih objekata te otkriveno 8 novih, pronađenih usput tijekom obilazaka. Sve skupa, brojka staje na 38 istraženih objekata – a 39. je posebna priča: jama Nedam.</p>
<p class="wp-block-paragraph">Ove godine nastavili smo istraživati paralelni krak u Nedam, gdje se prošle ekspedicije stalo na suženju na -600 m, na dijelu zvanom Usisavač. Radilo se u dva smjera: jedna ekipa penjala je dimnjak, a druga nastavila spuštanje. Ispenjan je dimnjak od 25 m do stropa meandra, a na suprotnoj strani vidi se okno koje tek treba istražiti. Spuštanje prema dolje nastavlja se kroz suženja, pa je nacrtano novih 40 m. Od posljednje dosegnute točke kamen pada još 30-ak metara – dakle, perspektive za nastavak itekako ima.</p>
<p class="wp-block-paragraph">Uz istraživanja nastavljena su i znanstvena mjerenja. Skupljani su stari i postavljani novi uređaji za praćenje kemijsko-fizikalnih svojstava jama i meteoroloških uvjeta na ulazima. U objektima s prostranim ulazima (Lukina jama, Patkov gušt, G6) zabilježeno je spuštanje razine leda za oko 30 metara.</p>
<p class="wp-block-paragraph">Posebno veselje izazvao je pronalazak Prvog spita. Kombinacijom mudrosti, iskustva i upornosti, jama Prvi spit je napokon pronađena – na opću radost svih!</p>
<p class="wp-block-paragraph">Vremenske prilike bile su većinom povoljne: omjer sunčanih i kišnih dana bio je uvjerljivo na strani sunca. Za kišne dane imali smo stabilan i dobro opremljen kamp. Naša plava polu-kupola od cerada dva tjedna nas je čuvala i od kiše i od vjetra.</p>
<p class="wp-block-paragraph">Kroz cijelu ekspediciju bilo je i pregršt prilika za brušenje raznih vještina – ne samo speleoloških. Osim postavljanja i crtanja, pilila su se i cijepala drva, mijenjale gume na autima, kuhala jela na vatri. To su sitnice koje se podrazumijevaju, ali ako se ne prenose dalje, lako izblijede. Budući da je bilo puno mlađih članova, stalno se učilo i prenosilo znanje. Jer, kako kaže stara poslovica: nije znanje znati, nego znanje znanje predati – a upravo to stvara našu speleološku kulturu.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-2 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/08/26/fugro-qgis-orux/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6529" data-id="6529" data-orig-size="3000,648" height="233" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/paul-karoshi.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6527" data-id="6527" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/paul-karoshi-2.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6533" data-id="6533" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/cedo-josipovic.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6532" data-id="6532" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/dalibor-paar.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6530" data-id="6530" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/dalibor-paar_2.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6526" data-id="6526" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/dalibor-paar_3.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6523" data-id="6523" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/dalibor-paar_4.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6528" data-id="6528" data-orig-size="1365,2048" height="1080" loading="lazy" sizes="(max-width: 720px) 100vw, 720px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/matej-blatnik_4.jpg" width="720"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6525" data-id="6525" data-orig-size="2048,1365" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/matej-blatnik_3.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6524" data-id="6524" data-orig-size="2048,1365" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/matej-blatnik_2.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6531" data-id="6531" data-orig-size="2048,1365" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/matej-blatnik.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6536" data-id="6536" data-orig-size="2000,924" height="498" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/dragana-rajkovic.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6537" data-id="6537" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/08/paula-skelin.jpg" width="1080"/></figure>
</figure>$html$,
  $cat$Ekspedicije$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/08/p8014716.jpg?fit=1200%2C800&ssl=1$img$,
  $legacy$novosti/fugro-qgis-orux.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$fugro-qgis-orux$slug2$,
  true,
  false,
  false,
  $dt$2025-08-26T06:34:50+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$hej-haj-terihaj-i-gorsko$slug$,
  $title$Hej, haj Terihaj (i Gorsko)$title$,
  $summary$O HGSS-u smo već slušali na predavanjima, ali ovo je bio prvi put da smo ih vidjeli kako djeluju u praksi$summary$,
  $body$Napisala: Klara Kundid
Fotografije: Petra Jagodić, Klara Kundid, Ines Šašić
Drugi vikend školice, s Terihajem u subotu i Gorskim zrcalom u nedjelju, donio je prve konkretne susrete sa stijenom i užetom.
Subota je započela penjalištem Terihaj, gdje smo prvi put iskusili penjanje po stijeni. Dan je započeo okupljanjem u 8:30 u Glinenom golubu, gdje smo nakon kave i Matijinog famoznog kolača od mrkve krenuli prema samom penjalištu. Po dolasku smo prvo postavili “velebitaški stol” kako bismo se okrijepili prije početka.
Dan smo otvorili ponavljanjem i učenjem uzlova. Radili smo i izradu improviziranog sjedišta od zamke, tzv.
gaće
. Nakon toga smo se, pod vodstvom instruktora Fere i Ines, prebacili na vježbanje klasičnih tehnika. Vježbali smo spuštanje pomoću Dülferovog sjedišta, uz to smo prošli i tehniku klasičnog osiguravanja preko tijela, koristeći vlastito tijelo kao kočnicu u slučaju pada partnera i spuštanje francuzom. Zatim je na red došao rad s prusicima. Kad smo te osnove apsolvirali, prebacili smo se na samu stijenu Terihaja. Tu mi je tek kliknulo koliko spravice zapravo olakšavaju cijelu stvar, sve je puno lakše. Na klasičnim tehnikama nisam baš imala povjerenja da bih se samo tako pustila na užetu, ali na stijeni, uz spravice, skužila sam da sve drži i da je osjećaj zapravo jako siguran.
Na stijeni sam bila s instruktorom Lukom i moram reći da je cijelo vrijeme ostao jako miran, skroz staložen, i pustio me da se spustim na miru i odradim sve do kraja, bez obzira na to što su ga u jednom trenutku zvali da treba doći HGSS zbog nesreće dolje. Iako se ispod nas već odvijala cijela situacija, gore je sve vodio potpuno smireno i bez žurbe, što je baš pomoglo da ostanem fokusirana i odradim svoje.
Dolje se počela odvijati situacija sa spašavanjem nakon nesreće na tirolki. Kasnije sam saznala da se je jedan školarac ozbiljno ozlijedio, pa je došla HGSS ekipa i preuzela stvar. O HGSS-u smo već slušali na predavanjima, ali ovo je bio prvi put da smo ih vidjeli kako djeluju u praksi. Nakon toga je sve nekako utihnulo i cijela atmosfera se promijenila. Vratili smo se oko osam navečer, umorni i prljavi kao i obično, ali s dosta jakim dojmovima.
Nedjelja nas je odvela na Gorsko zrcalo. Okupljanje je bilo kod Voljenog Vukovara, a u 8 smo već krenuli dalje prema stijeni. Dolaskom na Gorsko zrcalo prvo smo se upoznali sa samom stijenom i prostorom na kojem ćemo provesti dan. Kasnije na predavanju iz geologije saznali smo da je to glatka, okomita stijena visoka oko dvadesetak metara, a nastala je kao rasjedna ploha uzduž tektonske pukotine. Kosi urezi na stijeni (strije) pokazuju da se u geološkoj prošlosti duž tog loma pomicanje odvijalo udesno, a riječ je o vapnencu. U tom trenutku na samoj stijeni to nismo znali, ali je bilo zanimljivo kasnije povezati ono što smo vidjeli s objašnjenjem s predavanja.
Po dolasku smo prvo skupljali granje za vatru. Nakon toga imali smo radionicu prve pomoći gdje smo učili kako pravilno pristupiti unesrećenome, imobilizirati ozljede i osnove reanimacije. A onda opet stijena, moj instruktor bio je Čajko. U početku sam imala osjećaj da sam sigurnija što sam bliže stijeni, pa sam se doslovno lijepila za nju, što je završilo s poprilično natučenim koljenima. Trebalo mi je neko vrijeme da shvatim da zapravo trebam napraviti suprotno i više vjerovati opremi nego svojem instinktu. Nakon penjanja prešli smo na vježbu prelaska čvora na užetu, situaciju koja simulira oštećeno uže. Što je u teoriji zvučalo izvedivo… dok nisam došla na red. Tu sam zapela i visila dosta dugo pokušavajući skužiti što dalje. Kombinacija umora i stvarne situacije očito napravi razliku. Iskreno, bilo mi je baš žao što su me svi čekali dok sam se borila s tim. Umorni, ali zadovoljni, na kraju smo spakirali opremu i krenuli nazad, s novim iskustvom i pokojom modricom…$body$,
  $html$<p class="wp-block-paragraph"><em>Napisala: Klara Kundid<br/>Fotografije: Petra Jagodić, Klara Kundid, Ines Šašić</em></p>
<p class="wp-block-paragraph"><br/>Drugi vikend školice, s Terihajem u subotu i Gorskim zrcalom u nedjelju, donio je prve konkretne susrete sa stijenom i užetom.</p>
<p class="wp-block-paragraph">Subota je započela penjalištem Terihaj, gdje smo prvi put iskusili penjanje po stijeni. Dan je započeo okupljanjem u 8:30 u Glinenom golubu, gdje smo nakon kave i Matijinog famoznog kolača od mrkve krenuli prema samom penjalištu. Po dolasku smo prvo postavili “velebitaški stol” kako bismo se okrijepili prije početka.</p>
<p class="wp-block-paragraph">Dan smo otvorili ponavljanjem i učenjem uzlova. Radili smo i izradu improviziranog sjedišta od zamke, tzv. <em>gaće</em>. Nakon toga smo se, pod vodstvom instruktora Fere i Ines, prebacili na vježbanje klasičnih tehnika. Vježbali smo spuštanje pomoću Dülferovog sjedišta, uz to smo prošli i tehniku klasičnog osiguravanja preko tijela, koristeći vlastito tijelo kao kočnicu u slučaju pada partnera i spuštanje francuzom. Zatim je na red došao rad s prusicima. Kad smo te osnove apsolvirali, prebacili smo se na samu stijenu Terihaja. Tu mi je tek kliknulo koliko spravice zapravo olakšavaju cijelu stvar, sve je puno lakše. Na klasičnim tehnikama nisam baš imala povjerenja da bih se samo tako pustila na užetu, ali na stijeni, uz spravice, skužila sam da sve drži i da je osjećaj zapravo jako siguran.</p>
<p class="wp-block-paragraph">Na stijeni sam bila s instruktorom Lukom i moram reći da je cijelo vrijeme ostao jako miran, skroz staložen, i pustio me da se spustim na miru i odradim sve do kraja, bez obzira na to što su ga u jednom trenutku zvali da treba doći HGSS zbog nesreće dolje. Iako se ispod nas već odvijala cijela situacija, gore je sve vodio potpuno smireno i bez žurbe, što je baš pomoglo da ostanem fokusirana i odradim svoje.</p>
<p class="wp-block-paragraph">Dolje se počela odvijati situacija sa spašavanjem nakon nesreće na tirolki. Kasnije sam saznala da se je jedan školarac ozbiljno ozlijedio, pa je došla HGSS ekipa i preuzela stvar. O HGSS-u smo već slušali na predavanjima, ali ovo je bio prvi put da smo ih vidjeli kako djeluju u praksi. Nakon toga je sve nekako utihnulo i cijela atmosfera se promijenila. Vratili smo se oko osam navečer, umorni i prljavi kao i obično, ali s dosta jakim dojmovima.</p>
<p class="wp-block-paragraph">Nedjelja nas je odvela na Gorsko zrcalo. Okupljanje je bilo kod Voljenog Vukovara, a u 8 smo već krenuli dalje prema stijeni. Dolaskom na Gorsko zrcalo prvo smo se upoznali sa samom stijenom i prostorom na kojem ćemo provesti dan. Kasnije na predavanju iz geologije saznali smo da je to glatka, okomita stijena visoka oko dvadesetak metara, a nastala je kao rasjedna ploha uzduž tektonske pukotine. Kosi urezi na stijeni (strije) pokazuju da se u geološkoj prošlosti duž tog loma pomicanje odvijalo udesno, a riječ je o vapnencu. U tom trenutku na samoj stijeni to nismo znali, ali je bilo zanimljivo kasnije povezati ono što smo vidjeli s objašnjenjem s predavanja.</p>
<p class="wp-block-paragraph">Po dolasku smo prvo skupljali granje za vatru. Nakon toga imali smo radionicu prve pomoći gdje smo učili kako pravilno pristupiti unesrećenome, imobilizirati ozljede i osnove reanimacije. A onda opet stijena, moj instruktor bio je Čajko. U početku sam imala osjećaj da sam sigurnija što sam bliže stijeni, pa sam se doslovno lijepila za nju, što je završilo s poprilično natučenim koljenima. Trebalo mi je neko vrijeme da shvatim da zapravo trebam napraviti suprotno i više vjerovati opremi nego svojem instinktu. Nakon penjanja prešli smo na vježbu prelaska čvora na užetu, situaciju koja simulira oštećeno uže. Što je u teoriji zvučalo izvedivo… dok nisam došla na red. Tu sam zapela i visila dosta dugo pokušavajući skužiti što dalje. Kombinacija umora i stvarne situacije očito napravi razliku. Iskreno, bilo mi je baš žao što su me svi čekali dok sam se borila s tim. Umorni, ali zadovoljni, na kraju smo spakirali opremu i krenuli nazad, s novim iskustvom i pokojom modricom…</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2026/04/20/hej-haj-terihaj-i-gorsko/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6800" data-id="6800" data-orig-size="1566,2080" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/gorsko-zrcalo-na-stijeni_petra-jagodic.jpg" width="813"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6798" data-id="6798" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/pogled-na-stijenu-terihaj_klara-kundid.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6799" data-id="6799" data-orig-size="1333,2000" height="1080" loading="lazy" sizes="(max-width: 720px) 100vw, 720px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/radionica-i-vjezbe-prve-pomoci_ines-sasic.jpg" width="720"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6801" data-id="6801" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/terihaj-uspon_ines-sasic.jpg" width="1080"/></figure>
</figure>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/04/terihaj-uspon_ines-sasic.jpg?fit=1200%2C900&ssl=1$img$,
  $legacy$novosti/hej-haj-terihaj-i-gorsko.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$hej-haj-terihaj-i-gorsko$slug2$,
  true,
  false,
  false,
  $dt$2026-04-20T06:35:53+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$istrazivanja-na-gubackom-vrhu$slug$,
  $title$Istraživanja na Gubačkom vrhu$title$,
  $summary$U nedjelju, 27. travnja 2025. godine mala ali motivirana ekipa antropologa, arheologa i speleologa se uputila ka Gubačkom vrhu,$summary$,
  $body$U nedjelju, 27. travnja 2025. godine mala ali motivirana ekipa antropologa, arheologa i speleologa se uputila ka Gubačkom vrhu, gdje su ranije 2020. godine članovi Speleološkog kluba Ursus spelaeus  Rafael Kućan, Goran Radović i Hrvoje Cvitanović prilikom istraživanja jame pod Gubačkim vrhom pronašlii zanimljive arheološke i antropološke nalaze (Rafael Kućan). Trenutna istraživanja su pokrenuta na inicijativu Zavičajnog muzeja Ozalj. Nakon gotovo dvosatnog planinarenja ekipa je došla na nalazište. Potvrđeno je da su nalazi znanstveno vrlo zanimljivi te je napravljena terenska dokumentacija a nalazi sakupljeni za daljnje analize. Nakon što se ekipa uspješno vratila noseći vrijedan teret, umornih ali ozarenih lica, dogovoreno je da se kreće u znanstvenu valorizaciju i provođenje detaljnih analiza korištenjem suvremenih metoda (bioantropologija, arheologija, analize drevne DNA, stabilnih izotopa, radiometrijske datacije i dr.).
Na fotografiji s lijeva na desno: Hrvoje Cvitanović
(Speleološki klub Ursus spelaeus, Karlovac),
Dalibor Reš (Speleološka udruga Estavela, Kastav), Ivor Janković
(Institut za Antropologiju, Zagreb; Speleološki odsjek PDS Velebit)
, Miroslav Razum (Zavičajni muzej Ozalj), Marko Kušan (student), Mario Novak (Institut za Antropologiju, Zagreb), Nataša Cvitanović (Speleološki klub Ursus spelaeus, Karlovac), Saša Minihofer (Speleološki klub Ursus spelaeus, Karlovac).$body$,
  $html$<p class="wp-block-paragraph">U nedjelju, 27. travnja 2025. godine mala ali motivirana ekipa antropologa, arheologa i speleologa se uputila ka Gubačkom vrhu, gdje su ranije 2020. godine članovi Speleološkog kluba Ursus spelaeus  Rafael Kućan, Goran Radović i Hrvoje Cvitanović prilikom istraživanja jame pod Gubačkim vrhom pronašlii zanimljive arheološke i antropološke nalaze (Rafael Kućan). Trenutna istraživanja su pokrenuta na inicijativu Zavičajnog muzeja Ozalj. Nakon gotovo dvosatnog planinarenja ekipa je došla na nalazište. Potvrđeno je da su nalazi znanstveno vrlo zanimljivi te je napravljena terenska dokumentacija a nalazi sakupljeni za daljnje analize. Nakon što se ekipa uspješno vratila noseći vrijedan teret, umornih ali ozarenih lica, dogovoreno je da se kreće u znanstvenu valorizaciju i provođenje detaljnih analiza korištenjem suvremenih metoda (bioantropologija, arheologija, analize drevne DNA, stabilnih izotopa, radiometrijske datacije i dr.).</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/04/30/6412/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6413" data-id="6413" data-orig-size="3200,2400" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/gubacki-vrh-2025.jpg" width="1080"/></figure>
</figure>
<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Na fotografiji s lijeva na desno: Hrvoje Cvitanović <a>(Speleološki klub Ursus spelaeus, Karlovac), </a>Dalibor Reš (Speleološka udruga Estavela, Kastav), Ivor Janković <a>(Institut za Antropologiju, Zagreb; Speleološki odsjek PDS Velebit)</a>, Miroslav Razum (Zavičajni muzej Ozalj), Marko Kušan (student), Mario Novak (Institut za Antropologiju, Zagreb), Nataša Cvitanović (Speleološki klub Ursus spelaeus, Karlovac), Saša Minihofer (Speleološki klub Ursus spelaeus, Karlovac).</p>$html$,
  $cat$Istraživanja$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/04/gubacki-vrh-2025.jpg?fit=1200%2C900&ssl=1$img$,
  $legacy$novosti/istrazivanja-na-gubackom-vrhu.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$istrazivanja-na-gubackom-vrhu$slug2$,
  true,
  false,
  false,
  $dt$2025-04-30T07:44:10+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$izlet-iz-snova$slug$,
  $title$Izlet iz snova$title$,
  $summary$...Pitamo se gdje je svemu ovome kraj, padaju oklade. Kanal buja na sve strane i završava dvoranom u kojoj je duboki blatni krater, ogroman penj i lijepa pješčana zaravan, gdje se može leći i odmoriti na žalu. Ovdje završavamo sa istraživanjem i odlazimo na bi$summary$,
  $body$Napisala: Olga Jerković Perić
Ekipa: Marko Batovanja, Marin Glušević (SOM), Nini Legović (SD Proteus), Venio Fabijančić (SUE), Ana Bakšić, Olga Jerković Perić (SOV)
Fotografije: Nini i Ana
Već na posljednjem istraživanju Munižabe dogovorena je nova avantura, a ovog puta ekipu su činili Nini, Marko, Marin, Venio, Ana i ja. Plan je bio ispenjati mali penj u tunelu blizu novog bivka i vidjeti kuda ide.  Oko ovog penja postoji teorija zavjere koja glasi: ako skrene za 90 stupnjeva spaja se direkt na Kitu. Kako nisam pobornik takvih vjerovanja, ne polagah velike nade u spoj s JSC i samo sam se nadala da se neće odmah zatvoriti, nego da nas na kraju penja ipak očekuju neki kanali.
U petak, 26.9., oko 10 ujutro, Ana i ja krećemo iz Zagreba za Crnopac. Uz Dodinu pomoć uspjele smo ugurati ključ u bravu kombija, upaliti ga te bez problema, ali i bez treće brzine, doploviti do Gračaca. Nakon već tradicionalnog pit stopa kod Drlje po novu rezervnu gumu, uputile smo se do okretaljke. Laganim tempom oko 10 navečer stižemo u novi bivak očekujući da će dio ekipe koji je krenuo popodne doći rano ujutro. U 4 ujutro na bivak dolijeću i 2 galeba Marin i Marko i Nini iz daleke Istre. U 7 ujutro, taman kad smo se nas dvije probudile, stigao je i Venio koji je na istraživanje dojurio izravno sa austrijskih Alpa.  Kako se njemu nije spavalo nas dvoje odmah odlazimo u istraživanje, za zagrijavanje smo odabrali upitnik među glonđama u blizini bivka. Tu smo nacrtali 50-ak m kanala, a nakon ove jutarnje tjelovježbe uputili smo se u gore spomenuti glavni upitnik. To je bio jedan kraći penj nakon kojeg slijedi vertikala i dugačak blatni kanal po dnu kojeg teče voda. U jednom trenutku pridružuje nam se i ostatak ekipe. Nakon još malo crtanja, opremanja i fotkanja vraćamo se na bivak gdje nas je Ana, kao prava domaćica, dočekala sa finim izdašnim napojem od gljiva i pašte. Uz priču za laku noć o neuzvraćenoj francuskoj ljubavi u Kiti, diskusiju o tome što sve stane u vreće i vrećice, ali i Anine špiljarske poslovice o volovima i …  veselo tonemo u san, sa još novih 300-tinjak m kanala.
Sutradan (nedjelja) Nini, Marin i Marko odlaze van, Ana ih prati i ostaje spavati na prvom bivku, a Venio i ja nastavljamo raditi. Na kraju kanala koji smo jučer istraživali mala je prostorija sa neimpozantnom blatnom vertikalom u kojoj se kamen kotrlja 8 sekundi. Kao vječiti pesimist, očekivala sam da je to samo neka špranja u kojoj ćemo zaglibiti i smočiti se, međutim vertikala je postajala sve šira i šira, a na njenom dnu … opet horizontalni kanali! U ovim predjelima svijeta blato živi nekim vlastitim životom i više puta niotkud smo bili bombardirani gvaljama glibeža koje samoinicijativno padaju niz vertikalu. Na dnu vertikale najprije slobodno penjemo u nekom kanalu koji izgledom podsjeća na jamu Punar u luci, samo je manjih dimenzija – crne isprane stijene, čuje se voda, meandar s dobrim oprimcima. Kad je penj postao preokomit za moj ukus odlučujemo se vratiti i nastaviti šetati kanalom. Dolazimo do novog najdubljeg djela jame – to je rupa u podu kanala u kojoj je sifon, a izgledom podsjeća na malu cenotu. Zanimljivo je da uz blato naše svagdašnje u ovom kanalu ima i dosta pijeska.
Nailazimo na kosi penj, visine 15 m uz nagib 75%. Od opreme preostao nam je još samo ficlek od 20 m, 3 fiksa, 2 ringa i 2 matice. Venio mi se sad već doslovno popeo navrh glave… tako je pređeno čak 120 cm penja. Zatim je uz premještanje neprocjenjive matice sa sidrišta na sidrište penj uspješno ispenjan do kraja. Tamo nastavljamo sa hodanjem po prostranom kanalu. Pitamo se gdje je svemu ovome kraj, padaju oklade.  Kanal buja na sve strane i završava dvoranom u kojoj je duboki blatni krater, ogroman penj i lijepa pješčana zaravan, gdje se može leći i odmoriti na žalu. Ovdje završavamo sa istraživanjem i odlazimo na bivak, sretni što se ništa nije zatvorilo i stalo.
Sutradan (29.9.) nakon 3 sata (po njegovom mišljenju) psihofizičke torture napokon sam uspjela natjerati Venia da se izvuče iz vreće i oko 5 popodne izlazimo iz jame. Putem kroz bogato ukrašenu Bančekovu perspektivu glavom mi se vrtio izmijenjeni stih poznate planinarske pjesme:
Sad zbogom saljevi i sige oko nas, mi ćemo se vratiti i posjetiti vas!
Uistinu ćemo se vratiti i to uskoro, jer ima još mnogo upitnika za istražiti i novih prostranstava koja treba otkriti.$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph"><em>Napisala: Olga Jerković Perić</em><br/><em>Ekipa: Marko Batovanja, Marin Glušević (SOM), Nini Legović (SD Proteus), Venio Fabijančić (SUE), Ana Bakšić, Olga Jerković Perić (SOV)</em><br/><em>Fotografije: Nini i Ana</em></p>
<p class="wp-block-paragraph">Već na posljednjem istraživanju Munižabe dogovorena je nova avantura, a ovog puta ekipu su činili Nini, Marko, Marin, Venio, Ana i ja. Plan je bio ispenjati mali penj u tunelu blizu novog bivka i vidjeti kuda ide.  Oko ovog penja postoji teorija zavjere koja glasi: ako skrene za 90 stupnjeva spaja se direkt na Kitu. Kako nisam pobornik takvih vjerovanja, ne polagah velike nade u spoj s JSC i samo sam se nadala da se neće odmah zatvoriti, nego da nas na kraju penja ipak očekuju neki kanali. </p>
<p class="wp-block-paragraph">U petak, 26.9., oko 10 ujutro, Ana i ja krećemo iz Zagreba za Crnopac. Uz Dodinu pomoć uspjele smo ugurati ključ u bravu kombija, upaliti ga te bez problema, ali i bez treće brzine, doploviti do Gračaca. Nakon već tradicionalnog pit stopa kod Drlje po novu rezervnu gumu, uputile smo se do okretaljke. Laganim tempom oko 10 navečer stižemo u novi bivak očekujući da će dio ekipe koji je krenuo popodne doći rano ujutro. U 4 ujutro na bivak dolijeću i 2 galeba Marin i Marko i Nini iz daleke Istre. U 7 ujutro, taman kad smo se nas dvije probudile, stigao je i Venio koji je na istraživanje dojurio izravno sa austrijskih Alpa.  Kako se njemu nije spavalo nas dvoje odmah odlazimo u istraživanje, za zagrijavanje smo odabrali upitnik među glonđama u blizini bivka. Tu smo nacrtali 50-ak m kanala, a nakon ove jutarnje tjelovježbe uputili smo se u gore spomenuti glavni upitnik. To je bio jedan kraći penj nakon kojeg slijedi vertikala i dugačak blatni kanal po dnu kojeg teče voda. U jednom trenutku pridružuje nam se i ostatak ekipe. Nakon još malo crtanja, opremanja i fotkanja vraćamo se na bivak gdje nas je Ana, kao prava domaćica, dočekala sa finim izdašnim napojem od gljiva i pašte. Uz priču za laku noć o neuzvraćenoj francuskoj ljubavi u Kiti, diskusiju o tome što sve stane u vreće i vrećice, ali i Anine špiljarske poslovice o volovima i …  veselo tonemo u san, sa još novih 300-tinjak m kanala.<br/></p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/10/02/izlet-iz-snova/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6564" data-id="6564" data-orig-size="1200,1600" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/kucica.jpg" width="810"/></figure>
</figure>
<p class="wp-block-paragraph">Sutradan (nedjelja) Nini, Marin i Marko odlaze van, Ana ih prati i ostaje spavati na prvom bivku, a Venio i ja nastavljamo raditi. Na kraju kanala koji smo jučer istraživali mala je prostorija sa neimpozantnom blatnom vertikalom u kojoj se kamen kotrlja 8 sekundi. Kao vječiti pesimist, očekivala sam da je to samo neka špranja u kojoj ćemo zaglibiti i smočiti se, međutim vertikala je postajala sve šira i šira, a na njenom dnu … opet horizontalni kanali! U ovim predjelima svijeta blato živi nekim vlastitim životom i više puta niotkud smo bili bombardirani gvaljama glibeža koje samoinicijativno padaju niz vertikalu. Na dnu vertikale najprije slobodno penjemo u nekom kanalu koji izgledom podsjeća na jamu Punar u luci, samo je manjih dimenzija – crne isprane stijene, čuje se voda, meandar s dobrim oprimcima. Kad je penj postao preokomit za moj ukus odlučujemo se vratiti i nastaviti šetati kanalom. Dolazimo do novog najdubljeg djela jame – to je rupa u podu kanala u kojoj je sifon, a izgledom podsjeća na malu cenotu. Zanimljivo je da uz blato naše svagdašnje u ovom kanalu ima i dosta pijeska.</p>
<p class="wp-block-paragraph">Nailazimo na kosi penj, visine 15 m uz nagib 75%. Od opreme preostao nam je još samo ficlek od 20 m, 3 fiksa, 2 ringa i 2 matice. Venio mi se sad već doslovno popeo navrh glave… tako je pređeno čak 120 cm penja. Zatim je uz premještanje neprocjenjive matice sa sidrišta na sidrište penj uspješno ispenjan do kraja. Tamo nastavljamo sa hodanjem po prostranom kanalu. Pitamo se gdje je svemu ovome kraj, padaju oklade.  Kanal buja na sve strane i završava dvoranom u kojoj je duboki blatni krater, ogroman penj i lijepa pješčana zaravan, gdje se može leći i odmoriti na žalu. Ovdje završavamo sa istraživanjem i odlazimo na bivak, sretni što se ništa nije zatvorilo i stalo.</p>
<p class="wp-block-paragraph">Sutradan (29.9.) nakon 3 sata (po njegovom mišljenju) psihofizičke torture napokon sam uspjela natjerati Venia da se izvuče iz vreće i oko 5 popodne izlazimo iz jame. Putem kroz bogato ukrašenu Bančekovu perspektivu glavom mi se vrtio izmijenjeni stih poznate planinarske pjesme:</p>
<p class="wp-block-paragraph"><em>Sad zbogom saljevi i sige oko nas, mi ćemo se vratiti i posjetiti vas!</em></p>
<p class="wp-block-paragraph">Uistinu ćemo se vratiti i to uskoro, jer ima još mnogo upitnika za istražiti i novih prostranstava koja treba otkriti.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-2 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/10/02/izlet-iz-snova/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6563" data-id="6563" data-orig-size="1600,1200" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/niski-vodostaj.jpg" width="1080"/></figure>
</figure>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-3 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/10/02/izlet-iz-snova/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6565" data-id="6565" data-orig-size="1798,1223" height="734" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/novi-kanali.jpg" width="1080"/></figure>
</figure>$html$,
  $cat$Istraživanja$cat$,
  $img$https://sovelebit.wordpress.com/wp-content/uploads/2025/10/niski-vodostaj_naslovna.jpg$img$,
  $legacy$novosti/izlet-iz-snova.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$izlet-iz-snova$slug2$,
  true,
  false,
  false,
  $dt$2025-10-02T16:46:35+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$novi-velebiten$slug$,
  $title$Novi Velebiten!$title$,
  $summary$Novi broj našeg časopisa Velebiten je tu! Pregršt priča iz planinarstva, visokogorstva, speleologije, putopisa i punom vrećom prekrasnih fotografija$summary$,
  $body$Ivor Janković, urednik
Drage velebitašice, dragi velebitaši, dragi svi čitatelji našeg dragog časopisa,
Prođe vrijeme, dođe rok – Velebiten nam opet stiže, skok na skok. Kao i svake godine, razdoblje zimskog sna bit će nam ljepše ako se ušuškamo uz vatru (logorsku, kućnu ili onu po vlastitim mogućnostima i preferencijama), s čašicom ___________(ubaciti željeni napitak) u jednoj, a novim brojem Velebitena u drugoj ruci.
Osim duge tradicije našeg časopisa, čini mi se da tradicija postaje i to da su posljednjih godina brojevi puni različitih obljetnica (za mlađe članove – trk do našeg knjižničara po ranije brojeve). Prošlim izdanjem obilježili smo 150 godina organiziranog planinarstva u Hrvatskoj, godinu ranije 70. obljetnicu osvajanja Mount Everesta, a 2022. pisali smo o stotoj obljetnici istraživanja Jame kod Rašpora (usput, upravo je izdana predivna monografija posvećena tom speleološkom objektu). Još godinu ranije podsjetili smo se osnutka Planinske satnije Velebit te prve hrvatske alpinističke ekspedicije Greenland 1971. To je tek mali dio obljetnica koje smo s razlogom obilježili u Velebitenu – i to s dobrim razlogom, jer pokazuju da naše društvo i naši Velebitaši imaju ključnu ulogu u mnogim istraživanjima i događajima svjetskog značaja.
A tako je i danas. No ovaj broj posebno je svečan, i to ne samo za jedan od naših vrijednih odsjeka, nego za čitavo društvo. Ove godine slavimo 75 godina od osnutka našeg društva! Naša pročelnica već od početka godine (a zapravo i prije) svojom palicom daje ritam brojnim događajima kojima obilježavamo ovaj, usuđujem se reći, i za širu zajednicu važan jubilej. Nema mnogo 75-godišnjaka koji i dalje radosno skakuću po planinskim vrhovima, uvlače se u uske špiljske meandre, smrzavaju u već ofucanim vrećama na minusima i pritom uživaju u svim drugim veselim aktivnostima koje naš 75-godišnji tinejdžer svakodnevno provodi. (Iako, naši „Fosili“ i dalje slave s nama na tradicionalnim Fosilijadama – ove godine već 27. put!) Upravo tim našim „Velebitskim vukovima“ (za tekst istoimene pjesme – javite se Edi) posvetili smo dio ovog broja, podsjetivši se na njih riječima i slikama. Što je društvo starije, čini se da nam je onaj svima poznati velebitaški duh sve mlađi. Zato – neka nam je sretna ova 75.! Ako moji (Rožanski) kukovi izdrže, obećajem da ću se dogegati do društva i na stotu obljetnicu.
No do tada – Velebiten u ruke. I ove godine bili smo izuzetno aktivni. Kao najvažnije, treba istaknuti kontinuiranu izobrazbu novih velebitašica i velebitaša (jer kako drukčije doživjeti stotu?) kroz školice naših odsjeka. Iako su svi voditelji i voditeljice škola svoje dužnosti shvatili vrlo ozbiljno te nam s lakoćom „isporučili“ nove speleologe, alpiniste, planinare i visokogorce. Čini mi se da ih ponekad nije lako nagovoriti da svoje uspjehe pretoče iz djela u riječi. No, uz metodu mrkve i batine, i ovaj broj donosi štorije o dogodovštinama školaraca i instruktora – koje ćemo čitati sada, ali i ponovno, kad mnogi od nas zaborave i što su tog dana doručkovali. Računam, negdje oko 100. obljetnice društva.
Upravo taj dokument trenutka, taj presjek sadašnjosti Velebita, važan je i predivan zalog za budućnost. Osim obljetničkih tekstova i osvrta na školice, u ovom broju možete uživati i u prikazima raznih istraživanja i rekognosciranja te u raznim putešestvijama (Munižaba – kojoj je posvećeno više tekstova, Duman, Ledenica, talijanska avantura), kao i u izvještaju s već tradicionalne ekspedicije Sjeverni Velebit.
U svoje osobno ime, velika hvala glavnom grafičkom i tehničkom uredniku Marjanu Prpiću – Luki. Bez njega bi Velebiten, ako bi uopće ugledao svjetlo dana, možda bio nalik prvim brojevima – crno-bijelim, umnoženim na fotokopirnom stroju – a ne ovako divno uređenom i dizajniranom izdanju. No čak i u takvim okolnostima, kao u prvim godinama časopisa, tekstovi i prilozi bili bi jednako sjajni!
Zato velika hvala svim autorima tekstova, predivnih fotografija te sudionicima svih aktivnosti koji naše društvo čine onakvima kakvo jest. Hvala svima koji su pomogli da ovaj obljetnički broj izgleda upravo ovako – raznoliko, posebno i predivno, baš kao i naše društvo.
A sada – na čitanje!
Velebiten 59_ sadržaj$body$,
  $html$<p class="wp-block-paragraph"><strong><em>Ivor Janković, urednik</em></strong><br/><br/><br/>Drage velebitašice, dragi velebitaši, dragi svi čitatelji našeg dragog časopisa,</p>
<p class="wp-block-paragraph">Prođe vrijeme, dođe rok – Velebiten nam opet stiže, skok na skok. Kao i svake godine, razdoblje zimskog sna bit će nam ljepše ako se ušuškamo uz vatru (logorsku, kućnu ili onu po vlastitim mogućnostima i preferencijama), s čašicom ___________(ubaciti željeni napitak) u jednoj, a novim brojem Velebitena u drugoj ruci.</p>
<p class="wp-block-paragraph">Osim duge tradicije našeg časopisa, čini mi se da tradicija postaje i to da su posljednjih godina brojevi puni različitih obljetnica (za mlađe članove – trk do našeg knjižničara po ranije brojeve). Prošlim izdanjem obilježili smo 150 godina organiziranog planinarstva u Hrvatskoj, godinu ranije 70. obljetnicu osvajanja Mount Everesta, a 2022. pisali smo o stotoj obljetnici istraživanja Jame kod Rašpora (usput, upravo je izdana predivna monografija posvećena tom speleološkom objektu). Još godinu ranije podsjetili smo se osnutka Planinske satnije Velebit te prve hrvatske alpinističke ekspedicije Greenland 1971. To je tek mali dio obljetnica koje smo s razlogom obilježili u Velebitenu – i to s dobrim razlogom, jer pokazuju da naše društvo i naši Velebitaši imaju ključnu ulogu u mnogim istraživanjima i događajima svjetskog značaja.</p>
<p class="wp-block-paragraph">A tako je i danas. No ovaj broj posebno je svečan, i to ne samo za jedan od naših vrijednih odsjeka, nego za čitavo društvo. Ove godine slavimo 75 godina od osnutka našeg društva! Naša pročelnica već od početka godine (a zapravo i prije) svojom palicom daje ritam brojnim događajima kojima obilježavamo ovaj, usuđujem se reći, i za širu zajednicu važan jubilej. Nema mnogo 75-godišnjaka koji i dalje radosno skakuću po planinskim vrhovima, uvlače se u uske špiljske meandre, smrzavaju u već ofucanim vrećama na minusima i pritom uživaju u svim drugim veselim aktivnostima koje naš 75-godišnji tinejdžer svakodnevno provodi. (Iako, naši „Fosili“ i dalje slave s nama na tradicionalnim Fosilijadama – ove godine već 27. put!) Upravo tim našim „Velebitskim vukovima“ (za tekst istoimene pjesme – javite se Edi) posvetili smo dio ovog broja, podsjetivši se na njih riječima i slikama. Što je društvo starije, čini se da nam je onaj svima poznati velebitaški duh sve mlađi. Zato – neka nam je sretna ova 75.! Ako moji (Rožanski) kukovi izdrže, obećajem da ću se dogegati do društva i na stotu obljetnicu.</p>
<p class="wp-block-paragraph">No do tada – Velebiten u ruke. I ove godine bili smo izuzetno aktivni. Kao najvažnije, treba istaknuti kontinuiranu izobrazbu novih velebitašica i velebitaša (jer kako drukčije doživjeti stotu?) kroz školice naših odsjeka. Iako su svi voditelji i voditeljice škola svoje dužnosti shvatili vrlo ozbiljno te nam s lakoćom „isporučili“ nove speleologe, alpiniste, planinare i visokogorce. Čini mi se da ih ponekad nije lako nagovoriti da svoje uspjehe pretoče iz djela u riječi. No, uz metodu mrkve i batine, i ovaj broj donosi štorije o dogodovštinama školaraca i instruktora – koje ćemo čitati sada, ali i ponovno, kad mnogi od nas zaborave i što su tog dana doručkovali. Računam, negdje oko 100. obljetnice društva.</p>
<p class="wp-block-paragraph">Upravo taj dokument trenutka, taj presjek sadašnjosti Velebita, važan je i predivan zalog za budućnost. Osim obljetničkih tekstova i osvrta na školice, u ovom broju možete uživati i u prikazima raznih istraživanja i rekognosciranja te u raznim putešestvijama (Munižaba – kojoj je posvećeno više tekstova, Duman, Ledenica, talijanska avantura), kao i u izvještaju s već tradicionalne ekspedicije Sjeverni Velebit.</p>
<p class="wp-block-paragraph">U svoje osobno ime, velika hvala glavnom grafičkom i tehničkom uredniku Marjanu Prpiću – Luki. Bez njega bi Velebiten, ako bi uopće ugledao svjetlo dana, možda bio nalik prvim brojevima – crno-bijelim, umnoženim na fotokopirnom stroju – a ne ovako divno uređenom i dizajniranom izdanju. No čak i u takvim okolnostima, kao u prvim godinama časopisa, tekstovi i prilozi bili bi jednako sjajni!</p>
<p class="wp-block-paragraph">Zato velika hvala svim autorima tekstova, predivnih fotografija te sudionicima svih aktivnosti koji naše društvo čine onakvima kakvo jest. Hvala svima koji su pomogli da ovaj obljetnički broj izgleda upravo ovako – raznoliko, posebno i predivno, baš kao i naše društvo.</p>
<p class="wp-block-paragraph">A sada – na čitanje!</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/12/15/novi-velebiten-4/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6704" data-id="6704" data-orig-size="4961,3508" height="763" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/12/sadrzaj.jpg" width="1080"/><figcaption class="wp-element-caption">Velebiten 59_ sadržaj</figcaption></figure>
</figure>
<p class="wp-block-paragraph"></p>$html$,
  $cat$Velebiten$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/12/600217430_1265737175584096_2146797365093729525_n.jpg?fit=1200%2C676&ssl=1$img$,
  $legacy$novosti/novi-velebiten.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$novi-velebiten$slug2$,
  true,
  false,
  false,
  $dt$2025-12-15T10:52:36+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$pa-po-uzetu-dol-pa-po-uzetu-gor$slug$,
  $title$Pa po užetu dol’ pa po užetu gor’!$title$,
  $summary$Školarci se po redu spuštaju u jamu, sa osmjehom za koji nisam sigurna da li je od sreće ili straha. Ma kakav strah, čim sam ukopčala stop descender i počela se spuštati nestalo je sve$summary$,
  $body$Napisala: Vesna Selaković
Fotografije: Jelena Babić, Gorana Perić, Klara Krstičević
Baš kako u naslovu stoji, tako bi se mogao sažeti predzadnji izlet 56. speleo školice. Bili smo u Vražjoj i Bocinoj jami, bilo je uzbudljivo, pomalo zastrašujuće…. ali krenimo ispočetka.
U subotu ujutro okupljanje je bilo u caffe baru Jazbina u Ogulinu. Kavica za razbuđivanje, makar su neki od uzbuđenja (čitaj autorica teksta
) bili budni i spremni za nova iskustva. Nakon kavice, pokret i za čas stižemo na planirano odredište, lovačka kućica koja je smještena baš između Vražje i Bocine jame.
Nakon što smo postavili velebitaški stol i pojeli, počeli smo s izradom bivka pod stručnim okom i savjetima instruktora Luke, uspješno (manje ili više ovisi tko ocjenjuje) napravili smo si “spavaonu”. Bivak je gotov pa krećemo dalje po programu. Prva je na redu Vražja jama, oblačimo opremu, provjeravamo jedni druge, pa instruktori sve provjere još jednom. Uzbuđenje raste, ali i lagani strah, ipak idem u nepoznato u svakom mogućem smislu. Razna mi se pitanja motaju po glavi, i glavna misao: “samo da me ne uhvati panika” jer od samog pogleda u tu za mene veliku dubinu noge lagano klecaju.
Školarci se po redu spuštaju u jamu, sa osmjehom za koji nisam sigurna da li je od sreće ili straha. Ma kakav strah, čim sam ukopčala stop descender i počela se spuštati nestalo je sve osim divljenja tim stijenama, saljevima što ih prekrivaju. Spustivši se do police gdje su bili drugi opazih da je opremljena linija za dalje, pa i rekli su nam da ima još za one hrabre. Da li sam hrabra ili ne, ne znam, ali htjela sam svoje prvo špiljarsko iskustvo podijeliti sa svojim sestrom, pa smo se spustile još malo dublje. Divotica, sad shvaćam kako je primamljivo ići dalje, istraživati, vidjeti što se još krije duboko u mraku. Dok smo čekali da se svi spuste, imali smo foto session jer naravno da se mora obilježiti ovako nešto. Kako smo se po užetu spustili dol’, tako smo se i popeli gor’.
Dio školaraca je odmah išao u Bocinu jamu, rečeno nam je da je fora, dobro postavljena sa malo sidrišta, da bila je fora spuštati se dolje, ali prema gore činilo mi se nema kraja, pa je opet bilo po užetu dol’, pa po užetu gor’. Završili smo s aktivnostima za taj dan, pa se opustili uz vatru, iće i piće. Bilo je pjesme i ćakule, smijeha i zezancije, savršen završetak dana. U nedjelju smo nastavili s aktivnostima, neki u Bocinu jamu, neki na orijentaciju u prostoru uz pomoć digitalnih tehnologija (nadam se da sam se dobro izrazila, ispravite me ako nisam). Moj uređaj za digitalnu orijentaciju je malo poblesavio pa je pobrkao strane svijeta (veli instruktor Darko treba ga kalibrirati, budemo to riješili), sva sreća da su drugi uređaji ispravno radili, jer po mojem bi lutali šumom tražeći cilj u krivom smjeru.
Orijentacija je uspješno savladana kad smo se vratili u kamp, a tu nas je dočekao poligon: prečkanje između stabala. Oblačimo opremu i vježbamo, sad već imamo neko iskustvo i znanje sa vježbica na Žici pa instruktori gledaju i pomažu savjetima kako kome treba, pa opet po užetu gor’, po užetu dol’. To je bilo zabavno ovako ne previsoko iznad zemlje, kao neki adrenalinski park, ali sve su to vježbe za stvarne situacije i shvaćali smo sve ozbiljno, zato se i vježba, uči pa kad se pod zemljom nađemo u takvoj situaciji znamo što treba za prelazak određenih etapa….
Jedan po jedan prolazili smo prečnicu pa kod instruktora (svako kod nekog svog) imali blic ispit vezanja uzlova. Odradili smo sve zadatke, malo se odmorili, opustili, pa krenuli slagati opremu i stvari za povratak doma. Još jednom posjetili Jazbinu (mislim kafić
) osvježili se toplim ili hladnim napitkom (kako tko) pa se umorni ali zadovoljni uputili prema doma.$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph"><em>Napisala: Vesna Selaković<br/>Fotografije: Jelena Babić, Gorana Perić, Klara Krstičević</em></p>
<p class="wp-block-paragraph">Baš kako u naslovu stoji, tako bi se mogao sažeti predzadnji izlet 56. speleo školice. Bili smo u Vražjoj i Bocinoj jami, bilo je uzbudljivo, pomalo zastrašujuće…. ali krenimo ispočetka.<br/>U subotu ujutro okupljanje je bilo u caffe baru Jazbina u Ogulinu. Kavica za razbuđivanje, makar su neki od uzbuđenja (čitaj autorica teksta <img alt="😊" class="emoji" draggable="false" loading="lazy" role="img" src="./Pa po užetu dol’ pa po užetu gor’! – Speleološki odsjek PDS Velebit_files/1f60a.svg"/>) bili budni i spremni za nova iskustva. Nakon kavice, pokret i za čas stižemo na planirano odredište, lovačka kućica koja je smještena baš između Vražje i Bocine jame. </p>
<p class="wp-block-paragraph">Nakon što smo postavili velebitaški stol i pojeli, počeli smo s izradom bivka pod stručnim okom i savjetima instruktora Luke, uspješno (manje ili više ovisi tko ocjenjuje) napravili smo si “spavaonu”. Bivak je gotov pa krećemo dalje po programu. Prva je na redu Vražja jama, oblačimo opremu, provjeravamo jedni druge, pa instruktori sve provjere još jednom. Uzbuđenje raste, ali i lagani strah, ipak idem u nepoznato u svakom mogućem smislu. Razna mi se pitanja motaju po glavi, i glavna misao: “samo da me ne uhvati panika” jer od samog pogleda u tu za mene veliku dubinu noge lagano klecaju. <br/>Školarci se po redu spuštaju u jamu, sa osmjehom za koji nisam sigurna da li je od sreće ili straha. Ma kakav strah, čim sam ukopčala stop descender i počela se spuštati nestalo je sve osim divljenja tim stijenama, saljevima što ih prekrivaju. Spustivši se do police gdje su bili drugi opazih da je opremljena linija za dalje, pa i rekli su nam da ima još za one hrabre. Da li sam hrabra ili ne, ne znam, ali htjela sam svoje prvo špiljarsko iskustvo podijeliti sa svojim sestrom, pa smo se spustile još malo dublje. Divotica, sad shvaćam kako je primamljivo ići dalje, istraživati, vidjeti što se još krije duboko u mraku. Dok smo čekali da se svi spuste, imali smo foto session jer naravno da se mora obilježiti ovako nešto. Kako smo se po užetu spustili dol’, tako smo se i popeli gor’. <br/>Dio školaraca je odmah išao u Bocinu jamu, rečeno nam je da je fora, dobro postavljena sa malo sidrišta, da bila je fora spuštati se dolje, ali prema gore činilo mi se nema kraja, pa je opet bilo po užetu dol’, pa po užetu gor’. Završili smo s aktivnostima za taj dan, pa se opustili uz vatru, iće i piće. Bilo je pjesme i ćakule, smijeha i zezancije, savršen završetak dana. U nedjelju smo nastavili s aktivnostima, neki u Bocinu jamu, neki na orijentaciju u prostoru uz pomoć digitalnih tehnologija (nadam se da sam se dobro izrazila, ispravite me ako nisam). Moj uređaj za digitalnu orijentaciju je malo poblesavio pa je pobrkao strane svijeta (veli instruktor Darko treba ga kalibrirati, budemo to riješili), sva sreća da su drugi uređaji ispravno radili, jer po mojem bi lutali šumom tražeći cilj u krivom smjeru. <br/>Orijentacija je uspješno savladana kad smo se vratili u kamp, a tu nas je dočekao poligon: prečkanje između stabala. Oblačimo opremu i vježbamo, sad već imamo neko iskustvo i znanje sa vježbica na Žici pa instruktori gledaju i pomažu savjetima kako kome treba, pa opet po užetu gor’, po užetu dol’. To je bilo zabavno ovako ne previsoko iznad zemlje, kao neki adrenalinski park, ali sve su to vježbe za stvarne situacije i shvaćali smo sve ozbiljno, zato se i vježba, uči pa kad se pod zemljom nađemo u takvoj situaciji znamo što treba za prelazak određenih etapa…. <br/>Jedan po jedan prolazili smo prečnicu pa kod instruktora (svako kod nekog svog) imali blic ispit vezanja uzlova. Odradili smo sve zadatke, malo se odmorili, opustili, pa krenuli slagati opremu i stvari za povratak doma. Još jednom posjetili Jazbinu (mislim kafić <img alt="😊" class="emoji" draggable="false" loading="lazy" role="img" src="./Pa po užetu dol’ pa po užetu gor’! – Speleološki odsjek PDS Velebit_files/1f60a.svg"/>) osvježili se toplim ili hladnim napitkom (kako tko) pa se umorni ali zadovoljni uputili prema doma.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2026/05/05/pa-po-uzetu-dol-pa-po-uzetu-gor/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6825" data-id="6825" data-orig-size="1548,2064" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/cekin-pilic_jelena-babic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6823" data-id="6823" data-orig-size="1548,2064" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/sestrice-selakovic_jelena-babic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6826" data-id="6826" data-orig-size="1638,2184" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/skolarci_klara-krsticevic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6827" data-id="6827" data-orig-size="1638,2184" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/u-bocinoj-jami_klara-krsticevic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6828" data-id="6828" data-orig-size="1638,2184" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/u-vrazjoj-jami-na-polici_klara-krsticevic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6824" data-id="6824" data-orig-size="2064,1548" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/vatra_jelena-babic.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6829" data-id="6829" data-orig-size="1548,2064" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/vjezbanje-uzlova_jelena-babic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6831" data-id="6831" data-orig-size="2624,1478" height="608" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/jutarnje-zagrijavanje_gorana-peric.jpg" width="1080"/></figure>
</figure>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/jutarnje-zagrijavanje_gorana-peric.jpg?fit=1200%2C676&ssl=1$img$,
  $legacy$novosti/pa-po-uzetu-dol-pa-po-uzetu-gor.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$pa-po-uzetu-dol-pa-po-uzetu-gor$slug2$,
  true,
  false,
  false,
  $dt$2026-05-05T12:40:48+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$speleoloska-ekspedicija-sjeverni-velebit-2025$slug$,
  $title$Speleološka ekspedicija “Sjeverni Velebit 2025.”$title$,
  $summary$SO PDS Velebit vas poziva na speleološku ekspediciju „Sjeverni Velebit 2025“ koja će se održati na području Hajdučkih kukova unutar Nacionalnog Parka Sjeverni Velebit u razdoblju od 26. 7. – 10. 8. 2025.$summary$,
  $body$SO PDS Velebit vas poziva na speleološku ekspediciju „Sjeverni Velebit 2025“ koja će se održati na području Hajdučkih kukova unutar Nacionalnog Parka Sjeverni Velebit u razdoblju od 26. 7. – 10. 8. 2025.
Ciljevi ekspedicije su nastavak istraživanja u već poznatim objektima, prvenstveno u drugoj najdubljoj hrvatskoj jami Nedam u „Usisavaču“, gdje se 2022. godine stalo na dubini od -563 m.
Također, istraživat će se i okolno područje što podrazumijeva rekognosciranje terena, provjeravanje LiDAR koordinata, prikupljanje podataka koji nedostaju o već poznatim objektima, istraživanje u novopronađenim i otprije poznatim objektima te monitoring u objektima s ledom.
Kamp će biti na Velikom Lomu uz makadamsku cestu, s pristupom tehničkoj vodi iz bunara. Dovoljne količine pitke vode bit će osigurane u kampu svim sudionicima.
U kampu će biti agregat za punjenje tehničke i osobne opreme.
Kotizacija iznosi 7 € za jedan dan, 42 € za tjedan dana, odnosno 80 € za cijelu ekspediciju, a plaća se po dolasku na ekspediciju.
U kotizaciju su uključeni doručak, užina na terenu, kuhana večera i hrana potrebna za boravak u jami.
Prijave do 13.7.2025. putem obrasca na
LINKU
.
Za sva pitanja i nedoumice obratite se voditelju ekspedicije:
Vedran Ferenčak
+385 95 85 45 161
vendref@gmail.com$body$,
  $html$<p class="wp-block-paragraph">SO PDS Velebit vas poziva na speleološku ekspediciju „Sjeverni Velebit 2025“ koja će se održati na području Hajdučkih kukova unutar Nacionalnog Parka Sjeverni Velebit u razdoblju od 26. 7. – 10. 8. 2025.<br/>Ciljevi ekspedicije su nastavak istraživanja u već poznatim objektima, prvenstveno u drugoj najdubljoj hrvatskoj jami Nedam u „Usisavaču“, gdje se 2022. godine stalo na dubini od -563 m.<br/>Također, istraživat će se i okolno područje što podrazumijeva rekognosciranje terena, provjeravanje LiDAR koordinata, prikupljanje podataka koji nedostaju o već poznatim objektima, istraživanje u novopronađenim i otprije poznatim objektima te monitoring u objektima s ledom.<br/>Kamp će biti na Velikom Lomu uz makadamsku cestu, s pristupom tehničkoj vodi iz bunara. Dovoljne količine pitke vode bit će osigurane u kampu svim sudionicima.<br/>U kampu će biti agregat za punjenje tehničke i osobne opreme.<br/>Kotizacija iznosi 7 € za jedan dan, 42 € za tjedan dana, odnosno 80 € za cijelu ekspediciju, a plaća se po dolasku na ekspediciju.<br/>U kotizaciju su uključeni doručak, užina na terenu, kuhana večera i hrana potrebna za boravak u jami.<br/>Prijave do 13.7.2025. putem obrasca na <a href="https://docs.google.com/forms/d/e/1FAIpQLSeYO7C2GF2IyCk4Ife_auDTytCVy1F9fiIWm3N1Vrq8a9W9_g/viewform?pli=1" rel="noreferrer noopener" target="_blank">LINKU</a>.<br/>Za sva pitanja i nedoumice obratite se voditelju ekspedicije:<br/>Vedran Ferenčak<br/>+385 95 85 45 161<br/>vendref@gmail.com</p>$html$,
  $cat$Ekspedicije$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2024/07/img_20240615_185331.jpg?fit=900%2C1200&ssl=1$img$,
  $legacy$novosti/speleoloska-ekspedicija-sjeverni-velebit-2025.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$speleoloska-ekspedicija-sjeverni-velebit-2025$slug2$,
  true,
  false,
  false,
  $dt$2025-07-07T14:26:59+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$speleoloska-ekspedicija-sjeverni-velebit-2026$slug$,
  $title$Speleološka ekspedicija Sjeverni Velebit 2026$title$,
  $summary$SO PDS Velebit poziva na speleološku ekspediciju na području Hajdučkih i Rožanskih kukova od 25.7. do 9.8.2026.$summary$,
  $body$Ciljevi ekspedicije su sustavno rekognosciranje terena radi pronalaska novih speleoloških objekata, prikupljanje podataka koji nedostaju o poznatim objektima te monitoring u objektima s ledom.
Nastavit će se i istraživanje u
Nedam
, drugoj najdubljoj hrvatskoj jami, u drugoj grani na dubini od
-614 m
(“Usisavač”), gdje se prošle godine kroz suženje ušlo u perspektivnu vertikalu.
Kamp i kotizacija
Kamp će biti na
Velikom Lomu
, do kojeg je moguć pristup autom. Na lokaciji kampa bit će postavljen tank pitke vode od 1000 litara.
Kotizacija iznosi
7 € po danu
, odnosno
80 € za cijelu ekspediciju
. U kotizaciju su uključeni doručak, užina na terenu, kuhana večera i hrana potrebna za boravak u jami.
Prijave
Prijave su otvorene do
20. 7.
putem prijavnog obrasca.$body$,
  $html$<p>Ciljevi ekspedicije su sustavno rekognosciranje terena radi pronalaska novih speleoloških objekata, prikupljanje podataka koji nedostaju o poznatim objektima te monitoring u objektima s ledom.</p><p>Nastavit će se i istraživanje u <strong>Nedam</strong>, drugoj najdubljoj hrvatskoj jami, u drugoj grani na dubini od <strong>-614 m</strong> (“Usisavač”), gdje se prošle godine kroz suženje ušlo u perspektivnu vertikalu.</p><h2>Kamp i kotizacija</h2><p>Kamp će biti na <strong>Velikom Lomu</strong>, do kojeg je moguć pristup autom. Na lokaciji kampa bit će postavljen tank pitke vode od 1000 litara.</p><p>Kotizacija iznosi <strong>7 € po danu</strong>, odnosno <strong>80 € za cijelu ekspediciju</strong>. U kotizaciju su uključeni doručak, užina na terenu, kuhana večera i hrana potrebna za boravak u jami.</p><h2>Prijave</h2><p>Prijave su otvorene do <strong>20. 7.</strong> putem prijavnog obrasca.</p>$html$,
  $cat$Ekspedicije$cat$,
  $img$assets/ekspedicija-sjeverni-velebit-2026-banner.png$img$,
  $legacy$novosti/speleoloska-ekspedicija-sjeverni-velebit-2026.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$speleoloska-ekspedicija-sjeverni-velebit-2026$slug2$,
  true,
  false,
  true,
  $dt$2026-05-20T10:00:00+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$spust-u-jamu$slug$,
  $title$Spust u jamu$title$,
  $summary$I konačno je stigao dan da se spustimo u jamu, donekle, ili barem u špilju s jamskim ulazom, Jopićevu špilju, dovoljno blizu da budemo sretni.$summary$,
  $body$Autor: Matija Hrženjak
Fotografije: Jelena Babić, Klara Krstičević, Matija Hrženjak
I konačno je stigao dan da se spustimo u jamu, donekle, ili barem u špilju s jamskim ulazom, Jopićevu špilju, dovoljno blizu da budemo sretni. Tri nova iskustva su obilježila ovaj naš treći izlet speleološke školice: gubljenje u špilji i izvan nje kako bi se zatim mogli orijentirati i naći pravi put, izrada minimalističkog bivka (pokoji megalomanskih razmjera) za sigurno spavanje te crtanje nacrta špilje kako se sljedeći puta ne bi morali više gubiti u njoj.
Subota nam je krenula lagano, tegljenjem svih stvari kojima smo se prenatrpali od parkinga pa do kampa malo ispod jamskih ulaza. Moramo naučiti malo bolje (čitaj: lakše) se spakirati. Dok su naši marljivi instruktori postavljali jamske ulaze, Fero nam je održao kratak uvod u bivakiranje, a Marina nam je pomogla sa samom izradom našeg bivka, koji je bio dovoljno divan da se na kraju i sama smjestila u njega. Skupili smo i nešto štapića, štapova i štapetina za logorsku vatru. Ovi zadnji su posebno obradovali Tonija koji se odmah ulovio motorne pile.
Nakon ručka podijelili smo se u dvije skupine od kojih se jedna izgubila u jamu, a druga izvan nje. Čedo nas je naučio kako se snaći i orijentirati izvan špilje koristeći karte, kompase i što kad ih nemamo pri ruci. Marina nas je naučila kako crtati nacrt špilje, što sve moramo navest na njemu i kako koristiti digitalne uređaje za mjerenje te kako se snaći kada ne žele surađivati. Iznimno me iznenadilo kako smo u mraku, vlazi i blatu još uvijek u mogućnosti lijepo i precizno crtati po izbrušenom milimetarskom papiru. Moj nadraži dio ovog izleta. Našli smo se zajedno natrag tek u sumrak, lijepo najeli u mnoštvu domaće kuhinje koja je bila prisutna za velebitaškim stolom i još se umjereno zabavili Ninovom svirkom i pjesmom grijući se uz vatru do
ranih jutarnjih
prikladnog vremena za spavanje tako da smo svi odmorni i spremni za sljedeći dan.
Nedjelja je krenula polako, nekima i blago hladno, ali neprekinuto kuhanje kave je zagrijalo i najsmrznutije. Krenuli smo opet spuštanjem u špilju i zatim dugim i prekrasnim prohodom kroz vrludave kanale koji se svako malo presijecaju. Bez Dalibora koji nas je vodio, bili bismo totalno i potpuno izgubljeni, posve sigurno. Mnoštvo penjanja, spuštanja, istezanja, saginjanja i ponešto provlačenja je učinilo naš put još zabavnijim. Veselim se i saznati ponešto više o posebnim penjačkim tehnikama u speleologiji (više od jednog slajda sljedeći put, molit ću lijepo). Naučili smo i posebno voljeti koraloidne tipove siga jer nam ukazuju na strujanje zraka i vjerojatnost pronalaska novih prolaza.
Poslije izlaska smo se ponovno podijelili u dvije skupine. Jedna je odradila orijentaciju u prirodi, a druga je vježbala spuštanje i penjanje po užetu. Nakon toga je došao najtužniji dio svakog našeg izleta, raspremanje, pakiranje i povratak kući.$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph"><em>Autor: Matija Hrženjak<br/>Fotografije: Jelena Babić, Klara Krstičević, Matija Hrženjak</em></p>
<p class="wp-block-paragraph">I konačno je stigao dan da se spustimo u jamu, donekle, ili barem u špilju s jamskim ulazom, Jopićevu špilju, dovoljno blizu da budemo sretni. Tri nova iskustva su obilježila ovaj naš treći izlet speleološke školice: gubljenje u špilji i izvan nje kako bi se zatim mogli orijentirati i naći pravi put, izrada minimalističkog bivka (pokoji megalomanskih razmjera) za sigurno spavanje te crtanje nacrta špilje kako se sljedeći puta ne bi morali više gubiti u njoj.</p>
<p class="wp-block-paragraph">Subota nam je krenula lagano, tegljenjem svih stvari kojima smo se prenatrpali od parkinga pa do kampa malo ispod jamskih ulaza. Moramo naučiti malo bolje (čitaj: lakše) se spakirati. Dok su naši marljivi instruktori postavljali jamske ulaze, Fero nam je održao kratak uvod u bivakiranje, a Marina nam je pomogla sa samom izradom našeg bivka, koji je bio dovoljno divan da se na kraju i sama smjestila u njega. Skupili smo i nešto štapića, štapova i štapetina za logorsku vatru. Ovi zadnji su posebno obradovali Tonija koji se odmah ulovio motorne pile.</p>
<p class="wp-block-paragraph">Nakon ručka podijelili smo se u dvije skupine od kojih se jedna izgubila u jamu, a druga izvan nje. Čedo nas je naučio kako se snaći i orijentirati izvan špilje koristeći karte, kompase i što kad ih nemamo pri ruci. Marina nas je naučila kako crtati nacrt špilje, što sve moramo navest na njemu i kako koristiti digitalne uređaje za mjerenje te kako se snaći kada ne žele surađivati. Iznimno me iznenadilo kako smo u mraku, vlazi i blatu još uvijek u mogućnosti lijepo i precizno crtati po izbrušenom milimetarskom papiru. Moj nadraži dio ovog izleta. Našli smo se zajedno natrag tek u sumrak, lijepo najeli u mnoštvu domaće kuhinje koja je bila prisutna za velebitaškim stolom i još se umjereno zabavili Ninovom svirkom i pjesmom grijući se uz vatru do <s>ranih jutarnjih</s> prikladnog vremena za spavanje tako da smo svi odmorni i spremni za sljedeći dan.</p>
<p class="wp-block-paragraph">Nedjelja je krenula polako, nekima i blago hladno, ali neprekinuto kuhanje kave je zagrijalo i najsmrznutije. Krenuli smo opet spuštanjem u špilju i zatim dugim i prekrasnim prohodom kroz vrludave kanale koji se svako malo presijecaju. Bez Dalibora koji nas je vodio, bili bismo totalno i potpuno izgubljeni, posve sigurno. Mnoštvo penjanja, spuštanja, istezanja, saginjanja i ponešto provlačenja je učinilo naš put još zabavnijim. Veselim se i saznati ponešto više o posebnim penjačkim tehnikama u speleologiji (više od jednog slajda sljedeći put, molit ću lijepo). Naučili smo i posebno voljeti koraloidne tipove siga jer nam ukazuju na strujanje zraka i vjerojatnost pronalaska novih prolaza.</p>
<p class="wp-block-paragraph">Poslije izlaska smo se ponovno podijelili u dvije skupine. Jedna je odradila orijentaciju u prirodi, a druga je vježbala spuštanje i penjanje po užetu. Nakon toga je došao najtužniji dio svakog našeg izleta, raspremanje, pakiranje i povratak kući.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2026/04/28/6806/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6809" data-id="6809" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/grupno-crtanje_klara-krsticevic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6810" data-id="6810" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/matija-na-dnu_jelena-babic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6815" data-id="6815" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/natovarena-kolona_jelena-babic.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6813" data-id="6813" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/pricanje-o-spitanju_jelena-babic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6814" data-id="6814" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/pricanje-o-topografskom-snimanju_jelena-babic.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6808" data-id="6808" data-orig-size="1080,1440" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/u-iscekivanju-kave_klara-krsticevic.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6812" data-id="6812" data-orig-size="1502,2000" height="1079" loading="lazy" sizes="(max-width: 811px) 100vw, 811px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/velebitaski-stol_matija-hrzenjak.jpg" width="811"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6811" data-id="6811" data-orig-size="2000,1500" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/04/vjezbanje-uzlova_jelena-babic.jpg" width="1080"/></figure>
</figure>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/04/vjezbanje-uzlova_jelena-babic.jpg?fit=1200%2C900&ssl=1$img$,
  $legacy$novosti/spust-u-jamu.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$spust-u-jamu$slug2$,
  true,
  false,
  false,
  $dt$2026-04-28T16:16:28+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha$slug$,
  $title$Sve što je lijepo kratko traje, osim puta do Velebitaškog duha$title$,
  $summary$Možda tek pišući ovaj tekst shvaćam koliki smo uspjeh postigli. U šest tjedana školice napravili smo nešto što ja osobno nisam mogla zamisliti da ću ikada postići.$summary$,
  $body$Autorica: Klara Krstičević
Fotografije: Gorana Perić, Klara Krstičević, Paula Skelin, Lea Okićki
Ako se po zadnjem vikendu školica poznaje, onda je ova, barem po autoricinom mišljenju, bila… zanimljiva. Subotu započinjemo nenormalno rano. Goga naređuje polazak već u sedam ujutro jer je destinacija daleko od Zagreba, a čeka nas i hrpa obaveza. Lagano okupljanje u Dionisu, kavica, zadnja prilika za vježbanje uzlova i krećemo prema odredištu. Put do jama na kojima trebamo pokazati što smo naučili zadnjih nekoliko tjedana bio je malo duži nego što su neki školarci očekivali. Razlog? Naše vodičice Ana i Mia bile su
vrlo
sigurne da znaju najkraći put do kampa. (Hint: Goga je znala kraći. I lakši.)
Školarci nestrpljivo očekuju početak ispita, ali prvo se moramo pripremiti za noć. Slijedi izrada bivka, ovaj put bez pomoći instruktora, i po našoj procjeni, najboljeg do sada. Shvatite to kako hoćete.
Brzo oblačimo opremu i… krećemo. Dok dio školaraca polaže prelazak prečnice, drugi dio radi na liniji za vježbanje na stijeni i liniji za prelazak uzla. U ovom trenutku slijedi prvo jedan od najposebnijih, a onda i jedan od najbizarnijih trenutaka vikenda. Taman dok smo se zagrijali, iznad linije za prelazak prečnice pojavljuje se srna. Gospođa, ja sam odlučila da se zove Jerka, što je kako ćemo uskoro saznati bila ogromna greška jer sam se instant vezala za nju, odlučila se smjestiti na stijenu i sunčati. Taman u trenutku kad se autorica ovog teksta krenula penjati po užetu da demonstrira prelazak uzla, gospođa Jerka odlučuje da je vrijeme da promijeni pozu što se, nažalost, pokazalo kao kobna greška. Jerka pada. Jerka zadobiva po život opasne ozljede. Jerka umire svima nama pred očima. Iako je bila s nama vrlo kratko, Jerka je definitivno ostavila traga na svima nama. Pamtit ćemo ju zauvijek.
I kako sad dalje? Nema vremena za oplakivanje i nastavljamo. Iako smo mislili da će ispit biti najstresniji dio dana, većina će se složiti da smo bili u krivu. Suženje koje nas je dočekalo u Maloj jami zadalo je, barem meni, više muka nego sam ispit (navodno kruži neki video koji prikazuje moju patnju pa nije samo rekla-kazala nego su se svi uvjerili u to). ALI, svi smo uspješno savladali to, a i druge čari Male jame. Sami ispit je na kraju prošao bez previše stresa. Moram pohvaliti sve naše instruktore, ne samo na ispitu već i tijekom školice, koji su pokazali ogromnu količinu razumijevanja za naše strahove, napadaje panike dok visimo u previjesu, pazili da pravilno uplićemo stop-descender, da pijemo vode, dijelili s nama svoju čokoladu dok smo, smrzavajući se na dnu jame, čekali svoj red za izlazak, podupirali nas kad smo sami mislili da više ne možemo, vidjeli potencijal u nama i htjeli da svima, a ponajviše sami sebi dokažemo da mi sve ovo skupa možemo.
Moja grupa prvi dan ispitnog vikenda završava demonstracijom Olgi onoga što smo naučili o klasičnim tehnikama te ispitom iz vezanja uzlova s Lukom (definitivno stresnije od spusta u jamu). Iako su nas instruktori u jednom trenutku protjerali u šumu dok su raspravljali o tome što smo pokazali taj dan — toliko dugo da smo morali zapaliti vlastitu vatricu — sve smo im oprostili kad smo probali gulaš koji su Roberto i njegovi sous-chefovi kuhali. Bez pretjerivanja: može se mjeriti s onim koji kuha moja mama, što je ekstremno velika pohvala. Naravno, mislilo se i na one koji ne jedu meso pa se kuhao i fini rižoto s povrćem.
Drugi dan našeg ispitnog vikenda, a ujedno i posljednji dan školice, prolazi samo malo manje stresno nego prvi. Nakon što smo se po prvi put tijekom školice svi dobro naspavali u našem bivku, instruktori su nas pustili da na miru popijemo kavu i jedemo, ali odmah nakon toga krenuli smo s demonstracijama. Luka nam je pokazao čari tehničkog penjanja (autorica je čvrsto odlučila da se takvim nečim nikada neće baviti), dok su nam Olga i Jelena demonstrirale jednu od tehnika samospašavanja. Potom jednu grupu usmeno ispituje Goga, dok je moja grupa imala sreće i dodijeljeni su nam Dalibor i Ana. Nakon svega ovoga, i dalje malo nesigurna u sebe, u jednom trenutku pitala sam Gogu: “Jesmo li mi položili?” Nakon njezinog “Zar si sumnjala?” shvatila sam da su oni od početka vjerovali u nas i znali da mi to možemo.
Da povratak kući ne prođe glatko pobrinule su se gužve na autocestama koje su nas natjerale da na putu do kupališta na Dobri, gdje smo prali opremu, čak prijeđemo u Sloveniju, a i akumulator u Čedinom autu koji je odlučio da je sada pravi trenutak da se isprazni pa su Goga i Sebastian morali malo pripomoći. Ovogodišnji školarci, sada već speleolozi pripravnici, zaputili su se svojim kućama umorni (čitaj: iscrpljeni), ali sretni i ponosni na sami sebe.
Možda tek pišući ovaj tekst shvaćam koliki smo uspjeh postigli. U šest tjedana školice napravili smo nešto što ja osobno nisam mogla zamisliti da ću ikada postići. Pokazali smo snagu, volju, izdržljivost, disciplinu, prešli preko strahova koji su se na početku činili nepremostivi, brinuli jedni o drugima i stvorili prijateljstva za koja se nadam da će potrajati. Barem jednom sam pomislila da ove ljude poznajem godinama, a ne samo nekoliko tjedana (trauma bonding, I guess).
Za kraj, čestitke svim školarcima, zahvale svim instruktorima i jedna poruka svima koji razmišljaju o bavljenju speleologijom: za ovo sigurno moraš biti bar malo lud, ali ja u svom životu nigdje drugdje nisam upoznala toliko šaroliku skupinu ljudi, u različitim fazama života, s različitim interesima, različitim osobnostima, a koji toliko vole ovu jednu stvar kojom se bave da su spremni izdvojiti hrpetinu svog slobodnog vremena da bi i nekog drugog naučili. Peace out i vidimo se sljedeće godine.
R.I.P. Jerka.$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph"><em>Autorica: Klara Krstičević<br/>Fotografije: Gorana Perić, Klara Krstičević, Paula Skelin, Lea Okićki</em></p>
<p class="wp-block-paragraph"><br/>Ako se po zadnjem vikendu školica poznaje, onda je ova, barem po autoricinom mišljenju, bila… zanimljiva. Subotu započinjemo nenormalno rano. Goga naređuje polazak već u sedam ujutro jer je destinacija daleko od Zagreba, a čeka nas i hrpa obaveza. Lagano okupljanje u Dionisu, kavica, zadnja prilika za vježbanje uzlova i krećemo prema odredištu. Put do jama na kojima trebamo pokazati što smo naučili zadnjih nekoliko tjedana bio je malo duži nego što su neki školarci očekivali. Razlog? Naše vodičice Ana i Mia bile su <em>vrlo</em> sigurne da znaju najkraći put do kampa. (Hint: Goga je znala kraći. I lakši.)</p>
<p class="wp-block-paragraph">Školarci nestrpljivo očekuju početak ispita, ali prvo se moramo pripremiti za noć. Slijedi izrada bivka, ovaj put bez pomoći instruktora, i po našoj procjeni, najboljeg do sada. Shvatite to kako hoćete.</p>
<p class="wp-block-paragraph">Brzo oblačimo opremu i… krećemo. Dok dio školaraca polaže prelazak prečnice, drugi dio radi na liniji za vježbanje na stijeni i liniji za prelazak uzla. U ovom trenutku slijedi prvo jedan od najposebnijih, a onda i jedan od najbizarnijih trenutaka vikenda. Taman dok smo se zagrijali, iznad linije za prelazak prečnice pojavljuje se srna. Gospođa, ja sam odlučila da se zove Jerka, što je kako ćemo uskoro saznati bila ogromna greška jer sam se instant vezala za nju, odlučila se smjestiti na stijenu i sunčati. Taman u trenutku kad se autorica ovog teksta krenula penjati po užetu da demonstrira prelazak uzla, gospođa Jerka odlučuje da je vrijeme da promijeni pozu što se, nažalost, pokazalo kao kobna greška. Jerka pada. Jerka zadobiva po život opasne ozljede. Jerka umire svima nama pred očima. Iako je bila s nama vrlo kratko, Jerka je definitivno ostavila traga na svima nama. Pamtit ćemo ju zauvijek.</p>
<p class="wp-block-paragraph">I kako sad dalje? Nema vremena za oplakivanje i nastavljamo. Iako smo mislili da će ispit biti najstresniji dio dana, većina će se složiti da smo bili u krivu. Suženje koje nas je dočekalo u Maloj jami zadalo je, barem meni, više muka nego sam ispit (navodno kruži neki video koji prikazuje moju patnju pa nije samo rekla-kazala nego su se svi uvjerili u to). ALI, svi smo uspješno savladali to, a i druge čari Male jame. Sami ispit je na kraju prošao bez previše stresa. Moram pohvaliti sve naše instruktore, ne samo na ispitu već i tijekom školice, koji su pokazali ogromnu količinu razumijevanja za naše strahove, napadaje panike dok visimo u previjesu, pazili da pravilno uplićemo stop-descender, da pijemo vode, dijelili s nama svoju čokoladu dok smo, smrzavajući se na dnu jame, čekali svoj red za izlazak, podupirali nas kad smo sami mislili da više ne možemo, vidjeli potencijal u nama i htjeli da svima, a ponajviše sami sebi dokažemo da mi sve ovo skupa možemo.</p>
<p class="wp-block-paragraph">Moja grupa prvi dan ispitnog vikenda završava demonstracijom Olgi onoga što smo naučili o klasičnim tehnikama te ispitom iz vezanja uzlova s Lukom (definitivno stresnije od spusta u jamu). Iako su nas instruktori u jednom trenutku protjerali u šumu dok su raspravljali o tome što smo pokazali taj dan — toliko dugo da smo morali zapaliti vlastitu vatricu — sve smo im oprostili kad smo probali gulaš koji su Roberto i njegovi sous-chefovi kuhali. Bez pretjerivanja: može se mjeriti s onim koji kuha moja mama, što je ekstremno velika pohvala. Naravno, mislilo se i na one koji ne jedu meso pa se kuhao i fini rižoto s povrćem.</p>
<p class="wp-block-paragraph">Drugi dan našeg ispitnog vikenda, a ujedno i posljednji dan školice, prolazi samo malo manje stresno nego prvi. Nakon što smo se po prvi put tijekom školice svi dobro naspavali u našem bivku, instruktori su nas pustili da na miru popijemo kavu i jedemo, ali odmah nakon toga krenuli smo s demonstracijama. Luka nam je pokazao čari tehničkog penjanja (autorica je čvrsto odlučila da se takvim nečim nikada neće baviti), dok su nam Olga i Jelena demonstrirale jednu od tehnika samospašavanja. Potom jednu grupu usmeno ispituje Goga, dok je moja grupa imala sreće i dodijeljeni su nam Dalibor i Ana. Nakon svega ovoga, i dalje malo nesigurna u sebe, u jednom trenutku pitala sam Gogu: “Jesmo li mi položili?” Nakon njezinog “Zar si sumnjala?” shvatila sam da su oni od početka vjerovali u nas i znali da mi to možemo.</p>
<p class="wp-block-paragraph">Da povratak kući ne prođe glatko pobrinule su se gužve na autocestama koje su nas natjerale da na putu do kupališta na Dobri, gdje smo prali opremu, čak prijeđemo u Sloveniju, a i akumulator u Čedinom autu koji je odlučio da je sada pravi trenutak da se isprazni pa su Goga i Sebastian morali malo pripomoći. Ovogodišnji školarci, sada već speleolozi pripravnici, zaputili su se svojim kućama umorni (čitaj: iscrpljeni), ali sretni i ponosni na sami sebe.</p>
<p class="wp-block-paragraph">Možda tek pišući ovaj tekst shvaćam koliki smo uspjeh postigli. U šest tjedana školice napravili smo nešto što ja osobno nisam mogla zamisliti da ću ikada postići. Pokazali smo snagu, volju, izdržljivost, disciplinu, prešli preko strahova koji su se na početku činili nepremostivi, brinuli jedni o drugima i stvorili prijateljstva za koja se nadam da će potrajati. Barem jednom sam pomislila da ove ljude poznajem godinama, a ne samo nekoliko tjedana (trauma bonding, I guess). </p>
<p class="wp-block-paragraph">Za kraj, čestitke svim školarcima, zahvale svim instruktorima i jedna poruka svima koji razmišljaju o bavljenju speleologijom: za ovo sigurno moraš biti bar malo lud, ali ja u svom životu nigdje drugdje nisam upoznala toliko šaroliku skupinu ljudi, u različitim fazama života, s različitim interesima, različitim osobnostima, a koji toliko vole ovu jednu stvar kojom se bave da su spremni izdvojiti hrpetinu svog slobodnog vremena da bi i nekog drugog naučili. Peace out i vidimo se sljedeće godine.</p>
<p class="wp-block-paragraph">R.I.P. Jerka.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2026/05/07/sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6840" data-id="6840" data-orig-size="2000,1333" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/autorica-teksta-na-izlazu-iz-dvojame_paula-skelin.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6844" data-id="6844" data-orig-size="2624,1478" height="608" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/demonstracija-tehnickog-penjanja_gorana-peric.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6843" data-id="6843" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/prelazak-uzla_lea-okicki.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6838" data-id="6838" data-orig-size="2000,1333" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/roberto-kuha-gulas_paula-skelin.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6846" data-id="6846" data-orig-size="1333,2000" height="1080" loading="lazy" sizes="(max-width: 720px) 100vw, 720px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/sebastian-u-maloj-jami_paula-skelin-1.jpg" width="720"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6842" data-id="6842" data-orig-size="1500,2000" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/srna_lea-okicki.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6839" data-id="6839" data-orig-size="1638,2184" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2026/05/skolarci_klara-krsticevic-1.jpg" width="810"/></figure>
</figure>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/demonstracija-tehnickog-penjanja_gorana-peric.jpg?fit=1200%2C676&ssl=1$img$,
  $legacy$novosti/sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha$slug2$,
  true,
  false,
  false,
  $dt$2026-05-07T10:41:39+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$zec-il-zaba-na-vrazjoj-i-bocinoj$slug$,
  $title$‘Zec il’ žaba?’ na Vražjoj i Bocinoj$title$,
  $summary$Kažu da vrijeme leti u dobrom društvu. Tako je proletila i ova škola i evo nas već na zadnjem izletu pred ispit. Nakon što smo na Studeni osvojili svoje prve jame, nastavljamo u istom tonu. Ovaj su put na redu Vražja i Bocina jama u blizini Ogulina.$summary$,
  $body$05.04.-06.04.2025 (55. SOV speleo škola)
Autorica: Lucija Cindrić
Fotografije: Arhiva 55. speleološke škole SOV; Dejan, Goga, Petra, Igor
Kažu da vrijeme leti u dobrom društvu. Tako je proletila i ova škola i evo nas već na zadnjem izletu pred ispit. Nakon što smo na Studeni osvojili svoje prve jame, nastavljamo u istom tonu. Ovaj su put na redu Vražja i Bocina jama u blizini Ogulina.
Jutro počinje već tradicionalnim okupljanjem u Cugu, koji sam ja ovaj put preskočila pa o tome mogu reći samo da se vjerojatno pila kava. Ovaj put nije bilo križnog puta jer se skroz do kampa moglo doći autom. Neki od nas to nisu znali, što je šteta jer bismo se onda natovarili s više stvari koje nam ustvari ne trebaju. Po dolasku u kamp krećemo s izradom bivaka i skupljanjem drva. Slijedio je (do)ručak na već poznatoj plavoj ceradi, koja je ovaj put bila prostrta na pravom stolu i to još pod krovom! Bilo je tu kao i obično svakojakih delikatesa, jedino iznenađene je bilo što Teove poznate slastice ovaj put nisu bile njegovih ruku djelo, jer je bio zauzet učenjem (želimo mu sreću na kolokviju koji ga je u tome spriječio).  Kad smo se svi dobro najeli bio je red otići po zamke i vježbati uzlove. Iako je ispit sve bliže, dobrom dijelu nas neki od uzlova su još uvijek malo zbunjujući, pogotovo kad ih treba vezati oko drveta i to još sa nekooperativnim užetom od 9mm. Posebno nas je zbunjivalo vezanje bulina uplitanjem (odnosno metodom zec kroz rupu i oko drveta) gdje se
vodila živa rasprava s koje strane se ulazi u rupu i kako oko drveta, a nekima zec uopće nije bio zec nego žaba
. U konačnici nam je mozak bio uspješno zapetljan,  a uzao oko drveta kako kome.
Kad smo sve uspješno zavezali dijelimo se u dvije grupe i krećemo u jame! Moja ekipa odlazi prema Bocinoj jami, vrijeme je objektivno gledano jako lijepo – sunčano i toplo, ali subjektivan dojam u korduri i pododijelu je da se kuhamo. Srećom jama je blizu pa ubrzo nalazimo spas u hladovini još neprolistalog drveća,  dijelimo se u parove školarac-instruktor i krećemo. Čekanje na ulazak u jamu ispunili smo nostalgičnim razgovorima o crtićima iz djetinjstva i uz to (bezuspješno) pokušali odgonetnuti kako je jama dobila ime (možda po čovjeku iz sela po nadimku Boca?). Nakon kraće (ili malo dulje) borbe s devijatorom na samom početku ulaza, dolazimo do police na nekih 50m. Jama ide i dalje, ali mi ne, jer je na dnu sifon. Dvije su linije pa nazad možemo tek nakon što se svi spuste, što nam je taman dalo vremena da jamu pretvorimo u fotostudio i okinemo fotku koje se ni Tomislav ne bi posramio. Nakon uspješnog poziranja krećemo van, a dok smo čekali svoj red za penjanje,
zagrijavali smo se po običaju – pjevanjem
. Osim domaćih hitova na repertoaru su se našle dječje pjesmice i mongolsko pjevanje (ili barem pokušaj istog).
Kad smo izašli bilo je već oko 17 sati. Do nas pristiže ekipa iz Vražje jame što znači samo jedno – danas osvajamo obje. Nakon kratke pauze za nešto prigristi krećemo put Vražje jame. Odlučilo se da ovaj put jedna linija bude za spuštanje, a druga za penjanje da se cijela stvar ubrza. Za školarce je to značilo da nemaju instruktora odmah pored sebe pa smo kroz jamu morali više-manje sami, što je bila prilično dobra vježba za ispit. Na ulazak u jamu se svejedno malo čekalo pa smo se stigli diviti  mjesecu i brusiti vještine orijentacije tako što smo po njemu odredili koliko je sati. Opet se raspravljalo o porijeklu imena jame, ovaj put smo došli malo bliže:  dosta je ljudi unutra ušlo,  a još nitko izašao bit će da je zato vražja (tko zna što nas tamo dolje čeka).  Jedino što nas je dočekalo je bio hladan tuš prilikom penjanja (jer jama malo „curi“).  Školarci su i  ovdje išli do nekih 50 metara dubine, ali dio ekipe je kasnije otišao i dalje gdje ih je dočekalo lijepo iznenađenje –
jama u koju se ide već godinama ide dalje!
Svi smo ušli i izašli bez većih problema pa se sretni i zadovoljni vraćamo u kamp da se zagrijemo. Dočekala nas je večera i vatra, uz koju se cijelo noć pjevalo
uz gitaru i Jelinu melodijsku pratnju zviždukanjem
. Ekipa je bila dovoljno inspirirana i da nadopuni stihove jedne velebitaške pjesme (na oduševljenje naše pročelnice Barbare).  Naš trud nam je zaradio doručak od 9 (živio vođa!) s time da nam ovaj put pomicanje sata nije ukralo tih dodatnih sat vremena sna.
Sljedeći dan je bio, po procjeni instruktora, za nas daleko najteži od svih dosad. Uz iznenadno zahlađenje i uzaludne pokušaje da se zagrijemo morali smo se čak dvaput pomaknuti po metar dva između različitih demonstracija! Šalu na stranu, čekale su nas dvije jako zanimljive demonstracije, a nekima je dan stvarno bio i težak jer ljetna vreća za spavanje i snijeg koji je padao ujutro su stvarno loša kombinacija. A nismo bome ni samo sjedili – uzlovi se uvijek mogu ponavljati, a okušali smo se i u bušenju sidrišta gdje se kao najveći izazov pokazalo nalaženje dobre stijene.
Prva na redu je bila
demonstracija samospašavanja
(ustvari prva prva demonstracija je bila kako napraviti sidrište na krovu  uz izbjegavanje pomalo labilne ograde), koja se sastojala od mnogobrojnih koraka koje ja sad ne mogu ponoviti, u sjećanju mi je ostalo samo da je aktivnost vrlo intenzivna i još se u realnoj situaciji sve to mora izvesti unutar dvije minute. Nakon toga
Jenny nam je održala jako zanimljivo predavanje o speleoronjenju
, koje zahtjeva čelične živce i volju jer spaja dvije već dovoljno ekstremne aktivnosti, a uz to je opasno po život i novčanik.
Na kraju, preostalo je samo čekanje ekipe koja je otišla crtati novootkriveni dio Vražie jame. Nakon toga je uslijedilo pakiranje opreme pa pakiranje po autima i
dok si rekao jama ode nam i ovaj vikend
pa nas još samo korak dijeli od kraja, odnosno pravog početka naše speleoavanture!$body$,
  $html$<p class="wp-block-paragraph"><em>05.04.-06.04.2025 (55. SOV speleo škola)<br/>Autorica: Lucija Cindrić<br/>Fotografije: Arhiva 55. speleološke škole SOV; Dejan, Goga, Petra, Igor</em></p>
<p class="wp-block-paragraph">Kažu da vrijeme leti u dobrom društvu. Tako je proletila i ova škola i evo nas već na zadnjem izletu pred ispit. Nakon što smo na Studeni osvojili svoje prve jame, nastavljamo u istom tonu. Ovaj su put na redu Vražja i Bocina jama u blizini Ogulina. </p>
<p class="wp-block-paragraph">Jutro počinje već tradicionalnim okupljanjem u Cugu, koji sam ja ovaj put preskočila pa o tome mogu reći samo da se vjerojatno pila kava. Ovaj put nije bilo križnog puta jer se skroz do kampa moglo doći autom. Neki od nas to nisu znali, što je šteta jer bismo se onda natovarili s više stvari koje nam ustvari ne trebaju. Po dolasku u kamp krećemo s izradom bivaka i skupljanjem drva. Slijedio je (do)ručak na već poznatoj plavoj ceradi, koja je ovaj put bila prostrta na pravom stolu i to još pod krovom! Bilo je tu kao i obično svakojakih delikatesa, jedino iznenađene je bilo što Teove poznate slastice ovaj put nisu bile njegovih ruku djelo, jer je bio zauzet učenjem (želimo mu sreću na kolokviju koji ga je u tome spriječio).  Kad smo se svi dobro najeli bio je red otići po zamke i vježbati uzlove. Iako je ispit sve bliže, dobrom dijelu nas neki od uzlova su još uvijek malo zbunjujući, pogotovo kad ih treba vezati oko drveta i to još sa nekooperativnim užetom od 9mm. Posebno nas je zbunjivalo vezanje bulina uplitanjem (odnosno metodom zec kroz rupu i oko drveta) gdje se <strong>vodila živa rasprava s koje strane se ulazi u rupu i kako oko drveta, a nekima zec uopće nije bio zec nego žaba</strong>. U konačnici nam je mozak bio uspješno zapetljan,  a uzao oko drveta kako kome.</p>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6403" data-orig-size="2096,1572" height="561" loading="lazy" sizes="(max-width: 748px) 100vw, 748px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-1_dejan.jpg" width="748"/></figure>
<p class="wp-block-paragraph">Kad smo sve uspješno zavezali dijelimo se u dvije grupe i krećemo u jame! Moja ekipa odlazi prema Bocinoj jami, vrijeme je objektivno gledano jako lijepo – sunčano i toplo, ali subjektivan dojam u korduri i pododijelu je da se kuhamo. Srećom jama je blizu pa ubrzo nalazimo spas u hladovini još neprolistalog drveća,  dijelimo se u parove školarac-instruktor i krećemo. Čekanje na ulazak u jamu ispunili smo nostalgičnim razgovorima o crtićima iz djetinjstva i uz to (bezuspješno) pokušali odgonetnuti kako je jama dobila ime (možda po čovjeku iz sela po nadimku Boca?). Nakon kraće (ili malo dulje) borbe s devijatorom na samom početku ulaza, dolazimo do police na nekih 50m. Jama ide i dalje, ali mi ne, jer je na dnu sifon. Dvije su linije pa nazad možemo tek nakon što se svi spuste, što nam je taman dalo vremena da jamu pretvorimo u fotostudio i okinemo fotku koje se ni Tomislav ne bi posramio. Nakon uspješnog poziranja krećemo van, a dok smo čekali svoj red za penjanje, <strong>zagrijavali smo se po običaju – pjevanjem</strong>. Osim domaćih hitova na repertoaru su se našle dječje pjesmice i mongolsko pjevanje (ili barem pokušaj istog).</p>
<p class="wp-block-paragraph">Kad smo izašli bilo je već oko 17 sati. Do nas pristiže ekipa iz Vražje jame što znači samo jedno – danas osvajamo obje. Nakon kratke pauze za nešto prigristi krećemo put Vražje jame. Odlučilo se da ovaj put jedna linija bude za spuštanje, a druga za penjanje da se cijela stvar ubrza. Za školarce je to značilo da nemaju instruktora odmah pored sebe pa smo kroz jamu morali više-manje sami, što je bila prilično dobra vježba za ispit. Na ulazak u jamu se svejedno malo čekalo pa smo se stigli diviti  mjesecu i brusiti vještine orijentacije tako što smo po njemu odredili koliko je sati. Opet se raspravljalo o porijeklu imena jame, ovaj put smo došli malo bliže:  dosta je ljudi unutra ušlo,  a još nitko izašao bit će da je zato vražja (tko zna što nas tamo dolje čeka).  Jedino što nas je dočekalo je bio hladan tuš prilikom penjanja (jer jama malo „curi“).  Školarci su i  ovdje išli do nekih 50 metara dubine, ali dio ekipe je kasnije otišao i dalje gdje ih je dočekalo lijepo iznenađenje – <strong>jama u koju se ide već godinama ide dalje!</strong></p>
<p class="wp-block-paragraph">Svi smo ušli i izašli bez većih problema pa se sretni i zadovoljni vraćamo u kamp da se zagrijemo. Dočekala nas je večera i vatra, uz koju se cijelo noć pjevalo <strong>uz gitaru i Jelinu melodijsku pratnju zviždukanjem</strong>. Ekipa je bila dovoljno inspirirana i da nadopuni stihove jedne velebitaške pjesme (na oduševljenje naše pročelnice Barbare).  Naš trud nam je zaradio doručak od 9 (živio vođa!) s time da nam ovaj put pomicanje sata nije ukralo tih dodatnih sat vremena sna.</p>
<p class="wp-block-paragraph">Sljedeći dan je bio, po procjeni instruktora, za nas daleko najteži od svih dosad. Uz iznenadno zahlađenje i uzaludne pokušaje da se zagrijemo morali smo se čak dvaput pomaknuti po metar dva između različitih demonstracija! Šalu na stranu, čekale su nas dvije jako zanimljive demonstracije, a nekima je dan stvarno bio i težak jer ljetna vreća za spavanje i snijeg koji je padao ujutro su stvarno loša kombinacija. A nismo bome ni samo sjedili – uzlovi se uvijek mogu ponavljati, a okušali smo se i u bušenju sidrišta gdje se kao najveći izazov pokazalo nalaženje dobre stijene.</p>
<p class="wp-block-paragraph">Prva na redu je bila <strong>demonstracija samospašavanja</strong>  (ustvari prva prva demonstracija je bila kako napraviti sidrište na krovu  uz izbjegavanje pomalo labilne ograde), koja se sastojala od mnogobrojnih koraka koje ja sad ne mogu ponoviti, u sjećanju mi je ostalo samo da je aktivnost vrlo intenzivna i još se u realnoj situaciji sve to mora izvesti unutar dvije minute. Nakon toga <strong>Jenny nam je održala jako zanimljivo predavanje o speleoronjenju</strong>, koje zahtjeva čelične živce i volju jer spaja dvije već dovoljno ekstremne aktivnosti, a uz to je opasno po život i novčanik.</p>
<p class="wp-block-paragraph">Na kraju, preostalo je samo čekanje ekipe koja je otišla crtati novootkriveni dio Vražie jame. Nakon toga je uslijedilo pakiranje opreme pa pakiranje po autima i <strong>dok si rekao jama ode nam i ovaj vikend</strong> pa nas još samo korak dijeli od kraja, odnosno pravog početka naše speleoavanture!</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/04/25/zec-il-zaba-na-vrazjoj-i-bocinoj/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6405" data-id="6405" data-orig-size="2000,1125" height="607" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-_goga.jpg" width="1080"/></figure>
</figure>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-2 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/04/25/zec-il-zaba-na-vrazjoj-i-bocinoj/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6403" data-id="6403" data-orig-size="2096,1572" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-1_dejan.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6401" data-id="6401" data-orig-size="1572,2096" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina_igor.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6399" data-id="6399" data-orig-size="2096,1572" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-6_dejan.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6397" data-id="6397" data-orig-size="1572,2096" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-5_dejan.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6398" data-id="6398" data-orig-size="1572,2096" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-4_dejan.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6400" data-id="6400" data-orig-size="2096,1572" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-3_dejan.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6402" data-id="6402" data-orig-size="2096,1572" height="810" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-2_dejan.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6407" data-id="6407" data-orig-size="1566,2080" height="1079" loading="lazy" sizes="(max-width: 813px) 100vw, 813px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina_petra.jpg" width="813"/></figure>
</figure>$html$,
  $cat$Speleoškola$cat$,
  $img$https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/04/vrazja-i-bocina-_goga.jpg?fit=1200%2C675&ssl=1$img$,
  $legacy$novosti/zec-il-zaba-na-vrazjoj-i-bocinoj.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$zec-il-zaba-na-vrazjoj-i-bocinoj$slug2$,
  true,
  false,
  false,
  $dt$2025-04-25T07:56:50+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;

insert into public.sov_news (
  slug, title, summary, body, content_html, category, image_url, legacy_url,
  cta_label, cta_url, published, pinned, featured, published_at, source
)
values (
  $slug$zumberacki-odred-za-cistocu$slug$,
  $title$Žumberački odred za čistoću$title$,
  $summary$Ukupno je očišćeno 6 speleoloških objekata na području PP Žumberak – Samoborsko gorje...$summary$,
  $body$Tekst i fotografije; Toni Kočevar
Nakon još jednog dugog i uspješnog speleoistraživačkog ljeta, došlo je vrijeme za nove Velebitaške pobjede, ali ovoga puta u nešto drugačijem okruženju. Tijekom godine već je nekoliko puta najavljivana jesenska akcija uklanjanja otpada iz speleoloških objekata na području PP Žumberak – Samoborsko gorje. Uzevši u obzir sve aspekte akcije (lokacija: Žumberak, vrijeme: idealno, Veliki vođa: Marko Ličko, hrana i smještaj: osigurani), prekvalifikacije iz špiljara u smećare nam nisu teško padale. Postepenim rješavanjem birokracijskih pitanja i prikupljanjem zainteresiranih sudionika, kao udarno vrijeme akcije označen je vikend 13./14.9. Kratko saopćenje o tome što smo sve delali na Žumberku, možete saznati u nadolazećim poglavljima.
Dan Prvi, Subota, 13.9.
Za ljude koji nisu bili dovoljno odvažni zaputiti se prema Žumberku noć ranije (ili poput mene, lijeni), sastajanje s ostatkom ekipe započelo je u ranojutarnjim subotnjim satima u Planinarskoj kući Vodice iznad Sošica. Nakon kratke ćakule, prvenstveno o rezultatima liječničkih pregleda, konzumacije Darkićevih
Wolt
-plati i Klarine
vrhunske
kave, riječ je preuzeo Veliki nam vođa i odradio brifing današnje akcije. Formirane su četiri ekipe pri čemu je svakoj dodijeljen jedan od ciljanih speleoloških objekata (Jamina pod Piskom, Jama na Oklinku, špilja Rakićka, jama Kotari). Prethodno je bilo poznato kako je najveća količina otpada utvrđena u Jamini pod Piskom (35 m
3
) i jami Kotari (20 m
3
), te je stoga, po završetku čišćenja pripadnih jama, dogovoreno udruživanje drugih dviju ekipa s ekipom
Kotari
, dok će ekipi u Jamini pod Piskom (Ličko, Jele, Tibor, Klara, Dora) u pomoć priskočiti lokalna zajednica i djelatnici PP Žumberak.
Prikupljanjem Filipa i opreme potrebne za napad na jamu, Kruno i ja zaputismo se prema Kotarima. Usputna stanica nam je bila špilja Rakićka, na kojoj smo ostavili Ivonu i Nina u željnom iščekivanju Gogina dolaska. Dolaskom na jamu brzinski je dogovorena podjela zadataka – Kruno i ja čistimo vegetaciju i otpad oko i na putu do jame, dok Filip ulazi u jamu i javlja
status quo
. Nedugo nakon ulaska u jamu, Filip nas obavještava o potencijalnom problemu – naišao je na mrtvu kozu ili ovcu, nije siguran što je točno. Nakon kratkih konzultacija s Velikim vođom dogovoreno je kako koza/ovca ide van, a akcija se nastavlja dalje. U tim trenutcima, ekipi se pridružuje i posljednji, neopjevani član Luka, koji se vrlo entuzijastično baca na slaganje sustava za što efikasnije izvlačenje otpada iz jame. Nešto kasnije, pridružuje nam se ekipa
Oklinak
(Mićo, Vesna, Igor), a nedugo zatim i ekipa
Rakićka
(Goga, Ivona, Nino). Potaknuti velikom količinom dostupnog ljudstva unutar i izvan jame, čak i uz nezgode poput pucanja stijene sa sidrištem,
smećarili
smo sve do ranih večernjih sati. Zalaskom sunca zaputili smo se natrag ka PD Vodice na zasluženi odmor i okrepu, a uz vatru smo razmijenili iskustva i izvijestili Velikog vođu o dosadašnjem napretku.
Dan Drugi, Nedjelja, 14.9.
Osvitom novoga dana, čili špiljari-smećari kreću u nove pobjede. Nakon kafenisanja na Ličkovoj djedovini u susjedstvu Posjetiteljskog centra Sošice, slijedi podjela u dvije ekipe – ekipa
Jamina pod Piskom
(Ličko, Jele, Tibor, Klara, Dora) i ekipa
Kotari
(Kruno, Filip, Mićo, Vesna, Igor, Ivona, Nino i ja). Iako smo u međuvremenu ostali bez tandema Goga-Luka, radnog elana nije nedostajalo pa smo poslu prionuli jednako lako kao i prethodnoga dana. Dogovor je bio da će se raditi do 15:00/15:30 kako bismo na vrijeme došli do Sošica i sastali se s drugom ekipom. Ovaj je dan prošao znatno glađe i monotonije nego prethodni (čitaj: nije bilo koze/ovce u jami te ostalih tehničkih poteškoća), pri čemu je posao i dalje bio odrađen – izvađeno je cca 5 m
3
otpada iz jame, količinski slično kao i dan prije. Nakon kratke meze Darkićevih
Wolt
-plata, koje smo se ovaj put sjetili ponijeti, ekipa i oprema bili su spremni za pokret ka Sošicama i finalno izvještavanje Velikog vođe o obavljenom poslu. Nisam bio tamo, ali navodno je bio zadovoljan…
Nekoliko dana kasnije, Veliki se vođa javio s detaljnijim informacijama o protekloj akciji – iz jame Kotari ukupno je izvađeno 9 m
3
otpada te joj je pridružen otpad iz špilje Rakićka koja je očišćena do kraja, dok su u međuvremenu očišćene i Jama kod starog mlina te Cepinka (ukupno 6 transportnih vreći otpada). S druge strane, iz Jamine pod Piskom uklonjeno je ukupno 14 od 35 m
3
otpada što je značilo samo jedno – akciju će trebati ponoviti tijekom nadolazećeg vikenda. Također, kako situacija ne bi bila prejednostavna, u međuvremenu je u jamu Kotari uplovila još jedna ovca (ovaj put je jednoglasno odlučeno da se radi o ovci) koju je bilo potrebno ukloniti.
Dan Treći, Subota, 20.9.
Pa da utvrdimo gradivo od prošlog vikenda:
Za ljude koji nisu bili dovoljno odvažni zaputiti se prema Žumberku noć ranije
(ovaj put ispriječile su se poslovne obveze, a ne lijenost),
sastajanje s ostatkom ekipe započelo je u ranojutarnjim subotnjim satima
ovoga puta u Ličkovoj hiži pored Posjetiteljskog centra Sošice. Nakon ekipnog okupljanja, jedne klasične Klarine kave, Čedinih žudnji za travaricom, druženja s Dorinim sinom Borisom i kratkog brifinga, krenusmo ka Jamini pod Piskom. Odmah po dolasku na jamu Ličko se baca na opremanje, a nedugo nakon ulaze Filip i Dora te kreću s čišćenjem. Ekipa vani (Klara, Filipova djevojka Marta i ja) povlači transportne vreće napunjene otpadom na površinu te otpad iskrcava u prikolicu
Ličkomobila
za kasniji transport na istovarnu lokaciju. Sve se događa pod budnim okom nadzornika Čede. Ekipi u jami kasnije se pridružuje i Goga.
Smećari
se sve do popodnevnih sati kada se ekipa, nakon kratke deliberacije što/kako dalje, odlučuje za povratak u smještaj (PD Vodice) i prebacivanje akcije ovčje ekstrakcije na nedjelju. Dolaskom u PD skupljaju se drva, loži se vatra, peku se kobase i gljive te uz piće i ćakulu, konačno odlazi i na počinak. Možemo reći, još jedan dosta dobar smećarski dan.
Dan Četvrti, Nedjelja, 21.9.
Slatki ranojutarnji san prekida gromki glas Velikog vođe u izvedbi njegova autorskog singla
Snooze button
, čiji je najveći fan definitivno bila Dora. Uz lagano negodovanje određenih članova, ekipa se uspjela spustiti na kaficu u Sošice kako bi se razradio plan. Ovoga puta, on je bio zaista kompleksan – doći do Kotara, spustiti se unutra, upakirati ovcu, izvući sebe i nju van. Oh da, skoro pa sam zaboravio napomenuti, ali sigurno ne možete pogoditi koga je dopala čast vađenja dotične živine (pa ti budi biolog…). Nego, da nastavimo dalje – oko 9:30 konačno se dovukosmo do Kotara, ja ulazim prvi s velikom vrećom za pohranu ovčetine, a za mnom i Dora koja je nevoljko igrala ulogu devijatora prilikom ekstrakcije. O procesu upakiravanja ovce u vreću bolje je ne pričati previše, ako vas zanimaju detalji, pitajte Doru koja je sve pomno promatrala viseći iznad mene. Na kraju se sve dobro završilo, ovca je uspješno uklonjena, a mi se nadamo kako će se na jamu postaviti rešetka koja će spriječiti daljnje ovčje harikirije. Iscrpljeni, ali zadovoljni, još smo jednom kratko objedovali (neDarkićeve plate) te se uz pozdrave zaputili svatko u svome smjeru.
Na kraju, dao bih svim čitateljima
sumarni pregled
ove dvovikendaške (13./14. i 20./21.9.) akcije (ako vam se već neda čitati cijeli izvještaj, ali ipak iskreno vam preporučam da ga pročitate, čuo sam da je dosta dobar): Ukupno je očišćeno
6
speleoloških objekata na području PP Žumberak – Samoborsko gorje, pri čemu je uklonjeno ukupno
30
m
3
otpada. U akciji je sudjelovalo ukupno
18
ljudi, gotovo svi članovi SO Velebit, te jedna članica SO Željezničar.
…kako AI vidi Velikog vođu čišćenja
…i ostatak ekipe$body$,
  $html$<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Tekst i fotografije; Toni Kočevar</p>
<p class="wp-block-paragraph">Nakon još jednog dugog i uspješnog speleoistraživačkog ljeta, došlo je vrijeme za nove Velebitaške pobjede, ali ovoga puta u nešto drugačijem okruženju. Tijekom godine već je nekoliko puta najavljivana jesenska akcija uklanjanja otpada iz speleoloških objekata na području PP Žumberak – Samoborsko gorje. Uzevši u obzir sve aspekte akcije (lokacija: Žumberak, vrijeme: idealno, Veliki vođa: Marko Ličko, hrana i smještaj: osigurani), prekvalifikacije iz špiljara u smećare nam nisu teško padale. Postepenim rješavanjem birokracijskih pitanja i prikupljanjem zainteresiranih sudionika, kao udarno vrijeme akcije označen je vikend 13./14.9. Kratko saopćenje o tome što smo sve delali na Žumberku, možete saznati u nadolazećim poglavljima.</p>
<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Dan Prvi, Subota, 13.9.</p>
<p class="wp-block-paragraph">Za ljude koji nisu bili dovoljno odvažni zaputiti se prema Žumberku noć ranije (ili poput mene, lijeni), sastajanje s ostatkom ekipe započelo je u ranojutarnjim subotnjim satima u Planinarskoj kući Vodice iznad Sošica. Nakon kratke ćakule, prvenstveno o rezultatima liječničkih pregleda, konzumacije Darkićevih <em>Wolt</em>-plati i Klarine <em>vrhunske</em> kave, riječ je preuzeo Veliki nam vođa i odradio brifing današnje akcije. Formirane su četiri ekipe pri čemu je svakoj dodijeljen jedan od ciljanih speleoloških objekata (Jamina pod Piskom, Jama na Oklinku, špilja Rakićka, jama Kotari). Prethodno je bilo poznato kako je najveća količina otpada utvrđena u Jamini pod Piskom (35 m<sup>3</sup>) i jami Kotari (20 m<sup>3</sup>), te je stoga, po završetku čišćenja pripadnih jama, dogovoreno udruživanje drugih dviju ekipa s ekipom <em>Kotari</em>, dok će ekipi u Jamini pod Piskom (Ličko, Jele, Tibor, Klara, Dora) u pomoć priskočiti lokalna zajednica i djelatnici PP Žumberak.</p>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6588" data-orig-size="1832,4080" height="1080" loading="lazy" sizes="(max-width: 485px) 100vw, 485px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/unnamed.jpg" width="485"/></figure>
<p class="wp-block-paragraph">Prikupljanjem Filipa i opreme potrebne za napad na jamu, Kruno i ja zaputismo se prema Kotarima. Usputna stanica nam je bila špilja Rakićka, na kojoj smo ostavili Ivonu i Nina u željnom iščekivanju Gogina dolaska. Dolaskom na jamu brzinski je dogovorena podjela zadataka – Kruno i ja čistimo vegetaciju i otpad oko i na putu do jame, dok Filip ulazi u jamu i javlja <em>status quo</em>. Nedugo nakon ulaska u jamu, Filip nas obavještava o potencijalnom problemu – naišao je na mrtvu kozu ili ovcu, nije siguran što je točno. Nakon kratkih konzultacija s Velikim vođom dogovoreno je kako koza/ovca ide van, a akcija se nastavlja dalje. U tim trenutcima, ekipi se pridružuje i posljednji, neopjevani član Luka, koji se vrlo entuzijastično baca na slaganje sustava za što efikasnije izvlačenje otpada iz jame. Nešto kasnije, pridružuje nam se ekipa <em>Oklinak </em>(Mićo, Vesna, Igor), a nedugo zatim i ekipa <em>Rakićka</em> (Goga, Ivona, Nino). Potaknuti velikom količinom dostupnog ljudstva unutar i izvan jame, čak i uz nezgode poput pucanja stijene sa sidrištem, <em>smećarili</em> smo sve do ranih večernjih sati. Zalaskom sunca zaputili smo se natrag ka PD Vodice na zasluženi odmor i okrepu, a uz vatru smo razmijenili iskustva i izvijestili Velikog vođu o dosadašnjem napretku.</p>
<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Dan Drugi, Nedjelja, 14.9.</p>
<p class="wp-block-paragraph">Osvitom novoga dana, čili špiljari-smećari kreću u nove pobjede. Nakon kafenisanja na Ličkovoj djedovini u susjedstvu Posjetiteljskog centra Sošice, slijedi podjela u dvije ekipe – ekipa <em>Jamina pod Piskom</em> (Ličko, Jele, Tibor, Klara, Dora) i ekipa <em>Kotari</em> (Kruno, Filip, Mićo, Vesna, Igor, Ivona, Nino i ja). Iako smo u međuvremenu ostali bez tandema Goga-Luka, radnog elana nije nedostajalo pa smo poslu prionuli jednako lako kao i prethodnoga dana. Dogovor je bio da će se raditi do 15:00/15:30 kako bismo na vrijeme došli do Sošica i sastali se s drugom ekipom. Ovaj je dan prošao znatno glađe i monotonije nego prethodni (čitaj: nije bilo koze/ovce u jami te ostalih tehničkih poteškoća), pri čemu je posao i dalje bio odrađen – izvađeno je cca 5 m<sup>3</sup> otpada iz jame, količinski slično kao i dan prije. Nakon kratke meze Darkićevih <em>Wolt</em>-plata, koje smo se ovaj put sjetili ponijeti, ekipa i oprema bili su spremni za pokret ka Sošicama i finalno izvještavanje Velikog vođe o obavljenom poslu. Nisam bio tamo, ali navodno je bio zadovoljan…</p>
<p class="wp-block-paragraph">Nekoliko dana kasnije, Veliki se vođa javio s detaljnijim informacijama o protekloj akciji – iz jame Kotari ukupno je izvađeno 9 m<sup>3</sup> otpada te joj je pridružen otpad iz špilje Rakićka koja je očišćena do kraja, dok su u međuvremenu očišćene i Jama kod starog mlina te Cepinka (ukupno 6 transportnih vreći otpada). S druge strane, iz Jamine pod Piskom uklonjeno je ukupno 14 od 35 m<sup>3</sup> otpada što je značilo samo jedno – akciju će trebati ponoviti tijekom nadolazećeg vikenda. Također, kako situacija ne bi bila prejednostavna, u međuvremenu je u jamu Kotari uplovila još jedna ovca (ovaj put je jednoglasno odlučeno da se radi o ovci) koju je bilo potrebno ukloniti.</p>
<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Dan Treći, Subota, 20.9.</p>
<p class="wp-block-paragraph">Pa da utvrdimo gradivo od prošlog vikenda: <em>Za ljude koji nisu bili dovoljno odvažni zaputiti se prema Žumberku noć ranije </em>(ovaj put ispriječile su se poslovne obveze, a ne lijenost),<em> sastajanje s ostatkom ekipe započelo je u ranojutarnjim subotnjim satima </em>ovoga puta u Ličkovoj hiži pored Posjetiteljskog centra Sošice. Nakon ekipnog okupljanja, jedne klasične Klarine kave, Čedinih žudnji za travaricom, druženja s Dorinim sinom Borisom i kratkog brifinga, krenusmo ka Jamini pod Piskom. Odmah po dolasku na jamu Ličko se baca na opremanje, a nedugo nakon ulaze Filip i Dora te kreću s čišćenjem. Ekipa vani (Klara, Filipova djevojka Marta i ja) povlači transportne vreće napunjene otpadom na površinu te otpad iskrcava u prikolicu <em>Ličkomobila</em> za kasniji transport na istovarnu lokaciju. Sve se događa pod budnim okom nadzornika Čede. Ekipi u jami kasnije se pridružuje i Goga. <em>Smećari</em> se sve do popodnevnih sati kada se ekipa, nakon kratke deliberacije što/kako dalje, odlučuje za povratak u smještaj (PD Vodice) i prebacivanje akcije ovčje ekstrakcije na nedjelju. Dolaskom u PD skupljaju se drva, loži se vatra, peku se kobase i gljive te uz piće i ćakulu, konačno odlazi i na počinak. Možemo reći, još jedan dosta dobar smećarski dan.</p>
<p class="has-pale-cyan-blue-background-color has-background wp-block-paragraph">Dan Četvrti, Nedjelja, 21.9.</p>
<p class="wp-block-paragraph">Slatki ranojutarnji san prekida gromki glas Velikog vođe u izvedbi njegova autorskog singla <em>Snooze button</em>, čiji je najveći fan definitivno bila Dora. Uz lagano negodovanje određenih članova, ekipa se uspjela spustiti na kaficu u Sošice kako bi se razradio plan. Ovoga puta, on je bio zaista kompleksan – doći do Kotara, spustiti se unutra, upakirati ovcu, izvući sebe i nju van. Oh da, skoro pa sam zaboravio napomenuti, ali sigurno ne možete pogoditi koga je dopala čast vađenja dotične živine (pa ti budi biolog…). Nego, da nastavimo dalje – oko 9:30 konačno se dovukosmo do Kotara, ja ulazim prvi s velikom vrećom za pohranu ovčetine, a za mnom i Dora koja je nevoljko igrala ulogu devijatora prilikom ekstrakcije. O procesu upakiravanja ovce u vreću bolje je ne pričati previše, ako vas zanimaju detalji, pitajte Doru koja je sve pomno promatrala viseći iznad mene. Na kraju se sve dobro završilo, ovca je uspješno uklonjena, a mi se nadamo kako će se na jamu postaviti rešetka koja će spriječiti daljnje ovčje harikirije. Iscrpljeni, ali zadovoljni, još smo jednom kratko objedovali (neDarkićeve plate) te se uz pozdrave zaputili svatko u svome smjeru.</p>
<p class="has-vivid-green-cyan-background-color has-background wp-block-paragraph">Na kraju, dao bih svim čitateljima <strong>sumarni pregled</strong> ove dvovikendaške (13./14. i 20./21.9.) akcije (ako vam se već neda čitati cijeli izvještaj, ali ipak iskreno vam preporučam da ga pročitate, čuo sam da je dosta dobar): Ukupno je očišćeno <strong>6</strong> speleoloških objekata na području PP Žumberak – Samoborsko gorje, pri čemu je uklonjeno ukupno <strong>30</strong> m<sup>3</sup> otpada. U akciji je sudjelovalo ukupno <strong>18</strong> ljudi, gotovo svi članovi SO Velebit, te jedna članica SO Željezničar.</p>
<figure class="wp-block-gallery has-nested-images columns-default is-cropped wp-block-gallery-1 is-layout-flex wp-block-gallery-is-layout-flex" data-carousel-extra='{"blog_id":125847801,"permalink":"https://sovelebit.wordpress.com/2025/10/13/zumberacki-odred-za-cistocu/"}'>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6579" data-id="6579" data-orig-size="1296,966" height="805" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000125521.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6578" data-id="6578" data-orig-size="2000,900" height="486" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000126218.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6586" data-id="6586" data-orig-size="3024,4032" height="1080" loading="lazy" sizes="(max-width: 810px) 100vw, 810px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000127175.jpg" width="810"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6585" data-id="6585" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128053.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6584" data-id="6584" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128063.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6580" data-id="6580" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128065.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6582" data-id="6582" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128067.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6581" data-id="6581" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128071.jpg" width="1080"/></figure>
<figure class="wp-block-image size-large"><img alt="" class="wp-image-6583" data-id="6583" data-orig-size="1600,1066" height="719" loading="lazy" sizes="(max-width: 1080px) 100vw, 1080px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/1000128073.jpg" width="1080"/></figure>
</figure>
<figure class="wp-block-image size-full"><img alt="" class="wp-image-6595" data-orig-size="1024,768" height="768" loading="lazy" sizes="(max-width: 1024px) 100vw, 1024px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/cave-man-cleaning-the-cave-with-broom-behind-him-is.png" width="1024"/><figcaption class="wp-element-caption">…kako AI vidi Velikog vođu čišćenja</figcaption></figure>
<figure class="wp-block-image size-full"><img alt="" class="wp-image-6600" data-orig-size="1024,768" height="768" loading="lazy" sizes="(max-width: 1024px) 100vw, 1024px" src="https://sovelebit.wordpress.com/wp-content/uploads/2025/10/group-cave-men-and-women-cleaning-the-cave-with-broom.png" width="1024"/><figcaption class="wp-element-caption">…i ostatak ekipe <img alt="🙂" class="emoji" draggable="false" loading="lazy" role="img" src="./Žumberački odred za čistoću – Speleološki odsjek PDS Velebit_files/1f642.svg"/></figcaption></figure>$html$,
  $cat$Ekologija$cat$,
  $img$https://sovelebit.wordpress.com/wp-content/uploads/2025/10/group-cave-men-and-women-cleaning-the-cave-with-broom.png$img$,
  $legacy$novosti/zumberacki-odred-za-cistocu.html$legacy$,
  'Otvori',
  'vijest.html?slug=' || $slug2$zumberacki-odred-za-cistocu$slug2$,
  true,
  false,
  false,
  $dt$2025-10-13T05:53:19+00:00$dt$::timestamptz,
  'static-migration-v5.58.3'
)
on conflict (slug) do update set
  legacy_url = coalesce(public.sov_news.legacy_url, excluded.legacy_url),
  category = coalesce(public.sov_news.category, excluded.category),
  image_url = coalesce(nullif(public.sov_news.image_url,''), excluded.image_url),
  content_html = coalesce(nullif(public.sov_news.content_html,''), excluded.content_html),
  body = coalesce(nullif(public.sov_news.body,''), excluded.body),
  summary = coalesce(nullif(public.sov_news.summary,''), excluded.summary),
  cta_url = case when public.sov_news.cta_url is null or public.sov_news.cta_url = '' or public.sov_news.cta_url like 'novosti/%' then excluded.cta_url else public.sov_news.cta_url end;


-- Public RPCs keep the website simple and avoid exposing unpublished rows to visitors.
create or replace function public.sov_news_public_list(p_limit integer default 60)
returns table (
  id uuid,
  slug text,
  title text,
  summary text,
  category text,
  image_url text,
  image_alt text,
  cta_url text,
  published_at timestamptz,
  pinned boolean,
  featured boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select id, slug, title, summary, category, image_url, image_alt,
         coalesce(nullif(cta_url,''), 'vijest.html?slug=' || slug) as cta_url,
         published_at, pinned, featured
  from public.sov_news
  where published = true
  order by pinned desc, featured desc, published_at desc nulls last, created_at desc
  limit greatest(1, least(coalesce(p_limit,60), 200));
$$;

create or replace function public.sov_news_public_detail(p_slug text)
returns table (
  id uuid,
  slug text,
  title text,
  summary text,
  body text,
  content_html text,
  category text,
  author_name text,
  image_url text,
  image_alt text,
  gallery_urls text[],
  attachment_urls text[],
  pdf_url text,
  cta_label text,
  cta_url text,
  published_at timestamptz,
  legacy_url text
)
language sql
stable
security definer
set search_path = public
as $$
  select id, slug, title, summary, body, content_html, category, author_name,
         image_url, image_alt, gallery_urls, attachment_urls, pdf_url, cta_label, cta_url,
         published_at, legacy_url
  from public.sov_news
  where slug = p_slug and published = true
  limit 1;
$$;

grant execute on function public.sov_news_public_list(integer) to anon, authenticated;
grant execute on function public.sov_news_public_detail(text) to anon, authenticated;

-- Quick check:
-- select slug,title,category,published_at from public.sov_news order by published_at desc;
