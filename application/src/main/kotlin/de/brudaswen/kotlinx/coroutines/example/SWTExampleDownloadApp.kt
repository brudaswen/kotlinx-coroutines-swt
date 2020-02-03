package de.brudaswen.kotlinx.coroutines.example

import kotlinx.coroutines.*
import kotlinx.coroutines.swt.swt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.ProgressBar
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Update download progress every x milliseconds. */
private const val DOWNLOAD_DELAY = 250L

/**
 * SWT example that starts a long running (download) operation as a coroutine.
 *
 * Dispatches UI updates via [Dispatchers.swt] for a non-default Display.
 */
fun main() {
    // Initialize default Display in another thread
    thread {
        Display.getDefault()
    }.join()

    // Create and show GUI using a custom Display
    val display = Display()
    val gui = Gui(display)
    gui.open()

    // Execute long running background operation
    GlobalScope.launch(Dispatchers.IO) {
        expensiveDownload(gui)
    }

    // Dispatch events until SWT window is closed
    while (!gui.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    display.dispose()

    println("Exiting...")
    Thread.sleep(1_000)
}

/**
 * Execute long running operation and show progress in [gui] window.
 *
 * @param gui The [Gui] that will get updated.
 */
private suspend fun expensiveDownload(gui: Gui) {
    for (i in 0..50) {
        val percent = i * 2
        gui.updateProgress(percent)
        delay(DOWNLOAD_DELAY)
    }
    gui.close()
}

/**
 * Allow to make changes to the GUI window.
 *
 * All UI updates are executed within SWT event dispatch thread.
 */
private class Gui(display: Display) : CoroutineScope {

    private val shell = Shell(display).apply {
        text = "Kotlin Coroutines SWT example"
        layout = FillLayout().apply {
            type = SWT.VERTICAL
            marginWidth = 10
            marginHeight = 10
            spacing = 5
            setMinimumSize(400, 0)
        }
    }

    override val coroutineContext = Dispatchers.swt(shell.display) + SupervisorJob()

    val isDisposed
        get() = shell.isDisposed

    private val progressBar = ProgressBar(shell, SWT.SMOOTH).apply {
        minimum = 0
        maximum = 100
    }

    private val progressText = Text(shell, SWT.SINGLE or SWT.CENTER).apply {
        editable = false
        enabled = false
    }

    /** Open GUI window.*/
    fun open() = launchIfNotDisposed {
        shell.pack()
        shell.open()
    }

    /** Close GUI window.*/
    fun close() = launchIfNotDisposed {
        shell.close()
    }

    /** Update progress information.*/
    fun updateProgress(percent: Int) = launchIfNotDisposed {
        println("Updating progress: $percent")
        progressText.text = "$percent %"
        progressBar.selection = percent
        shell.layout(true, true)
    }

    private fun launchIfNotDisposed(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) = launch(context, start) {
        if (!shell.isDisposed) {
            block()
        }
    }
}
