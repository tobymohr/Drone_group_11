package picture;

import java.awt.image.BufferedImage;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class Video implements Runnable
{

	Java2DFrameConverter converter1;
	CanvasFrame canvas;
	BufferedImage arg0;
	
	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	public Video(BufferedImage arg0){
		this.arg0 = arg0;
		converter1 = new Java2DFrameConverter();
		canvas = new CanvasFrame("Video");
	}

	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		canvas.showImage(converter1.convert(arg0));
		}
		
	}
}