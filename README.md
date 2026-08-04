# DevSquad Backend

API do DevSquad para hubs, projetos, propostas, processos seletivos, tarefas, anexos e notificações.

## Stack

- Java 25 e Spring Boot 4.1
- PostgreSQL 18 e Flyway
- Clerk JWT e webhooks Svix
- S3 compatível com MinIO
- Arquitetura hexagonal por domínio
- Imagem nativa GraalVM para produção

## Desenvolvimento

```bash
cp .env.example .env
set -a
source .env
set +a
docker compose up -d postgres minio
./gradlew bootRun
```

O arquivo `.env` é ignorado pelo Git. Ajuste nele as chaves do Clerk e as credenciais locais antes de iniciar a API.
O Compose também aplica `CORS_ALLOWED_ORIGINS` ao CORS global do MinIO; a aplicação fica responsável
apenas por garantir que o bucket exista.

## Verificação

```bash
./gradlew test processAot --no-daemon
```

A documentação arquitetural está em [`docs/architecture.md`](docs/architecture.md).

## Produção

Pushes em `main` executam testes, criam uma imagem nativa, realizam um smoke test com PostgreSQL e
MinIO, publicam `ghcr.io/devsquad-hub/devsquad-api` e implantam no Coolify usando uma tag imutável.
