package coordinateSystem;

import javax.swing.*;

import helper.CustomPoint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Map extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int MAX_Y = 1078;
	
	private ArrayList<CustomPoint> cords;
	private static double scale = 0.5;
	private static int push = 50;
	private static int x = (int) (963 * scale + push);
	private static int y = (int) (1078 * scale + push);
	private static CustomPoint placement = new CustomPoint(0, 0);

	private Map(ArrayList<CustomPoint> cords) {
		this.cords = cords;
		add(new DrawPanel());
	}

	public static Map init(ArrayList<CustomPoint> cords) {
		Map frame = new Map(cords);
		frame.setTitle("Map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(x + push * 2, y + push * 2);
		frame.setLocationRelativeTo(null); // Center the frame
		frame.setVisible(true);
		return frame;
	}
	
	public void addCord(CustomPoint point) {
		cords.add(new CustomPoint(point.getX() + placement.getX(), point.getY() + placement.getY(), point.getColour()));
		repaint();
	}
	
	public void addCord(CustomPoint point, CustomPoint placement) {
		Map.placement = placement;
		cords.add(new CustomPoint(point.getX() + placement.getX(), point.getY() + placement.getY(), point.getColour()));
		repaint();
	}
	
	public void addCords(ArrayList<CustomPoint> tempList) {
		for (CustomPoint cord : tempList) {
			cords.add(new CustomPoint(cord.getX() + placement.getX(), cord.getY() + placement.getY(), cord.getColour()));
		}
		repaint();		
	}

	public void addCords(ArrayList<CustomPoint> tempList, CustomPoint placement) {
		Map.placement = placement;
		for (CustomPoint cord : tempList) {
			cords.add(new CustomPoint(cord.getX() + placement.getX(), cord.getY() + placement.getY(), cord.getColour()));
		}
		repaint();
	}
	class DrawPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		DrawPanel() {
			repaint();
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			
			drawGrid(g);
			
			drawQR(g);

			// draw points
			for (int i = 0; i < cords.size(); i++) {
				if (cords.get(i).getColour() == CustomPoint.GREEN) {
					g.setColor(Color.GREEN);
				} else {
					g.setColor(Color.RED);
				}
				g.fillOval((int)(cords.get(i).getX() * scale + push - 5), (int) (cords.get(i).getY() * scale + push - 5), 10,
						10);

			}
		}
		
		public void drawGrid(Graphics g) {
			// draw grid
			// X
			int xtemp = push;
			g.drawLine(xtemp, push, xtemp, y);
			g.drawString(String.valueOf(xtemp / scale - push * 2), xtemp, push - 35);
			xtemp += push;
			while (xtemp < x) {
				drawDashedLine(g, xtemp, push, xtemp, y);
				g.drawString(String.valueOf(xtemp / scale - push * 2), xtemp, push - 35);
				xtemp += push;
			}
			g.drawLine(x, push, x, y);
			g.drawString(String.valueOf(1078), x, push - 35);
			// Y
			int ytemp = push;
			g.drawLine(push, ytemp, x, ytemp);
			g.drawString(String.valueOf(ytemp / scale - push * 2), push - 40, ytemp);
			ytemp += push;
			while (ytemp < y) {
				drawDashedLine(g, push, ytemp, x, ytemp);
				g.drawString(String.valueOf(ytemp / scale - push * 2), push - 40, ytemp);
				ytemp += push;
			}
			g.drawLine(push, y, x, y);
			g.drawString(String.valueOf(963), push - 40, y);
		}
		
		public void drawQR(Graphics g) {
			String csvFile = "WallCoordinates.csv";
			BufferedReader br = null;
			String line = "";
			String cvsSplitBy = ";";
			
			try {
				br = new BufferedReader(new FileReader(csvFile));
				while ((line = br.readLine()) != null) {
					String[] coordinate = line.split(cvsSplitBy);
					int QRX = (int) (Integer.parseInt(coordinate[1]) * scale) + push;
					int QRY = (int) (Integer.parseInt(coordinate[2]) * scale) + push;
					g.drawString(coordinate[0], QRX, QRY);
					g.fillOval(QRX - 5, QRY - 5, 10, 10);
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
		
		public void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2){
	        Graphics2D graphics = (Graphics2D) g.create();

	        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
	        graphics.setStroke(dashed);
	        graphics.drawLine(x1, y1, x2, y2);

	        graphics.dispose();
		}

	}
}