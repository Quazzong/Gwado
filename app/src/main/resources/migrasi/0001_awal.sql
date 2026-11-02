-- 0001_awal.sql — skema awal Gwado (GW6), subset skema SERPER
-- Struktur, CHECK, dan UNIQUE sama dengan migrasi SERPER 0001, 0002, 0003, dan 0005 untuk
-- tabel yang dibawa. Tidak dibawa: job_runs, class_schedule, lampiran, kalender, metrik,
-- tampilan, dan kolom *_override milik perencana. Semua waktu teks ISO 8601 UTC.

-- ===== DATA SUMBER  =====

CREATE TABLE sources (
  id     TEXT PRIMARY KEY,                -- 'brone', 'eling', 'classroom'
  nama   TEXT NOT NULL,
  url    TEXT,                            -- alamat situs; kampus lain cukup mengubah ini
  aktif  INTEGER NOT NULL DEFAULT 1 CHECK (aktif IN (0, 1))
) STRICT;

CREATE TABLE courses (
  id             INTEGER PRIMARY KEY,
  source_id      TEXT NOT NULL REFERENCES sources(id),
  external_id    TEXT NOT NULL,
  nama           TEXT NOT NULL,
  shortname      TEXT,
  kode_mk        TEXT,                    -- kosong untuk Classroom (diisi manual)
  kategori       TEXT,
  mulai          TEXT,
  selesai        TEXT,
  dibuat         TEXT,                    -- creationTime Classroom
  diikuti        INTEGER NOT NULL DEFAULT 1 CHECK (diikuti IN (0, 1)),
  alasan_diikuti TEXT,
  UNIQUE (source_id, external_id)
) STRICT;

CREATE TABLE tasks (
  id                  TEXT PRIMARY KEY,   -- 's-' + hash (sumber, ID eksternal); 'm-' + acak untuk manual
  source_id           TEXT REFERENCES sources(id),
  external_id         TEXT,
  course_id           INTEGER REFERENCES courses(id),
  jenis               TEXT NOT NULL CHECK (jenis IN ('assign', 'quiz', 'coursework', 'manual')),
  link_id             TEXT,               -- cmid Moodle, untuk membuat link
  judul               TEXT NOT NULL,
  deskripsi           TEXT,
  deadline            TEXT,
  url                 TEXT,
  status_kumpul_situs TEXT,               -- hanya ditampilkan, bukan centang pengguna
  first_seen          TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  last_seen           TEXT,
  content_hash        TEXT,
  status_situs        TEXT NOT NULL DEFAULT 'aktif' CHECK (status_situs IN ('aktif', 'hilang')),
  jumlah_absen        INTEGER NOT NULL DEFAULT 0,
  asal                TEXT NOT NULL CHECK (asal IN ('sumber', 'manual')),
  duplikat_dari       TEXT REFERENCES tasks(id),
  kode_mk_manual      TEXT,               -- hanya untuk tugas manual
  UNIQUE (source_id, external_id),
  CHECK (asal = 'manual' OR (source_id IS NOT NULL AND external_id IS NOT NULL)),
  CHECK ((asal = 'manual') = (jenis = 'manual')),
  CHECK (kode_mk_manual IS NULL OR (asal = 'manual' AND kode_mk_manual <> ''))
) STRICT;

CREATE INDEX idx_tasks_deadline ON tasks(deadline);

CREATE TABLE task_changes (
  id          INTEGER PRIMARY KEY,
  task_id     TEXT NOT NULL REFERENCES tasks(id),
  field       TEXT NOT NULL,
  nilai_lama  TEXT,
  nilai_baru  TEXT,
  waktu       TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  oleh        TEXT NOT NULL CHECK (oleh IN ('ingest', 'aku'))
) STRICT;

CREATE INDEX idx_task_changes_task ON task_changes(task_id);

CREATE TABLE source_runs (
  id            INTEGER PRIMARY KEY,
  source_id     TEXT NOT NULL REFERENCES sources(id),
  fetch_id      TEXT NOT NULL UNIQUE,     -- mencegah ingest ganda
  mulai         TEXT NOT NULL,            -- waktu fetch (UTC), bukan waktu ingest
  selesai       TEXT NOT NULL,
  status        TEXT NOT NULL CHECK (status IN ('success', 'partial', 'failed')),
  jumlah_tugas  INTEGER CHECK (jumlah_tugas IS NULL OR jumlah_tugas >= 0),
  error_singkat TEXT
) STRICT;

CREATE INDEX source_runs_per_sumber ON source_runs (source_id, mulai);

-- ===== DATA PENGGUNA  =====

-- Tugas tanpa baris di sini dianggap 'todo'
CREATE TABLE task_user_state (
  task_id  TEXT PRIMARY KEY REFERENCES tasks(id),
  status   TEXT NOT NULL DEFAULT 'todo' CHECK (status IN ('todo', 'doing', 'done')),
  done_at  TEXT,
  catatan  TEXT
) STRICT;

-- Termasuk aturan semester (semester_label, semester_kode, semester_mulai); diisi di GW7
CREATE TABLE preferences (
  kunci TEXT PRIMARY KEY,
  nilai TEXT NOT NULL
) STRICT;

INSERT INTO sources (id, nama, url) VALUES
  ('brone',     'Brone',            'https://brone.ub.ac.id'),
  ('eling',     'Eling',            'https://eling.ub.ac.id'),
  ('classroom', 'Google Classroom', 'https://classroom.google.com');
