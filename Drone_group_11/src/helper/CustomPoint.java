package helper;

public class CustomPoint {
	private static final int MAX_Y_COORDINATE = 6; //TODO FIGURE OUT THE REAL MAX Y COORDINATE
	private static final int MAX_X_COORDINATE = 5; //TODO FIGURE OUT THE REAL MAX x COORDINATE

	private double x;
	private double y;
	
	public CustomPoint() {
	}
	
	public CustomPoint(double x, double y) {
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
	
	public static double calculateAngle(double distanceToPoint, double distanceBetweenPoints) {
		return Math.toDegrees(Math.atan(distanceBetweenPoints / distanceToPoint));
	}
	
	public static CustomPoint parseQRText(String text) {
		text = text.substring(1);
		if (text.startsWith("00")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)) + 1, MAX_Y_COORDINATE);
		} else if (text.startsWith("01")) {
			return new CustomPoint(MAX_X_COORDINATE, Double.parseDouble(text.substring(4)) + 1);
		} else if (text.startsWith("02")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)) + 1, 0);
		} else if (text.startsWith("03")) {
			return new CustomPoint(0, Double.parseDouble(text.substring(4)) + 1);
		} else {
			return null;
		}
	}
	
	public static CustomPoint parseQRTextLeft(String text) {
		text = text.substring(1);
		if (text.startsWith("00")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)), MAX_Y_COORDINATE);
		} else if (text.startsWith("01")) {
			return new CustomPoint(MAX_X_COORDINATE, Double.parseDouble(text.substring(4)) + 2);
		} else if (text.startsWith("02")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)) + 2, 0);
		} else if (text.startsWith("03")) {
			return new CustomPoint(0, Double.parseDouble(text.substring(4)));
		} else {
			return null;
		}
	}
	
	public static CustomPoint parseQRTextRight(String text) {
		text = text.substring(1);
		if (text.startsWith("00")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)) + 2, MAX_Y_COORDINATE);
		} else if (text.startsWith("01")) {
			return new CustomPoint(MAX_X_COORDINATE, Double.parseDouble(text.substring(4)));
		} else if (text.startsWith("02")) {
			return new CustomPoint(Double.parseDouble(text.substring(4)), 0);
		} else if (text.startsWith("03")) {
			return new CustomPoint(0, Double.parseDouble(text.substring(4)) + 2);
		} else {
			return null;
		}
	}
}
