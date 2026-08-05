# DevSquad Backend

API do DevSquad para hubs, projetos, propostas, processos seletivos, tarefas, anexos e notificações.

## Stack

- Java 25 e Quarkus 3.33 LTS
- PostgreSQL 18 e Flyway
- Clerk JWT e webhooks Svix
- S3 compatível com MinIO
- Arquitetura hexagonal por domínio
- Imagem nativa Mandrel/GraalVM para produção

## Desenvolvimento

```bash
cp .env.example .env
set -a
source .env
set +a
docker compose up -d postgres minio
./gradlew quarkusDev
```

O arquivo `.env` é ignorado pelo Git. Ajuste nele as chaves do Clerk e as credenciais locais antes de iniciar a API.
O Compose também aplica `CORS_ALLOWED_ORIGINS` ao CORS global do MinIO; a aplicação fica responsável
apenas por garantir que o bucket exista.

## Verificação

```bash
./gradlew test quarkusBuild --no-daemon
```

## Dados de demonstração

O seed de demonstração fica fora do Flyway e nunca é executado no startup. Ele cria uma massa
idempotente com hubs, membros, projetos, propostas, recrutamento, tarefas, comentários, notificações
e anexos pendentes. O primeiro usuário master e o hub `devsquad` existentes são preservados.

Com o PostgreSQL do Compose:

```bash
docker compose up -d postgres
./scripts/seed-demo.sh
```

Para apontar explicitamente para outro banco, informe a URL completa:

```bash
SEED_CONFIRM=DEMO_SEED_V1 \
SEED_PSQL_URL=postgresql://devsquad:senha@localhost:5432/devsquad \
./scripts/seed-demo.sh
```

O script exige `SEED_CONFIRM=DEMO_SEED_V1` para qualquer conexão explicitamente configurada, evitando
que variáveis herdadas apontem silenciosamente para um banco compartilhado. Ele roda em uma transação,
usa um lock transacional e aborta se o namespace determinístico já estiver ocupado; a segunda execução
com o marcador presente não insere novamente os dados. Revise a URL antes de usar em qualquer ambiente
compartilhado; o comando é uma ação explícita e não faz parte do deploy normal.

A documentação arquitetural está em [`docs/architecture.md`](docs/architecture.md).

## Produção

Pushes em `main` executam testes, criam uma imagem nativa, realizam um smoke test com PostgreSQL e
MinIO, publicam `ghcr.io/devsquad-hub/devsquad-api` e implantam no Coolify usando uma tag imutável.

O endpoint de readiness usado pela infraestrutura é `/q/health/ready`. Durante a migração, o alias
`/actuator/health/readiness` permanece disponível e delega ao mesmo estado real de saúde.
