package coordinateSystem;

import java.util.ArrayList;

public class Coordinates {
	ArrayList<Cord> cordList;
	
	public ArrayList<Cord> getCords() {
		return cordList;
	}

	public Coordinates(){
		cordList = new ArrayList<Cord>();
	}
	
	public void addCord(double x, double y, boolean green){
		cordList.add(new Cord(x,y,green));
	}
	

}

