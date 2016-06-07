package app;
import java.util.concurrent.LinkedBlockingDeque;

import de.yadrone.base.IARDrone;

public class CommandController implements Runnable {
	public DroneInterface dC;
	private LinkedBlockingDeque<Task> q;
	private long startTime = 0;
	private boolean wait = false;
	private Task task;

	class Task{
		public int time;
		public int task;
		public Task(int task, int time){
			this.task = task;
			this.time = time;
		}
	}


	public CommandController(IARDrone drone) {
		dC = new DroneCommunicator(drone);
		dC.setBottomCamera();
		q = new LinkedBlockingDeque<Task>();
	}

	@Override
	public void run() {
		while(true){
			try {
				synchronized(this){
					while(wait){
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					task = q.take();
					switch(task.task){
					//Go commands
					case 1:
						startTime = System.currentTimeMillis();
						dC.goForward(task.time);
						break;
					case 2:
						startTime = System.currentTimeMillis();
						dC.goBackwards(task.time);
						break;
					case 3:
						startTime = System.currentTimeMillis();
						dC.goLeft(task.time);
						break;
					case 4:
						startTime = System.currentTimeMillis();
						dC.goRight(task.time);
						break;
					case 5:
						startTime = System.currentTimeMillis();
						dC.goUp(task.time);
						break;
					case 6:
						startTime = System.currentTimeMillis();
						dC.goUp(task.time);
						break;
						//Rotate commands
					case 7:
						startTime = System.currentTimeMillis();
						dC.spinLeft(task.time);
						break;
					case 8:
						startTime = System.currentTimeMillis();
						dC.spinRight(task.time);
						break;
						//Take off and land
					case 9:
						dC.takeOff();
						break;
					case 10:
						dC.land();
					}
					wait(task.time);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}


	}


	public void addCommand(int c, int t){
		q.offer(new Task(c,t));
	}

	public void emergencyStop(){
		dC.emergencyStop();
	}
	
	

	public void hover(Boolean con, int duration){
		long difference = System.currentTimeMillis() - startTime;
		wait = true;
		synchronized(this){
			dC.hover();
			try {
				wait(duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			wait = false;
			notify();
			if(con){
				
				if(difference < task.time){
					task.time = (int) difference;
					q.offerFirst(task);
				}
			}
		}
	}


}