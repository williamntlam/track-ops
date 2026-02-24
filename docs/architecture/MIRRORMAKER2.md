# Kafka MirrorMaker 2.0 (MM2)

MirrorMaker 2.0 provides **cross-cluster replication** for disaster recovery, data aggregation, or cloud migration. It runs on the Kafka Connect framework and keeps topic configs, ACLs, and **consumer group offsets** in sync so consumers can fail over to the target cluster and resume from the correct position.

## Overview

- **Source cluster**: Primary Kafka (`trackops-kafka`, from `docker/kafka.yml`).
- **Target cluster**: Replica Kafka (`trackops-kafka-target`, from `docker/kafka-target.yml`).
- **MM2 Connect worker**: Dedicated Connect cluster that runs only MM2 connectors (port **8086**), separate from Debezium Connect (port 8083).

## Features

- **Built on Kafka Connect**: Scalable, fault-tolerant replication.
- **Consumer offset translation**: Offsets from the source cluster are mapped to the target so consumer groups can resume after failover.
- **Dynamic topic sync**: New topics and partition changes on the source are created/updated on the target.
- **Cycle detection**: Identity replication policy and topic/group excludes avoid infinite replication loops in multi-cluster setups.
- **Heartbeats**: `MirrorHeartbeatConnector` emits heartbeats to monitor connectivity and replication latency.

## Connectors

| Connector | Purpose |
|-----------|--------|
| **MirrorSourceConnector** (`trackops-mm2-source`) | Replicates topic data and topic configs from source → target. |
| **MirrorCheckpointConnector** (`trackops-mm2-checkpoint`) | Syncs consumer group offsets so groups can fail over to the target. |
| **MirrorHeartbeatConnector** (`trackops-mm2-heartbeat`) | Sends heartbeats to measure connectivity and latency. |

## Deployment

### 1. Start clusters and MM2

Start the source Kafka, then the target Kafka, then the MM2 Connect worker (create the network first if needed):

```bash
docker network create trackops-network 2>/dev/null || true

# Source cluster (and Schema Registry)
docker compose -f docker/kafka.yml up -d

# Target cluster
docker compose -f docker/kafka-target.yml up -d

# MM2 Connect worker (builds image on first run)
docker compose -f docker/mirror-maker-2.yml up -d
```

### 2. Register MM2 connectors

After MM2 Connect is healthy (e.g. `curl -s http://localhost:8086/connectors` returns `[]`):

```bash
./scripts/setup-mm2-connectors.sh
```

Optional: use a different MM2 URL:

```bash
MM2_CONNECT_URL=http://localhost:8086 ./scripts/setup-mm2-connectors.sh
```

### 3. Verify

- **Topics**: Produce to a topic on the source (e.g. `orders.orders`). It should appear on the target with the same name (IdentityReplicationPolicy).
- **Offset sync**: Run a consumer group on the source, then check that the same group’s offsets are present on the target (e.g. via `kafka-consumer-groups` against the target bootstrap).
- **Heartbeats**: Check the heartbeat topic on the target (default naming with IdentityReplicationPolicy) for heartbeat messages.
- **New topics**: Create a new topic on the source; it should be created on the target within the refresh interval (e.g. ~60 seconds).

## Configuration

- **Connector configs**: `docker/mm2/mm2-source-connector.json`, `mm2-checkpoint-connector.json`, `mm2-heartbeat-connector.json`.
- **Replication policy**: `IdentityReplicationPolicy` — target topic names match source (no prefix). For active-active or multi-source aggregation, consider a policy that adds a source prefix (e.g. `source.topic_name`) to avoid collisions.
- **Topics/groups**: Source connector uses `topics: ".*"` and excludes internal/Connect topics. Checkpoint uses `groups: ".*"` with excludes for console/connect/mm2 groups. Adjust regexes in the JSON files if needed.
- **Security**: Current config uses PLAINTEXT. For SASL/SSL, add `source.cluster.security.protocol`, `source.cluster.sasl.*`, and corresponding `target.cluster.*` and SSL settings to each connector config.

## Ports

| Service            | Port | Description        |
|--------------------|------|--------------------|
| Source Kafka       | 9092 | Bootstrap (host)   |
| Target Kafka       | 9094 | Bootstrap (host)   |
| MM2 Connect REST   | 8086 | Connector API      |

## Troubleshooting

- **Connectors not loading**: Ensure the MM2 Connect image was built and the connect-mirror JAR is on the plugin path (`docker/mirror-maker-2/Dockerfile`).
- **No replication**: Confirm both Kafka clusters are healthy and the MM2 worker can reach both (e.g. `docker exec trackops-mirror-maker-2 curl -s trackops-kafka:9092` and `trackops-kafka-target:9092` from inside the container).
- **Offset sync / failover**: Checkpoint connector must run; allow time for offset-syncs and heartbeats. Use the same consumer group ID on the target after failover.
