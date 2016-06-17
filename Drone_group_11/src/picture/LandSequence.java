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

	public void run() {

		System.out.println("HOVER");
		cC.dC.hover();
		while (code == null) {
			code = OFC.scanQrCode(camMat);
			sleep(10);
		}
		System.out.println(code);

		sleep(1900);
		cC.dC.setSpeed(5);
		cC.addCommand(Command.UP, 2600, 15);
		sleep(2600);
		cC.dC.hover();

		while (true) {
			circles = OFC.myCircle(camMat);
			if (circles > 0) {
				cC.addCommand(Command.DOWN, 1000, 20);
				sleep(2000);
//				while (checkCode == null) {
//					checkCode = OFC.scanQrCode(camMat);
//					sleep(10);
//				}
				cC.addCommand(Command.UP, 1000, 20);
				sleep(2100);
				if (code.equals(checkCode)) {
					System.out.println("Found");
					// TODO: Save coordinates
					break;
				}
				
			}
		}

		while (true) {
		
		circles = OFC.myCircle(camMat);
		if (circles > 0) {
			cC.addCommand(Command.DOWN, 1000, 20);
			sleep(2000);
		}
		
		boolean check = OFC.checkDecodedQR(camMat);
		
		if(check || circles > 0){
			cC.dC.land();
		}
		
		
		}
//
//			boolean check = OFC.checkDecodedQR(camMat);
//
//			if (check) {
//				System.out.println("checked");
//				circles = OFC.myCircle(camMat);
//				cC.addCommand(Command.DOWN, 1000, 20);
//				sleep(1200);
//				if (circles > 0) {
//					while (true) {
//						System.out.println("found circle");
//
//						
//						if (circles > 0 || check) {
//							cC.dC.land();
//							break;
//						}
//
//					}
//				}
//				// if (circles > 0) {
//				//
//				// aboveLanding = true;
//				// System.out.println("going down");
//				//
//				// sleep(10);
//				// counts++;
//				// circleCounter = 0;
//				// } else {
//				// circles = 0;
//				// circleCounter++;
//				// System.out.println(circleCounter);
//				//
//				// }
//				// if (circleCounter >= 120) {
//				// aboveLanding = false;
//				// circleCounter = 0;
//				// counts = 0;
//				// }
//				// if (counts >= 3) {
//				// System.out.println("landing");
//				//
//				// cC.addCommand(Command.LAND, 6000, 2);
//				// break;
//				// }
//			}
//		}
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