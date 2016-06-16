package helper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CustomPoint {
	public static final int GREEN = 0;
	public static final int RED = 1;
	
	private double x;
	private double y;
	private int colour;
	
	public CustomPoint() {
	}
	
	public CustomPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public CustomPoint(double x, double y, int colour) {
		this.x = x;
		this.y = y;
		this.colour = colour;
	}
	
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
	
	public int getColour() {
		return colour;
	}
	
	public void setColour(int colour) {
		this.colour = colour;
	}

	public CustomPoint subtract(CustomPoint p) {
		return new CustomPoint(this.x - p.getX(), this.y - p.getY());
	}
	
	public CustomPoint add(CustomPoint p) {
		return new CustomPoint(this.x + p.getX(), this.y + p.getY());
	}
	
	public double distance(CustomPoint p) {
		return Math.sqrt(Math.pow(this.x - p.getX(), 2) + Math.pow(this.y - p.getY(), 2));
	}
	
	public CustomPoint scale(double s) {
		return new CustomPoint(this.x * s, this.y * s);
	}
	
	public String toString() {
		return x + "|" + y;
	}
	
	public static double calculateAngle(double distanceToPoint, double distanceBetweenPoints) {
		return Math.atan(distanceBetweenPoints / distanceToPoint);
	}
	
	public static double calculateDistance(CustomPoint P1, CustomPoint P2) {
		return Math.sqrt(Math.pow(P1.getX() - P2.getX(), 2) + Math.pow(P1.getY() - P2.getY(), 2));
	}
	
	public static CustomPoint parseQRText(String text) {
		String csvFile = "WallCoordinates.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ";";
		CustomPoint result = new CustomPoint();
		
		try {
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
				String[] coordinate = line.split(cvsSplitBy);
				if (coordinate[0].equals(text)) {
					result.setX(Integer.parseInt(coordinate[1]));
					result.setY(Integer.parseInt(coordinate[2]));
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	public static CustomPoint parseQRTextLeft(String text) {
		int coordinate = Integer.parseInt(text.substring(4));
		coordinate--;
		return parseQRText(text.substring(0, 5) + coordinate);
	}
	
	public static CustomPoint parseQRTextRight(String text) {
		int coordinate = Integer.parseInt(text.substring(4));
		coordinate++;
		return parseQRText(text.substring(0, 5) + coordinate);
	}
}
