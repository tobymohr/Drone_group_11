package flightcontrol;

import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.Mat;
import helper.CustomPoint;
import app.CommandController;
import helper.Command;
import picture.PictureController;

public class FlightControl implements Runnable {
	private static final int CHUNK_SIZE_Y = 80;
	public static final int MAX_Y_CORD = 950;
	public static final int MIN_Y_CORD = 220;
	public static final int MAX_X_CORD = 847;
	public static final int MIN_X_CORD = 80;
	public static final int CHUNK_SIZE_X = MAX_X_CORD / 6;

	private MoveHelper moveHelper;
	private CommandController commandController;
	private DownScanSeq downScan = new DownScanSeq(commandController);
	private HashMap<Integer, ArrayList<CustomPoint>> moves;

	public FlightControl(CommandController commandController, DownScanSeq down) {
		this.downScan = down;
		this.commandController = commandController;
		this.moveHelper = new MoveHelper(commandController);
		moves = new HashMap<Integer, ArrayList<CustomPoint>>();
	}

	public void setImage(Mat img) {
		moveHelper.camMat = img.clone();
	}

	@Override
	public void run() {
		populateMoves();
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleepThread(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 400, 100);
		flyLaneOne();
		 commandController.addCommand(Command.LEFT, MoveHelper.FIELD_DURATION
		 , MoveHelper.FIELD_SPEED);
		 flyLaneTwo();
		 commandController.addCommand(Command.RIGHT, MoveHelper.FIELD_DURATION
		 , MoveHelper.FIELD_SPEED);
		 flyLaneThree();

	}

	private void flyLaneOne() {
		// downScan.scanForCubes();
		// TODO LANDING FIS
		boolean backwards = true;
		for (CustomPoint point : moves.get(1)) {
			System.out.println("MOVE TO: " + point.toString());
			backwards = moveHelper.moveOneChunk(backwards, point.getY(), "W02.00", "W00.04");
			commandController.addCommand(Command.HOVER, 5000, 5);
			downScan.run();

			// TODO LANDING FIS
		}
		System.out.println("DONE");
	}

	private void flyLaneTwo() {
		boolean backwards = true;
		for (CustomPoint point : moves.get(2)) {
			System.out.println("MOVE TO: " + point.toString());
			backwards = moveHelper.moveOneChunk(backwards, point.getY(), "W00.03", "W02.01");
			commandController.addCommand(Command.HOVER, 5000, 5);
			downScan.run();
		}
		moveHelper.backwards = true;
		System.out.println("DONE");
	}

	private void flyLaneThree() {
		boolean backwards = true;
		for (CustomPoint point : moves.get(2)) {
			System.out.println("MOVE TO: " + point.toString());
			backwards = moveHelper.moveOneChunk(backwards,point.getY(), "W02.02", "W00.02");
			commandController.addCommand(Command.HOVER, 5000, 5);
			downScan.run();
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
		for(CustomPoint p : tempList){
			System.out.println(p.getX());
		}
		PictureController.addCords(tempList);
		PictureController.addCords(tempList2);
		PictureController.addCords(tempList3);
		PictureController.addCords(tempList4);
		PictureController.addCords(tempList5);
		PictureController.addCords(tempList6);
	}
}
