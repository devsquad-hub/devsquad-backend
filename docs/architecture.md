# Arquitetura

DevSquad é um monólito modular. Cada contexto mantém domínio, casos de uso, portas e adaptadores próximos,
sem criar serviços distribuídos antes de existir necessidade operacional.

```text
Browser
  -> Next.js App Router
     -> Server Components para leituras iniciais
     -> BFF /api/backend/v1/** para mutações autenticadas
        -> Quarkus REST /api/v1/**
           -> application -> ports -> JDBC, Clerk e S3 adapters
```

## Limites

- `domain` não conhece Quarkus, HTTP, banco, Clerk ou S3.
- `application` não importa adapters, JDBC, AWS SDK ou Jackson. ArchUnit verifica esta regra.
- Controllers traduzem HTTP; casos de uso autorizam e orquestram; adapters implementam persistência e integrações.
- Portas são coesas por responsabilidade. Não existe uma interface artificial para cada método.
- PostgreSQL é a fonte de autorização e estado do produto. Clerk é somente a fonte de identidade.

Os contextos são `identity`, `hub`, `proposal`, `project`, `recruitment`, `work`, `attachment` e
`notification`. Recrutamento e trabalho usam stores JDBC próprios para manter operações concorrentes e
transacionais juntas, sem levar SQL para a camada de aplicação.

## Identidade e autoridade

- `MASTER` administra os papéis do hub e possui as permissões de `ADMIN`.
- `ADMIN` do hub revisa propostas e nomeia administradores de projeto.
- `ADMIN` do projeto opera projeto, recrutamento e workflow.
- `MEMBER` propõe ideias, candidata-se e trabalha nos projetos dos quais participa.

O webhook Clerk registra o `svix-id` antes de processar o evento. Eventos repetidos não produzem efeitos.
Contas criadas ou atualizadas recebem membership `MEMBER` no hub configurado por
`DEFAULT_HUB_SLUG`; o bootstrap promove somente o usuário configurado a `MASTER`.

O frontend recebe `viewerCapabilities` do backend e não reconstrói a matriz de papéis. Toda ação continua
protegida no servidor, mesmo quando um controle não está visível na interface.

## Seed de demonstração

`scripts/seed-demo.sql` é uma massa de dados manual, separada das migrations do Flyway. O arquivo roda
em uma única transação, descobre o hub padrão e o master existentes, usa um namespace determinístico
para as entidades sintéticas e grava o marcador `DEMO_SEED_V1`. Por isso, uma segunda execução é um
no-op e o seed nunca é aplicado automaticamente em produção ou durante o startup do Quarkus.

## Segurança e consistência

- Catálogo, perfis públicos, progresso e posições abertas não exigem autenticação.
- Quadro, tarefas, comentários, arquivos e notificações exigem membership ativa.
- E-mail de membro só é retornado a administradores do hub.
- Arquivos ficam privados no MinIO e usam tickets assinados de curta duração.
- Movimentação de tarefa exige `expectedVersion`; atualizações antigas recebem `stale_task_version`.
- Aprovação de proposta e aceite de candidatura executam suas mudanças relacionadas na mesma transação.
- Constraints parciais impedem master, candidatura pendente e convite pendente duplicados.

Erros HTTP usam `application/problem+json` com `code` estável. A especificação navegável fica em
`/q/swagger-ui` no ambiente local.
