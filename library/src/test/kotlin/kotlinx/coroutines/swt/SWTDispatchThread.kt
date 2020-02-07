package kotlinx.coroutines.swt

import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

/**
 * Dispatcher thread for the SWT default [Display].
 *
 * Creates [Display] by executing [createDisplay] in a new thread.
 * The new thread handles all dispatched events for the default [Display].
 *
 * @param createDisplay The method to create a new [Display] (default: `Display()`).
 */
internal class SWTDispatchThread(private val createDisplay: () -> Display = { Display() }) : Closeable {

    /** The name of the thread. */
    val name = "SWTDefaultDisplayDispatchThread"

    private var thread: Thread

    /** Get [Display] of this dispatcher thread */
    val display: Display

    /** Get [Shell] of this dispatcher thread */
    val shell: Shell

    init {
        // Start new thread and let it initialize Display and Shell
        val future = CompletableFuture<Shell>()
        thread = thread(name = name) {
            try {
                val display = createDisplay()
                val shell = Shell(display)
                future.complete(shell)

                // Start dispatch loop
                while (!display.isDisposed) {
                    if (!display.readAndDispatch()) {
                        display.sleep()
                    }
                }
                display.dispose()
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }

        // Wait until Shell is ready
        shell = future.get()
        display = shell.display
    }

    /** Dispose the display and stop the event dispatcher thread. */
    override fun close() {
        if (!display.isDisposed) {
            display.syncExec {
                display.dispose()
            }

            thread.join()
        }
    }
}
