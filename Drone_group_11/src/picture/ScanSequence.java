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

public class ScanSequence implements Runnable {
	private static final int MIN_HIT_COUNT = 6;
	private static final int FORWARD_TIME_2 = 2000;
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
	private static final int FIELD_DURATION = 1500;
	private static final int FIELD_SPEED = 7;
	private static final int FIELD_SMALL_DURATION = 800;
	private static final int FIELD_SMALL_SPEED = 6;
	private static final int MAX_Y_CORD = 1060;
	private static final int MIN_Y_CORD = -10;
	private static final int MAX_X_CORD = 926;
	private static final int MIN_X_CORD = 80;

	// #TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
	public static final double SMALL_MOVE_LIMIT = 100;

	private CommandController commandController;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private int foundFrameCount = 0;
	public static CustomPoint placement = null;
	private boolean moveToStart = false;
	private int endX = 847;
	private int endY = -10;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	private boolean isFirst = false;
	private boolean manualUpdateX = true;
	private final static int CHUNK_SIZE = 70;
	private double distanceFromQr = 0;
	private boolean moveX;
	private boolean doCommand = false;
	private boolean startSmallMove = false;
	private boolean canGetDist = true;

	public ScanSequence(CommandController commandController) {
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	@Override
	public void run() {
		commandController.droneInterface.takeOff();

		code = "W01.02";
		placement = new CustomPoint();
		placement.setX(514);
		placement.setY(721);
		moveToStart = true;

		sleep(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 5000, 15);

		// while(PictureController.shouldScan) {
		// if (OFVideo.imageChanged) {
		// scanSequence();
		// } else {
		// sleep(50);
		// }
		// }
		//
		firstAxisToMove();

		while (moveToStart) {
			moveDroneToStart();
		}
	}

	private void firstAxisToMove() {
		if (code.contains("W02") || code.contains("W00")) {
			moveX = false;
		}
		if (code.contains("W03") || code.contains("W01")) {
			moveX = true;
		}
	}

	private int getCorrectXMove() {
		if (code.contains("W02")) {
			return Command.LEFT;
		}
		if (code.contains("W03")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W00")) {
			return Command.RIGHT;
		}
		if (code.contains("W01")) {
			return Command.FORWARD;
		}
		return Command.NONE;

	}

	private int getCorrectYMove(double y) {
		if (code.contains("W02")) {
			return Command.NONE;
		}
		if (code.contains("W03")) {
			return Command.LEFT;
		}
		if (code.contains("W00")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W01")) {
			return Command.RIGHT;
		}

		return Command.NONE;

	}

	private void rotateCheck() {
		if (frameCount < 4) {
			frameCount++;
		} else {
			code = null;
			if (rotateCount < 15) {
				addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
				rotateCount++;
			} else {
				rotateCount = 0;
			}
			frameCount = 0;
		}
	}

	private RotatedRect mostCenteredRect(List<Mat> contours) {
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
		return rect;
	}

	private void centerCheck(double center) {
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
	}

	private void spinCheck(double positionFromCenter) {
		if (positionFromCenter > 0) {
			addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
		} else {
			addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
		}
	}

	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = OFC.findQrContours(camMat);
		if (contours.size() == 0) {
			rotateCheck();
			return;
		}
		foundFrameCount = 0;
		frameCount = 0;
		RotatedRect rect = mostCenteredRect(contours);

		double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			spinCheck(positionFromCenter);
			return;
		}
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			centerCheck(center);
			return;
		}
		previousCenter = -1;
		// double distanceToDrone = OFC.calcDistance(rect);
		// if (distanceToDrone < 300) {
		// addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
		// return;
		// }

		Mat qrImg = OFC.warpImage(camMat.clone(), rect);
		String tempCode = OFC.scanQrCode(qrImg);
		if (tempCode != null) {
			code = tempCode;
			System.out.println(tempCode);
		}
		System.out.println("CENTERED");
		if (code != null) {
			if (contours.size() == 3) {
				placement = calculatePlacement(camMat, contours);
				moveToStart = true;
				rotateCount = 0;
				PictureController.shouldScan = false;
				return;
			} else if (scannedCount < 50) {
				scannedCount++;
				return;
			} else {
				scannedCount = 0;
				// #TODO Fly backwards (0.5 meters)
				addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
				return;
			}
		} else {
			double distanceToSquare = OFC.calcDistance(rect);
			if (distanceToSquare > 300) {
				addCommand(Command.FORWARD, FORWARD_TIME_2, FORWARD_SPEED);
				return;
			} else {
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

			System.out.println("Distance to QR" + distanceFromQr + " placementX " + placement.getX());

			// if(moveToStart){
			//
			// commandController.addCommand(task, duration, speed);
			//
			// if(manualUpdateX){
			// placement.setX(placement.getX() + CHUNK_SIZE);
			// placement.setY(distanceFromQr);
			// }else {
			// placement.setY(placement.getY() + CHUNK_SIZE);
			// placement.setX(distanceFromQr);
			// }
			// }
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

		CustomPoint[] points = OFC.calcPosition(OFC.calcDistance(leftQR), OFC.calcDistance(middleQR),
				OFC.calcDistance(rightQR), code);
		CustomPoint scannedPoint = CustomPoint.parseQRText(code);
		for (CustomPoint e : points) {
			System.out.println(e.toString());
			if (Math.round(e.getX()) != scannedPoint.getX() || Math.round(e.getY()) != scannedPoint.getY()) {
				placement = e;
			}
		}

		return placement;
	}

	private void moveDroneToStart() {
		OFVideo.imageChanged = false;
		doCommand = false;
		List<Mat> contours = OFC.findQrContoursNoThresh(camMat);
		// find mostcenteredrect
		RotatedRect rect = mostCenteredRect(contours);
		// Get distance
		if(canGetDist ){
			distanceFromQr = OFC.calcDistance(rect);
		}
		// use distance for new X or Y coordinat
		if (!moveX) {
			if (!Double.isInfinite(distanceFromQr)) {
				if (code.contains("W00")) {
					placement.setY(MAX_Y_CORD - distanceFromQr);
				} else {
					placement.setY(MIN_Y_CORD + distanceFromQr);
				}
			}
			manualUpdateX = true;
		}
		if (moveX) {
			if (!Double.isInfinite(distanceFromQr)) {
				if (code.contains("W01")) {
					placement.setX(MAX_X_CORD - distanceFromQr);
				} else {
					placement.setX(MIN_X_CORD + distanceFromQr);
				}
			}
			manualUpdateX = false;
		}

		// Start moving
		if (moveX) {
			int moveX = calcMoveXAxis(placement.getX());
			decideMove(moveX);
		} else {
			int moveY = calcMovesYAxis(placement.getY());
			decideMove(moveY);
		}
	}
	
	private void decideMove(int move){
		if (startSmallMove) {
			addCommand(move, FIELD_SMALL_DURATION, FIELD_SMALL_SPEED);
			moveX = false;
		} else {
			if (move == Command.LEFT || move == Command.RIGHT) {
				addCommand(move, STRAFE_TIME, SPIN_SPEED);
			}else {
				addCommand(move, FIELD_DURATION, FIELD_SPEED);
			}
		}
	}

	private int calcMoveXAxis(double x) {
		if (x < endX) {
			if (endX - x < SMALL_MOVE_LIMIT) {
				startSmallMove = true;
				canGetDist = false;
			}
			return getCorrectXMove();
		} else {
			canGetDist = false;
			moveX = false;
		}
		return Command.NONE;
	}

	private int calcMovesYAxis(double y) {
		if (y < endY) {
			moveX = true;
			return Command.BACKWARDS;
		}
		if (y > endY) {
			if (endY - y > SMALL_MOVE_LIMIT) {
				canGetDist = false;
				startSmallMove = true;
			}
			return getCorrectYMove(y);
		} else {
			canGetDist = false;
			moveX = true;
		}
		return Command.NONE;
	}

}