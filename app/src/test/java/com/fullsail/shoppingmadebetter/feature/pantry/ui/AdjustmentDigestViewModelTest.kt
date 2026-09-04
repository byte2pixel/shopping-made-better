package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentDigestEntry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetAdjustmentDigestUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AdjustmentDigestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake digest use case: returns a settable [output] and counts the calls. */
    private class FakeGetAdjustmentDigestUseCase(
        var output: GetAdjustmentDigestUseCase.Output =
            GetAdjustmentDigestUseCase.Output.Success(emptyList()),
    ) : GetAdjustmentDigestUseCase {
        var callCount = 0
        override suspend fun execute(input: Unit): GetAdjustmentDigestUseCase.Output {
            callCount++
            return output
        }
    }

    /**
     * Fake undo use case: records every id it was asked to reverse and fails the ones in
     * [failFor]. [gate] lets a test hold the call suspended to observe the in-flight state.
     */
    private class FakeUndoInventoryAdjustmentUseCase(
        private val failFor: Set<String> = emptySet(),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : UndoInventoryAdjustmentUseCase {
        val undone = mutableListOf<String>()
        override suspend fun execute(
            input: UndoInventoryAdjustment,
        ): UndoInventoryAdjustmentUseCase.Output {
            gate?.await()
            undone += input.adjustmentId
            return if (input.adjustmentId in failFor) {
                UndoInventoryAdjustmentUseCase.Output.Failure(IOException("boom"))
            } else {
                UndoInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 1)
            }
        }
    }

    private fun entry(adjustmentId: String, productName: String = "Jasmine Rice") =
        AdjustmentDigestEntry(
            adjustmentId = adjustmentId,
            lotId = "lot-$adjustmentId",
            productId = "p1",
            productName = productName,
            imageUrl = "",
            delta = -1,
            quantityNow = 2,
            productQuantity = 2,
            daysAgo = 0,
        )

    private fun buildViewModel(
        digest: FakeGetAdjustmentDigestUseCase = FakeGetAdjustmentDigestUseCase(),
        undo: FakeUndoInventoryAdjustmentUseCase = FakeUndoInventoryAdjustmentUseCase(),
    ) = AdjustmentDigestViewModel(digest, undo)

    private fun digestOf(vararg ids: String) = FakeGetAdjustmentDigestUseCase(
        GetAdjustmentDigestUseCase.Output.Success(ids.map { entry(it) })
    )

    private val AdjustmentDigestUiState.entries: List<AdjustmentDigestEntry>
        get() = (this as AdjustmentDigestUiState.Success).entries

    @Test
    fun `loads the digest on creation`() = runTest {
        val viewModel = buildViewModel(digest = digestOf("a1", "a2"))

        assertEquals(listOf("a1", "a2"), viewModel.uiState.value.entries.map { it.adjustmentId })
    }

    @Test
    fun `an empty digest is Success, not an error`() = runTest {
        val viewModel = buildViewModel(digest = digestOf())

        assertTrue(viewModel.uiState.value is AdjustmentDigestUiState.Success)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }

    @Test
    fun `a failed read shows Error`() = runTest {
        val digest = FakeGetAdjustmentDigestUseCase(
            GetAdjustmentDigestUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(digest = digest)

        assertEquals(AdjustmentDigestUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `load retries after an error`() = runTest {
        val digest = FakeGetAdjustmentDigestUseCase(
            GetAdjustmentDigestUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(digest = digest)
        assertEquals(AdjustmentDigestUiState.Error, viewModel.uiState.value)

        digest.output = GetAdjustmentDigestUseCase.Output.Success(listOf(entry("a1")))
        viewModel.load()

        assertEquals(2, digest.callCount)
        assertEquals(listOf("a1"), viewModel.uiState.value.entries.map { it.adjustmentId })
    }

    @Test
    fun `onUndo reverses that row and removes only it`() = runTest {
        val undo = FakeUndoInventoryAdjustmentUseCase()
        val viewModel = buildViewModel(digest = digestOf("a1", "a2"), undo = undo)

        viewModel.onUndo(viewModel.uiState.value.entries.first())

        assertEquals(listOf("a1"), undo.undone)
        assertEquals(listOf("a2"), viewModel.uiState.value.entries.map { it.adjustmentId })
    }

    @Test
    fun `a failed onUndo puts the row back where it was and says so`() = runTest {
        val undo = FakeUndoInventoryAdjustmentUseCase(failFor = setOf("a2"))
        val viewModel = buildViewModel(digest = digestOf("a1", "a2", "a3"), undo = undo)

        viewModel.onUndo(viewModel.uiState.value.entries[1])

        assertEquals(
            listOf("a1", "a2", "a3"),
            viewModel.uiState.value.entries.map { it.adjustmentId },
        )
        assertEquals(
            AdjustmentDigestEvent.UndoFailed("Jasmine Rice"),
            viewModel.events.first(),
        )
    }

    @Test
    fun `onUndoAll reverses every row in order and reports the total`() = runTest {
        val undo = FakeUndoInventoryAdjustmentUseCase()
        val viewModel = buildViewModel(digest = digestOf("a1", "a2", "a3"), undo = undo)

        viewModel.onUndoAll()

        assertEquals(listOf("a1", "a2", "a3"), undo.undone)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertEquals(AdjustmentDigestEvent.UndoneAll(3, 3), viewModel.events.first())
    }

    @Test
    fun `onUndoAll keeps the rows it could not reverse`() = runTest {
        val undo = FakeUndoInventoryAdjustmentUseCase(failFor = setOf("a2"))
        val viewModel = buildViewModel(digest = digestOf("a1", "a2", "a3"), undo = undo)

        viewModel.onUndoAll()

        assertEquals(listOf("a2"), viewModel.uiState.value.entries.map { it.adjustmentId })
        assertEquals(AdjustmentDigestEvent.UndoneAll(2, 3), viewModel.events.first())
    }

    @Test
    fun `undoingAll is set while the batch runs and cleared after`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val undo = FakeUndoInventoryAdjustmentUseCase(gate = gate)
        val viewModel = buildViewModel(digest = digestOf("a1", "a2"), undo = undo)

        viewModel.onUndoAll()
        assertTrue((viewModel.uiState.value as AdjustmentDigestUiState.Success).undoingAll)

        gate.complete(Unit)

        assertFalse((viewModel.uiState.value as AdjustmentDigestUiState.Success).undoingAll)
        assertEquals(listOf("a1", "a2"), undo.undone)
    }

    @Test
    fun `onUndo is ignored while a bulk undo is running`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val undo = FakeUndoInventoryAdjustmentUseCase(gate = gate)
        val viewModel = buildViewModel(digest = digestOf("a1", "a2"), undo = undo)
        val first = viewModel.uiState.value.entries.first()

        viewModel.onUndoAll()
        viewModel.onUndo(first)
        gate.complete(Unit)

        // Each row was reversed once, by the batch.
        assertEquals(listOf("a1", "a2"), undo.undone)
    }

    @Test
    fun `onUndoAll does nothing when there is nothing listed`() = runTest {
        val undo = FakeUndoInventoryAdjustmentUseCase()
        val viewModel = buildViewModel(digest = digestOf(), undo = undo)

        viewModel.onUndoAll()

        assertTrue(undo.undone.isEmpty())
        assertFalse((viewModel.uiState.value as AdjustmentDigestUiState.Success).undoingAll)
    }
}
