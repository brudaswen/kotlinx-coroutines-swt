package kotlinx.coroutines.swt

import kotlinx.coroutines.*
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Widget
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches a new coroutine using the dispatcher for the given [display].
 *
 * The coroutine is only launched if (and only if) the display is not already disposed.
 *
 * Otherwise, this method and its parameters behave like [kotlinx.coroutines.launch].
 *
 * **Note:** Does not work with [CoroutineStart.LAZY].
 *
 * @param display The display that is checked for its state.
 * @return The [Job] if a new coroutine was launched or `null` if the display was disposed.
 *
 * @see kotlinx.coroutines.launch
 */
fun CoroutineScope.launch(
    display: Display,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job? =
    if (!display.isDisposed) {
        val context = coroutineContext + Dispatchers.swt(display)
        launch(context, start, block)
    } else {
        null
    }

/**
 * Launches a new coroutine using the dispatcher for the given [widget].
 *
 * The coroutine is only launched if (and only if) the widget is not already disposed.
 *
 * Otherwise, this method and its parameters behave like [kotlinx.coroutines.launch].
 *
 * **Note:** Does not work with [CoroutineStart.LAZY].
 *
 * @param widget The widget that is checked for its state.
 * @return The [Job] if a new coroutine was launched or `null` if the widget was disposed.
 *
 * @see kotlinx.coroutines.launch
 */
fun <T : Widget> CoroutineScope.launch(
    widget: T,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job? =
    if (!widget.isDisposed) {
        val context = coroutineContext + Dispatchers.swt(widget)
        launch(context, start, block)
    } else {
        null
    }

/**
 * Dispose-safe access to [Widget].
 *
 * @return This [Widget] if it is not disposed, otherwise returns `null`.
 */
fun <T : Widget> T.orNull() = if (!isDisposed) this else null