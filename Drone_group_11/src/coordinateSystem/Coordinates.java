package coordinateSystem;

import java.awt.Color;
import java.util.ArrayList;

import helper.CustomPoint;

public class Coordinates {
	public static void main(String[] args) {
		ArrayList<CustomPoint> points = new ArrayList<>();
		points.add(new CustomPoint(250, 500, Color.RED));
		points.add(new CustomPoint(265, 587, Color.GREEN));
		Map.init(points);
	}

}
