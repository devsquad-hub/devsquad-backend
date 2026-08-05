#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
seed_file="$script_dir/seed-demo.sql"

if [[ -n "${SEED_PSQL_URL:-}" ]]; then
  exec psql "$SEED_PSQL_URL" -v ON_ERROR_STOP=1 -f "$seed_file"
fi

if [[ -n "${PGHOST:-}" || -n "${PGDATABASE:-}" || -n "${PGUSER:-}" ]]; then
  exec psql -v ON_ERROR_STOP=1 -f "$seed_file"
fi

if command -v docker >/dev/null 2>&1 && docker compose ps --services 2>/dev/null | grep -qx postgres; then
  exec docker compose exec -T postgres \
    psql -U "${POSTGRES_USER:-devsquad}" \
         -d "${POSTGRES_DB:-devsquad}" \
         -v ON_ERROR_STOP=1 \
         -f - < "$seed_file"
fi

cat >&2 <<'EOF'
No database connection was configured.

Use one of:
  SEED_PSQL_URL=postgresql://user:password@host:5432/database ./scripts/seed-demo.sh
  PGHOST=localhost PGPORT=5432 PGUSER=devsquad PGDATABASE=devsquad PGPASSWORD=... ./scripts/seed-demo.sh
  docker compose up -d postgres && ./scripts/seed-demo.sh
EOF
exit 2
