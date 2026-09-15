FROM python:3.11.13-slim AS runtime
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK=True HOME=/tmp/ocr
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 libglib2.0-0 libgl1 socat && rm -rf /var/lib/apt/lists/*
COPY requirements.lock .
RUN pip install --no-cache-dir -r requirements.lock
COPY pneus_ocr pneus_ocr
COPY ponte_local.py .
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && useradd --uid 10001 --create-home ocr
USER 10001
EXPOSE 8091
ENTRYPOINT ["/entrypoint.sh"]
