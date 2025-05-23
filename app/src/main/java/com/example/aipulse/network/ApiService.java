package com.example.aipulse.network;

import com.example.aipulse.network.DiagnosisRequest;
import com.example.aipulse.network.DiagnosisResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/diagnosis")
    Call<DiagnosisResponse> submitDiagnosis(@Body DiagnosisRequest request);
}
