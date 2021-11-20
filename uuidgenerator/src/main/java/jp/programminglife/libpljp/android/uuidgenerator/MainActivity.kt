package jp.programminglife.libpljp.android.uuidgenerator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.programminglife.libpljp.android.Logger.Companion.get
import jp.programminglife.libpljp.android.UuidGenerator
import kotlinx.android.synthetic.main.main_activity_a.*

class MainActivity : AppCompatActivity() {
    private val log = get(javaClass)
    private lateinit var repository: UuidGenerator.UuidRepository
    private lateinit var uuidGenerator: UuidGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_a)
        repository = UuidGenerator.PreferencesUuidRepository(this)
        uuidGenerator = UuidGenerator(repository)

        a_generate_button.setOnClickListener {
            val n = kotlin.runCatching { a_num_text.text.toString().toInt() }
                    .onFailure { log.d(it, "数のパースエラー") }
                    .getOrDefault(1)
            val nodeIdStr = a_node_id_text.text.toString()
            val nodeId = if (nodeIdStr.isEmpty()) {
                val tmpUuid = uuidGenerator.generate(System.currentTimeMillis(), System.nanoTime())
                tmpUuid.node()
            }
            else {
                kotlin.runCatching { nodeIdStr.toLong(16) }
                        .onFailure { log.d(it, "ノードIDのパースエラー") }
                        .getOrDefault(0)
            }
            val uuidStr = buildString {
                (0 until n).joinTo(buffer = this, separator = "\n", transform = {
                    uuidGenerator.generate(System.currentTimeMillis(), System.nanoTime()).toString()
                })
            }
            a_uuid_text.setText(uuidStr)
        }
    }
}
