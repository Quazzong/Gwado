-- Pengecualian manual aturan semester untuk satu mata kuliah. Menang atas aturan otomatis
-- saat ingest berikutnya. Diatur pengguna; ingest hanya membaca.
CREATE TABLE course_overrides (
  source_id   TEXT NOT NULL REFERENCES sources(id),
  external_id TEXT NOT NULL,
  aksi        TEXT NOT NULL CHECK (aksi IN ('ikuti', 'abaikan')),
  PRIMARY KEY (source_id, external_id)
) STRICT;
