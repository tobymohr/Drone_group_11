package picture;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.VideoInputFrameGrabber;

import app.CommandController;
import coordinateSystem.Map;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.H264;
import de.yadrone.base.command.VideoBitRateMode;
import de.yadrone.base.command.VideoCodec;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.navdata.BatteryListener;
import de.yadrone.base.video.ImageListener;
import helper.Command;
import helper.CustomPoint;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;

public class PictureController {
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private IARDrone drone;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	public static String qrCodeText = "";
	private Set<KeyCode> pressedKeys = new HashSet<KeyCode>();
	private Mat camMat = null;
	public static boolean shouldScan = false;
	public static boolean shouldTestWall = false;
	public static boolean shouldLand = false;
	public static boolean shouldFlyControl = false;

	public BufferedImage billede;
	public int navn = 0;
	public int speed = 10;
	public int duration = 3000;
	public static int colorInt = 4;
	public static boolean restart = false;
	public static final int SHOW_QR = 0;
	public static final int SHOW_FILTER = 1;
	public static final int SHOW_POLYGON = 2;
	public static final int SHOW_LANDING = 3;
	public static int imageInt = SHOW_POLYGON;
	private int prevBattery = 0;
	public static volatile boolean imageChanged;
	private static Map map;

	// CAMERA
	@FXML
	private HBox Hbox;
	@FXML
	private BorderPane borderpane;
	@FXML
	private ImageView mainFrame;
	@FXML
	private static Slider minimumThresh;
	@FXML
	private static Slider maximumThresh;
	@FXML
	private Label qrCode;
	@FXML
	private Label qrDist;
	@FXML
	private Label headingLbl;
	@FXML
	private ImageView bufferedframe;
	@FXML
	private Label lowBatteryLbl;
	@FXML
	private Label movelbl;
	@FXML
	private Label coordinatFoundlbl;

	public void setUpKeys() {
		borderpane.getScene().getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				if (commandController != null) {
					if (commandController.droneInterface.getDroneFlying()) {
						land();
					}
				}
				Platform.exit();
				System.exit(0);
			}
		});

		borderpane.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				KeyCode note = event.getCode();

				if (!pressedKeys.contains(note)) {
					pressedKeys.add(note);
					switch (event.getCode()) {
					case W:
						commandController.addCommand(Command.FORWARD, duration, speed);
						break;
					case S:
						commandController.addCommand(Command.BACKWARDS, duration, speed);
						break;
					case A:
						commandController.addCommand(Command.LEFT, duration, speed);
						break;
					case D:
						commandController.addCommand(Command.RIGHT, duration, speed);
						break;
					case M:
						commandController.droneInterface.land();
						break;
					case F:
						navn++;
						String navn2 = navn + ".jpg";
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
						commandController.emergencyStop();
						break;
					case UP:
						commandController.addCommand(Command.UP, duration, speed);
						break;
					case DOWN:
						commandController.addCommand(Command.DOWN, duration, speed);
						break;
					case LEFT:
						commandController.addCommand(Command.SPINLEFT, duration, speed);
						break;
					case RIGHT:
						commandController.addCommand(Command.SPINRIGHT, duration, speed);
						break;
					case ENTER:
						commandController.droneInterface.hover();
						break;
					case O:
						speed += 5;
						commandController.droneInterface.setSpeed(speed);
						qrCode.setText("speed: " + speed);
						break;
					case I:
						speed -= 5;
						commandController.droneInterface.setSpeed(speed);
						qrCode.setText("speed: " + speed);
						break;
					case L:
						duration += 100;
						qrDist.setText("duration: " + duration);
						break;
					case K:
						duration -= 100;
						qrDist.setText("duration: " + duration);
						break;
					case NUMPAD1:
						pictureProcessingHelper.blueMin -= 1;
						break;
					case NUMPAD2:
						pictureProcessingHelper.blueMin += 1;
						break;
					case NUMPAD4:
						pictureProcessingHelper.blueMax -= 1;
						break;
					case NUMPAD5:
						pictureProcessingHelper.blueMax += 1;
						break;
					case T:
						commandController.droneInterface.setBottomCamera();
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
		commandController = new CommandController(drone);
		drone.getCommandManager().setVideoCodec(VideoCodec.H264_720P);
		commandController.droneInterface.setFrontCamera();
		// cC.droneInterface.setBottomCamera();
		new Thread(commandController).start();
		map = Map.init(new ArrayList<>());
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		drone.getNavDataManager().addBatteryListener(new BatteryListener() {

			@Override
			public void voltageChanged(int arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void batteryLevelChanged(int arg0) {
				if (arg0 != prevBattery) {
					prevBattery = arg0;
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							lowBatteryLbl.setText("Battery: " + arg0 + "%");
							if (arg0 < 24) {
								lowBatteryLbl.setText("Battery level is low: " + arg0 + "%");
							}
						}
					});

					lowBatteryLbl.setVisible(true);
				}

			}
		});
		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;

			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
					new Thread(ofvideo = new OFVideo(mainFrame, movelbl, arg0, commandController, bufferedframe))
							.start();
					isFirst = false;
				}
				ofvideo.setArg0(arg0);
				billede = arg0;
				imageChanged = true;
			}
		});
		drone.getCommandManager().setVideoBitrateControl(VideoBitRateMode.MANUAL);
		drone.getCommandManager().setVideoBitrate(H264.MAX_BITRATE);
		drone.getCommandManager().setVideoCodecFps(H264.MAX_FPS);
	}

	public void grabFromVideo() throws org.bytedeco.javacv.FrameGrabber.Exception {

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
					filteredMat = pictureProcessingHelper.findContoursBlackMat(camMat);
					break;
				case 2:
					filteredMat = pictureProcessingHelper.findContoursRedMat(camMat);
					break;
				case 3:
					filteredMat = pictureProcessingHelper.findContoursGreenMat(camMat);
					break;
				default:
					filteredMat = pictureProcessingHelper.findContoursBlueMat(camMat);

					break;
				}

				switch (imageInt) {
				case SHOW_QR:
					showQr(camMat.clone());
					break;
				case SHOW_FILTER:
					showFilter(filteredMat);
					break;
				case SHOW_POLYGON:
					showPolygons(camMat, filteredMat);
					break;
				case SHOW_LANDING:
					try {
						showLanding(camMat.clone());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				default:
					showPolygons(camMat, filteredMat);
					break;
				}

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						qrCode.setText("QR Code Found: " + pictureProcessingHelper.getQrCode());
						qrDist.setText("Dist: " + pictureProcessingHelper.getDistance());
					}
				});

				isFirst = false;

			}
		};
		timer = Executors.newSingleThreadScheduledExecutor();
		timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
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

	public void showFilter(Mat filteredMat) {
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

	public void trackBlack() {
		colorInt = 1;
	}

	public void trackRed() {
		colorInt = 2;
	}

	public void trackGreen() {
		colorInt = 3;
	}

	public BufferedImage MatToBufferedImage(Mat src) {
		OpenCVFrameConverter.ToMat grabberConverter = new OpenCVFrameConverter.ToMat();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}

	public void emergencyStop() {
		commandController.emergencyStop();
	}

	public void land() {
		commandController.droneInterface.land();
		shouldScan = false;
	}

	public void takeOff() throws InterruptedException {
		// cC.droneInterface.takeOff();
		// shouldFlyControl = true;
		// shouldLand = true;
		shouldScan = true;
	}

	public void showQr() {
		imageInt = SHOW_QR;
	}

	public void showPolygon() {
		imageInt = SHOW_POLYGON;
	}

	public void showLanding() {
		imageInt = SHOW_LANDING;
	}

	public void showFilter() {
		imageInt = SHOW_FILTER;
	}

	public static void addCord(CustomPoint point) {
		map.addCord(point);
	}

	public static void addCord(CustomPoint point, CustomPoint placement) {
		map.addCord(point, placement);
	}

	public static void addCords(ArrayList<CustomPoint> tempList) {
		map.addCords(tempList);
	}

	public static void addCords(ArrayList<CustomPoint> tempList, Color color) {
		map.addCords(tempList, color);
	}

	public static void addCords(ArrayList<CustomPoint> tempList, CustomPoint placement) {
		map.addCords(tempList, placement);
	}

	public static CustomPoint getPlacement() {
		return map.getPlacement();
	}

	public static void setPlacement(CustomPoint placement) {
		map.setPlacement(placement);
	}
}
