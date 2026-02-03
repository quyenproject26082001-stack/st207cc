package com.pony.avatar.ocmaker.core.service
import com.pony.avatar.ocmaker.data.model.PartAPI
import com.pony.avatar.ocmaker.data.model.StickerCategoryModel
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("/api/ST215_PonyMaker2")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>

    @GET("/api/TT999_Cat_Sticker/Sticker")
    suspend fun getStickerCategories(): Response<List<StickerCategoryModel>>
}