package exercise3

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.*
import de.hpi.dbs2.exercise3.InnerJoinOperation
import de.hpi.dbs2.exercise3.JoinAttributePair
import java.lang.Math.abs
import java.util.function.Consumer

@ChosenImplementation(true)
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
        return 5 * (leftInputRelation.estimatedBlockCount() + rightInputRelation.estimatedBlockCount())
    }

    override fun join(
        leftInputRelation: Relation,
        rightInputRelation: Relation,
        outputRelation: Relation
    ) {
        //
        //  - calculate a sensible bucket count
        //  - hash relation
        if (blockManager.freeBlocks < 3) {
            throw Operation.RelationSizeExceedsCapacityException()
        }


        val leftBuffers = partition(leftInputRelation)
        val rightBuffers = partition((rightInputRelation))

        //  - join hashed blocks

        val tupleAppender = TupleAppender(outputRelation.getBlockOutput())

        for ((i, v) in leftBuffers.withIndex()) {
            val leftEntries = leftBuffers[i]
            if (i !in rightBuffers.indices) {
                break
            }
            val rightEntries = rightBuffers[i]

            if (leftEntries != null) {
                for (l in leftEntries) {
                    var blockL = blockManager.load(l)
                    if (rightEntries != null) {
                        for (r in rightEntries) {

                            var blockR = blockManager.load(r)
                            joinBlocks(
                                l,
                                r,
                                outputRelation.columns,
                                tupleAppender as Consumer<Tuple>,
                            );
                            blockManager.release(blockR, false)
                        }
                    }
                    blockManager.release(blockL, false)
                }
            }
            tupleAppender.close()
        }

    }

    fun hash(input: Any, size: Int): Int {
        return input.hashCode() % size
    }

    fun partition(
        inputRelation: Relation,
    ): Array<ArrayList<Block>?> {
        val bucketNumber: Int = blockManager.freeBlocks - 2
        val diskBuffers = Array<ArrayList<Block>?>(bucketNumber) { i -> arrayListOf() }
        val buffers = arrayListOf<Block>()

        for (i in 0..bucketNumber) {
            buffers.add(i, blockManager.allocate(true))
        }
        for (blockRef: Block in inputRelation) {
            val block = blockManager.load(blockRef)
            for (tuple: Tuple in block) {
                val blockNumber = abs(hash(tuple, bucketNumber))
                if (buffers[blockNumber].isFull()) {
                    //spill
                    val outputBlock = blockManager.release(block, true)
                    if (outputBlock != null) {
                        var currentList = arrayListOf<Block>()
                        diskBuffers[blockNumber]?.add(outputBlock)
                    }
                    buffers.add(blockNumber, blockManager.allocate(true))
                }
                buffers[blockNumber].append(tuple)

            }
            blockManager.release(block, false)
        }


        for (i in 0..buffers.size) {
            val outputBlock = blockManager.release(buffers[i], true)
            if (outputBlock != null) {
                diskBuffers[i]?.add(outputBlock)
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



