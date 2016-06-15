package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_imgcodecs;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import helper.Move;

public class ScanSequence implements Runnable {
	private static final int FORWARD_TIME_2 = 500;
	private static final int BACKWARD_TIME = 500;
	private static final int BACKWARD_SPEED = 15;
	private static final int STRAFE_TIME = 650;
	private static final int STRAFE_SPEED = 10;
	private static final int SPIN_TIME = 300;
	private static final int SPIN_SPEED = 15;
	private static final int FORWARD_TIME = 1500;
	private static final int FORWARD_SPEED = 10;
	private static final int ROTATE_TIME = 4000;
	private static final int ROTATE_SPEED = 15;
	
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
	private int scannedCount = 0;
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
		commandController.addCommand(Command.UP, 2000, 20);
		sleep(2000);
		System.out.println("HOVER");
		commandController.dC.hover();
		sleep(6000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 2000, 20);
		sleep(2000);
		
		while(PictureController.shouldScan) {
			if (OFVideo.imageChanged) {
				scanSequence();
			} else {
				sleep(50);
			}
		}
	}
	
	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = OFC.findQrContours(camMat);
		
		if (contours.size() == 0) {
			if (frameCount < 20) {
				frameCount++;
				return;
			} else {
				code = null;
				if (rotateCount < 15) {
					//#TODO Rotate 90 degrees
					addCommand(Command.SPINRIGHT, ROTATE_TIME, ROTATE_SPEED);
					rotateCount++;
				} else {
					//#TODO Fly forwards (1 meter)
//					addCommand(Command.FORWARD, FORWARD_TIME, FORWARD_SPEED);
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

		double distanceFomCenter = Double.MAX_VALUE;
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
			//#TODO Rotate <positionFromCenter> pixels to center the QR code in image
			if (positionFromCenter > 0) {
				addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
			} else {
				addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
			}
			return;
		}
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			//#TODO Strafe the drone <center> amount. Right is chosen as standard.
			if (strafeRight) {
				addCommand(Command.RIGHT, STRAFE_TIME, STRAFE_SPEED);
			} else {
				addCommand(Command.LEFT, STRAFE_TIME, STRAFE_SPEED);
			}
			if (previousCenter == -1) {
				// Record center in order to react to it next iteration
				previousCenter = center;
			} else {
				double difference = center - previousCenter;
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
			System.out.println(contours.size());
			if (contours.size() == 3) {
				//#TODO Calculate distance and placement in coordinate system
				CustomPoint placement = calculatePlacement(camMat, contours);
				moveDroneToStart(placement);
				code = null;
				rotateCount = 0;
				PictureController.shouldScan = false;
				return;
			} else if (scannedCount < 50) {
				scannedCount++;
				return;
			}
			else {
				scannedCount = 0;
				//#TODO Fly backwards (0.5 meters)
				addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
				return;
			}
		} else {
			double distanceToSquare = OFC.calcDistance(rect);
			// It might still be a QR code, we're too far away to know
			if (distanceToSquare > 100) {
				//#TODO Fly closer to the square (0.5 meters)
				addCommand(Command.FORWARD, FORWARD_TIME_2, FORWARD_SPEED);
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
	
	private void addCommand(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			commandController.addCommand(task, duration, speed);
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
			System.out.println(e.toString());
			if (Math.round(e.getX()) != scannedPoint.getX() || Math.round(e.getY()) != scannedPoint.getY()) {
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