package com.fullsail.shoppingmadebetter.feature.product.domain

import com.fullsail.shoppingmadebetter.feature.product.data.ProductDetailDto
import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [GetProductDetailUseCaseImpl] using a handwritten fake
 * [ProductRepository].
 */
class GetProductDetailUseCaseTest {

    /** Fake repository: returns [product] for the lookup, or throws [error]. */
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

    private suspend fun productFor(dto: ProductDetailDto): ProductDetail =
        (GetProductDetailUseCaseImpl(FakeProductRepository(product = dto), fixedClock)
            .execute("p1") as GetProductDetailUseCase.Output.Success).product

    @Test
    fun `execute maps the DTO to a domain product and forwards the id on success`() = runTest {
        val repo = FakeProductRepository(product = dto)
        val useCase = GetProductDetailUseCaseImpl(repo, fixedClock)

        val output = useCase.execute("p1")

        assertTrue(output is GetProductDetailUseCase.Output.Success)
        val product = (output as GetProductDetailUseCase.Output.Success).product
        assertEquals("p1", product.id)
        assertEquals("Milk", product.name)
        assertEquals("Dairy Co", product.brand)
        assertEquals("2% milk", product.description)
        assertEquals("1 gal", product.size)
        assertEquals("http://img/milk.png", product.imageUrl)
        assertEquals(2, product.quantityOnHand)
        assertEquals("p1", repo.requestedId)
    }

    @Test
    fun `execute derives expiresInDays from the soonest expiry relative to today`() = runTest {
        assertEquals(5, productFor(dto.copy(expiryDate = today.plus(5, DateTimeUnit.DAY))).expiresInDays)
        assertEquals(0, productFor(dto.copy(expiryDate = today)).expiresInDays)
        assertEquals(-3, productFor(dto.copy(expiryDate = today.minus(3, DateTimeUnit.DAY))).expiresInDays)
        assertNull(productFor(dto).expiresInDays)
    }

    @Test
    fun `a product the user no longer holds maps to nothing on hand and no expiry`() = runTest {
        // What the view returns for a product bought on a past trip but not in the pantry:
        // the catalog columns, a zero total and no expiry date.
        val notHeld = dto.copy(quantity = 0, expiryDate = null, lowStockThreshold = null)

        val product = productFor(notHeld)

        assertEquals("Milk", product.name)
        assertEquals(0, product.quantityOnHand)
        assertNull(product.expiresInDays)
        assertNull(product.lowStockThreshold)
    }

    @Test
    fun `execute carries the low stock threshold through`() = runTest {
        assertEquals(4, productFor(dto.copy(lowStockThreshold = 4)).lowStockThreshold)
    }

    @Test
    fun `execute returns NotFound when the repository returns null`() = runTest {
        val useCase = GetProductDetailUseCaseImpl(FakeProductRepository(product = null), fixedClock)

        val output = useCase.execute("missing")

        assertTrue(output is GetProductDetailUseCase.Output.NotFound)
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetProductDetailUseCaseImpl(FakeProductRepository(error = boom), fixedClock)

        val output = useCase.execute("p1")

        assertTrue(output is GetProductDetailUseCase.Output.Failure)
        assertSame(boom, (output as GetProductDetailUseCase.Output.Failure).error)
    }
}
