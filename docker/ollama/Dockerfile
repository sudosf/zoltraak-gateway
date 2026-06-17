FROM nvidia/cuda:12.4.1-runtime-ubuntu22.04

RUN apt update -y && apt install -y pciutils lshw curl zstd
RUN curl -fsSL https://ollama.com/install.sh | sh

ENV OLLAMA_HOST=0.0.0.0
ENV OLLAMA_ORIGINS=*
ENV OLLAMA_MODELS=/workspace/ollama/models

CMD ["ollama", "serve"]