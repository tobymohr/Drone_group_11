package picture;

import java.awt.image.BufferedImage;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Video implements Runnable
{
	ImageView polyFrame;
	Image arg0;
	
	public void setArg0(BufferedImage arg0) {
		this.arg0 = SwingFXUtils.toFXImage(arg0, null);
;
	}

	public Video(ImageView polyFrame, BufferedImage arg0){
		this.polyFrame = polyFrame;
		this.arg0 = SwingFXUtils.toFXImage(arg0, null);
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
			polyFrame.setImage(arg0);
		}
		
	}
}