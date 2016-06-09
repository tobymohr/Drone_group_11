package helper;

public class Move {
	
	public static int MOVE_NONE = 0;
	public static int MOVE_FORWARD = 1;
	public static int MOVE_LEFT = 2;
	public static int MOVE_RIGHT = 3;
	public static int MOVE_DOWN = 4;
	private int move = 0;
	
	public Move (int move){
		this.move = move;
	}
	
	public int getMove(){
		return this.move;
	}

}
