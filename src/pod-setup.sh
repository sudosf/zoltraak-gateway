#!/bin/bash
# Zoltraak Pod Setup - Ollama + Vision Model
set -e

echo "=== Installing system tools ==="
apt update && apt install -y pciutils lshw curl

echo "=== Checking GPU ==="
nvidia-smi

echo "=== Installing Ollama ==="
curl -fsSL https://ollama.com/install.sh | sh

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

echo "=== Pulling vision model ==="
ollama pull huihui_ai/qwen3.5-abliterated:9b

echo "=== Done ==="
echo "Ollama log: tail -f ollama.log"
echo "Test: ollama run huihui_ai/qwen3.5-abliterated:9b"