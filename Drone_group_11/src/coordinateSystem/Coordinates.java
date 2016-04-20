package coordinateSystem;

import java.util.ArrayList;

import helper.Point;

public class Coordinates {
	ArrayList<Cord> cordList;
	double picScale = 0.1; //!!!!! skal scaleres så picture passer til valgt frame størrelse i rummet
	
	public ArrayList<Cord> getCords() {
		return cordList;
	}

	public Coordinates(){
		cordList = new ArrayList<Cord>();
	}
	
	public void addCord(double x, double y, boolean green){
		cordList.add(new Cord(x,y,green));
	}
	
	public void addCords(ArrayList<Cord> tempList, double altitude, Point currentPos){
		// get altitude
		double altScale = altitude * 0.23; //0.23 skal rettes til whatever
		
		
		for(Cord cord : tempList){
			addCord(cord.x*altScale+currentPos.getX(),cord.y*altScale+currentPos.getY(),cord.green);
		}
	}
	
}

