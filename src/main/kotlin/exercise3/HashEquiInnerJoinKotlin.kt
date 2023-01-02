package exercise3

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.*
import de.hpi.dbs2.exercise3.InnerJoinOperation
import de.hpi.dbs2.exercise3.JoinAttributePair
import java.util.function.Consumer

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
        //
        //  - calculate a sensible bucket count
        //  - hash relation

        val leftBuffers = partition(leftInputRelation)
        val rightBuffers = partition((rightInputRelation))

        //  - join hashed blocks

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
                        tupleAppender as Consumer<Tuple>,
                    )
                }
            }
        }

    }

    fun hash(input: Any, size: Int): Int {
        return input.hashCode() % size
    }

    fun partition(
        inputRelation: Relation,
    ): ArrayList<ArrayList<Block>> {
        val bucketNumber: Int = blockManager.freeBlocks - 1
        val diskBuffers = arrayListOf<ArrayList<Block>>()
        val buffers = arrayListOf<Block>()

        for (i in 0..bucketNumber) {
            buffers[i] = blockManager.allocate(true)
        }
        for (blockRef: Block in inputRelation) {
            val block = blockManager.load(blockRef)
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

    internal inner class TupleAppender(var blockOutput: BlockOutput) : AutoCloseable,
        Consumer<Tuple?> {
        var outputBlock: Block = blockManager.allocate(true)
        override fun accept(tuple: Tuple?) {
            if (outputBlock.isFull()) {
                blockOutput.move(outputBlock)
                outputBlock = blockManager.allocate(true)
            }
            outputBlock.append(tuple!!)
        }

        override fun close() {
            if (!outputBlock.isEmpty()) {
                blockOutput.move(outputBlock)
            } else {
                blockManager.release(outputBlock, false)
            }
        }
    }

}



