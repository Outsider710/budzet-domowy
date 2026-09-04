# Budżet Domowy

Prosta aplikacja Android (Kotlin + Jetpack Compose + Room) do lokalnego zarządzania budżetem: przychody, wydatki, kategorie i saldo miesiąca. Bez konta, bez internetu, bez reklam — zaprojektowana tak, by spełnić wymogi Google Play i nie ryzykować blokady za „Limited Functionality”.

**Package ID:** `com.budzetdomowy.app` (zmień przed publikacją na własny, unikalny).

---

## Wymagania deweloperskie

- Android Studio Ladybug+ / JDK 17
- Android SDK 35
- Konto [Google Play Console](https://play.google.com/console) (osobiste po 13.11.2023 → obowiązkowy closed test 12×14)

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release AAB (po skonfigurowaniu keystore — patrz niżej)
./gradlew :app:bundleRelease
```

Artifacty:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/bundle/release/app-release.aab`

---

## Signing (upload key)

1. Skopiuj `keystore.properties.example` → `keystore.properties`.
2. Wygeneruj keystore:

```bash
keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

3. Uzupełnij `keystore.properties` (hasła, ścieżka do `.jks`).
4. **Nie commituj** `keystore.properties`, `*.jks`, `*.keystore`.
5. Zbuduj AAB: `./gradlew :app:bundleRelease`.

Zachowaj kopię zapasową keystore — bez niego nie zaktualizujesz aplikacji w Play.

---

## Privacy Policy (obowiązkowa)

1. Uzupełnij placeholdery w [`docs/privacy-policy.html`](docs/privacy-policy.html) (imię/nazwa z Play Console + e-mail).
2. Opublikuj plik pod publicznym HTTPS (GitHub Pages, Netlify, własna domena).
3. Wklej URL w Play Console → App content → Privacy policy.
4. Podmień `privacy_policy_url` w [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml) (link w Ustawieniach aplikacji).

Teksty store listing: [`docs/store-listing-pl.md`](docs/store-listing-pl.md).

---

## Checklista Google Play Console

### A. Utworzenie aplikacji
- [ ] Create app → nazwa **Budżet Domowy**, język domyślny PL, darmowa, typ App
- [ ] Declarations: ads = **No**, zgodność z politykami

### B. Store listing
- [ ] Tytuł, krótki i pełny opis (z `docs/store-listing-pl.md`)
- [ ] Ikona 512×512, feature graphic 1024×500, min. 2 screenshoty z **realnego UI**
- [ ] Kategoria: Finanse lub Narzędzia
- [ ] Dane kontaktowe / e-mail

### C. App content (musi być Complete przed closed testem)
- [ ] **Privacy policy** — publiczny URL
- [ ] **App access** — All functionality available without special access (brak logowania)
- [ ] **Ads** — No
- [ ] **Content ratings** — kwestionariusz IARC (narzędzie / finanse osobiste, bez treści wrażliwych)
- [ ] **Target audience** — nie targetować dzieci; unikaj Designed for Families
- [ ] **News app** — No
- [ ] **Data safety** — patrz sekcja poniżej
- [ ] **Government / financial features** — to lokalny tracker wydatków użytkownika, **nie** bankowość / płatności; odpowiadaj zgodnie z faktami w formularzu

### D. Data safety (dla tej aplikacji)
- [ ] Does your app collect or share any of the required user data types? → **No**
- [ ] (Jeśli formularz pyta o dane lokalne: transakcje są tylko on-device, nie zbierane przez Ciebie)
- [ ] Security practices: dane nie opuszczają urządzenia; brak szyfrowania w tranzycie (brak sieci)
- [ ] Upewnij się, że deklaracja = rzeczywistość (brak analytics/Ads SDK w `build.gradle.kts`)

### E. Closed testing — obowiązkowe (konto osobiste po 13.11.2023)
1. Test and release → Testing → **Closed testing** → utwórz release, wgraj `.aab`.
2. Utwórz listę e-mail z **min. 12** kontami Google.
3. Wyślij testerom link opt-in; muszą **zaakceptować** zaproszenie (status Opted in).
4. Utrzymaj **≥ 12 opted-in bez przerwy przez 14 dni**.
   - Opt-out resetuje ciągłość dla tej osoby.
5. Poproś testerów o użycie: dodaj transakcję, edycja, zmiana miesiąca, usuń.
6. Zbieraj feedback (e-mail / formularz / Testing feedback w Console).
7. Wprowadź **co najmniej 1 poprawkę** na podstawie feedbacku i wgraj nowy build do closed testu (Google pyta o to we wniosku o Production).

Oficjalnie: [Wymagania testów dla kont osobistych](https://support.google.com/googleplay/android-developer/answer/14151465).

### F. Wniosek o Production (`Apply for production`)
Po zielonym checklistcie 12×14 na Dashboard:

| Sekcja | Co napisać (konkretnie, nie ogólniki) |
|---|---|
| Rekrutacja testerów | np. rodzina, znajomi, koledzy z pracy — jak zaprosiłeś |
| Łatwość rekrutacji | wybór z listy |
| Zaangażowanie | czy używali dodawania/edycji/filtrów miesiąca |
| Feedback | 3–5 konkretnych uwag + kanał zbierania |
| Target audience | osoby prowadzące domowy budżet w PL |
| Value proposition | szybki lokalny rejestr wydatków bez konta |
| Install estimate | realistycznie (np. poniżej 10 tys.) |
| Zmiany po teście | np. poprawka walidacji kwoty / etykiet kategorii |
| Gotowość | brak crashy, Data safety OK, store listing kompletny |

### G. Production
- [ ] Po akceptacji wniosku: utwórz release Production z AAB
- [ ] Full rollout lub staged (np. 20% → 100%)
- [ ] Review zwykle do ~7 dni

---

## Harmonogram

| Tydzień | Działanie |
|---|---|
| 0 | Keystore, privacy URL, grafiki, upload AAB, start closed test |
| 1–3 | 14 dni × 12 testerów + poprawki |
| ~3 | Apply for production |
| ~3–4 | Production live |

---

## Czego unikać (ryzyko blokady konta)

- Pusta / „Hello World” / jedna statyczna strona
- Fałszywy listing i cudze screenshoty
- Data safety sprzeczne z SDK
- Fake testerzy bez realnego użycia
- Klon bez wartości / spam

---

## Struktura projektu

```
app/src/main/java/com/budzetdomowy/app/
  data/          Room + Repository
  ui/home/       Saldo + lista miesiąca
  ui/add/        Dodawanie / edycja / usuwanie
  ui/settings/   Privacy + wersja
  util/          Formatowanie PLN i dat
docs/
  privacy-policy.html
  store-listing-pl.md
```

## Licencja

Kod startowy do własnego użytku / publikacji pod Twoim kontem deweloperskim.
Zmień `applicationId` i branding przed publikacją.
