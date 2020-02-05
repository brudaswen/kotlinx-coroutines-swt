package kotlinx.coroutines.swt

import kotlinx.coroutines.*
import org.eclipse.swt.widgets.Display
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SwtTest : TestBase() {

    private val display
        get() = DISPATCHER.display

    @BeforeTest
    fun setup() {
        ignoreLostThreads(DISPATCHER.name)
    }

    @Test
    fun testDelay() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        display.asyncExec { expect(2) }
        val job = launch(Dispatchers.SWT) {
            check(display.isEventDispatchThread())
            expect(3)
            display.asyncExec { expect(4) }
            delay(100)
            check(display.isEventDispatchThread())
            expect(5)
        }

        job.join()
        finish(6)
    }

    @Test
    fun testCancel() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val job = launch(Dispatchers.SWT) {
            check(display.isEventDispatchThread())
            expect(2)
            cancel()
            yield()
            expectUnreached()
        }

        job.join()
        finish(3)
    }

    @Test
    fun testCancel2() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val firstJob = launch(Dispatchers.SWT) {
            check(display.isEventDispatchThread())
            expect(2)
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(100)
            expect(3)
        }

        val secondJob = launch(Dispatchers.SWT) {
            expectUnreached()
        }
        secondJob.cancelAndJoin()

        val thirdJob = launch(Dispatchers.SWT) {
            expect(4)
        }

        firstJob.join()
        thirdJob.join()
        finish(5)
    }

    @Test
    fun testLaunchInMainScope() = runTest {
        check(!display.isEventDispatchThread())

        val component = SwtComponent(display)
        val job = component.testLaunch()

        job.join()

        assertTrue(component.executed)
        component.cancel()
        component.coroutineContext[Job]!!.join()
    }

    @Test
    fun testFailureInMainScope() = runTest {
        check(!display.isEventDispatchThread())

        var exception: Throwable? = null
        val component = SwtComponent(
            display,
            CoroutineExceptionHandler { _, e -> exception = e })
        val job = component.testFailure()

        job.join()

        assertTrue(exception is TestException)
        component.cancel()
        join(component)
    }

    @Test
    fun testCancellationInMainScope() = runTest {
        check(!display.isEventDispatchThread())

        val component = SwtComponent(display)
        component.cancel()
        component.testCancellation().join()
        join(component)
    }

    @Test
    fun testImmediateDispatcherYield() = runBlocking(Dispatchers.SWT) {
        expect(1)
        // launch in the immediate dispatcher
        launch(Dispatchers.SWT.immediate) {
            expect(2)
            yield()
            expect(4)
        }
        expect(3) // after yield
        yield() // yield back
        finish(5)
    }

    private suspend fun join(component: SwtComponent) {
        component.coroutineContext[Job]!!.join()
    }

    private class SwtComponent(
        private val display: Display,
        coroutineContext: CoroutineContext = EmptyCoroutineContext
    ) : CoroutineScope by MainScope() + coroutineContext {

        var executed = false

        fun testLaunch(): Job = launch {
            check(display.isEventDispatchThread())
            executed = true
        }

        fun testFailure(): Job = launch {
            check(display.isEventDispatchThread())
            throw TestException()
        }

        fun testCancellation(): Job = launch(start = CoroutineStart.ATOMIC) {
            check(display.isEventDispatchThread())
            delay(Long.MAX_VALUE)
        }
    }

    companion object {

        private lateinit var DISPATCHER: SWTDefaultDisplayDispatchThread

        @BeforeAll
        @JvmStatic
        fun init() {
            DISPATCHER = SWTDefaultDisplayDispatchThread()
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            DISPATCHER.dispose()
        }
    }
}

private fun Display.isEventDispatchThread() = thread == Thread.currentThread()
