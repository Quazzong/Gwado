package io.github.quazzong.gwado.db

import android.util.Log
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Spike GW6b di HP: sama dengan SpikeSqliteTest, tapi memakai .so ARM dari APK. */
@RunWith(AndroidJUnit4::class)
class SpikeSqliteHpTest {

    @Test
    fun fiturIngestSerperTersediaDiHp() {
        BundledSQLiteDriver().open(":memory:").use { c ->
            val versi = c.prepare("SELECT sqlite_version()").use { it.step(); it.getText(0) }
            Log.i("GwadoSpike", "SQLite bundled di HP: $versi")
            val (mayor, minor) = versi.split(".").map { it.toInt() }
            assertTrue("butuh >= 3.37, dapat $versi", mayor > 3 || (mayor == 3 && minor >= 37))

            c.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY, k TEXT NOT NULL UNIQUE, " +
                "n INTEGER NOT NULL CHECK (n >= 0)) STRICT")
            val upsert = "INSERT INTO t (k, n) VALUES (?, ?) " +
                "ON CONFLICT (k) DO UPDATE SET n = excluded.n RETURNING id"
            fun simpan(k: String, n: Long): Long = c.prepare(upsert).use { st ->
                st.bindText(1, k); st.bindLong(2, n); st.step(); st.getLong(0)
            }
            assertEquals(simpan("x", 1), simpan("x", 2))
            assertThrows(Exception::class.java) { c.execSQL("INSERT INTO t (k, n) VALUES ('y', 'abc')") }

            val jumlah = c.prepare("SELECT count(*) FROM json_each(?)").use { st ->
                st.bindText(1, """["a","b","c"]"""); st.step(); st.getLong(0)
            }
            assertEquals(3L, jumlah)
        }
    }
}
