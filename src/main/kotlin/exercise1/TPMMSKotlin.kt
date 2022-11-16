package exercise1

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.*
import de.hpi.dbs2.exercise1.SortOperation

import de.hpi.dbs2.dbms.utils.BlockSorter

@ChosenImplementation(true)
class TPMMSKotlin(
        manager: BlockManager,
        sortColumnIndex: Int
) : SortOperation(manager, sortColumnIndex) {
    override fun estimatedIOCost(relation: Relation): Int = TODO()

    override fun sort(relation: Relation, output: BlockOutput) {

        // Throw exception if input relation is too big
        if(relation.estimatedSize > blockManager.freeBlocks * blockManager.freeBlocks + 1) {
            throw RelationSizeExceedsCapacityException()
        }

        // comparator to compare Tuples by the correct column
        val comp = relation.columns.getColumnComparator(sortColumnIndex)

        // PHASE I
        val sortedLists = mutableListOf<MutableList<Block>>() // the lists which get sorted in phase I and merged in II

        val iter = relation.iterator()
        while(iter.hasNext()) {
            sortedLists.add(mutableListOf())
            // read blocks while we have memory available
            while (blockManager.freeBlocks > 0 && iter.hasNext()) {
                sortedLists.last().add(blockManager.load(iter.next()))
            }
            // sort everything we just loaded into memory
            BlockSorter.sort(relation, sortedLists.last(), comp)
            // write back the sorted blocks
            for (i in sortedLists.last().indices) {
                blockManager.release(sortedLists.last()[i], true)!!
            }
        }

        // PHASE II
        val blockIndices = mutableListOf<Int>() // for each list store index of first block that wasn't fully processed
        val tupleIndices = mutableListOf<Int>() // for each list store index (inside block) of first unprocessed tuple
        for (list in sortedLists){
            blockManager.load(list[0])
            blockIndices.add(0)
            tupleIndices.add(0)
        }

        // helper functions to avoid using ugly nested indices all the time
        val currentBlockFromList = { i: Int -> sortedLists[i][blockIndices[i]]}
        val firstTupleFromList = { i: Int -> currentBlockFromList(i)[tupleIndices[i]] }
        val isListFinished = { i: Int -> blockIndices[i] >= sortedLists[i].size }

        val outputBlock = blockManager.allocate(true) // the block in which we write our output
        while(true) {
            var allListsEmpty = true // flag to signal when all lists are empty
            // find minimal list head
            var minList = 0 // will contain the index of list with minimal head
            for (currentList in sortedLists.indices) {
                // skip fully processed lists
                if (isListFinished(currentList)) {
                    if(minList == currentList)
                        minList ++ // increase so it doesn't point to a list without any blocks left
                    continue
                }
                allListsEmpty = false
                // if current list has smaller head, update minList
                if (comp.compare(firstTupleFromList(currentList), firstTupleFromList(minList)) < 0)
                    minList = currentList
            }
            // if all lists are empty stop
            if(allListsEmpty)
                break

            // add to output block and write it to disk if full
            outputBlock.append(firstTupleFromList(minList))
            if (outputBlock.isFull())
                output.output(outputBlock)

            // increase indices to point to next tuple; load new block if necessary
            tupleIndices[minList] ++
            if(tupleIndices[minList] == currentBlockFromList(minList).size) {
                blockManager.release(currentBlockFromList(minList), false)
                tupleIndices[minList] = 0
                blockIndices[minList] ++
                // if list is not fully processed, load next block
                if(blockIndices[minList] < sortedLists[minList].size) {
                    blockManager.load(currentBlockFromList(minList))
                }
            }

        }

        // if output block still has tuples, write it to disc
        if(!outputBlock.isEmpty())
            output.output(outputBlock)
        blockManager.release(outputBlock, false)

    }
}
