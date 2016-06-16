package picture;

import static org.bytedeco.javacpp.opencv_imgproc.contourArea;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_imgcodecs;

import app.CommandController;
import helper.Command;
import helper.CustomPoint;

public class ScanSequence implements Runnable {
	private static final int MIN_HIT_COUNT = 6;
	private static final int FORWARD_TIME_2 =2000;
	private static final int BACKWARD_TIME = 1500;
	private static final int BACKWARD_SPEED = 9;
	private static final int STRAFE_TIME = 1200;
	private static final int STRAFE_SPEED = 10;
	private static final int SPIN_TIME = 1500;
	private static final int SPIN_SPEED = 6;
	private static final int FORWARD_TIME = 1200;
	private static final int FORWARD_SPEED = 9;
	private static final int ROTATE_TIME = 5000;
	private static final int ROTATE_SPEED = 17;
	private static final int FIELD_DURATION = 1000;
	private static final int FIELD_SPEED = 12;
	
	
	//#TODO Tweak these values based on testing
	public static final double CENTER_UPPER = 0.15;
	public static final double CENTER_LOWER = -0.15;
	public static final double CENTER_DIFFERENCE = 0.05;
		
	private CommandController commandController;
	private double previousCenter = -1;
	private boolean strafeRight = true;
	private String code = null;
	private int rotateCount = 0;
	private int frameCount = 0;
	private int scannedCount = 0;
	private Mat camMat;
	private int foundFrameCount = 0;
	private CustomPoint placement = null;
	private boolean moveToStart = false;
	private int maxX = 847;
	private int minY = -10;
	private PictureProcessingHelper OFC = new PictureProcessingHelper();
	private Map<Integer, Integer> moves = new HashMap<>();
	private boolean isFirst = false;;
	
	public ScanSequence(CommandController commandController) {
		this.commandController = commandController;
	}
	
	public void setImage(Mat camMat) {
		this.camMat = camMat;
	}
	
	@Override
	public void run() {
		commandController.droneInterface.takeOff();

		sleep(2000);
		System.out.println("HOVER");
		commandController.droneInterface.hover();
		sleep(2000);
		System.out.println("UP");
		commandController.addCommand(Command.UP, 5000, 15);
		
		while(PictureController.shouldScan) {
			if (OFVideo.imageChanged) {
				scanSequence();
			} else {
				sleep(50);
			}
		}
		
		
		
		while(moveToStart){
			moveDroneToStart();
		}
	}
	
	private int getCorrectXMove(){
		if(code.contains("W02")){
			return Command.LEFT;
		}
		if(code.contains("W03")){
			return Command.BACKWARDS;
		}
		if(code.contains("W00")){
			return Command.RIGHT;
		}
		if(code.contains("W01")) {
			return Command.NONE;
		}
		return Command.NONE;
		
	}
	
	private int getCorrectYMove(double y){
		if(code.contains("W02")){
			if(y<minY){
				return Command.BACKWARDS;
			}
			return Command.NONE;
		}
		
		if(code.contains("W03")){
			return Command.LEFT;
		}
		if(code.contains("W00")){
			return Command.BACKWARDS;
		}
		if(code.contains("W01")) {
			return Command.RIGHT;
		}
		
		return Command.NONE;
		
	}
	
	private void rotateCheck(){
		if (frameCount < 4) {
			frameCount++;
		} else {
			code = null;
			if (rotateCount < 15) {
				addCommand(Command.ROTATERIGHT, ROTATE_TIME, ROTATE_SPEED);
				rotateCount++;
			} else {
				rotateCount = 0;
			}
			frameCount = 0;
		}
	}
	
	private RotatedRect mostCenteredRect(List<Mat> contours){
		double distanceFomCenter = Double.MAX_VALUE;
		RotatedRect rect = new RotatedRect();
		for (int i = 0; i < contours.size(); i++) {
			 RotatedRect rect2 = minAreaRect(contours.get(i));
			 double distance = (camMat.arrayWidth() / 2) - rect.center().x();
			 if (distanceFomCenter > distance) {
				 distanceFomCenter = Math.abs(distance);
				 rect = rect2;
			 }
		}
		return rect;
	}
	
	private void centerCheck(double center){
		if (strafeRight) {
			addCommand(Command.RIGHT, STRAFE_TIME, STRAFE_SPEED);
		} else {
			addCommand(Command.LEFT, STRAFE_TIME, STRAFE_SPEED);
		}
		if (previousCenter == -1) {
			previousCenter = center;
		} else {
			double difference = center - previousCenter;
			if (difference > CENTER_DIFFERENCE) {
				strafeRight = !strafeRight;
				previousCenter = center;
				System.out.println("CHANGE STRAFE DIRECTION");
			}
		}
	}
	
	private void spinCheck(double positionFromCenter){
		if (positionFromCenter > 0) {
			addCommand(Command.SPINRIGHT, SPIN_TIME, SPIN_SPEED);
		} else {
			addCommand(Command.SPINLEFT, SPIN_TIME, SPIN_SPEED);
		}
	}
	
	private void scanSequence() {
		OFVideo.imageChanged = false;
		List<Mat> contours = OFC.findQrContours(camMat);
		if (contours.size() == 0) {
			rotateCheck();
			return;
		}
		foundFrameCount = 0;
		frameCount = 0;
		RotatedRect rect = mostCenteredRect(contours);
		
		double positionFromCenter = OFC.isCenterInImage(camMat.clone(), rect);
		if (positionFromCenter != 0) {
			spinCheck(positionFromCenter);
			return;
		}
		double center = OFC.center(rect);
		if (center > CENTER_UPPER || center < CENTER_LOWER) {
			centerCheck(center);
			return;
		}
		previousCenter = -1;
//		double distanceToDrone = OFC.calcDistance(rect);
//		if (distanceToDrone < 300) {
//			addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
//			return;
//		}
		
		Mat qrImg = OFC.warpImage(camMat.clone(), rect);
		String tempCode = OFC.scanQrCode(qrImg);
		if (tempCode != null) {
			code = tempCode;
			System.out.println(tempCode);
		}
		System.out.println("CENTERED");
		if(code != null) {
			if (contours.size() == 3) {
				placement = calculatePlacement(camMat, contours);
				moveToStart = true;
				rotateCount = 0;
				PictureController.shouldScan = false;
				return;
			} else if (scannedCount < 50) {
				scannedCount++;
				return;
			}
			else {
				scannedCount = 0;
				//#TODO Fly backwards (0.5 meters)
				addCommand(Command.BACKWARDS, BACKWARD_TIME, BACKWARD_SPEED);
				return;
			}
		} else {
			double distanceToSquare = OFC.calcDistance(rect);
			if (distanceToSquare > 300) {
				addCommand(Command.FORWARD, FORWARD_TIME_2, FORWARD_SPEED);
				return;
			} else {
				return;
			}
		}
	}
	
	private void sleep(int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
	
	private CustomPoint calculatePlacement(Mat srcImage, List<Mat> contours) {
		CustomPoint placement = new CustomPoint();
		List<Double> positions = new ArrayList<>();
		for (Mat mat : contours) {
			positions.add(OFC.isCenterInImage(srcImage, minAreaRect(mat)));
		}
		int minIndex = positions.indexOf(Collections.min(positions));
		int maxIndex = positions.indexOf(Collections.max(positions));
		int middleIndex = 3 - minIndex - maxIndex;
		
		RotatedRect leftQR = minAreaRect(contours.get(minIndex));
		RotatedRect middleQR = minAreaRect(contours.get(middleIndex));
		RotatedRect rightQR = minAreaRect(contours.get(maxIndex));
		
		CustomPoint[] points = OFC.calcPosition(OFC.calcDistance(leftQR), 
				OFC.calcDistance(middleQR), OFC.calcDistance(rightQR), code);
		CustomPoint scannedPoint = CustomPoint.parseQRText(code);
		for (CustomPoint e : points) {
			System.out.println(e.toString());
			if (Math.round(e.getX()) != scannedPoint.getX() || Math.round(e.getY()) != scannedPoint.getY()) {
				placement = e;
			}
		}
		
		return placement;
	}
	
	private void moveDroneToStart() {
		OFVideo.imageChanged = false;
		List<Mat> contours = OFC.findQrContoursNoThresh(camMat);
		RotatedRect rect = mostCenteredRect(contours);
		double distanceToSquare = OFC.calcDistance(rect);
		int moveY = calcMovesYAxis(placement.getY());
		int moveX = calcMoveXAxis(placement.getX());
		if(code.equals("W02") || code.equals("W00")){
			placement.setY(distanceToSquare);
		}
		if(code.equals("W03") || code.equals("W01")){
			placement.setX(distanceToSquare);
		}
		
		addCommand(moveX, FIELD_DURATION, FIELD_SPEED);
		addCommand(moveY, FIELD_DURATION, FIELD_SPEED);
	}
	
	private int calcMoveXAxis(double x) {
		if(x<maxX){
			return getCorrectXMove();
		}
	
		return Command.NONE;
	}
	
	private int calcMovesYAxis(double y) {
		if(y>minY){
			return getCorrectYMove(y);
		}
		
		if(y<minY){
			return getCorrectYMove(y);
		}
		return Command.NONE;
	}

	
}