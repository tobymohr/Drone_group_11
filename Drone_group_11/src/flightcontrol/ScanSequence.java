package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import picture.OFVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class ScanSequence implements Runnable {
	
	private static final int MIN_HIT_COUNT = 6;
	private static final int FORWARD_TIME_2 = 800;
	private static final int BACKWARD_TIME = 1500;
	private static final int BACKWARD_SPEED = 9;
	private static final int STRAFE_TIME = 800;
	private static final int STRAFE_SPEED = 7;
	private static final int SPIN_TIME = 500;
	private static final int SPIN_SPEED = 20;
	private static final int FORWARD_TIME = 800;
	private static final int FORWARD_SPEED = 7;
	private static final int ROTATE_TIME = 300;
	private static final int ROTATE_SPEED = 90;
	private static final int FIELD_DURATION = 1500;
	private static final int FIELD_SPEED = 12;
	private static final int FIELD_SMALL_DURATION = 800;
	private static final int FIELD_SMALL_SPEED = 6;
	private static final int MAX_Y_CORD = 1060;
	private static final int MIN_Y_CORD = -10;
	private static final int MAX_X_CORD = 926;
	private static final int MIN_X_CORD = 80;
	private static int noDistFound = 0;
	private static final int noDistFoundLimit = 200;
	private static final int MAX_FRAME_COUNT = 8;
	private static final int MAX_ROTATE_COUNT = 15;
	private double contourSize = 0;

	// #TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
	public static final double SMALL_MOVE_LIMIT = 100;


	private CommandController commandController;
	private double chunkSize = 0;
	private double smallChunkSize = 30;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private int foundFrameCount = 0;
	private boolean moveToStart = false;
	private int endX = 847;
	private int endY = -10;
	private CustomPoint endPlacement;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	private boolean isFirst = false;
	private boolean manualUpdateX = true;
	private final static int CHUNK_SIZE = 70;
	private double distanceFromQr = 0;
	private boolean moveX;
	private boolean doCommand = false;
	private boolean startSmallMove = false;
	private boolean canGetDist = true;
	private boolean noMove = false;
	private double prevDistance = 0;
	private boolean runScanSequence = true;
	private boolean xDone = false;
	private boolean yDone = false;
	private DownScanSeq down;
	boolean stopUsingSpin = xDone == true || yDone == true;

	public ScanSequence(CommandController commandController, DownScanSeq down) {
		this.commandController = commandController;
		this.down = down;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	@Override
	public void run() {
		commandController.droneInterface.takeOff();
		sleep(2000);
		commandController.droneInterface.hover();
		sleep(2000);
		commandController.addCommand(Command.UP, 3500, 12);

		while (runScanSequence) {
			if (OFVideo.imageChanged) {
				scanSequence();
			} else {
				sleep(50);
			}
		}
		
		scanCubes();
		firstAxisToMove();
		frameCount = 0;

		while (moveToStart) {
			if (OFVideo.imageChanged) {
				moveDroneToPlacement(new CustomPoint(847, FlightControl.MIN_Y_CORD));
			} else {
				sleep(50);
			}
			
		}
		
		PictureController.shouldFlyControl = true;
		OFVideo.isFirst =  true;
		PictureController.shouldScan = false;
		System.out.println("START THE CUDE SEQUENCE");

	}
	
	private void scanCubes() {
		commandController.droneInterface.hover();
		commandController.droneInterface.setBottomCamera();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		down.run();
		
		commandController.droneInterface.setFrontCamera();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			return Command.FORWARD;
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

		if (frameCount < MAX_FRAME_COUNT) {
			frameCount++;
		} else {
			code = null;
			if (rotateCount < MAX_ROTATE_COUNT) {
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
			double distance = (camMat.arrayWidth() / 2) - rect2.center().x();
			if (distanceFomCenter > distance) {
				distanceFomCenter = Math.abs(distance);
				rect = rect2;
				contourSize =  contourArea(contours.get(i));
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
			}
		}
	}

	private void spinCheck(double positionFromCenter) {
		if (positionFromCenter > 0) {
			addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED + 5);
		} else {
			addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
		}
	}


	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		if (contours.size() == 0) {
			rotateCheck();
			return;
		}
		foundFrameCount = 0;
		frameCount = 0;
		RotatedRect rect = mostCenteredRect(contours);

		double positionFromCenter = pictureProcessingHelper.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			spinCheck(positionFromCenter);
			return;
		}
		double center = pictureProcessingHelper.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			centerCheck(center);
			return;
		}
		previousCenter = -1;

		Mat qrImg = pictureProcessingHelper.warpImage(camMat.clone(), rect);
		String tempCode = pictureProcessingHelper.scanQrCode(qrImg);
		if (tempCode != null) {
			code = tempCode;
		}
		if (code != null) {
			if (contours.size() == 3) {
				PictureController.setPlacement(calculatePlacement(camMat, contours));
				moveToStart = true;
				rotateCount = 0;
				runScanSequence = false;

				return;
			} else if (scannedCount < 3) {
				scannedCount++;
				return;
			} else {
				scannedCount = 0;
				// #TODO Fly backwards (0.5 meters)
				addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
				return;
			}
		} else {
			double distanceToSquare = pictureProcessingHelper.calcDistance(rect);
			if (distanceToSquare > 300) {
				addCommand(Command.FORWARD, FIELD_DURATION, FIELD_SPEED);
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
		}
	}

	private void addCommandForPlacement(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					updateRelativeCord(task);
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
			positions.add(pictureProcessingHelper.isCenterInImage(srcImage, minAreaRect(mat)));
		}
		int minIndex = positions.indexOf(Collections.min(positions));
		int maxIndex = positions.indexOf(Collections.max(positions));
		int middleIndex = 3 - minIndex - maxIndex;

		RotatedRect leftQR = minAreaRect(contours.get(minIndex));
		RotatedRect middleQR = minAreaRect(contours.get(middleIndex));
		RotatedRect rightQR = minAreaRect(contours.get(maxIndex));

		CustomPoint[] points = pictureProcessingHelper.calcPosition(pictureProcessingHelper.calcDistance(leftQR),
				pictureProcessingHelper.calcDistance(middleQR), pictureProcessingHelper.calcDistance(rightQR), code);
		CustomPoint scannedPoint = CustomPoint.parseQRText(code);
		for (CustomPoint e : points) {
			if (Math.round(e.getX()) != scannedPoint.getX() || Math.round(e.getY()) != scannedPoint.getY()) {
				placement = e;
			}
		}

		return placement;
	}
	
	public boolean canMoveWithoutSpinCheck(RotatedRect rect){
		double positionFromCenter = pictureProcessingHelper.isCenterInImageBigger(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			if (positionFromCenter > 0) {
				if(pictureProcessingHelper.getSpinSpeed(contourSize)> 0){
					addCommand(Command.SPINRIGHT, SPIN_TIME,pictureProcessingHelper.getSpinSpeed(contourSize));
				}else {
					addCommand(Command.SPINRIGHT, SPIN_TIME,SPIN_SPEED);
				}
			} else {
				if(pictureProcessingHelper.getSpinSpeed(contourSize)>0){
					addCommand(Command.SPINLEFT, SPIN_TIME, pictureProcessingHelper.getSpinSpeed(contourSize));
				}else {
					addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
				}
				
			}
			return false;
		}
		return true;
	}

	private void moveDroneToPlacement(CustomPoint placement) {
		endPlacement = placement;
		OFVideo.imageChanged = false;
		doCommand = false;
		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		// find mostcenteredrect
		RotatedRect rect = mostCenteredRect(contours);
		if (frameCount >= MAX_FRAME_COUNT) {
			frameCount = 0;
			distanceFromQr = pictureProcessingHelper.calcDistance(rect);
			chunkSize = 55;
			
			// Start moving
			if (moveX && !xDone) {
				if (contours.size() > 0 && !stopUsingSpin) {
					if (!canMoveWithoutSpinCheck(rect)) {
						return;
					}
				}
				int move = calcMoveXAxis(PictureController.getPlacement().getX(), placement);
				decideMove(move);
			} else {
				if (!yDone) {
					if (contours.size() > 0 && !stopUsingSpin) {
						if (!canMoveWithoutSpinCheck(rect)) {
							return;
						}
					}
					int move = calcMovesYAxis(PictureController.getPlacement().getY(), placement);
					decideMove(move);
					
				}
			}
		} else {
			frameCount++;
		}
	}
	
	private CustomPoint dynamicCheck(double distanceFromQr){
		CustomPoint tempPlace = PictureController.getPlacement();
		if (!Double.isInfinite(distanceFromQr)) {

			
			if (code.contains("W02")) {
				tempPlace.setY(FlightControl.MIN_Y_CORD + distanceFromQr);
				PictureController.setPlacement(tempPlace);
			}
			if (code.contains("W00")) {
				tempPlace.setY(FlightControl.MAX_Y_CORD - distanceFromQr);
				PictureController.setPlacement(tempPlace);
			}
			
			if (code.contains("W03")) {
				tempPlace.setY(FlightControl.MIN_X_CORD + distanceFromQr);
				PictureController.setPlacement(tempPlace);
			}
			if (code.contains("W01")) {
				tempPlace.setY(FlightControl.MAX_X_CORD - distanceFromQr);
				PictureController.setPlacement(tempPlace);
			}
		}
		return tempPlace;
	}

	private void updateRelativeCord(int move) {
		CustomPoint placement = PictureController.getPlacement();
		if (move == Command.BACKWARDS) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() - chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() - chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() + chunkSize);
			} else {
				placement.setX(placement.getX() + chunkSize);
			}
		} else if (move == Command.FORWARD) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() + chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() + chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() - chunkSize);
			} else {
				placement.setX(placement.getX() - chunkSize);
			}
		} else if (move == Command.RIGHT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() + smallChunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() - smallChunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() - smallChunkSize);
			} else {
				placement.setY(placement.getY() + smallChunkSize);
			}
		} else if (move == Command.LEFT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() - smallChunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() + smallChunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() + smallChunkSize);
			} else {
				placement.setY(placement.getY() - smallChunkSize);
			}
			
			
		}
		placement = dynamicCheck(distanceFromQr);
		
		double differenceY = Math.abs((placement.getY() - endPlacement.getY()));
		boolean endYCondition = differenceY > 0 && differenceY < 60;

		double differenceX = Math.abs((placement.getX() - endPlacement.getX()));
		boolean endXCondition = differenceX > 0 && differenceX < 60;

		if (endYCondition && endXCondition) {
			moveToStart = false;
		}

		yDone = endYCondition;
		xDone = endXCondition;

		if (yDone) {
			moveX = true;
		}

		if (xDone) {
			moveX = false;
		}
		// if (!Double.isInfinite(distanceFromQr)) {
		// if (placement.getX() >= 500 && placement.getY() >= 500) {
		// if (!moveX) {
		// if (code.contains("W00")) {
		// placement.setY(MAX_Y_CORD - distanceFromQr);
		// } else {
		// placement.setY(MIN_Y_CORD + distanceFromQr);
		// }
		//
		// } else {
		// if (code.contains("W01")) {
		// placement.setX(MAX_X_CORD - distanceFromQr);
		// } else {
		// placement.setX(MIN_X_CORD + distanceFromQr);
		// }
		// }
		// noMove = false;
		// }
		// }

		PictureController.setPlacement(placement);
		scanCubes();
	}

	private void decideMove(int move) {
		if (move == Command.LEFT || move == Command.RIGHT) {
			addCommandForPlacement(move, STRAFE_TIME, SPIN_SPEED);
		} else {
			addCommandForPlacement(move, FIELD_DURATION, FIELD_SPEED);
		}
	}

	private int calcMoveXAxis(double x, CustomPoint placement) {
		if (x < placement.getX()) {
			return getCorrectXMove();
		}
		return Command.NONE;
	}

	private int calcMovesYAxis(double y, CustomPoint placement) {
		if (y < placement.getY()) {
			moveX = true;
			return Command.BACKWARDS;
		}
		if (y > placement.getY()) {
			return getCorrectYMove(y);
		}
		return Command.NONE;
	}

}