package app;

import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.VideoChannel;


public class DroneCommunicator implements DroneInterface {
	private IARDrone drone;
	private boolean connectedToDrone = false;
	private boolean droneFlying = false;
	private CommandManager commandManager = null;
	private int speed = 10;
	private VideoChannel videoChannel;
	
	public DroneCommunicator(IARDrone drone){
		this.drone = drone;
		commandManager = drone.getCommandManager();
		
		connectedToDrone = true;
	}
	
	@Override
	public void takeOff() {
		if (connectedToDrone && !droneFlying) {
			commandManager.schedule(0, new Runnable() {				
				@Override
				public void run() {
					commandManager.flatTrim().doFor(1000);
					commandManager.takeOff().doFor(2000);
				}
			});
			droneFlying = true;
		}
	}
	
	@Override
	public void land() {
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {				
				@Override
				public void run() {
					commandManager.freeze(); //Hvad gør freeze?
					commandManager.landing();
				}
			});
			droneFlying = false;
		}
	}
	
	@Override
	public void freeze(){
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.freeze();
				}
			});
		}
	}
	
	@Override
	public void emergencyStop(){
		if (connectedToDrone) {
			commandManager.schedule(0, new Runnable() {
				public void run() {
					commandManager.emergency();
				}
			});
			droneFlying = false;
		}
	}

	@Override
	public void goLeft(int duration) {
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.goLeft(speed).doFor(duration);
				}
			});
		}
	}

	@Override
	public void goRight(int duration) {
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.goRight(speed).doFor(duration);
				}
			});
		}
	}

	@Override
	public void goForward(int duration) {
		commandManager.schedule(0, new Runnable() {
			@Override
			public void run() {
				commandManager.forward(speed).doFor(duration);
			}
		});
	}

	@Override
	public void goBackwards(int duration) {
		commandManager.schedule(0, new Runnable() {
			@Override
			public void run() {
				commandManager.backward(speed).doFor(duration);
			}
		});
	}

	@Override
	public void goUp(int duration) {
		commandManager.schedule(0, new Runnable() {
			@Override
			public void run() {
				commandManager.up(speed).doFor(duration);
			}
		});
	}

	@Override
	public void goDown(int duration) {
		commandManager.schedule(0, new Runnable() {
			@Override
			public void run() {
				commandManager.down(speed).doFor(duration);
			}
		});
	}

	@Override
	public void spinLeft(int duration) {
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.spinLeft(speed).doFor(duration);
				}
			});
		}
	}

	@Override
	public void spinRight(int duration) {
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.spinRight(speed).doFor(duration);
				}
			});
		}
	}
	
	@Override
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	@Override
	public boolean getDroneFlying() {
		return droneFlying;
	}
	
	@Override
	public void stop() {
		drone.stop();
	}
	
	@Override
	public void setBottomCamera() {
		commandManager.setVideoChannel(VideoChannel.VERT);
		videoChannel = VideoChannel.VERT;
	}
	
	@Override
	public void setFrontCamera() {
		commandManager.setVideoChannel(VideoChannel.HORI);
		videoChannel = VideoChannel.HORI;
	}

	@Override
	public VideoChannel getVideoChannel() {
		return videoChannel;
	}
	
	@Override
	public void hover(){
		if (connectedToDrone && droneFlying) {
			commandManager.schedule(0, new Runnable() {
				@Override
				public void run() {
					commandManager.hover();
				}
			});
		}
	}
	
}
