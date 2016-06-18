package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.sql.Time;
import java.util.List;

import helper.Command;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import com.google.zxing.qrcode.encoder.QRCode;

import app.CommandController;
import picture.DownScanSeq;
import picture.OFVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class FlightControl implements Runnable {
	public static final double CENTER_UPPER = 0.1;
	public static final double CENTER_LOWER = -0.1;

	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private Mat camMat;
	private int timeOut;
	private double centerDistance;
	private DownScanSeq downScan = new DownScanSeq(commandController);
	double distance;
	double currentDistance;
	private boolean tooFar = true;
	private boolean tooClose = true;

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
		commandController.addCommand(Command.UP, 5000, 15);
		sleepThread(5000);
		commandController.addCommand(Command.UP, 5000, 15);
		sleepThread(5000);

		flyLaneOne();
		flyLaneTwo();
		flyLaneThree();
		flyLaneFour();
		flyLaneFive();
		flyLaneSix();
	}

	public void flyLaneOne() {
		commandController.droneInterface.setFrontCamera();
		// commandController.addCommand(Command.ROTATELEFT, 4000, 30); // spin
		// 180
		// sleepThread(4500);

		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		RotatedRect rect = rightMostRect(contours);
		Mat qrImg = null;
		String tempCode = "";
		int i = 0;
		double distance = pictureProcessingHelper.calcDistance(rect);
		double currentDistance = pictureProcessingHelper.calcDistance(rect);
		adjustToRectDistance(rect);
		do {
			commandController.addCommand(Command.FORWARD, 1500, 15);
			sleepThread(2500);
			// hover
			commandController.droneInterface.setBottomCamera();
			do {
				downScan.scanGreen();
				downScan.scanRed();
			} while (!downScan.greenDone && !downScan.redDone);
			downScan.greenDone = false;
			downScan.redDone = false;

			// TODO: Scan for immediate threats
			// boxes

			contours = pictureProcessingHelper.findQrContours(camMat);
			if (contours.size() > 0) {
				rect = rightMostRect(contours);
				qrImg = pictureProcessingHelper.warpImage(camMat, rect);
				tempCode = pictureProcessingHelper.scanQrCode(qrImg);
			}
		} while (!tempCode.startsWith("W00"));

		//
		// System.out.println("FOUND");
		// contours = pictureProcessingHelper.findQrContours(camMat);
		// rect = rightMostRect(contours);
		//

		// Vi kan nu se QR kode og centrerer på den
		do {
			// kåre centrere QR kode
			// check distance to QR kode
			// contours = pictureProcessingHelper.findQrContours(camMat);
			// rect = mostCenteredRect(contours);
			// distance = pictureProcessingHelper.calcDistance(rect);

			commandController.addCommand(Command.FORWARD, 1000, 10);
			sleepThread(1500);

			commandController.droneInterface.setBottomCamera();
			do {
				downScan.scanGreen();
				downScan.scanRed();
			} while (!downScan.greenDone && !downScan.redDone);
			downScan.greenDone = false;
			downScan.redDone = false;

		} while (distance > 100);
		System.out.println("CLOSE");
	}
	
	private boolean checkAspectRatio(RotatedRect rect) {
		double center = pictureProcessingHelper.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			return false;
		}
		return true;
	}

	private RotatedRect rightMostRect(List<Mat> contours) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			if (distanceFomCenter > distance && checkAspectRatio(rect2)) {
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
			double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			if (distanceFomCenter > distance && checkAspectRatio(rect2)) {
				distanceFomCenter = Math.abs(distance);
				rect = rect2;
			}
		}
		return rect;
	}

	private void adjustToRectDistance(RotatedRect rect) {
		distance = pictureProcessingHelper.calcDistance(rect);
		commandController.addCommand(Command.FORWARD, 1000, 10); // 1 chunk
		while (!tooClose && !tooFar) {
			currentDistance = pictureProcessingHelper.calcDistance(rect);
			if ((distance - currentDistance) > 95) {
				commandController.addCommand(Command.BACKWARDS, 500, 10);
			}
			if ((distance - currentDistance) < 75) {
				commandController.addCommand(Command.FORWARD, 500, 10);
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

	private void flyLaneTwo() {
		// TODO Auto-generated method stub

	}

	private void flyLaneThree() {
		// TODO Auto-generated method stub

	}

	private void flyLaneFour() {
		// TODO Auto-generated method stub

	}

	private void flyLaneFive() {
		// TODO Auto-generated method stub

	}

	private void flyLaneSix() {
		// TODO Auto-generated method stub

	}

}
