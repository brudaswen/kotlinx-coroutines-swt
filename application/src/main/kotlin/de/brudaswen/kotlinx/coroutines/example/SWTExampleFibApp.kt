package de.brudaswen.kotlinx.coroutines.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swt.SWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.*
import java.math.BigInteger
import java.text.NumberFormat
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * SWT example that starts a long running computation in a new thread.
 *
 * Dispatches UI updates via [Dispatchers.SWT] for the default Display.
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
            setMinimumSize(400, 300)
        }
    }

    val table = Table(shell, SWT.BORDER).apply {
        headerVisible = true
    }
    TableColumn(table, SWT.NULL).apply {
        text = "i"
    }
    TableColumn(table, SWT.NULL).apply {
        text = "fib(i)"
    }

    shell.pack()
    shell.open()

    // Start calculation in new thread and update UI using [Dispatchers.SWT].
    val cancelled = AtomicBoolean(false)
    thread {
        try {
            var i = 0
            while (!cancelled.get()) {
                val n = i++

                // Calculate next Fibonacci number
                println("Calculating fib($n)")
                val result = fib(BigInteger.valueOf(n.toLong()), cancelled)

                // Add a new row to table
                GlobalScope.launch(Dispatchers.SWT) {
                    if (!shell.isDisposed) {
                        TableItem(table, SWT.NULL, 0).apply {
                            setText(0, n.toString())
                            setText(1, NumberFormat.getNumberInstance().format(result))
                        }
                        table.getColumn(0).pack()
                        table.getColumn(1).pack()
                    }
                }
            }
        } catch (e: CancellationException) {
            println("Calculation cancelled.")
        }
    }

    // Stop calculation when closing window
    shell.addListener(SWT.Close) {
        cancelled.set(true)
    }

    // Dispatch events until SWT window is closed
    while (!shell.isDisposed) {
        if (!display.readAndDispatch()) {
            display.sleep()
        }
    }
    display.dispose()

    println("Exiting...")
}

private fun fib(n: BigInteger, cancelled: AtomicBoolean): BigInteger =
    when {
        cancelled.get() -> throw CancellationException()
        n < BigInteger.TWO -> BigInteger.ONE
        else -> fib(n - BigInteger.ONE, cancelled) + fib(n - BigInteger.TWO, cancelled)
    }