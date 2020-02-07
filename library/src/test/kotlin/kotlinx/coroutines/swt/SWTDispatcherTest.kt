package kotlinx.coroutines.swt

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import org.eclipse.swt.SWTException
import org.eclipse.swt.widgets.Display
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*

@UseExperimental(ExperimentalCoroutinesApi::class)
class SWTDispatcherTest : TestBase() {

    private val display
        get() = DISPATCHER.display

    private val widget
        get() = DISPATCHER.shell

    @BeforeTest
    fun setup() {
        ignoreLostThreads(DISPATCHER.name)
    }

    @Nested
    @DisplayName("SWTDispatchers")
    inner class SWTDispatchers {

        @Test
        @DisplayName("Dispatchers.Main should execute in correct SWT thread")
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

        @Test
        @DisplayName("Dispatchers.SWT should execute in correct SWT thread")
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

        @Test
        @DisplayName("Dispatchers.swt(Display) should execute in correct SWT thread")
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

        @Test
        @DisplayName("Dispatchers.swt(Widget) should execute in correct SWT thread")
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
    }

    @Nested
    @DisplayName("SWTDispatcher")
    inner class SWTDispatcher {
        @Test
        @DisplayName("delay() should cause re-scheduling")
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
        @DisplayName("cancel() should stop further execution")
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
        @DisplayName("Job.cancel() should ensure that job is not executed at all")
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
        @DisplayName("Dispatchers.SWT should have a meaningful name")
        fun testSWTDispatcherToStringContainsSWT() {
            assertContains("SWT", Dispatchers.SWT.toString())
        }
    }

    @Nested
    @DisplayName("MainScope")
    inner class MainScope {

        @Test
        @DisplayName("using MainScope() should execute in correct SWT thread")
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
        @DisplayName("failure in MainScope() should be handled correctly")
        fun testFailureInMainScope() = runTest {
            check(!display.isEventDispatchThread())

            var exception: Throwable? = null
            val component = SwtComponent(
                display,
                CoroutineExceptionHandler { _, e -> exception = e }
            )
            val job = component.testFailure()

            job.join()

            assertTrue(exception is TestException)
            component.cancel()
            component.join()
        }

        @Test
        @DisplayName("cancellation in MainScope() should be handled correctly and not block")
        fun testCancellationInMainScope() = runTest {
            check(!display.isEventDispatchThread())

            val component = SwtComponent(display)
            component.cancel()
            component.testCancellation().join()
            component.join()
        }
    }

    @Nested
    @DisplayName("ImmediateDispatcher")
    inner class ImmediateDispatcher {

        @Test
        @DisplayName("yield() in Dispatchers.SWT.immediate should trigger re-scheduling")
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

        @Test
        @DisplayName("Dispatchers.SWT.immediate should not create a new instance and instead return itself")
        fun testImmediateDispatcherShouldReturnSameInstance() {
            val immediateDispatcher = Dispatchers.SWT.immediate
            assertSame(immediateDispatcher, immediateDispatcher.immediate)
        }

        @Test
        @DisplayName("Dispatchers.SWT.immediate should have a meaningful name")
        fun testImmediateSWTDispatcherToStringContainsSWTAndImmediate() {
            val immediateDispatcher = Dispatchers.SWT.immediate
            assertContains("SWT", immediateDispatcher.toString())
            assertContains("immediate", immediateDispatcher.toString(), ignoreCase = true)
        }
    }


    @Nested
    @DisplayName("OnTimeout")
    inner class OnTimeout {

        @Test
        @DisplayName("select.onTimeout should be executed in correct SWT thread")
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

        @Test
        @DisplayName("select.onTimeout should not be executed if there is no timeout")
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
    }

    @Nested
    @DisplayName("Secondary display")
    inner class SecondaryDisplay {

        @Test
        @DisplayName("Dispatchers.swt(Display) should be executed in correct Display thread")
        fun testLaunchInSecondaryDisplay() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    val job = launch(Dispatchers.swt(display)) {
                        check(display.isEventDispatchThread())
                        expect(2)
                    }

                    job.join()
                    finish(3)
                }
            }
        }

        @Test
        @DisplayName("Dispatchers.swt(Widget) should be executed in correct Display thread")
        fun testLaunchInSecondaryDisplayWithWidget() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    val job = launch(Dispatchers.swt(dispatcher.shell)) {
                        check(display.isEventDispatchThread())
                        expect(2)
                    }

                    job.join()
                    finish(3)
                }
            }
        }

        @Test
        @DisplayName("Test that launch on closed Display fails")
        fun testLaunchOnClosedDisplay() = runTest(expected = { it is SWTException }) {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display

                check(!display.isEventDispatchThread())
                expect(1)

                dispatcher.close()
                val job = launch(Dispatchers.swt(display)) {
                    expectUnreached()
                }
                finish(2)
                job.join() // Should throw SWTException
            }
        }
    }

    @Nested
    @DisplayName("LaunchDisplayExtensions")
    inner class LaunchDisplayExtensions {

        @Test
        @DisplayName("launch(Display) should execute in correct SWT thread")
        fun testLaunchWithDisplay() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    val job = launch(display) {
                        check(display.isEventDispatchThread())
                        expect(2)
                    }
                    assertNotNull(job)

                    job.join()
                    finish(3)
                }
            }
        }

        @Test
        @DisplayName("launch(Display) on disposed display should not throw exception")
        fun testLaunchWithDisposedDisplay() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    dispatcher.close()
                    val job = launch(display) {
                        expectUnreached()
                    }
                    assertNull(job)

                    finish(2)
                }
            }
        }
    }

    @Nested
    @DisplayName("LaunchWidgetExtensions")
    inner class LaunchWidgetExtensions {

        @Test
        @DisplayName("launch(Widget) should execute in correct SWT thread")
        fun testLaunchWithDisplay() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                val widget = dispatcher.shell
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    val job = launch(widget) {
                        check(display.isEventDispatchThread())
                        expect(2)
                    }
                    assertNotNull(job)

                    job.join()
                    finish(3)
                }
            }
        }

        @Test
        @DisplayName("launch(Widget) on disposed display should not throw exception")
        fun testLaunchWithDisposedDisplay() {
            SWTDispatchThread().use { dispatcher ->
                val display = dispatcher.display
                val widget = dispatcher.shell
                runBlocking {
                    check(!display.isEventDispatchThread())

                    expect(1)
                    dispatcher.close()
                    val job = launch(widget) {
                        expectUnreached()
                    }
                    assertNull(job)

                    finish(2)
                }
            }
        }
    }

    @Nested
    @DisplayName("WidgetOrNullExtension")
    inner class WidgetOrNullExtension {

        @Test
        @DisplayName("Widget.orNull() should return Widget if not disposed")
        fun testWidgetNotDisposed() {
            SWTDispatchThread().use { dispatcher ->
                assertNotNull(dispatcher.shell.orNull())
            }
        }

        @Test
        @DisplayName("Widget.orNull() should return null if disposed")
        fun testWidgetDisposed() {
            SWTDispatchThread().use { dispatcher ->
                dispatcher.close()
                assertNull(dispatcher.shell.orNull())
            }
        }
    }

    companion object {

        private lateinit var DISPATCHER: SWTDispatchThread

        @BeforeAll
        @JvmStatic
        fun init() {
            DISPATCHER = SWTDispatchThread { Display.getDefault() }
        }

        @AfterAll
        @JvmStatic
        fun cleanup() {
            DISPATCHER.close()
        }
    }
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

private fun assertContains(expected: String, value: String, ignoreCase: Boolean = false) =
    assertTrue(value.contains(expected, ignoreCase), "Value '$value' did not contain '$expected'")

private suspend fun SwtComponent.join() = coroutineContext[Job]!!.join()

private fun Display.isEventDispatchThread() = thread == Thread.currentThread()
