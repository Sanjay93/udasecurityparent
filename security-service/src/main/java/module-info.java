module securityservice {
    exports com.udacity.catpoint.service1;
    exports com.udacity.catpoint.application;
    exports com.udacity.catpoint.data;
    opens com.udacity.catpoint.service1;
    requires transitive java.desktop;
    requires transitive miglayout.swing;
    requires transitive imageservice;
    requires transitive java.prefs;
    requires transitive guava;
    requires transitive gson;
}