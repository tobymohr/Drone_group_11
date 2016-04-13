package helper;

public class Circle {
	private Point center;
	private double radius;
	
	public Circle() {	
	}
	
	public Circle(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}
	public Point getCenter() {
		return center;
	}

	public void setCenter(Point center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}
	public void setRadius(double r) {
		this.radius = r;
	}
	
	public static double calculateRadius(double distanceBetweenPoints, double angle) {
		return 0.5 * (distanceBetweenPoints / Math.sin(angle));
	}
	
	public static Point calculateCenter(Point P1, Point P2, double distanceBetweenPoints, double angle) {
		double x, y, a;
		a = distanceBetweenPoints;
		
		x = 0.5 * (P2.getY() - P1.getY())
				/(Math.sqrt(Math.pow(Math.abs(-1 * P2.getY() + P1.getY()), 2) 
						+ Math.pow(Math.abs(-1 * P2.getX() + P1.getX()), 2)))
				* Math.sqrt((Math.pow(a, 2))/(Math.pow(Math.sin(angle), 2)) - Math.pow(a, 2))
				+ 0.5 * P1.getX() + 0.5 * P2.getX();

		y = 0.5 * (-1 * P2.getX() + P1.getX())
				/(Math.sqrt(Math.pow(Math.abs(-1 * P2.getY() + P1.getY()), 2) 
						+ Math.pow(Math.abs(-1 * P2.getX() + P1.getX()), 2)))
				* Math.sqrt((Math.pow(a, 2))/(Math.pow(Math.sin(angle), 2)) - Math.pow(a, 2))
				+ 0.5 * P1.getY() + 0.5 * P2.getY();

		return new Point(x, y);
	}
	
	//Source: http://paulbourke.net/geometry/circlesphere/
	public static Point[] intersection(Circle c1, Circle c2) {
		Point[] result = new Point[2];
		Point P0 = new Point(c1.getCenter().getX(), c1.getCenter().getY());
		Point P1 = new Point(c2.getCenter().getX(), c2.getCenter().getY());
		
		double d = P0.distance(P1);
		double a = (Math.pow(c1.getRadius(), 2) - Math.pow(c2.getRadius(), 2) + Math.pow(d, 2))/(2 * d);
		double h = Math.sqrt(Math.pow(c1.getRadius(), 2) - Math.pow(a, 2));
		
		Point P2 = P1.subtract(P0).scale(a/d).add(P0);
		
		Point P3 = new Point();
		Point P4 = new Point();
		
		P3.setX(P2.getX() + h * (P1.getY() - P0.getY()) / d);
		P3.setY(P2.getY() - h * (P1.getX() - P0.getX()) / d);
		
		P4.setX(P2.getX() - h * (P1.getY() - P0.getY()) / d);
		P4.setY(P2.getY() + h * (P1.getX() - P0.getX()) / d);
		
		result[0] = P3;
		result[1] = P4;
		
		return result;
	}
}
