package picture;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.awt.image.BufferedImage;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OFVideo implements Runnable {

	private Java2DFrameConverter converter1;
	private OpenCVFrameConverter.ToIplImage converter;
	private OpenCVFrameConverter.ToMat converterMat;
	private ImageView filterFrame;
	private ImageView polyFrame;
	private ImageView qrFrame;
	private ImageView landingFrame;
	private Label qrCode;
	private Label qrDist;
	private BufferedImage arg0;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();

	public OFVideo(ImageView filterFrame, ImageView polyFrame, ImageView qrFrame, ImageView landingFrame, Label qrCode,
			Label qrDist, BufferedImage arg0) {
		this.arg0 = arg0;
		this.filterFrame = filterFrame;
		this.polyFrame = polyFrame;
		this.qrFrame = qrFrame;
		this.qrDist = qrDist;
		this.landingFrame = landingFrame;
		this.qrCode = qrCode;
		converter = new OpenCVFrameConverter.ToIplImage();
		converterMat = new ToMat();
		converter1 = new Java2DFrameConverter();

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
					break;
				default:
					filteredImage = OFC.findContoursBlackMat(newImg);
					break;
				}

				showQr(newImg.clone());
				showLanding(newImg.clone(), filteredImage.clone());
				showPolygons(newImg.clone(), filteredImage.clone());
				showFilter(filteredImage.clone());

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						qrCode.setText("QR Code: " + OFC.getQrCode());
						qrDist.setText("Dist: " + OFC.getDistance());

					}
				});
				if (PictureController.shouldScan) {
					scanSequence(newImg.clone());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void scanSequence(Mat camMat) {
		List<Mat> contours = OFC.findQrContours(camMat);
		if(contours.size() != 0){
			RotatedRect rect = minAreaRect(contours.get(0));
			double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
			if(positionFromCenter == 0 && OFC.center(rect)){
				System.out.println("CENTER");
				Mat qrImg = OFC.warpImage(camMat.clone(), rect);
				String code = OFC.scanQrCode(qrImg);
				if(code != null){
					System.out.println("QR SCANNED: " + code);
					System.out.println(contours.size());
				}
			}
		}
	}


	public void showQr(Mat camMat) {
		Mat qrMat = OFC.extractQRImage(camMat);
		BufferedImage bufferedImageQr = MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		qrFrame.setImage(imageQr);
	}

	public void showLanding(Mat camMat, Mat filteredMat) {
		Mat landing = OFC.center(camMat.clone(), filteredMat.clone());
		BufferedImage bufferedImageLanding = MatToBufferedImage(landing);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		landingFrame.setImage(imageLanding);
	}

	public void showFilter(Mat filteredMat) {
		BufferedImage bufferedMatImage = MatToBufferedImage(filteredMat);
		Image imageFilter = SwingFXUtils.toFXImage(bufferedMatImage, null);
		filterFrame.setImage(imageFilter);

	}

	public void showPolygons(Mat camMat, Mat filteredMat) {
		filteredMat = OFC.erodeAndDilate(filteredMat);
		Mat polyImage = OFC.findPolygonsMat(camMat, filteredMat, 4);
		BufferedImage bufferedImage = MatToBufferedImage(polyImage);
		Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
		polyFrame.setImage(imagePoly);
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