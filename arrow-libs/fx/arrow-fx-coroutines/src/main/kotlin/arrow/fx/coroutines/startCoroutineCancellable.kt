package arrow.fx.coroutines

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.resume

/**
 * Type to constraint [startCoroutineCancellable] to the [CancellableContinuation] constructor.
 */
abstract class CancellableContinuation<A> internal constructor() : Continuation<A>

/** Constructor for [CancellableContinuation] */
@Suppress("FunctionName")
fun <A> CancellableContinuation(
  ctx: CoroutineContext = ComputationPool,
  resumeWith: (Result<A>) -> Unit
): CancellableContinuation<A> = CancellableContinuation(ctx, SuspendConnection(), resumeWith)

/**
 * Starts a coroutine without a receiver and with result type [A].
 * This function creates and starts a new, fresh instance of suspendable cancellable computation every time it is invoked.
 * The [completion] continuation is invoked when the coroutine completes with a result or an exception.
 *
 * @returns Disposable handler to cancel the started suspendable cancellable computation.
 */
fun <A> (suspend () -> A).startCoroutineCancellable(completion: CancellableContinuation<A>): Disposable {
  val conn = completion.context[SuspendConnection] ?: SuspendConnection.uncancellable
  createCoroutineUnintercepted(completion).intercepted().resume(Unit)
  return {
    Platform.unsafeRunSync { conn.cancel() }
  }
}

/**
 *
 * Constructor that allows us to launch a [CancellableContinuation] on an existing [SuspendConnection].
 */
@Suppress("FunctionName")
internal fun <A> CancellableContinuation(
  ctx: CoroutineContext = ComputationPool,
  conn: SuspendConnection,
  resumeWith: (Result<A>) -> Unit
): CancellableContinuation<A> = object : CancellableContinuation<A>() {
  override val context: CoroutineContext = conn + ctx // Faster in case ctx is EmptyCoroutineContext
  override fun resumeWith(result: Result<A>) {
    if (conn.isNotCancelled()) resumeWith(result)
  }
}
