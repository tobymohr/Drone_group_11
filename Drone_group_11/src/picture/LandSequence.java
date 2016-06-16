package picture;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import helper.Command;
import javacvdemo.AvoidWallDemo;

public class LandSequence implements Runnable {

	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private CommandController cC;
	public boolean wallClose = false;
	private AvoidWallDemo CK;
	private Map<Integer, Integer> moveSet = new HashMap<>();
	private static boolean aboveLanding = false;
	private static int circleCounter = 0;
	private static int counts = 0;
	private Mat camMat;
	int circles = 0;
	String code = null;
	
	
	public LandSequence(CommandController commandController) {
		moveSet.put(Command.LEFT, 0);
		moveSet.put(Command.RIGHT, 0);
		moveSet.put(Command.SPINLEFT, 0);
		moveSet.put(Command.SPINRIGHT, 0);
		this.cC = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	public void run() {
		
		System.out.println("HOVER");
		cC.dC.hover();
		while(true){
			code = OFC.scanQrCode(camMat);
			
			sleep(10);
			if(code != null){
				System.out.println(code);
				break;
			}
				
		}
		
		sleep(5000);
		cC.dC.setSpeed(5);
		cC.addCommand(Command.UP, 2600, 20);
		sleep(2700);
		
		while (true) {
			
			boolean check = OFC.checkDecodedQR(camMat);

			if (check) {

					circles = OFC.myCircle(camMat);

					if (circles > 0) {
						aboveLanding = true;
						// If false restart landing sequence
						// Land
						System.out.println("going down");
						// Thread.sleep(10);
						sleep(500);
						counts++;
						System.out.println(counts);
						circleCounter = 0;
					} else {
						circles = 0;
						circleCounter++;
						System.out.println(circleCounter);

					}
					if (circleCounter >= 120) {
						aboveLanding = false;
						circleCounter = 0;
						counts = 0;
					}
					if (counts == 3) {
						System.out.println("landing");

						cC.addCommand(Command.LAND, 6000, 2);
						break;
					}
					// }
				}
			}
		}
	//	}

	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
