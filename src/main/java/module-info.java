module com.ad_passchanger.ad_passchanger {
    requires javafx.controls;
    requires javafx.fxml;
    requires MaterialFX;

    opens com.ad_passchanger.ad_passchanger to javafx.fxml;
    exports com.ad_passchanger.ad_passchanger;
}