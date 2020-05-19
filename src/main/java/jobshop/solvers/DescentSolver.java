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


    @Override
    public Result solve(Instance instance, long deadline) {

    	
        Schedule sol_ini = new GreedySolver(Priority.EST_LRPT).solve(instance, deadline).schedule;  
              
        
        boolean found_best = true;
        // on continue a chercher tant que la solution s'améliore
        while (found_best ) {
        	
        	if(System.currentTimeMillis() > deadline) {
    			return new Result(instance, sol_ini, Result.ExitCause.Timeout);
        	}else {
        		found_best = false;
                ResourceOrder best_order = new ResourceOrder(sol_ini);
                ResourceOrder test_order = new ResourceOrder(sol_ini);

                
            	List<Block> block_list = blocksOfCriticalPath(best_order);
                
                for (int i=0 ; i<block_list.size(); i++) {
                	
                	Block block = block_list.get(i);
                	System.out.println("first");
                	System.out.println(block.firstTask);
                	System.out.println("last");
                	System.out.println(block.lastTask);
                	
                	List<Swap> neighbors_list = neighbors(block);
                	System.out.println(neighbors_list.size());
                	
                	for (int n = 0; n< neighbors_list.size(); n++) {
                    	

                		neighbors_list.get(n).applyOn(test_order);
                		
                		int new_makespan = test_order.toSchedule().makespan();
                		//si le makespan est meilleur on update la solution
            			if(new_makespan < sol_ini.makespan() ) {
            				sol_ini= test_order.toSchedule();
            				found_best = true;
            			}else {
            				//sinon on revient a la solution 
                    		neighbors_list.get(n).applyOn(test_order);

            			}

                			
                	}
                }
        	}
        	
        }
        
        
	    return new Result(instance, sol_ini, Result.ExitCause.Timeout);
    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {
		 Schedule s = order.toSchedule();
		 List<Task> path = s.criticalPath();
	     List<Block> blocks = new ArrayList<Block>();
	     
	     //block initial
	     Task task_0 = path.get(0);
	     int current_machine = order.instance.machine(task_0);
	     int first = Arrays.asList(order.tasksByMachine[current_machine]).indexOf(task_0);
	     int last = first;
	     
	     for (int i = 0; i < path.size(); i++) {
	    	 Task t =path.get(i);
	    	 if(order.instance.machine(t) == current_machine) {
	    		 last ++;
	    	 }else {
	    		 if (first != last) {
	    			 blocks.add(new Block(current_machine,first,last));
	    		 }
	    		 current_machine = order.instance.machine(t);
	    		 
	    		 first =  Arrays.asList(order.tasksByMachine[current_machine]).indexOf(t);
	    		 last = first;
	    	 }
	    	 
	     }
	     return blocks;

    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
    			
	  List<Swap> swaps = new ArrayList<Swap>();
	  int size_block = block.lastTask - block.firstTask;
	  for (int i = 0; i<size_block; i++) { 
		  swaps.add(new Swap(block.machine,block.firstTask + i, block.firstTask + (i+1))); }
	  
	  return swaps;
		   
    }

}
