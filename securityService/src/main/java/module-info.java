module com.udacity.catpoint.security.securityService {
    requires transitive java.desktop;
    requires transitive miglayout.swing;
    requires transitive java.prefs;
    requires transitive gson;
    requires transitive com.google.common;
    requires transitive com.udacity.catpoint.image.imageService;
    requires transitive java.sql;
    opens com.udacity.catpoint.security.data to gson;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.application;
    exports com.udacity.catpoint.security.data;
    opens com.udacity.catpoint.security.service;
    opens com.udacity.catpoint.security.application;
}