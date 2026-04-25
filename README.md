# 🏥 FedMedAI Backend

> Spring Boot REST API for a Privacy-Preserving Federated Learning Medical Diagnosis System.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=flat-square&logo=docker)](https://www.docker.com/)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Docker](#-docker)
- [Environment Variables](#-environment-variables)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Related Repositories](#-related-repositories)

---

## 🔍 Overview

**FedMedAI Backend** is the central REST API server for a Federated Learning Medical Diagnosis System. It coordinates AI model training across multiple hospitals **without sharing patient data**, ensuring privacy while enabling collaborative learning across institutions.

> ⚕️ **FL Privacy Principle**: Patient data never leaves the hospital. Only model weights (mathematical numbers) are shared between hospitals and the central server.

### What This System Does

- Predicts **5 diseases simultaneously**: Heart Disease, Diabetes, Kidney Disease, Liver Disease, Stroke
- Trains AI models using **Federated Learning** (FedAvg + Flower Framework)
- Supports **Differential Privacy** for enhanced model weight protection
- Generates **PDF medical reports** with AI recommendations
- Emails reports **from hospital identity** (not central server)
- Automatically **schedules weekly training** rounds
- Exports patient and prediction data as **CSV**

---

## ✨ Features

### 🔐 Authentication & Security
- JWT authentication (access + refresh tokens)
- Role-based access control (`ADMIN` / `HOSPITAL_ADMIN`)
- BCrypt password hashing
- Stateless session management

### 🏥 Hospital Management
- Full CRUD for hospitals
- Hospital admin assignment
- Patient count tracking
- Email PDF reports from hospital identity (FL-compliant)

### 👥 Patient Management
- Patient registration with 18 medical features (JSONB)
- Hospital-scoped data access
- Patient code system

### 🤖 Multi-Disease AI Prediction
- Heart Disease ❤️
- Diabetes 🩸
- Chronic Kidney Disease 🫘
- Liver Disease 🫁
- Stroke 🧠
- AI-powered recommendations (OpenRouter LLaMA)
- PDF report generation via ML service

### 🔄 Federated Learning
- Custom FedAvg algorithm
- Flower Framework integration
- Differential Privacy (Laplace mechanism)
- Training round management
- Model version history
- Before/After accuracy comparison

### ⏰ Auto Training Scheduler
- Automated weekly FL training (Every Monday 2AM)
- Manual trigger via API
- ML service health check before training
- Run history tracking (last 20 runs)

### 📊 Analytics & Export
- Dashboard analytics (patients, predictions, accuracy)
- Hospital comparison statistics
- Patient risk timeline
- CSV export for patients and predictions

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   React Frontend (:3000)                 │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP REST + JWT
                      ▼
┌─────────────────────────────────────────────────────────┐
│             Spring Boot Backend (:8080)                  │
│                                                         │
│  Controllers → Services → Repositories → Database       │
│                                                         │
│  Auth | Hospital | Patient | Prediction | Model         │
│  Email | Export  | Scheduler | Analytics                │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │ HTTP
               ▼                      ▼
┌──────────────────────┐  ┌──────────────────────────────┐
│  PostgreSQL (:5432)  │  │  FastAPI ML Service (:8000)  │
│                      │  │                              │
│  users               │  │  /ml/predict                 │
│  hospitals           │  │  /ml/train/all               │
│  patients            │  │  /ml/aggregate               │
│  predictions         │  │  /ml/report/generate         │
│  model_versions      │  │  /ml/analytics/full          │
│  training_logs       │  │  /ml/health                  │
└──────────────────────┘  └──────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.x |
| Security | Spring Security + JJWT | Latest |
| Database | PostgreSQL | 15 |
| ORM | Spring Data JPA + Hibernate | Latest |
| JSON Storage | Hypersistence Utils (JSONB) | Latest |
| API Docs | SpringDoc OpenAPI (Swagger) | Latest |
| Email | JavaMailSender (Gmail SMTP) | Latest |
| Build Tool | Maven | 3.9.x |
| Container | Docker | Latest |
| Code Gen | Lombok | Latest |

---

## 📦 Prerequisites

```bash
# Required
Java 17+
Maven 3.9+
PostgreSQL 15+

# For full functionality
Python ML Service running on port 8000
Gmail account with App Password enabled
```

---

## 🚀 Quick Start

### Step 1: Clone Repository

```bash
git clone https://github.com/deepakbhat18/FedMedAI-backend.git
cd FedMedAI-backend
```

### Step 2: Create Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE fedlearn_medical;
\q
```

### Step 3: Configure Local Secrets

```bash
# Copy example template
cp src/main/resources/(example)application.yml \
 src/main/resources/application.yml

# Edit with your actual values
nano src/main/resources/application.yml
```

### Step 4: Run Application

```bash
# With local profile (uses application-local.yml)
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local
```

### Step 5: Verify

```bash
# Check health
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

### Step 6: Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---


### Gmail App Password Setup

```
1. Enable 2-Factor Authentication on Gmail
2. Go to: Google Account → Security → App Passwords
3. Generate password for "Mail"
4. Use generated 16-char password in config
```

---

## 🔑 Default Credentials

> Automatically seeded on first startup via `DataSeeder.java`

| Role | Username | Password | Hospital |
|---|---|---|---|
| System Admin | `admin` | `admin123` | All hospitals |
| Hospital Admin | `hospital1_admin` | `hospital123` | City General Hospital |
| Hospital Admin | `hospital2_admin` | `hospital123` | Metro Heart Center |
| Hospital Admin | `hospital3_admin` | `hospital123` | Valley Medical Institute |

### Seeded Data

```
3 Hospitals
60 Patients (20 per hospital)
Sample medical features (blood pressure, cholesterol, etc.)
```

---



## 🐳 Docker

### Build Image

```bash
docker build -t fedlearn-backend .
```

### Run Container

```bash
docker run -d \
  --name fedlearn-backend \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  -e MAIL_USERNAME=your@gmail.com \
  -e MAIL_PASSWORD=your_app_password \
  -e ML_SERVICE_URL=http://host.docker.internal:8000 \
  fedlearn-backend
```

### Check Logs

```bash
docker logs fedlearn-backend -f
```

### Stop Container

```bash
docker stop fedlearn-backend
docker rm fedlearn-backend
```

---

## 🔧 Environment Variables

| Variable | Default | Required | Description |
|---|---|---|---|
| `DB_HOST` | `localhost` | No | PostgreSQL host |
| `DB_PORT` | `5432` | No | PostgreSQL port |
| `DB_NAME` | `fedlearn_medical` | No | Database name |
| `DB_USERNAME` | `postgres` | No | Database user |
| `DB_PASSWORD` | - | ✅ | Database password |
| `JWT_SECRET` | - | ✅ | JWT signing key (64+ chars) |
| `JWT_EXPIRATION_MS` | `3600000` | No | Token expiry (1 hour) |
| `MAIL_HOST` | `smtp.gmail.com` | No | SMTP host |
| `MAIL_PORT` | `587` | No | SMTP port |
| `MAIL_USERNAME` | - | ✅ | Gmail address |
| `MAIL_PASSWORD` | - | ✅ | Gmail app password |
| `ML_SERVICE_URL` | `http://localhost:8000` | No | ML service URL |
| `SCHEDULER_ENABLED` | `true` | No | Enable auto training |
| `SCHEDULER_CRON` | `0 0 2 * * MON` | No | Training schedule |
| `SCHEDULER_ENGINE` | `custom` | No | FL engine (custom/flower) |

---

## 🗃️ Database Schema

```
users
├── id, username, email, password_hash
├── full_name, role (ADMIN/HOSPITAL_ADMIN)
├── is_active
└── created_at, updated_at

hospitals
├── id, name, code (unique)
├── address, contact_email, contact_phone
├── admin_user_id → users.id
├── status (ACTIVE/INACTIVE/SUSPENDED)
└── created_at, updated_at

patients
├── id, hospital_id → hospitals.id
├── patient_code (unique per hospital)
├── age, gender (MALE/FEMALE/OTHER)
├── features (JSONB) ← 18 medical values
├── diagnosis, actual_label (0/1)
└── created_at, updated_at

predictions
├── id, patient_id, hospital_id, model_version_id
├── prediction (0/1), prediction_label
├── confidence (0.0-1.0)
├── features_used (JSONB)
└── created_at

model_versions
├── id, version_name (e.g. v1.0)
├── model_type, accuracy, loss, f1_score
├── is_active, training_round
├── participating_hospitals
├── aggregation_method (fedavg)
└── created_at

training_logs
├── id, training_round
├── hospital_id, model_version_id
├── local_accuracy, local_loss, local_f1_score
├── epochs, learning_rate, batch_size
├── status (COMPLETED/FAILED/IN_PROGRESS)
├── error_message
└── created_at
```

---

## 🔒 Security

### Role Permissions

| Endpoint | ADMIN | HOSPITAL_ADMIN |
|---|---|---|
| All hospitals | ✅ | ❌ (own only) |
| All patients | ✅ | ❌ (own only) |
| Trigger training | ✅ | ❌ |
| Scheduler control | ✅ | ❌ |
| Create hospital | ✅ | ❌ |
| View predictions | ✅ | ✅ |
| Make predictions | ✅ | ✅ |
| Export CSV | ✅ | ✅ |

### FL Privacy Design

```
Patient email → NOT stored in database
              → Doctor enters at send time
              → Email sent FROM hospital identity
              → Central server not mentioned to patient
              → Consistent with FL privacy principles
```

---

## 📁 Project Structure

```
src/main/java/com/fedlearn/medical/
├── config/
│   ├── CorsConfig.java
│   ├── DataSeeder.java
│   ├── RestTemplateConfig.java
│   ├── SchedulerConfig.java
│   ├── SecurityConfig.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AnalyticsController.java
│   ├── AuthController.java
│   ├── ExportController.java
│   ├── HospitalController.java
│   ├── ModelController.java
│   ├── PatientController.java
│   ├── PredictionController.java
│   ├── SchedulerController.java
│   └── TrainingLogController.java
├── dto/
│   ├── request/
│   │   ├── HospitalEmailRequest.java
│   │   ├── HospitalRequest.java
│   │   ├── LoginRequest.java
│   │   ├── PatientRequest.java
│   │   ├── PredictionRequest.java
│   │   ├── RegisterRequest.java
│   │   └── TrainingTriggerRequest.java
│   └── response/
│       ├── AnalyticsResponse.java
│       ├── ApiResponse.java
│       ├── AuthResponse.java
│       ├── HospitalResponse.java
│       ├── ModelVersionResponse.java
│       ├── PatientResponse.java
│       ├── PredictionResponse.java
│       ├── RiskTimelineResponse.java
│       ├── SchedulerStatusResponse.java
│       └── TrainingLogResponse.java
├── entity/
│   ├── Hospital.java
│   ├── ModelVersion.java
│   ├── Patient.java
│   ├── Prediction.java
│   ├── TrainingLog.java
│   └── User.java
├── enums/
│   ├── Gender.java
│   ├── HospitalStatus.java
│   ├── Role.java
│   └── TrainingStatus.java
├── exception/
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   ├── MLServiceException.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── repository/
│   ├── HospitalRepository.java
│   ├── ModelVersionRepository.java
│   ├── PatientRepository.java
│   ├── PredictionRepository.java
│   ├── TrainingLogRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
└── service/
    ├── AnalyticsService.java
    ├── AuthService.java
    ├── EmailService.java
    ├── ExportService.java
    ├── HospitalService.java
    ├── MLServiceClient.java
    ├── ModelService.java
    ├── PatientService.java
    ├── PredictionService.java
    ├── TrainingLogService.java
    └── TrainingSchedulerService.java
```

---

## 🔗 Related Repositories

| Repository | Tech | Description |
|---|---|---|
| **FedMedAI-backend** | Spring Boot | This repo - REST API |
| **FedMedAI-ml-service** | FastAPI + Python | ML models + FL training |
| **FedMedAI-frontend** | React + Vite | Web dashboard |

---

## 📜 License
This project is licensed under the MIT License.

---

<div align="center">

**Built Privacy-Preserving Medical AI**

⭐ Star this repo if you found it helpful!

</div>
