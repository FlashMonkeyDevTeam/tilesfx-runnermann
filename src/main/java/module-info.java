module eu.hansolo.tilesfxrunnermann {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;


    opens eu.hansolo.tilesfx.runnermann to javafx.fxml;
    exports eu.hansolo.tilesfx.runnermann;
}