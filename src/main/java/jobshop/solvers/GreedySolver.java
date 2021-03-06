package jobshop.solvers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class GreedySolver implements Solver{

	
	public enum Priority {SPT,LPT,SRPT, LRPT,EST_SPT,EST_LRPT,EST_LPT,EST_SRPT};
	private Priority priority;
	
	private int start_times[][];
	private int releaseTimeOfMachine[] ;

	
	public GreedySolver(Priority priority){
        this.priority = priority;
    }
	
	@Override
	public Result solve(Instance instance, long deadline) {
		// TODO Auto-generated method stub
		ResourceOrder r = new ResourceOrder(instance);
		
		ArrayList<Task> task_todo = new ArrayList<Task>();
		ArrayList<Task> task_done = new ArrayList<Task>();
		
       


		// initialisation de task_todo
		
		for (int j=0; j<instance.numJobs;j++) {
			task_todo.add(new Task(j,0));
		}
				
		while(task_todo.size() != 0) {
			int wanted_machine = 0;

			Task chosen_task = select(task_todo,instance, task_done);
			wanted_machine = instance.machine(chosen_task.job, chosen_task.task);
			int job_num = 0;
			try {
			while(r.tasksByMachine[wanted_machine][job_num] != null) {
					job_num ++;
				}
			}catch(Exception e) {e.printStackTrace();}
			r.tasksByMachine[wanted_machine][job_num] = chosen_task;
			//update des task realisables
			task_done.add(chosen_task);
			task_todo = update_tasks(task_done,instance);
			
		}
		
		 Schedule res = r.toSchedule();
	     return new Result(instance, res, Result.ExitCause.Timeout);
		
	}
	
	private ArrayList<Task> update_tasks(ArrayList<Task> done, Instance instance){
		
	ArrayList<Task> tasks = new ArrayList<>();
	      
       for(int i=0; i < instance.numTasks; i++){
            for(int j=0; j < instance.numJobs; j++) {
                if (!done.contains(new Task(j,i))){ 
                    if(i == 0 || done.contains(new Task(j, i - 1))){
                        tasks.add(new Task(j,i));
                    }
                }
            }
       }
    return tasks;
	}
	
	private Task select(ArrayList<Task> todo,Instance instance, ArrayList<Task> done) {
		
		if ((this.priority).equals(Priority.SPT)){     
			return getSPT(todo,instance);
	        
	    }else if ((this.priority).equals(Priority.LPT)) {
	            return getLPT(todo,instance);
	            
	    }else if ((this.priority).equals(Priority.SRPT)) {
            return getSRPT(todo,instance,done);
            
	    }else if ((this.priority).equals(Priority.LRPT)) {
            return getLRPT(todo,instance,done);
            
	    }else if ((this.priority).equals(Priority.EST_SPT)) {
            return getEST_SPT(todo,instance);
            
	    }else if ((this.priority).equals(Priority.EST_LRPT)) {
            return getEST_LRPT(todo,instance,done);
            
	    }else if ((this.priority).equals(Priority.EST_LPT)) {
            return getEST_LPT(todo,instance);
            
        }else if ((this.priority).equals(Priority.EST_SRPT)) {
            return getEST_SRPT(todo,instance,done);
            
        }else {
	    	return todo.get(0);
	    }
	         
	}
	
	//� SPT (Shortest Processing Time) : donne priorit� � la t�che la plus courte ;

	private Task getSPT(ArrayList<Task> tasks, Instance instance) {
		int time = Integer.MAX_VALUE;
		Task chosen_task = tasks.get(0);
		for(Task t:tasks) {
			if (instance.duration(t)< time) {
				chosen_task = t;
				time = instance.duration(t);
			}
		}
		return chosen_task;
	}
	
	//� LPT (Longest Processing Time) : donne priorit� � la t�che la plus longue ;

	private Task getLPT(ArrayList<Task> tasks, Instance instance) {
		int time = Integer.MIN_VALUE;
		Task chosen_task = tasks.get(0);
		for(Task t:tasks) {
			if (instance.duration(t)> time) {
				chosen_task = t;
				time = instance.duration(t);
			}
		}
		return chosen_task;
	}

	//SRPT (Shortest Remaining Processing Time) : donne la priorit� � la t�che appartenant au job ayant la plus petite dur�e

		private Task getSRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
			
			int[] dureejobs = new int[instance.numJobs];
			
			for(int i = 0; i<instance.numJobs;i++) {
	        	int dureejob=0;
	        	for (int j =0; j<instance.numTasks; j++) {
	        		dureejob += instance.duration(i,j);
	        	}
	        	dureejobs[i] = dureejob;
	        	//System.out.println("dur�e job : " + dureejob);
	        }
	        
	        int minValue = Integer.MAX_VALUE; 
	        int index_min = 0;
	        
	        for(int i=0;i < dureejobs.length;i++){ 
	          if(dureejobs[i] < minValue){ 
	             minValue = dureejobs[i]; 
	             index_min = i;
	          } 
	        } 
	       // System.out.println("max value :"+ maxValue);
	        //System.out.println("index max "+ index_max);
	       // int longerjob = Arrays.asList(dureejobs).indexOf(maxValue);
	        
	        Task taskMin = tasks.get(0);
	        
	        for(Task t : tasks) {
	        	//System.out.println("numjob tache" + t.job);
	        	if(t.job == index_min) {
	        		taskMin = t;
	        		break;
	        	}
	        	
	        }
			
	        
	        return taskMin;
			
		}
	
	//LRPT (Longest Remaining Processing Time) : donne la priorit� � la t�che appartenant au job ayant la plus grande dur�e

	private Task getLRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
	
				
		int[] dureejobs = new int[instance.numJobs];
		
		for(int i = 0; i<instance.numJobs;i++) {
        	int dureejob=0;
        	for (int j =0; j<instance.numTasks; j++) {
        		dureejob += instance.duration(i,j);
        	}
        	dureejobs[i] = dureejob;
        	//System.out.println("dur�e job : " + dureejob);
        }
        
        int maxValue = Integer.MIN_VALUE; 
        int index_max = 0;
        
        for(int i=0;i < dureejobs.length;i++){ 
          if(dureejobs[i] > maxValue){ 
             maxValue = dureejobs[i]; 
             index_max = i;
          } 
        } 
       // System.out.println("max value :"+ maxValue);
        //System.out.println("index max "+ index_max);
       // int longerjob = Arrays.asList(dureejobs).indexOf(maxValue);
        
        Task taskMin = tasks.get(0);
        
        for(Task t : tasks) {
        	//System.out.println("numjob tache" + t.job);
        	if(t.job == index_max) {
        		taskMin = t;
        		break;
        	}
        	
        }
                
        return taskMin;
		
	}
	
	private Task getEST_SPT(ArrayList<Task> tasks, Instance instance) {
		
		ArrayList<Task> beginners = selectEST(tasks,instance);

		return getSPT(beginners, instance);
	}
	
	
private Task getEST_LPT(ArrayList<Task> tasks, Instance instance) {
		
		ArrayList<Task> beginners = selectEST(tasks,instance);

		return getLPT(beginners, instance);
		
	}
private Task getEST_SRPT(ArrayList<Task> tasks, Instance instance,ArrayList<Task> done) {
	
	ArrayList<Task> beginners = selectEST(tasks,instance);

	return getSRPT(beginners, instance,done);
}
private Task getEST_LRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
		//System.out.println(tasks.size());
		ArrayList<Task> beginners = selectEST(tasks,instance);
		//System.out.println(beginners.size());
		Task taskMin = getLRPT(beginners, instance, done);
     
        return taskMin;
		
	}
	
	
	
	private ArrayList<Task> selectEST(ArrayList<Task> todo, Instance instance){
		
		 // indicate for each task that have been scheduled, its start time
		this.start_times = new int [instance.numJobs][instance.numTasks];
		
        // for each machine, earliest time at which the machine can be used
        this.releaseTimeOfMachine = new int[instance.numMachines];
		

		ArrayList<Task> beginners = new ArrayList<Task>();
		int EST = Integer.MAX_VALUE;

		for (Task t : todo) {
			
			 // compute the earliest start time (est) of the task
			int est;
			if(t.task == 0) {
			  est = 0;
			} else {
			 est = this.start_times[t.job][t.task-1] + instance.duration(t.job, t.task-1);
			}
	        est = Math.max(est, releaseTimeOfMachine[instance.machine(t)]);
	        
	        this.start_times[t.job][t.task] = est;
	        
	        // increase the release time of the machine
	        releaseTimeOfMachine[instance.machine(t)] = est + instance.duration(t.job, t.task);
	        
	        if (est<EST) {
	        	EST = est;
	        }
		}
		//System.out.println("est : "+EST+ "prio : "+this.priority);
		for(Task t : todo) {
			if (start_times[t.job][t.task] == EST) {
				beginners.add(t);
			}
		}
		
		
		return beginners;
	}
}



