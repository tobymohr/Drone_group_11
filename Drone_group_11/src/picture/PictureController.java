package picture;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import app.DroneCommunicator;
import app.DroneInterface;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.video.ImageListener;
import helper.Circle;
import helper.Point;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
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

	public PictureController() throws Exception {
	}

	private void setDimension(ImageView image, int dimension) {
		image.setFitWidth(dimension);
		image.setPreserveRatio(true);
	}

	@FXML
	protected void startCamera() {
		setDimension(polyFrame, 400);
		setDimension(filterFrame, 400);
		try {
			grabFromVideo();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		}
	}

	public void startDrone() {
		 initDrone();
		 grabFromDrone();
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
		OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
		grabber.start();
		// Declare img as IplImage

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
					filteredImage = OFC.findContoursBlack(camImage);
					break;
				}
				
				IplImage polyImage = OFC.findPolygons(camImage,filteredImage,4);
				BufferedImage bufferedImage = IplImageToBufferedImage(polyImage);
				BufferedImage bufferedImageFilter = IplImageToBufferedImage(filteredImage);
				Image imageFilter = SwingFXUtils.toFXImage(bufferedImageFilter, null);
				Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
				polyFrame.setImage(imagePoly);
				filterFrame.setImage(imageFilter);
			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
	}	
	
	public IplImage grabFromCam(OpenCVFrameConverter.ToIplImage converter, OpenCVFrameGrabber grabber){
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
