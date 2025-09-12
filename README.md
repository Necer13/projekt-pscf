# PSCF – Simon Go Manager

Aplikacja mobilna do sterowania urządzeniami cybernetyczno-fizycznymi (CPS) przy użyciu gestów ręki.  

Umożliwia m.in.:  

- Sterowanie przekaźnikami (Switch)  
- Regulację jasności (Dimmer)  
- Obsługę termostatu (Thermostat)  
- Mapowanie gestów na konkretne urządzenia i akcje  
- Eksport / import konfiguracji gestów  

---

## Technologie

- **Android / Kotlin**  
- **CameraX** – analiza obrazu w czasie rzeczywistym  
- **MediaPipe** – detekcja gestów ręki  
- **OkHttp** – wysyłanie komend HTTP do urządzeń  
- **SharedPreferences / JSON** – przechowywanie konfiguracji gestów  
- **MVVM / Fragmenty** – struktura aplikacji  

---

## Struktura projektu
```
pl.polsl.simon_go_manager/
├─ data/ # Dane aplikacji i domyślne akcje
│ ├─ DefaultActions.kt
│ └─ GestureConfigManager.kt
├─ model/ # Modele danych
│ ├─ GestureAction.kt
│ └─ DefaultActions.kt
├─ ui/
│ ├─ devices/ # Definicja urządzeń i typów
│ │ ├─ Device.kt
│ │ └─ DeviceStorage.kt
│ ├─ gesture/ # Logika detekcji gestów
│ │ └─ GestureRecognitionFragment.kt
│ └─ settings/ # Ustawienia, mapowanie gestów
│ └─ AdvancedSettingsFragment.kt
├─ utils/ # Narzędzia pomocnicze
│ └─ DeviceStorage.kt
```
---

## Funkcjonalności

### Sterowanie urządzeniami
- **Switch** – włącz / wyłącz przekaźniki pojedynczo lub oba naraz.  
- **Dimmer** – ustawienie jasności od 0% do 100%, możliwość zwiększania / zmniejszania o 10%.  
- **Thermostat** – włącz / wyłącz, ustaw temperaturę, tryb BOOST, podwyższanie / obniżanie temperatury o 1°C.  

### Gesty
Obsługiwane gesty ręki:  

- Thumb_Up  
- Thumb_Down  
- Victory  
- Closed_Fist  
- Pointing_Up  
- Open_Palm

### Mapowanie gestów
- Gesty można mapować na urządzenia i konkretne akcje.  
- Konfiguracja przechowywana w `SharedPreferences`.  
- Możliwość eksportu / importu konfiguracji w formacie JSON.  

---

## Instalacja

1. Sklonuj repozytorium:  
```bash
git clone https://github.com/twoje-repo/pscf.git
```
2. Otwórz w Android Studio.

3. Skonfiguruj urządzenia w DeviceStorage z ich IP.

4. Uruchom aplikację na urządzeniu Android.
