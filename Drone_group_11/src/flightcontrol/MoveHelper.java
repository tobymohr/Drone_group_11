package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;
import picture.OFVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class MoveHelper {
	private static final int MIN_HIT_COUNT = 6;
	private static final int STRAFE_TIME = 1000;
	private static final int SPIN_TIME = 1000;
	private static final int SPIN_SPEED = 10;
	private static final int FIELD_DURATION = 1500;
	private static final int FIELD_SPEED = 7;
	private static final int MAX_FRAME_COUNT = 8;
	private double contourSize = 0;

	// #TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
	public static final double SMALL_MOVE_LIMIT = 100;

	private CommandController commandController;
	private double chunkSize = 0;
	private double smallChunkSize = 30;
	private String code = null;
	private int frameCount = 0;

	public Mat camMat;
	private CustomPoint endPlacement;
	private PictureProcessingHelper pictureProcessingHelper = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	private boolean moveX = false;
	private boolean xDone = false;
	private boolean yDone = false;
	boolean stopUsingSpin = xDone == true || yDone == true;
	private boolean done = false;

	public MoveHelper(CommandController commandController) {
		this.commandController = commandController;
	}

	public void moveDroneToPlacement(CustomPoint placement, String code) {
		this.code = code;
		while (!done) {
			endPlacement = placement;
			int move = calcMovesYAxis(PictureController.getPlacement().getY(), placement);
			decideMove(move);

		}
	}

	private RotatedRect mostCenteredRect(List<Mat> contours) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			RotatedRect rect2 = minAreaRect(contours.get(i));
			double distance = (camMat.arrayWidth() / 2) - rect2.center().x();
			if (distanceFomCenter > distance) {
				distanceFomCenter = Math.abs(distance);
				rect = rect2;
				contourSize = contourArea(contours.get(i));
			}
		}
		return rect;
	}

	public boolean canMoveWithoutSpinCheck(RotatedRect rect) {
////		double positionFromCenter = pictureProcessingHelper.isCenterInImageBigger(camMat.clone(), rect);
//		if (positionFromCenter != 0) {
//			if (positionFromCenter > 0) {
//				if (pictureProcessingHelper.getSpinSpeed(contourSize) > 0) {
//					addCommand(Command.SPINRIGHT, SPIN_TIME, pictureProcessingHelper.getSpinSpeed(contourSize));
//				} else {
//					addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
//				}
//			} else {
//				if (pictureProcessingHelper.getSpinSpeed(contourSize) > 0) {
//					addCommand(Command.SPINLEFT, SPIN_TIME, pictureProcessingHelper.getSpinSpeed(contourSize));
//				} else {
//					addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
//				}
//
//			}
//			return false;
//		}
		return true;
	}

	private void decideMove(int move) {
		if (move == Command.LEFT || move == Command.RIGHT) {
			addCommandForPlacement(move, STRAFE_TIME, SPIN_SPEED);
		} else {
			addCommandForPlacement(move, FIELD_DURATION, FIELD_SPEED);
		}
	}

	private int calcMoveXAxis(double x, CustomPoint placement) {
		if (x < placement.getX()) {
			return getCorrectXMove();
		}
		return Command.NONE;
	}

	private int calcMovesYAxis(double y, CustomPoint placement) {
		return getCorrectYMove(y);
	}

	private void addCommand(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
		}
	}

	private void addCommandForPlacement(int task, int duration, int speed) {
		if (commandController.isDroneReady()) {
			if (moves.containsKey(task)) {
				if ((task == Command.SPINLEFT || task == Command.SPINRIGHT) && moves.get(task) > MIN_HIT_COUNT / 2) {
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else if (moves.get(task) > MIN_HIT_COUNT) {
					updateRelativeCord(task);
					commandController.addCommand(task, duration, speed);
					moves.clear();
				} else {
					moves.put(task, moves.get(task) + 1);
				}
			} else {
				moves.put(task, 1);
			}
		}
	}

	private int getCorrectXMove() {
		if (code.contains("W02")) {
			return Command.LEFT;
		}
		if (code.contains("W03")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W00")) {
			return Command.RIGHT;
		}
		if (code.contains("W01")) {
			return Command.FORWARD;
		}
		return Command.NONE;
	}

	private int getCorrectYMove(double y) {
		if (code.contains("W02")) {
			return Command.BACKWARDS;
		}
		if (code.contains("W03")) {
			return Command.LEFT;
		}
		if (code.contains("W00")) {
			return Command.FORWARD;
		}
		if (code.contains("W01")) {
			return Command.RIGHT;
		}

		return Command.NONE;
	}

	private void updateRelativeCord(int move) {
		CustomPoint placement = PictureController.getPlacement();
		if (move == Command.BACKWARDS) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() - chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() - chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() + chunkSize);
			} else {
				placement.setX(placement.getX() + chunkSize);
			}
		} else if (move == Command.FORWARD) {
			if (code.contains("W00")) {
				placement.setY(placement.getY() + chunkSize);
			} else if (code.contains("W01")) {
				placement.setX(placement.getX() + chunkSize);
			} else if (code.contains("W02")) {
				placement.setY(placement.getY() - chunkSize);
			} else {
				placement.setX(placement.getX() - chunkSize);
			}
		} else if (move == Command.RIGHT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() + smallChunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() - smallChunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() - smallChunkSize);
			} else {
				placement.setY(placement.getY() + smallChunkSize);
			}
		} else if (move == Command.LEFT) {
			if (code.contains("W00")) {
				placement.setX(placement.getX() - smallChunkSize);
			} else if (code.contains("W01")) {
				placement.setY(placement.getY() + smallChunkSize);
			} else if (code.contains("W02")) {
				placement.setX(placement.getX() + smallChunkSize);
			} else {
				placement.setY(placement.getY() - smallChunkSize);
			}
		}
//		double differenceY = Math.abs((placement.getY() - endPlacement.getY()));
//		boolean endYCondition = differenceY > 0 && differenceY < 60;
//
//		double differenceX = Math.abs((placement.getX() - endPlacement.getX()));
//		boolean endXCondition = differenceX > 0 && differenceX < 60;
//
//		if (endYCondition) {
			done = true;
//		}
//
//		yDone = endYCondition;
//		xDone = endXCondition;
//
//		if (yDone) {
//			System.out.println("Y COORD DONE");
//			moveX = true;
//		}
//
//		if (xDone) {
//			System.out.println("X COORD DONE");
//			moveX = false;
//		}
//
//		System.out.println(placement.toString());
//		PictureController.setPlacement(placement);
	}
}
