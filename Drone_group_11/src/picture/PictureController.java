package picture;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_videoio.CvCapture;
import org.bytedeco.javacv.Frame;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import app.DroneCommunicator;
import app.DroneInterface;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.video.ImageListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PictureController {

	private OpticalFlowCalculator OFC = new OpticalFlowCalculator();
	private DroneInterface droneCommunicator;
	private IARDrone drone;
	private Video video;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;

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
		IplImage f = cvLoadImage("0QR.jpg");
		OFC = new OpticalFlowCalculator();
		IplImage qrImage = OFC.extractQRImage(f);
		setDimension(polyFrame, 600);
		BufferedImage bufferedImageQr = IplImageToBufferedImage(qrImage);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		polyFrame.setImage(imageQr);

//		setDimension(filterFrame, 600);
//		try {
//			grabFromVideo();
//		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
//			e.printStackTrace();
//		}
	}

	public void startDrone() {
		 initDrone();
		 setDimension(polyFrame, 800);
			setDimension(filterFrame, 800);
			setDimension(qrFrame, 800);
		 grabFromDrone();
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
//		droneCommunicator.setFrontCamera();
		droneCommunicator.setBottomCamera();
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;
			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
//					new Thread(video = new Video(polyFrame, arg0)).start();
					new Thread(ofvideo = new OFVideo(filterFrame, polyFrame, arg0)).start();
					isFirst = false;
				}
//				video.setArg0(arg0);
				ofvideo.setArg0(arg0);
			}
		});
	}

	public void grabFromVideo() throws org.bytedeco.javacv.FrameGrabber.Exception {
		OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
		
		// Works with Tobias CAM - adjust grabFromCam accordingly
		OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
		
		// Works with Mathias CAM - adjust grabFromCam accordingly
//		 FrameGrabber grabber = new VideoInputFrameGrabber(0);
		grabber.start();


		Runnable frameGrabber = new Runnable() {
			@Override
			public void run() {
				IplImage camImage = grabFromCam(converter, grabber);
				
				IplImage filteredImage = null;
				
				switch(colorInt){
				case 1:
					filteredImage = OFC.findContoursBlack(camImage);
					break;
				case 2: 
					filteredImage = OFC.findContoursRed(camImage);
					break;
				case 3: 
					filteredImage = OFC.findContoursGreen(camImage);
					break;
				default: 
					filteredImage = OFC.findContoursBlue(camImage);
					break;
				}
				
				IplImage polyImage = OFC.findPolygons(camImage,filteredImage,4);
				IplImage f = cvLoadImage("OQR.jpg");
				IplImage qrImage = OFC.extractQRImage(f);
				BufferedImage bufferedImage = IplImageToBufferedImage(polyImage);
				BufferedImage bufferedImageFilter = IplImageToBufferedImage(filteredImage);
				BufferedImage bufferedImageQr = IplImageToBufferedImage(qrImage);
				Image imageFilter = SwingFXUtils.toFXImage(bufferedImageFilter, null);
				Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
				Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
				polyFrame.setImage(imagePoly);
				filterFrame.setImage(imageFilter);
				qrFrame.setImage(imageQr);
			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
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
	
	public void emergencyStop() {
		droneCommunicator.emergencyStop();
	}
	
	public void land() {
		droneCommunicator.freeze();
		droneCommunicator.land();
	}

}
