package jp.programminglife.libpljp.android.uuidgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.programminglife.libpljp.android.Logger.Companion.get
import jp.programminglife.libpljp.android.UuidGenerator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.scan
//import kotlinx.android.synthetic.main.main_activity_a.*

class MainActivity : ComponentActivity() {
    private val log = get(javaClass)
    private lateinit var repository: UuidGenerator.UuidRepository
    private lateinit var specifiedValueRepository: SpecifiedValueUuidRepository
    private lateinit var uuidGenerator: UuidGenerator
    private val generateUuidIntent = MutableSharedFlow<Pair<String, String>>(
            extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flowOfState = presenter(generateUuidIntent)
        repository = UuidGenerator.PreferencesUuidRepository(this)
        specifiedValueRepository = SpecifiedValueUuidRepository(repository)
        uuidGenerator = UuidGenerator(specifiedValueRepository)
        setContent {
            val state = flowOfState.collectAsState(initial = UuidGeneratorState.INITIAL_STATE)
            UUIDGeneratorUI(state.value, ::onClickGenerateButton)
        }
    }

    
    private fun presenter(intent: SharedFlow<Pair<String, String>>): Flow<UuidGeneratorState> {
        return intent.scan(UuidGeneratorState.INITIAL_STATE) { oldState, (countStr, nodeIdStr) ->
            val n = kotlin.runCatching { countStr.toInt() }
                    .onFailure { log.d(it, "数のパースエラー") }
                    .getOrDefault(1)
            val nodeId = if (nodeIdStr.isNotEmpty()) {
                kotlin.runCatching { nodeIdStr.toLong(16) }
                        .onFailure { log.d(it, "ノードIDのパースエラー") }
                        .getOrDefault(0)
            } else {
                null
            }
            specifiedValueRepository.nodeId = nodeId
            val uuidStr = buildString {
                (0 until n).joinTo(buffer = this, separator = "\n", transform = {
                    uuidGenerator.generate(System.currentTimeMillis(), System.nanoTime()).toString()
                })
            }
            oldState.copy(uuids = uuidStr)
        }
    }


    private fun onClickGenerateButton(countStr: String, nodeId: String) {
        generateUuidIntent.tryEmit(countStr to nodeId)
    }


    private class SpecifiedValueUuidRepository(
            private val repository: UuidGenerator.UuidRepository
    ) : UuidGenerator.UuidRepository {

        var nodeId: Long? = null

        override fun loadNodeId(): Long? {
            return nodeId ?: repository.loadNodeId()
        }

        override fun saveNodeId(nodeId: Long?) {
            repository.saveNodeId(nodeId)
        }

    }
}


data class UuidGeneratorState(
        val uuids: String,
) {
    companion object {
        val INITIAL_STATE =  UuidGeneratorState("")
    }
}


@Composable
fun UUIDGeneratorUI(
        state: UuidGeneratorState,
        onClickGenerateButton: (count: String, nodeId: String) -> Unit,
) {
    // 生成する数とNode IDを入力するためのテキストフィールドの状態
    var count by remember { mutableStateOf("") }
    var nodeId by remember { mutableStateOf("") }

    // UIのレイアウト
    Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // テキストフィールド: 生成する数
        OutlinedTextField(
                value = count,
                onValueChange = { count = it },
                label = { Text("生成する数") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // テキストフィールド: Node ID
        OutlinedTextField(
                value = nodeId,
                onValueChange = { nodeId = it },
                label = { Text("Node ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // ボタン: 生成
        Button(
                onClick = {
                    onClickGenerateButton(count, nodeId)
                }
        ) {
            Text("生成")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 生成されたUUIDのリスト(枠で囲む)
        Surface(
                modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                    modifier = Modifier
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                    contentAlignment = Alignment.Center
            ) {
                // リストが空でなければ、各要素にテキストコンポーネントを表示する
                if (state.uuids.isNotEmpty()) {
                    BasicTextField(
                            value = state.uuids,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true
                    )
                } else {
                    // リストが空なら、テキストコンポーネントでメッセージを表示する
                    Text("UUIDが生成されていません")
                }
            }
        }
    }
}

@Composable
@Preview
private fun MainPreview() {
    UUIDGeneratorUI(UuidGeneratorState.INITIAL_STATE) { _, _ -> }
}