package exercise2

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

@ChosenImplementation(true)
class BPlusTreeKotlin : AbstractBPlusTree {
    constructor(order: Int) : super(order)
    constructor(rootNode: BPlusTreeNode<*>) : super(rootNode)

    override fun insert(key: Int, value: ValueReference): ValueReference? {
        val searchKey = key

        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.

        var currentNode = rootNode
        var visitedNodes = Stack<BPlusTreeNode<*>>()

        visitedNodes.push(rootNode)
        while (currentNode.height > 0) { // while current Node is not a LeafNode
            currentNode = (currentNode as InnerNode).selectChild(searchKey)
            visitedNodes.push(currentNode)
        }
        var leaf = visitedNodes.pop() as LeafNode

        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!

        for ((index, key) in leaf.keys.withIndex()) {
            if(searchKey.equals(key)) {
                var oldValue = leaf.references[index]
                leaf.references[index] = value
                return oldValue
            }
        }

        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...

        var newNode : BPlusTreeNode<*> = leaf
        var shouldStillInsert = false

        if (!leaf.isFull) {
            var newKeys = mutableListOf<Int>()
            var newValues = mutableListOf<ValueReference>()
            var inserted = false
            for ((index, key) in leaf.keys.withIndex()) {
                if (key == null) continue
                if(searchKey < key && !inserted) {
                    newKeys.add(searchKey)
                    newValues.add(value)
                    inserted = true
                }
                newKeys.add(key)
                newValues.add(leaf.references[index])
            }
            if(!inserted) {
                newKeys.add(searchKey)
                newValues.add(value)
            }
            for(index in newKeys.indices) {
                leaf.keys[index] = newKeys[index]
                leaf.references[index] = newValues[index]
            }
        } else {

            // Otherwise
            //   Split the LeafNode in two!
            //   Is parent node root?
            //     update rootNode = ... // will have only one key
            //   Was node instanceof LeafNode?
            //     update parentNode.keys[?] = ...
            //   Don't forget to update the parent keys and so on...

            if (rootNode.height == 0) {
                // replace InitialRootNode with LeafNode
                var leafKeys = leaf.keys
                var leafReferences = leaf.references
                leaf = LeafNode(order)
                for (index in leafKeys.indices) {
                    leaf.keys[index] = leafKeys[index]
                    leaf.references[index] = leafReferences[index]
                }
            }

            // split leaf
            var newLeaf = LeafNode(order)
            var newIndex = 0
            // create list with all keys
            var newKeys = mutableListOf<Int>()
            var newValues = mutableListOf<ValueReference>()
            var inserted = false
            for ((index, key) in leaf.keys.withIndex()) {
                if (key == null) continue
                if (searchKey < key && !inserted) {
                    newKeys.add(searchKey)
                    newValues.add(value)
                    inserted = true
                }
                newKeys.add(key)
                newValues.add(leaf.references[index])
            }
            if (!inserted) {
                newKeys.add(searchKey)
                newValues.add(value)
            }
            // divide keys over both nodes
            for (index in newKeys.indices) {
                if (index < ceil(newKeys.size / 2.0)) {
                    leaf.keys[index] = newKeys[index]
                    leaf.references[index] = newValues[index]
                } else {
                    newLeaf.keys[newIndex] = newKeys[index]
                    newLeaf.references[newIndex] = newValues[index]
                    if (index < leaf.keys.size) {
                        leaf.keys[index] = null
                        leaf.references[index] = null
                    }
                    newIndex++
                }
            }
            newLeaf.nextSibling = leaf.nextSibling
            leaf.nextSibling = newLeaf

            if (rootNode.height == 0) {
                // create new root if leaf was root
                rootNode = InnerNode(order, leaf, newLeaf)
                return null
            }

            newNode = newLeaf
            shouldStillInsert = true

        }

        while(visitedNodes.size > 0) {
            var inner = visitedNodes.pop() as InnerNode

            var newSmallestKeys = mutableListOf<Int>()
            var newReferences = mutableListOf<BPlusTreeNode<*>>()
            for (reference in inner.references) {
                if(reference != null) {
                    if (shouldStillInsert && newNode.smallestKey < reference.smallestKey) {
                        newReferences.add(newNode)
                        newSmallestKeys.add(newNode.smallestKey)
                        shouldStillInsert = false
                    }
                    newReferences.add(reference)
                    newSmallestKeys.add(reference.smallestKey)
                }
            }
            if(shouldStillInsert) {
                newReferences.add(newNode)
                newSmallestKeys.add(newNode.smallestKey)
            }

            // not full yet
            if (newReferences.size <= order) {
                for(index in inner.references.indices) {
                    if(index < newReferences.size) {
                        inner.references[index] = newReferences[index]
                        if(index + 1 < newSmallestKeys.size) {
                            inner.keys[index] = newSmallestKeys[index + 1]
                        }
                    } else {
                        inner.references[index] = null
                        if(index + 1 < newSmallestKeys.size) {
                            inner.keys[index] = null
                        }
                    }
                }
                shouldStillInsert = false
            } else {
                // split inner node
                var newInner = InnerNode(order)
                var referencesLeft = floor(newReferences.size / 2.0)
                var newIndex = 0
                for (index in newReferences.indices) {
                    if(index <= referencesLeft) {
                        inner.references[index] = newReferences[index]
                        if(index >= 1) {
                            inner.keys[index - 1] = newSmallestKeys[index]
                        }
                    } else {
                        newInner.references[newIndex] = newReferences[index]
                        if(newIndex >= 1) {
                            newInner.keys[newIndex - 1] = newSmallestKeys[index]
                        }
                        if(index < inner.references.size) {
                            inner.keys[index - 1] = null
                            inner.references[index] = null
                        }
                        newIndex ++
                    }
                }
                if(visitedNodes.size == 0) {
                    rootNode = InnerNode(order, inner, newInner)
                    return null
                }
                newNode = newInner
                shouldStillInsert = true
            }
        }



        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!


        return null
    }
}
