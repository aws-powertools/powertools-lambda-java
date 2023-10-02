package helloworld

import org.junit.Assert
import org.junit.Test

class AppTest {
    @Test
    fun successfulResponse() {
        val app = App()
        val result = app.handleRequest(null, null)
        Assert.assertEquals(200, result.statusCode.toLong())
        Assert.assertEquals("application/json", result.headers["Content-Type"])
        val content = result.body
        Assert.assertNotNull(content)
        Assert.assertTrue(""""message"""" in content)
        Assert.assertTrue(""""hello world"""" in content)
        Assert.assertTrue(""""location"""" in content)
    }
}
