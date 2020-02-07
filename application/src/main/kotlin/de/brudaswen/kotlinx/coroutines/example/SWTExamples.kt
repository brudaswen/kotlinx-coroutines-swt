package de.brudaswen.kotlinx.coroutines.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swt.SWT
import kotlinx.coroutines.swt.launch
import kotlinx.coroutines.swt.orNull
import kotlinx.coroutines.swt.swt
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import kotlin.concurrent.thread

fun main() {
    // Create UI in some thread
    val display = Display.getDefault()
    val shell = Shell(display).apply {
        addListener(SWT.Close) {
            display.dispose()
        }
        open()
    }
    val label = Label(shell, SWT.NULL).apply {
        text = "Init!"
        font = Font(display, font.fontData.apply { forEach { it.height = 48f } })
        pack()
    }

    updateUiInNewThread(display, label)

    while (!display.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
}

fun updateUiInNewThread(display: Display, label: Label) = thread {
    // Dispatch to default display (via [Dispatchers.Main])
    Thread.sleep(1000)
    GlobalScope.launch(Dispatchers.Main) {
        label.text = "launch(Dispatchers.Main)"
        label.pack()
    }

    // Dispatch to default display (via [Dispatchers.SWT])
    Thread.sleep(1000)
    GlobalScope.launch(Dispatchers.SWT) {
        label.text = "launch(Dispatchers.SWT)"
        label.pack()
    }

    // Dispatch to given display
    Thread.sleep(1000)
    GlobalScope.launch(Dispatchers.swt(display)) {
        label.text = "launch(Dispatchers.swt(display))"
        label.pack()
    }

    // Dispatch to display of widget
    Thread.sleep(1000)
    GlobalScope.launch(Dispatchers.swt(label)) {
        label.text = "launch(Dispatchers.swt(label))"
        label.pack()
    }

    // Dispatch via Display extension
    Thread.sleep(1000)
    GlobalScope.launch(display) {
        label.orNull()?.text = "launch(display)"
        label.orNull()?.pack()
    }

    // Dispatch via Widget extension
    Thread.sleep(1000)
    GlobalScope.launch(label) {
        label.orNull()?.text = "launch(label)"
        label.orNull()?.pack()
    }
}
