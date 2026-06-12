module debloater {
    requires javafx.controls;
    requires javafx.web;
    requires com.google.gson;
    requires jdk.jsobject;

    opens debloater to javafx.graphics;
    opens debloater.bridge to javafx.web, com.google.gson;
    opens debloater.model to com.google.gson;

    exports debloater;
    exports debloater.bridge;
    exports debloater.model;
    exports debloater.service;
}
