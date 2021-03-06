package jp.programminglife.libpljp.android

import java.util.ArrayDeque


@Deprecated("")
class TreeTraverser<T>(private val children: (T) -> Iterable<T>) {

    /**
     * 親ノード優先でツリーを横断する。
     */
    fun preOrderTraversal(root: T): Sequence<T> {

        return sequence {
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

    /**
     * 子ノード優先でツリーを横断する。
     */
    fun postOrderTraversal(root: T): Sequence<T> {

        return sequence {
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