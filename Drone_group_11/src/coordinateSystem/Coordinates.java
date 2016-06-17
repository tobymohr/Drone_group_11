package coordinateSystem;

import java.util.ArrayList;

import helper.CustomPoint;

public class Coordinates {
	public static void main(String[] args) {
		ArrayList<CustomPoint> points = new ArrayList<>();
		points.add(new CustomPoint(250, 500, CustomPoint.RED));
		points.add(new CustomPoint(265, 587, CustomPoint.GREEN));
		Map frame = Map.init(points);
	}

}
