module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires java.prefs;
    opens com.example to javafx.fxml;
    exports com.example;
}
