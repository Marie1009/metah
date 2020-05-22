package jobshop.solvers;

import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.DescentSolver.Block;
import jobshop.solvers.DescentSolver.Swap;
import jobshop.solvers.GreedySolver.Priority;

public class TabooSolver implements Solver {
	
	private int maxIter;
	private int maxTime;
	private int[][] sTaboo ; 

	
	public TabooSolver(int maxIter, int maxTime) {
		super();
		this.maxIter = maxIter;
		this.maxTime = maxTime;
	}

   


    @Override
    public Result solve(Instance instance, long deadline) {

    	 this.sTaboo = new int[instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks] ;
         // int debut_makespan = sol_ini.makespan(); 
          //System.out.println("debut");
          //System.out.println(debut_makespan);

          for(int i = 0 ; i<(instance.numJobs * instance.numTasks) ; i++)
          {
              for(int j = 0 ; j<(instance.numJobs * instance.numTasks) ; j++)
              {
                  sTaboo[i][j] = 0;
              }
          }
    	GreedySolver first_solver = new GreedySolver(Priority.EST_SPT);
    	//RandomSolver first_solver = new RandomSolver();
    	Result first_soluce = first_solver.solve(instance, deadline);
        
    	//to return
    	Result best_current_soluce = first_soluce;
    	
    	Schedule current_schedule = best_current_soluce.schedule;
    	
    	ResourceOrder current_r_order = new ResourceOrder(current_schedule);
    	int current_makespan = current_schedule.makespan();
    	int current_makespan_taboo = current_makespan;
    	
    	boolean can_continue = true;
    	
    	int iter = 0;
    	
    	while(iter < maxIter && can_continue)
    	{
    		iter++;
    		can_continue = false;
    		
    		//when there is not non taboo solutions
    		boolean better_taboo_found = false;
    		
    		Swap best_swap = null;
    		
    		ResourceOrder local_taboo_r_order = current_r_order.copy();
    		
    		//when there is a non taboo solution
    		boolean makespan_swaps_is_not_initialized = true;
    		int current_makespan_swaps = -1;
    		boolean valid_swap_found = false;
    		
    		Swap best_swap_taboo = null;
    		ResourceOrder local_r_order = current_r_order.copy();
    		
    		//to cover all neighbors
    		List<Block> all_blocks = DescentSolver.blocksOfCriticalPath(current_r_order);
        	int numb_blocks = all_blocks.size();
        	
        	for(int b = 0 ; b < numb_blocks ; b++)
        	{        		
        		List<Swap> all_current_swaps = DescentSolver.neighbors(all_blocks.get(b));
        		int numb_swaps = all_current_swaps.size();
        		
        		for(int s = 0 ; s < numb_swaps ; s++)
        		{
        			//for this neighbor
        			ResourceOrder new_r_order = current_r_order.copy();
        			
        			Swap current_swap = all_current_swaps.get(s);
        			
        			
        			boolean is_taboo = check_taboo(current_swap, iter, new_r_order);
        			
        			
        			
        			current_swap.applyOn(new_r_order);
        			
        			Schedule new_schedule = new_r_order.toSchedule();
        			
        			//checking and choices
        			if(new_schedule != null)
        			{
        				if(new_schedule.isValid())
        				{
        					int new_makespan = new_schedule.makespan();
        					
        					if(!is_taboo)
                			{
        						//current_makespan_taboo already initialized with last makespan
        						//for each better neighbor (valid and taboo)
        						if(new_makespan < current_makespan_taboo)
            					{
            						local_taboo_r_order = new_r_order;
            						current_makespan_taboo = new_makespan;
            						better_taboo_found = true;
            						
            						best_swap_taboo = current_swap;
            						
            					}
                			}
                			else
                			{
                				//made only for the first neighbor (valid and non-taboo)
                				if(makespan_swaps_is_not_initialized)
                				{
                					current_makespan_swaps = new_makespan;
                					makespan_swaps_is_not_initialized = false;
                				}
                				
                				//for each better neighbor (valid and non-taboo)
                				if(new_makespan <= current_makespan_swaps)
            					{
            						local_r_order = new_r_order;
            						current_makespan_swaps = new_makespan;
            						valid_swap_found = true;
            						best_swap = current_swap;
            						
            					}
                			}

        				}
        			}//end checking
        			
        			
        		}//end one neighbor
        	}//end all neighbors
        	
        	can_continue = valid_swap_found || better_taboo_found;
        	
        	if(can_continue)
        	{
        		if(valid_swap_found)
        		{
        			current_r_order = local_r_order;
        			current_makespan = current_makespan_swaps;
        			addTaboo(best_swap, local_r_order, iter);
        		}
        		else
        		{
        			current_r_order = local_taboo_r_order;
        			current_makespan = current_makespan_taboo;
        			addTaboo(best_swap_taboo, local_taboo_r_order, iter);
        		}
        	}
        	
        	current_makespan_taboo = current_makespan;
        	
    	}//end while
    	
    	best_current_soluce = new Result(best_current_soluce.instance, current_r_order.toSchedule(), best_current_soluce.cause);
    	
    	return best_current_soluce;
    }
    
    private void addTaboo(Swap swap, ResourceOrder order, int k) {
		Task task1 = order.tasksByMachine[swap.machine][swap.t1] ; 
		Task task2 = order.tasksByMachine[swap.machine][swap.t2] ; 
		sTaboo[task2.job * order.instance.numTasks+task2.task][task1.job * order.instance.numTasks + task1.task] = k + this.maxTime ; 		
	}
    
    private boolean check_taboo(Swap swap, int k, ResourceOrder order) {
    	Task task1 = order.tasksByMachine[swap.machine][swap.t1] ; 
		Task task2 = order.tasksByMachine[swap.machine][swap.t2] ; 
		return k >= sTaboo[task1.job * order.instance.numTasks + task1.task][task2.job * order.instance.numTasks+task2.task] ;
    }



}
