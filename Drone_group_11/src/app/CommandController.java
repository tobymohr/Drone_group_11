package app;
import java.util.concurrent.LinkedBlockingDeque;

import de.yadrone.base.IARDrone;
import helper.Command;

public class CommandController implements Runnable {
	public DroneInterface dC;
	private LinkedBlockingDeque<Task> q;
	private long startTime = 0;
	public static boolean wait = false;
	private Task task;
	public static final int COUNTER_FACTOR = 6;
	public static final int COUNTERSPEED_FACTOR = 7;
	public static boolean droneIsReady = false;

	class Task{
		public int time;
		public int task;
		public int speed;
		public Task(int task, int time, int speed){
			this.task = task;
			this.time = time;
			this.speed = speed;
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
					while(wait || q.peek()==null){
						try {
							dC.hover();
							System.out.println("HOVER");
							wait(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					task = q.take();
					droneIsReady = false;
					dC.setSpeed(task.speed);
					switch(task.task){
					
					//Go commands
					case Command.FORWARD:
						startTime = System.currentTimeMillis();
						dC.goForward(task.time);
						System.out.println("Forward");
						break;
					case Command.BACKWARDS:
						startTime = System.currentTimeMillis();
						dC.goBackwards(task.time);
						break;
					case Command.LEFT:
						startTime = System.currentTimeMillis();
						dC.goLeft(task.time);
						System.out.println("LEFT");
						break;
					case Command.RIGHT:
						startTime = System.currentTimeMillis();
						dC.goRight(task.time);
						System.out.println("RIGHT");
						break;
					case Command.UP:
						startTime = System.currentTimeMillis();
						dC.goUp(task.time);
						System.out.println("UP");
						break;
					case Command.DOWN:
						startTime = System.currentTimeMillis();
						dC.goDown(task.time);
						System.out.println("DOWN");
						break;
						//Rotate commands
					case Command.SPINLEFT:
						startTime = System.currentTimeMillis();
						dC.spinLeft(task.time);
						System.out.println("SPIN LEFT");
						break;
					case Command.SPINRIGHT:
						startTime = System.currentTimeMillis();
						dC.spinRight(task.time);
						System.out.println("SPIN RIGHT");
						break;
					case Command.ROTATERIGHT:
						startTime = System.currentTimeMillis();
						dC.spinRight(task.time);
						System.out.println("ROTATE RIGHT");
						break;
					case Command.ROTATELEFT:
						startTime = System.currentTimeMillis();
						dC.spinLeft(task.time);
						System.out.println("ROTATE LEFT");
						break;
						//Take off and land
					case Command.TAKEOFF:
						dC.takeOff();
						break;
					case Command.LAND:
						dC.land();
					}
					
					wait(task.time);
					
					System.out.println("TIME " + (int) (task.time) + " SPEED " + (task.speed));
					
//					if(task.task == Command.ROTATELEFT){
//						System.out.println("COUNTER SPIN RIGHT");
//						dC.setSpeed((int)(task.speed/COUNTERSPEED_FACTOR));
//						dC.spinRight((int)(task.time/COUNTER_FACTOR));
//						wait((int)(task.time/COUNTER_FACTOR));
//					}
//					
//					if(task.task == Command.ROTATERIGHT){
//						System.out.println("COUNTER SPIN LEFT");
//						dC.setSpeed((int)(task.speed/COUNTERSPEED_FACTOR));
//						dC.spinLeft((int)(task.time/COUNTER_FACTOR));
//						wait((int)(task.time/COUNTER_FACTOR));
//					}
//					
//					dC.hover();
//					wait(1500);
					
//					System.out.println("TIME " + (int) (task.time/COUNTER_FACTOR) + " SPEED " + (task.speed/COUNTER_FACTOR));
					
					droneIsReady = true;
					
				
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}


	}


	public void addCommand(int c, int t, int speed){
		q.offer(new Task(c,t, speed));
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