package de.brudaswen.kotlinx.coroutines.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Update time every x milliseconds. */
private const val TIME_UPDATE_DELAY = 50L

/**
 * SWT example that periodically updates the current time.
 *
 * Dispatches UI updates via [Dispatchers.Main] for the default Display.
 */
fun main() {
    // Create and show GUI
    val display = Display.getDefault()
    val shell = Shell(display).apply {
        text = "Kotlin Coroutines SWT example"
        layout = FillLayout().apply {
            type = SWT.VERTICAL
            marginWidth = 10
            marginHeight = 10
            spacing = 5
            setMinimumSize(400, 0)
        }
    }

    val label = Label(shell, SWT.SINGLE or SWT.CENTER)

    shell.pack()
    shell.open()

    // Start a coroutines using [Dispatchers.Main] to update time.
    // This works since timeText widget is part of the default display.
    GlobalScope.launch(Dispatchers.Main) {
        val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        while (!shell.isDisposed) {
            println("Updating time")
            label.text = dateFormat.format(LocalTime.now())
            delay(TIME_UPDATE_DELAY)
        }
    }

    // Dispatch events until SWT window is closed
    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    display.dispose()
}
