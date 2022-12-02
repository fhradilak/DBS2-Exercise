package exercise2

import com.sun.jdi.IntegerType
import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import java.util.Stack
import kotlin.math.ceil

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
        //   I DON'T THINK THAT THE PARENT KEY HAS TO BE UPDATED IF THERE IS STILL SPACE!

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
            return null;
        }

        // Otherwise
        //   Split the LeafNode in two!
        //   Is parent node root?
        //     update rootNode = ... // will have only one key
        //   Was node instanceof LeafNode?
        //     update parentNode.keys[?] = ...
        //   Don't forget to update the parent keys and so on...

        if(leaf.height == 0) {
            // replace InitialRootNode with LeafNode
            var leafKeys = leaf.keys;
            var leafReferences = leaf.references;
            leaf = BPlusTreeNode.buildTree(order) as LeafNode;
            leaf.keys = leafKeys;
            leaf.references = leafReferences;
        }

        var newLeaf = BPlusTreeNode.buildTree(order) as LeafNode
        var newIndex = 0
        for (index in leaf.keys.indices) {
            if (index >= ceil(leaf.keys.size / 2.0)) {
                newLeaf.keys[newIndex] = leaf.keys[index]
                newLeaf.references[newIndex] = leaf.references[index]
            }
        }
        newLeaf.nextSibling = leaf.nextSibling;
        leaf.nextSibling = newLeaf;

        if



        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!


        return null
    }
}
