/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.android.things.driver.hcsr04;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;

import java.io.IOException;
import java.util.UUID;

public class Hcsr04SensorDriver implements AutoCloseable {
    // DRIVER parameters
    // documented at https://source.android.com/devices/sensors/hal-interface.html#sensor_t
    private static final String DRIVER_VENDOR = "ElecFreaks";
    private static final String DRIVER_NAME = "HC-SR04";
    private static final int DRIVER_MIN_DELAY_US = Hcsr04.MEASUREMENT_INTERVAL_MS * 1000;
    private static final int DRIVER_MAX_DELAY_US = Hcsr04.MEASUREMENT_INTERVAL_MS * 1000;
    private static final float DRIVER_MAX_RANGE = Hcsr04.MAX_RANGE;
    private static final float DRIVER_POWER = Hcsr04.MAX_POWER_CONSUMPTION_UA / 1000.f;
    private static final float DRIVER_RESOLUTION = Hcsr04.ACCURACY;
    private static final int DRIVER_VERSION = 1;
    private static final String DRIVER_REQUIRED_PERMISSION = "";

    private Hcsr04 mDevice;

    private DistanceUserDriver mDistanceUserDriver;

    /**
     * Create a new framework sensor driver connected on the given bus and address.
     * The driver emits {@link android.hardware.Sensor} with proximity, angular velocity,
     * magnetic induction and temperature data when registered.
     *
     * @throws IOException
     */
    public Hcsr04SensorDriver(String trigPin, String echoPin) throws IOException {
        mDevice = new Hcsr04(trigPin, echoPin);
    }

    /**
     * Close the driver and the underlying device.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregisterProximitySensor();
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Register a {@link UserSensor} that pipes proximity readings into the Android SensorManager.
     *
     * @see #unregisterProximitySensor()
     */
    public void registerProximitySensor() {
        if (mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }

        if (mDistanceUserDriver == null) {
            mDistanceUserDriver = new DistanceUserDriver();
            UserDriverManager.getManager().registerSensor(mDistanceUserDriver.getUserSensor());
        }
    }

    /**
     * Unregister the proximity {@link UserSensor}.
     */
    public void unregisterProximitySensor() {
        if (mDistanceUserDriver != null) {
            UserDriverManager.getManager().unregisterSensor(mDistanceUserDriver.getUserSensor());
            mDistanceUserDriver = null;
        }
    }

    private class DistanceUserDriver extends UserSensorDriver {
        private UserSensor mUserSensor;

        private UserSensor getUserSensor() {
            if (mUserSensor == null) {
                mUserSensor = new UserSensor.Builder()
                        .setType(Sensor.TYPE_PROXIMITY)
                        .setName(DRIVER_NAME)
                        .setVendor(DRIVER_VENDOR)
                        .setVersion(DRIVER_VERSION)
                        .setMaxRange(DRIVER_MAX_RANGE)
                        .setResolution(DRIVER_RESOLUTION)
                        .setPower(DRIVER_POWER)
                        .setMinDelay(DRIVER_MIN_DELAY_US)
                        .setMaxDelay(DRIVER_MAX_DELAY_US)
                        .setRequiredPermission(DRIVER_REQUIRED_PERMISSION)
                        .setUuid(UUID.randomUUID())
                        .setDriver(this)
                        .build();
            }
            return mUserSensor;
        }

        @Override
        public UserSensorReading read() throws IOException {
            return new UserSensorReading(new float[]{mDevice.readDistance()});
        }

    }
}
