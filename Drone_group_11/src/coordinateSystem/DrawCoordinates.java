package coordinateSystem;



import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class DrawCoordinates extends JFrame {

	private static final long serialVersionUID = 1L;
	ArrayList<Cord> cords;
	static int scale = 60;

public DrawCoordinates(ArrayList<Cord> cords) {
	this.cords = cords;
    add(new DrawPanel());
  }

  public static void main(String[] args) {
	 init();
  }
  
  public static void init(){
	  ArrayList<Cord> cordss = new ArrayList<Cord>();
	  cordss.add(new Cord(10.78*scale,9.63*scale, true));
	  cordss.add(new Cord(50,6, false));
	  DrawCoordinates frame = new DrawCoordinates(cordss);
    frame.setTitle("Map");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(675, 630);
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
      for(int i=0; i<cords.size();i++){
    	  if(cords.get(i).green){
    		  g.setColor(Color.GREEN);
    	  }else{
    		  g.setColor(Color.RED);
    	  }
      g.fillOval((int)cords.get(i).x, (int)cords.get(i).y, 10, 10);
      
      }
  }

}
}