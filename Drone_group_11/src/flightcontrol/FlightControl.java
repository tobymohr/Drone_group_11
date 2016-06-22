package flightcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import org.bytedeco.javacpp.opencv_core.Mat;
import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class FlightControl implements Runnable {
	private static final int CHUNK_SIZE_Y = 80;
	public static final int MAX_Y_CORD = 950;
	public static final int MIN_Y_CORD = 220;
	public static final int MAX_X_CORD = 847;
	public static final int MIN_X_CORD = 80;
	public static final int CHUNK_SIZE_X = MAX_X_CORD / 6;
	
	private MoveHelper moveHelper;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private CommandController commandController;
	private Mat camMat;
	private DownScanSeq downScan = new DownScanSeq(commandController);
	private HashMap<Integer, ArrayList<CustomPoint>> moves;
	
	
	public FlightControl(CommandController commandController) {
		this.commandController = commandController;
		this.moveHelper = new MoveHelper(commandController);
		moves = new HashMap<Integer, ArrayList<CustomPoint>>();
	}

	public void setImage(Mat img) {
		camMat = img;
		moveHelper.camMat = img.clone();
	}

	@Override
	public void run() {
		PictureController.setPlacement(new CustomPoint(0, 0));
		populateMoves();
		commandController.droneInterface.takeOff();

		sleepThread(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleepThread(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 400, 100);
		PictureController.setPlacement(new CustomPoint(847, 50));
		flyLaneOne();
		commandController.addCommand(Command.LEFT, MoveHelper.FIELD_DURATION , MoveHelper.FIELD_SPEED);
		flyLaneTwo();
		commandController.addCommand(Command.RIGHT, MoveHelper.FIELD_DURATION , MoveHelper.FIELD_SPEED);
		flyLaneThree();
		
	}
	
	private void flyLaneOne(){
		downScan.scanForCubes();
		boolean backwards = true;
		for (CustomPoint point : moves.get(1)) {
			System.out.println("MOVE TO: " + point.toString());
			moveHelper.moveDroneToPlacement(point, "W02.00");
			downScan.scanForCubes();
			backwards = moveHelper.moveOneChunk(backwards, point.getY(), "W02.02", "W00.02");
		}
		System.out.println("DONE");
	}
	
	private void flyLaneTwo(){
		downScan.scanForCubes();
		boolean backwards = true;
		for (CustomPoint point : moves.get(2)) {
			System.out.println("MOVE TO: " + point.toString());
			backwards = moveHelper.moveOneChunk(backwards, point.getY(), "WWW", "LOL");
			downScan.scanForCubes();
			
		}
		moveHelper.backwards = true;
		System.out.println("DONE");
	}
	
	private void flyLaneThree(){
		downScan.scanForCubes();
		for (CustomPoint point : moves.get(2)) {
			System.out.println("MOVE TO: " + point.toString());
			moveHelper.moveDroneToPlacement(point, "W02.00");
			downScan.scanForCubes();
			
		}
		moveHelper.backwards = true;
		System.out.println("DONE");
	}
	
	

	
	private void sleepThread(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			System.out.println("InterruptedEX");
		}
	}
	
	public void populateMoves() {
		ArrayList<CustomPoint> tempList = new ArrayList<CustomPoint>();
		
		for (int j = MIN_Y_CORD; j < MAX_Y_CORD; j = j + CHUNK_SIZE_Y) {
			tempList.add(new CustomPoint(MAX_X_CORD, j));			
		}
		moves.put(1, tempList);
		
		ArrayList<CustomPoint> tempList2 = new ArrayList<CustomPoint>();
		for (int j = MAX_Y_CORD; j > MIN_Y_CORD; j = j - CHUNK_SIZE_Y) {
			tempList2.add(new CustomPoint(MAX_X_CORD - CHUNK_SIZE_X, j));
		}
		moves.put(2, tempList2);
		
		ArrayList<CustomPoint> tempList3 = new ArrayList<CustomPoint>();
		for (int j = MIN_Y_CORD; j < MAX_Y_CORD; j = j + CHUNK_SIZE_Y) {
			tempList3.add(new CustomPoint(MAX_X_CORD - CHUNK_SIZE_X * 2, j));			
		}
		moves.put(3, tempList3);
		
		ArrayList<CustomPoint> tempList4 = new ArrayList<CustomPoint>();
		for (int j = MAX_Y_CORD; j > MIN_Y_CORD; j = j - CHUNK_SIZE_Y) {
			tempList.add(new CustomPoint(MAX_X_CORD - CHUNK_SIZE_X * 3, j));
		}
		moves.put(4, tempList4);

		ArrayList<CustomPoint> tempList5 = new ArrayList<CustomPoint>();
		for (int j = MIN_Y_CORD; j < MAX_Y_CORD; j = j + CHUNK_SIZE_Y) {
			tempList5.add(new CustomPoint(MAX_X_CORD - CHUNK_SIZE_X * 4, j));			
		}
		moves.put(5, tempList5);

		ArrayList<CustomPoint> tempList6 = new ArrayList<CustomPoint>();
		for (int j = MAX_Y_CORD; j > MIN_Y_CORD; j = j - CHUNK_SIZE_Y) {
			tempList6.add(new CustomPoint(MAX_X_CORD - CHUNK_SIZE_X * 5, j));
		}
		moves.put(6, tempList6);
		PictureController.addCords(tempList);
		PictureController.addCords(tempList2);
		PictureController.addCords(tempList3);
		PictureController.addCords(tempList4);
		PictureController.addCords(tempList5);
		PictureController.addCords(tempList6);
	}
}
