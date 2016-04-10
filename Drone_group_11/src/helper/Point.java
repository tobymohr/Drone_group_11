package helper;

public class Point {
	private static final int MAX_Y_COORDINATE = 6; //TODO FIGURE OUT THE REAL MAX Y COORDINATE
	private static final int MAX_X_COORDINATE = 5; //TODO FIGURE OUT THE REAL MAX x COORDINATE

	private double x;
	private double y;
	
	public Point() {
	}
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
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

	public Point subtract(Point p) {
		return new Point(this.x - p.getX(), this.y - p.getY());
	}
	
	public Point add(Point p) {
		return new Point(this.x + p.getX(), this.y + p.getY());
	}
	
	public double distance(Point p) {
		return Math.sqrt(Math.pow(this.x - p.getX(), 2) + Math.pow(this.y - p.getY(), 2));
	}
	
	public Point scale(double s) {
		return new Point(this.x * s, this.y * s);
	}
	
	public static double calculateAngle(double distanceToPoint, double distanceBetweenPoints) {
		return Math.atan(distanceBetweenPoints / distanceToPoint);
	}
	
	public static Point parseQRText(String text) {
		text = text.substring(1);
		if (text.startsWith("00")) {
			return new Point(Double.parseDouble(text.substring(4)), MAX_Y_COORDINATE);
		} else if (text.startsWith("01")) {
			return new Point(MAX_X_COORDINATE, Double.parseDouble(text.substring(4)));
		} else if (text.startsWith("02")) {
			return new Point(Double.parseDouble(text.substring(4)), 0);
		} else if (text.startsWith("03")) {
			return new Point(0, Double.parseDouble(text.substring(4)));
		} else {
			return null;
		}
	}
}
