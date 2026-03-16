# DPDP Notification Consumer

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-LGPL%203.0-blue.svg)](LICENSE)
[![DPDP Act 2023](https://img.shields.io/badge/DPDP%20Act-2023%20Compliant-success.svg)](https://www.meity.gov.in/writereaddata/files/Digital%20Personal%20Data%20Protection%20Act%202023.pdf)

> Kafka consumer for asynchronous notification delivery via multiple channels (SMS, Email, Webhooks)

The DPDP Notification Consumer is the delivery engine of the Digital Personal Data Protection (DPDP) Consent Management System, responsible for consuming notification events from Kafka and delivering them through multiple channels. This microservice ensures reliable, asynchronous notification delivery with retry mechanisms and comprehensive status tracking.

**Documentation:** https://docs.jio.com/dpdp-cms/
**Repository:** https://dev.azure.com/JPL-Limited/dpdp-notification-consumer

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Kafka Consumers](#kafka-consumers)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Dependencies](#dependencies)
- [Support](#support)

## Features

### Core Capabilities

- **Multi-Channel Delivery** - SMS, Email, and Webhook/Callback notification delivery
- **Kafka-Based Processing** - Consumes events from 3 dedicated Kafka topics
- **Retry Mechanisms** - Exponential backoff retry with configurable attempts (3 attempts default)
- **Template Resolution** - Dynamic template argument substitution using master lists from MongoDB
- **Status Tracking** - Updates notification status in tenant-specific databases
- **DLT Compliance** - TRAI DLT entity and template ID validation for SMS notifications
- **Token Management** - DigiGov OAuth2 token caching and automatic refresh
- **Multi-Tenant Support** - Dynamic MongoDB database routing per tenant
- **Manual Acknowledgment** - Ensures reliable message processing with Kafka manual acks
- **Circuit Breaker Pattern** - Resilience against external API failures

### Notification Channels

1. **SMS Notifications**
   - DigiGov SMS API integration
   - DLT entity and template validation
   - Sender ID customization
   - Delivery status tracking
   - 60-second API timeout

2. **Email Notifications**
   - DigiGov Email API integration
   - HTML and plain text support
   - Attachment support (Base64 encoded)
   - Custom sender name and reply-to
   - 90-second API timeout

3. **Webhook/Callback Notifications**
   - HTTP POST to configured URLs
   - JWT-signed payloads for security
   - Custom headers support
   - 60-second timeout
   - Retry on failure

## Architecture

### High-Level Architecture

```
┌──────────────────────┐      ┌─────────────────────┐      ┌──────────────────┐
│  Notification Module │      │  Kafka Topics       │      │  Notification    │
│  (Producer)          │─────▶│  - SMS              │─────▶│  Consumer        │
│  Port 9003           │      │  - Email            │      │  Port 9020       │
└──────────────────────┘      │  - Callback         │      └──────────────────┘
                              └─────────────────────┘               │
                                                                    │
                    ┌───────────────────────────────────────────────┼─────────────┐
                    │                                               │             │
                    ▼                                               ▼             ▼
        ┌───────────────────────┐                     ┌──────────────────────────────┐
        │  MongoDB (Multi-Tenant)│                     │  DigiGov Partner Portal      │
        │  - Tenant Databases    │                     │  - SMS API                   │
        │  - Master Lists        │                     │  - Email API                 │
        │  - Status Updates      │                     │  - Token Generation          │
        └───────────────────────┘                     └──────────────────────────────┘
                                                                    │
                                                                    ▼
                                                        ┌──────────────────────────────┐
                                                        │  External Systems            │
                                                        │  - SMS Gateway               │
                                                        │  - Email Gateway             │
                                                        │  - Webhook Endpoints         │
                                                        └──────────────────────────────┘
```

### Technology Stack

- **Framework:** Spring Boot 3.5.7
- **Language:** Java 21
- **Database:** MongoDB 5.0+ (multi-tenant)
- **Messaging:** Apache Kafka 2.8+ with SASL/Kerberos
- **Caching:** Caffeine (in-memory)
- **Monitoring:** Micrometer + Prometheus
- **Logging:** Logback with JSON encoding

### Kafka Topics (Consumed)

- `DEV_CMS_NOTIFICATION_SMS` - SMS notification messages
- `DEV_CMS_NOTIFICATION_EMAIL` - Email notification messages
- `DEV_CMS_NOTIFICATION_CALLBACK` - Webhook/callback notification messages

## Prerequisites

- **Java 21** or higher
- **Apache Maven 3.8+**
- **MongoDB 5.0+**
- **Apache Kafka 2.8+**
- **Docker 20.10+** (for containers)
- **Kubernetes 1.21+** (for orchestration)

## Quick Start

### 1. Clone Repository

```bash
git clone https://dev.azure.com/JPL-Limited/dpdp-notification-consumer
cd dpdp-notification-consumer
```

### 2. Configure Environment

```bash
export MONGODB_URI=mongodb://localhost:27017/
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export DIGIGOV_ADMIN_CLIENT_ID=your_client_id
export DIGIGOV_ADMIN_CLIENT_SECRET=your_client_secret
```

### 3. Build & Run

```bash
mvn clean package
mvn spring-boot:run
```

### 4. Verify

```bash
curl http://localhost:9020/notification-consumer/actuator/health
```

## Configuration

### Environment Variables

#### Server (Port 9020)
- `SERVER_PORT`: `9020`
- `SERVER_SERVLET_CONTEXT_PATH`: `/notification-consumer`

#### Kafka Consumer
- `KAFKA_GROUP_ID`: `DEV_CMS_NOTIFICATION`
- `SMS_TOPIC`: `DEV_CMS_NOTIFICATION_SMS`
- `EMAIL_TOPIC`: `DEV_CMS_NOTIFICATION_EMAIL`
- `CALLBACK_TOPIC`: `DEV_CMS_NOTIFICATION_CALLBACK`

#### Retry Configuration
- `RETRY_SMS_MAX_ATTEMPTS`: `3`
- `RETRY_EMAIL_MAX_ATTEMPTS`: `3`
- `RETRY_CALLBACK_MAX_ATTEMPTS`: `3`
- `RETRY_BASE_DELAY_MS`: `60000`
- `RETRY_MULTIPLIER`: `2.0`

See README.md in dpdp-notification-module for complete configuration reference.

## Kafka Consumers

### 1. SMS Consumer
- **Topic:** DEV_CMS_NOTIFICATION_SMS
- **Retry:** 3 attempts with exponential backoff
- **Timeout:** 60 seconds

### 2. Email Consumer
- **Topic:** DEV_CMS_NOTIFICATION_EMAIL
- **Retry:** 3 attempts with exponential backoff
- **Timeout:** 90 seconds

### 3. Callback Consumer
- **Topic:** DEV_CMS_NOTIFICATION_CALLBACK
- **Security:** JWT-signed payloads
- **Timeout:** 60 seconds

## Development

```bash
mvn clean install
mvn test
mvn jacoco:report
docker build -t notification-consumer:1.0.0 -f deployment/Dockerfile .
```

## Testing

```bash
mvn test
mvn test -Dtest=SmsNotificationConsumerTest
mvn clean test jacoco:report
```

## Deployment

### Docker
```bash
docker run -d -p 9020:9020 \
  -e MONGODB_URI=mongodb://host:27017/ \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  notification-consumer:1.0.0
```

### Kubernetes
```bash
kubectl apply -f deployment/jio-dl-deployment-sit.yaml -n dpdp-cms
```

## Monitoring

```bash
# Health
curl http://localhost:9020/notification-consumer/actuator/health

# Metrics
curl http://localhost:9020/notification-consumer/actuator/prometheus

# Kafka consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group DEV_CMS_NOTIFICATION --describe
```

## Dependencies

- Spring Boot 3.5.7
- Java 21
- MongoDB 5.0+
- Apache Kafka 2.8+
- Lombok 1.18.30
- Jackson (JSON)
- Logstash Logback Encoder 7.4

## Support

- **Documentation:** https://docs.jio.com/dpdp-cms/
- **Email:** Jio.ConsentSupport@ril.com
- **Security:** Jio.ConsentSupport@ril.com
- **Issues:** https://dev.azure.com/JPL-Limited/dpdp-notification-consumer/issues

## License

GNU Lesser General Public License v3.0 (LGPL-3.0) with DPDP addendum.

Copyright (c) 2025 Reliance Jio Infocomm Limited

---

**Version:** 1.0.0
**Last Updated:** 2025-01-21
**Maintained by:** Jio DPDP CMS Team
