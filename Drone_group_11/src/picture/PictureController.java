package picture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import de.yadrone.base.command.VideoCodec;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.video.ImageListener;
import de.yadrone.base.video.VideoManager;
import helper.Circle;
import helper.CustomPoint;
import helper.Move;
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

public class PictureController {

	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private CommandController cC;
	private IARDrone drone;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	public static String qrCodeText = "";
	private FrameGrabber grabber = new OpenCVFrameGrabber(0);
	private Set<KeyCode> pressedKeys = new HashSet<KeyCode>();
	private Mat camMat = null;
	public static boolean shouldScan = true;
	private static boolean aboveLanding = false;
	private static int circleCounter = 0;
	public BufferedImage billede;
	public int navn = 0;
	public int speed = 10;
	public int duration = 3000;
	public static int colorInt = 4;

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

	public void setUpKeys() {
		borderpane.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				KeyCode note = event.getCode();

				if (!pressedKeys.contains(note)) {
					System.out.println(note);
					pressedKeys.add(note);
					switch (event.getCode()) {
	                case W:   cC.addCommand(1, duration);
	                break;
	                case S:   cC.addCommand(2, duration);
	                break;
	                case A:   cC.addCommand(3, duration);
	                break;
	                case D:   cC.addCommand(4, duration);
	                break;
	                case M:		cC.dC.land();
	                break;
	                case F:
	                	navn++;
	                	String navn2 = navn+".jpg";
	File outputfile = new File(navn2);
						try {
							ImageIO.write(billede, "jpg", outputfile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

	break;
	                case G:
	                case H:
	                	cC.emergencyStop();
	                	break;
	                case UP:
	                	cC.addCommand(5, duration);
	                	break;
	                case DOWN:
	                	cC.addCommand(6, duration);
	                	break;
	                case LEFT:
	                	cC.dC.spinLeft(duration);
	                	break;
	                case RIGHT:
	                	cC.dC.spinRight(duration);
	                	break;
	                case ENTER:
	                	cC.dC.hover();
	                	break;
	                case O:
	                	speed += 5;
	                	cC.dC.setSpeed(speed);
	                	qrCode.setText("speed: "+speed);
	                break;
	                case I:
	                	speed -= 5;
	                	cC.dC.setSpeed(speed);
	                	qrCode.setText("speed: "+speed);
	               break;
	                case L:
	                	duration += 250;
	                	qrDist.setText("duration: "+ duration);
	                	break;
	                case K:
	                	duration -= 250;
	                	qrDist.setText("duration: "+ duration);
	                	break;
					case NUMPAD1:
						OFC.blueMin -= 1;
						System.out.println("Min: " + OFC.blueMin);
						break;
					case NUMPAD2:
						OFC.blueMin += 1;
						System.out.println("Min: " + OFC.blueMin);
						break;
					case NUMPAD4:
						OFC.blueMax -= 1;
						System.out.println("Max: " + OFC.blueMax);
						break;
					case NUMPAD5:
						OFC.blueMax += 1;
						System.out.println("Max: " + OFC.blueMax);
						break;
					default:

						break;
					}

				}
			}
		});

		borderpane.getScene().setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				pressedKeys.remove(event.getCode());
				System.out.println(event.getCode().toString() + " removed");
				cC.dC.hover();
			}
		});
	}

	private void setDimension(ImageView image, int dimension) {
		image.setFitWidth(dimension);
		image.setPreserveRatio(true);
	}

	@FXML
	protected void startCamera() {
		try {
			setUpKeys();
			grabFromVideo();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		}
	}

	public void startDrone() {
		initDrone();
		setUpKeys();
		grabFromDrone();
		land();
	}

	public static double getMinThresh() {

		return minimumThresh.getValue();
	}

	public static double getMaxThresh() {
		return minimumThresh.getValue();
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
		drone.getCommandManager().setVideoCodec(VideoCodec.H264_720P);
		cC.dC.setFrontCamera();
		new Thread(cC).start();
		// droneCommunicator.setBottomCamera();
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;

			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
					new Thread(
							ofvideo = new OFVideo(filterFrame, polyFrame, qrFrame, landingFrame, qrCode, qrDist, arg0, cC))
									.start();
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

			@Override
			public void run() {

				camMat = grabMatFromCam(converterMat, grabber);

				Mat filteredMat = null;

				switch (colorInt) {
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
					filteredMat = OFC.findContoursBlueMat(camMat);

					break;
				}

				showQr(camMat.clone());
				showLanding(camMat.clone());
				showPolygons(camMat, filteredMat);
				showFilter(filteredMat);
				if (shouldScan) {
					scanSequence(camMat.clone());
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						qrCode.setText("QR Code Found: " + OFC.getQrCode());
						qrDist.setText("Dist: " + OFC.getDistance());
					}
				});

				isFirst = false;

			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
	}

	public void scanSequence(Mat camMat) {
	}

	public void showQr(Mat camMat) {
		Mat qrMat = OFC.extractQRImage(camMat);
		BufferedImage bufferedImageQr = MatToBufferedImage(qrMat);
		Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);
		qrFrame.setImage(imageQr);
	}

	public void showLanding(Mat mat) {
//		Mat landing = OFC.center(camMat.clone(), filteredMat.clone());
		Mat landing = mat;
		int circles = 0;
		boolean check = OFC.checkDecodedQR(mat);
		if(check){
			circles = OFC.myCircle(mat);
		}
		if(circles > 0 ){
			aboveLanding = true;
			//If false restart landing sequence
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

	public Mat grabMatFromCam(OpenCVFrameConverter.ToMat converter, FrameGrabber grabber) {
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

	public IplImage grabFromCam(OpenCVFrameConverter.ToIplImage converter, FrameGrabber grabber) {
		IplImage newImg = null;
		try {
			newImg = converter.convert(grabber.grab());
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();

		}
		return newImg;
	}

	public void trackBlack() {
		colorInt = 1;
	}

	public void trackRed() {
		colorInt = 2;
	}

	public void trackGreen() {
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
		shouldScan = true;
		cC.dC.takeOff();
		//#TODO Adjust height to line up with QR codes.
	}

}
