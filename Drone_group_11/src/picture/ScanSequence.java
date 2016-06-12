package picture;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import helper.Move;

public class ScanSequence implements Runnable {
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
	private Mat camMat;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	
	public ScanSequence(CommandController commandController) {
		this.commandController = commandController;
	}
	
	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}
	
	@Override
	public void run() {
		commandController.dC.takeOff();
		sleep(2000);
		System.out.println("HOVER");
		commandController.dC.hover();
		sleep(6000);
		
		System.out.println("UP");
		commandController.dC.setSpeed(20);
		commandController.addCommand(Command.UP, 2000);
		sleep(2000);
		
		System.out.println("HOVER");
		commandController.dC.hover();
		sleep(6000);

	
		System.out.println("UP");
		commandController.dC.setSpeed(20);
		commandController.addCommand(Command.UP, 2000);
		sleep(2000);
		
		while(PictureController.shouldScan) {
			scanSequence();
		}
	}
	
	private void scanSequence() {
		List<Mat> contours = OFC.findQrContours(camMat);
		
		if (contours.size() == 0) {
			if (frameCount < 20) {
				frameCount++;
				return;
			} else {
				System.out.println("HOVER");
				commandController.dC.hover();
				sleep(4000);
				if (rotateCount < 15) {
					//#TODO Rotate 90 degrees
					System.out.println("ROTATE");
					commandController.dC.setSpeed(10);
					commandController.addCommand(Command.SPINRIGHT, 3500);
					sleep(2500);
					rotateCount++;
				} else {
					//#TODO Fly forwards (1 meter)
					System.out.println("Forward");
					commandController.dC.setSpeed(20);
					commandController.addCommand(Command.FORWARD, 1500);
					sleep(1000);
					rotateCount = 0;
				}
			}
		}
		
		frameCount = 0;
		boolean wallClose = false;		
		if (wallClose) {
			//#TODO Fly backwards (4-5 meters)
			//#TODO Rotate 90 degrees
			commandController.addCommand(Command.BACKWARDS, 2000);
			sleep(2000);
			commandController.addCommand(Command.SPINRIGHT, 1500);
			sleep(1500);
			return;
		}

		double distanceFomCenter = 5000;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			 RotatedRect rect2 = minAreaRect(contours.get(i));
			 double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			 if (distanceFomCenter > distance && OFC.checkAngles(rect2)) {
				 distanceFomCenter = Math.abs(distance);
				 rect = rect2;
			 }
		}
		
		double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(4000);
			System.out.println("PositionFromCenter: " + positionFromCenter);
			//#TODO Rotate <positionFromCenter> pixels to center the QR code in image
			commandController.dC.setSpeed(15);
			if (positionFromCenter > 0) {
				System.out.println("SPINRIGHT");
				commandController.addCommand(Command.SPINRIGHT, 300);
				sleep(1000);
			} else {
				System.out.println("SPINLEFT");
				commandController.addCommand(Command.SPINLEFT, 300);
				sleep(1000);
			}
			return;
		}
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(4000);
			//#TODO Strafe the drone <center> amount. Right is chosen as standard.
			commandController.dC.setSpeed(20);
			if (strafeRight) {
				System.out.println("STRAFERIGHT");
				commandController.addCommand(Command.RIGHT, 650);
				sleep(1000);
			} else {
				System.out.println("STRAFELEFT");
				commandController.addCommand(Command.LEFT, 650);
				sleep(1000);
			}
			if (previousCenter == -1) {
				// Record center in order to react to it next iteration
				previousCenter = center;
			} else {
				double difference = center - previousCenter;
				System.out.println("PREV " + previousCenter);
				System.out.println("CENTER " + center);
				if (difference > CENTER_DIFFERENCE) {
					// We moved the wrong way. Change strafe direction.
					strafeRight = !strafeRight;
					previousCenter = center;
					System.out.println("CHANGE STRAFE DIRECTION");
				}
			}
			return;
		}
		// Reset the previous center
		previousCenter = -1;
		
		Mat qrImg = OFC.warpImage(camMat.clone(), rect);
		
		String tempCode = OFC.scanQrCode(qrImg);

		if (tempCode != null) {
//				// Code stored as field, we need to use it even if we're too far away to scan it.
//				//#TODO Ensure that the field code is set to null every time we need to reset.  
			code = tempCode;
			System.out.println(tempCode);
		}
		System.out.println("CENTERED");
		if(code != null) {
			// Check amount of squares found
			// #TODO Implement some way to check squares across more than one frame
			if (contours.size() == 3) {
				//#TODO Calculate distance and placement in coordinate system
				CustomPoint placement = new CustomPoint();
				moveDroneToStart(placement);
				code = null;
				rotateCount = 0;
				PictureController.shouldScan = false;
				return;
			} else {
				System.out.println("HOVER");
				commandController.dC.hover();
				sleep(4000);
				//#TODO Fly backwards (0.5 meters)
				commandController.addCommand(Command.BACKWARDS, 500);
				sleep(500);
				return;
			}
		} else {
			double distanceToSquare = OFC.calcDistance(rect);
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(4000);
			// It might still be a QR code, we're too far away to know
			if (distanceToSquare > 100) {
				//#TODO Fly closer to the square (0.5 meters)
				commandController.addCommand(Command.FORWARD, 500);
				sleep(500);
				return;
			} else {
				//#TODO Fly backwards (4-5 meters)
				//#TODO Rotate 90 degrees
				commandController.addCommand(Command.BACKWARDS, 2000);
				sleep(2000);
				commandController.addCommand(Command.SPINRIGHT, 1500);
				sleep(1500);
				return;
			}
		}
	}
	
	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void moveDroneToStart(CustomPoint placement) {
		List<Move> moves = OFC.calcMoves(placement.getX(), placement.getY());
		for (Move move : moves) {
			//#TODO Actually move the drone according to moves
		}
	}
}
