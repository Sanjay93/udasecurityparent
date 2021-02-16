package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;


/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class SecurityServiceTest {

    public static final String SENSOR = "sensor";

    @Mock
    private StatusListener statusListener;

    @InjectMocks
    private SecurityService securityService;

    @Mock
    private PretendDatabaseSecurityRepositoryImpl pretendDatabaseSecurityRepository;

    @Mock
    private ImageService imageService;

    @Test
    public void addAndRemoveSensor() {
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        securityService.addSensor(sensor);
        assertNotNull(securityService.getSensors());
        securityService.removeSensor(sensor);
    }

    /*
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @Test
    public void armedAlarmActivatedSensorPendingAlarmResult() {
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(false);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
        assertNotNull(securityService.getAlarmStatus());
    }

    /*
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set on the alarm.
     */
    @Test
    public void armedAlarmActivatedSensorPendingAlarmAlarmResult() {
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void pendingAlarmWithInactiveSensorsNoAlarmResult() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(false);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    public void activeAlarmNoAffectOnAlarmIfSensorStateChanged() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(true);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(0))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    public void activateActiveSensorWhileSystemInPendingStateAlarmResult() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(true);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void deactivateInActiveSensorNoChangeInAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(false);
        Mockito.when(pretendDatabaseSecurityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(0))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 7. If the image service identifies an image containing a cat while the system is armed-home,
     * put the system into alarm status.
     */
    @Test
    public void catDetectedSystemArmedResultInAlarm() {
        BufferedImage bufferedImage = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat()))
                .thenReturn(Boolean.TRUE);
        Mockito.when(pretendDatabaseSecurityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 8. If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void catNotDetectedSensorsNotActiveChangeToAlarm() throws BackingStoreException {
        Mockito.when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat()))
                .thenReturn(Boolean.FALSE);
        BufferedImage bufferedImage = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(false);
        securityService.processImage(bufferedImage);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 9. If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void systemDisarmedChangeToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }

    /*
     * 10. If the system is armed, reset all sensors to inactive.
     */
    @Test
    public void systemArmedDeactivateAllSensors() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertTrue(securityService.getSensors().stream().allMatch(sensor1 -> Boolean.FALSE.equals(sensor1.getActive())));
    }

    /*
     * 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    public void armedHomeCatDetectedChangeToAlarm() {
        BufferedImage bufferedImage = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat()))
                .thenReturn(Boolean.TRUE);
        securityService.processImage(bufferedImage);
        Mockito.verify(pretendDatabaseSecurityRepository, Mockito.times(1))
                .setAlarmStatus(any(AlarmStatus.class));
    }
}