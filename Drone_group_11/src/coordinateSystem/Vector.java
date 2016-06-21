package coordinateSystem;

import java.util.ArrayList;


public class Vector {
	public double x, y;

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Vector add(Vector v) {
		return new Vector(this.x + v.x, this.y + v.y);
	}

	public Vector subtract(Vector v) {
		return new Vector(this.x - v.x, this.y - v.y);
	}

	public double scalarProduct(Vector v) {
		return this.x * v.x + this.y * v.y;
	}

	public Vector mult(double factor) {
		return new Vector(this.x * factor, this.y * factor);
	}

	public Vector mult(Vector v) {
		return new Vector(this.x * v.x + this.x * v.y, this.y * v.x + this.y * v.y);
	}

	public double angle(Vector v) {
		return Math.toDegrees(Math.acos((this.scalarProduct(v)) / (getVectorLength(this) * getVectorLength(v))));
	}

	public double getVectorLength(Vector v) {
		return Math.abs(Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.y, 2)));
	}

	public double radiansBetweenV(Vector v) {
		return Math.acos((scalarProduct(v) / (getVectorLength(v) * getVectorLength(this))));
	}

	public Vector unit() {
		double L = Math.sqrt((this.x * this.x) + (this.y * this.y));
		return new Vector(this.x / L, this.y / L);
	}

	public String toString() {
		return "(" + this.x + "," + this.y + ")";
	}

}
