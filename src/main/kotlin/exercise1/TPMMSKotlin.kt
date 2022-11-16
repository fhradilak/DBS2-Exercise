package exercise1

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.Block
import de.hpi.dbs2.dbms.BlockManager
import de.hpi.dbs2.dbms.BlockOutput
import de.hpi.dbs2.dbms.Relation
import de.hpi.dbs2.dbms.utils.BlockSorter
import de.hpi.dbs2.exercise1.SortOperation
import java.util.*

@ChosenImplementation(true)
class TPMMSKotlin(
    manager: BlockManager,
    sortColumnIndex: Int
) : SortOperation(manager, sortColumnIndex) {

    override fun estimatedIOCost(relation: Relation): Int = 2*relation.estimatedSize;

    override fun sort(relation: Relation, output: BlockOutput) {
        val comparator = relation.columns.getColumnComparator(sortColumnIndex)
        if(relation.estimatedSize< this.blockManager.freeBlocks*this.blockManager.freeBlocks){
           throw RelationSizeExceedsCapacityException();
        }

        val numberLists = relation.estimatedSize/ this.blockManager.freeBlocks
        var lists = Vector<MutableList<Block>>();
        var listSize = relation.estimatedSize/numberLists
        val relationIterator = relation.iterator();
        val currentList:MutableList<Block> = mutableListOf();//
        val length = 0;
        while(relationIterator.hasNext()){
            currentList.add(relationIterator.next())
            if(currentList.size % listSize == 0){
                lists.add(currentList)
                currentList.clear()
            }
        }


        for (i in 0..lists.size){
            for (j in 0..lists[i].size){
               lists[i][j]= blockManager.load(lists[i][j])
            }
            //lists[i].sortWith(comparator);//????????
        }
    }
}

