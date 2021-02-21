package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;


/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest {

    private static final String SENSOR = "testsensor";

    @InjectMocks
    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

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
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedAlarmActivatedSensorPendingAlarmResult(ArmingStatus armingStatus) {
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        Mockito.when(securityRepository.getSensors()).thenReturn(getSensors(true, 2));
        Mockito.when(securityService.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);
    }

    /*
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set on the alarm.
     */
    @Test
    public void armedAlarmActivatedSensorPendingAlarmAlarmResult() {
        Mockito.when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository, Mockito.times(1))
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
        Mockito.when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        Mockito.verify(securityRepository, Mockito.times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /*
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    public void activeAlarmNoAffectOnAlarmIfSensorStateChanged() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(true);
        Mockito.when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository, Mockito.times(0))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    /*
     * 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    public void activateActiveSensorWhileSystemInPendingStateAlarmResult() {
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        Mockito.when(securityRepository.getSensors()).thenReturn(getSensors(true, 2));
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        Mockito.verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    /*
     * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void deactivateInActiveSensorNoChangeInAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor(SENSOR, SensorType.DOOR);
        sensor.setActive(false);
        Mockito.when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository, Mockito.times(0))
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
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
        Mockito.when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        Mockito.verify(securityRepository, Mockito.times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
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
        Mockito.verify(securityRepository, Mockito.times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /*
     * 9. If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void systemDisarmedChangeToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository, Mockito.times(1))
                .setAlarmStatus(AlarmStatus.NO_ALARM);
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
        Mockito.when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        Mockito.verify(securityRepository, Mockito.times(1))
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void setArmingStatusMethod(ArmingStatus status) {
        securityService.setArmingStatus(status);
    }

    private Set<Sensor> getSensors(boolean active, int count) {
        String str = UUID.randomUUID().toString();

        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i <= count; i++) {
            Sensor sensor = new Sensor(str, SensorType.DOOR);
            sensors.add(sensor);
        }
        sensors.forEach(it -> it.setActive(active));
        return sensors;
    }

    @Test
    public void updateSensorWhenArmed() {
        ArmingStatus armingStatus = ArmingStatus.ARMED_HOME;
        Sensor sensor = new Sensor("test_sensor", SensorType.DOOR);
        sensor.setActive(true);
        Mockito.when(securityRepository.getSensors())
                .thenReturn(Collections.singleton(sensor));
        securityService.setArmingStatus(armingStatus);
        Mockito.verify(securityRepository, Mockito.times(1)).updateSensor(any());
    }

    @ParameterizedTest
    @CsvSource({"NO_ALARM,DOOR,true", "NO_ALARM,DOOR,false","PENDING_ALARM,DOOR,true", "PENDING_ALARM,DOOR,false",
            "PENDING_ALARM,WINDOW,true", "PENDING_ALARM,WINDOW,false"})
    public void changeSensorActivationStatusWithAllAlarms(AlarmStatus alarmStatus, SensorType sensorType,
                                                          Boolean active) {
        Mockito.when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("udacitySensor", sensorType);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, active);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, active);
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        sensor = new Sensor("udacitySensor", sensorType);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, active);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, active);
    }

    @Test
    public void testAddAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

}