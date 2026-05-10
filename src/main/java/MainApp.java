import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/ProdukView.fxml")
        );
        Scene scene = new Scene(loader.load(), 1280, 900);
        stage.setTitle("SIMP");
        stage.setScene(scene);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/PlusJakartaSans-Bold.ttf"), 14);
        try {
            java.sql.Connection conn = util.DBConnection.getConnection();
            System.out.println("=== DB INFO ===");
            System.out.println("URL: " + conn.getMetaData().getURL());
            System.out.println("User: " + conn.getMetaData().getUserName());
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