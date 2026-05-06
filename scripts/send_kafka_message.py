#!/usr/bin/env python3
import argparse
import json
import os
import sys

try:
    from kafka import KafkaProducer
except ImportError:
    print("Install dependency first: python3 -m pip install kafka-python", file=sys.stderr)
    raise


DEFAULT_PAYLOAD = {
    "title": "Kafka Film",
    "genre": "Test",
    "durationMinutes": 120,
}


def parse_payload(raw_payload: str) -> dict:
    try:
        payload = json.loads(raw_payload)
    except json.JSONDecodeError as exc:
        raise argparse.ArgumentTypeError(f"payload must be valid JSON: {exc}") from exc
    if not isinstance(payload, dict):
        raise argparse.ArgumentTypeError("payload must be a JSON object")
    return payload


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Send a lab 12 JSON command to Kafka.",
        epilog=(
            "Example: scripts/send_kafka_message.py "
            "--entity FILM --operation POST "
            "--payload '{\"title\":\"Kafka Film\",\"genre\":\"Test\",\"durationMinutes\":120}'"
        ),
    )
    parser.add_argument(
        "--bootstrap-server",
        default=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "hl15.zil:9094,hl14.zil:9094"),
        help="Kafka bootstrap server, default: %(default)s",
    )
    parser.add_argument(
        "--topic",
        default=os.getenv("KAFKA_TOPIC", "hl03"),
        help="Kafka topic, default: %(default)s",
    )
    parser.add_argument("--entity", default="FILM", choices=["FILM", "VIEWER", "TICKET"])
    parser.add_argument("--operation", default="POST", choices=["POST", "PUT", "DEL"])
    parser.add_argument("--payload", type=parse_payload, default=DEFAULT_PAYLOAD)
    parser.add_argument("--key", default=None)
    args = parser.parse_args()

    message = {
        "entity": args.entity,
        "operation": args.operation,
        "payload": args.payload,
    }

    producer = KafkaProducer(
        bootstrap_servers=[server.strip() for server in args.bootstrap_server.split(",")],
        key_serializer=lambda key: None if key is None else key.encode("utf-8"),
        value_serializer=lambda value: json.dumps(value, ensure_ascii=False).encode("utf-8"),
    )
    metadata = producer.send(args.topic, key=args.key, value=message).get(timeout=10)
    producer.flush()
    producer.close()

    print(
        "Sent message to "
        f"topic={metadata.topic}, partition={metadata.partition}, offset={metadata.offset}: "
        f"{json.dumps(message, ensure_ascii=False)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
