package jobshop.solvers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.GreedySolver.Priority;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
        
        public String toString()
        {
        	return "[m=" + machine + ", [" + firstTask + ", " + lastTask + "]]";
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swap with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {  	
        	Task temp = order.tasksByMachine[this.machine][this.t1];
        	order.tasksByMachine[this.machine][this.t1] = order.tasksByMachine[this.machine][this.t2];
        	order.tasksByMachine[this.machine][this.t2] = temp;
        	
 
        }
    }


    public Result solve(Instance instance, long deadline) {

        Schedule sol_ini = new GreedySolver(Priority.EST_SPT).solve(instance, deadline).schedule;  
       
        ResourceOrder best_order = new ResourceOrder(sol_ini);
        
                
       // int debut_makespan = sol_ini.makespan(); 
       // System.out.println("debut");
        //System.out.println(debut_makespan);
        
        boolean found_best = true;
        // on continue a chercher tant que la solution s'améliore
        while (found_best ) {
        	
        	if(System.currentTimeMillis() > deadline) {
    			return new Result(instance, best_order.toSchedule() , Result.ExitCause.Timeout);
        	}else {
        		     
        		found_best = false;
            	List<Block> block_list = blocksOfCriticalPath(best_order);
            	
            	//System.out.print("\n" + block_list + "\n");
       	
            	int makespan = best_order.toSchedule().makespan();
            	ResourceOrder best_local = best_order;
            	int best_makespan = Integer.MAX_VALUE ; 

                for (int i=0 ; i<block_list.size(); i++) {
                	
                	Block block = block_list.get(i);
                	
                	List<Swap> neighbors_list = neighbors(block);
                	
                	
                	for (int n = 0; n< neighbors_list.size(); n++) {
                    	
                		ResourceOrder test_order = best_order.copy();
                		
                		Swap swap = neighbors_list.get(n);
                		swap.applyOn(test_order);

                		Schedule new_sched = test_order.toSchedule();
                		                    	                		
                		if(new_sched != null && new_sched.isValid()) {
                			
                			int new_makespan = new_sched.makespan();
                			
                			//si le makespan est meilleur on update la solution
                			if(new_makespan < best_makespan)  {
                				best_makespan = new_makespan;
                				best_local = test_order.copy();
                				found_best = true;
                				//System.out.println("better");
                		        //System.out.println(new_makespan);
                			}
                		            			            			
                		}
        			
                	}
               }
                
                if (best_makespan < makespan) {
                	best_order = best_local.copy();
                }
               
        	
        	}
        }
        
	    return new Result(instance, sol_ini, Result.ExitCause.Timeout);
	    
    }

    /** Returns a list of all blocks of the critical path. */
    static public List<Block> blocksOfCriticalPath(ResourceOrder order) {
    	
	  List<Task> path = order.toSchedule().criticalPath();
	  List<Block> blocks = new ArrayList<Block>();
	  
	  //ini
	  int current_machine = -1; 

	  int first = 0; 
	  int  last = 0;
	  
	  for(Task t : path) { 
		  //initialisation
		  if(current_machine == -1) {
			  current_machine = order.instance.machine(t);
			  first = Arrays.asList(order.tasksByMachine[current_machine]).indexOf(t); 
			  last = first; 
		  }else if(current_machine == order.instance.machine(t)) { 
			  last++; 
		  } else {
			  if(last != first) { 
				  blocks.add(new Block(current_machine,first,last)); 
				  }
			  current_machine = order.instance.machine(t); 
			  first = Arrays.asList(order.tasksByMachine[current_machine]).indexOf(t); 
			  last = first;
			  }
		  }
	
	  return blocks;

    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    static public List<Swap> neighbors(Block block) {
    			
	  List<Swap> swaps = new ArrayList<Swap>();
	  int size_block = block.lastTask - block.firstTask;
	  
	  if(size_block == 1) {
		  swaps.add(new Swap(block.machine,block.firstTask, block.lastTask)); 
	  }else {
		 swaps.add(new Swap(block.machine,block.firstTask, block.firstTask +1)); 
		  swaps.add(new Swap(block.machine,block.lastTask -1 , block.lastTask)); 
	  }
	  return swaps;
		
		 // for (int i = 0; i<size_block; i++) { swaps.add(new
		  //Swap(block.machine,block.firstTask + i, block.firstTask + (i+1))); }
		  
		 // return swaps;
		 
		   
    }

}
