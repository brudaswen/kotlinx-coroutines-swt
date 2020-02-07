package kotlinx.coroutines.swt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import org.eclipse.swt.widgets.Display
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

@UseExperimental(ExperimentalCoroutinesApi::class)
class SwtTest : TestBase() {

    private val display
        get() = DISPATCHER.display

    private val widget
        get() = DISPATCHER.shell

    @BeforeTest
    fun setup() {
        ignoreLostThreads(DISPATCHER.name)
    }

    /**
     * Test that [Dispatchers.Main] executes in correct SWT thread.
     */
    @Test
    fun testDispatcherMain() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val job = launch(Dispatchers.Main) {
            check(display.isEventDispatchThread())
            expect(2)
        }

        job.join()
        finish(3)
    }

    /**
     * Test that [Dispatchers.SWT] executes in correct SWT thread.
     */
    @Test
    fun testDispatcherSWT() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val job = launch(Dispatchers.SWT) {
            check(display.isEventDispatchThread())
            expect(2)
        }

        job.join()
        finish(3)
    }

    /**
     * Test that [Dispatchers.swt] with [Display] executes in correct SWT thread.
     */
    @Test
    fun testDispatcherForDisplay() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val job = launch(Dispatchers.swt(display)) {
            check(display.isEventDispatchThread())
            expect(2)
        }

        job.join()
        finish(3)
    }

    /**
     * Test that [Dispatchers.swt] with [org.eclipse.swt.widgets.Widget]
     * executes in correct SWT thread.
     */
    @Test
    fun testDispatcherForWidget() = runBlocking {
        check(!display.isEventDispatchThread())

        expect(1)
        val job = launch(Dispatchers.swt(widget)) {
            check(display.isEventDispatchThread())
            expect(2)
        }

        job.join()
        finish(3)
    }

    /**
     * Test that [delay] causes re-scheduling.
     */
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

    /**
     * Test that [cancel] stops further execution.
     */
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

    /**
     * Test that a new [Job] that is cancelled before it is able
     * to get executed is not executed at all.
     */
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

    /**
     * Test execution in [MainScope].
     */
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

    /**
     * Test failure in [MainScope].
     */
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

    /**
     * Test cancellation in [MainScope].
     */
    @Test
    fun testCancellationInMainScope() = runTest {
        check(!display.isEventDispatchThread())

        val component = SwtComponent(display)
        component.cancel()
        component.testCancellation().join()
        join(component)
    }

    /**
     * Test execution with immediate [Dispatchers.SWT].
     */
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

    /**
     * Test that [kotlinx.coroutines.selects.SelectBuilder.onTimeout]
     * is executed in correct SWT thread.
     */
    @Test
    fun testOnTimeoutIsExecutedInSWTThread() = runBlocking(Dispatchers.SWT) {
        expect(1)
        val channel = produce { delay(50); send(Unit) }
        select<Unit> {
            channel.onReceive {
                expectUnreached()
            }
            onTimeout(10) {
                check(display.isEventDispatchThread())
                expect(2)
            }
        }

        delay(100)
        finish(3)

        channel.cancel()
    }

    /**
     * Test that [kotlinx.coroutines.selects.SelectBuilder.onTimeout] is not executed
     * if [select] does not run into timeout.
     */
    @Test
    fun testOnTimeoutIsNotExecuted() = runBlocking(Dispatchers.SWT) {
        expect(1)
        val channel = produce { send(Unit) }
        select<Unit> {
            channel.onReceive {
                check(display.isEventDispatchThread())
                expect(2)
            }
            onTimeout(50) {
                expectUnreached()
            }
        }

        // Wait to ensure that onTimeout is not executed
        delay(100)
        finish(3)
    }

    /**
     * The immediate [Dispatchers.SWT] should not create a new instance and instead return itself.
     */
    @Test
    fun testImmediateDispatcherShouldReturnSameInstance() {
        val immediateDispatcher = Dispatchers.SWT.immediate
        assertSame(immediateDispatcher, immediateDispatcher.immediate)
    }

    /**
     * Ensure that [Dispatchers.SWT] has a meaningful name.
     */
    @Test
    fun testSWTDispatcherToStringContainsSWT() {
        assertContains("SWT", Dispatchers.SWT.toString())
    }

    /**
     * Ensure that immediate [Dispatchers.SWT] has a meaningful name.
     */
    @Test
    fun testImmediateSWTDispatcherToStringContainsSWTAndImmediate() {
        val immediateDispatcher = Dispatchers.SWT.immediate
        assertContains("SWT", immediateDispatcher.toString())
        assertContains("immediate", immediateDispatcher.toString(), ignoreCase = true)
    }

    private suspend fun join(component: SwtComponent) {
        component.coroutineContext[Job]!!.join()
    }

    private fun assertContains(expected: String, value: String, ignoreCase: Boolean = false) =
        assertTrue(value.contains(expected, ignoreCase), "Value '$value' did not contain '$expected'")

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
