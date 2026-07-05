# Architecture Overview

OdinSync will start as a modular monolith and evolve into microservices.

## Initial Architecture

Client
↓
Spring Boot Modular Monolith
↓
MySQL

## Why Modular Monolith First?

At the beginning, the project needs speed, simplicity, and clear domain modeling.

Microservices will be introduced later when independent scaling, deployment, and ownership become necessary.

## Future Architecture

Client
↓
API Gateway
↓
Microservices
↓
Kafka
↓
Databases
↓
Kubernetes

## Core Bounded Contexts

- Identity & Access
- Organization
- CRM
- Catalog
- Inventory
- Sales
- Finance
- Notifications
- Analytics
- AI Copilot