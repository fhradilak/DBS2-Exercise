package exercise3

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.*
import de.hpi.dbs2.exercise3.InnerJoinOperation
import de.hpi.dbs2.exercise3.JoinAttributePair
import de.hpi.dbs2.exercise3.NestedLoopEquiInnerJoin.TupleAppender

@ChosenImplementation(false)
class HashEquiInnerJoinKotlin(
    blockManager: BlockManager,
    leftColumnIndex: Int,
    rightColumnIndex: Int,
) : InnerJoinOperation(
    blockManager,
    JoinAttributePair.EquiJoinAttributePair(
        leftColumnIndex,
        rightColumnIndex
    )
) {

    override fun estimatedIOCost(
        leftInputRelation: Relation,
        rightInputRelation: Relation
    ): Int {
        TODO()
    }

    override fun join(
        leftInputRelation: Relation,
        rightInputRelation: Relation,
        outputRelation: Relation
    ) {
        val bucketCount: Int = blockManager.freeBlocks - 1
        // TODO:
        //  - calculate a sensible bucket count
        //  - hash relation

        val leftBuffers = partition(leftInputRelation)
        val rightBuffers = partition((rightInputRelation))

        //  - join hashed blocks

        val leftI = 0//JoinAttributePair.EquiJoinAttributePair.leftColumnIndex
        val rightI = 0//JoinAttributePair.rightColumnIndex
        val tupleAppender = TupleAppender(outputRelation.getBlockOutput())

        for (i in 0..leftBuffers.size) {
            val leftEntries = leftBuffers[i]
            val rightEntries = rightBuffers[i]

            for(l in leftEntries){
                for(r in rightEntries){
                    joinBlocks(
                        l,
                        r,
                        outputRelation.columns,
                        tupleAppender
                    )
                }
            }
        }

        TODO()

    }

    fun hash(input: Any, size: Int): Int {
        return input.hashCode() % size
    }

    fun partition(
        leftInputRelation: Relation,
    ): ArrayList<ArrayList<Block>> {
        val bucketNumber: Int = blockManager.freeBlocks - 1;
        val diskBuffers = arrayListOf<ArrayList<Block>>()
        val buffers = arrayListOf<Block>()

        for (i in 0..bucketNumber) {
            buffers[i] = blockManager.allocate(true)
        }
        for (leftBlockRef: Block in leftInputRelation) {
            val block = blockManager.load(leftBlockRef)
            for (tuple: Tuple in block) {
                val blockNumber = hash(tuple, bucketNumber)
                if (buffers[blockNumber].isFull()) {
                    //spill
                    val outputBlock = blockManager.release(block, true)
                    if (outputBlock != null) {
                        diskBuffers[blockNumber].add(outputBlock)
                    }
                    buffers[blockNumber] = blockManager.allocate(true)
                }
                buffers[blockNumber].append(tuple)
            }
        }

        for (i in 0..buffers.size) {
            if (!buffers[i].isEmpty()) {
                val outputBlock = blockManager.release(buffers[i], true)
                if (outputBlock != null) {
                    diskBuffers[i].add(outputBlock)
                }
            }
        }
        return diskBuffers
    }


}



