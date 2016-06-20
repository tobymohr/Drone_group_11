package coordinateSystem;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import helper.CustomPoint;

public class Map extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int MAX_X = 926;
	private static final int MAX_Y = 1078;
	private static final int STEP_X = MAX_X / 10;
	private static final int STEP_Y = MAX_Y / 10;

	private ArrayList<CustomPoint> cords;
	private CustomPoint placement = new CustomPoint(0, 0);

	private Map(ArrayList<CustomPoint> cords) {
		this.cords = cords;
		add(new DrawPanel());
	}

	public static Map init(ArrayList<CustomPoint> cords) {
		Map frame = new Map(cords);
		frame.setTitle("Map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 800);
		frame.setLocationRelativeTo(null); // Center the frame
		frame.setVisible(true);
		return frame;
	}

	public void addCord(CustomPoint point) {
		cords.add(new CustomPoint(point.getX() + placement.getX(), point.getY() + placement.getY(), point.getColour()));
		repaint();
	}

	public void addCord(CustomPoint point, CustomPoint placement) {
		this.placement = placement;
		cords.add(new CustomPoint(point.getX() + placement.getX(), point.getY() + placement.getY(), point.getColour()));
		repaint();
	}

	public void addCords(ArrayList<CustomPoint> tempList) {
		for (CustomPoint cord : tempList) {
			cords.add(
					new CustomPoint(cord.getX() + placement.getX(), cord.getY() + placement.getY(), cord.getColour()));
		}
		repaint();
	}

	public void addCords(ArrayList<CustomPoint> tempList, CustomPoint placement) {
		this.placement = placement;
		for (CustomPoint cord : tempList) {
			cords.add(
					new CustomPoint(cord.getX() + placement.getX(), cord.getY() + placement.getY(), cord.getColour()));
		}
		repaint();
	}
	public void addCords(ArrayList<CustomPoint> tempList, Color color) {
		for (CustomPoint cord : tempList) {
			cords.add(
					new CustomPoint(cord.getX() + placement.getX(), cord.getY() + placement.getY(), color));
		}
		repaint();
	}
	public CustomPoint getPlacement() {
		return placement;
	}
	
	public void setPlacement(CustomPoint placement) {
		this.placement = placement;
		repaint();
	}

	class DrawPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		DrawPanel() {
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			CoordinateSystem coordinateSystem = new CoordinateSystem(0.5, 0.5, 100, 600);
			drawGrid(g, coordinateSystem);
			drawQR(g, coordinateSystem);

			// draw points
			for (int i = 0; i < cords.size(); i++) {
				int cordX = (int) cords.get(i).getX();
				int cordY = (int) cords.get(i).getY();
				coordinateSystem.drawPoint(g, new Vector(cordX, cordY), 10, cords.get(i).getColour());
			}

			// draw drone
			coordinateSystem.drawPoint(g, new Vector(placement.getX(), placement.getY()), 15);
		}

		public void drawGrid(Graphics g, CoordinateSystem coordinateSystem) {
			// draw grid
			// X
			int xtemp = STEP_X;
			coordinateSystem.drawLine(g, new Vector(0, 0), new Vector(0, MAX_Y));
			coordinateSystem.drawString(g, String.valueOf(0), new Vector(0, -50));
			while (xtemp < MAX_X - STEP_X) {
				coordinateSystem.drawDashedLine(g, new Vector(xtemp, 0), new Vector(xtemp, MAX_Y));
				coordinateSystem.drawString(g, String.valueOf(xtemp), new Vector(xtemp, -50));
				xtemp += STEP_X;
			}
			coordinateSystem.drawLine(g, new Vector(MAX_X, 0), new Vector(MAX_X, MAX_Y));
			coordinateSystem.drawString(g, String.valueOf(MAX_X), new Vector(MAX_X, -50));

			int ytemp = STEP_Y;
			coordinateSystem.drawLine(g, new Vector(0, 0), new Vector(MAX_X, 0));
			while (ytemp < MAX_Y - STEP_Y) {
				coordinateSystem.drawDashedLine(g, new Vector(0, ytemp), new Vector(MAX_X, ytemp));
				coordinateSystem.drawString(g, String.valueOf(ytemp), new Vector(-50, ytemp));
				ytemp += STEP_Y;
			}
			coordinateSystem.drawLine(g, new Vector(0, MAX_Y), new Vector(MAX_X, MAX_Y));
			coordinateSystem.drawString(g, String.valueOf(MAX_Y), new Vector(-50, MAX_Y));
		}

		public void drawQR(Graphics g, CoordinateSystem coordinateSystem) {
			String csvFile = "WallCoordinates.csv";
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ";";

			try {
				br = new BufferedReader(new FileReader(csvFile));
				while ((line = br.readLine()) != null) {
					String[] coordinate = line.split(cvsSplitBy);
					int QRX = Integer.parseInt(coordinate[1]);
					int QRY = Integer.parseInt(coordinate[2]);
					if (coordinate[0].startsWith("W00")) {
						coordinateSystem.drawString(g, coordinate[0], new Vector(QRX, QRY + 50), Color.BLUE);
					} else if (coordinate[0].startsWith("W01")) {
						coordinateSystem.drawString(g, coordinate[0], new Vector(QRX + 60, QRY), Color.BLUE);
					} else if (coordinate[0].startsWith("W02")) {
						coordinateSystem.drawString(g, coordinate[0], new Vector(QRX, QRY - 100), Color.BLUE);
					} else if (coordinate[0].startsWith("W03")) {
						coordinateSystem.drawString(g, coordinate[0], new Vector(QRX - 150, QRY), Color.BLUE);
					}
					coordinateSystem.drawPoint(g, new Vector(QRX, QRY), 10, Color.BLUE);
					g.setColor(Color.BLACK);
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
		}
	}
}