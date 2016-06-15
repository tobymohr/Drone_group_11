package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_imgcodecs;

import app.CommandController;
import de.yadrone.base.command.CommandManager;
import helper.Command;
import helper.CustomPoint;
import helper.Move;
import javafx.scene.control.Label;

public class ScanSequence implements Runnable {
	private static final int FORWARD_TIME_2 = 500;
	private static final int BACKWARD_TIME = 500;
	private static final int BACKWARD_SPEED = 15;
	private static final int STRAFE_TIME = 650;
	private static final int STRAFE_SPEED = 20;
	private static final int SPIN_TIME = 300;
	private static final int SPIN_SPEED = 15;
	private static final int FORWARD_TIME = 4000;
	private static final int FORWARD_SPEED = 10;
	private static final int ROTATE_TIME = 2000;
	private static final int ROTATE_SPEED = 10;
	private static final int HOVER_TIME = 2000;
	private static final int READY_TO_MOVE = 12;
	// #TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.1;
	public static final double CENTER_LOWER = -0.1;
	public static final double CENTER_DIFFERENCE = 0.05;
	private Map<Integer, Integer> moveSet = new HashMap<>();
	private CommandController commandController;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int foundFrameCount = 0;
	private int centerCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private double centerOfImage = 0;
	private long startTime = 0;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();

	public static volatile boolean imageChanged;

	public ScanSequence(CommandController commandController) {
		moveSet.put(Command.LEFT, 0);
		moveSet.put(Command.RIGHT, 0);
		moveSet.put(Command.SPINLEFT, 0);
		moveSet.put(Command.SPINRIGHT, 0);
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	public void findMove() {
		startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - 2000 < startTime);
		
			for (Integer key : moveSet.keySet()) {
				Integer pair = moveSet.get(key);
				if (pair >= READY_TO_MOVE) {
					commandController.addCommand(pair, getTimeForCommand(pair), getSpeedForCommand(pair));
					frameCount = 0;
					moveSet.clear();
					return;
				}
				if (rotateCount < 6) {
					commandController.addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
					rotateCount++;
					moveSet.clear();
					return;
				} else {
					commandController.addCommand(Command.FORWARD, FORWARD_TIME, FORWARD_SPEED);
					rotateCount = 0;
					moveSet.clear();
					return;
				}
			}
	}

	public int getTimeForCommand(int command) {
		if (command == Command.SPINLEFT || command == Command.SPINRIGHT) {
			return SPIN_TIME;
		}
		if (command == Command.RIGHT || command == Command.LEFT) {
			return STRAFE_TIME;
		}
		return 2000;
	}

	public int getSpeedForCommand(int command) {
		if (command == Command.SPINLEFT || command == Command.SPINRIGHT) {
			return SPIN_SPEED;
		}
		if (command == Command.RIGHT || command == Command.LEFT) {
			return STRAFE_SPEED;
		}
		return 15;
	}

	@Override
	public void run() {
		commandController.dC.takeOff();
		sleep(2000);
		commandController.dC.hover();
		sleep(6000);
		commandController.addCommand(Command.UP, 3000, 30);
		sleep(2000);

		while (PictureController.shouldScan) {
			scanSequence();
		}
	}

	private void scanSequence() {

		if (moveSet.isEmpty()) {
			moveSet.put(Command.LEFT, 0);
			moveSet.put(Command.RIGHT, 0);
			moveSet.put(Command.SPINLEFT, 0);
			moveSet.put(Command.SPINRIGHT, 0);
		}

		if (imageChanged) {
			imageChanged = false;
			List<Mat> contours = OFC.findQrContours(camMat);

			frameCount++;
			foundFrameCount = 0;
			// boolean wallClose = false;
			//
			// if(OFC.getDistance() <= 200){
			// commandController.dC.hover();
			// System.out.println("WallClose");
			// wallClose = true;
			// }
			//
			// if (wallClose) {
			//
			// //#TODO Rotate 90 degrees
			// return;
			// }

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
					moveSet.put(Command.SPINRIGHT, moveSet.get(Command.SPINRIGHT) + 1);
				} else {
					moveSet.put(Command.SPINLEFT, moveSet.get(Command.SPINLEFT) + 1);
				}
			}
			double center = OFC.center(rect);
			if (center > CENTER_UPPER || center < CENTER_LOWER) {
				// #TODO Strafe the drone <center> amount. Right is chosen as
				// standard.
				if (strafeRight) {
					moveSet.put(Command.RIGHT, moveSet.get(Command.RIGHT) + 1);
				} else {
					moveSet.put(Command.LEFT, moveSet.get(Command.LEFT) + 1);
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
					}
				}
			}
			// Reset the previous center
			previousCenter = -1;
			//
			// Mat qrImg = OFC.warpImage(camMat.clone(), rect);
			//
			// String tempCode = OFC.scanQrCode(qrImg);
			//
			// if (tempCode != null) {
			//// // Code stored as field, we need to use it even if we're too
			// far away to scan it.
			//// //#TODO Ensure that the field code is set to null every time we
			// need to reset.
			// code = tempCode;
			// //System.out.println(tempCode);
			// }
			// //System.out.println("CENTERED");
			// if(code != null) {
			// // Check amount of squares found
			// // #TODO Implement some way to check squares across more than one
			// frame
			// if (contours.size() == 3) {
			// //#TODO Calculate distance and placement in coordinate system
			// CustomPoint placement = calculatePlacement(camMat, contours);
			// //System.out.println(placement.getX() + "|" + placement.getY());
			// moveDroneToStart(placement);
			// code = null;
			// centerCount++;
			// rotateCount = 0;
			// PictureController.shouldScan = false;
			// return;
			// } else if(CommandController.droneIsReady && centerCount > 10 ) {
			// //System.out.println("HOVER");
			// centerCount = 0;
			// commandController.dC.hover();
			// sleep(HOVER_TIME);
			// //#TODO Fly backwards (0.5 meters)
			// commandController.addCommand(Command.BACKWARDS, BACKWARD_TIME,
			// BACKWARD_SPEED);
			// sleep(BACKWARD_TIME);
			// return;
			// }
			// } else {
			// double distanceToSquare = OFC.calcDistance(rect);
			// //System.out.println("HOVER");
			// commandController.dC.hover();
			// sleep(HOVER_TIME);
			// // It might still be a QR code, we're too far away to know
			// if (distanceToSquare > 100 && CommandController.droneIsReady) {
			// //#TODO Fly closer to the square (0.5 meters)
			// commandController.addCommand(Command.FORWARD, FORWARD_TIME_2,
			// BACKWARD_SPEED);
			// sleep(FORWARD_TIME_2);
			// return;
			// } else {
			// //#TODO Fly backwards (4-5 meters)
			// //#TODO Rotate 90 degrees
			// return;
			// }
			// }

			findMove();
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

	private void moveDroneToStart(CustomPoint placement) {
		List<Move> moves = OFC.calcMoves(placement.getX(), placement.getY());
		for (Move move : moves) {
			// #TODO Actually move the drone according to movess
		}
	}
}
