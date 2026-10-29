package com.gymbro.mad

import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import com.gymbro.mad.data.repo.EmptyFoodRepository
import com.gymbro.mad.data.repo.FoodRepository
import com.gymbro.mad.data.repo.NetworkFoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Ticket J-3: Barcode scanning & lookup.
 */
class BarcodeScannerTest {

    @Test
    fun emptyRepositoryReturnsNullForBarcode() = runTest {
        val repo: FoodRepository = EmptyFoodRepository()
        val result = repo.byBarcode("8901030383749")
        assertNull(result)
    }

    @Test
    fun mockBarcodeLookupFindsExistingProduct() = runTest {
        val testItem = FoodItem(
            id = "food_1",
            name = "Greek Yogurt",
            brand = "Amul",
            per100g = Macros(kcal = 97.0, proteinG = 10.0, carbsG = 4.0, fatG = 0.5),
            barcode = "8901262010114",
        )
        val mockRepo = object : FoodRepository {
            override suspend fun search(query: String): List<FoodItem> = listOf(testItem)
            override suspend fun byBarcode(barcode: String): FoodItem? {
                return if (barcode == "8901262010114") testItem else null
            }
        }

        val found = mockRepo.byBarcode("8901262010114")
        assertNotNull(found)
        assertEquals("Greek Yogurt", found?.name)
        assertEquals(97.0, found?.per100g?.kcal ?: 0.0, 0.01)
        assertEquals("8901262010114", found?.barcode)
    }

    @Test
    fun mockBarcodeLookupReturnsNullForUnknownBarcode() = runTest {
        val mockRepo = object : FoodRepository {
            override suspend fun search(query: String): List<FoodItem> = emptyList()
            override suspend fun byBarcode(barcode: String): FoodItem? = null
        }

        val unknown = mockRepo.byBarcode("0000000000000")
        assertNull(unknown)
    }

    @Test
    fun manualEntryFromUnknownBarcodeRetainsBarcodeAndValidatesMacros() {
        val scannedBarcode = "012345678905"
        val manualItem = FoodItem(
            id = "custom_123",
            name = "Local Bakery Sourdough",
            per100g = Macros(
                kcal = 240.0,
                proteinG = 8.5,
                carbsG = 48.0,
                fatG = 1.2,
            ),
            barcode = scannedBarcode,
        )

        assertEquals("012345678905", manualItem.barcode)
        assertEquals(240.0, manualItem.per100g.kcal, 0.01)

        // Verify live scaling for a 150g portion
        val portionMacros = manualItem.per100g.scaled(150.0 / 100.0)
        assertEquals(360.0, portionMacros.kcal, 0.01)
        assertEquals(12.75, portionMacros.proteinG, 0.01)
    }

    @Test
    fun networkRepositoryHandlesEmptyBarcodeGracefully() = runTest {
        val repo = NetworkFoodRepository()
        assertNull(repo.byBarcode(""))
        assertNull(repo.byBarcode("   "))
    }
}
