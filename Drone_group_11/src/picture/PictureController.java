package picture;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_core.*;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import app.DroneCommunicator;
import app.DroneInterface;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.command.LEDAnimation;
import de.yadrone.base.command.VideoBitRateMode;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.navdata.Altitude;
import de.yadrone.base.navdata.AltitudeListener;
import de.yadrone.base.video.ImageListener;
import de.yadrone.base.video.VideoManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class PictureController {

	private OpticalFlowCalculator OFC;
	private DroneInterface droneCommunicator;
	private IARDrone drone;
	private Video video;
	private OFVideo ofvideo;
	private ScheduledExecutorService timer;
	private int colorInt = 0;

	// DRONE
	@FXML
	private Button connectDrone;
	@FXML
	private Button takeOffDrone;
	@FXML
	private Button landDrone;
	@FXML
	private Button forwardDrone;
	@FXML
	private Button backwardsDrone;
	@FXML
	private Button emergencyDrone;
	@FXML
	private TextField speedDrone;

	@FXML
	private Button redButton;
	@FXML
	private Button greenButton;
	@FXML
	private Button yellowButton;

	// CAMERA
	@FXML
	private Button cameraButton;
	@FXML
	private ImageView polyFrame;
	@FXML
	private ImageView filterFrame;
	@FXML
	private Slider hueStart;
	@FXML
	private Slider hueStop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Slider saturationStop;
	@FXML
	private Slider valueStart;
	@FXML
	private Slider valueStop;
	@FXML
	private Label hsvCurrentValues;

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
		start();
	}

	public void start() {
		try {
			grabFromVideo();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// initDrone(); grabFromDrone();
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

		JFrame frame1 = new JFrame("Emergency");

		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton buttonEmergencyStop = new JButton("Emergency");
		buttonEmergencyStop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				droneCommunicator.emergencyStop();
			}
		});
		buttonEmergencyStop.setSize(200, 200);
		frame1.add(buttonEmergencyStop);

		JFrame frame2 = new JFrame("Land");
		frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton buttonFreezeLand = new JButton("Freeze and land");
		buttonFreezeLand.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				droneCommunicator.freeze();
				droneCommunicator.land();
			}
		});
		buttonFreezeLand.setSize(200, 200);
		frame2.add(buttonFreezeLand);

		frame1.setSize(400, 200);
		frame1.setVisible(true);
		frame2.setSize(400, 200);
		frame2.setVisible(true);
	}

	public void grabFromDrone() {

		drone.getVideoManager().start();
		// drone.getVideoManager().reinitialize();
		drone.getCommandManager().setVideoChannel(VideoChannel.VERT);

		drone.getVideoManager().addImageListener(new ImageListener() {
			boolean isFirst = true;
			QRCodeReader reader = new QRCodeReader();
			LuminanceSource source;
			BinaryBitmap bitmap;

			@Override
			public void imageUpdated(BufferedImage arg0) {
				if (isFirst) {
					new Thread(video = new Video(arg0)).start();
					new Thread(ofvideo = new OFVideo(arg0)).start();
					isFirst = false;
				}
				video.setArg0(arg0);
				ofvideo.setArg0(arg0);
				// source = new BufferedImageLuminanceSource(arg0);
				// bitmap = new BinaryBitmap(new HybridBinarizer(source));
				// try {
				// Result detectionResult = reader.decode(bitmap);
				// String code = detectionResult.getText();
				// System.out.println("--------------------------");
				// System.out.println(code);
				// System.out.println("--------------------------");
				// } catch (NotFoundException e)
				// {
				// } catch (ChecksumException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// } catch (FormatException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
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
			
			if (OFC == null) {
				OFC = new OpticalFlowCalculator(newImg);
				new Thread(OFC).start();
			}
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			// TODO Auto-generated catch block
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

}
