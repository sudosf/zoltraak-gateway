#!/bin/bash
# Zoltraak Pod Setup - Ollama + Vision Model

echo "=== Installing system tools ==="
apt update && apt install -y pciutils lshw curl

echo "=== Checking GPU ==="
nvidia-smi

echo "=== Installing Ollama ==="
if ! command -v ollama &> /dev/null; then
  curl -fsSL https://ollama.com/install.sh | sh
else
  echo "Ollama already installed, skipping"
fi

echo "=== Starting Ollama server ==="
export OLLAMA_HOST=0.0.0.0
export OLLAMA_ORIGINS=*
ollama serve > ollama.log 2>&1 &

echo "=== Waiting for Ollama to be ready ==="
until curl -s http://localhost:11434 > /dev/null; do
  echo "Waiting..."
  sleep 2
done
echo "Ollama is up"

echo "=== Pulling models ==="
ollama pull huihui_ai/qwen3.5-abliterated:4b
ollama pull Tohur/natsumura-storytelling-rp-llama-3.1
ollama pull qwen2.5-coder:32b

echo "=== Done ==="
