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

	
	public enum Priority {SPT,LPT,SRPT, LRPT,EST_SPT,EST_LRPT};
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
		
        // indicate for each task that have been scheduled, its start time
		start_times = new int [instance.numJobs][instance.numTasks];
		
        // for each machine, earliest time at which the machine can be used
        releaseTimeOfMachine = new int[instance.numMachines];


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
            
	    }else {
	    	return todo.get(0);
	    }
	         
	}
	
	//• SPT (Shortest Processing Time) : donne priorité à la tâche la plus courte ;

	private Task getSPT(ArrayList<Task> tasks, Instance instance) {
		int time = Integer.MAX_VALUE;
		Task chosen_task = null;
		for(Task t:tasks) {
			if (instance.duration(t)< time) {
				chosen_task = t;
			}
		}
		return chosen_task;
	}
	
	//• LPT (Longest Processing Time) : donne priorité à la tâche la plus longue ;

	private Task getLPT(ArrayList<Task> tasks, Instance instance) {
		int time = Integer.MIN_VALUE;
		Task chosen_task = null;
		for(Task t:tasks) {
			if (instance.duration(t)> time) {
				chosen_task = t;
			}
		}
		return chosen_task;
	}

	//SRPT (Shortest Remaining Processing Time) : donne la priorité à la tâche appartenant au job ayant la plus petite durée

		private Task getSRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
			
			int mintime = Integer.MAX_VALUE;
			int dureejob=0;
			int job=-1;
	        Task taskMin = null;
			
	        for (Task t : tasks){
				
				int j=t.job;
				
	            for(int k=0; k< instance.numTasks; k++){
					
					if (!done.contains(new Task(j,k))) {
						dureejob = dureejob+instance.duration(j,k);
					}
				}
				
				if (dureejob != 0 && dureejob < mintime){
	                mintime = dureejob;
	                dureejob = 0;
	                job = j;
	            }
	        }
			
	        for (Task t : tasks){
	            if (t.job == job) {
	                taskMin = t;
	                break;
	            }
	        }
			
	        
	        return taskMin;
			
		}
	
	//LRPT (Longest Remaining Processing Time) : donne la priorité à la tâche appartenant au job ayant la plus grande durée

	private Task getLRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
	
				
		int[] dureejobs = new int[instance.numJobs];
		
		for(int i = 0; i<instance.numJobs;i++) {
        	int dureejob=0;
        	for (int j =0; j<instance.numTasks; j++) {
        		dureejob += instance.duration(i,j);
        	}
        	dureejobs[i] = dureejob;
        	//System.out.println("durée job : " + dureejob);
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

		int time = Integer.MAX_VALUE;
		Task chosen_task = null;
		for(Task t:beginners) {
			if (instance.duration(t)< time) {
				chosen_task = t;
			}
		}
		return chosen_task;
	}
	
private Task getEST_LRPT(ArrayList<Task> tasks, Instance instance, ArrayList<Task> done) {
		//System.out.println(tasks.size());
		ArrayList<Task> beginners = selectEST(tasks,instance);
		//System.out.println(beginners.size());
		Task taskMin = getLRPT(beginners, instance, done);
	
		
        
        return taskMin;
		
	}
	
	
	
	private ArrayList<Task> selectEST(ArrayList<Task> todo, Instance instance){
		
		
		 ArrayList<Task> tachesFaiseablesEST = new ArrayList<Task>();

	        int EST = Integer.MAX_VALUE;
	      
		  
	        for (Task t : todo){
	            int machine = instance.machine(t.job, t.task);
	    
	            int test=0;
				
	            if(t.task == 0){
	                test = 0;
	            } else{
	                test = start_times[t.job][t.task-1] + instance.duration(t.job, t.task-1);
	            }
	            test = Math.max(test, releaseTimeOfMachine[machine]);
	            start_times[t.job][t.task] = test;

	            if (test < EST){
	                EST = test;
	            }
	        }


	        for (Task t : todo){
	            if (start_times[t.job][t.task] == EST){
	                tachesFaiseablesEST.add(t);
	            }
	        }
	        return tachesFaiseablesEST;
	}
}



