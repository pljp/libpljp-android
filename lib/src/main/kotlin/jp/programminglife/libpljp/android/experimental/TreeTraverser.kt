package jp.programminglife.libpljp.android.experimental

import jp.programminglife.libpljp.android.TreeTraverser


@Deprecated("パッケージ変更")
class TreeTraverser<T>(children: (T) -> Iterable<T>) {
    private val treeTraverser = TreeTraverser(children)
    fun preOrderTraversal(root: T) = treeTraverser.preOrderTraversal(root)
    fun postOrderTraversal(root: T) = treeTraverser.postOrderTraversal(root)
}
