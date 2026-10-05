package com.fullsail.shoppingmadebetter.feature.product.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.AdjustmentDigestEntryDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.product.data.ProductDetailDto
import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [GetProductDetailUseCaseImpl] using handwritten fakes for
 * [ProductRepository] and [PantryRepository].
 */
class GetProductDetailUseCaseTest {

    /** Fake product repository: returns [product] for the lookup, or throws [error]. */
    private class FakeProductRepository(
        private val product: ProductDetailDto? = null,
        private val error: Throwable? = null,
    ) : ProductRepository {
        var requestedId: String? = null

        override suspend fun getProductDetail(productId: String): ProductDetailDto? {
            requestedId = productId
            return error?.let { throw it } ?: product
        }
    }

    /** Fake pantry repository: returns [lots] for the per-product read, or throws [error]. */
    private class FakePantryRepository(
        private val lots: List<InventoryItemDto> = emptyList(),
        private val error: Throwable? = null,
    ) : PantryRepository {
        var requestedProductId: String? = null

        override suspend fun getInventoryItems(productId: String): List<InventoryItemDto> {
            requestedProductId = productId
            return error?.let { throw it } ?: lots
        }

        override suspend fun getInventoryItems(): List<InventoryItemDto> = lots
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit
        override suspend fun applyInventoryAdjustment(
            id: String,
            delta: Int,
            reason: String,
        ): InventoryAdjustmentResultDto = error("not used")

        override suspend fun undoInventoryAdjustment(adjustmentId: String): InventoryAdjustmentResultDto =
            error("not used")

        override suspend fun getAdjustmentDigest(): List<AdjustmentDigestEntryDto> = emptyList()
        override suspend fun addInventoryItem(productId: String, quantity: Int, location: String?): String =
            error("not used")
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-07-24T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())

    private val dto = ProductDetailDto(
        id = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        imageUrl = "http://img/milk.png",
        quantity = 2,
        expiryDate = null,
    )

    private fun lot(
        id: String,
        expiryDate: LocalDate?,
        quantity: Int = 1,
        location: String = "fridge",
        addedBy: String? = null,
        isOwn: Boolean = true,
    ) = InventoryItemDto(
        id = id,
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        quantity = quantity,
        imageUrl = "http://img/milk.png",
        expiryDate = expiryDate,
        location = location,
        addedBy = addedBy,
        isOwn = isOwn,
    )

    private fun useCase(
        products: FakeProductRepository = FakeProductRepository(product = dto),
        pantry: FakePantryRepository = FakePantryRepository(),
    ) = GetProductDetailUseCaseImpl(products, pantry, fixedClock)

    /**
     * The concurrent reads run under `async`, so coroutines hand the catch a stack-trace
     * recovered copy of the thrown exception; compare the type and message, not identity.
     */
    private fun assertFailureCarries(expected: Throwable, failure: GetProductDetailUseCase.Output.Failure) {
        assertEquals(expected::class, failure.error::class)
        assertEquals(expected.message, failure.error.message)
    }

    private suspend fun productFor(lots: List<InventoryItemDto>): ProductDetail =
        (useCase(pantry = FakePantryRepository(lots = lots)).execute("p1")
            as GetProductDetailUseCase.Output.Success).product

    @Test
    fun `execute maps the DTO to a domain product and forwards the id to both reads`() = runTest {
        val products = FakeProductRepository(product = dto)
        val pantry = FakePantryRepository(lots = listOf(lot("l1", null), lot("l2", null)))

        val output = useCase(products, pantry).execute("p1")

        assertTrue(output is GetProductDetailUseCase.Output.Success)
        val product = (output as GetProductDetailUseCase.Output.Success).product
        assertEquals("p1", product.id)
        assertEquals("Milk", product.name)
        assertEquals("Dairy Co", product.brand)
        assertEquals("2% milk", product.description)
        assertEquals("1 gal", product.size)
        assertEquals("http://img/milk.png", product.imageUrl)
        assertEquals(2, product.lots.size)
        assertEquals(2, product.quantityOnHand)
        assertEquals("p1", products.requestedId)
        assertEquals("p1", pantry.requestedProductId)
    }

    @Test
    fun `lots come back soonest expiry first with undated last`() = runTest {
        val product = productFor(
            listOf(
                lot("undated", null),
                lot("later", today.plus(9, DateTimeUnit.DAY)),
                lot("soon", today.plus(2, DateTimeUnit.DAY), quantity = 3, location = "pantry"),
            ),
        )

        assertEquals(listOf("soon", "later", "undated"), product.lots.map { it.id })
        assertEquals(PantryLocation.Pantry, product.lots.first().location)
        assertEquals(3, product.lots.first().quantity)
        assertEquals(5, product.quantityOnHand)
    }

    @Test
    fun `expiresInDays is the soonest lot's expiry relative to today`() = runTest {
        assertEquals(
            5,
            productFor(
                listOf(lot("l1", today.plus(9, DateTimeUnit.DAY)), lot("l2", today.plus(5, DateTimeUnit.DAY))),
            ).expiresInDays,
        )
        assertEquals(0, productFor(listOf(lot("l1", today))).expiresInDays)
        assertEquals(-3, productFor(listOf(lot("l1", today.minus(3, DateTimeUnit.DAY)))).expiresInDays)
        assertNull(productFor(listOf(lot("l1", null))).expiresInDays)
    }

    @Test
    fun `a housemate's lot keeps who added it`() = runTest {
        val product = productFor(listOf(lot("l1", null, addedBy = "Demo Roommate", isOwn = false)))

        assertEquals("Demo Roommate", product.lots.single().addedBy)
        assertEquals(false, product.lots.single().isOwn)
    }

    @Test
    fun `a product the user no longer holds maps to no lots, nothing on hand and no expiry`() = runTest {
        val product = productFor(emptyList())

        assertEquals("Milk", product.name)
        assertTrue(product.lots.isEmpty())
        assertEquals(0, product.quantityOnHand)
        assertNull(product.expiresInDays)
        assertNull(product.lowStockThreshold)
    }

    @Test
    fun `execute carries the low stock threshold through`() = runTest {
        val output = useCase(products = FakeProductRepository(product = dto.copy(lowStockThreshold = 4)))
            .execute("p1") as GetProductDetailUseCase.Output.Success

        assertEquals(4, output.product.lowStockThreshold)
    }

    @Test
    fun `execute returns NotFound when the product is missing even if lots exist`() = runTest {
        val output = useCase(
            products = FakeProductRepository(product = null),
            pantry = FakePantryRepository(lots = listOf(lot("l1", null))),
        ).execute("missing")

        assertTrue(output is GetProductDetailUseCase.Output.NotFound)
    }

    @Test
    fun `execute returns Failure carrying the error when the product read throws`() = runTest {
        val boom = IOException("network down")

        val output = useCase(products = FakeProductRepository(error = boom)).execute("p1")

        assertTrue(output is GetProductDetailUseCase.Output.Failure)
        assertFailureCarries(boom, output as GetProductDetailUseCase.Output.Failure)
    }

    @Test
    fun `execute returns Failure carrying the error when the lots read throws`() = runTest {
        val boom = IOException("network down")

        val output = useCase(pantry = FakePantryRepository(error = boom)).execute("p1")

        assertTrue(output is GetProductDetailUseCase.Output.Failure)
        assertFailureCarries(boom, output as GetProductDetailUseCase.Output.Failure)
    }
}
