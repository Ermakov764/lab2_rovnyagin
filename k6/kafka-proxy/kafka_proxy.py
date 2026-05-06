"""
LAB13: REST → Kafka — публикация команд VIEWER POST в топик варианта (тот же формат, что scripts/send_kafka_message.py).

Запуск локально:
  pip install -r requirements.txt
  KAFKA_BOOTSTRAP_SERVERS=hl14.zil:9094,hl15.zil:9094 KAFKA_TOPIC=hl03 uvicorn kafka_proxy:app --host 0.0.0.0 --port 8082
"""

from __future__ import annotations

import json
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from kafka import KafkaProducer

TOPIC = os.environ.get("KAFKA_TOPIC", "hl03")
BOOTSTRAP = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "hl14.zil:9094,hl15.zil:9094")
BOOTSTRAP_LIST = [b.strip() for b in BOOTSTRAP.split(",") if b.strip()]

producer: KafkaProducer | None = None


def create_producer() -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=BOOTSTRAP_LIST,
        acks="all",
        request_timeout_ms=60_000,
        key_serializer=lambda k: None if k is None else str(k).encode("utf-8"),
        value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
    )


@asynccontextmanager
async def lifespan(app: FastAPI):
    global producer
    producer = create_producer()
    yield
    if producer is not None:
        producer.flush()
        producer.close()
        producer = None


app = FastAPI(title="LAB13 Kafka proxy (VIEWER → topic)", lifespan=lifespan)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "topic": TOPIC, "bootstrap": BOOTSTRAP_LIST}


@app.post("/produce/viewer")
def produce_viewer(data: dict) -> dict:
    if producer is None:
        raise HTTPException(status_code=503, detail="Producer not ready")
    payload = data.get("payload")
    if not isinstance(payload, dict):
        raise HTTPException(
            status_code=400, detail="body must include 'payload' object with name, email"
        )
    if not payload.get("name") or not payload.get("email"):
        raise HTTPException(
            status_code=400, detail="payload must include non-empty 'name' and 'email'"
        )
    message = {
        "entity": "VIEWER",
        "operation": "POST",
        "payload": payload,
    }
    # Ключ = email — стабильное распределение по партициям при двух консьюмерах
    key = str(payload.get("email")).strip().lower()
    try:
        md = producer.send(TOPIC, key=key, value=message).get(timeout=30)
        return {
            "status": "sent",
            "topic": md.topic,
            "partition": md.partition,
            "offset": md.offset,
        }
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
