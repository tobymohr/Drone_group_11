package picture;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OFVideo implements Runnable {

	Java2DFrameConverter converter1;
	OpenCVFrameConverter.ToIplImage converter;
	private ImageView filterFrame;
	private ImageView polyFrame;
	
	BufferedImage arg0;


	OpticalFlowCalculator OFC = new OpticalFlowCalculator();

	public OFVideo(ImageView filterFrame, ImageView polyFrame, BufferedImage arg0) {
		this.arg0 = arg0;
		this.filterFrame = filterFrame;
		this.polyFrame = polyFrame;
		converter = new OpenCVFrameConverter.ToIplImage();
		converter1 = new Java2DFrameConverter();		
		
	}

	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	@Override
	public void run() {
		try {
			IplImage newImg = null;
			while (true) {
					newImg = converter.convert(converter1.convert(arg0));
					IplImage filteredImage = null;
					
					switch(PictureController.colorInt){
					case 1:
						filteredImage = OFC.findContoursBlack(newImg);
						break;
					case 2: 
						filteredImage = OFC.findContoursRed(newImg);
						break;
					case 3: 
						filteredImage = OFC.findContoursGreen(newImg);
						break;
					default: 
						filteredImage = OFC.findContoursBlack(newImg);
						break;
					}
					
					IplImage polyImage = OFC.findPolygons(newImg,filteredImage,4);
					BufferedImage bufferedImage = IplImageToBufferedImage(polyImage);
					BufferedImage bufferedImageFilter = IplImageToBufferedImage(filteredImage);
					Image imageFilter = SwingFXUtils.toFXImage(bufferedImageFilter, null);
					Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
					polyFrame.setImage(imagePoly);
					filterFrame.setImage(imageFilter);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	public BufferedImage IplImageToBufferedImage(IplImage src) {
		OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}

}