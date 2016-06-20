package it.polito.teaching.cv;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.VideoChannel;
import de.yadrone.base.video.ImageListener;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller associated with the only view of our application. The
 * application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative
 * controls and the image segmentation process.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.5 (2015-11-26)
 * @since 1.0 (2015-01-13)
 * 
 */
public class ObjRecognitionController
{
	// FXML camera button
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// the FXML area for showing the mask
	@FXML
	private ImageView maskImage;
	// the FXML area for showing the output of the morphological operations
	@FXML
	private ImageView morphImage;
	// FXML slider for setting HSV ranges
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
	// FXML label to show the current values set with the sliders
	@FXML
	private Label hsvCurrentValues;

	// a timer for acquiring the video stream
	// the OpenCV object that performs the video capture
	private IARDrone drone = new ARDrone();;
	// a flag to change the button behavior
	private boolean cameraActive;
	private BufferedImage cam = null;

	// property for object binding
	private ObjectProperty<String> hsvValuesProp;
	private ScheduledExecutorService timer;

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	private void startCamera()
	{
		drone.start();
		drone.getVideoManager().start();
		drone.getCommandManager().setVideoChannel(VideoChannel.VERT);
		
		drone.getVideoManager().addImageListener(new ImageListener() {

			@Override
			public void imageUpdated(BufferedImage arg0) {
				cam = arg0;

			}
		});
		Runnable frameGrabber = new Runnable() {

			@Override
			public void run()
			{
				if(cam != null){
					Image imageToShow = grabFrame();
					originalFrame.setImage(imageToShow);
				}
			}
		};

		this.timer = Executors.newSingleThreadScheduledExecutor();
		this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
		// bind a text property with the string containing the current range of
		// HSV values for object detection
		hsvValuesProp = new SimpleObjectProperty<>();
		this.hsvCurrentValues.textProperty().bind(hsvValuesProp);

		// set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.originalFrame, 400);
		this.imageViewProperties(this.maskImage, 200);
		this.imageViewProperties(this.morphImage, 200);

		if (!this.cameraActive)
		{
			this.cameraActive = true;



			// update the button content
			this.cameraButton.setText("Stop Camera");


		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");




		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat(cam.getHeight(), cam.getWidth(), CvType.CV_8UC3);

		// check if the capture is open
		try
		{
			// read the current frame
			byte[] data = ((DataBufferByte) cam.getRaster().getDataBuffer()).getData();
			frame.put(0, 0, data);
			// if the frame is not empty, process it
			if (!frame.empty())
			{
				// init
				Mat blurredImage = new Mat();
				Mat hsvImage = new Mat();
				Mat mask = new Mat();
				Mat morphOutput = new Mat();
				Mat lowerHue = new Mat();
				Mat upperHue = new Mat();

				// remove some noise
				Imgproc.blur(frame, blurredImage, new Size(7, 7));

				// convert the frame to HSV
				Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

				// get thresholding values from the UI
				// remember: H ranges 0-180, S and V range 0-255

				Scalar minValues = new Scalar(this.hueStart.getValue(), this.saturationStart.getValue(),
						this.valueStart.getValue());
				Scalar maxValues = new Scalar(this.hueStop.getValue(), this.saturationStop.getValue(),
						this.valueStop.getValue());

				// show the current selected HSV range
				String valuesToPrint = "Hue range: " + minValues.val[0] + "-" + maxValues.val[0]
						+ "\tSaturation range: " + minValues.val[1] + "-" + maxValues.val[1] + "\tValue range: "
						+ minValues.val[2] + "-" + maxValues.val[2];
				this.onFXThread(this.hsvValuesProp, valuesToPrint);
				
				if(this.hueStart.getValue()>this.hueStop.getValue()){
				
					Scalar scalar1 = new Scalar(0, this.saturationStart.getValue(), this.valueStart.getValue());
					Scalar scalar2 = new Scalar(this.hueStop.getValue(), this.saturationStop.getValue(), this.valueStop.getValue());
					Scalar scalar3 = new Scalar(this.hueStart.getValue(), this.saturationStart.getValue(), this.valueStart.getValue());
					Scalar scalar4 = new Scalar(180, this.saturationStop.getValue(), this.valueStop.getValue());
					Core.inRange(hsvImage, scalar1, scalar2, lowerHue);
					Core.inRange(hsvImage, scalar3, scalar4, upperHue);
					Core.addWeighted(lowerHue, 1.0, upperHue, 1.0, 0.0, mask);
				}else{
					// threshold HSV image to select tennis balls
					Core.inRange(hsvImage, minValues, maxValues, mask);
				}
				
				// show the partial output
				this.onFXThread(this.maskImage.imageProperty(), this.mat2Image(mask));

				// morphological operators
				// dilate with large element, erode with small ones
				Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
				Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

				Imgproc.erode(mask, morphOutput, erodeElement);
				Imgproc.erode(mask, morphOutput, erodeElement);

				Imgproc.dilate(mask, morphOutput, dilateElement);
				Imgproc.dilate(mask, morphOutput, dilateElement);

				// show the partial output
				this.onFXThread(this.morphImage.imageProperty(), this.mat2Image(morphOutput));

				// find the tennis ball(s) contours and show them
				frame = this.findAndDrawBalls(morphOutput, frame);

				// convert the Mat object (OpenCV) to Image (JavaFX)
				imageToShow = mat2Image(frame);
			}

		}
		catch (Exception e)
		{
			// log the (full) error
			System.err.print("ERROR");
			e.printStackTrace();
		}


		return imageToShow;
	}

	/**
	 * Given a binary image containing one or more closed surfaces, use it as a
	 * mask to find and highlight the objects contours
	 * 
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original {@link Mat} image to be used for drawing the
	 *            objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	private Mat findAndDrawBalls(Mat maskedImage, Mat frame)
	{
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
		{
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			{
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}

		return frame;
	}

	/**
	 * Set typical {@link ImageView} properties: a fixed width and the
	 * information to preserve the original image ration
	 * 
	 * @param image
	 *            the {@link ImageView} to use
	 * @param dimension
	 *            the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension)
	{
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}

	/**
	 * Convert a {@link Mat} object (OpenCV) in the corresponding {@link Image}
	 * for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame)
	{
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer, according to the PNG format
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	private <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(new Runnable() {

			@Override
			public void run()
			{
				property.set(value);
			}
		});
	}

}
