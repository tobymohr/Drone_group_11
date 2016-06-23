package flightcontrol;

import java.util.HashMap;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;

import de.yadrone.base.command.FlyingMode;
import app.CommandController;
import helper.Command;
import picture.PictureProcessingHelper;

public class LandSequence implements Runnable {

	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	public boolean wallClose = false;
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
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	public void run() {

		// TAKEOFF sequence
		commandController.droneInterface.hover();
		while (code == null) {
			code = pictureProcessingHelper.scanQrCode(camMat);
			sleep(10);
		}
		sleep(20000);
		sleep(1900);
		commandController.addCommand(Command.UP, 2600, 15);
		sleep(2600);
		commandController.droneInterface.hover();
		// TAKEOFF sequence END

		// check during flight sequence
		while (true) {
			circles = pictureProcessingHelper.findCircles(camMat);
			if (circles > 0) {
				commandController.addCommand(Command.DOWN, 1000, 20);
				sleep(2000);
				while (checkCode == null) {
					checkCode = pictureProcessingHelper.scanQrCode(camMat);
					sleep(10);
				}
				commandController.addCommand(Command.UP, 1000, 17);
				sleep(2100);
				if (code.equals(checkCode)) {
					break;
				}
				break;
			}
		}
		// check during flight sequence END
		commandController.droneInterface.land();

		// LANDING sequence
		while (true) {

			circles = pictureProcessingHelper.findCircles(camMat);
			if (circles > 0) {
				commandController.addCommand(Command.DOWN, 1000, 20);
				sleep(1000);
			}

			boolean check = pictureProcessingHelper.checkDecodedQR(camMat);

			if (check || circles > 0) {
				commandController.droneInterface.land();
			}
		}
		// LANDING sequence END
	}

	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}