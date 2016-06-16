package coordinateSystem;

import java.util.ArrayList;

import helper.CustomPoint;

public class Coordinates {
	ArrayList<CustomPoint> pointList;

	public ArrayList<CustomPoint> getPoints() {
		return pointList;
	}

	public Coordinates() {
		pointList = new ArrayList<CustomPoint>();
	}

	public void addPoint(double x, double y, int colour) {
		pointList.add(new CustomPoint(x, y, colour));
	}

	public void addPoints(ArrayList<CustomPoint> tempList, CustomPoint currentPos) {
		for (CustomPoint cord : tempList) {
			addPoint(cord.getX() + currentPos.getX(), cord.getY() + currentPos.getY(), cord.getColour());
		}
	}

	public void drawCoordinates() {
		DrawCoordinates.init(pointList);
	}
}
