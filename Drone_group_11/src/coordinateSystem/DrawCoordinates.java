package coordinateSystem;



import javax.swing.*;

import helper.Vector;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class DrawCoordinates extends JFrame {

	private static final long serialVersionUID = 1L;
	ArrayList<Cord> cords;

public DrawCoordinates(ArrayList<Cord> cords) {
	this.cords = cords;
    add(new DrawPanel());
  }

  public static void main(String[] args) {
	  ArrayList<Cord> cordss = new ArrayList<Cord>();
	  cordss.add(new Cord(5,5, true));
	  cordss.add(new Cord(9,6, false));
	  DrawCoordinates frame = new DrawCoordinates(cordss);
    frame.setTitle("Trajectory");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1200, 1000);
    frame.setLocationRelativeTo(null); // Center the frame
    frame.setVisible(true);
  }

  class DrawPanel extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 DrawPanel(){
	      
	    }


    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      for(int i=0; i<cords.size()-1;i++){
      g.fillOval((int)cords[i]., (int)y, 1, 1);
      }
  }

}
}