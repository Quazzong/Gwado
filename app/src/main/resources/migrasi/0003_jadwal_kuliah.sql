-- Jadwal kuliah. Belum diisi dari layar mana pun; label matkul dan tugas manual sudah membacanya.
CREATE TABLE class_schedule (
  id             INTEGER PRIMARY KEY,
  hari           INTEGER NOT NULL CHECK (hari BETWEEN 1 AND 7),  -- 1 = Senin ... 7 = Minggu
  mulai          TEXT NOT NULL,           -- jam lokal WIB, 'HH:MM'
  selesai        TEXT NOT NULL,
  kode_mk        TEXT,
  matkul         TEXT NOT NULL,
  kelas          TEXT,
  dosen          TEXT,
  ruang          TEXT,
  berlaku_dari   TEXT,                    -- 'YYYY-MM-DD'
  berlaku_sampai TEXT
) STRICT;
