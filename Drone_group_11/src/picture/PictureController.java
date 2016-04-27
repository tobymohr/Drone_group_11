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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import static org.bytedeco.javacpp.opencv_imgproc.*;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
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
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PictureController {

	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private DroneInterface droneCommunicator;
	private IARDrone drone;
	private Video video;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	private Result qrCodeResult;


	public static int colorInt = 0;

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
	

	public PictureController() throws Exception {
	
	}

	private void setDimension(ImageView image, int dimension) {
		image.setFitWidth(dimension);
		image.setPreserveRatio(true);
	}

	@FXML
	protected void startCamera() {
		 setDimension(polyFrame, 800);
			setDimension(filterFrame, 800);
			setDimension(qrFrame, 800);
		try {
			grabFromVideo();
			
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		}
	}

	public void startDrone() {
		 initDrone();
		 setDimension(polyFrame, 800);
			setDimension(filterFrame, 800);
			setDimension(qrFrame, 800);
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
					new Thread(ofvideo = new OFVideo(filterFrame, polyFrame, qrFrame, arg0)).start();
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
		// Works with Tobias CAM - adjust grabFromCam accordingly
		OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
		
		// Works with Mathias CAM - adjust grabFromCam accordingly
//		 FrameGrabber grabber = new VideoInputFrameGrabber(0);
		grabber.start();
		
		

		Runnable frameGrabber = new Runnable() {
			boolean isFirst = true;
//			IplImage camImage = null;
			Mat camMat = null;
			@Override
			public void run() {
				
				
				if(!isFirst){
					camMat  = new Mat();
//					camImageOld= IplImage.create(camImage.width(), camImage.height(), IPL_DEPTH_8U, 1);
//					cvCvtColor(camImage, camImageOld, CV_BGR2GRAY);
				}		
				
				camMat = grabMatFromCam(converterMat, grabber);
//				camImage = grabFromCam(converter, grabber);
				
				
//				IplImage filteredImage = null;
				Mat filteredMat = null;
				
				switch(colorInt){
				case 1:
//					filteredImage = OFC.findContoursBlack(camImage);
					break;
				case 2: 
//					filteredImage = OFC.findContoursRed(camImage);
					break;
				case 3: 
//					filteredImage = OFC.findContoursGreen(camImage);
					break;
				default: 
					filteredMat = OFC.findContoursRedMat(camMat);
//					filteredImage = OFC.findContoursBlue(camImage);
					break;
				}

				//POLY
//				filteredImage = OFC.erodeAndDilate(filteredImage);
//				IplImage polyImage = OFC.findPolygons(camImage,filteredImage,4);
//				BufferedImage bufferedImage = IplImageToBufferedImage(polyImage);
//				Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
//				polyFrame.setImage(imagePoly);
				
//				QR
//				IplImage qrImage = OFC.extractQRImage(camImage);
//				BufferedImage bufferedImageQr = IplImageToBufferedImage(qrImage);
//				Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);

//				qrFrame.setImage(imageQr);
				
//				try {
//					qrCodeResult = new MultiFormatReader().decode(bm);
//					System.out.println(qrCodeResult.getText());
//					Thread.sleep(2000);
//				} catch (NotFoundException e) {
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
			
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
				
				
				//Optical Flow
//				if(!isFirst){
//				IplImage opticalImage = OFC.opticalFlowOnDrones(camImageOld, camImage);
//				BufferedImage bufferedImageOptical = IplImageToBufferedImage(opticalImage);
//				Image imageOptical = SwingFXUtils.toFXImage(bufferedImageOptical, null);
//				polyFrame.setImage(imageOptical);
//				}
				
				
				
				isFirst = false;
				
			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
	}	
	
	public Mat grabMatFromCam(OpenCVFrameConverter.ToMat converter, OpenCVFrameGrabber grabber){
		Mat newImg = null;
		try {
			newImg = converter.convert(grabber.grab());
			
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
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
