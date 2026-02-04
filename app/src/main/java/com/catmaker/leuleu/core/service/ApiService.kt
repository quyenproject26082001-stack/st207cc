package com.catmaker.leuleu.core.service
import com.catmaker.leuleu.data.model.PartAPI
import com.catmaker.leuleu.data.model.StickerCategoryModel
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("/api/ST207_CatEmojiCatPFPMaker")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>

    @GET("/api/TT999_Cat_Sticker/Sticker")
    suspend fun getStickerCategories(): Response<List<StickerCategoryModel>>
}