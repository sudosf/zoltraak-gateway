# Persistent Ollama Setup on RunPod (Network Volume)

This guide sets up **persistent** Ollama so new pods start fast with models already in GPU VRAM.

## Motivation

RunPod pods have ephemeral storage, everything outside `/workspace` disappears when the pod stops. This setup keep
Ollama + models on the network volume and run a lightweight init script on boot.

## Step 1: One-Time Setup

```bash
apt update && apt install -y zstd curl

mkdir -p /workspace/ollama_bin

curl -fsSL https://ollama.com/download/ollama-linux-amd64.tar.zst | tar --zstd -xC /workspace/ollama_bin
```

**Locations:**

- Binary: `/workspace/ollama_bin/bin/ollama`
- Models: `/workspace/ollama/models` (setup on step 2 below)

## Step 2: Create Entrypoint Script

Create `/workspace/entrypoint.sh`:

```bash
#!/bin/bash
sleep 5

apt update && apt install -y pciutils lshw zstd

export OLLAMA_HOST="0.0.0.0"
export OLLAMA_ORIGINS="*"
export OLLAMA_MODELS="/workspace/ollama/models"

echo "=== Starting Ollama ==="
/workspace/ollama_bin/bin/ollama serve > /workspace/ollama.log 2>&1 &

echo "=== Waiting for server... ==="
until curl -s http://127.0.0.1:11434/ > /dev/null; do sleep 2; done

echo "Ollama ready. Preloading model..."
TARGET_MODEL="llama3.1"   # set preferred pre-loaded model

curl -s -X POST http://127.0.0.1:11434/api/generate -d "{
  \"model\": \"$TARGET_MODEL\",
  \"keep_alive\": -1
}" > /dev/null &

echo "Preload started"
```

Make it executable:

```bash
chmod +x /workspace/entrypoint.sh
```

## Step 3: RunPod Template Settings

Set **Docker Command** to:

```bash
bash -c "bash /workspace/entrypoint.sh & exec /start.sh"
```

This runs your script in the background and lets RunPod's normal startup finish (avoids restart loops).

## Key Notes

- `keep_alive: -1` keeps the model loaded in VRAM.
- First preload after pod start can take 30–90 seconds.
- Change `TARGET_MODEL` to match your model (e.g. `llama3.1`, `mistral`, etc.).
- Logs go to `/workspace/ollama.log`
