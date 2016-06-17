package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.sql.Time;
import java.util.List;

import helper.Command;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import com.google.zxing.qrcode.encoder.QRCode;

import app.CommandController;
import picture.OFVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class FlightControl implements Runnable {

	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private Mat camMat;
	private int timeOut;
	private double centerDistance;

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
		// if (pph.isCenterInImage(camMat, rect) == 0) {
		// flyLaneOne();
		// }

		// TODO: Center for qr-code

		// TODO: Spin 180
		// findContourBlack
		// center on white mass
		// check for blue box tower
		// fly chunksize
		// recent on white mass
		// call downScanSeq
		// frontcam, center on mass
		// check for distance
		// fly chunk

		// strafe left or right
		// center on mass (QR or Triangle)
		// spin 180
		// rinse and repeat

		// TODO:

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
		do {
			commandController.addCommand(Command.FORWARD, 1000, 10); // 1 chunk
			sleepThread(2500);
			// downScanSeq.scan()
			contours = pictureProcessingHelper.findQrContours(camMat);
			if (contours.size() > 0) {
				rect = mostCenteredRect(contours);
				qrImg = pictureProcessingHelper.warpImage(camMat, rect);
				tempCode = pictureProcessingHelper.scanQrCode(qrImg);
			}
			if (i > 3) {
				commandController.addCommand(Command.LEFT, 1000, 10);
				sleepThread(2000);
				i = 0;
			}
			i++;
		} while (!tempCode.startsWith("W00"));
		System.out.println("FOUND");
		contours = pictureProcessingHelper.findQrContours(camMat);
		rect = mostCenteredRect(contours);
		double distance = pictureProcessingHelper.calcDistance(rect);
		do {
			contours = pictureProcessingHelper.findQrContours(camMat);
			rect = mostCenteredRect(contours);
			distance = pictureProcessingHelper.calcDistance(rect);
			commandController.addCommand(Command.FORWARD, 1000, 13);
			sleepThread(1500);
			// downScanSeq.scan();
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

	
	private void sleepThread(int duration)
	{
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			System.out.println("InterruptedEX");
		}
	}

}
