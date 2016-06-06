package application;
	
import helper.Circle;
import helper.CustomPoint;
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
		double distanceBetweenPointsTwo = 1.5;
		double distanceOne = 3.3;
		double distanceTwo = 3;
		double distanceThree = 3.3;
		double angleA = CustomPoint.calculateAngle(distanceOne, distanceBetweenPointsOne);
		double angleB = CustomPoint.calculateAngle(distanceThree, distanceBetweenPointsTwo);
//		System.out.println(angleA);
//		System.out.println(angleB);
		String codeOne = "W02.02";
		String codeTwo = "W02.01";
		String codeThree = "W02.00";
		CustomPoint P1 = new CustomPoint(6, 0);
		CustomPoint P2 = new CustomPoint(5, 0);
		CustomPoint P3 = new CustomPoint(4, 0);
		Circle C1 = new Circle(Circle.calculateCenter(P1, P2, distanceBetweenPointsOne, angleA), 
				Circle.calculateRadius(distanceBetweenPointsOne, angleA));
		System.out.println(C1.getCenter().getX() + "|" + C1.getCenter().getY() + "|" + C1.getRadius());
		Circle C2 = new Circle(Circle.calculateCenter(P2, P3, distanceBetweenPointsTwo, angleB), 
				Circle.calculateRadius(distanceBetweenPointsTwo, angleB));
		System.out.println(C2.getCenter().getX() + "|" + C2.getCenter().getY() + "|" + C2.getRadius());
		CustomPoint[] points = Circle.intersection(C1, C2);
		for (CustomPoint p : points) {
			System.out.println(Math.round(p.getX()) + "|" + Math.round(p.getY()));
		}
		launch(args);
	}
}
