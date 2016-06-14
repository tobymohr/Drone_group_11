package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import helper.Move;

public class ScanSequence implements Runnable {
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
				code = null;
				System.out.println("HOVER");
				commandController.dC.hover();
				sleep(HOVER_TIME);
				if (rotateCount < 15) {
					//#TODO Rotate 90 degrees
					System.out.println("ROTATE");
					commandController.dC.setSpeed(ROTATE_SPEED);
					commandController.addCommand(Command.SPINRIGHT, ROTATE_TIME);
					sleep(ROTATE_TIME);
					rotateCount++;
				} else {
					//#TODO Fly forwards (1 meter)
					System.out.println("Forward");
					commandController.dC.setSpeed(FORWARD_SPEED);
					commandController.addCommand(Command.FORWARD, FORWARD_TIME);
					sleep(FORWARD_TIME);
					rotateCount = 0;
				}
			}
		}
		
		if (foundFrameCount < 2) {
			foundFrameCount++;
			return;
		}
		
		frameCount = 0;
		foundFrameCount = 0;
		boolean wallClose = false;		
		if (wallClose) {
			//#TODO Fly backwards (4-5 meters)
			//#TODO Rotate 90 degrees
			return;
		}

		double distanceFomCenter = 5000;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			 RotatedRect rect2 = minAreaRect(contours.get(i));
			 double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			 if (distanceFomCenter > distance) {
				 distanceFomCenter = Math.abs(distance);
				 rect = rect2;
			 }
		}
		
		
		
		double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(HOVER_TIME);
			//#TODO Rotate <positionFromCenter> pixels to center the QR code in image
			int speed = SPIN_SPEED;
			int newSpeed = OFC.getSpinSpeed(contours);
			if(newSpeed > 0){
				speed = newSpeed; 
			}
			
			commandController.dC.setSpeed(speed);
			if (positionFromCenter > 0) {
				System.out.println("SPINRIGHT");
				commandController.addCommand(Command.SPINRIGHT, SPIN_TIME);
				sleep(SPIN_TIME);
			} else {
				System.out.println("SPINLEFT");
				commandController.addCommand(Command.SPINLEFT, SPIN_TIME);
				sleep(SPIN_TIME);
			}
			return;
		}
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(HOVER_TIME);
			//#TODO Strafe the drone <center> amount. Right is chosen as standard.
			commandController.dC.setSpeed(STRAFE_SPEED);
			if (strafeRight) {
				System.out.println("STRAFERIGHT");
				commandController.addCommand(Command.RIGHT, STRAFE_TIME);
				sleep(STRAFE_TIME);
			} else {
				System.out.println("STRAFELEFT");
				commandController.addCommand(Command.LEFT, STRAFE_TIME);
				sleep(STRAFE_TIME);
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
				CustomPoint placement = calculatePlacement(camMat, contours);
				System.out.println(placement.getX() + "|" + placement.getY());
				moveDroneToStart(placement);
				code = null;
				rotateCount = 0;
				PictureController.shouldScan = false;
				return;
			} else {
				System.out.println("HOVER");
				commandController.dC.hover();
				sleep(HOVER_TIME);
				//#TODO Fly backwards (0.5 meters)
				commandController.dC.setSpeed(BACKWARD_SPEED);
				commandController.addCommand(Command.BACKWARDS, BACKWARD_TIME);
				sleep(BACKWARD_TIME);
				return;
			}
		} else {
			double distanceToSquare = OFC.calcDistance(rect);
			System.out.println("HOVER");
			commandController.dC.hover();
			sleep(HOVER_TIME);
			// It might still be a QR code, we're too far away to know
			if (distanceToSquare > 100) {
				//#TODO Fly closer to the square (0.5 meters)
				commandController.dC.setSpeed(FORWARD_SPEED);
				commandController.addCommand(Command.FORWARD, FORWARD_TIME_2);
				sleep(FORWARD_TIME_2);
				return;
			} else {
				//#TODO Fly backwards (4-5 meters)
				//#TODO Rotate 90 degrees
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
	
	private CustomPoint calculatePlacement(Mat srcImage, List<Mat> contours) {
		CustomPoint placement = new CustomPoint();
		List<Double> positions = new ArrayList<>();
		for (Mat mat : contours) {
			positions.add(OFC.isCenterInImage(srcImage, minAreaRect(mat)));
		}
		int minIndex = positions.indexOf(Collections.min(positions));
		int maxIndex = positions.indexOf(Collections.max(positions));
		int middleIndex = 3 - minIndex - maxIndex;
		
		RotatedRect leftQR = minAreaRect(contours.get(minIndex));
		RotatedRect middleQR = minAreaRect(contours.get(middleIndex));
		RotatedRect rightQR = minAreaRect(contours.get(maxIndex));
		
		CustomPoint[] points = OFC.calcPosition(OFC.calcDistance(leftQR), 
				OFC.calcDistance(middleQR), OFC.calcDistance(rightQR), code);
		CustomPoint scannedPoint = CustomPoint.parseQRText(code);
		for (CustomPoint e : points) {
			if (e.getX() != scannedPoint.getX() || e.getY() != scannedPoint.getY()) {
				placement = e;
			}
		}
		
		return placement;
	}
	
	private void moveDroneToStart(CustomPoint placement) {
		List<Move> moves = OFC.calcMoves(placement.getX(), placement.getY());
		for (Move move : moves) {
			//#TODO Actually move the drone according to movess
		}
	}
}
