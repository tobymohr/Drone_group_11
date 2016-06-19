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
	private ArrayList<CustomPoint> greenReturnResults;
	private ArrayList<CustomPoint> redReturnResults;
	private ArrayList<ArrayList<CustomPoint>> subSetResult;
	private CommandController commandController;
	private int maxSize = 0;

	public DownScanSeq(CommandController commandController) {
		greenDone = false;
		redDone = false;
		PPH = new PictureProcessingHelper();
		this.commandController = commandController;
	}

	public void setImage(Mat camMat) {
		this.camMat = camMat;
		this.camMat2 = camMat.clone();
	}
	
	public void scanForCubes() {
		commandController.droneInterface.setBottomCamera();
		do {
			scanGreen();
			scanRed();
		} while (!greenDone && !redDone);
		greenDone = false;
		redDone = false;
		
		PictureController.addCords(calculateScanResults(redResults));
		PictureController.addCords(calculateScanResults(greenResults));
	}

	public void scanGreen()
	{
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
		Mat greenMat = camMat;
		OFVideo.imageChangedGreen = false;
		greenMat = PPH.findContoursGreenMat(camMat);
		greenResults.add(PPH.findObjectsMat(greenMat));

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
		Mat redMat = camMat2;
		OFVideo.imageChangedRed = false;
		redMat = PPH.findContoursGreenMat(camMat2);
		redResults.add(PPH.findObjectsMat(redMat));

		// todo make done constraints
		if (redResults.size() == 100) {
			redDone = true;
		}
		return redDone;
	}



	public ArrayList<CustomPoint> calculateScanResults(ArrayList<ArrayList<CustomPoint>> results) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		subSetResult.clear();
		for (ArrayList<CustomPoint> points : results) {
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
		for(int i = 0; i < map.size(); i++){
			maxSize = map.get(i);
			
			if(maxSize < map.get(i)){
				maxSize = map.get(i);
			}
			
		}
		subSetResult = new ArrayList<ArrayList<CustomPoint>>();
		for(ArrayList<CustomPoint> points : results){
			if(points.size() == maxSize)
				subSetResult.add(points);
		}
		
		return subSetResult.get(subSetResult.size()-1);
	}

}
