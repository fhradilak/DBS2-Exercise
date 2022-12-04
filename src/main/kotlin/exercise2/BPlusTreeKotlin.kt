package exercise2

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import kotlin.math.ceil
import kotlin.math.floor

@ChosenImplementation(true)
class BPlusTreeKotlin : AbstractBPlusTree {
    constructor(order: Int) : super(order)
    constructor(rootNode: BPlusTreeNode<*>) : super(rootNode)

    override fun insert(key: Int, value: ValueReference): ValueReference? {
        if (getOrNull(key) != null)
            return replaceValueForPresentKey(key, value)

        insertIntoNode(key, value, rootNode)
        return null
    }

    /**
     * Replace value for a given key return old value or null if not present.
     */
    private fun replaceValueForPresentKey(searchKey: Int, value: ValueReference): ValueReference? {
        val leaf = rootNode.findLeaf(searchKey)
        for ((index, key) in leaf.keys.withIndex()) {
            if (searchKey == key) {
                val oldValue = leaf.references[index]
                leaf.references[index] = value
                return oldValue
            }
        }
        return null
    }

    /**
     * Generate correctly sorted lists of all keys and values that have to be inside a leaf after adding a new entry
     */
    private fun updatedKeysAndValuesLists(leaf: LeafNode, newKey: Int, newValue: ValueReference): Pair<List<Int>, List<ValueReference>> {
        val updatedKeys = leaf.keys.toMutableList()
        val updatedValues = leaf.references.toMutableList()
        for ((index, key) in updatedKeys.withIndex()) {
            if (key == null || newKey < key) {
                updatedKeys.add(index, newKey)
                updatedValues.add(index, newValue)
                return Pair(updatedKeys.filterNotNull(), updatedValues.filterNotNull())
            }
        }
        updatedKeys.add(newKey)
        updatedValues.add(newValue)
        return Pair(updatedKeys.filterNotNull(), updatedValues.filterNotNull())
    }

    /**
     * Generate correctly sorted list of all references to children nodes that have to be inside an inner node after adding a new reference.
     * If no new reference should be added just return list with current reference.
     */
    private fun updatedReferencesList(inner: InnerNode, newReference: BPlusTreeNode<*>?): List<BPlusTreeNode<*>> {
        val updatedReferences = inner.references.toMutableList()
        if (newReference == null)
            return updatedReferences.filterNotNull()
        for ((index, reference) in updatedReferences.withIndex()) {
            if (reference == null || newReference.smallestKey < reference.smallestKey) {
                updatedReferences.add(index, newReference)
                return updatedReferences.filterNotNull()
            }
        }
        updatedReferences.add(newReference)
        return updatedReferences.filterNotNull()
    }

    /**
     * Generate list of keys for a list of references.
     * Key i will be the smallest key in reference i+1's leafs.
     */
    private fun keysForReferenceList(references: List<BPlusTreeNode<*>>): MutableList<Int> {
        val keys = mutableListOf<Int>()
        for (index in 1 until references.size)
            keys.add(references[index].smallestKey)
        return keys
    }

    /**
     * Replace the keys and references for a node with the values from a list.
     */
    private fun <V> overwriteKeysAndReferences(node: BPlusTreeNode<V>, keys: List<Int>, references: List<V>) {
        for (index in node.keys.indices) {
            if (index < keys.size)
                node.keys[index] = keys[index]
            else
                node.keys[index] = null
        }
        for (index in node.references.indices)
            if (index < references.size)
                node.references[index] = references[index]
            else
                node.references[index] = null
    }


    /**
     * Recursively add a new key value pair into the subtree of a given node.
     * Return the newly created Node if a split happened or null otherwise.
     */
    private fun insertIntoNode(key: Int, value: ValueReference, _node: BPlusTreeNode<*>): BPlusTreeNode<*>? {
        // this will be returned
        var newNode: BPlusTreeNode<*>?
        // work with local copy so we can overwrite this
        var node = _node

        // Base case: node is a leaf
        if (node.height == 0) {
            val (updatedKeys, updatedValues) = updatedKeysAndValuesLists(node as LeafNode, key, value)
            // if there is still space
            if (!node.isFull) {
                overwriteKeysAndReferences(node, updatedKeys, updatedValues)
                return null
            }
            // if leaf was InitialRootNode, replace it with normal leaf Node
            if (rootNode.height == 0)
                node = LeafNode(order)
            // split node
            newNode = LeafNode(order)
            newNode.nextSibling = node.nextSibling
            node.nextSibling = newNode
            val referenceCountLeft = ceil(updatedValues.size / 2.0).toInt()
            overwriteKeysAndReferences(node, updatedKeys.take(referenceCountLeft), updatedValues.take(referenceCountLeft))
            overwriteKeysAndReferences(newNode, updatedKeys.drop(referenceCountLeft), updatedValues.drop(referenceCountLeft))
        } else {
            // node is InnerNode

            // recursive call to correct child
            val newReference = insertIntoNode(key, value, (node as InnerNode).selectChild(key))
            val updatedReferences = updatedReferencesList(node, newReference)
            // if it still fits inside current node
            if (updatedReferences.size <= order) {
                overwriteKeysAndReferences(node, keysForReferenceList(updatedReferences), updatedReferences)
                return null
            }
            // split node
            newNode = InnerNode(order)
            val referenceCountLeft = floor(updatedReferences.size / 2.0).toInt()
            overwriteKeysAndReferences(node,
                    keysForReferenceList(updatedReferences.take(referenceCountLeft)),
                    updatedReferences.take(referenceCountLeft))
            overwriteKeysAndReferences(
                    newNode,
                    keysForReferenceList(updatedReferences.drop(referenceCountLeft)),
                    updatedReferences.drop(referenceCountLeft))
        }

        // if node is root, create new root
        if (node.height == rootNode.height)
            rootNode = InnerNode(order, node, newNode)

        return newNode
    }


}
