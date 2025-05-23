package com.example.aipulse;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aipulse.network.DiagnosisRequest;
import com.example.aipulse.network.DiagnosisResponse;
import com.example.aipulse.network.RetrofitClient;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private TextView bpmText;
    private EditText ageInput;
    private LineChart bpmChart;
    private LineDataSet bpmDataSet;
    private int timeIndex = 0;

    private EditText bpmInput;
    private Button predictButton, historyButton;
    private Interpreter tflite;
    private float lastBpm = 0f;
    private boolean sensorAvailable = false;
    private Handler simulationHandler = new Handler();
    private Runnable simulationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // ✅ placer ceci EN PREMIER

        bpmText = findViewById(R.id.bpmText);
        bpmInput = findViewById(R.id.bpmInput);
        ageInput = findViewById(R.id.ageInput);
        predictButton = findViewById(R.id.predictButton);
        historyButton = findViewById(R.id.historyButton);
        bpmChart = findViewById(R.id.bpmChart);

        bpmDataSet = new LineDataSet(new ArrayList<>(), "BPM");
        bpmDataSet.setColor(getResources().getColor(R.color.teal_700));
        bpmDataSet.setValueTextColor(getResources().getColor(android.R.color.black));
        bpmDataSet.setLineWidth(2f);
        bpmDataSet.setDrawCircles(false);

        LineData lineData = new LineData(bpmDataSet);
        bpmChart.setData(lineData);

        XAxis xAxis = bpmChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = bpmChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        bpmChart.getAxisRight().setEnabled(false);
        bpmChart.getDescription().setEnabled(false);
        bpmChart.invalidate();

        predictButton.setEnabled(false);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Toast.makeText(this, "Erreur chargement modèle", Toast.LENGTH_SHORT).show();
            return;
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor != null) {
            sensorAvailable = true;
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            bpmText.setText("Capteur indisponible. Simulation en cours...");
            startBpmSimulation();
        }

                             //  Pour prdireee//

        predictButton.setOnClickListener(v -> {
            int bpm = Integer.parseInt(bpmInput.getText().toString());
            String result = predictWithTFLite(bpm);
            boolean isRisk = result.toLowerCase().contains("détectée");

            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("result", result);
            intent.putExtra("risk", isRisk);
            startActivity(intent);

            DiagnosisRequest req = new DiagnosisRequest(bpm);
            RetrofitClient.getApiService().submitDiagnosis(req).enqueue(new Callback<DiagnosisResponse>() {
                @Override
                public void onResponse(Call<DiagnosisResponse> call, Response<DiagnosisResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Diagnostic sauvegardé", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Erreur sauvegarde", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DiagnosisResponse> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        });

        historyButton.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private String predictWithTFLite(int bpm) {
        int age = Integer.parseInt(ageInput.getText().toString());

        float[][] input = new float[1][2];
        input[0][0] = bpm;
        input[0][1] = age;

        float[][] output = new float[1][3];
        tflite.run(input, output);

        int maxIndex = 0;
        float maxValue = output[0][0];
        for (int i = 1; i < 3; i++) {
            if (output[0][i] > maxValue) {
                maxValue = output[0][i];
                maxIndex = i;
            }
        }

        switch (maxIndex) {
            case 0: return "Bradycardie détectée";
            case 1: return "Rythme normal";
            case 2: return "Tachycardie détectée";
            default: return "Erreur de prédiction";
        }
    }

    private void startBpmSimulation() {
        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                lastBpm = 60 + random.nextInt(40);
                bpmText.setText("BPM simulé : " + (int) lastBpm);
                bpmInput.setText(String.valueOf((int) lastBpm));
                predictButton.setEnabled(true);

                bpmDataSet.addEntry(new Entry(timeIndex++, lastBpm));
                bpmDataSet.notifyDataSetChanged();
                bpmChart.getData().notifyDataChanged();
                bpmChart.notifyDataSetChanged();
                bpmChart.setVisibleXRangeMaximum(10);
                bpmChart.moveViewToX(timeIndex);

                simulationHandler.postDelayed(this, 3000);
            }
        };
        simulationHandler.post(simulationRunnable);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileInputStream fis = new FileInputStream(getAssets().openFd("heart_model.tflite").getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        long startOffset = getAssets().openFd("heart_model.tflite").getStartOffset();
        long declaredLength = getAssets().openFd("heart_model.tflite").getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            lastBpm = event.values[0];
            bpmText.setText("BPM détecté : " + (int) lastBpm);
            bpmInput.setText(String.valueOf((int) lastBpm));
            predictButton.setEnabled(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorAvailable) {
            sensorManager.unregisterListener(this);
        } else {
            simulationHandler.removeCallbacks(simulationRunnable);
        }
        if (tflite != null) {
            tflite.close();
        }
    }
}
