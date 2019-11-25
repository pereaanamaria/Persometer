package com.example.persometer.steps;

public class StepDetector {

    private static final int ACCEL_RING_SIZE = 500;
    private static final int VEL_RING_SIZE = 100;

    //sensitivity of the steps
    private static final float STEP_THRESHOLD = 75f;
    private static final int STEP_DELAY_NS = 500000000;

    private int accelRingCounter = 0;
    private float[] accelRingX = new float[ACCEL_RING_SIZE];
    private float[] accelRingY = new float[ACCEL_RING_SIZE];
    private float[] accelRingZ = new float[ACCEL_RING_SIZE];
    private int velRingCounter = 0;
    private float[] velRing = new float[VEL_RING_SIZE];
    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    private StepListener listener;

    public void registerListener(StepListener listener) {
        this.listener = listener;
    }


    public void updateAccel(long timeNs, float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        // Update the guess of where the global z vector is.
        accelRingCounter++;
        accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
        accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
        accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

        float[] worldZ = new float[3];
        worldZ[0] = SensorFilter.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[1] = SensorFilter.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
        worldZ[2] = SensorFilter.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

        float normalization_factor = SensorFilter.norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        float currentZ = SensorFilter.dot(worldZ, currentAccel) - normalization_factor;
        velRingCounter++;
        velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

        //estimate speed of the accelerometer change
        float velocityEstimate = SensorFilter.sum(velRing);

        //if current speed if bigger than STEP_THRESHOLD and old speed is less or equal
        //to STEP_THRESHOLD, there has been another sensor change  ==>  another step occurred
        if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
            listener.step(timeNs);
            lastStepTimeNs = timeNs;
        }
        oldVelocityEstimate = velocityEstimate;
    }
}
