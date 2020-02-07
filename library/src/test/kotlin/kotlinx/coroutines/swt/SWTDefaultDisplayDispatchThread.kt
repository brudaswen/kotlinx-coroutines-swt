package kotlinx.coroutines.swt

import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

/**
 * Dispatcher thread for the SWT default [Display].
 *
 * Creates [Display.getDefault] in a new thread.
 * The new thread handles all dispatched events for the default [Display].
 */
internal class SWTDefaultDisplayDispatchThread {

    /** The name of the thread. */
    val name = "SWTDefaultDisplayDispatchThread"

    private var thread: Thread

    /** Get [Shell] of this dispatcher thread */
    val shell: Shell

    /** Get [Display] of this dispatcher thread */
    val display: Display
        get() = shell.display

    init {
        // Start new thread and let it initialize Display and Shell
        val future = CompletableFuture<Shell>()
        thread = thread(name = name) {
            try {
                val display = Display.getDefault()
                val shell = Shell(display)
                future.complete(shell)

                // Start dispatch loop
                while (!shell.isDisposed) {
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
    }

    /** Dispose the display and stop the event dispatcher thread. */
    fun dispose() {
        if (!display.isDisposed) {
            display.syncExec {
                shell.close()
            }

            thread.join()
        }
    }
}
