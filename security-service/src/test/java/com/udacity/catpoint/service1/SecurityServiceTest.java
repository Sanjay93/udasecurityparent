package com.udacity.catpoint.service1;


import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.FakeImageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for simple App.
 */
public class SecurityServiceTest {
    /**
     * Rigorous Test :-)
     */

    private FakeImageService imageService = new FakeImageService();
    private SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
    private SecurityService securityService = new SecurityService(securityRepository, imageService);
    private JTextField newSensorNameField = new JTextField();
    private JComboBox newSensorTypeDropdown = new JComboBox(SensorType.values());

    @Mock
    private BufferedImage currentCameraImage;
    @Mock
    private StatusListener statusListener;
    private Sensor sensor = new Sensor(newSensorNameField.getText(), SensorType.valueOf(newSensorTypeDropdown.getSelectedItem().toString()));


    @Test
    public void testArmingStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.processImage(currentCameraImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.processImage(currentCameraImage);
        assertEquals(securityService.getArmingStatus(), ArmingStatus.ARMED_HOME);
        securityService.processImage(currentCameraImage);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        assertEquals(securityService.getAlarmStatus(), AlarmStatus.NO_ALARM);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.processImage(currentCameraImage);
    }

    @Test
    public void addandRemoveSensor() {
        securityService.addSensor(sensor);
        assertNotNull(securityService.getSensors());
        securityService.removeSensor(sensor);
    }

    @Test
    public void testProcessImage() {
        securityService.processImage(currentCameraImage);
    }

    @Test
    public void testChangeSensorActivationStatus() {
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
    }

    @Test
    public void testaddStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

}
