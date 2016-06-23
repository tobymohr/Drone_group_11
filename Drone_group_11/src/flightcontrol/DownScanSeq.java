package flightcontrol;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.Mat;

import dronecontrol.CommandController;
import helper.CustomPoint;
import picture.DroneVideo;
import picture.PictureController;
import picture.PictureProcessingHelper;

public class DownScanSeq {

	private static final double MAX_RES_X = 1280.0;
	private static final double MAX_RES_Y = 720.0;
	private static final double CENTER_OF_DRONE_Y = 42.5;
	private static final double CENTER_OF_DRONE_X = 75.0;
	private static final double PIXELS_PER_CM_Y = MAX_RES_X / 150.0;
	private static final double PIXELS_PER_CM_X = MAX_RES_Y / 85.0;
	public boolean greenDone;
	public boolean redDone;
	private PictureProcessingHelper pictureProcessingHelper;
	private Mat camMat;
	private ArrayList<ArrayList<CustomPoint>> greenResults = new ArrayList<ArrayList<CustomPoint>>();
	private ArrayList<ArrayList<CustomPoint>> redResults = new ArrayList<ArrayList<CustomPoint>>();
	public static boolean flyingEast = true;
	private int maxSize = 0;

	public DownScanSeq(CommandController commandController, Mat mat) {
		greenDone = false;
		redDone = false;
		pictureProcessingHelper = new PictureProcessingHelper();
		camMat = mat;
	}

	public DownScanSeq(CommandController commandController) {
		greenDone = false;
		redDone = false;
		pictureProcessingHelper = new PictureProcessingHelper();
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}

	public void run() {
		do {
			scanRedGreen();
		} while (!greenDone && !redDone);
		greenDone = false;
		redDone = false;
		PictureController.addCords(calculateScanResults(redResults), Color.RED);
		PictureController.addCords(calculateScanResults(greenResults), Color.GREEN);

	}

	private void scanRedGreen() {

		while (!(redResults.size() == 100 && greenResults.size() == 100)) {
			if (DroneVideo.imageChanged) {
				DroneVideo.imageChanged = false;

				Mat objectsGreen = pictureProcessingHelper.findContoursGreenMat(camMat.clone());
				Mat objectsRed = pictureProcessingHelper.findContoursRedMat(camMat.clone());
				greenResults.add(pictureProcessingHelper.findObjectsMat(objectsGreen));
				redResults.add(pictureProcessingHelper.findObjectsMat(objectsRed));
			}
		}
		greenDone = true;
		redDone = true;
	}

	public ArrayList<CustomPoint> calculateScanResults(ArrayList<ArrayList<CustomPoint>> results) {
		maxSize = 0;
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
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
			if (maxSize < key) {
				maxSize = key;
			}
		}
		ArrayList<ArrayList<CustomPoint>> subSetResult = new ArrayList<ArrayList<CustomPoint>>();
		for (ArrayList<CustomPoint> points : results) {
			if (points.size() == maxSize)
				subSetResult.add(points);
		}
		if (!subSetResult.isEmpty()) {
			for (CustomPoint point : subSetResult.get(subSetResult.size() - 1)) {
				point.setX(CENTER_OF_DRONE_X - point.getX() / PIXELS_PER_CM_X);
				point.setY(CENTER_OF_DRONE_Y - point.getY() / PIXELS_PER_CM_Y);

			}

			return subSetResult.get(subSetResult.size() - 1);
		}

		return new ArrayList<CustomPoint>();

	}

}
