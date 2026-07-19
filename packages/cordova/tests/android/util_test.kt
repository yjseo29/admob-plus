package admob.plus.core

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

internal class optFloatTest {
    @Test
    fun `value = undefined`() {
        assertEquals(optFloat(JSONObject("{}"), "key"), null)
    }

    @Test
    fun `value = null`() {
        assertEquals(optFloat(JSONObject("""{"v": null}"""), "v"), null)
    }

    @Test
    fun `value = 1`() {
        assertEquals(optFloat(JSONObject("""{"v": 1}"""), "v"), 1.0f)
    }
}
