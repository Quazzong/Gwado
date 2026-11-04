-- Lapisan kalender, acara, dan pengecualian (batal/pindah) untuk acara dan kuliah.
-- Belum diisi dari layar mana pun; kalender deadline sudah membacanya.
CREATE TABLE kalender (
  id      INTEGER PRIMARY KEY,
  kode    TEXT NOT NULL UNIQUE CHECK (kode GLOB '[a-z]*' AND kode NOT GLOB '*[^a-z0-9-]*'),
  nama    TEXT NOT NULL CHECK (length(trim(nama)) BETWEEN 1 AND 40),
  peran   TEXT NOT NULL CHECK (peran IN ('sibuk', 'tersedia', 'info')),
  tampil  INTEGER NOT NULL DEFAULT 1 CHECK (tampil IN (0, 1)),
  bawaan  INTEGER NOT NULL DEFAULT 0 CHECK (bawaan IN (0, 1)),     -- 1 = tidak bisa dihapus
  urutan  INTEGER NOT NULL DEFAULT 0,
  warna   TEXT NOT NULL DEFAULT '#8c99a6'
    CHECK (warna GLOB '#[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]' AND length(warna) = 7)
) STRICT;

CREATE TABLE acara (
  id           INTEGER PRIMARY KEY,
  kalender_id  INTEGER NOT NULL REFERENCES kalender(id),
  judul        TEXT NOT NULL CHECK (length(trim(judul)) BETWEEN 1 AND 200),
  lokasi       TEXT CHECK (lokasi IS NULL OR length(lokasi) BETWEEN 1 AND 200),
  catatan      TEXT CHECK (catatan IS NULL OR length(catatan) BETWEEN 1 AND 2000),
  jenis        TEXT NOT NULL CHECK (jenis IN ('sekali', 'seharian', 'mingguan')),
  mulai        TEXT CHECK (mulai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z'),
  selesai      TEXT CHECK (selesai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z'),
  tgl_mulai    TEXT CHECK (tgl_mulai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  tgl_selesai  TEXT CHECK (tgl_selesai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  hari         INTEGER CHECK (hari BETWEEN 1 AND 7),                -- 1 = Senin, sama dengan class_schedule
  jam_mulai    TEXT CHECK (jam_mulai GLOB '[0-2][0-9]:[0-5][0-9]'),
  jam_selesai  TEXT CHECK (jam_selesai GLOB '[0-2][0-9]:[0-5][0-9]'),
  dibuat       TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  diubah       TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  -- sekali: rentang UTC, boleh melewati tengah malam WIB
  CHECK (jenis <> 'sekali' OR (mulai IS NOT NULL AND selesai IS NOT NULL AND selesai > mulai
         AND tgl_mulai IS NULL AND tgl_selesai IS NULL
         AND hari IS NULL AND jam_mulai IS NULL AND jam_selesai IS NULL)),
  -- seharian: rentang tanggal WIB inklusif, tidak berulang
  CHECK (jenis <> 'seharian' OR (tgl_mulai IS NOT NULL AND tgl_selesai IS NOT NULL AND tgl_selesai >= tgl_mulai
         AND mulai IS NULL AND selesai IS NULL
         AND hari IS NULL AND jam_mulai IS NULL AND jam_selesai IS NULL)),
  -- mingguan: satu hari per seri, jam dinding WIB dalam satu hari,
  -- berlaku tgl_mulai..tgl_selesai (NULL = tanpa akhir)
  CHECK (jenis <> 'mingguan' OR (hari IS NOT NULL AND jam_mulai IS NOT NULL
         AND jam_selesai IS NOT NULL AND jam_selesai > jam_mulai AND tgl_mulai IS NOT NULL
         AND (tgl_selesai IS NULL OR tgl_selesai >= tgl_mulai)
         AND mulai IS NULL AND selesai IS NULL))
) STRICT;

CREATE TABLE acara_pengecualian (
  id            INTEGER PRIMARY KEY,
  acara_id      INTEGER REFERENCES acara(id),
  class_id      INTEGER REFERENCES class_schedule(id),
  tanggal_asli  TEXT NOT NULL CHECK (tanggal_asli GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  jenis         TEXT NOT NULL CHECK (jenis IN ('batal', 'pindah')),
  mulai         TEXT CHECK (mulai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z'),
  selesai       TEXT CHECK (selesai GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]Z'),
  judul         TEXT CHECK (judul IS NULL OR length(trim(judul)) BETWEEN 1 AND 200),   -- pengganti, opsional
  lokasi        TEXT CHECK (lokasi IS NULL OR length(lokasi) BETWEEN 1 AND 200),
  catatan       TEXT CHECK (catatan IS NULL OR length(catatan) BETWEEN 1 AND 2000),
  CHECK ((acara_id IS NULL) <> (class_id IS NULL)),
  CHECK (jenis <> 'batal' OR (mulai IS NULL AND selesai IS NULL
         AND judul IS NULL AND lokasi IS NULL AND catatan IS NULL)),
  CHECK (jenis <> 'pindah' OR (mulai IS NOT NULL AND selesai IS NOT NULL AND selesai > mulai))
) STRICT;

CREATE UNIQUE INDEX idx_pengecualian_acara  ON acara_pengecualian (acara_id, tanggal_asli) WHERE acara_id IS NOT NULL;
CREATE UNIQUE INDEX idx_pengecualian_kuliah ON acara_pengecualian (class_id, tanggal_asli) WHERE class_id IS NOT NULL;

INSERT INTO kalender (id, kode, nama, peran, tampil, bawaan, urutan, warna) VALUES
  (1, 'sibuk',    'Penting',      'sibuk',    1, 1, 1, '#8c99a6'),
  (2, 'pribadi',  'Pribadi',      'info',     1, 1, 2, '#9b85c9'),
  (3, 'tersedia', 'Jam tambahan', 'tersedia', 1, 1, 3, '#5fa8a0');
