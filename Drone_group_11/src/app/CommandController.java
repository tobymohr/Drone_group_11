package app;
import java.util.concurrent.LinkedBlockingDeque;

import de.yadrone.base.IARDrone;
import helper.Command;

public class CommandController implements Runnable {
	public DroneInterface droneInterface;
	private LinkedBlockingDeque<Task> q;
	private long startTime = 0;
	public static boolean wait = false;
	private Task task;
	public static final int COUNTER_FACTOR = 6;
	public static final int COUNTERSPEED_FACTOR = 7;
	public static boolean droneIsReady = false;
	public static String moveString = "";

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
		droneInterface = new DroneCommunicator(drone);
		droneInterface.setBottomCamera();
		q = new LinkedBlockingDeque<Task>();
	}

	@Override
	public void run() {
		while(true){
			try {
				synchronized(this){
					while(wait || q.peek()==null){
						Thread.sleep(50);
					}

					task = q.take();
					droneIsReady = false;
					droneInterface.setSpeed(task.speed);
					switch(task.task){
					
					//Go commands
					case Command.FORWARD:
						startTime = System.currentTimeMillis();
						droneInterface.goForward(task.time);
						System.out.println("FORWARD");
						moveString = "FORWARD";
						break;
					case Command.BACKWARDS:
						System.out.println("BACKWARDS");
						moveString = "BACKWARDS";
						startTime = System.currentTimeMillis();
						droneInterface.goBackwards(task.time);
						break;
					case Command.LEFT:
						startTime = System.currentTimeMillis();
						droneInterface.goLeft(task.time);
						System.out.println("LEFT");
						moveString = "LEFT";
						break;
					case Command.RIGHT:
						startTime = System.currentTimeMillis();
						droneInterface.goRight(task.time);
						System.out.println("RIGHT");
						moveString = "RIGHT";
						break;
					case Command.UP:
						startTime = System.currentTimeMillis();
						droneInterface.goUp(task.time);
						System.out.println("UP");
						moveString = "UP";
						break;
					case Command.DOWN:
						startTime = System.currentTimeMillis();
						droneInterface.goDown(task.time);
						System.out.println("DOWN");
						moveString = "DOWN";
						break;
						//Rotate commands
					case Command.SPINLEFT:
						startTime = System.currentTimeMillis();
						droneInterface.spinLeft(task.time);
						System.out.println("SPIN LEFT");
						moveString = "SPIN LEFT";
						break;
					case Command.SPINRIGHT:
						startTime = System.currentTimeMillis();
						droneInterface.spinRight(task.time);
						System.out.println("SPIN RIGHT");
						moveString = "SPIN RIGHT";
						break;
					case Command.ROTATERIGHT:
						startTime = System.currentTimeMillis();
						droneInterface.spinRight(task.time);
						System.out.println("ROTATE RIGHT");
						moveString = "ROTATE RIGHT";
						break;
					case Command.ROTATELEFT:
						startTime = System.currentTimeMillis();
						droneInterface.spinLeft(task.time);
						System.out.println("ROTATE LEFT");
						moveString = "ROTATE LEFT";
						break;
						//Take off and land
					case Command.TAKEOFF:
						droneInterface.takeOff();
						break;
					case Command.LAND:
						droneInterface.land();
						break;
					case Command.NONE: 
						droneInterface.hover();
						break;
					}
					Thread.sleep(task.time);
					System.out.println("TASK SPEED " + task.speed );
					droneInterface.hover();
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
		droneInterface.emergencyStop();
	}
	
	public boolean isDroneReady() {
		return droneIsReady;
	}
	
	

	public void hover(Boolean con, int duration){
		long difference = System.currentTimeMillis() - startTime;
		wait = true;
		synchronized(this){
			droneInterface.hover();
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