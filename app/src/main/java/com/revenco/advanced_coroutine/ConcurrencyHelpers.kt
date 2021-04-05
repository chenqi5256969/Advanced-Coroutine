import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicReference
import kotlin.DeprecationLevel.ERROR

/**
 * 在协程中按顺序执行任务的帮助类.
 *
 * [afterPrevious] 这个方法可以确保任务块在这个方法调用之前执行
 */
class SingleRunner {
    /**
     * 相当于java里面的lock
     */
    private val mutex = Mutex()

    /**
     * 确保只在之前的所有工作完成后才执行
     *
     * When several coroutines call afterPrevious at the same time, they will queue up in the order
     * that they call afterPrevious. Then, one coroutine will enter the block at a time.
     * 当多个协程同时调用afterPrevious时，它们将按照调用afterPrevious的顺序排队。然后，一个协程将一次进入块
     *
     *
     * In the following example, only one save operation (user or song) will be executing at a time.
     * 在下面的例子中，一次只执行一个保存操作(用户或歌曲)
     *
     *
     * ```
     * class UserAndSongSaver {
     *    val singleRunner = SingleRunner()
     *
     *    fun saveUser(user: User) {
     *        singleRunner.afterPrevious { api.post(user) }
     *    }
     *
     *    fun saveSong(song: Song) {
     *        singleRunner.afterPrevious { api.post(song) }
     *    }
     * }
     * ```
     *
     * @param block the code to run after previous work is complete.
     */
    suspend fun <T> afterPrevious(block: suspend () -> T): T {
        // 在运行块之前，通过对块进行锁，确保没有其他块正在运行 互斥锁
        //当我们到达这里时，如果其他任何块已经在运行，它将等待它完成
        //返回时互斥锁将自动释放
        mutex.withLock {
            return block()
        }
    }
}

/**
 * A controlled runner decides what to do when new tasks are run.
 *
 * By calling [joinPreviousOrRun], the new task will be discarded and the result of the previous task
 * will be returned. This is useful when you want to ensure that a network request to the same
 * resource does not flood.
 *
 * By calling [cancelPreviousThenRun], the old task will *always* be cancelled and then the new task will
 * be run. This is useful in situations where a new event implies that the previous work is no
 * longer relevant such as sorting or filtering a list.
 */
class ControlledRunner<T> {
    /**
     * The currently active task.
     *
     * This uses an atomic reference to ensure that it's safe to update activeTask on both
     * Dispatchers.Default and Dispatchers.Main which will execute coroutines on multiple threads at
     * the same time.
     */
    private val activeTask = AtomicReference<Deferred<T>?>(null)

    /**
     * Cancel all previous tasks before calling block.
     *
     * When several coroutines call cancelPreviousThenRun at the same time, only one will run and
     * the others will be cancelled.
     *
     * In the following example, only one sort operation will execute and any previous sorts will be
     * cancelled.
     *
     * ```
     * class Products {
     *    val controlledRunner = ControlledRunner<Product>()
     *
     *    fun sortAscending(): List<Product> {
     *        return controlledRunner.cancelPreviousThenRun { dao.loadSortedAscending() }
     *    }
     *
     *    fun sortDescending(): List<Product> {
     *        return controlledRunner.cancelPreviousThenRun { dao.loadSortedDescending() }
     *    }
     * }
     * ```
     *
     * @param block the code to run after previous work is cancelled.
     * @return the result of block, if this call was not cancelled prior to returning.
     */
    suspend fun cancelPreviousThenRun(block: suspend () -> T): T {
        //首先将比对的任务置为null
        activeTask.get()?.cancelAndJoin()
        LogUtils.i("newTask---$activeTask")
        return coroutineScope {

            val newTask = async(start = LAZY) {
                block()
            }

            newTask.invokeOnCompletion {
                LogUtils.i("newTask---invokeOnCompletion")
                activeTask.compareAndSet(newTask, null)
            }

            val result: T

            while (true) {
                if (!activeTask.compareAndSet(null, newTask)) {
                    LogUtils.i("newTask---newTask")
                    activeTask.get()?.cancelAndJoin()
                    yield()
                } else {
                    LogUtils.i("newTask---null")
                    result = newTask.await()
                    break
                }
            }
            result
        }
    }

    /**
     * Don't run the new block if a previous block is running, instead wait for the previous block
     * and return it's result.
     *
     * When several coroutines call jonPreviousOrRun at the same time, only one will run and
     * the others will return the result from the winner.
     *
     * In the following example, only one network operation will execute at a time and any other
     * requests will return the result from the "in flight" request.
     *
     * ```
     * class Products {
     *    val controlledRunner = ControlledRunner<Product>()
     *
     *    fun fetchProducts(): List<Product> {
     *        return controlledRunner.joinPreviousOrRun {
     *            val results = api.fetchProducts()
     *            dao.insert(results)
     *            results
     *        }
     *    }
     * }
     * ```
     *
     * @param block the code to run if and only if no other task is currently running
     * @return the result of block, or if another task was running the result of that task instead.
     */
    suspend fun joinPreviousOrRun(block: suspend () -> T): T {
        activeTask.get()?.let {
            return it.await()
        }
        return coroutineScope {
            val newTask = async(start = LAZY) {
                block()
            }

            newTask.invokeOnCompletion {
                LogUtils.i("newTask--after--$activeTask")
                activeTask.compareAndSet(newTask, null)
            }

            val result: T

            while (true) {
                LogUtils.i("newTask--$activeTask")
                if (!activeTask.compareAndSet(null, newTask)) {
                    LogUtils.i("newTask--middle")
                    val currentTask = activeTask.get()
                    if (currentTask != null) {
                        newTask.cancel()
                        result = currentTask.await()
                        break
                    } else {
                        yield()
                    }
                } else {
                    result = newTask.await()
                    break
                }
            }
            result
        }
    }
}