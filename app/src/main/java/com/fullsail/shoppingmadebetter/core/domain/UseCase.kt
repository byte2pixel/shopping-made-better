package com.fullsail.shoppingmadebetter.core.domain

/**
 * A single unit of application logic: takes an [Input], performs work
 * (typically against a repository), and returns an [Output].
 *
 * Each concrete use case defines its own nested `Input`/`Output` so the contract
 * is self-contained. Use [Unit] as the input when no arguments are needed.
 */
interface UseCase<in Input, out Output> {
    suspend fun execute(input: Input): Output
}
