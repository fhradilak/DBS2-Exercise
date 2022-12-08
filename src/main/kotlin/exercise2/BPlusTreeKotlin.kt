package exercise2

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import org.w3c.dom.Node

@ChosenImplementation(false)
class BPlusTreeKotlin : AbstractBPlusTree {
    constructor(order: Int) : super(order)
    constructor(rootNode: BPlusTreeNode<*>) : super(rootNode)

    override fun insert(key: Int, value: ValueReference): ValueReference? {

        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
        var path = ArrayDeque<InnerNode>()
        var depth = rootNode.getHeight()

        //checken ob Baum nur 1 Knoten hat ->

        //wenn Baum tiefe 2 -> findLeaf

        //wenn Baum tiefe >2 ->
        var currentInner = InnerNode(order);

        var selectedChild = rootNode;
        for (i in rootNode.keys.indices) {
            val currentKey: Int = rootNode.keys.get(i)
            if (currentKey != null) {
                if (key < currentKey) {
                    currentInner = rootNode.references.get(i) as InnerNode
                    path.addLast(currentInner.selectChild(key) as InnerNode)
                }
            } else {
                if (rootNode.references.get(i) is InnerNode) {
                    currentInner = rootNode.references.get(i) as InnerNode// "left" reference
                    path.addLast(currentInner.selectChild(key) as InnerNode)
                }
            }
        }
        //create inner node stack

        for (i in 0..depth - 3) {
            path.addLast(currentInner.selectChild(key) as InnerNode)
            currentInner = currentInner.selectChild(key) as InnerNode
        }
        //get leaf
        val leaf = currentInner.selectChild(key)



        // Does the key already exist? Overwrite!
        if(leaf.getOrNull(key) != null){
            for(i in 0..leaf.keys.size){
                if(key == leaf.keys[i]){
                    val oldValue:ValueReference = leaf.getOrNull(i)!!
                    leaf.references[i] = value;
                    return oldValue
                }
            }

        }else{
            // New key - Is there still space?

            
            //   leafNode.keys[pos] = key;
            //   leafNode.references[pos] = value;
            //   Don't forget to update the parent keys and so on...
            // Otherwise
            //   Split the LeafNode in two!
            //   Is parent node root?
            //     update rootNode = ... // will have only one key
            //   Was node instanceof LeafNode?
            //     update parentNode.keys[?] = ...
            //   Don't forget to update the parent keys and so on...

            // Check out the exercise slides for a flow chart of this logic.
            // If you feel stuck, try to draw what you want to do and
            // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
            // Also check out all the methods on BPlusTreeNode and how they are implemented or
            // the tests in BPlusTreeNodeTests and BPlusTreeTests!
        }

        //   But remember return the old value!



    }
}
