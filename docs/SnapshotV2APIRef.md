# Snapshot V2 API Ref

## Purpose

This document defines the canonical `Snapshot V2` sync contract for Deadliner.

It is the migration reference for:

- iOS
- Android
- HarmonyOS

It covers:

- DDL data model changes
- local database migration requirements
- WebDAV sync file strategy
- V1 and V2 compatibility rules

This document is normative for cross-platform migration.

## Core Principles

1. `Snapshot V2` is the canonical sync model.
2. `Snapshot V1` is compatibility output only.
3. Business lifecycle state is modeled by `state`.
4. Sync deletion remains modeled by `deleted` or local `tombstone`.
5. Inner Todo is part of DDL document content, not an independently synchronized record.
6. Invalid states must fail explicitly. Do not guess.

## DDL Lifecycle Model

### Canonical state

```json
"state": "active | completed | archived | abandoned"
```

Meaning:

- `active`: normal working state
- `completed`: task finished by user
- `archived`: task already completed and moved out of active list
- `abandoned`: user explicitly gave up the task

### Tombstone

Tombstone is not part of the business state machine.

It remains an independent sync-layer deletion signal.

Meaning:

- `state` answers: what is the task lifecycle status?
- `deleted` answers: should the record be treated as deleted in sync?

## Snapshot V2 File

### WebDAV path

Canonical V2 file:

- `Deadliner/snapshot-v2.json`

Compatibility V1 file:

- `Deadliner/snapshot-v1.json`

## Root Schema

```json
{
  "version": {
    "ts": "2026-03-23T12:00:00Z",
    "dev": "ABC123"
  },
  "items": [
    {
      "uid": "ABC123:1",
      "ver": {
        "ts": "2026-03-23T12:00:00Z",
        "ctr": 0,
        "dev": "ABC123"
      },
      "deleted": false,
      "doc": {
        "id": 1,
        "name": "Write report",
        "start_time": "2026-03-23T08:00:00",
        "end_time": "2026-03-23T18:00:00",
        "state": "active",
        "complete_time": "",
        "note": "example",
        "is_stared": 0,
        "type": "task",
        "habit_count": 0,
        "habit_total_count": 0,
        "calendar_event": -1,
        "timestamp": "2026-03-23T08:00:00",
        "sub_tasks": []
      }
    }
  ]
}
```

## Field Definitions

### Root.version

- `ts`: snapshot creation time in ISO string form
- `dev`: device id

### Item

- `uid`: globally unique record id
- `ver`: per-record version
- `deleted`: tombstone flag
- `doc`: omitted or `null` only when `deleted == true`

### Item.ver

- `ts`: version timestamp
- `ctr`: tie-break counter for same timestamp
- `dev`: writer device id

### Doc fields

- `id`: local legacy id, not globally unique
- `name`: DDL title
- `start_time`: ISO string
- `end_time`: ISO string
- `state`: canonical lifecycle state
- `complete_time`: ISO string, empty string allowed if not completed-like
- `note`: note body
- `is_stared`: `0 | 1`
- `type`: currently `task` or `habit`
- `habit_count`: existing habit progress field
- `habit_total_count`: existing habit target field
- `calendar_event`: existing calendar event id
- `timestamp`: business timestamp string
- `sub_tasks`: embedded inner todo array

## Inner Todo Schema

```json
{
  "id": "sub-1",
  "content": "Draft outline",
  "is_completed": 0,
  "sort_order": 0,
  "created_at": "2026-03-23T08:00:00Z",
  "updated_at": "2026-03-23T08:05:00Z"
}
```

Field rules:

- `id`: stable string id, must remain stable across edits
- `content`: plain text content
- `is_completed`: `0 | 1`
- `sort_order`: integer ordering
- `created_at`: optional ISO string
- `updated_at`: optional ISO string

## Local Database Migration

### DDL table changes

All platforms should move toward this logical DDL model:

- add `state`
- keep `tombstone` or equivalent deleted marker independent
- embed inner todo into DDL document payload

Recommended local fields:

- `state`
- `complete_time`
- `sub_tasks_json`
- `is_tombstoned`

Legacy fields that must be retired from canonical logic:

- `isCompleted`
- `isArchived`

They may exist temporarily during migration, but must not remain the canonical source of truth.

### Legacy backfill rules

When migrating old local records:

- if `isArchived == true`, migrate to `state = archived`
- else if `isCompleted == true`, migrate to `state = completed`
- else migrate to `state = active`

### Invalid data handling

Required rule:

- if `state` is invalid, throw an error
- do not silently downgrade unknown states
- do not infer a new state from unrelated fields when canonical state already exists

## State Transition Rules

Allowed transitions:

- `active -> completed`
- `active -> abandoned`
- `completed -> archived`
- `completed -> active`
- `archived -> completed`
- `archived -> active`
- `abandoned -> active`

No other transition should be accepted silently.

If a client tries to apply an invalid transition, it must fail explicitly.

## WebDAV Migration Strategy

### Files

During migration, clients must understand the following:

- V2 source of truth: `Deadliner/snapshot-v2.json`
- V1 compatibility file: `Deadliner/snapshot-v1.json`

### Read strategy

Recommended strategy:

1. Try loading `snapshot-v2.json`
2. If present, decode V2 directly
3. Also load `snapshot-v1.json` if needed for compatibility merge
4. Convert V1 records into V2 records before merge

### Write strategy

Recommended strategy:

1. Merge in V2 model
2. Write `snapshot-v2.json`
3. Project merged V2 data into V1-compatible shape
4. Write `snapshot-v1.json`

This keeps new clients canonical while old clients continue to function.

### Concurrency

Writes must continue to use version-precondition logic:

- compare by `ver.ts`
- if equal, compare `ver.ctr`
- if equal, compare `ver.dev`

For WebDAV conditional writes:

- use remote `ETag` when available
- on `412 Precondition Failed`, reload remote and merge again

Do not overwrite remote files blindly.

## V1 Compatibility Projection

V1 has no canonical `state` field and no `sub_tasks`.

V2 to V1 projection rules:

- `active` -> `is_completed = 0`, `is_archived = 0`
- `completed` -> `is_completed = 1`, `is_archived = 0`
- `archived` -> `is_completed = 1`, `is_archived = 1`
- `abandoned` -> downgrade to `archived`

This downgrade is intentionally lossy.

Reason:

- V1 clients cannot represent `abandoned`
- compatibility exists to keep old clients functional, not to preserve full V2 semantics

### SubTask projection

`sub_tasks` are dropped when projecting V2 into V1.

Reason:

- V1 has no canonical field for inner todo
- old clients must not invent partial semantics

## V1 to V2 Upgrade Rules

When a client reads V1 and upgrades it into V2:

- `is_archived == 1` -> `state = archived`
- else if `is_completed == 1` -> `state = completed`
- else -> `state = active`
- `sub_tasks = []`

Do not fabricate `abandoned` from V1 data.

## Platform Migration Checklist

### Android

Required work:

- add canonical `state`
- add embedded `sub_tasks_json`
- update repository logic to use `state`
- stop using `isCompleted/isArchived` as canonical fields
- implement V1 to V2 and V2 to V1 projection at sync boundary
- preserve independent tombstone handling

### HarmonyOS

Required work:

- same as Android
- keep local state machine strict
- keep V1 compatibility only in sync projection layer

### iOS

Current expected direction:

- already migrated to canonical `state`
- already embedding inner todo into DDL payload
- already treating V2 as canonical target

## Strictness Requirements

The following are prohibited during migration:

- silently swallowing invalid `state`
- converting unknown states into `active`
- mixing canonical business logic with V1 compatibility logic
- treating Markdown as canonical inner todo persistence
- overwriting V2 with V1 semantics

## Minimal Client Requirements

A compliant V2 client must:

1. Read and write `snapshot-v2.json`
2. Understand all four states
3. Keep tombstone independent from state
4. Preserve `sub_tasks`
5. Reject invalid V2 states
6. Project V2 into V1 for compatibility if old clients still exist

## Final Rule

If a conflict exists between:

- old local boolean semantics
- old V1 sync semantics
- new V2 canonical semantics

Then V2 canonical semantics win inside the app model.

Compatibility handling must remain at the sync boundary only.
