package helper;

public class Move {
	
	public static final int MOVE_NONE = 0;
	public static final int MOVE_FORWARD = 1;
	public static final int MOVE_LEFT = 2;
	public static final int MOVE_RIGHT = 3;
	public static final int MOVE_DOWN = 4;
	private int move = 0;
	
	public Move (int move){
		this.move = move;
	}
	
	public int getMove(){
		return this.move;
	}

}
