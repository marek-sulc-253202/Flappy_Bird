# Flappy Bird Android & Django Project

Mobilní hra inspirovaná klasikou Flappy Bird, rozšířená o online funkce, správu hráčů a pokročilou grafickou optimalizaci. Projekt se skládá z klientské části (Android aplikace) a serverové části (Django REST API).

**Autor:** Marek Šulc (ID: 253202)

---

## 🚀 Klíčové vlastnosti

- **Core Gameplay:** Plynulá fyzika ptáčka, nekonečné generování překážek s férovým navazováním mezer.
- **Multiplayer & Profily:** Možnost hrát za různé hráče uložené v online databázi.
- **Systém obtížností:** Tři úrovně (LEHKÁ, NORMAL, TĚŽKÁ), přičemž každá má svůj vlastní oddělený žebříček rekordů.
- **Kustomizace:** Výběr z několika barevných skinů ptáčka (dynamické barvení těla při zachování detailů).
- **Online/Offline režim:** Aplikace automaticky detekuje dostupnost serveru. Pokud je server offline, hra funguje v lokálním režimu.
- **Optimalizace:** Použití techniky Bitmap Caching pro plynulé vykreslování i na slabších zařízeních.
- **Zvukové efekty:** Implementované zvuky pro skok, náraz a bodování s možností ztlumení přímo ve hře.

---

## 🛠 Použité technologie

### Android (Klient)
- **Jazyk:** Java
- **UI:** SurfaceView (vlastní herní smyčka ve vlákně)
- **Síťová komunikace:** Retrofit 2 + Gson
- **Lokální úložiště:** SharedPreferences
- **Grafika:** XML Vector Drawables převedené na Bitmapy

### Django (Server)
- **Jazyk:** Python 3
- **Framework:** Django 5.x
- **Databáze:** SQLite3 (součást Django)

---

## 💻 Jak spustit projekt

### 1. Spuštění serveru
Server běží na tvém notebooku a slouží jako centrální databáze hráčů a skóre.

1. Přejdi do složky se serverem:
   ```bash
   cd server
   ```
2. Vytvoř a aktivuj virtuální prostředí:
   ```bash
   python -m venv FlappyVenv
   source FlappyVenv/bin/activate  # Pro Linux/Mac
   # nebo
   FlappyVenv\Scripts\activate     # Pro Windows
   ```
3. Nainstaluj potřebné knihovny:
   ```bash
   pip install -r requirements.txt
   ```
4. Připrav databázi a spusť server:
   ```bash
   python manage.py migrate
   python manage.py runserver 0.0.0.0:8000
   ```
   *(Příkaz `0.0.0.0` zajistí, že server bude viditelný i pro mobilní telefon v síti.)*

### 2. Spuštění Android aplikace
1. Otevři projekt v **Android Studiu**.
2. V souboru `NetworkManager.java` uprav konstantu `BASE_URL` na hostname nebo IP tvého počítače.
3. Připoj mobilní zařízení (nebo emulátor) a spusť aplikaci pomocí tlačítka **Run**.
4. **Důležité:** Pro online režim musí být mobil i notebook na stejné síti (např. hotspot z mobilu).

---

## 📂 Struktura projektu
- `/app` - Zdrojové kódy Android aplikace (Java, XML, Assety).
- `/server` - Backendová část v Django (Modely, API views, Databáze).
- `/res/drawable` - Definice vektorové grafiky (tělo ptáčka, detaily, trubky).
- `/res/raw` - Zvukové soubory aplikace.
