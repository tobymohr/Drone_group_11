package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import de.yadrone.base.command.HoverCommand;
import helper.Command;
import picture.DownScanSeq;
import picture.PictureProcessingHelper;

public class FlightControl implements Runnable {
	public static final int CHUNK_SIZE = 85;

	// TODO Tweak values below this line
	private static final int STRAFE_UPPER = 200;
	private static final int STRAFE_LOWER = -200;
	private static final double CENTER_UPPER = 0.1;
	private static final double CENTER_LOWER = -0.1;
	private static final int SPIN_TIME = 500;
	private static final int SPIN_SPEED = 10;
	private static final int MIN_HIT_COUNT = 20;

	
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private Mat camMat;
	private DownScanSeq downScan = new DownScanSeq(commandController);
	double distance;
	double currentDistance;
	private Map<Integer, Integer> moves = new HashMap<>();

	public FlightControl(CommandController cc) {
		this.commandController = cc;
	}

	public void setImage(Mat img) {
		camMat = img;
	}

	@Override
	public void run() {
		commandController.droneInterface.takeOff();

		sleepThread(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleepThread(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 2750, 30);
		sleepThread(3750);

		flyLaneOne();
		// flyLaneTwo();
		// flyLaneThree();
		// flyLaneFour();
		// flyLaneFive();
		// flyLaneSix();
	}

	public void flyLaneOne() {
		commandController.droneInterface.setFrontCamera();
		// TODO Ensure centered on the start QR (W02.00)
		// Rotate 180 degrees
//		commandController.addCommand(Command.ROTATELEFT, 2000, 90);
//		sleepThread(2500);
		commandController.droneInterface.hover();
		sleepThread(500);

		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		RotatedRect rect = rightMostRect(contours);
		adjustLaneRotate(1);
		Mat qrImg = null;
		String tempCode = "";
		distance = pictureProcessingHelper.calcDistance(rect);
		while (!tempCode.startsWith("W00") && distance > 150) {
			goForwardOneChunk(rect, 1);
			addCommand(Command.HOVER, 2000, 10);
			//downScan.scanForCubes();

			// TODO: Scan for immediate threats (boxes)

			RotatedRect newRect = findRect(1);
			rect = adjustLaneStrafe(rect, newRect, 1);
			qrImg = pictureProcessingHelper.warpImage(camMat, rect);
			tempCode = pictureProcessingHelper.scanQrCode(qrImg);
			distance = pictureProcessingHelper.calcDistance(rect);
		}
		
		if (tempCode.equals("W00.04")) {
//			distance = pictureProcessingHelper.calcDistance(rect);
//			while (distance > 100) {
//				goForwardOneChunk(rect);
//				//downScan.scanForCubes();
//				contours = pictureProcessingHelper.findQrContours(camMat);
//				int i = 0;
//				while (contours.size() == 0 && i < 10) {
//					sleepThread(50);
//					contours = pictureProcessingHelper.findQrContours(camMat);
//					i++;
//				}
//				if (contours.size() > 0) {
//					RotatedRect newRect = null;
//					for (Mat contour : contours) {
//						qrImg = pictureProcessingHelper.warpImage(camMat, minAreaRect(contour));
//						tempCode = pictureProcessingHelper.scanQrCode(qrImg);
//						if (tempCode.startsWith("W00.04")) {
//							newRect = minAreaRect(contour);
//							break;
//						}
//					}
//					if (newRect == null) {
//						newRect = rightMostRect(contours);
//					}
//					rect = adjustLaneStrafe(rect, newRect, 1);
//					distance = pictureProcessingHelper.calcDistance(rect);
//				}
//			}
		} else if (tempCode.startsWith("W00")) {
			while (!tempCode.equals("W00.04")) {
				addCommand(Command.RIGHT, 500, 10);
				addCommand(Command.HOVER, 2000, 30);
				rect = findRect(1);
				qrImg = pictureProcessingHelper.warpImage(camMat, rect);
				tempCode = pictureProcessingHelper.scanQrCode(qrImg);
			}
		}

		System.out.println("CLOSE");
	}

	private void flyLaneTwo() {
	}

	private void flyLaneThree() {
	}

	private void flyLaneFour() {
	}

	private void flyLaneFive() {
	}

	private void flyLaneSix() {
	}

	private boolean checkAspectRatio(RotatedRect rect) {
		double center = pictureProcessingHelper.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			return false;
		}
		System.out.println("aspectTrue" + center);
		return true;
	}

	private RotatedRect findRect(int lane) {
		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		int i = 0;
		while (contours.size() == 0 && i < 10) {
			sleepThread(50);
			contours = pictureProcessingHelper.findQrContours(camMat);
			i++;
		}
		switch (lane) {
		case 1:
			return rightMostRect(contours);
		default:
			return mostCenteredRect(contours);
		}
	}

	private RotatedRect rightMostRect(List<Mat> contours) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect2.center().x();
			if (distanceFomCenter > distance) {
				distanceFomCenter = distance;
				rect = rect2;
			}
		}
		return rect;
	}

	private RotatedRect mostCenteredRect(List<Mat> contours) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect2.center().x();
			if (distanceFomCenter > distance && checkAspectRatio(rect2)) {
				distanceFomCenter = Math.abs(distance);
				rect = rect2;
			}
		}
		return rect;
	}

	private RotatedRect adjustLaneRotate(int lane) {
		RotatedRect rect = findRect(lane);
		double position = pictureProcessingHelper.isCenterInImage(camMat, rect);
		System.out.println(position);
		while (position != 0) {
			if (position > 0) {
				addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
			} else {
				addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
			}
			rect = findRect(lane);
			position = pictureProcessingHelper.isCenterInImage(camMat, rect);
		}
		return rect;
	}

	private RotatedRect adjustLaneStrafe(RotatedRect prevRect, RotatedRect newRect, int lane) {
		double difference = prevRect.center().x() - newRect.center().x();
		while (difference > STRAFE_UPPER || difference < STRAFE_LOWER) {
			// TODO Tweak these values
			if (difference > STRAFE_UPPER) {
				addCommand(Command.LEFT, 500, 10);
			} else {
				addCommand(Command.RIGHT, 500, 10);
			}
			List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
			switch (lane) {
			case 1:
				newRect = rightMostRect(contours);
				break;
			default:
				newRect = mostCenteredRect(contours);
				break;
			}

			difference = prevRect.center().x() - newRect.center().x();
		}
		return newRect;
	}

	private void goForwardOneChunk(RotatedRect rect, int lane) {
		distance = pictureProcessingHelper.calcDistance(rect);
		addCommand(Command.FORWARD, 1500, 10); // 1 chunk
		boolean tooClose = false;
		boolean tooFar = false;
		while (!tooClose || !tooFar) {
			tooFar = true;
			tooClose = true;

			currentDistance = pictureProcessingHelper.calcDistance(findRect(lane));
			if ((distance - currentDistance) > 95) {
				addCommand(Command.BACKWARDS, 500, 10);
				tooFar = false;
			}
			if ((distance - currentDistance) < 75) {
				addCommand(Command.FORWARD, 500, 10);
				tooClose = false;
			}
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
					System.out.println("ADDFORWARDSDASDSD");
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
		}
	}


	private void sleepThread(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			System.out.println("InterruptedEX");
		}
	}
}
