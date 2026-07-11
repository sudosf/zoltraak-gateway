![Maven Test](https://github.com/sudosf/zoltraak-gateway/actions/workflows/master.yml/badge.svg)
![Qodana](https://github.com/sudosf/zoltraak-gateway/actions/workflows/qodana_code_quality.yml/badge.svg)
![Release](https://github.com/sudosf/zoltraak-gateway/actions/workflows/release.yml/badge.svg)

# Zoltraak Gateway

Control plane for self-hosted LLM workloads. Manages GPU cloud instances on demand, proxies requests to Ollama and ComfyUI, and exposes a unified REST API for creative and coding features.

For backlog items and progress on issues and new features, please see
the [Project Board](https://github.com/users/sudosf/projects/2)

## Prerequisites

- Java 25
- Maven 3.9+
- A Runpod / Vast.ai account with credits
- Ollama setup and running on [Runpod](https://console.runpod.io/deploy) / [Vast.ai](https://vast.ai) pod

## Quick start

### Requirements

- [Docker](https://docs.docker.com/engine/install/)
- A [Runpod](https://console.runpod.io/deploy) / [Vast.ai](https://vast.ai) account and API key

### Step 1: Download the required files

Create a directory to hold the deployment files and move into it:

```bash
mkdir zoltraak-gateway
cd zoltraak-gateway
```

Download `docker-compose.yml` and `example.env` from the latest release:

```bash
wget -O docker-compose.yml https://github.com/sudosf/zoltraak-gateway/releases/latest/download/docker-compose.yml
wget -O .env https://github.com/sudosf/zoltraak-gateway/releases/latest/download/example.env
```

### Step 2: Configure your environment

Populate your `.env` with your environment variables (see `example.env` for reference):

```
RUNPOD_API_KEY=your_key_here
```

### Step 3: Start the gateway

```bash
docker compose up -d
```

The gateway will be available at `http://localhost:8081`

## Updating

Pull the latest image and restart:

```bash
docker compose pull && docker compose up -d
```

## Documentation

- [Architecture](https://www.notion.so/Architecture-3543302b53c48074acdbc3bdc0f178dd?source=copy_link)
- [Project Structure](https://www.notion.so/Project-Structure-3543302b53c480a3b990ceb2b09b4954?source=copy_link)
- [Sequence Diagrams](https://www.notion.so/Sequence-Diagrams-3543302b53c4807fa371da27a3eabe8b?source=copy_link)
- [Domain Models](https://www.notion.so/Domain-Models-3543302b53c480249e0cd3faba0d6c23?source=copy_link) 
