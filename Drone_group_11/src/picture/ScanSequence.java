package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_imgcodecs;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import helper.Move;

public class ScanSequence implements Runnable {
	private static final int MIN_HIT_COUNT = 6;
	private static final int FORWARD_TIME_2 =2000;
	private static final int BACKWARD_TIME = 1500;
	private static final int BACKWARD_SPEED = 9;
	private static final int STRAFE_TIME = 1200;
	private static final int STRAFE_SPEED = 10;
	private static final int SPIN_TIME = 1500;
	private static final int SPIN_SPEED = 6;
	private static final int FORWARD_TIME = 1200;
	private static final int FORWARD_SPEED = 9;
	private static final int ROTATE_TIME = 5000;
	private static final int ROTATE_SPEED = 17;
	
	//#TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
		
	private CommandController commandController;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private int foundFrameCount = 0;
	private CustomPoint placement = null;
	private boolean moveToStart = false;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	
	public ScanSequence(CommandController commandController) {
		this.commandController = commandController;
	}
	
	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}
	
	@Override
	public void run() {
		commandController.droneInterface.takeOff();

		sleep(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 5000, 15);
		
		while(PictureController.shouldScan) {
			if (OFVideo.imageChanged) {
				scanSequence();
			} else {
				sleep(50);
			}
		}
		
		while(moveToStart){
			moveDroneToStart(placement);
		}
	}
	
	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = OFC.findQrContours(camMat);
		if (contours.size() == 0) {
			if (frameCount < 4) {
				frameCount++;
			} else {
				code = null;
				if (rotateCount < 15) {
					//#TODO Rotate 90 degrees
					addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
					rotateCount++;
				} else {
					//#TODO Fly forwards (1 meter)
//					addCommand(Command.FORWARD, FORWARD_TIME, FORWARD_SPEED);
					rotateCount = 0;
				}
				frameCount = 0;
			}
			return;
		}
		
		
		
		foundFrameCount = 0;
		frameCount = 0;
		

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
			if (positionFromCenter > 0) {
				addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
			} else {
				addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
			}
			return;
		}
		
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			if (strafeRight) {
				addCommand(Command.RIGHT, STRAFE_TIME, STRAFE_SPEED);
			} else {
				addCommand(Command.LEFT, STRAFE_TIME, STRAFE_SPEED);
			}
			if (previousCenter == -1) {
				previousCenter = center;
			} else {
				double difference = center - previousCenter;
				if (difference > CENTER_DIFFERENCE) {
					strafeRight = !strafeRight;
					previousCenter = center;
					System.out.println("CHANGE STRAFE DIRECTION");
				}
			}
			return;
		}
		
		
		// Reset the previous center
		previousCenter = -1;
//		double distanceToDrone = OFC.calcDistance(rect);
//		if (distanceToDrone < 300) {
//			addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
//			return;
//		}
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
				placement = calculatePlacement(camMat, contours);
				moveToStart = true;
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
			if (distanceToSquare > 300) {
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
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
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
		OFVideo.imageChanged = false;
//		double distanceToSquare = OFC.calcDistance(rect);
	}
	
	private List<Move> calcMovesXAxis(double x, double y) {
		List<Move> moves = new ArrayList<>();
		int minY = 0;
		int maxX = 5;
		int maxY = 5;
		// Calc moves in x-axis
		for (int i = 0; i < 5; i++) {
			if (x < maxX) {
				x++;
				moves.add(new Move(Move.MOVE_RIGHT));
			}
		}
		// Calc moves in y-axis
		for (int i = 0; i < 5; i++) {
			if (y <= maxY && y > minY) {
				y--;
				moves.add(new Move(Move.MOVE_DOWN));
			}
		}
		return moves;
	}
	
	private List<Move> calcMovesYAxis(double x, double y) {
		List<Move> moves = new ArrayList<>();
		int minY = 0;
		int maxX = 5;
		int maxY = 5;
		// Calc moves in x-axis
		for (int i = 0; i < 5; i++) {
			if (y <= maxY && y > minY) {
				y--;
				moves.add(new Move(Move.MOVE_DOWN));
			}
		}
		return moves;
	}

	private void printMoves(List<Move> moves) {
		for (Move move : moves) {
			if (move.getMove() == Move.MOVE_RIGHT) {
//				System.out.println("MOVE RIGHT");
			}
			if (move.getMove() == Move.MOVE_DOWN) {
//				System.out.println("MOVE DOWN");
			}
			if (move.getMove() == Move.MOVE_LEFT) {
//				System.out.println("MOVE LEFT");
			}
			if (move.getMove() == Move.MOVE_FORWARD) {
//				System.out.println("MOVE FORWARD");
			}
		}
	}
}