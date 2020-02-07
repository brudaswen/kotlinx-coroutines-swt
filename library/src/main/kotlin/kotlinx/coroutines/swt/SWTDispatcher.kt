package kotlinx.coroutines.swt

import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Widget
import kotlin.coroutines.CoroutineContext

/**
 * Dispatches execution onto SWT event dispatching thread of the **default** [Display] and provides native
 * [kotlinx.coroutines.delay] support.
 *
 * **NOTE:** Be aware that calling this method creates a default [Display] (making the thread that invokes this
 * method its user-interface thread) if it did not already exist.
 */
@Suppress("unused")
val Dispatchers.SWT: MainCoroutineDispatcher
    get() = SwtDefault

/**
 * Dispatches execution onto SWT event dispatching thread of the given [Display] and provides native
 * [kotlinx.coroutines.delay] support.
 */
@Suppress("unused")
fun Dispatchers.swt(display: Display): MainCoroutineDispatcher =
    SwtDispatcherImpl(display)

/**
 * Dispatches execution onto SWT event dispatching thread of the given [Widget]'s [Display] and provides native
 * [kotlinx.coroutines.delay] support.
 */
@Suppress("unused")
fun Dispatchers.swt(widget: Widget): MainCoroutineDispatcher =
    SwtDispatcherImpl(widget.display)

/**
 * Base dispatcher for SWT event dispatching thread.
 *
 * This class provides type-safety and a point for future extensions.
 */
@UseExperimental(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
internal abstract class SwtDispatcher(
    internal val display: Display,
    internal val name: String
) : MainCoroutineDispatcher(), Delay {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        display.asyncExec(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val action = Runnable {
            with(continuation) { resumeUndispatched(Unit) }
        }
        val handle = schedule(timeMillis, action)
        continuation.disposeOnCancellation(handle)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle =
        schedule(timeMillis, block)

    private fun schedule(timeMillis: Long, action: Runnable): DisposableHandle {
        display.timerExec(timeMillis.toInt(), action)
        return DisposableHandle { display.timerExec(-1, action) }
    }

    override fun toString() = "SWT-$name"
}

/**
 * Immediate dispatcher for SWT event dispatching for the given [Display].
 */
private class ImmediateSWTDispatcher(display: Display, name: String) : SwtDispatcher(display, name) {
    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        Thread.currentThread() != display.thread

    override fun toString() = "${super.toString()} [immediate]"
}

/**
 * Dispatcher for SWT event dispatching for the given [Display].
 */
internal open class SwtDispatcherImpl(display: Display, name: String = display.toString()) :
    SwtDispatcher(display, name) {
    override val immediate: MainCoroutineDispatcher
        get() = ImmediateSWTDispatcher(display, name)
}

/**
 * [MainDispatcherFactory] that dispatches events for the **default** SWT [Display].
 */
@InternalCoroutinesApi
internal class SWTDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int
        get() = 2 // Swing has 0; JavaFx has 1

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher =
        SwtDefault
}

/**
 * Dispatches execution onto SWT event dispatching thread of the **default** [Display] and provides native
 * [kotlinx.coroutines.delay] support.
 */
internal object SwtDefault : SwtDispatcherImpl(Display.getDefault(), "Default")
