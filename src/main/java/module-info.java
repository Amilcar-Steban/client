module com.sockets {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.sockets to javafx.fxml;
    exports com.sockets;
}
