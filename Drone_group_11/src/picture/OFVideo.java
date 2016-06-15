package picture;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import app.CommandController;
import app.DroneCommunicator;
import de.yadrone.base.IARDrone;
import helper.Command;
import helper.CustomPoint;
import helper.Move;
import javacvdemo.ChrisKiller;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OFVideo implements Runnable {
	private Java2DFrameConverter converter1;
	private OpenCVFrameConverter.ToIplImage converter;
	private OpenCVFrameConverter.ToMat converterMat;
	private ImageView mainFrame;
	private ImageView polyFrame;
	private ImageView qrFrame;
	private ImageView landingFrame;
	private ImageView bufferedframe;
	private Label qrCode;
	private Label qrDist;
	private BufferedImage arg0;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private CommandController cC;
	private IARDrone drone;
	private static boolean aboveLanding = false;
	private static int circleCounter = 0;
	private static int counts = 0;
	// Scansequence fields
	private ScanSequence scanSequence;
	private boolean isFirst = true;
	public boolean wallClose = false;
	private ChrisKiller CK;
	
	public OFVideo(ImageView mainFrame, Label qrCode,
			Label qrDist, BufferedImage arg0, CommandController cC, ImageView bufferedframe) {
		this.arg0 = arg0;
		this.mainFrame = mainFrame;
		this.bufferedframe = bufferedframe;
		this.qrDist = qrDist;
		this.qrCode = qrCode;
		converter = new OpenCVFrameConverter.ToIplImage();
		converterMat = new ToMat();
		converter1 = new Java2DFrameConverter();
		scanSequence = new ScanSequence(cC);
		CK = new ChrisKiller(cC);
	}

	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	@Override
	public void run() {
		try {
			Mat newImg = null;
			while (true) {
				newImg = converterMat.convert(converter1.convert(arg0));
				Mat filteredImage = null;

				switch (PictureController.colorInt) {
				case 1:
					filteredImage = OFC.findContoursBlackMat(newImg);
					break;
				case 2:
					filteredImage = OFC.findContoursRedMat(newImg);
					break;
				case 3:
					filteredImage = OFC.findContoursGreenMat(newImg);
					BufferedImage bufferedImageCont = MatToBufferedImage(filteredImage);
					Image imageCont = SwingFXUtils.toFXImage(bufferedImageCont, null);
					bufferedframe.setImage(imageCont);
					break;
				default:
					filteredImage = OFC.findContoursBlueMat(newImg);
					break;
				}
				
				switch (PictureController.imageInt) {
				case PictureController.SHOW_QR:
					showQr(newImg.clone());
					break;
				case PictureController.SHOW_FILTER:
					showFilter(filteredImage.clone());
					break;
				case PictureController.SHOW_POLYGON:
					showPolygons(newImg.clone(), filteredImage.clone());
					break;
				case PictureController.SHOW_LANDING:
					showLanding(newImg.clone(), filteredImage.clone());
					break;
				default:
					showPolygons(newImg.clone(), filteredImage.clone());
					break;
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						qrCode.setText("QR Code: " + OFC.getQrCode());
						qrDist.setText("Dist: " + OFC.getDistance());

					}
				});
//				if (PictureController.shouldScan) {
//					scanSequence.setImage(newImg.clone());
//					if (isFirst) {
//						new Thread(scanSequence).start();
//						isFirst = false;
//					}
//				}
				if (PictureController.shouldScan){
					CK.setImage(newImg.clone());
					if(isFirst){
						new Thread(CK).start();
						isFirst = false;
					}
					scanSequence.imageChanged = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public void showQr(Mat camMat) {
		
		Mat qrMat = OFC.extractQRImage(camMat);
		BufferedImage bufferedImageQr = MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		mainFrame.setImage(imageQr);
	}

	public void showLanding(Mat mat, Mat filteredMat) throws InterruptedException {
		Mat landing = mat;
		int circles = 0;
		
//		if (PictureController.shouldScan) {
//			scanSequence.setImage(mat.clone());
//			if (isFirst) {
//				new Thread(scanSequence).start();
//				isFirst = false;
//			}
//		}
		
		boolean check = OFC.checkDecodedQR(mat);
		if(check){
			
			circles = OFC.myCircle(mat);
			
//			for(int i = 0; i < 4; ){
				if (circles > 0) {
					aboveLanding = true;
					// If false restart landing sequence
					//Drone skal flye lidt ned
					System.out.println("going down");
//					Thread.sleep(10);
					cC.dC.goDown(6);
					Thread.sleep(10);
					counts++;
					System.out.println(counts);
					}
				else {
						circles = 0;
						circleCounter++;
						System.out.println(circleCounter);
						
					}
				if(circleCounter>=120){
					aboveLanding = false;
					circleCounter = 0;
					counts = 0;
				}
				if(counts == 3){
					System.out.println("landing");
					
					cC.dC.land();
				}
//			}
		}
		BufferedImage bufferedImageLanding = MatToBufferedImage(landing);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		mainFrame.setImage(imageLanding);
		// System.out.println(aboveLanding);
		
	}

	public void showFilter(Mat filteredMat) {
		BufferedImage bufferedMatImage = MatToBufferedImage(filteredMat);
		Image imageFilter = SwingFXUtils.toFXImage(bufferedMatImage, null);
		mainFrame.setImage(imageFilter);

	}

	public void showPolygons(Mat camMat, Mat filteredMat) {
		filteredMat = OFC.erodeAndDilate(filteredMat);
		Mat polyImage = OFC.findPolygonsMat(camMat, filteredMat, 4);
		BufferedImage bufferedImage = MatToBufferedImage(polyImage);
		Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
		mainFrame.setImage(imagePoly);
	}

	public BufferedImage IplImageToBufferedImage(IplImage src) {
		OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}

	public BufferedImage MatToBufferedImage(Mat src) {
		OpenCVFrameConverter.ToMat grabberConverter = new OpenCVFrameConverter.ToMat();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}
}
