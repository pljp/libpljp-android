package jp.programminglife.libpljp.android.uuidgenerator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.programminglife.libpljp.android.Logger.Companion.get
import jp.programminglife.libpljp.android.UUIDUtils
import kotlinx.android.synthetic.main.main_activity_a.*

class MainActivity : AppCompatActivity() {
    private val log = get(javaClass)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_a)

        a_generate_button.setOnClickListener {
            val n = kotlin.runCatching { a_num_text.text.toString().toInt() }
                    .onFailure { log.d(it, "数のパースエラー") }
                    .getOrDefault(1)
            val nodeIdStr = a_node_id_text.text.toString()
            val nodeId = if (nodeIdStr.isEmpty()) {
                UUIDUtils.getDeviceNodeId(this@MainActivity)
            }
            else {
                kotlin.runCatching { nodeIdStr.toLong(16) }
                        .onFailure { log.d(it, "ノードIDのパースエラー") }
                        .getOrDefault(0)
            }
            val uuidStr = buildString {
                (0 until n).joinTo(buffer = this, separator = "\n", transform = {
                    UUIDUtils.generate(nodeId, System.currentTimeMillis()).toString()
                })
            }
            a_uuid_text.setText(uuidStr)
        }
    }
}
