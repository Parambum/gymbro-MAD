package com.gymbro.mad

import com.gymbro.mad.data.repo.FOOD_API_BASE_URL
import com.gymbro.mad.data.repo.NetworkFoodRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class NetworkFoodRepositoryTest {

    @Test
    fun `search with blank query returns empty list without calling fetcher`() = runBlocking {
        var fetchCalled = false
        val repo = NetworkFoodRepository(
            fetcher = {
                fetchCalled = true
                ""
            },
        )

        val results = repo.search("   ")
        assertTrue(results.isEmpty())
        assertEquals(false, fetchCalled)
    }

    @Test
    fun `search parses array of food items correctly`() = runBlocking {
        val sampleJson = """
            [
              {
                "id": "food_1",
                "name": "Chicken Breast",
                "brand": "Kirkland",
                "per100g": {
                  "kcal": 165.0,
                  "proteinG": 31.0,
                  "carbsG": 0.0,
                  "fatG": 3.6
                },
                "barcode": "123456789012"
              },
              {
                "id": "food_2",
                "name": "Rolled Oats",
                "brand": "Quaker",
                "macros": {
                  "calories": 389,
                  "protein": 16.9,
                  "carbs": 66.3,
                  "fat": 6.9
                }
              }
            ]
        """.trimIndent()

        var requestedUrl: String? = null
        val repo = NetworkFoodRepository(
            fetcher = { url ->
                requestedUrl = url
                sampleJson
            },
        )

        val results = repo.search("chicken")
        assertEquals(2, results.size)
        assertEquals("$FOOD_API_BASE_URL/api/fuel/foods/search?q=chicken&limit=25", requestedUrl)

        val first = results[0]
        assertEquals("food_1", first.id)
        assertEquals("Chicken Breast", first.name)
        assertEquals("Kirkland", first.brand)
        assertEquals(165.0, first.per100g.kcal, 0.01)
        assertEquals(31.0, first.per100g.proteinG, 0.01)
        assertEquals(0.0, first.per100g.carbsG, 0.01)
        assertEquals(3.6, first.per100g.fatG, 0.01)
        assertEquals("123456789012", first.barcode)

        val second = results[1]
        assertEquals("food_2", second.id)
        assertEquals("Rolled Oats", second.name)
        assertEquals("Quaker", second.brand)
        assertEquals(389.0, second.per100g.kcal, 0.01)
        assertEquals(16.9, second.per100g.proteinG, 0.01)
        assertEquals(66.3, second.per100g.carbsG, 0.01)
        assertEquals(6.9, second.per100g.fatG, 0.01)
        assertNull(second.barcode)
    }

    @Test
    fun `search parses wrapped foods object`() = runBlocking {
        val sampleJson = """
            {
              "foods": [
                {
                  "foodId": "food_99",
                  "title": "Greek Yogurt 0%",
                  "brand_name": "Fage",
                  "kcal": 54,
                  "proteinG": 10.3,
                  "carbsG": 3.0,
                  "fatG": 0.0
                }
              ]
            }
        """.trimIndent()

        val repo = NetworkFoodRepository(
            fetcher = { sampleJson },
        )

        val results = repo.search("yogurt")
        assertEquals(1, results.size)
        val item = results.first()
        assertEquals("food_99", item.id)
        assertEquals("Greek Yogurt 0%", item.name)
        assertEquals("Fage", item.brand)
        assertEquals(54.0, item.per100g.kcal, 0.01)
        assertEquals(10.3, item.per100g.proteinG, 0.01)
    }

    @Test
    fun `byBarcode returns single food item from barcode api object`() = runBlocking {
        val sampleJson = """
            {
              "found": true,
              "food": {
                "id": "barcode_food",
                "name": "Maggi Noodles",
                "brand": "Maggi",
                "per100g": {
                  "kcal": 437,
                  "proteinG": 10.4,
                  "carbsG": 44.5,
                  "fatG": 15.7
                },
                "barcode": "8901058851298"
              }
            }
        """.trimIndent()

        var requestedUrl: String? = null
        val repo = NetworkFoodRepository(
            fetcher = { url ->
                requestedUrl = url
                sampleJson
            },
        )

        val item = repo.byBarcode("8901058851298")
        assertNotNull(item)
        assertEquals("$FOOD_API_BASE_URL/api/fuel/barcode?code=8901058851298", requestedUrl)
        assertEquals("barcode_food", item?.id)
        assertEquals("Maggi Noodles", item?.name)
        assertEquals("Maggi", item?.brand)
        assertEquals(437.0, item?.per100g?.kcal ?: 0.0, 0.01)
    }

    @Test
    fun `byBarcode returns null when found is false`() = runBlocking {
        val sampleJson = """{"found":false}"""
        val repo = NetworkFoodRepository(fetcher = { sampleJson })
        val item = repo.byBarcode("9999999999")
        assertNull(item)
    }

    @Test
    fun `byBarcode handles 404 by returning null`() = runBlocking {
        val repo = NetworkFoodRepository(
            fetcher = { throw IOException("HTTP 404 Not Found") },
        )

        val item = repo.byBarcode("0000000000")
        assertNull(item)
    }

    @Test
    fun `failed network lookup throws IOException so errors do not appear as empty cupboards`() = runBlocking {
        val repo = NetworkFoodRepository(
            fetcher = { throw IOException("HTTP 500 Internal Server Error: Database offline") },
        )

        try {
            repo.search("chicken")
            fail("Expected IOException on network failure")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("HTTP 500") == true)
        }
    }
}
