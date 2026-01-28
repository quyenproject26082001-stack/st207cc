package com.pony.avatar.ocmaker.core.service
import com.pony.avatar.ocmaker.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("/api/ST215_PonyMaker2")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}