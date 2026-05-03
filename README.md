# Zoltraak Gateway

Control plane for self-hosted model workloads. Manages GPU cloud instances on demand, proxies requests to Ollama and ComfyUI, and exposes a unified REST API for creative and coding features.

## Prerequisites

- Java 25
- Maven 3.9+
- A RunPod or Vast.ai account with credits
- Ollama running locally on the home server (for bot intent classification)

## Getting Started

Clone the repository:

```bash
git clone https://github.com/sudosf/zoltraak-gateway.git
cd zoltraak-gateway
```

Copy the environment variable template and fill in your values:

```bash
cp .env.example .env
```

Run in development mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Environment Variables

| Variable | Description |
|---|---|
| ZOLTRAAK_JWT_SECRET | JWT signing secret, minimum 256 bits |
| RUNPOD_API_KEY | RunPod API key from settings |
| RUNPOD_POD_ID | Pod ID from RunPod dashboard |
| VASTAI_API_KEY | Vast.ai API key |
| VASTAI_INSTANCE_ID | Instance ID from Vast.ai console |
| TELEGRAM_BOT_TOKEN | Token from BotFather |
| TELEGRAM_SECRET_TOKEN | Secret for validating Telegram webhook calls |
| TELEGRAM_WEBHOOK_URL | Public HTTPS URL Telegram delivers webhooks to |

## Running Tests

```bash
./mvnw test
```

## Building for Production

```bash
./mvnw clean package -DskipTests
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

## Documentation

- [Architecture](https://www.notion.so/Architecture-3543302b53c48074acdbc3bdc0f178dd?source=copy_link)
- [Project Structure](https://www.notion.so/Project-Structure-3543302b53c480a3b990ceb2b09b4954?source=copy_link)
- [Sequence Diagrams](https://www.notion.so/Sequence-Diagrams-3543302b53c4807fa371da27a3eabe8b?source=copy_link)
- [Domain Models](https://www.notion.so/Domain-Models-3543302b53c480249e0cd3faba0d6c23?source=copy_link) 