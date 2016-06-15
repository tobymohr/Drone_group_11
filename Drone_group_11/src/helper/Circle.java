package helper;

public class Circle {
	private CustomPoint center;
	private double radius;
	
	public Circle() {	
	}
	
	public Circle(CustomPoint center, double radius) {
		this.center = center;
		this.radius = radius;
	}
	public CustomPoint getCenter() {
		return center;
	}

	public void setCenter(CustomPoint center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}
	public void setRadius(double r) {
		this.radius = r;
	}
	
	public String toString() {
		return center.getX() + "|" + center.getY() + "|" + radius;
	}
	
	public static double calculateRadius(double distanceBetweenPoints, double angle) {
		return 0.5 * (distanceBetweenPoints / Math.sin(angle));
	}
	
	public static CustomPoint calculateCenter(CustomPoint P1, CustomPoint P2, double a, double angle) {
		double x1, y1, x2, y2, xCenter, yCenter;
		
		x1 = P1.getX();
		y1 = P1.getY();
		x2 = P2.getX();
		y2 = P2.getY();
				
		double t1 = y2 - y1;
		double t2 = x1 - x2;
		double b1 = Math.pow(y1-y2, 2);
		double b2 = Math.pow(x1 - x2, 2);
		double b3 = Math.sqrt(b1 + b2);
		double e1 = x1/2.0 + x2/2.0;
		double e2 = y1/2.0 + y2/2.0;
		
		double aPow = Math.pow(a, 2);
		double aSine = Math.pow(Math.sin(angle), 2);
		double t = Math.sqrt(1.0 / aSine * aPow - aPow);
		
		
		xCenter = t1 / b3 / 2.0 * t + e1;
		yCenter = t2 / b3 / 2.0 * t + e2;

		return new CustomPoint(xCenter, yCenter);
	}
	
	//Source: http://paulbourke.net/geometry/circlesphere/
	public static CustomPoint[] intersection(Circle c1, Circle c2) {
		CustomPoint[] result = new CustomPoint[2];
		CustomPoint P0 = new CustomPoint(c1.getCenter().getX(), c1.getCenter().getY());
		CustomPoint P1 = new CustomPoint(c2.getCenter().getX(), c2.getCenter().getY());
		
		double d = P0.distance(P1);
		double a = (Math.pow(c1.getRadius(), 2) - Math.pow(c2.getRadius(), 2) + Math.pow(d, 2))/(2 * d);
		double h = Math.sqrt(Math.pow(c1.getRadius(), 2) - Math.pow(a, 2));
		
		CustomPoint P2 = P1.subtract(P0).scale(a/d).add(P0);
		
		CustomPoint P3 = new CustomPoint();
		CustomPoint P4 = new CustomPoint();
		
		P3.setX(P2.getX() + h * (P1.getY() - P0.getY()) / d);
		P3.setY(P2.getY() - h * (P1.getX() - P0.getX()) / d);
		
		P4.setX(P2.getX() - h * (P1.getY() - P0.getY()) / d);
		P4.setY(P2.getY() + h * (P1.getX() - P0.getX()) / d);
		
		result[0] = P3;
		result[1] = P4;
		
		return result;
	}
}
