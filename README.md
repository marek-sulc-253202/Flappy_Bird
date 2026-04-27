**Flappy Bird Android & Django Project**  
   
 Mobilní hra inspirovaná klasikou Flappy Bird, rozšířená o online funkce, správu hráčů a pokročilou grafickou optimalizaci. Projekt se skládá z klientské části (Android aplikace) a serverové části (Django REST API).  
   
 **Autor:** Marek Šulc (ID: 253202)  
   
 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnEAAAACCAYAAAA3pIp+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAANUlEQVR4nO3OQQmAABRAsSeYxZy/l2ASz0bwbAVvImwJtszMVu0BAPAX51rd1fH1BACA164HxPUF3eyKTGsAAAAASUVORK5CYII=)  
   
 **🚀 Klíčové vlastnosti**  
- **Core Gameplay:** Plynulá fyzika ptáčka s dynamickou rotací dle směru letu, nekonečné generování překážek s inteligentním navazováním mezer pro zajištění hratelnosti.  
- **Online Režim & Profily:** Možnost přepínání mezi Offline a Online režimem přímo v menu. Online režim umožňuje kolování mezi profily hráčů uloženými v centrální databázi.  
- **Správa hráčů:** Integrované funkce pro přidávání nových hráčů a mazání existujících profilů přímo z mobilní aplikace.  
- **Dynamická konfigurace:** Možnost změny IP adresy nebo hostname serveru přímo v běžící aplikaci bez nutnosti úpravy zdrojového kódu.  
- **Systém obtížností:** Tři úrovně (LEHKÁ, NORMAL, TĚŽKÁ), přičemž každá má svůj vlastní oddělený žebříček rekordů uložený lokálně i na serveru.  
- **Kustomizace:** Výběr z několika barevných skinů ptáčka s využitím vrstvené grafiky (dynamické barvení těla při zachování detailů očí a zobáku).  
- **Optimalizace:** Použití techniky Bitmap Caching pro plynulé vykreslování i na slabších zařízeních.  
- **Zvukové efekty:** Implementované zvuky pro skok, náraz a bodování s interaktivní ikonou pro ztlumení dostupnou v menu i během hry.  
 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnEAAAACCAYAAAA3pIp+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAANElEQVR4nO3OQQmAABRAsad4EFMY9fewnUms4E2ELcGWmTmrKwAA/uLeqrU6vp4AAPDa/gDzYgM3ZPdzEgAAAABJRU5ErkJggg==)  
 **🛠 Použité technologie**  
 **Android (Klient)**  
- **Jazyk:** Java  
- **UI:** SurfaceView (vlastní herní smyčka běžící v samostatném vlákně)  
- **Síťová komunikace:** Retrofit 2 + Gson pro asynchronní HTTP požadavky  
- **Lokální úložiště:** SharedPreferences pro permanentní uložení nastavení a lokálních rekordů  
- **Grafika:** Vektorová XML grafika renderovaná do bitmap pro maximální výkon  
 **Django (Server)**  
- **Jazyk:** Python 3  
- **Framework:** Django 5.x (REST API endpointy)  
- **Databáze:** SQLite3 (produkční data hráčů a jejich rekordů)  
 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnEAAAACCAYAAAA3pIp+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAANklEQVR4nO3OMQ2AABAAsSNhRgDK0ML0OlGADCywEZJWQZeZ2aszAAD+4l6rrTq+ngAA8Nr1AKKjBEJaiE2uAAAAAElFTkSuQmCC)  
 **💻 Jak spustit projekt**  
 **1. Spuštění serveru**  
   
 Server běží na notebooku a slouží jako centrální databáze.  
1. Přejdi do složky se serverem:  
2. cd server  
   
    
3. Vytvoř a aktivuj virtuální prostředí:  
4. python -m venv FlappyVenv  
   
  source FlappyVenv/bin/activate  # Pro Linux/Mac  
   
  # nebo  
   
  FlappyVenv\Scripts\activate     # Pro Windows  
   
    
5. Nainstaluj potřebné knihovny:  
6. pip install -r requirements.txt  
   
    
7. Připrav databázi a spusť server:  
8. python manage.py migrate  
   
  python manage.py runserver [ip/hostname zařízení]:8000  
**2. Spuštění Android aplikace**  
9. Otevři projekt v **Android Studiu**.  
10. Připoj mobilní zařízení (nebo emulátor) a spusť aplikaci pomocí tlačítka **Run**.  
11. Pro připojení na server klikni v aplikaci na tlačítko **online** a napiš IP nebo hostname zařízení na kterém běží server.  
12. **Důležité:** Pro online režim musí být mobil i notebook na stejné síti.  
 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnEAAAACCAYAAAA3pIp+AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAANUlEQVR4nO3OMQ2AABAAsSNhQgNikPYDMpnRgQU2QtIq6DIze3UGAMBf3Gu1VcfHEQAA3rseaH0EMiclAP4AAAAASUVORK5CYII=)  
 **📂 Struktura projektu**  
- /app - Zdrojové kódy Android aplikace (Java, XML, Assety).  
- /server - Backendová část v Django (Modely, API views, Databáze).  
- /res/drawable - Definice vektorové grafiky (tělo ptáčka, detaily, trubky).  
- /res/raw - Zvukové soubory aplikace.  
