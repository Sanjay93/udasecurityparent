module imageservice {
    exports com.udacity.catpoint.service;
    requires transitive org.slf4j;
    requires transitive software.amazon.awssdk.services.rekognition;
    requires transitive software.amazon.awssdk.auth;
    requires transitive software.amazon.awssdk.regions;
    requires transitive software.amazon.awssdk.core;
    requires transitive java.desktop;
}