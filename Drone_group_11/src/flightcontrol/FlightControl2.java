package flightcontrol;

import org.bytedeco.javacpp.opencv_core.Mat;

import app.CommandController;
import helper.Command;
import picture.DownScanSeq;
import picture.PictureProcessingHelper;

public class FlightControl2 implements Runnable{

	
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private Mat camMat;
	private DownScanSeq downScan = new DownScanSeq(commandController);
	
	public FlightControl2(CommandController cc) {
		this.commandController = cc;
	}

	public void setImage(Mat img) {
		camMat = img;
	}

	@Override
	public void run() {
		commandController.droneInterface.takeOff();

		sleepThread(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleepThread(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 2750, 30);
		sleepThread(3750);
		
		flyLaneOne();
	}
	
	private void flyLaneOne(){
		//
	}

	
	private void sleepThread(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			System.out.println("InterruptedEX");
		}
	}
}
