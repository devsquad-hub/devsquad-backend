create table accounts (
    id uuid primary key default uuidv7(),
    clerk_user_id varchar(64) not null unique,
    email varchar(320),
    display_name varchar(160) not null,
    avatar_url text,
    bio text,
    skills text[] not null default '{}',
    github_url text,
    linkedin_url text,
    portfolio_url text,
    availability_hours integer,
    status varchar(20) not null default 'ACTIVE' check (status in ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table clerk_webhook_receipts (
    message_id varchar(128) primary key,
    event_type varchar(80) not null,
    received_at timestamptz not null default now()
);

create table hubs (
    id uuid primary key default uuidv7(),
    name varchar(160) not null,
    slug varchar(80) not null unique,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table hub_memberships (
    id uuid primary key default uuidv7(),
    hub_id uuid not null references hubs(id),
    account_id uuid not null references accounts(id),
    role varchar(20) not null check (role in ('MASTER', 'ADMIN', 'MEMBER')),
    status varchar(20) not null default 'ACTIVE' check (status in ('ACTIVE', 'SUSPENDED', 'LEFT')),
    joined_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (hub_id, account_id)
);
create unique index one_active_master_per_hub on hub_memberships(hub_id) where role = 'MASTER' and status = 'ACTIVE';
create index hub_memberships_account_idx on hub_memberships(account_id, status);

create table project_proposals (
    id uuid primary key default uuidv7(),
    hub_id uuid not null references hubs(id),
    author_id uuid not null references accounts(id),
    title varchar(180) not null,
    summary text not null,
    problem text,
    proposed_solution text,
    goals text,
    desired_skills text[] not null default '{}',
    status varchar(20) not null check (status in ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    reviewer_id uuid references accounts(id),
    decision_reason text,
    project_id uuid,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    decided_at timestamptz
);
create index project_proposals_review_idx on project_proposals(hub_id, status, created_at);

create table projects (
    id uuid primary key default uuidv7(),
    hub_id uuid not null references hubs(id),
    source_proposal_id uuid unique references project_proposals(id),
    name varchar(180) not null,
    slug varchar(100) not null,
    project_key varchar(12) not null,
    summary text not null,
    description text,
    status varchar(20) not null check (status in ('PLANNING', 'RECRUITING', 'ACTIVE', 'COMPLETED', 'ARCHIVED')),
    repository_url text,
    communication_url text,
    tags text[] not null default '{}',
    start_date date,
    target_date date,
    next_task_number bigint not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    version integer not null default 0,
    unique (hub_id, slug),
    unique (hub_id, project_key)
);
alter table project_proposals add constraint project_proposals_project_fk foreign key (project_id) references projects(id);

create table project_memberships (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    account_id uuid not null references accounts(id),
    role varchar(20) not null check (role in ('ADMIN', 'MEMBER')),
    functional_role varchar(120),
    status varchar(20) not null default 'ACTIVE' check (status in ('ACTIVE', 'INACTIVE')),
    joined_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (project_id, account_id)
);
create index project_memberships_account_idx on project_memberships(account_id, status);

create table recruitment_rounds (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    name varchar(160) not null,
    description text,
    status varchar(20) not null check (status in ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED')),
    opens_at timestamptz,
    closes_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table recruitment_positions (
    id uuid primary key default uuidv7(),
    round_id uuid not null references recruitment_rounds(id),
    title varchar(140) not null,
    description text,
    skills text[] not null default '{}',
    capacity integer not null check (capacity > 0),
    filled integer not null default 0 check (filled >= 0 and filled <= capacity),
    status varchar(20) not null default 'OPEN' check (status in ('OPEN', 'FILLED', 'CLOSED')),
    version integer not null default 0,
    created_at timestamptz not null default now()
);

create table recruitment_form_versions (
    id uuid primary key default uuidv7(),
    position_id uuid not null references recruitment_positions(id),
    version integer not null,
    published_at timestamptz not null default now(),
    unique (position_id, version)
);

create table recruitment_questions (
    id uuid primary key default uuidv7(),
    form_version_id uuid not null references recruitment_form_versions(id),
    question_key varchar(64) not null,
    label varchar(500) not null,
    type varchar(30) not null check (type in ('SHORT_TEXT', 'LONG_TEXT', 'URL', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'BOOLEAN')),
    required boolean not null default false,
    position integer not null,
    options jsonb not null default '[]'::jsonb,
    unique (form_version_id, question_key),
    unique (form_version_id, position)
);

create table project_applications (
    id uuid primary key default uuidv7(),
    position_id uuid not null references recruitment_positions(id),
    form_version_id uuid not null references recruitment_form_versions(id),
    applicant_id uuid not null references accounts(id),
    answers jsonb not null,
    status varchar(20) not null check (status in ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    reviewer_id uuid references accounts(id),
    decision_note text,
    submitted_at timestamptz not null default now(),
    decided_at timestamptz
);
create unique index one_active_application_per_position on project_applications(position_id, applicant_id) where status = 'SUBMITTED';

create table project_invitations (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    position_id uuid references recruitment_positions(id),
    account_id uuid not null references accounts(id),
    invited_by uuid not null references accounts(id),
    functional_role varchar(120),
    status varchar(20) not null check (status in ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED')),
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    responded_at timestamptz
);
create unique index one_pending_invitation_per_project on project_invitations(project_id, account_id) where status = 'PENDING';

create table workflow_columns (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    semantic_group varchar(20) not null check (semantic_group in ('BACKLOG', 'PLANNED', 'STARTED', 'COMPLETED', 'CANCELLED')),
    position integer not null,
    is_default boolean not null default false,
    archived_at timestamptz,
    unique (project_id, name),
    unique (project_id, position)
);
create unique index one_default_column_per_project on workflow_columns(project_id) where is_default and archived_at is null;

create table milestones (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    title varchar(180) not null,
    description text,
    status varchar(20) not null default 'OPEN' check (status in ('OPEN', 'COMPLETED', 'CANCELLED')),
    start_date date,
    due_date date,
    created_by uuid not null references accounts(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table labels (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    name varchar(80) not null,
    color varchar(7) not null,
    unique (project_id, name)
);

create table tasks (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    sequence bigint not null,
    parent_id uuid references tasks(id),
    column_id uuid not null references workflow_columns(id),
    milestone_id uuid references milestones(id),
    title varchar(240) not null,
    description text,
    priority varchar(20) not null default 'NONE' check (priority in ('NONE', 'LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    start_date date,
    due_date date,
    position integer not null default 0,
    created_by uuid not null references accounts(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    completed_at timestamptz,
    version integer not null default 0,
    unique (project_id, sequence)
);
create index tasks_board_idx on tasks(project_id, column_id, position);
create index tasks_due_idx on tasks(project_id, due_date) where completed_at is null;

create table task_assignees (
    task_id uuid not null references tasks(id) on delete cascade,
    account_id uuid not null references accounts(id),
    primary key (task_id, account_id)
);

create table task_labels (
    task_id uuid not null references tasks(id) on delete cascade,
    label_id uuid not null references labels(id) on delete cascade,
    primary key (task_id, label_id)
);

create table comments (
    id uuid primary key default uuidv7(),
    task_id uuid not null references tasks(id),
    author_id uuid not null references accounts(id),
    body text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table attachments (
    id uuid primary key default uuidv7(),
    project_id uuid not null references projects(id),
    task_id uuid references tasks(id),
    comment_id uuid references comments(id),
    uploaded_by uuid not null references accounts(id),
    object_key text not null unique,
    original_name varchar(255) not null,
    content_type varchar(120) not null,
    size_bytes bigint not null check (size_bytes > 0),
    status varchar(20) not null check (status in ('PENDING', 'READY', 'DELETED')),
    created_at timestamptz not null default now(),
    ready_at timestamptz,
    check ((task_id is not null)::integer + (comment_id is not null)::integer = 1)
);

create table activity_events (
    id uuid primary key default uuidv7(),
    hub_id uuid not null references hubs(id),
    project_id uuid references projects(id),
    actor_id uuid references accounts(id),
    event_type varchar(80) not null,
    entity_type varchar(60) not null,
    entity_id uuid not null,
    metadata jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null default now()
);
create index activity_project_idx on activity_events(project_id, occurred_at desc);

create table notifications (
    id uuid primary key default uuidv7(),
    receiver_id uuid not null references accounts(id),
    actor_id uuid references accounts(id),
    hub_id uuid references hubs(id),
    project_id uuid references projects(id),
    type varchar(80) not null,
    title varchar(240) not null,
    body text,
    entity_type varchar(60),
    entity_id uuid,
    created_at timestamptz not null default now(),
    read_at timestamptz
);
create index notifications_unread_idx on notifications(receiver_id, read_at, created_at desc);
