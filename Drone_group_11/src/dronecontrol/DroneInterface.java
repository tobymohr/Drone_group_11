package dronecontrol;

import de.yadrone.base.command.FlyingMode;
import de.yadrone.base.command.VideoChannel;

public interface DroneInterface {
	
	public void takeOff();
	public void land();
	public void freeze();
	public void emergencyStop();
	public void goLeft(int duration);
	public void goRight(int duration);
	public void goForward(int duration);
	public void goBackwards(int duration);
	public void goUp(int duration);
	public void goDown(int duration);
	public void spinLeft(int duration);
	public void spinRight(int duration);
	public void setSpeed(int speed);
	public void stop();
	public boolean getDroneFlying();
	public void setBottomCamera();
	public void setFrontCamera();
	public VideoChannel getVideoChannel();
	public void hover();
	public void setFlightMode(FlyingMode mode);
}
