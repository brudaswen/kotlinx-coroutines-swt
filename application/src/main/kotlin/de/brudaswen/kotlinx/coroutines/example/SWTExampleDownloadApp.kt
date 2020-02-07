package de.brudaswen.kotlinx.coroutines.example

import kotlinx.coroutines.*
import kotlinx.coroutines.swt.launch
import kotlinx.coroutines.swt.orNull
import kotlinx.coroutines.swt.swt
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.ProgressBar
import org.eclipse.swt.widgets.Shell
import kotlin.concurrent.thread

/** Update download progress every x milliseconds. */
private const val DOWNLOAD_DELAY = 250L

/**
 * SWT example that starts a long running (download) operation as a coroutine.
 *
 * Dispatches UI updates via [Dispatchers.swt] for a non-default Display.
 */
fun main() {
    // Initialize default Display in another thread
    thread { Display.getDefault() }.join()

    // Create and show GUI using a custom Display
    val display = Display()
    val gui = Gui(display)
    gui.open()

    // Execute long running background operation
    GlobalScope.launch(Dispatchers.IO) {
        expensiveDownload(gui)
    }

    // Dispatch events until SWT window is closed
    while (!display.isDisposed) {
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
        addListener(SWT.Close) {
            display.dispose()
        }
    }

    override val coroutineContext = Dispatchers.swt(display) + SupervisorJob()

    private val progressBar = ProgressBar(shell, SWT.SMOOTH).apply {
        minimum = 0
        maximum = 100
    }

    private val progressLabel = Label(shell, SWT.SINGLE or SWT.CENTER)

    /** Open GUI window.*/
    fun open() = launch(shell) {
        shell.orNull()?.pack()
        shell.orNull()?.open()
    }

    /** Close GUI window.*/
    fun close() = launch(shell) {
        shell.orNull()?.close()
    }

    /** Update progress information.*/
    fun updateProgress(percent: Int) = launch(shell) {
        println("Updating progress: $percent")
        progressLabel.orNull()?.text = "$percent %"
        progressBar.orNull()?.selection = percent
        shell.orNull()?.layout(true, true)
    }
}
