package org.danp.workmanagertasksequencial

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET

interface FileApi {

    // obtenemos la imagen del sitio web
    @GET("/blog/wp-content/uploads/2019/12/02182956/catarata-1.jpg")
    // descargamos la imagen
    suspend fun downloadImage(): Response<ResponseBody>

    // creamos el archivo de la imagen
    companion object {
        val instance by lazy {
            Retrofit.Builder()
                .baseUrl("https://denomades.s3.us-west-2.amazonaws.com")
                .build()
                .create(FileApi::class.java)
        }
    }
}