package picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
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

import app.CommandController;
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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class PictureController  {

	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private CommandController cC;
	private IARDrone drone;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	public static String qrCodeText = "";
	FrameGrabber grabber = new OpenCVFrameGrabber(0);
	Set<KeyCode> pressedKeys = new HashSet<KeyCode>();

	public static int colorInt = 3;

	// CAMERA
	@FXML
	private HBox Hbox;
	@FXML
	private BorderPane borderpane;
	@FXML
	private ImageView polyFrame;
	@FXML
	private ImageView filterFrame;
	@FXML
	private ImageView landingFrame;
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
	

	public void setUpKeys(){
		borderpane.getScene().setOnKeyPressed(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent event){
	        	KeyCode note = event.getCode();
	        	
	        	if (!pressedKeys.contains(note)){
	        		System.out.println(note);
	        		pressedKeys.add(note);
	        	switch (event.getCode()) {
                case W:   cC.dC.goForward(10000);
                break;
                case S:   cC.dC.goBackwards(10000);
                break;
                case A:   cC.dC.goLeft(10000);
                break;
                case D:  cC.dC.goRight(10000);
                break;
				default:
					break;
            }
	        	
	        }
	        }
	    });
		
		borderpane.getScene().setOnKeyReleased(new EventHandler<KeyEvent>()
	    {
	        @Override
	        public void handle(KeyEvent event){
	        	pressedKeys.remove(event.getCode());
	        	System.out.println(event.getCode().toString() + " removed");
	        	cC.dC.hover();
	        }
	    });
	}
	
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
		 setUpKeys();
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
		cC = new CommandController(drone);
		cC.dC.setFrontCamera();
//		droneCommunicator.setBottomCamera();
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;
			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
					new Thread(ofvideo = new OFVideo(filterFrame, polyFrame, qrFrame,qrCode, qrDist, arg0)).start();
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
			Mat camMat = null;
			@Override
			public void run() {
				camMat = grabMatFromCam(converterMat, grabber);
				
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
				
				showQr(camMat);
				showLanding(camMat, filteredMat);
				showPolygons(camMat, filteredMat);
				showFilter(filteredMat);
				
				Platform.runLater(new Runnable() {
		            @Override public void run() {
		            	qrCode.setText("QR Code Found: " + OFC.getQrCode());
		            }
		        });
				
				isFirst = false;
				
			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
	}
	
	public void showQr(Mat camMat){
		Mat qrMat = OFC.extractQRImage(camMat);
		BufferedImage bufferedImageQr =  MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		qrFrame.setImage(imageQr);
	}
	
	public void showLanding(Mat camMat, Mat filteredMat){

		Mat qrMat = OFC.extractQRImage(camMat);
		Boolean check = OFC.CheckdecodedQR(qrMat);
		Mat landing = OFC.center(camMat.clone(), filteredMat.clone());
		BufferedImage bufferedImageLanding =  MatToBufferedImage(landing);
		Image imageLanding = SwingFXUtils.toFXImage(bufferedImageLanding, null);
		landingFrame.setImage(imageLanding);
	}
	
	public void showFilter(Mat filteredMat){
		BufferedImage bufferedMatImage = MatToBufferedImage(filteredMat);
		Image imageFilter = SwingFXUtils.toFXImage(bufferedMatImage, null);
		filterFrame.setImage(imageFilter);
		
	}
	
	public void showPolygons(Mat camMat, Mat filteredMat){
		filteredMat = OFC.erodeAndDilate(filteredMat);
		Mat polyImage = OFC.findPolygonsMat(camMat,filteredMat,4);
		BufferedImage bufferedImage = MatToBufferedImage(polyImage);
		Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
		polyFrame.setImage(imagePoly);
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
		cC.emergencyStop();
	}
	
	public void land() {
		cC.dC.land();
	}
	
	public void takeOff() {
		System.out.println("TAKEOFF");
		cC.dC.takeOff();
	}

}
