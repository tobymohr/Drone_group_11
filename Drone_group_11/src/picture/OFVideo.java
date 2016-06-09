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

import helper.CustomPoint;
import helper.Move;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OFVideo implements Runnable {
	//#TODO Tweak these values based on testing
	private static final double CENTER_UPPER = 0.22;
	private static final double CENTER_LOWER = -0.4;
	private static final double CENTER_DIFFERENCE = -0.2;
	
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
	private static boolean aboveLanding = false;
	private static int circleCounter = 0;
	
	// Scansequence fields
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;

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
					filteredImage = OFC.findContoursBlueMat(newImg);
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
		//#TODO Check if wall is close
		boolean wallClose = false;
		if (wallClose) {
			//#TODO Fly backwards (4-5 meters)
			//#TODO Rotate 90 degrees
			return;
		}
		
		List<Mat> contours = OFC.findQrContours(camMat);

		if(contours.size() != 0) {
			RotatedRect rect = minAreaRect(contours.get(0));
			double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
			if (positionFromCenter != 0) {
				System.out.println("PositionFromCenter: " + positionFromCenter);
				//#TODO Rotate <positionFromCenter> pixels to center the QR code in image
				return;
			}
			double center = OFC.center(rect);
			if (center > CENTER_UPPER || center < CENTER_LOWER) {
				//#TODO Strafe the drone <center> amount. Right is chosen as standard.
				if (previousCenter == -1) {
					// Record center in order to react to it next iteration
					previousCenter = center;
				} else {
					double difference = previousCenter - center;
					if (difference < CENTER_DIFFERENCE) {
						// We moved the wrong way. Change strafe direction.
						strafeRight = !strafeRight;
					}
				}
				return;
			}
			// Reset the previous center
			previousCenter = -1;
			
			Mat qrImg = OFC.warpImage(camMat.clone(), rect);
			
			String tempCode = OFC.scanQrCode(qrImg);

			if (tempCode != null) {
				// Code stored as field, we need to use it even if we're too far away to scan it.
				//#TODO Ensure that the field code is set to null every time we need to reset.  
				code = tempCode;
			}
			if(code != null){
				// Check amount of squares found
				// #TODO Implement some way to check squares across more than one frame
				if (contours.size() == 3) {
					//#TODO Calculate distance and placement in coordinate system
					CustomPoint placement = new CustomPoint();
					moveDroneToStart(placement);
					code = null;
					rotateCount = 0;
					PictureController.shouldScan = false;
					return;
				} else {
					//#TODO Fly backwards (0.5 meters)
					return;
				}
			} else {
				double distanceToSquare = OFC.calcDistance(rect);
				// It might still be a QR code, we're too far away to know
				if (distanceToSquare > 100) {
					//#TODO Fly closer to the square (0.5 meters)
					return;
				} else {
					//#TODO Fly backwards (4-5 meters)
					//#TODO Rotate 90 degrees
					return;
				}
			}
		} else {
			if (rotateCount != 4) {
				//#TODO Rotate 90 degrees
				rotateCount++;
			} else {
				//#TODO Fly forwards (1 meter)
			}
			return;
		}
	}
	
	private void moveDroneToStart(CustomPoint placement) {
		List<Move> moves = OFC.calcMoves(placement.getX(), placement.getY());
		for (Move move : moves) {
			//#TODO Actually move the drone according to moves
		}
	}


	public void showQr(Mat camMat) {
		Mat qrMat = OFC.extractQRImage(camMat);
		BufferedImage bufferedImageQr = MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		qrFrame.setImage(imageQr);
	}

	public void showLanding(Mat mat, Mat filteredMat) {
		Mat landing = mat;
		int circles = 0;
		boolean check = OFC.checkDecodedQR(mat);
		if(check){
			circles = OFC.myCircle(mat);
		}
		if(circles > 0 ){
			aboveLanding = true;
		}else{
			circles = 0;
			circleCounter++;
		}
		if(circleCounter >= 120){
			aboveLanding = false;
			circleCounter = 0;
		}
		BufferedImage bufferedImageLanding = MatToBufferedImage(landing);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		landingFrame.setImage(imageLanding);
//		System.out.println(aboveLanding);
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
