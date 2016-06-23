package flightcontrol;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;

import dronecontrol.CommandController;
import helper.Command;
import helper.CustomPoint;
import picture.OFVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class MoveHelper {
	public static final int MIN_HIT_COUNT = 6;
	public static final int STRAFE_TIME = 1000;
	public static final int SPIN_TIME = 50;
	public static final int SPIN_SPEED = 100;
	public static final int FIELD_DURATION = 1000;
	public static final int FIELD_SPEED = 16;
	public static final int MAX_FRAME_COUNT = 8;
	private double contourSize = 0;
	private double distanceFromQr = 0;

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
	private boolean xDone = false;
	private boolean yDone = false;
	boolean stopUsingSpin = xDone == true || yDone == true;
	private boolean done = false;
	public boolean backwards = true;

	public MoveHelper(CommandController commandController) {
		this.commandController = commandController;
	}

	public void moveDroneToPlacement(CustomPoint placement, String code) {
		this.code = code;
		endPlacement = placement;
		chunkSize = 65;

		while (!done) {
			if (OFVideo.imageChanged) {
				OFVideo.imageChanged = false;

				List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
				if (frameCount >= MAX_FRAME_COUNT) {
					frameCount = 0;
					if (contours.size() > 0) {
						if (!Double.isInfinite(distanceFromQr)) {

							CustomPoint tempPlace = PictureController.getPlacement();
							if (code.contains("W02")) {
								tempPlace.setY(FlightControl.MIN_Y_CORD + distanceFromQr);
								PictureController.setPlacement(tempPlace);
							}
							if (code.contains("W00")) {
								tempPlace.setY(FlightControl.MAX_Y_CORD - distanceFromQr);
								PictureController.setPlacement(tempPlace);
							}
							if (distanceFromQr > 490 && backwards) {
								commandController.addCommand(Command.ROTATELEFT, 2000, 90);
								backwards = !backwards;
								return;
							}
						}

					}
					int move = calcMovesYAxis(PictureController.getPlacement().getY(), placement);
					decideMove(move);

				} else {
					frameCount++;
				}
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		done = false;

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

	private RotatedRect mostCenteredRect(List<Mat> contours, Mat srcImage) {
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			if (contourArea(contours.get(i)) > 1000) {
				RotatedRect rect2 = minAreaRect(contours.get(i));
				int angle = Math.abs((int) rect.angle());
				float height;
				float width;
				if (angle >= 0 && angle < 10) {
					height = (int) rect2.size().height();
					width = (int) rect2.size().width();
				} else {
					height = rect2.size().width();
					width = rect2.size().height();
				}
				double distance = (srcImage.arrayWidth() / 2) - rect2.center().x();
				double ratio = height / width;
				if (distanceFomCenter > distance && ratio > 1.15) {
					distanceFomCenter = Math.abs(distance);
					rect = rect2;
				}
			}
		}
		return rect;
	}

	public boolean canMoveWithoutSpinCheck(RotatedRect rect) {
		double positionFromCenter = pictureProcessingHelper.isCenterInImageBigger(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			if (positionFromCenter > 0) {
				if (pictureProcessingHelper.getSpinSpeed(contourSize) > 0) {
					addCommand(Command.RIGHT, 250, 85);
				} else {
					addCommand(Command.RIGHT, 250, 85);
				}
			} else {
				if (pictureProcessingHelper.getSpinSpeed(contourSize) > 0) {
					addCommand(Command.LEFT, 250, 85);
				} else {
					addCommand(Command.LEFT, 250, 85);
				}

			}
			return false;
		}
		return true;
	}

	private void decideMove(int move) {
		if (move == Command.LEFT || move == Command.RIGHT) {
			addCommandForPlacement(move, STRAFE_TIME, SPIN_SPEED);
		} else {
			addCommandForPlacement(move, FIELD_DURATION, FIELD_SPEED);
		}
	}

	private int calcMovesYAxis(double y, CustomPoint placement) {
		return getCorrectYMove();
		// if (y > placement.getY()) {
		// if (backwards) {
		// return Command.FORWARD;
		// } else {
		// return Command.BACKWARDS;
		// }
		// }
	}

	private void addCommandForPlacement(int task, int duration, int speed) {
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

	private int getCorrectYMove() {
		if (backwards) {
			return Command.BACKWARDS;
		}
		if (!backwards) {
			return Command.FORWARD;
		}
		return Command.NONE;
	}

	public boolean moveOneChunk(boolean backwards, double goalY, String backwardsCode, String forwardsCode) {
		boolean done = false;
		double difference = 0;
		while (!done) {
			if (OFVideo.imageChanged) {
				OFVideo.imageChanged = false;
				if (frameCount >= MAX_FRAME_COUNT) {
					List<Mat> contours = pictureProcessingHelper.findQrContours(camMat);
					if (contours.size() > 0) {
						RotatedRect rect = null;
						if (backwards) {
							rect = checkForQRScan(camMat, contours, backwardsCode);
						} else {
							rect = checkForQRScan(camMat, contours, forwardsCode);
						}
						if (rect == null) {
							rect = mostCenteredRect(contours, camMat);
						}
						while (!canMoveWithoutSpinCheck(rect)) {
							contours = pictureProcessingHelper.findQrContours(camMat);
							rect = mostCenteredRect(contours, camMat);
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						distanceFromQr = pictureProcessingHelper.calcDistance(rect);
						if (!Double.isInfinite(distanceFromQr)) {
							CustomPoint tempPlace = PictureController.getPlacement();
							if (distanceFromQr > 490 && backwards) {
								commandController.addCommand(Command.ROTATELEFT, 2200, 90);
								// commandController.addCommand(Command.ROTATERIGHT,
								// 200, 90);
								backwards = false;
							} else {
								if (backwards) {
									difference = goalY - distanceFromQr;

									if (difference > 15) {
										addCommand(Command.BACKWARDS, calculateDuration(difference), FIELD_SPEED);
									} else if (difference < -15) {
										addCommand(Command.FORWARD, calculateDuration(difference), FIELD_SPEED + 6);
									} else {
										done = true;
									}
									tempPlace.setY(distanceFromQr - 10);
								} else {
									if (difference > 15) {
										addCommand(Command.FORWARD, calculateDuration(difference), FIELD_SPEED);
									} else if (difference < -15) {
										addCommand(Command.BACKWARDS, calculateDuration(difference), FIELD_SPEED + 6);
									} else {
										done = true;
									}
									tempPlace.setY(FlightControl.MAX_Y_CORD - distanceFromQr);
								}
								PictureController.setPlacement(tempPlace);
							}
						} else {
							if (backwards) {
								addCommand(Command.BACKWARDS, FIELD_DURATION, FIELD_SPEED);
							} else {
								addCommand(Command.FORWARD, FIELD_DURATION, FIELD_SPEED);
							}
						}
					} else {
						if (backwards) {
							addCommand(Command.BACKWARDS, FIELD_DURATION, FIELD_SPEED);
						} else {
							addCommand(Command.FORWARD, FIELD_DURATION, FIELD_SPEED);
						}
					}
				} else {
					frameCount++;
				}
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return backwards;
	}

	private RotatedRect checkForQRScan(Mat image, List<Mat> contours, String code) {
		for (Mat contour : contours) {
			RotatedRect rect = minAreaRect(contour);
			Mat warpImg = pictureProcessingHelper.warpImage(image, rect);
			String result = pictureProcessingHelper.scanQrCode(warpImg);
			if (result.equals(code)) {
				return rect;
			}
		}
		return null;
	}

	private int calculateDuration(double difference) {
		double absDifference = Math.abs(difference);
		if (absDifference > 70) {
			return FIELD_DURATION;
		} else if (absDifference > 50) {
			return (int) (FIELD_DURATION * 0.75);
		} else if (absDifference > 30) {
			return (int) (FIELD_DURATION * 0.5);
		} else {
			return (int) (FIELD_DURATION * 0.25);
		}
	}
}
