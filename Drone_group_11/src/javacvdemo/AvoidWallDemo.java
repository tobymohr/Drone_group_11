package javacvdemo;

import org.bytedeco.javacpp.opencv_core.Mat;

import app.CommandController;
import helper.Command;
import javafx.scene.control.Label;
import picture.PictureProcessingHelper;

public class AvoidWallDemo implements Runnable {

	private static final int FORWARD_TIME_2 = 500;
	private static final int BACKWARD_TIME = 500;
	private static final int BACKWARD_SPEED = 15;
	private static final int STRAFE_TIME = 650;
	private static final int STRAFE_SPEED = 20;
	private static final int SPIN_TIME = 300;
	private static final int SPIN_SPEED = 15;
	private static final int FORWARD_TIME = 1500;
	private static final int FORWARD_SPEED = 20;
	private static final int ROTATE_TIME = 4500;
	private static final int ROTATE_SPEED = 18;
	private static final int HOVER_TIME = 2000;
	//#TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.1;
	public static final double CENTER_LOWER = -0.1;
	public static final double CENTER_DIFFERENCE = 0.05;		
	private CommandController commandController;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int foundFrameCount = 0;
	private Mat camMat;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private Label qrCode;
	private Label qrDist;
	
	public AvoidWallDemo(CommandController cC){
		this.commandController = cC;
	}
	
	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}
	
	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		commandController.droneInterface.takeOff();
		
		sleep(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(6000);
		System.out.println("UP");
		commandController.droneInterface.setSpeed(5);
		commandController.addCommand(Command.UP, 2000, 10);
		sleep(2000);
		
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(6000);
		while(true){
			commandController.addCommand(Command.FORWARD, 250, 5);
			sleep(2000);
			if(OFC.getDistance() <= 200)
				break;
			commandController.droneInterface.hover();
			sleep(500);
		}
		System.out.println("OUT");
		commandController.droneInterface.land();
		
	}
}
