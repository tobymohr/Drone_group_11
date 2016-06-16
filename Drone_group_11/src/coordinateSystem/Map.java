package coordinateSystem;

import javax.swing.*;

import helper.CustomPoint;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Map extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int MAX_X = 481;
	private static final int MAX_Y = 539;
	
	private int push = 50;
	private int x = 1078/2 + push;
	private int y = 963/2 + push;
	private ArrayList<CustomPoint> pointsList;
	private static CustomPoint placement = new CustomPoint(0, 0);
	
	public Map(ArrayList<CustomPoint> pointsList) {
		this.pointsList = pointsList;
		add(new DrawPanel());
	}

	public static Map init(ArrayList<CustomPoint> points) {
		Map frame = new Map(points);
		frame.setTitle("Map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 700);
		frame.setLocationRelativeTo(null); // Center the frame
		frame.setVisible(true);
		return frame;
	}
	
	public static void main(String[] args) {
		Map map = Map.init(new ArrayList<>());
	}
	
	public void addPoint(CustomPoint point, CustomPoint placement) {
		pointsList.add(new CustomPoint(point.getX() + placement.getX(), 
				point.getY() + placement.getY(), point.getColour()));
		Map.placement = placement;
		repaint();
	}
	
	public void addPoints(ArrayList<CustomPoint> tempList, CustomPoint placement) {
		for (CustomPoint point : tempList) {
			pointsList.add(new CustomPoint(point.getX() + placement.getX(), 
					point.getY() + placement.getY(), point.getColour()));
		}
		Map.placement = placement;
		repaint();
	}
	
	public static void setPlacement(CustomPoint placement) {
		Map.placement = placement;
	}
	
	public static CustomPoint getPlacement() {
		return placement;
	}

	class DrawPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		DrawPanel() {
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			paintWallMarks(g);
			int xtemp = push;
			while (xtemp < x) {
				g.drawLine(xtemp, push, xtemp, y);
				g.drawString(String.valueOf(xtemp * 2), xtemp, push - 10);
				xtemp += 50;
			}
			g.drawLine(x, push, x, y);
			g.drawString(String.valueOf(10.78), x, push - 10);
//			// Y
//			int ytemp = push;
//			while (ytemp < y) {
//				g.drawLine(push, ytemp, x, ytemp);
//				g.drawString(String.valueOf(ytemp / scale), push - 10, ytemp);
//				ytemp += scale;
//			}
//			g.drawLine(push, y, x, y);
//			g.drawString(String.valueOf(9.63), push - 25, y);
//
//			// draw points
//			for (int i = 0; i < cords.size(); i++) {
//				if (cords.get(i).green) {
//					g.setColor(Color.GREEN);
//				} else {
//					g.setColor(Color.RED);
//				}
//				g.fillOval((int) (cords.get(i).x * scale + push - 5), (int) (cords.get(i).y * scale + push - 5), 10,
//						10);
//
//			}
		}
		
		private void paintWallMarks(Graphics g) {
			String csvFile = "WallCoordinates.csv";
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ";";
			
			try {
				br = new BufferedReader(new FileReader(csvFile));
				while ((line = br.readLine()) != null) {
					String[] coordinate = line.split(cvsSplitBy);
					int x = Integer.parseInt(coordinate[1]) / 2 + push;
					int y = MAX_Y - Integer.parseInt(coordinate[2]) / 2 + push;
					g.fillOval(x - 5, y - 5, 10, 10);
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