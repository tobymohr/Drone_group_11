package application;
	
import helper.Circle;
import helper.Point;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("ControlJfx.fxml"));
			Scene scene = new Scene(root,1000,1000);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		double distanceBetweenPointsOne = 1.5;
		double distanceBetweenPointsTwo = Math.sqrt(Math.pow(1.5, 2) + Math.pow(1.5, 2));
		double distanceOne = 2.55;
		double distanceTwo = 2.7;
		double distanceThree = 2.7;
		double angleA = Point.calculateAngle(distanceOne, distanceBetweenPointsOne);
		double angleB = Point.calculateAngle(distanceThree, distanceBetweenPointsTwo);
//		System.out.println(angleA);
//		System.out.println(angleB);
		String codeOne = "W02.02";
		String codeTwo = "W02.01";
		String codeThree = "W02.00";
		Point P1 = new Point(2, 0);
		Point P2 = new Point(1, 0);
		Point P3 = new Point(0, 1);
		Circle C1 = new Circle(Circle.calculateCenter(P1, P2, distanceBetweenPointsOne, angleA), 
				Circle.calculateRadius(distanceBetweenPointsOne, angleA));
		System.out.println(C1.getCenter().getX() + "|" + C1.getCenter().getY() + "|" + C1.getRadius());
		Circle C2 = new Circle(Circle.calculateCenter(P2, P3, distanceBetweenPointsTwo, angleB), 
				Circle.calculateRadius(distanceBetweenPointsTwo, angleB));
		System.out.println(C2.getCenter().getX() + "|" + C2.getCenter().getY() + "|" + C2.getRadius());
		Point[] points = Circle.intersection(C1, C2);
		for (Point p : points) {
			System.out.println(p.getX() + "|" + p.getY());
		}
		launch(args);
	}
}
