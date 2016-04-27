package picture;

import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OFVideo implements Runnable {

	Java2DFrameConverter converter1;
	OpenCVFrameConverter.ToIplImage converter;
	OpenCVFrameConverter.ToMat converterMat;
	private ImageView filterFrame;
	private ImageView polyFrame;
	private ImageView qrFrame;
	
	BufferedImage arg0;


	PictureProcessingHelper OFC = new PictureProcessingHelper();

	public OFVideo(ImageView filterFrame, ImageView polyFrame, ImageView qrFrame, BufferedImage arg0) {
		this.arg0 = arg0;
		this.filterFrame = filterFrame;
		this.polyFrame = polyFrame;
		this.qrFrame = qrFrame;
		converter = new OpenCVFrameConverter.ToIplImage();
		converterMat = new ToMat();
		converter1 = new Java2DFrameConverter();		
		
	}

	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	@Override
	public void run() {
		try {
			Mat newImg = null;
			while (true) {
					newImg = converterMat.convert(converter1.convert(arg0));
					Mat filteredImage = null;
					
					switch(PictureController.colorInt){
					case 1:
				//		filteredImage = OFC.findContoursBlack(newImg);
						break;
					case 2: 
						filteredImage = OFC.findContoursRedMat(newImg);
						break;
					case 3: 
						filteredImage = OFC.findContoursGreenMat(newImg);
						break;
					default: 
					//	filteredImage = OFC.findContoursBlack(newImg);
						break;
					}
					
					Mat polyImage = OFC.findPolygonsMat(newImg,filteredImage, 4);
				//	IplImage qrImage = OFC.extractQRImage(newImg);

					BufferedImage bufferedImage = MatToBufferedImage(polyImage);
					BufferedImage bufferedImageFilter = MatToBufferedImage(filteredImage);
					//BufferedImage bufferedImageQr = MatToBufferedImage(qrImage);

					Image imageFilter = SwingFXUtils.toFXImage(bufferedImageFilter, null);
					Image imagePoly = SwingFXUtils.toFXImage(bufferedImage, null);
				//	Image imageQr = SwingFXUtils.toFXImage(bufferedImageQr, null);

					polyFrame.setImage(imagePoly);
					filterFrame.setImage(imageFilter);
					//qrFrame.setImage(imageQr);
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
	public BufferedImage MatToBufferedImage(Mat src) {
		OpenCVFrameConverter.ToMat grabberConverter = new OpenCVFrameConverter.ToMat();
		Java2DFrameConverter paintConverter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return paintConverter.getBufferedImage(frame, 1);
	}
}