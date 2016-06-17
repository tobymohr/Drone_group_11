package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.sql.Time;
import java.util.List;

import helper.Command;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import com.google.zxing.qrcode.encoder.QRCode;

import app.CommandController;
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
		commandController.addCommand(Command.UP, 2750, 20);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		String qrCode;
		
		commandController.addCommand(Command.ROTATELEFT, 4000, 30); // spin 180
			do {
				commandController.addCommand(Command.FORWARD, 1000, 13); // 1 chunk
				commandController.dC.hover();
				// downScanSeq.scan()
				commandController.dC.setFrontCamera();

			} while (!pictureProcessingHelper.scanQrCode(camMat).equals("W00.04"));

			commandController.dC.hover();

			//Now we can see W00.04
			do{
				
				
				// CenterOnW00.04
				//When centered
				commandController.addCommand(Command.FORWARD, 1000, 13);
				commandController.dC.hover();
				//downScanSeq.scan();
				commandController.dC.setFrontCamera();
				
			}while(pictureProcessingHelper.getDistance() > 100);
			

			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			if (pictureProcessingHelper.scanQrCode(camMat).equals("W00.04")) {
				while (centerDistance != 0) {
					List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);

					double distanceFomCenter = 5000;
					RotatedRect rect = new RotatedRect();
					for (int i = 0; i < contours.size(); i++) {
						RotatedRect rect2 = minAreaRect(contours.get(i));
						double distance = (camMat.arrayWidth() / 2)
								- rect.center().x();
						if (distanceFomCenter > distance) {
							distanceFomCenter = Math.abs(distance);
							rect = rect2;
						}

					}
					double centerDistance = pictureProcessingHelper.isCenterInImage(camMat, rect);

					if (centerDistance > 0) {
						commandController.addCommand(Command.LEFT, 100, 14);
					} else if (centerDistance < 0) {
						commandController.addCommand(Command.RIGHT, 100, 14);
					}
				}
				
			}
		do {

		} while (timeOut < 5);

		//
	}

}
