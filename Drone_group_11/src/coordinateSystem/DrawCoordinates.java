package coordinateSystem;



import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class DrawCoordinates extends JFrame {

	private static final long serialVersionUID = 1L;
	ArrayList<Cord> cords;
	static int scale = 60;
	static int push = 50;
	static int x = (int)(10.78*scale+push);
    static int y = (int)(9.63*scale+push);

public DrawCoordinates(ArrayList<Cord> cords) {
	this.cords = cords;
    add(new DrawPanel());
  }

  public static void init(ArrayList<Cord> cords){
	  DrawCoordinates frame = new DrawCoordinates(cords);
    frame.setTitle("Map");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(x+push, y+push);
    frame.setLocationRelativeTo(null); // Center the frame
    frame.setVisible(true);
  }
  

  class DrawPanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 DrawPanel(){
	      repaint();
	    }


    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(Color.BLACK);
      
      // draw grid
      // X
      int xtemp = push;
      while (xtemp<x){
    	  g.drawLine(xtemp, push, xtemp, y);
    	  g.drawString(String.valueOf(xtemp/scale), xtemp, push-10);
    	  xtemp+= scale;
      }
      g.drawLine(x, push, x, y);
      g.drawString(String.valueOf(10.78), x, push-10);
      // Y
      int ytemp = push;
      while (ytemp<y){
    	  g.drawLine(push, ytemp, x, ytemp);
    	  g.drawString(String.valueOf(ytemp/scale), push-10, ytemp);
    	  ytemp+= scale;
      }
      g.drawLine(push, y, x, y);
      g.drawString(String.valueOf(9.63), push-25, y);
      
      
      
      
      // draw points
      for(int i=0; i<cords.size();i++){
    	  if(cords.get(i).green){
    		  g.setColor(Color.GREEN);
    	  }else{
    		  g.setColor(Color.RED);
    	  }
      g.fillOval((int)(cords.get(i).x*scale+push-5), (int)(cords.get(i).y*scale+push-5), 10, 10);
      
      }
  }

}
}