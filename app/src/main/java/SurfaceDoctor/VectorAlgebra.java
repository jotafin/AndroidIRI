package SurfaceDoctor;

import android.hardware.SensorManager;

public class VectorAlgebra {

    /**
     * Converte leituras do acelerômetro das coordenadas xyz do telefone para o sistema de coordenadas da Terra.
     *
     * Eixo X = leste / oeste
     * Eixo Y = pólo magnético norte / sul
     * Eixo Z = Para cima / Para baixo
     *
     * @param accelerometer float [] Uma matriz que contém os dados do acelerômetro do telefone [0:X, 1:Y, 2:Z]
     * @param magnetometer float[]
     * @param gravity float[]
     * @param sensorManager SensorManager Android SensorManager
     * @return
     */
    public static float[] earthAccelerometer(float[] accelerometer, float[] magnetometer, float[] gravity, SensorManager sensorManager) {

        float[] phoneAcceleration = new float[4];
        phoneAcceleration[0] = accelerometer[0];
        phoneAcceleration[1] = accelerometer[1];
        phoneAcceleration[2] = accelerometer[2];
        phoneAcceleration[3] = 0;

        // Altera os valores de aceleração relativa do dispositivo para valores relativos à terra
        // Eixo X -> Leste
        // Eixo Y -> Polo Norte
        // Eixo Z -> Céu

        float[] R = new float[16], I = new float[16], earthAcceleration = new float[16], earthAccelerationFinal = new float[3];

        sensorManager.getRotationMatrix(R, I, gravity, magnetometer);

        float[] inv = new float[16];

        android.opengl.Matrix.invertM(inv, 0, R, 0);
        android.opengl.Matrix.multiplyMV(earthAcceleration, 0, inv, 0, phoneAcceleration, 0);

        earthAccelerationFinal[0] = earthAcceleration[0];
        earthAccelerationFinal[1] = earthAcceleration[1];
        earthAccelerationFinal[2] = earthAcceleration[2];

        return earthAccelerationFinal;

    }


    /**
     * Retorna os valores de orientação do telefone em radianos.
     *
     * @param accelerometer
     * @param magnetometer
     * @param sensorManager
     * @return
     */
    public static float[] phoneOrientation(float[] accelerometer, float[] magnetometer, SensorManager sensorManager) {

        // Matriz flutuante vazia para manter a matriz de rotação.
        float[] rotationMatrix = new float[9];
        // Matriz de flutuação vazia para conter o Direção horizontal, a inclinação e a rotação.
        float orientationValues[] = new float[3];

        // Não sei exatamente como isso funciona, mas preenche a matriz com os dados de entrada. rotationOK retorna true se o
        // o método .getRotationMatrix foi bem-sucedido.
        // "Você pode transformar qualquer vetor do sistema de coordenadas do telefone para o sistema de coordenadas da Terra multiplicando-o pela matriz de rotação."
        boolean rotationOK = sensorManager.getRotationMatrix(rotationMatrix,
          null, accelerometer, magnetometer);

        // Se o método getRotationMatrix for bem-sucedido, execute o seguinte código,
        // TODO Do I need this at all?.
        if (rotationOK) {

            sensorManager.getOrientation(rotationMatrix, orientationValues);

        }

        return orientationValues;
    }


    /**
     * Converte os valores de orientação do telefone de radianos em graus.
     *
     * @param inputRadians float [] Uma matriz de valores de orientação.
     * @return
     */
    public static double[] radiansToDegrees(float[] inputRadians) {

        double[] outputDegrees = new double[inputRadians.length];

        for (int i=0; i < inputRadians.length; i++) {
            outputDegrees[i] = inputRadians[i] * (180/Math.PI);
        }

        return outputDegrees;
    }
}
