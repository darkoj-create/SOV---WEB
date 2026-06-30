SOV Admin 1.4.24 - Laptop hub upute

Sto je novo:
- APK sada ima lokalni Laptop hub sync u ekranu Izleti.
- Mobitel moze poslati .sovpkg paket na laptop hub.
- Mobitel moze povuci roster/ekipe koje su slozene na laptopu.
- SOV Cloud ostaje isti i nije zamijenjen laptop hubom.

Na laptopu:
1. Pokreni SOV Teren Hub.
2. U laptop UI-u pogledaj adresu, npr. http://192.168.43.1:8080.
3. Pogledaj PIN.
4. Ako slazes ekipe, otvori dio Izleti i ekipe i tamo slozi teamove.

Na mobitelu:
1. Spoji mobitel na isti Wi-Fi/hotspot kao laptop hub.
2. Otvori SOV Admin APK.
3. Otvori Izleti.
4. U kartici Laptop hub upisi adresu laptopa.
5. Upisi PIN s laptopa.
6. Klikni Spremi.
7. Klikni Test.
8. Ako pise da je hub dostupan, klikni Povuci ekipe s laptopa.

Slanje paketa mobitel -> laptop:
1. U Izleti otvori ili napravi lokalni terenski paket.
2. Na kartici Moji izleti klikni Na laptop.
3. APK ce izvesti .sovpkg i poslati ga laptop hubu na /upload.
4. Na laptopu provjeri inbox/pakete.

Povlacenje teamova laptop -> mobitel:
1. Na laptopu u hub UI-u slozi izlet i ekipe.
2. Na mobitelu u Izleti klikni Povuci ekipe s laptopa.
3. Otvori cloud izlet.
4. U kartici Laptop hub sync vidjet ces lokalno povucene ekipe za taj izlet.

Bitno:
- Laptop hub je dodatni lokalni kanal za teren bez signala.
- Supabase/cloud sync nije diran.
- Za field hub nema novog SQL-a.
- Ako 1.4.23 SQL za posudbe vec postoji, ne treba nista novo pokretati u bazi.

Testiranje:
- U ovom okruzenju nije bilo moguce napraviti assembleDebug jer Gradle wrapper download
  s services.gradle.org vraca HTTP 403 prije kompilacije.
- Izvor je pripremljen kao source build za kompilaciju u Android Studiju ili na racunalu
  gdje Gradle wrapper moze skinuti Gradle 8.7.
