package coordinateSystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

public class CoordinateSystem {
	public Vector o; // Origo
	public Matrix F, S, T; // Flip, Scale, Transform,

	public CoordinateSystem(double sx, double sy, double ox, double oy) {
		o = new Vector(ox, oy);
		F = new Matrix(1, 0, 0, -1);
		S = new Matrix(sx, 0, 0, sy);
		T = F.mul(S);
	}

	public Vector transform(Vector v) {
		return T.mul(v).add(o);
	}

	public void drawLine(Graphics g, Vector p1, Vector p2) {
		Vector p1w = transform(p1);
		Vector p2w = transform(p2);
		g.drawLine((int) p1w.x, (int) p1w.y, (int) p2w.x, (int) p2w.y);
	}

	public void drawString(Graphics g, String string, Vector p1) {
		Vector p1w = transform(p1);
		g.drawString(string, (int) p1w.x, (int) p1w.y);
	}

	public void drawString(Graphics g, String string, Vector p1, Color c) {
		g.setColor(c);
		Vector p1w = transform(p1);
		g.drawString(string, (int) p1w.x, (int) p1w.y);
		g.setColor(Color.BLACK);
	}

	public void drawPoint(Graphics g, Vector p) {
		Vector pw = transform(p);
		g.fillOval((int) pw.x, (int) pw.y, 2, 2);
	}

	public void drawPoint(Graphics g, Vector p, int size) {
		Vector pw = transform(p);
		g.fillOval((int) pw.x - size / 2, (int) pw.y - size / 2, size, size);
	}

	public void drawPoint(Graphics g, Vector p, int size, Color c) {
		g.setColor(c);
		Vector pw = transform(p);
		g.fillOval((int) pw.x - size / 2, (int) pw.y - size / 2, size, size);
		g.setColor(Color.BLACK);
	}

	public void drawAxes(Graphics g) {
		drawLine(g, new Vector(0, 0), new Vector(1, 0));
		// Beautify axes
		drawLine(g, new Vector(0, 0), new Vector(0, 1));
	}

	public void drawEllipse(Graphics g, Vector p, int height, int width) {
		Vector pw = transform(p);
		g.drawOval((int) pw.x - width / 2, (int) pw.y - height / 2, width, height);
	}

	public void drawDashedLine(Graphics g, Vector p1, Vector p2) {
		Vector p1w = transform(p1);
		Vector p2w = transform(p2);
		drawDashedLine(g, (int) p1w.x, (int) p1w.y, (int) p2w.x, (int) p2w.y);
	}

	private void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
		Graphics2D graphics = (Graphics2D) g.create();

		Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2 }, 0);
		graphics.setStroke(dashed);
		graphics.drawLine(x1, y1, x2, y2);

		graphics.dispose();
	}
}
