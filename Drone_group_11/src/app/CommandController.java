package app;
import java.util.concurrent.LinkedBlockingDeque;

import de.yadrone.base.IARDrone;

public class CommandController implements Runnable {
	public DroneInterface dC;
	private LinkedBlockingDeque<Integer> q;
	private int task; //current task
	private int time;
	private long startTime = 0;
	private boolean wait = false;



	public CommandController(IARDrone drone) {
		dC = new DroneCommunicator(drone);
		dC.setBottomCamera();
		q = new LinkedBlockingDeque<Integer>();
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
					switch(task){
					//Go commands
					case 1:
						startTime = System.currentTimeMillis();
						dC.goForward(time);
						break;
					case 2:
						startTime = System.currentTimeMillis();
						dC.goBackwards(time);
						break;
					case 3:
						startTime = System.currentTimeMillis();
						dC.goLeft(time);
						break;
					case 4:
						startTime = System.currentTimeMillis();
						dC.goRight(time);
						break;
					case 5:
						startTime = System.currentTimeMillis();
						dC.goUp(time);
						break;
					case 6:
						startTime = System.currentTimeMillis();
						dC.goUp(time);
						break;
						//Rotate commands
					case 7:
						startTime = System.currentTimeMillis();
						dC.spinLeft(time);
						break;
					case 8:
						startTime = System.currentTimeMillis();
						dC.spinRight(time);
						break;
						//Take off and land
					case 9:
						dC.takeOff();
						break;
					case 10:
						dC.land();
					}
					wait(time);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}


	}

	public void setTime(int time) {
		this.time = time;
	}

	public void addCommand(int c){
		q.offer(c);
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
				
				if(difference < time){
					time = (int) difference;
					q.offerFirst(task);
				}
			}
		}
	}


}
