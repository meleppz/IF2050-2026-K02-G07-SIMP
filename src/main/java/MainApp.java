import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.control.ScrollPane;
import view.DashboardView;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DashboardView dashboard = new DashboardView();
        ScrollPane scrollPane = new ScrollPane(dashboard);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f4f6f9;");

        Scene scene = new Scene(scrollPane, 1280, 900);
        stage.setTitle("SIMP - Dashboard");
        stage.setScene(scene);

        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Bold.ttf"), 14);

        try {
            java.sql.Connection conn = util.DBConnection.getConnection();
            System.out.println("=== DB INFO ===");
            System.out.println("Connected: " + !conn.isClosed());
        } catch (Exception e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}