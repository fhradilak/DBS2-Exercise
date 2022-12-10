package exercise2

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import kotlin.math.ceil

@ChosenImplementation(false)
class BPlusTreeKotlin : AbstractBPlusTree {
    constructor(order: Int) : super(order)
    constructor(rootNode: BPlusTreeNode<*>) : super(rootNode)

    override fun insert(key: Int, value: ValueReference): ValueReference? {

        if (getOrNull(key) != null) {
            val node = rootNode.findLeaf(key)
            for (i in 0..node.keys.size) {//size zu groß?
                if (key == node.keys[i]) {
                    val oldValue: ValueReference = node.getOrNull(i)!!
                    node.references[i] = value;
                    return oldValue
                }
            }

        }

        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
       /*
        var path: ArrayDeque<BPlusTreeNode<*>> = ArrayDeque(listOf(rootNode))
        var depth = rootNode.getHeight()

        //checken ob Baum nur 1 Knoten hat ->

        //wenn Baum tiefe 2 -> findLeaf

        //wenn Baum tiefe >2 ->
        var currentInner = InnerNode(order);

        //var selectedChild = rootNode;
        for (i in rootNode.keys.indices) {
            val currentKey: Int = rootNode.keys[i]
            if (currentKey != null) {
                if (key <= currentKey) {
                    currentInner = rootNode.references.get(i) as InnerNode
                    //path.addLast(currentInner.selectChild(key) as InnerNode)
                } else {
                    break;
                }
            } else {
                if (rootNode.references[i] is InnerNode) {
                    currentInner = rootNode.references.get(i) as InnerNode// "left" reference
                    //path.addLast(currentInner.selectChild(key) as InnerNode)
                }
            }
        }
        //create inner node stack
        path.addLast(currentInner)
        for (i in 0..depth - 3) {
            path.addLast(currentInner.selectChild(key) as InnerNode)
            currentInner = currentInner.selectChild(key) as InnerNode
        }
        //get leaf
        val leaf = currentInner.selectChild(key)




        // Does the key already exist? Overwrite!
        if (leaf.getOrNull(key) != null) {
            //getOrNull(key) != null
            for (i in 0..leaf.keys.size) {//size zu groß?
                if (key == leaf.keys[i]) {
                    val oldValue: ValueReference = leaf.getOrNull(i)!!
                    (leaf as LeafNode).references[i] = value;
                    return oldValue
                }
            }

        }

        */
        insertIntoNode(key,value,rootNode)
        return null
    }


    //recursive call fct
    fun insertIntoNode(key: Int, value: ValueReference, _node: BPlusTreeNode<*>): Pair<Int?, BPlusTreeNode<*>?> {
        //base case
        var n2: BPlusTreeNode<*>
        var node = _node
        var newkey: Int



        if (node.height == 0) {
            //leaf
            //var newKeyValues: MutableList<Pair<Int, ValueReference>> = mutableListOf();
            var newKeys: MutableList<Int> = mutableListOf()
            var newReferences = mutableListOf<ValueReference>()
            if (!node.isFull) {
                for (i in 0..node.keys.size) {
                    if (node.keys[i] > key) {
                        //newKeyValues.add(Pair(key, value))
                        newKeys.add(key)
                        newReferences.add(value)
                    }
                    //newKeyValues.add(Pair(node.keys[i],node.references[i] as ValueReference))
                    newKeys.add(node.keys[i])
                    newReferences.add(node.references[i] as ValueReference)

                }
                for (i in 0..newKeys.size) {
                    node.keys[i] = newKeys[i]
                    (node as LeafNode).references[i] = newReferences[i]
                }

            }

            if (rootNode.height == 0)
                node = LeafNode(order)

            //split
            n2 = LeafNode(order)
            val numberRightValues = ceil(node.keys.size / 2.0).toInt()
            val beginIndexLeftNode = node.keys.size - numberRightValues
            val childkey: Int

            for (i in beginIndexLeftNode..node.keys.size) {
                n2.keys[i] = newKeys[i]
                n2.references[i] = newReferences[i]
                node.keys[i] = null
                (node as LeafNode).references[i] = null
            }

            //update pointers

            n2.nextSibling = (node as LeafNode).nextSibling
            node.nextSibling = n2

            //n root? TODO
            if (node.height == rootNode.height)
                rootNode = InnerNode(order, node, n2)

            //n is innerNOde -> biggest key in n1 to parent
            childkey = n2.keys.min()


            //check if node is initial root Node -> replace with leaf node TODO

            //split node , set new reference pointers, return second Node reference
            return Pair(childkey, n2)
        } else {
            var (childkey, newNode) = insertIntoNode(
                key,
                value,
                (node as InnerNode).selectChild(key)
            )//kann ich root übergeben?
            if (childkey == null)
                return Pair(null, null)

            var newKeys = mutableListOf<Int>()
            var newReferences = mutableListOf<BPlusTreeNode<*>>()



            //if child
            if (!node.isFull) {
                for (i in 0..node.keys.size) {
                    if (node.keys[i] > key) {
                        //newKeyValues.add(Pair(key, value))
                        newKeys.add(childkey)
                        if (newNode != null) {
                            newReferences.add(newNode)
                        }
                    }
                    //newKeyValues.add(Pair(node.keys[i],node.references[i] as ValueReference))
                    newKeys.add(node.keys[i])
                    newReferences.add(node.references[i])

                }
                for (i in 0..newKeys.size) {
                    node.keys[i] = newKeys[i]
                    node.references[i] = newReferences[i]
                }
            }
            //split
            n2 = InnerNode(order)
            val numberRightValues = ceil(node.keys.size / 2.0).toInt()
            val beginIndexLeftNode = node.keys.size - numberRightValues

            for (i in beginIndexLeftNode..node.keys.size) {
                n2.keys[i] = newKeys[i]
                n2.references[i] = newReferences[i]
                node.keys[i] = null
                node.references[i] = null
            }

            //update pointers

            //n root? TODO
            if (node.height == rootNode.height)
                rootNode = InnerNode(order, node, n2)

            //n is innerNOde -> biggest key in n1 to parent
            newkey = node.keys.max()
            //check if node is now to small potentially merge


        }
        return Pair(newkey, n2)
    }


    // Check out the exercise slides for a flow chart of this logic.
    // If you feel stuck, try to draw what you want to do and
    // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
    // Also check out all the methods on BPlusTreeNode and how they are implemented or
    // the tests in BPlusTreeNodeTests and BPlusTreeTests!
    //   But remember return the old value!


}
