# Evernote Tagging Pipeline

Eine lokale/Container-basierte Pipeline zur Verarbeitung von Evernote-Daten inkl. **ENEX-Parsing**, **CSV-Generierung**, **OCR (TrOCR/Paddle)**, **Tag-Inferenz** und **Modell-Training**.  
Die Architektur trennt **Seed-Daten**, **Python-API-Services (Uvicorn/FastAPI)** und **Java-Runner/Orchestrierung**.

---

## Architektur (vereinfacht)

docker-compose-services.yaml:
```text
            Host
   ┌───────────────────┐
   │ ./data            │
   │  └─ .jg-evernote  │
   └─────────┬─────────┘
             │  bind mount
             ▼
   ┌─────────────────────────────── Docker Compose Network ────────────────────────────────┐
   │                                                                                       │
   │  [data-init]  (one-shot)                                                              │
   │   image: data                                                                         │
   │   cp seed → /data/.jg-evernote                                                        │
   │        │                                                                              │
   │        ├───────────────► shared volume (/data) ◄───────────────────────────────┐      │
   │        │                                                                       │      │
   │  [app1] python-services                                                        │      │
   │   FastAPI + Uvicorn (Port 8000)                                                │      │
   │   OCR / Training / Inference                                                   │      │
   │        ▲                                                                       ▼      │
   │        │ HTTP                                                             [app2] java-services
   │        │                                                             Orchestrierung / Calls
   │        │                                                             Base URL: http://app1:8000
   │                                                                                       │
   └───────────────────────────────────────────────────────────────────────────────────────┘
```

docker-compose-enex.yaml:
```text
            Host
   ┌───────────────────┐
   │ ./data            │
   │  └─ .jg-evernote  │
   └─────────┬─────────┘
             │  bind mount
             ▼
   ┌─────────────── Docker Compose Network ───────────────┐
   │                                                      │
   │  [data-init]  (one-shot)                             │
   │   image: data                                        │
   │   cp seed → /data/.jg-evernote                       │
   │        │                                             │
   │  [enex-parse] java-enex                              │
   │   parse enex files                                   │
   │        │                                             │
   │  [enex-build-csv] java-enex                          │
   │   build csv files (for training)                     │
   │                                                      │
   └──────────────────────────────────────────────────────┘
```

---

## Quickstart (Docker)

Alle Funktionen sind per shell-scripts im Verzeichnis `shell` zur Verfügung gestellt:

### Ohne Docker
#### build
```bash
build_java.sh
```
#### run tests
```bash
run_tests_all.sh # führt alle tests aus
```

#### run services
```bash
run_python_server.sh # startet den python server
run_java_runner.sh   # startet den java service
```

#### run enex
```bash
run_java_enex.sh # startet den enex-Lauf
```

### Mit Docker
#### build
```bash
build_docker.sh
```

#### run services
```bash
run_docker_compose_services.sh
```

#### run enex
```bash
run_docker_compose_enex.sh
```



## Lizenz
Intern/privat
