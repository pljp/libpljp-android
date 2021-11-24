package jp.programminglife.libpljp.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UuidTest {

    @Test
    fun uuidRepositoryTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UuidGenerator.PreferencesUuidRepository(context).saveNodeId(987654321L)
        val node = UuidGenerator.PreferencesUuidRepository(context).loadNodeId()
        assertEquals(987654321L, node)
    }


    @Test
    fun clockSequenceOverflow() {
        val clockSeq = UuidGenerator.ClockSequence(0, 0x3f00)

        assertEquals("最初の値", 0x3f00, clockSeq.get())
        for (i in 1..0x3fff) {
            assertNotNull("${i.toString(16)}番目がnull", clockSeq.incrementAndGet())
        }
        assertNull("オーバフーロー後のincrementAndGet()がnull以外", clockSeq.incrementAndGet())
    }


    @Test
    fun basicUuidGeneration() {
        val generator = UuidGenerator(ReadOnlyUuidRepository())
        val time = 1_000_000_000_000L
        val nano = 999_1234_00L
        val uuid = generator.generate(time, nano)
        assertEquals(time, generator.epochMilli(uuid))
        assertEquals(1234L, uuid.timestamp() % 10000L)
        assertEquals(123456789L, uuid.node())
        assertEquals(1, uuid.version())
        assertEquals(2, uuid.variant())
    }


    @Test
    fun multipleUuidGenerationAtTheSameTime() {
        val generator = UuidGenerator(ReadOnlyUuidRepository())
        val time = 1_000_000_000_000L
        val nano = 0L
        val uuid1 = generator.generate(time, nano)
        val uuid2 = generator.generate(time, nano)
        assertNotEquals(uuid1, uuid2)
        assertEquals(uuid1.timestamp(), uuid2.timestamp())
        assertEquals(uuid1.node(), uuid2.node())
        assertNotEquals(uuid1.clockSequence(), uuid2.clockSequence())
    }


    @Test
    fun multipleUuidGenerationAtDifferentTimes() {
        val generator = UuidGenerator(ReadOnlyUuidRepository())
        val uuid1 = generator.generate(1_000_000_000_000L, 0L)
        val uuid2 = generator.generate(1_000_000_000_000L, 100L)
        assertNotEquals(uuid1, uuid2)
        assertNotEquals(uuid1.timestamp(), uuid2.timestamp())
        assertEquals(uuid1.node(), uuid2.node())
        assertEquals(uuid1.clockSequence(), uuid2.clockSequence())
    }


    @Test
    fun nodeIdStringFormat() {
        val generator = UuidGenerator(ReadOnlyUuidRepository(0x123456789AL))
        val uuid = generator.generate()
        assertEquals("00123456789A", uuid.nodeIdString)
    }


    private class ReadOnlyUuidRepository(private val nodeId: Long = 123456789L) : UuidGenerator.UuidRepository {
        override fun loadNodeId(): Long = nodeId
        override fun saveNodeId(nodeId: Long?) {}
    }

}