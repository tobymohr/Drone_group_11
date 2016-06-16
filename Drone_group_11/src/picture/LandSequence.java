package picture;

import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.HashMap;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;

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
	String checkCode = null;

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

	
	public enum VideoBitRateMode{
		DYNAMIC
	}
	
	public enum VideoCodec{
		H264_720P_SLRS
	}
	
	
	public void run() {

		System.out.println("HOVER");
		cC.dC.hover();
		while (code == null) {
			code = OFC.scanQrCode(camMat);
			sleep(10);
			if(code != null){
				System.out.println(code);
				break;
			}
				
		}
		System.out.println(code);

		sleep(5000);
		cC.dC.setSpeed(5);
		cC.addCommand(Command.UP, 3600, 30);
		sleep(3700);
		cC.dC.hover();

		while(true){
			circles = OFC.myCircle(camMat);
			if (circles > 0) {
				cC.addCommand(Command.DOWN, 2000, 15);
				sleep(2000);
				while (checkCode == null) {
					checkCode = OFC.scanQrCode(camMat);
					sleep(10);
				}
				cC.addCommand(Command.UP, 2500, 20);
				sleep(2100);
				if (code.equals(checkCode)){
					System.out.println("Found");
					break;
				}
				// TODO: Save coordinates
			}
		}
		
		while(true){
			circles = OFC.myCircle(camMat);
			if(circles != 0){
				cC.addCommand(Command.DOWN, 2000, 20);
				System.out.println(circles);
				break;
			}
		}
		
		
		
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
					sleep(10);
					counts++;
					// System.out.println(counts);
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
				if (counts >= 3) {
					System.out.println("landing");

					cC.addCommand(Command.LAND, 6000, 2);
					break;
				}
			}
		}
	}

	// }

	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
