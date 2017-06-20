package jp.programminglife.libpljp.android.experimental

import java.util.ArrayDeque
import kotlin.coroutines.experimental.buildSequence


class TreeTraverser<T>(private val children: (T) -> Iterable<T>) {

    fun preOrderTraversal(root: T): Sequence<T> {

        return buildSequence {
            val stack = ArrayDeque<Iterator<T>>()
            stack.push(listOf(root).iterator())

            while (stack.isNotEmpty()) {
                var itr = stack.pop()!!
                while(itr.hasNext()) {
                    itr.next().let { node ->
                        yield(node)
                        children(node).iterator().takeIf { it.hasNext() }?.let {
                            stack.push(itr)
                            itr = it
                        }
                    }
                }
            }
        }

    }

    fun postOrderTraversal(root: T): Sequence<T> {

        return buildSequence {
            val itrStack = ArrayDeque<Iterator<T>>()
            val nodeStack = ArrayDeque<T>()
            itrStack.push(children(root).iterator())
            nodeStack.push(root)

            while (itrStack.isNotEmpty()) {
                var itr = itrStack.pop()!!
                while (itr.hasNext()) {
                    itr.next().let { node ->
                        // 子要素があれば現在のイテレーターをスタックに積んで子要素のループに移る
                        children(node).iterator().takeIf { it.hasNext() }?.let {
                            nodeStack.push(node)
                            itrStack.push(itr)
                            itr = it
                        } ?: yield(node) // 子要素がなければ現在のノードを出力
                    }
                }
                // 子要素がなくなったらスタックのノードを出力
                yield(nodeStack.pop())
            }
        }

    }

}