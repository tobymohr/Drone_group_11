package picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import static org.bytedeco.javacpp.helper.opencv_core.*;
import org.bytedeco.javacpp.opencv_videoio.CvCapture;
import org.bytedeco.javacv.Frame;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import com.google.zxing.Result;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;
import org.bytedeco.javacv.*;
import static org.bytedeco.javacpp.opencv_core.*;
import app.DroneCommunicator;
import app.DroneInterface;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.VideoBitRateMode;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.video.ImageListener;
import de.yadrone.base.video.VideoManager;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PictureController {

	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private DroneInterface droneCommunicator;
	private IARDrone drone;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	private Result qrCodeResult;
	public static String qrCodeText = "";
	FrameGrabber grabber = new OpenCVFrameGrabber(0);

	public static int colorInt = 3;

	// CAMERA
	@FXML
	private ImageView polyFrame;
	@FXML
	private ImageView filterFrame;
	@FXML
	private ImageView qrFrame;
	@FXML
	private static Slider minimumThresh;
	@FXML
	private static Slider maximumThresh;
	@FXML
	private Label qrCode;
	@FXML
	private Label qrDist;
	

	public PictureController() throws Exception {
	
	}

	private void setDimension(ImageView image, int dimension) {
		image.setFitWidth(dimension);
		image.setPreserveRatio(true);
	}

	@FXML
	protected void startCamera() {
//		 setDimension(polyFrame, 800);
//			setDimension(filterFrame, 800);
//			setDimension(qrFrame, 800);
		try {
			grabFromVideo();
			
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
			try {
				grabber.restart();
			} catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.err.println("Couldn't restart grabber");
			}
		}
	}

	public void startDrone() {
		 initDrone();
//		 setDimension(polyFrame, 800);
//			setDimension(filterFrame, 800);
//			setDimension(qrFrame, 800);
		 grabFromDrone();
		 land();
//		OpticalFlowCalculator OFC = new OpticalFlowCalculator();
//		OFC.testWarp();
	}
	
	public static double getMinThresh(){
		
		return  minimumThresh.getValue();
	}
	
	public static double getMaxThresh(){
		return  minimumThresh.getValue();
	}

	public void initDrone() {
		drone = new ARDrone();
		drone.addExceptionListener(new IExceptionListener() {

			@Override
			public void exeptionOccurred(ARDroneException e) {
				e.printStackTrace();
			}
		});
		drone.start();
		droneCommunicator = new DroneCommunicator(drone);
		droneCommunicator.setFrontCamera();
//		droneCommunicator.setBottomCamera();
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;
			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
					new Thread(ofvideo = new OFVideo(filterFrame, polyFrame, qrFrame,qrCode, arg0)).start();
					isFirst = false;
				}
				ofvideo.setArg0(arg0);
			}
		});
		drone.getCommandManager().setVideoBitrate(100000);
		
	}

	public void grabFromVideo() throws org.bytedeco.javacv.FrameGrabber.Exception {
		OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
		OpenCVFrameConverter.ToMat converterMat = new OpenCVFrameConverter.ToMat();

		 FrameGrabber grabber = new VideoInputFrameGrabber(0);
		grabber.start();

		Runnable frameGrabber = new Runnable() {
			boolean isFirst = true;
			IplImage camImage;
			Mat camMat = null;
			Mat camImageOld = null;
			@Override
			public void run() {
				
				
//				if(!isFirst){
//					camMat  = new Mat();
//					camImageOld= new Mat(camMat);
////					CvtColor(camImageOld, camImageOld, CV_BGR2GRAY);
//				}		
				
				camMat = grabMatFromCam(converterMat, grabber);
				camImage = grabFromCam(converter, grabber);
				
				
//				IplImage filteredImage = null;
				Mat filteredMat = null;
				
				switch(colorInt){
				case 1:
					filteredMat = OFC.findContoursBlackMat(camMat);
					break;
				case 2: 
					filteredMat = OFC.findContoursRedMat(camMat);
					break;
				case 3: 
					filteredMat = OFC.findContoursGreenMat(camMat);
					break;
				default: 
//					filteredImage = OFC.findContoursBlue(camImage);
					break;
				}
				
			
				//Filter
				BufferedImage bufferedMatImage = MatToBufferedImage(filteredMat);
				Image imageFilter = SwingFXUtils.toFXImage(bufferedMatImage, null);
				filterFrame.setImage(imageFilter);
				
				//POLY
				filteredMat = OFC.erodeAndDilate(filteredMat);
				Mat polyImage = OFC.findPolygonsMat(camMat,filteredMat,4);
				BufferedImage bufferedImage = MatToBufferedImage(polyImage);
				Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
				polyFrame.setImage(imagePoly);
				
				
//				//Optical Flow
//				if(!isFirst){
//				IplImage opticalImage = OFC.opticalFlowOnDrones(camImageOld, camImage);
//				BufferedImage bufferedImageOptical = IplImageToBufferedImage(opticalImage);
//				Image imageOptical = SwingFXUtils.toFXImage(bufferedImageOptical, null);
//				polyFrame.setImage(imageOptical);
//				}
				
//				QR
				Mat qrImage = OFC.extractQRImage(camMat);
				BufferedImage bufferedImageQr =  MatToBufferedImage(qrImage);
				Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
				qrFrame.setImage(imageQr);
				
//				Platform.runLater(new Runnable() {
//		            @Override public void run() {
//		            	qrCode.setText("QR Code Found: " + OFC.getQrCode());
//		            }
//		        });
				
				isFirst = false;
				
			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
	}	
	
	public Mat grabMatFromCam(OpenCVFrameConverter.ToMat converter, FrameGrabber grabber){
		Mat newImg = null;
		try {
			newImg = converter.convert(grabber.grab());
			
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
			try {
				grabber.start();
			} catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.err.println("Couldn't restart grabber");
			}
		}
		
		return newImg;
		
	}
	
	public IplImage grabFromCam(OpenCVFrameConverter.ToIplImage converter, FrameGrabber grabber){
		IplImage newImg = null;
		try {
			newImg = converter.convert(grabber.grab());
			
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
			
		}
		
		return newImg;
		
	}
	
	public void trackBlack(){
		colorInt = 1;
	}
	
	public void trackRed(){
		colorInt = 2;
	}
	
	public void trackGreen(){
		colorInt = 3;
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
	
	public void emergencyStop() {
		droneCommunicator.emergencyStop();
	}
	
	public void land() {
		droneCommunicator.freeze();
		droneCommunicator.land();
	}
	
	public void takeOff() {
		System.out.println("TAKEOFF");
		droneCommunicator.takeOff();
	}

}
