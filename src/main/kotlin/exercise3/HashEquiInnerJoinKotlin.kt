package exercise3

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.*
import de.hpi.dbs2.exercise3.InnerJoinOperation
import de.hpi.dbs2.exercise3.JoinAttributePair
import java.lang.Math.floorMod
import java.util.function.Consumer

@ChosenImplementation(true)
class HashEquiInnerJoinKotlin(
        blockManager: BlockManager,
        leftColumnIndex: Int,
        rightColumnIndex: Int,
) : InnerJoinOperation(blockManager, JoinAttributePair.EquiJoinAttributePair(leftColumnIndex, rightColumnIndex)) {
    override fun estimatedIOCost(leftInputRelation: Relation, rightInputRelation: Relation): Int {
        return 3 * (leftInputRelation.estimatedBlockCount() + rightInputRelation.estimatedBlockCount())
    }

    override fun join(leftInputRelation: Relation, rightInputRelation: Relation, outputRelation: Relation) {
        // Calculate maximum bucket count. One free block will be needed to read next block from relation.
        val bucketCount: Int = blockManager.freeBlocks - 1
        // Load relations and partition them into buckets
        val bucketsLeft = partitionIntoBuckets(leftInputRelation, bucketCount, joinAttributePair.leftColumnIndex)
        val bucketsRight = partitionIntoBuckets(rightInputRelation, bucketCount, joinAttributePair.rightColumnIndex)

        // Create tupleAppender which is used to output into the output relation
        val tupleAppender = TupleAppender(outputRelation.getBlockOutput(), blockManager)
        // Join all bucket pairs
        for (bucketIndex in 0 until bucketCount) {
            joinBucket(bucketsLeft[bucketIndex], bucketsRight[bucketIndex], tupleAppender, outputRelation)
        }
        tupleAppender.close()
    }

    /**
     * Load a relation and assign every tuple into a bucket corresponding to the hash of a certain attribute.
     * @param relation The relation which will be loaded and partitioned into buckets.
     * @param bucketCount How many buckets should be created.
     * @param columnIndex The index of the column which will be hashed.
     * @return A list of lists of blocks.
     *         The outer list cointains a list for each bucket.
     *         Each inner list contains all the Blocks that belong to the bucket.
     */
    fun partitionIntoBuckets(relation: Relation, bucketCount: Int, columnIndex: Int): MutableList<MutableList<Block>> {
        // allocate one block as a buffer for each bucket
        val buckets = mutableListOf<MutableList<Block>>()
        for (i in 0 until bucketCount) {
            buckets.add(mutableListOf(blockManager.allocate(true)))
        }

        // iterate over all blocks in the relation and load them one by one
        for (blockRef in relation) {
            val block = blockManager.load(blockRef)
            // iterate over all tuples in the current block
            for (tuple in block) {
                // calculate hash
                val hash = floorMod(tuple.get(columnIndex).hashCode(), bucketCount)
                // if the buffer for this bucket is full, save it to disc and create a new (overflow) block as a buffer
                if (buckets[hash][0].isFull()) {
                    buckets[hash][0] = blockManager.release(buckets[hash][0], true)!!
                    buckets[hash].add(0, blockManager.allocate(true))
                }
                // add the current tuple to its corresponding bucket
                buckets[hash][0].append(tuple)
            }
            blockManager.release(block, false)
        }
        // iterate over all buckets and save their buffers to disc
        for (bucket in buckets) {
            bucket[0] = blockManager.release(bucket[0], true)!!
        }
        // return the buckets
        return buckets
    }


    /**
     * Join a bucket from the left relation with one from the right relation. (Both should correspond to the same hash).
     * @param bucketLeft List with all blocks belonging to the bucket of the left relation.
     * @param bucketRight List with all blocks belonging to the bucket of the right relation.
     * @param tupleAppender TupleAppender which is used to add tuples to the output relation.
     * @param outputRelation The relation where the output is saved.
     */
    fun joinBucket(bucketLeft: MutableList<Block>, bucketRight: MutableList<Block>, tupleAppender: TupleAppender, outputRelation: Relation) {
        // check which bucket is smaller
        val leftIsLarger = bucketRight.size < bucketLeft.size
        val bucketSmaller = if (leftIsLarger) bucketRight else bucketLeft
        val bucketLarger = if (leftIsLarger) bucketLeft else bucketRight

        // check if we have enough memory for one pass join
        // (all blocks from smaller bucket and single block from larger bucket needs to fit into memory.
        // Block for output is already reserved by tupleAppender)
        if (bucketSmaller.size > blockManager.freeBlocks - 1) {
            // close tuple appender to release reserved block
            tupleAppender.close()
            throw Operation.RelationSizeExceedsCapacityException()
        }

        // load all blocks from the smaller bucket into memory
        for (blockIndex in 0 until bucketSmaller.size) {
            bucketSmaller[blockIndex] = blockManager.load(bucketSmaller[blockIndex])
        }

        // load the blocks from the larger bucket one by one
        for (blockRef in bucketLarger) {
            val blockFromLarger = blockManager.load(blockRef)
            // create join tuples between all blocks from the smaller bucket with the current block of the larger bucket
            for (blockFromSmaller in bucketSmaller) {
                joinBlocks(
                        if (leftIsLarger) blockFromLarger else blockFromSmaller,
                        if (leftIsLarger) blockFromSmaller else blockFromLarger,
                        outputRelation.columns, tupleAppender
                )
            }
            blockManager.release(blockFromLarger, false)
        }
        // release all blocks from smaller bucket
        for (block in bucketSmaller) {
            blockManager.release(block, false)
        }

    }

    /**
     * Tuple Appender copied from NestedLoopEquiInnerJoin.java.
     * This is used to output into the output relation.
     */
    class TupleAppender(val blockOutput: BlockOutput, val blockManager: BlockManager) : AutoCloseable, Consumer<Tuple> {

        var outputBlock = blockManager.allocate(true)

        override fun accept(tuple: Tuple) {
            if (outputBlock.isFull()) {
                blockOutput.move(outputBlock)
                outputBlock = blockManager.allocate(true)
            }
            outputBlock.append(tuple)
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
