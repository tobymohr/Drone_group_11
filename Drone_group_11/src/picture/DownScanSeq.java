package picture;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import org.bytedeco.javacpp.opencv_core.Mat;

import app.CommandController;
import helper.CustomPoint;

public class DownScanSeq implements Runnable {

	private static final double MAX_RES_X = 640.0;
	private static final double MAX_RES_Y = 360.0;
	private static final double CENTER_OF_DRONE_Y = 42.5;
	private static final double CENTER_OF_DRONE_X = 75.0;
	private static final double PIXELS_PER_CM_Y = MAX_RES_X / 150.0;
	private static final double PIXELS_PER_CM_X = MAX_RES_Y / 85.0;
	public boolean greenDone;
	public boolean redDone;
	private PictureProcessingHelper pictureProcessingHelper;
	private Mat camMat;
	private Mat camMat2;
	private ArrayList<ArrayList<CustomPoint>> greenResults = new ArrayList<ArrayList<CustomPoint>>();
	private ArrayList<ArrayList<CustomPoint>> redResults = new ArrayList<ArrayList<CustomPoint>>();
//	private ArrayList<ArrayList<CustomPoint>> subSetResult;
	private CommandController commandController;
	public static boolean flyingEast = true;
	private int maxSize = 0;

	public DownScanSeq(CommandController commandController, Mat mat) {
		greenDone = false;
		redDone = false;
		pictureProcessingHelper = new PictureProcessingHelper();
		camMat = mat;
		this.commandController = commandController;
	}

	public DownScanSeq(CommandController commandController) {
		greenDone = false;
		redDone = false;
		pictureProcessingHelper = new PictureProcessingHelper();
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	public void run() {
		// commandController.droneInterface.setBottomCamera();
		do {
			System.out.println("Start green scan");
			long start;
			long elapsedTime;
			greenDone = false;
			redDone = false;
//			redResults.clear();
//			greenResults.clear();
			start = System.currentTimeMillis();
			scanRedGreen();
			elapsedTime = System.currentTimeMillis() - start;
			System.out.println("Time for green and green scan: " + elapsedTime
					+ " ms");
		} while (!greenDone && !redDone);
		greenDone = false;
		redDone = false;

		System.out.println(redResults.size());
		System.out.println(greenResults.size());
		PictureController.setPlacement(new CustomPoint(460, 107));
		PictureController.addCords(calculateScanResults(redResults), Color.RED);
		PictureController.addCords(calculateScanResults(greenResults),
				Color.GREEN);
		CustomPoint flipPoint1 = new CustomPoint(460 + 10, 107 + 10);
		CustomPoint flipPoint2 = new CustomPoint(460 + 10, 107 + 10);
		CustomPoint flipPoint3 = new CustomPoint(460 + 10, 107 + 10);
		PictureController.addCord(flipPoint1);
		flipPoint2.horiFlipCoords();

		PictureController.addCord(flipPoint2);
		flipPoint3.vertFlipCoords();

		PictureController.addCord(flipPoint3);

		// commandController.droneInterface.setFrontCamera();

	}

	private void scanRedGreen() {

		while (!(redResults.size() == 100 && greenResults.size() == 100)) {
			if (OFVideo.imageChanged) {
				OFVideo.imageChanged = false;

				Mat objectsGreen = pictureProcessingHelper
						.findContoursGreenMat(camMat.clone());
				Mat objectsRed = pictureProcessingHelper
						.findContoursRedMat(camMat.clone());
				greenResults.add(pictureProcessingHelper
						.findObjectsMat(objectsGreen));
				redResults.add(pictureProcessingHelper
						.findObjectsMat(objectsRed));
			}
		}
		greenDone = true;
		redDone = true;
	}

	public void scanGreen() {
		while (!greenDone) {
			if (OFVideo.imageChangedGreen) {
				scanGreenSeq();
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean scanGreenSeq() {
		Mat greenMat = camMat.clone();
		OFVideo.imageChangedGreen = false;
		greenMat = pictureProcessingHelper.findContoursGreenMat(camMat);
		greenResults.add(pictureProcessingHelper.findObjectsMat(greenMat));
		// todo make done constraints
		if (greenResults.size() == 100) {
			greenDone = true;
		}
		return greenDone;
	}

	public void scanRed() {
		while (!redDone) {
			if (OFVideo.imageChangedRed) {
				scanRedSeq();
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean scanRedSeq() {
		Mat redMat = camMat.clone();
		OFVideo.imageChangedRed = false;
		redMat = pictureProcessingHelper.findContoursRedMat(camMat);
		redResults.add(pictureProcessingHelper.findObjectsMat(redMat));

		// todo make done constraints
		if (redResults.size() == 100) {
			redDone = true;
		}
		return redDone;
	}

	public ArrayList<CustomPoint> calculateScanResults(
			ArrayList<ArrayList<CustomPoint>> results) {
		maxSize = 0;
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		// subSetResult.clear();
		for (ArrayList<CustomPoint> points : results) {
			Integer value = map.get(points.size());
			System.out.println(value);
			if (value != null) {
				map.put(points.size(), ++value);
			} else {
				map.put(points.size(), new Integer(1));
			}
		}
		for (Integer key : map.keySet()) {
			System.out.println("number of cubes: " + key + " frames: "
					+ map.get(key).toString());
			if (maxSize < key) {
				maxSize = key;
			}
		}
		System.out.println(maxSize);
		ArrayList<ArrayList<CustomPoint>> subSetResult = new ArrayList<ArrayList<CustomPoint>>();
		for (ArrayList<CustomPoint> points : results) {
			if (points.size() == maxSize)
				subSetResult.add(points);
		}
		if (!subSetResult.isEmpty()) {
			for (CustomPoint point : subSetResult.get(subSetResult.size() - 1)) {
				System.out
						.println("x: " + point.getX() + " y: " + point.getY());

				point.setX(CENTER_OF_DRONE_X - point.getX() / PIXELS_PER_CM_X);
				point.setY(CENTER_OF_DRONE_Y - point.getY() / PIXELS_PER_CM_Y);

				System.out.println("x_new: " + point.getX() + " y_new: "
						+ point.getY());
				if (flyingEast) {
					// TODO: flip Vertical
//					point.setY(point.getX() - Math.abs(point.getX() - PictureController.getPlacement().getX()));
				} else {
					//TODO: flip horizontal
//					point.setX(point.getY() - (point.getY() - PictureController.getPlacement().getY()));
				}
//				System.out.println("x_new: " + point.getX() + " y_new: "
//						+ point.getY());
			}

			return subSetResult.get(subSetResult.size() - 1);
		}

		return new ArrayList<CustomPoint>();

	}

}
