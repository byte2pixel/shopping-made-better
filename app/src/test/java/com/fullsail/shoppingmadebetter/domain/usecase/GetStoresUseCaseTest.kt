package com.fullsail.shoppingmadebetter.domain.usecase

import com.fullsail.shoppingmadebetter.data.StoreRepository
import com.fullsail.shoppingmadebetter.data.dto.StoreDto
import com.fullsail.shoppingmadebetter.domain.usecase.impl.GetStoresUseCaseImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [GetStoresUseCaseImpl] using a hand-written fake
 * [StoreRepository] — no mocking framework needed. Demonstrates the testing
 * pattern for use cases: swap the repository for a fake and assert on the sealed
 * [GetStoresUseCase.Output].
 */
class GetStoresUseCaseTest {

    /** Fake repository: returns [result] or, if set, throws [error]. */
    private class FakeStoreRepository(
        private val result: List<StoreDto> = emptyList(),
        private val error: Throwable? = null,
    ) : StoreRepository {
        override suspend fun getStores(): List<StoreDto> = error?.let { throw it } ?: result
    }

    @Test
    fun `execute maps DTOs to domain stores on success`() = runTest {
        val dtos = listOf(
            StoreDto("1", "Corner Market", "100 Main St", "Austin", "TX", "78701", "555-0100"),
            StoreDto("2", "Green Grocer", "200 Oak Ave", "Austin", "TX", "78702", phone = null),
        )
        val useCase = GetStoresUseCaseImpl(FakeStoreRepository(result = dtos))

        val output = useCase.execute(Unit)

        assertTrue(output is GetStoresUseCase.Output.Success)
        val stores = (output as GetStoresUseCase.Output.Success).stores
        assertEquals(2, stores.size)
        assertEquals("1", stores[0].id)
        assertEquals("Corner Market", stores[0].name)
        assertEquals("100 Main St", stores[0].address)
        assertEquals("78701", stores[0].postalCode)
        assertEquals("555-0100", stores[0].phone)
        assertEquals(null, stores[1].phone)
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetStoresUseCaseImpl(FakeStoreRepository(error = boom))

        val output = useCase.execute(Unit)

        assertTrue(output is GetStoresUseCase.Output.Failure)
        assertSame(boom, (output as GetStoresUseCase.Output.Failure).error)
    }
}
