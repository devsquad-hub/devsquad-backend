-- DevSquad demo dataset.
-- This file is intentionally not a Flyway migration. Run it explicitly with psql.
-- It is safe to run more than once: DEMO_SEED_V1 is the idempotency marker.

BEGIN;

SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

DO $seed$
DECLARE
  v_marker constant uuid := '90000000-0000-4000-8000-000000000001';
  v_default_hub_fallback constant uuid := '20000000-0000-4000-8000-000000000001';
  v_lab_hub_fallback constant uuid := '20000000-0000-4000-8000-000000000002';
  v_seed_master_fallback constant uuid := '10000000-0000-4000-8000-000000000001';
  v_default_hub_id uuid;
  v_lab_hub_id uuid;
  v_master_id uuid;
  v_lab_master_id uuid;
  v_account_id uuid;
  v_project_id uuid;
  v_round_id uuid;
  v_position_id uuid;
  v_form_id uuid;
  v_column_id uuid;
  v_milestone_id uuid;
  v_label_id uuid;
  v_task_id uuid;
  v_parent_id uuid;
  v_assignee_id uuid;
  v_group text;
  v_role text;
  v_priority text;
  v_skills text[];
  v_status text;
  v_task_base bigint;
  v_i integer;
  v_j integer;
  v_member_count integer;
  v_project record;
  v_task record;
  v_name text;
  v_names text[] := ARRAY[
    'Ana Costa', 'Bruno Lima', 'Carla Mendes', 'Diego Rocha', 'Elisa Martins',
    'Felipe Nunes', 'Gabriela Alves', 'Hugo Barros', 'Iris Carvalho', 'Joao Ribeiro',
    'Kamila Freitas', 'Lucas Vieira', 'Marina Dias', 'Nicolas Duarte', 'Olivia Ramos',
    'Pedro Tavares', 'Rafaela Gomes', 'Sergio Moraes', 'Tatiana Araujo', 'Uriel Santos',
    'Valentina Cruz', 'William Teixeira', 'Yasmin Torres'
  ];
  v_project_ids uuid[] := ARRAY[
    '40000000-0000-4000-8000-000000000001'::uuid,
    '40000000-0000-4000-8000-000000000002'::uuid,
    '40000000-0000-4000-8000-000000000003'::uuid,
    '40000000-0000-4000-8000-000000000004'::uuid,
    '40000000-0000-4000-8000-000000000005'::uuid,
    '40000000-0000-4000-8000-000000000006'::uuid
  ];
  v_default_project_ids uuid[] := ARRAY[
    '40000000-0000-4000-8000-000000000001'::uuid,
    '40000000-0000-4000-8000-000000000002'::uuid,
    '40000000-0000-4000-8000-000000000003'::uuid,
    '40000000-0000-4000-8000-000000000004'::uuid
  ];
  v_lab_project_ids uuid[] := ARRAY[
    '40000000-0000-4000-8000-000000000005'::uuid,
    '40000000-0000-4000-8000-000000000006'::uuid
  ];
  v_task_ids uuid[];
BEGIN
  -- Serialize every invocation before checking the marker. Without this lock two
  -- concurrent sessions could both pass the marker check and append children.
  PERFORM pg_advisory_xact_lock(hashtextextended('devsquad.demo.seed.v1', 0));

  IF EXISTS (
    SELECT 1
    FROM activity_events
    WHERE event_type = 'DEMO_SEED_V1' AND entity_id = v_marker
  ) THEN
    RAISE NOTICE 'demo_seed_v1: already_present';
    RETURN;
  END IF;

  -- Deterministic IDs make the dataset easy to inspect, but they must never be
  -- used as an upsert key for an unrelated row. Abort before the first write if
  -- this namespace, the lab slug, or the marker ID is already occupied.
  IF EXISTS (
    SELECT 1 FROM hubs
    WHERE id IN (v_default_hub_fallback, v_lab_hub_fallback)
  )
  OR EXISTS (
    SELECT 1 FROM hubs WHERE slug = 'opensource-lab'
  )
  OR EXISTS (
    SELECT 1 FROM accounts
    WHERE id::text LIKE '10000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM project_proposals
    WHERE id::text LIKE '30000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM projects
    WHERE id::text LIKE '40000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM recruitment_rounds
    WHERE id::text LIKE '50000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM recruitment_positions
    WHERE id::text LIKE '51000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM recruitment_form_versions
    WHERE id::text LIKE '52000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM project_applications
    WHERE id::text LIKE '53000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM project_invitations
    WHERE id::text LIKE '54000000-0000-4000-8000-%'
  )
  OR EXISTS (
    SELECT 1 FROM activity_events WHERE entity_id = v_marker
  ) THEN
    RAISE EXCEPTION USING
      ERRCODE = 'check_violation',
      MESSAGE = 'demo_seed_namespace_collision',
      DETAIL = 'Reserved demo IDs or the opensource-lab slug already exist';
  END IF;

  SELECT id INTO v_default_hub_id FROM hubs WHERE slug = 'devsquad' LIMIT 1;
  IF v_default_hub_id IS NULL THEN
    INSERT INTO hubs (id, name, slug, description)
    VALUES (
      v_default_hub_fallback,
      'DevSquad Community',
      'devsquad',
      'Comunidade de pessoas construindo projetos colaborativos.'
    )
    ON CONFLICT (slug) DO NOTHING;
    SELECT id INTO v_default_hub_id FROM hubs WHERE slug = 'devsquad' LIMIT 1;
  END IF;

  SELECT hm.account_id
  INTO v_master_id
  FROM hub_memberships hm
  WHERE hm.hub_id = v_default_hub_id
    AND hm.role = 'MASTER'
    AND hm.status = 'ACTIVE'
  LIMIT 1;

  IF v_master_id IS NULL THEN
    SELECT id INTO v_master_id FROM accounts WHERE clerk_user_id = 'seed-master' LIMIT 1;
    IF v_master_id IS NULL THEN
      INSERT INTO accounts (id, clerk_user_id, email, display_name, bio, skills, availability_hours)
      VALUES (
        v_seed_master_fallback,
        'seed-master',
        'master@demo.devsquad.local',
        'DevSquad Master',
        'Conta local de demonstração para administrar a comunidade.',
        ARRAY['Community', 'Product', 'Leadership'],
        10
      )
      ON CONFLICT (id) DO NOTHING;
      v_master_id := v_seed_master_fallback;
    END IF;
    INSERT INTO hub_memberships (hub_id, account_id, role, status)
    VALUES (v_default_hub_id, v_master_id, 'MASTER', 'ACTIVE')
    ON CONFLICT (hub_id, account_id) DO UPDATE
      SET role = 'MASTER', status = 'ACTIVE', updated_at = now();
  END IF;

  FOR v_i IN 2..24 LOOP
    v_account_id := ('10000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
    v_name := v_names[v_i - 1];
    v_skills := CASE v_i % 6
      WHEN 0 THEN ARRAY['Java', 'Quarkus', 'PostgreSQL']
      WHEN 1 THEN ARRAY['TypeScript', 'Next.js', 'Accessibility']
      WHEN 2 THEN ARRAY['Product', 'Research', 'Discovery']
      WHEN 3 THEN ARRAY['UX', 'Figma', 'Design systems']
      WHEN 4 THEN ARRAY['DevOps', 'Docker', 'Observability']
      ELSE ARRAY['Python', 'Data', 'Analytics']
    END;
    INSERT INTO accounts (
      id, clerk_user_id, email, display_name, avatar_url, bio, skills,
      github_url, linkedin_url, portfolio_url, availability_hours
    )
    VALUES (
      v_account_id,
      'seed-user-' || lpad(v_i::text, 2, '0'),
      'seed-' || lpad(v_i::text, 2, '0') || '@demo.devsquad.local',
      v_name,
      'https://api.dicebear.com/9.x/initials/svg?seed=devsquad-' || v_i,
      'Pessoa voluntaria da comunidade DevSquad, participando de projetos colaborativos.',
      v_skills,
      'https://github.com/devsquad-seed-' || lpad(v_i::text, 2, '0'),
      'https://www.linkedin.com/in/devsquad-seed-' || lpad(v_i::text, 2, '0'),
      'https://portfolio.demo.devsquad.local/' || lpad(v_i::text, 2, '0'),
      4 + (v_i % 13)
    )
    ON CONFLICT (id) DO NOTHING;
  END LOOP;

  INSERT INTO hubs (id, name, slug, description)
  VALUES (
    v_lab_hub_fallback,
    'Open Source Lab',
    'opensource-lab',
    'Laboratorio de projetos open source e experimentos tecnicos.'
  )
  ON CONFLICT (slug) DO NOTHING;
  SELECT id INTO v_lab_hub_id FROM hubs WHERE slug = 'opensource-lab' LIMIT 1;

  SELECT hm.account_id
  INTO v_lab_master_id
  FROM hub_memberships hm
  WHERE hm.hub_id = v_lab_hub_id
    AND hm.role = 'MASTER'
    AND hm.status = 'ACTIVE'
  LIMIT 1;
  IF v_lab_master_id IS NULL THEN
    v_lab_master_id := '10000000-0000-4000-8000-000000000002'::uuid;
    INSERT INTO hub_memberships (hub_id, account_id, role, status)
    VALUES (v_lab_hub_id, v_lab_master_id, 'MASTER', 'ACTIVE')
    ON CONFLICT (hub_id, account_id) DO UPDATE
      SET role = 'MASTER', status = 'ACTIVE', updated_at = now();
  END IF;

  FOR v_i IN 2..24 LOOP
    v_account_id := ('10000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
    v_role := CASE WHEN v_i IN (2, 3, 4) THEN 'ADMIN' ELSE 'MEMBER' END;
    IF v_account_id = v_master_id THEN
      v_role := 'MASTER';
    END IF;
    INSERT INTO hub_memberships (hub_id, account_id, role, status)
    VALUES (v_default_hub_id, v_account_id, v_role, 'ACTIVE')
    ON CONFLICT (hub_id, account_id) DO UPDATE SET
      role = excluded.role,
      status = 'ACTIVE',
      updated_at = now();
  END LOOP;

  FOR v_i IN 5..14 LOOP
    v_account_id := ('10000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
    v_role := CASE WHEN v_i IN (5, 6) THEN 'ADMIN' ELSE 'MEMBER' END;
    IF v_account_id = v_lab_master_id THEN
      v_role := 'MASTER';
    END IF;
    INSERT INTO hub_memberships (hub_id, account_id, role, status)
    VALUES (v_lab_hub_id, v_account_id, v_role, 'ACTIVE')
    ON CONFLICT (hub_id, account_id) DO UPDATE SET
      role = excluded.role,
      status = 'ACTIVE',
      updated_at = now();
  END LOOP;

  INSERT INTO project_proposals (
    id, hub_id, author_id, title, summary, problem, proposed_solution, goals,
    desired_skills, status, reviewer_id, decision_reason, created_at, decided_at
  )
  VALUES
    (
      '30000000-0000-4000-8000-000000000001', v_default_hub_id,
      '10000000-0000-4000-8000-000000000003'::uuid,
      'Painel de projetos da comunidade',
      'Um lugar unico para acompanhar ideias, tarefas e pessoas.',
      'As informacoes ficam espalhadas em chats e planilhas.',
      'Criar um painel colaborativo com projetos, processos seletivos e tarefas.',
      'Aumentar a transparencia e reduzir o tempo para entrar em um projeto.',
      ARRAY['Product', 'UX', 'Java', 'Next.js'], 'APPROVED', v_master_id,
      'Aprovado para o primeiro ciclo da comunidade.', now() - interval '80 days', now() - interval '72 days'
    ),
    (
      '30000000-0000-4000-8000-000000000002', v_default_hub_id,
      '10000000-0000-4000-8000-000000000004'::uuid,
      'Trilha de onboarding para novos membros',
      'Uma experiencia guiada para quem chega na comunidade.',
      'Novas pessoas nao sabem por onde comecar.',
      'Disponibilizar trilhas, projetos indicados e checklist de primeiros passos.',
      'Ajudar cada membro a encontrar uma contribuicao em ate uma semana.',
      ARRAY['Content', 'Community', 'Research'], 'APPROVED', v_master_id,
      'Aprovado junto com o calendario de recrutamento.', now() - interval '55 days', now() - interval '48 days'
    ),
    (
      '30000000-0000-4000-8000-000000000003', v_default_hub_id,
      '10000000-0000-4000-8000-000000000011'::uuid,
      'Kit de componentes acessiveis',
      'Componentes reutilizaveis para produtos da comunidade.',
      'Cada projeto resolve acessibilidade de um jeito diferente.',
      'Criar tokens, componentes e exemplos documentados.',
      'Cobrir os fluxos de formulario, navegacao e feedback.',
      ARRAY['Accessibility', 'React', 'Design systems'], 'PENDING', null,
      null, now() - interval '4 days', null
    ),
    (
      '30000000-0000-4000-8000-000000000004', v_default_hub_id,
      '10000000-0000-4000-8000-000000000006'::uuid,
      'Observabilidade para ambientes pequenos',
      'Um pacote simples de logs, metricas e alertas.',
      'Falhas sao descobertas apenas depois de uma reclamacao.',
      'Padronizar healthchecks, logs estruturados e dashboards leves.',
      'Diminuir o tempo de diagnostico sem aumentar muito os custos.',
      ARRAY['Docker', 'Observability', 'PostgreSQL'], 'REJECTED', v_master_id,
      'Retomar depois da primeira versao da plataforma.', now() - interval '35 days', now() - interval '28 days'
    ),
    (
      '30000000-0000-4000-8000-000000000005', v_default_hub_id,
      '10000000-0000-4000-8000-000000000007'::uuid,
      'Mapa de oportunidades da comunidade',
      'Catalogar temas e projetos onde pessoas podem contribuir.',
      'Membros descobrem oportunidades por acaso.',
      'Exibir um catalogo filtravel por skill, tempo e fase do projeto.',
      'Tornar as oportunidades publicas e faceis de encontrar.',
      ARRAY['Research', 'Product', 'Data'], 'DRAFT', null,
      null, now() - interval '2 days', null
    ),
    (
      '30000000-0000-4000-8000-000000000006', v_default_hub_id,
      '10000000-0000-4000-8000-000000000008'::uuid,
      'Programa de mentoria entre pares',
      'Conectar pessoas experientes e quem esta iniciando.',
      'O conhecimento fica concentrado em poucas pessoas.',
      'Organizar encontros curtos com metas e acompanhamento.',
      'Criar uma rede sustentavel de troca de conhecimento.',
      ARRAY['Community', 'Education', 'Leadership'], 'WITHDRAWN', v_master_id,
      'A autora decidiu reformular a proposta.', now() - interval '20 days', now() - interval '15 days'
    ),
    (
      '30000000-0000-4000-8000-000000000007', v_default_hub_id,
      '10000000-0000-4000-8000-000000000009'::uuid,
      'Laboratorio de dados abertos',
      'Experimentos usando dados publicos para responder perguntas locais.',
      'Dados publicos existem, mas sao dificeis de explorar.',
      'Construir notebooks e pequenas visualizacoes explicativas.',
      'Publicar tres estudos reproduziveis no primeiro trimestre.',
      ARRAY['Python', 'Data', 'Research'], 'APPROVED', v_master_id,
      'Aprovado como projeto de pesquisa aplicada.', now() - interval '70 days', now() - interval '62 days'
    ),
    (
      '30000000-0000-4000-8000-000000000008', v_lab_hub_id,
      '10000000-0000-4000-8000-000000000012'::uuid,
      'CLI para catalogos open source',
      'Uma ferramenta de terminal para descobrir e iniciar projetos.',
      'Catalogos web nao atendem quem trabalha no terminal.',
      'Criar comandos para buscar, clonar e configurar repositorios.',
      'Publicar uma versao inicial com documentacao e exemplos.',
      ARRAY['Go', 'CLI', 'Open source'], 'APPROVED', v_lab_master_id,
      'Aprovado para o laboratorio open source.', now() - interval '50 days', now() - interval '43 days'
    );

  INSERT INTO projects (
    id, hub_id, source_proposal_id, name, slug, project_key, summary, description,
    status, repository_url, communication_url, tags, start_date, target_date
  )
  VALUES
    (
      v_project_ids[1], v_default_hub_id, '30000000-0000-4000-8000-000000000001'::uuid,
      'DevSquad Hub', 'devsquad-hub', 'DS-101',
      'A plataforma que organiza projetos e oportunidades da comunidade.',
      'Produto principal para transformar ideias em projetos colaborativos com selecao transparente.',
      'ACTIVE', 'https://github.com/devsquad-hub/devsquad-frontend',
      'https://github.com/orgs/devsquad-hub/projects/1', ARRAY['platform', 'community', 'product'],
      current_date - 75, current_date + 90
    ),
    (
      v_project_ids[2], v_default_hub_id, '30000000-0000-4000-8000-000000000002'::uuid,
      'Onboarding aberto', 'onboarding-aberto', 'DS-102',
      'Trilha para novos membros encontrarem seu primeiro projeto.',
      'Conteudo, checklists e pequenos desafios conectam novos membros a pessoas e oportunidades.',
      'RECRUITING', 'https://github.com/devsquad-hub/onboarding-aberto',
      'https://chat.devsquad.local/onboarding', ARRAY['community', 'education', 'content'],
      current_date - 40, current_date + 45
    ),
    (
      v_project_ids[3], v_default_hub_id, null,
      'Kit de acessibilidade', 'kit-acessibilidade', 'DS-103',
      'Componentes e exemplos para produtos mais inclusivos.',
      'Projeto em planejamento, aguardando a aprovacao da proposta de componentes acessiveis.',
      'PLANNING', 'https://github.com/devsquad-hub/kit-acessibilidade',
      'https://github.com/devsquad-hub/kit-acessibilidade/discussions', ARRAY['accessibility', 'design-system'],
      current_date + 7, current_date + 150
    ),
    (
      v_project_ids[4], v_default_hub_id, '30000000-0000-4000-8000-000000000007'::uuid,
      'Dados abertos locais', 'dados-abertos-locais', 'DS-104',
      'Estudos reproduziveis com dados publicos.',
      'Ciclo concluido de estudos e visualizacoes para a comunidade.',
      'COMPLETED', 'https://github.com/devsquad-hub/dados-abertos-locais',
      'https://github.com/devsquad-hub/dados-abertos-locais/discussions', ARRAY['data', 'research', 'open-data'],
      current_date - 180, current_date - 15
    ),
    (
      v_project_ids[5], v_lab_hub_id, '30000000-0000-4000-8000-000000000008'::uuid,
      'Open Source CLI', 'open-source-cli', 'OS-201',
      'CLI para descobrir e configurar projetos open source.',
      'Uma ferramenta de terminal mantida pelo laboratorio para reduzir o atrito de contribuicao.',
      'ACTIVE', 'https://github.com/devsquad-hub/open-source-cli',
      'https://github.com/devsquad-hub/open-source-cli/discussions', ARRAY['open-source', 'cli', 'developer-tools'],
      current_date - 60, current_date + 120
    ),
    (
      v_project_ids[6], v_lab_hub_id, null,
      'Arquivo de experimentos', 'arquivo-experimentos', 'OS-202',
      'Espaco para prototipos e experimentos de curta duracao.',
      'Projetos arquivados e prototipos que servem como referencia para novas ideias.',
      'ARCHIVED', null, null, ARRAY['experiments', 'archive'],
      current_date - 300, current_date - 120
    )
  ON CONFLICT (id) DO NOTHING;

  UPDATE project_proposals
  SET project_id = '40000000-0000-4000-8000-000000000001'::uuid
  WHERE id = '30000000-0000-4000-8000-000000000001'::uuid;
  UPDATE project_proposals
  SET project_id = '40000000-0000-4000-8000-000000000002'::uuid
  WHERE id = '30000000-0000-4000-8000-000000000002'::uuid;
  UPDATE project_proposals
  SET project_id = '40000000-0000-4000-8000-000000000004'::uuid
  WHERE id = '30000000-0000-4000-8000-000000000007'::uuid;
  UPDATE project_proposals
  SET project_id = '40000000-0000-4000-8000-000000000005'::uuid
  WHERE id = '30000000-0000-4000-8000-000000000008'::uuid;

  FOREACH v_project_id IN ARRAY v_default_project_ids LOOP
    INSERT INTO project_memberships (project_id, account_id, role, functional_role)
    VALUES (v_project_id, v_master_id, 'ADMIN', 'Hub master and project lead')
    ON CONFLICT (project_id, account_id) DO UPDATE SET
      role = 'ADMIN', status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    IF v_master_id <> '10000000-0000-4000-8000-000000000002'::uuid THEN
      INSERT INTO project_memberships (project_id, account_id, role, functional_role)
      VALUES (v_project_id, '10000000-0000-4000-8000-000000000002'::uuid, 'ADMIN', 'Project administrator')
      ON CONFLICT (project_id, account_id) DO UPDATE SET
        role = 'ADMIN', status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    END IF;
    FOR v_i IN 3..10 LOOP
      v_account_id := ('10000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
      INSERT INTO project_memberships (project_id, account_id, role, functional_role)
      VALUES (v_project_id, v_account_id, 'MEMBER', CASE WHEN v_i % 2 = 0 THEN 'Contributor' ELSE 'Reviewer' END)
      ON CONFLICT (project_id, account_id) DO UPDATE SET
        status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    END LOOP;
  END LOOP;

  FOREACH v_project_id IN ARRAY v_lab_project_ids LOOP
    INSERT INTO project_memberships (project_id, account_id, role, functional_role)
    VALUES (v_project_id, v_lab_master_id, 'ADMIN', 'Laboratory lead')
    ON CONFLICT (project_id, account_id) DO UPDATE SET
      role = 'ADMIN', status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    IF v_lab_master_id <> '10000000-0000-4000-8000-000000000005'::uuid THEN
      INSERT INTO project_memberships (project_id, account_id, role, functional_role)
      VALUES (v_project_id, '10000000-0000-4000-8000-000000000005'::uuid, 'ADMIN', 'Maintainer')
      ON CONFLICT (project_id, account_id) DO UPDATE SET
        role = 'ADMIN', status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    END IF;
    FOR v_i IN 6..14 LOOP
      v_account_id := ('10000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
      INSERT INTO project_memberships (project_id, account_id, role, functional_role)
      VALUES (v_project_id, v_account_id, 'MEMBER', 'Open source contributor')
      ON CONFLICT (project_id, account_id) DO UPDATE SET
        status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();
    END LOOP;
  END LOOP;

  INSERT INTO project_memberships (project_id, account_id, role, functional_role)
  VALUES
    (v_project_ids[2], '10000000-0000-4000-8000-000000000012'::uuid, 'MEMBER', 'Content contributor'),
    (v_project_ids[5], '10000000-0000-4000-8000-000000000016'::uuid, 'MEMBER', 'CLI contributor')
  ON CONFLICT (project_id, account_id) DO UPDATE SET
    status = 'ACTIVE', functional_role = excluded.functional_role, updated_at = now();

  INSERT INTO recruitment_rounds (id, project_id, name, description, status, opens_at, closes_at)
  VALUES
    (
      '50000000-0000-4000-8000-000000000001'::uuid, v_project_ids[2], 'Ciclo 1 - Produto e conteudo',
      'Primeiro ciclo de selecao para o onboarding.', 'OPEN', now() - interval '15 days', now() + interval '20 days'
    ),
    (
      '50000000-0000-4000-8000-000000000002'::uuid, v_project_ids[1], 'Ciclo encerrado - MVP',
      'Selecao concluida para o primeiro MVP.', 'CLOSED', now() - interval '100 days', now() - interval '65 days'
    ),
    (
      '50000000-0000-4000-8000-000000000003'::uuid, v_project_ids[3], 'Pre-inscricoes do kit',
      'Rascunho da futura selecao de acessibilidade.', 'DRAFT', now() + interval '10 days', now() + interval '45 days'
    ),
    (
      '50000000-0000-4000-8000-000000000004'::uuid, v_project_ids[5], 'Contributors do laboratorio',
      'Vagas abertas para quem quer contribuir com a CLI.', 'OPEN', now() - interval '25 days', now() + interval '35 days'
    ),
    (
      '50000000-0000-4000-8000-000000000005'::uuid, v_project_ids[4], 'Pesquisa encerrada',
      'Ciclo historico de pesquisa de dados.', 'CLOSED', now() - interval '220 days', now() - interval '160 days'
    )
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO recruitment_positions (id, round_id, title, description, skills, capacity, filled, status)
  VALUES
    ('51000000-0000-4000-8000-000000000001'::uuid, '50000000-0000-4000-8000-000000000001'::uuid,
     'Frontend engineer', 'Construa telas acessiveis e performaticas.', ARRAY['TypeScript', 'Next.js', 'Accessibility'], 4, 1, 'OPEN'),
    ('51000000-0000-4000-8000-000000000002'::uuid, '50000000-0000-4000-8000-000000000001'::uuid,
     'Community writer', 'Transforme aprendizados em guias praticos.', ARRAY['Writing', 'Community', 'Research'], 3, 0, 'OPEN'),
    ('51000000-0000-4000-8000-000000000003'::uuid, '50000000-0000-4000-8000-000000000001'::uuid,
     'Product designer', 'Modele fluxos claros para novos membros.', ARRAY['UX', 'Figma', 'Research'], 2, 2, 'FILLED'),
    ('51000000-0000-4000-8000-000000000004'::uuid, '50000000-0000-4000-8000-000000000002'::uuid,
     'Full-stack contributor', 'Ajude a fechar o primeiro ciclo do hub.', ARRAY['Java', 'PostgreSQL', 'React'], 2, 2, 'CLOSED'),
    ('51000000-0000-4000-8000-000000000005'::uuid, '50000000-0000-4000-8000-000000000003'::uuid,
     'Accessibility specialist', 'Revise componentes e criterios de aceite.', ARRAY['Accessibility', 'WCAG', 'UX'], 3, 0, 'OPEN'),
    ('51000000-0000-4000-8000-000000000006'::uuid, '50000000-0000-4000-8000-000000000004'::uuid,
     'Go developer', 'Implemente comandos e integracoes da CLI.', ARRAY['Go', 'CLI', 'Git'], 3, 2, 'OPEN'),
    ('51000000-0000-4000-8000-000000000007'::uuid, '50000000-0000-4000-8000-000000000004'::uuid,
     'QA and docs', 'Garanta uma experiencia simples para contributors.', ARRAY['Testing', 'Documentation', 'Open source'], 2, 1, 'OPEN'),
    ('51000000-0000-4000-8000-000000000008'::uuid, '50000000-0000-4000-8000-000000000005'::uuid,
     'Data researcher', 'Explore fontes publicas e documente metodos.', ARRAY['Python', 'Data', 'Research'], 2, 2, 'CLOSED')
  ON CONFLICT (id) DO NOTHING;

  FOR v_i IN 1..8 LOOP
    v_position_id := ('51000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
    v_form_id := ('52000000-0000-4000-8000-' || lpad(v_i::text, 12, '0'))::uuid;
    INSERT INTO recruitment_form_versions (id, position_id, version, published_at)
    VALUES (v_form_id, v_position_id, 1, now() - (v_i || ' days')::interval)
    ON CONFLICT (id) DO NOTHING;
    FOR v_j IN 1..4 LOOP
      INSERT INTO recruitment_questions (
        form_version_id, question_key, label, type, required, position, options
      )
      VALUES (
        v_form_id,
        CASE v_j WHEN 1 THEN 'motivation' WHEN 2 THEN 'portfolio' WHEN 3 THEN 'experience' ELSE 'availability' END,
        CASE v_j
          WHEN 1 THEN 'Por que voce quer participar deste projeto?'
          WHEN 2 THEN 'Compartilhe um link de trabalho ou projeto anterior.'
          WHEN 3 THEN 'Qual e o seu nivel de experiencia na area?'
          ELSE 'Voce consegue reservar pelo menos quatro horas por semana?'
        END,
        CASE v_j WHEN 1 THEN 'LONG_TEXT' WHEN 2 THEN 'URL' WHEN 3 THEN 'SINGLE_CHOICE' ELSE 'BOOLEAN' END,
        v_j <> 2,
        v_j - 1,
        CASE v_j WHEN 3 THEN '["Iniciante", "Intermediario", "Avancado"]'::jsonb
                 WHEN 4 THEN '["Sim", "Nao"]'::jsonb ELSE '[]'::jsonb END
      )
      ON CONFLICT (form_version_id, question_key) DO NOTHING;
    END LOOP;
  END LOOP;

  INSERT INTO project_applications (
    id, position_id, form_version_id, applicant_id, answers, status, reviewer_id,
    decision_note, submitted_at, decided_at
  )
  VALUES
    (
      '53000000-0000-4000-8000-000000000001'::uuid,
      '51000000-0000-4000-8000-000000000001'::uuid,
      '52000000-0000-4000-8000-000000000001'::uuid,
      '10000000-0000-4000-8000-000000000011'::uuid,
      '{"motivation":"Quero ajudar a transformar os fluxos em uma experiencia clara.","portfolio":"https://portfolio.demo.devsquad.local/11","experience":"Intermediario","availability":true}'::jsonb,
      'SUBMITTED', null, null, now() - interval '3 days', null
    ),
    (
      '53000000-0000-4000-8000-000000000002'::uuid,
      '51000000-0000-4000-8000-000000000001'::uuid,
      '52000000-0000-4000-8000-000000000001'::uuid,
      '10000000-0000-4000-8000-000000000012'::uuid,
      '{"motivation":"Tenho experiencia criando componentes reutilizaveis.","portfolio":"https://portfolio.demo.devsquad.local/12","experience":"Avancado","availability":true}'::jsonb,
      'ACCEPTED', v_master_id, 'Boa experiencia e disponibilidade alinhada ao ciclo.', now() - interval '10 days', now() - interval '7 days'
    ),
    (
      '53000000-0000-4000-8000-000000000003'::uuid,
      '51000000-0000-4000-8000-000000000001'::uuid,
      '52000000-0000-4000-8000-000000000001'::uuid,
      '10000000-0000-4000-8000-000000000013'::uuid,
      '{"motivation":"Quero aprender contribuindo com o produto.","portfolio":"https://portfolio.demo.devsquad.local/13","experience":"Iniciante","availability":true}'::jsonb,
      'REJECTED', v_master_id, 'Escolhemos perfis com disponibilidade maior neste ciclo.', now() - interval '12 days', now() - interval '8 days'
    ),
    (
      '53000000-0000-4000-8000-000000000004'::uuid,
      '51000000-0000-4000-8000-000000000002'::uuid,
      '52000000-0000-4000-8000-000000000002'::uuid,
      '10000000-0000-4000-8000-000000000014'::uuid,
      '{"motivation":"Gostaria de escrever guias para a comunidade.","portfolio":"https://portfolio.demo.devsquad.local/14","experience":"Intermediario","availability":false}'::jsonb,
      'WITHDRAWN', null, 'A pessoa retirou a inscricao antes da revisao.', now() - interval '18 days', now() - interval '16 days'
    ),
    (
      '53000000-0000-4000-8000-000000000005'::uuid,
      '51000000-0000-4000-8000-000000000002'::uuid,
      '52000000-0000-4000-8000-000000000002'::uuid,
      '10000000-0000-4000-8000-000000000015'::uuid,
      '{"motivation":"Tenho interesse em documentacao tecnica.","portfolio":"https://portfolio.demo.devsquad.local/15","experience":"Intermediario","availability":true}'::jsonb,
      'SUBMITTED', null, null, now() - interval '1 day', null
    ),
    (
      '53000000-0000-4000-8000-000000000006'::uuid,
      '51000000-0000-4000-8000-000000000006'::uuid,
      '52000000-0000-4000-8000-000000000006'::uuid,
      '10000000-0000-4000-8000-000000000016'::uuid,
      '{"motivation":"Quero contribuir com ferramentas de terminal.","portfolio":"https://portfolio.demo.devsquad.local/16","experience":"Avancado","availability":true}'::jsonb,
      'ACCEPTED', v_lab_master_id, 'Experiencia forte em CLI e vontade de manter open source.', now() - interval '20 days', now() - interval '14 days'
    )
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO project_invitations (
    id, project_id, position_id, account_id, invited_by, functional_role, status, expires_at, responded_at
  )
  VALUES
    ('54000000-0000-4000-8000-000000000001'::uuid, v_project_ids[1], '51000000-0000-4000-8000-000000000001'::uuid,
     '10000000-0000-4000-8000-000000000011'::uuid, v_master_id, 'Frontend contributor', 'PENDING', now() + interval '7 days', null),
    ('54000000-0000-4000-8000-000000000002'::uuid, v_project_ids[1], null,
     '10000000-0000-4000-8000-000000000012'::uuid, v_master_id, 'Design systems', 'ACCEPTED', now() - interval '20 days', now() - interval '18 days'),
    ('54000000-0000-4000-8000-000000000003'::uuid, v_project_ids[1], null,
     '10000000-0000-4000-8000-000000000013'::uuid, v_master_id, 'Research contributor', 'DECLINED', now() - interval '18 days', now() - interval '16 days'),
    ('54000000-0000-4000-8000-000000000004'::uuid, v_project_ids[2], '51000000-0000-4000-8000-000000000002'::uuid,
     '10000000-0000-4000-8000-000000000014'::uuid, v_master_id, 'Writer', 'EXPIRED', now() - interval '2 days', null),
    ('54000000-0000-4000-8000-000000000005'::uuid, v_project_ids[5], '51000000-0000-4000-8000-000000000006'::uuid,
     '10000000-0000-4000-8000-000000000017'::uuid, v_lab_master_id, 'Go contributor', 'PENDING', now() + interval '10 days', null),
    ('54000000-0000-4000-8000-000000000006'::uuid, v_project_ids[5], null,
     '10000000-0000-4000-8000-000000000018'::uuid, v_lab_master_id, 'QA contributor', 'REVOKED', now() - interval '5 days', now() - interval '6 days')
  ON CONFLICT (id) DO NOTHING;

  FOREACH v_project_id IN ARRAY v_project_ids LOOP
    FOR v_i IN 1..5 LOOP
      INSERT INTO workflow_columns (id, project_id, name, semantic_group, position, is_default)
      VALUES (
        uuidv7(), v_project_id,
        (ARRAY['Backlog', 'Ready', 'In progress', 'Done', 'Canceled'])[v_i],
        (ARRAY['BACKLOG', 'PLANNED', 'STARTED', 'COMPLETED', 'CANCELLED'])[v_i],
        v_i - 1,
        v_i = 1
      )
      ON CONFLICT (project_id, name) DO UPDATE SET
        semantic_group = excluded.semantic_group, position = excluded.position,
        is_default = excluded.is_default, archived_at = null;
    END LOOP;

    FOR v_j IN 1..2 LOOP
      v_status := CASE
        WHEN v_project_id = v_project_ids[4] THEN 'COMPLETED'
        WHEN v_project_id = v_project_ids[6] THEN 'CANCELLED'
        ELSE 'OPEN'
      END;
      INSERT INTO milestones (id, project_id, title, description, status, start_date, due_date, created_by)
      VALUES (
        uuidv7(), v_project_id,
        CASE v_j WHEN 1 THEN 'Primeiro marco' ELSE 'Entrega e retrospectiva' END,
        CASE v_j WHEN 1 THEN 'Organizar o primeiro conjunto de entregas.' ELSE 'Validar resultados e registrar aprendizados.' END,
        v_status,
        current_date - (30 - v_j * 5),
        current_date + (45 - v_j * 12),
        CASE WHEN v_project_id = ANY(v_lab_project_ids) THEN v_lab_master_id ELSE v_master_id END
      );
    END LOOP;

    FOR v_j IN 1..5 LOOP
      INSERT INTO labels (id, project_id, name, color)
      VALUES (
        uuidv7(), v_project_id,
        (ARRAY['bug', 'feature', 'documentation', 'good first issue', 'blocked'])[v_j],
        (ARRAY['#d73a4a', '#1f883d', '#8250df', '#0969da', '#bf8700'])[v_j]
      )
      ON CONFLICT (project_id, name) DO UPDATE SET color = excluded.color;
    END LOOP;
  END LOOP;

  FOREACH v_project_id IN ARRAY v_project_ids LOOP
    SELECT coalesce(max(sequence), 0) + 1 INTO v_task_base FROM tasks WHERE project_id = v_project_id;
    v_task_ids := ARRAY[]::uuid[];
    FOR v_i IN 1..12 LOOP
      SELECT id, semantic_group
      INTO v_column_id, v_group
      FROM workflow_columns
      WHERE project_id = v_project_id AND position = ((v_i - 1) % 5);
      v_milestone_id := null;
      IF v_i > 2 THEN
        SELECT id INTO v_milestone_id
        FROM milestones
        WHERE project_id = v_project_id
        ORDER BY created_at
        OFFSET ((v_i - 3) % 2)
        LIMIT 1;
      END IF;
      v_parent_id := null;
      IF v_i IN (5, 6, 7) THEN
        v_parent_id := v_task_ids[1];
      END IF;
      v_priority := CASE v_i % 5
        WHEN 0 THEN 'URGENT' WHEN 1 THEN 'HIGH' WHEN 2 THEN 'MEDIUM' WHEN 3 THEN 'LOW' ELSE 'NONE' END;
      v_task_id := uuidv7();
      INSERT INTO tasks (
        id, project_id, sequence, parent_id, column_id, milestone_id, title, description,
        priority, start_date, due_date, position, created_by, completed_at
      )
      VALUES (
        v_task_id, v_project_id, v_task_base + v_i - 1, v_parent_id, v_column_id, v_milestone_id,
        CASE v_i
          WHEN 1 THEN 'Mapear o fluxo principal'
          WHEN 2 THEN 'Definir criterios de aceite'
          WHEN 3 THEN 'Implementar primeiro corte'
          WHEN 4 THEN 'Revisar acessibilidade'
          WHEN 5 THEN 'Adicionar estados vazios'
          WHEN 6 THEN 'Cobrir fluxo com testes'
          WHEN 7 THEN 'Documentar decisao tecnica'
          WHEN 8 THEN 'Validar com tres pessoas'
          WHEN 9 THEN 'Preparar release candidate'
          WHEN 10 THEN 'Corrigir feedback do review'
          WHEN 11 THEN 'Publicar atualizacao'
          ELSE 'Registrar aprendizados da entrega'
        END,
        'Tarefa de demonstracao para o ciclo ' || v_i || ' do projeto.',
        v_priority,
        current_date - (25 - v_i),
        current_date + (v_i * 3),
        v_i - 1,
        CASE WHEN v_project_id = ANY(v_lab_project_ids) THEN v_lab_master_id ELSE v_master_id END,
        CASE WHEN v_group = 'COMPLETED' THEN now() - (v_i || ' days')::interval ELSE null END
      );
      v_task_ids := array_append(v_task_ids, v_task_id);

      SELECT count(*) INTO v_member_count
      FROM project_memberships
      WHERE project_id = v_project_id AND status = 'ACTIVE';
      SELECT account_id INTO v_assignee_id
      FROM project_memberships
      WHERE project_id = v_project_id AND status = 'ACTIVE'
      ORDER BY account_id
      OFFSET ((v_i - 1) % greatest(v_member_count, 1))
      LIMIT 1;
      INSERT INTO task_assignees (task_id, account_id)
      VALUES (v_task_id, v_assignee_id)
      ON CONFLICT DO NOTHING;
      IF v_i % 4 = 0 THEN
        SELECT account_id INTO v_assignee_id
        FROM project_memberships
        WHERE project_id = v_project_id AND status = 'ACTIVE'
        ORDER BY account_id
        OFFSET (v_i % greatest(v_member_count, 1))
        LIMIT 1;
        INSERT INTO task_assignees (task_id, account_id)
        VALUES (v_task_id, v_assignee_id)
        ON CONFLICT DO NOTHING;
      END IF;

      SELECT id INTO v_label_id
      FROM labels
      WHERE project_id = v_project_id
      ORDER BY name
      OFFSET ((v_i - 1) % 5)
      LIMIT 1;
      INSERT INTO task_labels (task_id, label_id)
      VALUES (v_task_id, v_label_id)
      ON CONFLICT DO NOTHING;
    END LOOP;
    UPDATE projects
    SET next_task_number = greatest(next_task_number, v_task_base + 12), updated_at = now()
    WHERE id = v_project_id;
  END LOOP;

  FOREACH v_project_id IN ARRAY v_project_ids LOOP
    FOR v_task IN
      SELECT id, sequence
      FROM tasks
      WHERE project_id = v_project_id
      ORDER BY sequence
      LIMIT 3
    LOOP
      SELECT account_id INTO v_assignee_id
      FROM project_memberships
      WHERE project_id = v_project_id AND status = 'ACTIVE'
      ORDER BY account_id
      OFFSET ((v_task.sequence - 1) % 3)
      LIMIT 1;
      INSERT INTO comments (id, task_id, author_id, body, created_at)
      VALUES (
        uuidv7(), v_task.id, v_assignee_id,
        CASE (v_task.sequence % 3)
          WHEN 0 THEN 'Atualizei o contexto e deixei os proximos passos no card.'
          WHEN 1 THEN 'Validei este fluxo com o time. Podemos seguir para a proxima coluna.'
          ELSE 'Encontrei um detalhe importante; vou registrar a evidencia aqui.'
        END,
        now() - ((v_task.sequence % 12) || ' hours')::interval
      );
    END LOOP;

    FOR v_task IN
      SELECT id
      FROM tasks
      WHERE project_id = v_project_id
      ORDER BY sequence
      LIMIT 2
    LOOP
      INSERT INTO attachments (
        id, project_id, task_id, uploaded_by, object_key, original_name,
        content_type, size_bytes, status
      )
      VALUES (
        uuidv7(), v_project_id, v_task.id,
        CASE WHEN v_project_id = ANY(v_lab_project_ids) THEN v_lab_master_id ELSE v_master_id END,
        'demo/' || v_project_id || '/' || v_task.id || '/context.md',
        'context.md', 'text/markdown', 2048, 'PENDING'
      );
    END LOOP;

    FOR v_i IN 1..8 LOOP
      SELECT id INTO v_task_id
      FROM tasks
      WHERE project_id = v_project_id
      ORDER BY sequence
      OFFSET ((v_i - 1) % 12)
      LIMIT 1;
      INSERT INTO activity_events (
        hub_id, project_id, actor_id, event_type, entity_type, entity_id, metadata, occurred_at
      )
      SELECT
        p.hub_id, p.id,
        CASE WHEN p.hub_id = v_lab_hub_id THEN v_lab_master_id ELSE v_master_id END,
        (ARRAY['TASK_CREATED', 'TASK_MOVED', 'COMMENT_CREATED', 'TASK_UPDATED'])[1 + ((v_i - 1) % 4)],
        'TASK', v_task_id,
        jsonb_build_object('seed', true, 'sequence', v_i),
        now() - ((v_i * 2) || ' hours')::interval
      FROM projects p
      WHERE p.id = v_project_id;
    END LOOP;
  END LOOP;

  FOR v_project IN
    SELECT id, hub_id, status
    FROM projects
    WHERE id = ANY(v_project_ids) AND status <> 'ARCHIVED'
  LOOP
    SELECT id INTO v_task_id FROM tasks WHERE project_id = v_project.id ORDER BY sequence LIMIT 1;
    v_j := 0;
    FOR v_account_id IN
      SELECT pm.account_id
      FROM project_memberships pm
      WHERE pm.project_id = v_project.id AND pm.status = 'ACTIVE'
      ORDER BY pm.account_id
      LIMIT 8
    LOOP
      v_j := v_j + 1;
      INSERT INTO notifications (
        receiver_id, actor_id, hub_id, project_id, type, title, body,
        entity_type, entity_id, created_at, read_at
      )
      VALUES (
        v_account_id,
        CASE WHEN v_project.hub_id = v_lab_hub_id THEN v_lab_master_id ELSE v_master_id END,
        v_project.hub_id,
        v_project.id,
        CASE WHEN v_j % 3 = 0 THEN 'PROJECT_UPDATE' WHEN v_j % 3 = 1 THEN 'TASK_ASSIGNED' ELSE 'COMMENT_MENTION' END,
        CASE WHEN v_j % 3 = 0 THEN 'Atualizacao no projeto' WHEN v_j % 3 = 1 THEN 'Voce recebeu uma tarefa' ELSE 'Voce foi mencionado em um comentario' END,
        'Ha novidades para revisar no seu projeto de demonstracao.',
        'TASK', v_task_id,
        now() - (v_j || ' hours')::interval,
        CASE WHEN v_j % 2 = 0 THEN now() - ((v_j - 1) || ' hours')::interval ELSE null END
      );
    END LOOP;
  END LOOP;

  INSERT INTO activity_events (
    hub_id, project_id, actor_id, event_type, entity_type, entity_id, metadata
  )
  VALUES (
    v_default_hub_id,
    v_project_ids[1],
    v_master_id,
    'DEMO_SEED_V1',
    'SEED',
    v_marker,
    jsonb_build_object('version', 1, 'projects', 6, 'tasks', 72, 'generated_at', now())
  );
  RAISE NOTICE 'demo_seed_v1: inserted';
END
$seed$;

COMMIT;
