import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;



class Block {
    int tag;
    int valid;
    boolean dirty;
    
    Block() {
	tag = -1;
	valid = 0;
	dirty = false;
    }
}

class Cache2 {
    
    Block[][] blocks;
    int associativity;
    int blocksize_bytes;
    int cachesize_kb;
    int misspen;
    int numBlocks;
    int numSets;
    int numBlocksPerSet;
    ArrayList<ArrayList<Integer>>LRUList;
    
    int loadHits = 0;
    int loadMisses = 0;
    int storeHits = 0;
    int storeMisses = 0;
    int dirtyEvictions = 0;
    
    long executionTime = 0;
    int memoryaccesses = 0;
    int mempencycles = 0;


//    Cache Capacity = (Block Size in Bytes) * (Blocks per Set) * (Number of Sets)
//    Index Bits = LOG2(Blocks per Set)
//    Block Offset Bits = LOG2(Block Size in Bytes)
//    Tag Bits = (Address Bits) - (Index Bits) - (Block Offset Bits)
//    
//    Block Offset = Memory Address mod 2^n
//    Block Address = Memory Address / 2^n
//    Set Index = Block Address mod 2^s
    
    Cache2(int associativity, int blocksize_bytes, int cachesize_kb, int misspen) {
	this.numBlocks = (cachesize_kb * 1024) / blocksize_bytes;
	this.numSets = numBlocks / associativity;
	this.numBlocksPerSet = numBlocks / numSets;
	this.misspen = misspen;
	
//	System.out.println(numBlocks);
//	System.out.println(numSets);
//	System.out.println(numBlocksPerSet);
	
        this.blocks = new Block[numBlocksPerSet][numSets];
        
        LRUList = new ArrayList<ArrayList<Integer>>();


	ArrayList<Integer> l;
        //Init LRU List per Sets
        for (int i = 0; i < numSets; i++) {
             l = new ArrayList<Integer>();
            for (int j = 0; j < associativity; j++) {
            	l.add(0);
            }
            LRUList.add(l);
        }
        
        //System.out.println(LRUList.get(0).size());
        

	
	 // init blocks based on associativity and size
        for (int i = 0; i < blocks.length; i++) {
//          System.out.println();
          for (int j = 0; j < blocks[i].length; j++) {
            
            blocks[i][j] = new Block();
//            System.out.print(j);
          }
        }
//        System.out.println();
    }
    
    
    int getEmptyBlock(int setIndex, boolean load)
    {

      // check if there is a free block in set.
      for (int i = 0; i < numBlocksPerSet; i++) {
        if (blocks[i][setIndex].valid == 0 ) {
            //block no longer empty valid bit = 1
            return i;
        }
      }
      //check LRU list for LRU index, return it since no free blocks
      int index = longestLRU(setIndex);
      //LRUList.get(setIndex).set(index, 0);

      //System.out.println(index);
  
      if (blocks[index][setIndex].dirty) {
	  executionTime+=2;
	  mempencycles+=2;

	  dirtyEvictions++;
	  //blocks[index][setIndex].dirty = false;
	  if (load) {
	      blocks[index][setIndex].dirty = false;
	  }

      }


      return index;
    }
    
    int longestLRU(int setIndex) {
	int biggestindex = 0;
	for (int i = 0; i < numBlocksPerSet; i++) {
	    if (LRUList.get(setIndex).get(i) > LRUList.get(setIndex).get(biggestindex)) {
		biggestindex = i;
	    }
	}
	return biggestindex;
    }
    
    void incLRU(int setIndex) {
	for (int i = 0; i < numBlocksPerSet; i++) {
	    LRUList.get(setIndex).set(i, LRUList.get(setIndex).get(i)+1);
//	    System.out.print(setIndex + " ");
//	    System.out.println(LRUList.get(setIndex));
	}
	
	return;
    }
    

    public void load(int tag, int setIndex, int icount) {
	executionTime+=icount;
	for (int i = 0; i < numBlocksPerSet; i++) {
	    if (blocks[i][setIndex].valid == 1 && blocks[i][setIndex].tag == tag) {
		loadHits++;
		
		incLRU(setIndex);
		
		LRUList.get(setIndex).set(i, 0);
		
		//blocks[i][setIndex].dirty = false;

		return;
		//perhaps return time value/cycle value here??
	    } 
	}
	
	executionTime+=(misspen);
	mempencycles+=misspen;

	loadMisses++;
	int index = getEmptyBlock(setIndex, true);
	//System.out.println(setIndex);
        Block block = blocks[index][setIndex];
        //update tag
        block.tag = tag;
        block.valid = 1;
        //why do i do this?
        
        //block.dirty = false;
	incLRU(setIndex);

        LRUList.get(setIndex).set(index, 0);
        
    }
    
    public void store(int tag, int setIndex, int icount) {
	executionTime+=icount;
	Block block;

	for (int i = 0; i < numBlocksPerSet; i++) {
	    if (blocks[i][setIndex].valid == 1 && blocks[i][setIndex].tag == tag) {
		storeHits++;
		block = blocks[i][setIndex];
		
		incLRU(setIndex);
		LRUList.get(setIndex).set(i, 0);
		
		block.tag = tag;
		block.valid = 1;
		block.dirty = true;
		return;
		//perhaps return time value/cycle value here??
	    } 
	}
	
	executionTime+=(misspen);
	mempencycles+=misspen;
	
	storeMisses++;
	int index = getEmptyBlock(setIndex, false);

	block = blocks[index][setIndex];
	block.tag = tag;
	block.valid = 1;
	
	//store back policy, cache data does not match mem data
	block.dirty = true;
	
	incLRU(setIndex);
	LRUList.get(setIndex).set(index, 0);

    }
    
    
    
}


public class cache {
	
	static int associativity = 2;          // Associativity of cache
	static int blocksize_bytes = 32;       // Cache Block size in bytes
	static int cachesize_kb = 64;          // Cache size in KB
	static int miss_penalty = 30;

	public static void print_usage()
	{
	  System.out.println("Usage: gunzip2 -c <tracefile> | java cache -a assoc -l blksz -s size -mp mispen\n");
	  System.out.println("  tracefile : The memory trace file\n");
	  System.out.println("  -a assoc : The associativity of the cache\n");
	  System.out.println("  -l blksz : The blocksize (in bytes) of the cache\n");
	  System.out.println("  -s size : The size (in KB) of the cache\n");
	  System.out.println("  -mp mispen: The miss penalty (in cycles) of a miss\n");
	  System.exit(0);
	}
	
	public static void main(String[] args) {
		  long address;
		  int loadstore, icount;
		  
		  int i = 0;
		  int j = 0;
		  // Process the command line arguments
		 // Process the command line arguments
		  while (j < args.length) {
		    if (args[j].equals("-a")) {
		      j++;
		      if (j >= args.length)
		        print_usage();
		      associativity = Integer.parseInt(args[j]);
		      j++;
		    } else if (args[j].equals("-l")) {
		      j++;
		      if (j >= args.length)
		        print_usage ();
		      blocksize_bytes = Integer.parseInt(args[j]);
		      j++;
		    } else if (args[j].equals("-s")) {
		      j++;
		      if (j >= args.length)
		        print_usage ();
		      cachesize_kb = Integer.parseInt(args[j]);
		      j++;
		    } else if (args[j].equals("-mp")) {
		      j++;
		      if (j >= args.length)
		        print_usage ();
		      miss_penalty = Integer.parseInt(args[j]);
		      j++;
		    } else {
		    	System.out.println("Bad argument: " + args[j]);
		      print_usage ();
		    }
		  }
		  

		  // print out cache configuration
		  System.out.println("Cache parameters:\n");
		  System.out.format("Cache Size (KB)\t\t\t%d\n", cachesize_kb);
		  System.out.format("Cache Associativity\t\t%d\n", associativity);
		  System.out.format("Cache Block Size (bytes)\t%d\n", blocksize_bytes);
		  System.out.format("Miss penalty (cyc)\t\t%d\n",miss_penalty);
		  System.out.println("\n");
		  
		  Cache2 cache = new Cache2(associativity, blocksize_bytes, cachesize_kb, miss_penalty);

		  Scanner sc = new Scanner(System.in);
		  
		  int instructions = 0;
		  
		  try {
		  while (sc.hasNextLine()) {
			sc.next(); //get rid of hashmark
			loadstore = sc.nextInt();
			address = sc.nextLong(16); //16 specifies it's in hex
			icount = sc.nextInt();
			// Code to print out just the first 10 addresses.  You'll want to delete
		    // this part once you get things going.
//			if(i<10){
//			    System.out.println("\t " + loadstore + " " + Long.toHexString(address) + " " + icount);
//			}
//			else{
//			    System.exit(0);
//			}
			i++;
			instructions+=icount;
			
			int blockOffset = 0;
			if (associativity > 1) {
			    blockOffset = (int) (address % blocksize_bytes);
			}	
		    	int blockAddress = (int) (address / blocksize_bytes);
		    	int setIndex = (int) ((address / (blocksize_bytes)) % cache.numSets);
		    		//(int) ((address / blocksize_bytes) % cache.numBlocks) / associativity;
		    	int tag = (int) (address >> ((int) Math.floor((Math.log(blocksize_bytes) / Math.log(2))))
		    					+ ((int) Math.floor((Math.log(cache.numSets) / Math.log(2)))));
//		    	
		    	//int tag = (int) (address / setIndex);
		    	
//		    	System.out.println(Long.toBinaryString(address));
//		    	System.out.println(Integer.toBinaryString(blockOffset));
////		    	System.out.println(Integer.toBinaryString(blockAddress));
//		    	System.out.println(setIndex);
//		    	System.out.println(tag);
		    	
		    	
		    	if (loadstore == 0) {
		    	    cache.load(tag, setIndex,icount);
		    	}
		    	
		    	if (loadstore == 1) {
		    	    cache.store(tag, setIndex, icount);
		    	}
		    	


		    //here is where you will want to process your memory accesses

		  }
		  } catch(Exception NoSuchElementExepction) {
		      
		  }
		  // Here is where you want to print out stats
		  System.out.format("Lines found = %d \n",i);
		  System.out.println("Simulation results:\n");
		  //  Use your simulator to output the following statistics.  The 
		  //  print statements are provided, just replace the question marks with
		  //  your calcuations.
		  		
		  
		  
		  System.out.format("\texecution time %d cycles\n", cache.executionTime);
		  System.out.format("\tinstructions %d\n", instructions);
		  System.out.format("\tmemory accesses %d\n", i);
		  System.out.format("\toverall miss rate %.2f\n", ((float)(cache.loadMisses + cache.storeMisses) / i) );
		  System.out.format("\tload miss rate %.2f\n", (float) cache.loadMisses / (cache.loadHits + cache.loadMisses) );
		  System.out.format("\tmemory CPI %.2f\n", (float) cache.mempencycles / instructions );
		  System.out.format("\ttotal CPI %.2f\n", (float) cache.executionTime / instructions);
		  System.out.format("\taverage memory access time %.2f cycles\n",  (float) cache.mempencycles / (i));
		  System.out.format("dirty evictions %d\n", cache.dirtyEvictions);
		  System.out.format("load_misses %d\n", cache.loadMisses);
		  System.out.format("store_misses %d\n", cache.storeMisses);
		  System.out.format("load_hits %d\n", cache.loadHits);
		  System.out.format("store_hits %d\n", cache.storeHits);
		  
	}

}
