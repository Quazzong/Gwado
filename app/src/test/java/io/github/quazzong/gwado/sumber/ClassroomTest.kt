package io.github.quazzong.gwado.sumber

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class ClassroomTest {
    /** coursework_<id>.json → {id: isi}, urut nama file seperti sorted(glob) di Python. */
    private fun perKelas(awalan: String): Map<String, JsonObject> =
        Kontrak.file("fixtures/classroom").listFiles()!!
            .filter { it.name.startsWith(awalan) && it.name.endsWith(".json") }
            .sortedBy { it.name }
            .associate {
                it.nameWithoutExtension.removePrefix(awalan) to Json.parseToJsonElement(it.readText()).jsonObject
            }

    @Test fun golden() {
        val hasil = Classroom.normalisasi(
            Kontrak.muat("fixtures/classroom/courses.json"),
            perKelas("coursework_"),
            perKelas("submissions_"),
        )
        Kontrak.cocokGolden("classroom", hasil)
    }
}
