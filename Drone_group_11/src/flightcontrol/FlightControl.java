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
		//		commandController.addCommand(Command.ROTATELEFT, 4000, 30); // spin 180
		//		sleepThread(4500);

		List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
		RotatedRect rect = null;
		Mat qrImg = null;
		String tempCode = "";
		int i = 0;
		// TODO: noget aspect ratio, så vi ved vi kigger på en af de rect på væg over for dronen
		double distance = pictureProcessingHelper.calcDistance(rect);
		double currentDistance = pictureProcessingHelper.calcDistance(rect);
		adjustToRectDistance(rect);
		do {
			
			sleepThread(2500);
			//hover
			commandController.droneInterface.setBottomCamera();
			do{
				downScan.scanGreen();
				downScan.scanRed();
			}while(!downScan.greenDone && !downScan.redDone);
			downScan.greenDone = false;
			downScan.redDone = false;

			
			// TODO: Scan for immediate threats
			// boxes
			
			// adjust position relative to mostRightRect with correct aspect ratio
			contours = pictureProcessingHelper.findQrContours(camMat);
			if (contours.size() > 0) {
				rect = mostCenteredRect(contours);
				qrImg = pictureProcessingHelper.warpImage(camMat, rect);
				tempCode = pictureProcessingHelper.scanQrCode(qrImg);
			}
		} while (!tempCode.startsWith("W00.04"));

		
//		
//		System.out.println("FOUND");
//		contours = pictureProcessingHelper.findQrContours(camMat);
//		rect = rightMostRect(contours);
//		
		
		// Vi kan nu se QR kode og centrerer på den
		// Sekvens flyver nu ud fra QR og ikke mostRightRect
		do {
			// kåre centrere QR kode 
			// check distance to QR kode
//			contours = pictureProcessingHelper.findQrContours(camMat);
//			rect = mostCenteredRect(contours);
//			distance = pictureProcessingHelper.calcDistance(rect);
			adjustToQRDistance();
			
			commandController.addCommand(Command.FORWARD, 1000, 10);
			sleepThread(1500);
			
			commandController.droneInterface.setBottomCamera();
		do{
			downScan.scanGreen();
			downScan.scanRed();
		}while(!downScan.greenDone && !downScan.redDone);
		downScan.greenDone = false;
		downScan.redDone = false;

			
		} while (distance > 100);
		System.out.println("CLOSE");
	}





	

	private RotatedRect rightMostRect(List<Mat> contours){
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			if (distanceFomCenter > distance) {
				distanceFomCenter = distance;
				rect = rect2;
			}
		}
		return rect;
	}

	private RotatedRect mostCenteredRect(List<Mat> contours){
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
	
	private void adjustToRectDistance(RotatedRect rect){
		distance = pictureProcessingHelper.calcDistance(rect);
		commandController.addCommand(Command.FORWARD, 1000, 10); // 1 chunk
		while(!tooClose || !tooFar){
			tooFar = true;
			tooClose = true;
			currentDistance = pictureProcessingHelper.calcDistance(rect);
			if((distance - currentDistance) > 95){
				commandController.addCommand(Command.BACKWARDS, 500, 10);
				tooFar = false;
			}
			if((distance - currentDistance) < 75){
				commandController.addCommand(Command.FORWARD, 500, 10);
				tooClose = false;
			}
		}
	}
	
	private void adjustToQRDistance() {
		//TODO: sæt distance til QR
		commandController.addCommand(Command.FORWARD, 1000, 10); // 1 chunk
		while(!tooClose || !tooFar){
			tooFar = true;
			tooClose = true;
			//TODO:Sæt current distance to QR
			if((distance - currentDistance) > 95){
				commandController.addCommand(Command.BACKWARDS, 500, 10);
				tooFar = false;
			}
			if((distance - currentDistance) < 75){
				commandController.addCommand(Command.FORWARD, 500, 10);
				tooClose = false;
			}
		}
	}





	private void sleepThread(int duration)
	{
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
