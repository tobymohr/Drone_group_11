package picture;

import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;

import dronecontrol.CommandController;
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

public class DroneVideo implements Runnable {
	private Java2DFrameConverter converter1;
	private OpenCVFrameConverter.ToMat converterMat;
	private ImageView mainFrame;
	private BufferedImage arg0;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private static int circleCounter = 0;
	private static int counts = 0;
	// Scansequence fields
	private ScanSequence scanSequence;
	public static boolean isFirst = true;
	public boolean wallClose = false;
	public static volatile boolean imageChanged;
	public static volatile boolean imageChangedRed;
	public static volatile boolean imageChangedGreen;
	private Label movelbl;
	private LandSequence landSeq;
	private FlightControl fc2;
	private DownScanSeq down;

	public DroneVideo(ImageView mainFrame, Label movelbl, BufferedImage arg0, CommandController cC,
			ImageView bufferedframe) {
		this.arg0 = arg0;
		this.mainFrame = mainFrame;
		this.movelbl = movelbl;
		converterMat = new ToMat();
		converter1 = new Java2DFrameConverter();
		landSeq = new LandSequence(cC);
		down = new DownScanSeq(cC);
		scanSequence = new ScanSequence(cC, this.down);
		fc2 = new FlightControl(cC, down);

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
						showLanding(newImg.clone());
						break;
					default:
						showPolygons(newImg.clone(), filteredImage.clone());
						break;
					}

					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (CommandController.moveString != null) {
								movelbl.setText("Move : " + CommandController.moveString);
							}

						}
					});
					// TODO: sæt fc2 op
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
						down.setImage(newImg.clone());
						if (isFirst) {
							new Thread(scanSequence).start();
							isFirst = false;
						}
						imageChanged = true;

					}
					if (PictureController.shouldLand) {
						landSeq.setImage(newImg.clone());
						imageChanged = true;
						if (isFirst) {
							new Thread(landSeq).start();
							isFirst = false;
						}
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

	public void showLanding(Mat mat) throws InterruptedException {
		BufferedImage bufferedImageLanding = MatToBufferedImage(mat);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		mainFrame.setImage(imageLanding);

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
