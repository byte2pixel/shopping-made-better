package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Fake insert use case: records every item it was handed, and fails for any product
 * id in [failFor] so a partial failure can be exercised.
 */
private class FakeInsertItemUseCase(
    private val failFor: Set<String> = emptySet(),
) : InsertItemUseCase {
    val inserted = mutableListOf<InsertItem>()
    val boom = IOException("insert failed")

    override suspend fun execute(input: InsertItem): InsertItemUseCase.Output {
        inserted += input
        return if (input.productId in failFor) {
            InsertItemUseCase.Output.Failure(boom)
        } else {
            InsertItemUseCase.Output.Success("sli-${input.productId}")
        }
    }
}

/**
 * Unit tests for [AddTripToListUseCaseImpl] using the hand-written
 * [FakeHistoryRepository] plus a fake insert use case.
 */
class AddTripToListUseCaseTest {

    private fun input(vararg productIds: String) = AddTripToList(
        purchaseId = "trip-1",
        shoppingListId = "list-1",
        productIds = productIds.toSet(),
    )

    @Test
    fun `execute inserts only the selected line items`() = runTest {
        val insert = FakeInsertItemUseCase()
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(
                    row(purchaseId = "trip-1", id = "a", productId = "milk"),
                    row(purchaseId = "trip-1", id = "b", productId = "eggs"),
                    row(purchaseId = "trip-1", id = "c", productId = "bread"),
                ),
            ),
            insert,
        )

        val output = useCase.execute(input("milk", "bread"))

        assertEquals(AddTripToListUseCase.Output.Success(added = 2, skipped = 0), output)
        assertEquals(setOf("milk", "bread"), insert.inserted.map { it.productId }.toSet())
        assertTrue(insert.inserted.all { it.shoppingListId == "list-1" })
        assertTrue(insert.inserted.none { it.isChecked })
        assertTrue(insert.inserted.all { it.addInventory })
    }

    @Test
    fun `execute rounds fractional quantities up, never below one`() = runTest {
        val insert = FakeInsertItemUseCase()
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(
                    row(purchaseId = "trip-1", id = "a", productId = "beef", quantity = 1.5),
                    row(purchaseId = "trip-1", id = "b", productId = "tomato", quantity = 0.75),
                    row(purchaseId = "trip-1", id = "c", productId = "milk", quantity = 2.0),
                ),
            ),
            insert,
        )

        useCase.execute(input("beef", "tomato", "milk"))

        val quantities = insert.inserted.associate { it.productId to it.quantity }
        assertEquals(mapOf("beef" to 2, "tomato" to 1, "milk" to 2), quantities)
    }

    @Test
    fun `execute sums two lines of the same product into one list entry`() = runTest {
        val insert = FakeInsertItemUseCase()
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(
                    row(purchaseId = "trip-1", id = "a", productId = "milk", quantity = 1.0),
                    row(purchaseId = "trip-1", id = "b", productId = "milk", quantity = 2.0),
                ),
            ),
            insert,
        )

        val output = useCase.execute(input("milk"))

        assertEquals(AddTripToListUseCase.Output.Success(added = 1, skipped = 0), output)
        assertEquals(1, insert.inserted.size)
        assertEquals(3, insert.inserted.single().quantity)
    }

    @Test
    fun `execute skips a product no longer on the trip and still adds the rest`() = runTest {
        val insert = FakeInsertItemUseCase()
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(row(purchaseId = "trip-1", id = "a", productId = "milk")),
            ),
            insert,
        )

        // "delisted" was on screen but has since dropped out of the catalog-joined view.
        val output = useCase.execute(input("milk", "delisted"))

        assertEquals(AddTripToListUseCase.Output.Success(added = 1, skipped = 1), output)
        assertEquals(listOf("milk"), insert.inserted.map { it.productId })
    }

    @Test
    fun `execute reports partial failure when some inserts fail`() = runTest {
        val insert = FakeInsertItemUseCase(failFor = setOf("eggs"))
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(
                    row(purchaseId = "trip-1", id = "a", productId = "milk"),
                    row(purchaseId = "trip-1", id = "b", productId = "eggs"),
                ),
            ),
            insert,
        )

        val output = useCase.execute(input("milk", "eggs"))

        assertEquals(
            AddTripToListUseCase.Output.PartialFailure(added = 1, failed = 1, skipped = 0),
            output,
        )
    }

    @Test
    fun `execute fails when every insert fails`() = runTest {
        val insert = FakeInsertItemUseCase(failFor = setOf("milk"))
        val useCase = AddTripToListUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(row(purchaseId = "trip-1", id = "a", productId = "milk")),
            ),
            insert,
        )

        val output = useCase.execute(input("milk"))

        assertTrue("expected Failure but was $output", output is AddTripToListUseCase.Output.Failure)
        assertSame(insert.boom, (output as AddTripToListUseCase.Output.Failure).error)
    }

    @Test
    fun `execute fails and inserts nothing when the trip cannot be read`() = runTest {
        val insert = FakeInsertItemUseCase()
        val boom = IOException("network down")
        val useCase = AddTripToListUseCaseImpl(FakeHistoryRepository(error = boom), insert)

        val output = useCase.execute(input("milk"))

        assertTrue("expected Failure but was $output", output is AddTripToListUseCase.Output.Failure)
        assertSame(boom, (output as AddTripToListUseCase.Output.Failure).error)
        assertTrue(insert.inserted.isEmpty())
    }

    @Test
    fun `execute succeeds with nothing added when the trip is gone`() = runTest {
        val insert = FakeInsertItemUseCase()
        val useCase = AddTripToListUseCaseImpl(FakeHistoryRepository(rows = emptyList()), insert)

        val output = useCase.execute(input("milk"))

        assertEquals(AddTripToListUseCase.Output.Success(added = 0, skipped = 1), output)
        assertTrue(insert.inserted.isEmpty())
    }
}
