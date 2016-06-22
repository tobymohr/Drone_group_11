package picture;

import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;

import app.CommandController;
import flightcontrol.DownScanSeq;
import flightcontrol.FlightControl;
import flightcontrol.LandSequence;
import flightcontrol.ScanSequence;
import helper.Command;
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
	private ImageView bufferedframe;
	private Label qrCode;
	private Label qrDist;
	private BufferedImage arg0;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private static boolean aboveLanding = false;
	private static int circleCounter = 0;
	private static int counts = 0;
	// Scansequence fields
	private ScanSequence scanSequence;
	private DownScanSeq downScanSeq;
	private boolean isFirst = true;
	public boolean wallClose = false;
	public static volatile boolean imageChanged;
	public static volatile boolean imageChangedRed;
	public static volatile boolean imageChangedGreen;
	private Label movelbl;
	private Label coordinatFoundlbl;
	private LandSequence landSeq;
	private FlightControl fc2;
	
	
	public OFVideo(ImageView mainFrame, Label coordinatFoundlbl, Label movelbl, Label qrCode,
			Label qrDist, BufferedImage arg0, CommandController cC, ImageView bufferedframe) {
		this.arg0 = arg0;
		this.mainFrame = mainFrame;
		this.bufferedframe = bufferedframe;
		this.qrDist = qrDist;
		this.qrCode = qrCode;
		this.movelbl = movelbl;
		this.coordinatFoundlbl = coordinatFoundlbl;
		converter = new OpenCVFrameConverter.ToIplImage();
		converterMat = new ToMat();
		converter1 = new Java2DFrameConverter();
		scanSequence = new ScanSequence(cC);
		landSeq = new LandSequence(cC);
		fc2 = new FlightControl(cC);

	}

	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	@Override
	public void run() {
		try {
			Mat newImg = null;
			while (true) {
				if (PictureController.imageChanged) {
					PictureController.imageChanged = false;
					newImg = converterMat.convert(converter1.convert(arg0));
					Mat filteredImage = null;

				switch (PictureController.colorInt) {
				case 1:
					filteredImage = pictureProcessingHelper.findContoursBlackMat(newImg);
					break;
				case 2:
					filteredImage = pictureProcessingHelper.findContoursRedMat(newImg);
					break;
				case 3:
					filteredImage = pictureProcessingHelper.findContoursGreenMat(newImg);
					BufferedImage bufferedImageCont = MatToBufferedImage(filteredImage);
					Image imageCont = SwingFXUtils.toFXImage(bufferedImageCont, null);
					bufferedframe.setImage(imageCont);
					break;
				default:
					filteredImage = pictureProcessingHelper.findContoursBlueMat(newImg);
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
							if(CommandController.moveString != null){
								movelbl.setText("Move : " + CommandController.moveString);
							}
							
							if(PictureController.getPlacement().getX() != 0){
//								 coordinatFoundlbl.setVisible(true);
							}
							
						}
					});
					//TODO: sæt fc2 op
					if (PictureController.shouldFlyControl) {
						fc2.setImage(newImg.clone());
						if (isFirst) {
							new Thread(fc2).start();
							isFirst = false;
						}
						imageChanged = true;
					}
					
					if (PictureController.shouldScan) {
						scanSequence.setImage(newImg);
						if (isFirst) {
							new Thread(scanSequence).start();
							isFirst = false;
						}
						imageChanged = true;
					
					}
				} else {
					Thread.sleep(50);
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	public void showQr(Mat camMat) {

		Mat qrMat = pictureProcessingHelper.extractQRImage(camMat);
		BufferedImage bufferedImageQr = MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		mainFrame.setImage(imageQr);
	}

	public void showLanding(Mat mat, Mat filteredMat)
			throws InterruptedException {
		Mat landing = mat;
		int circles = 0;

		// if (PictureController.shouldScan) {
		// scanSequence.setImage(mat.clone());
		// if (isFirst) {
		// new Thread(scanSequence).start();
		// isFirst = false;
		// }
		// }

		boolean check = pictureProcessingHelper.checkDecodedQR(mat);
		if (check) {

			circles = pictureProcessingHelper.findCircle(mat);
//			for(int i = 0; i < 4; ){
				if (circles > 0) {
					aboveLanding = true;
					// If false restart landing sequence
					//Drone skal flyve lidt ned
					System.out.println("going down");
//					Thread.sleep(10);
					commandController.addCommand(Command.DOWN, 100, 20);
					Thread.sleep(200);
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
					
					commandController.droneInterface.land();
				}
//			}
		}
		BufferedImage bufferedImageLanding = MatToBufferedImage(landing);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		mainFrame.setImage(imageLanding);
		// System.out.println(aboveLanding);

	}

	public void showFilter(Mat filteredMat)

	{
		BufferedImage bufferedMatImage = MatToBufferedImage(filteredMat);
		Image imageFilter = SwingFXUtils.toFXImage(bufferedMatImage, null);
		mainFrame.setImage(imageFilter);

	}

	public void showPolygons(Mat camMat, Mat filteredMat) {
		filteredMat = pictureProcessingHelper.erodeAndDilate(filteredMat);
		Mat polyImage = pictureProcessingHelper.findPolygonsMat(camMat, filteredMat, 4);
		BufferedImage bufferedImage = MatToBufferedImage(polyImage);
		Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
		mainFrame.setImage(imagePoly);
	}

	public BufferedImage MatToBufferedImage(Mat src) {
		OpenCVFrameConverter.ToMat grabberConverter = new OpenCVFrameConverter.ToMat();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}
}
