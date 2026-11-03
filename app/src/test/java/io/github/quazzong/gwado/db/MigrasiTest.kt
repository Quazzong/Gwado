package io.github.quazzong.gwado.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MigrasiTest {

    @Test
    fun pisahPernyataanMelewatiKomentarDanTeks() {
        val sql = """
            -- komentar; dengan titik koma dan 'kutip'
            CREATE TABLE a (x TEXT DEFAULT 'a;b'); /* blok; komentar */
            INSERT INTO a VALUES ('it''s; ok');
        """.trimIndent()
        assertEquals(
            listOf("CREATE TABLE a (x TEXT DEFAULT 'a;b')", "INSERT INTO a VALUES ('it''s; ok')"),
            pisahPernyataan(sql),
        )
    }

    @Test
    fun migrasiMembuatSkemaDanSumber() {
        Db.buka(":memory:").use { db ->
            assertEquals(MIGRASI, db.migrasi())
            assertEquals(emptyList<String>(), db.migrasi())          // kedua kali: tidak ada yang jalan
            assertEquals(MIGRASI.size.toLong(), db.satu("PRAGMA user_version")!!["user_version"])
            assertEquals(1L, db.satu("PRAGMA foreign_keys")!!["foreign_keys"])

            val tabel = db.semua("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name")
                .map { it["name"] }
            assertEquals(
                listOf("course_overrides", "courses", "preferences", "source_runs", "sources", "task_changes",
                    "task_user_state", "tasks"),
                tabel,
            )
            assertEquals(listOf("brone", "classroom", "eling"),
                db.semua("SELECT id FROM sources ORDER BY id").map { it["id"] })

            // CHECK: kode_mk_manual hanya untuk tugas manual
            assertThrows(Exception::class.java) {
                db.exec("INSERT INTO tasks (id, source_id, external_id, jenis, judul, asal, kode_mk_manual) " +
                    "VALUES ('s-1', 'brone', 'assign:1', 'assign', 'x', 'sumber', 'ABC60001')")
            }
            // foreign key aktif
            assertThrows(Exception::class.java) {
                db.exec("INSERT INTO courses (source_id, external_id, nama) VALUES ('tidakada', '1', 'x')")
            }
            // transaksi yang gagal tidak meninggalkan apa pun
            assertThrows(IllegalStateException::class.java) {
                db.transaksi {
                    db.exec("INSERT INTO preferences (kunci, nilai) VALUES ('coba', '1')")
                    error("batal")
                }
            }
            assertEquals(null, db.satu("SELECT nilai FROM preferences WHERE kunci = 'coba'"))
        }
    }
}
