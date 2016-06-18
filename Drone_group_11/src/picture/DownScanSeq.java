package picture;

import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.Mat;

import app.CommandController;
import helper.CustomPoint;

public class DownScanSeq {

	public boolean greenDone;
	public boolean redDone;
	private PictureProcessingHelper PPH;
	private Mat camMat;
	private Mat camMat2;
	private ArrayList<ArrayList<CustomPoint>> greenResults;
	private ArrayList<ArrayList<CustomPoint>> redResults;
	private scanGreen green;
	private scanRed red;
	private CommandController commandController;

	public DownScanSeq(Mat camMat, CommandController commandController) {
		greenResults.clear();
		redResults.clear();
		this.camMat = camMat;
		camMat2 = camMat.clone();
		greenDone = false;
		redDone = false;
		PPH = new PictureProcessingHelper();
		this.commandController = commandController;
		commandController.droneInterface.setBottomCamera();
	}

	public void startThreads() {
		green = new scanGreen();
		red = new scanRed();
		green.start();
		red.start();
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
		this.camMat2 = camMat.clone();
	}

	class scanGreen extends Thread {
		Mat greenMat;

		@Override
		public void run() {
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
			OFVideo.imageChangedGreen = false;
			greenMat = PPH.findContoursGreenMat(camMat);
			greenResults.add(PPH.findObjectsMat(greenMat));
			

			// todo make done constraints
			if (greenResults.size() == 100) {
				greenDone = true;
			}
			return greenDone;
		}
	}

	class scanRed extends Thread {
		Mat redMat;

		@Override
		public void run() {
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
			if (redDone) {
				calculateScanResults();
			}
		}

		private boolean scanRedSeq() {
			OFVideo.imageChangedRed = false;
			redMat = PPH.findContoursGreenMat(camMat2);
			redResults.add(PPH.findObjectsMat(redMat));

			// todo make done constraints
			if (redResults.size() == 100) {
				redDone = true;
			}
			return redDone;
		}
	}

	public void calculateScanResults() {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (ArrayList<CustomPoint> points : redResults) {
			Integer value = map.get(points.size());
			if (value != null) {
				map.put(points.size(), value++);
			} else {
				map.put(points.size(), new Integer(1));
			}
		}
		for (Integer key : map.keySet()) {
			System.out.println(key + ": penis" + map.get(key).toString());
		}
	}

}
