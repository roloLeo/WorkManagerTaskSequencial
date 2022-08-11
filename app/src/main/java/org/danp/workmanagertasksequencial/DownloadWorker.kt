package org.danp.workmanagertasksequencial

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
// descarga nuestra imagen de manera confiable
// aun si la app se cierra
class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    // metodo que realiza la descarga y nos devuelve
    // un result si la descarga fue exitosa
    override suspend fun doWork(): Result {

        delay(5000L) // 5 segundo de retrazo para observar los cambios
        val response = FileApi.instance.downloadImage() // obtenemos nuestra imagen
        response.body()?.let { body ->
            return withContext(Dispatchers.IO) {
                // guardamos imagen en el almacenamiento interno
                val file = File(context.cacheDir, "image.jpg")
                // muestra la salida de la imagen
                val outputStream = FileOutputStream(file)
                outputStream.use { stream ->
                    try {
                        stream.write(body.bytes())
                    } catch(e: IOException) {
                        return@withContext Result.failure(
                            workDataOf(
                                WorkerKeys.ERROR_MSG to e.localizedMessage
                            )
                        )
                    }
                }
                // exito de descarga de imagen y guardado
                Result.success(
                    workDataOf(
                        WorkerKeys.IMAGE_URI to file.toUri().toString()
                    )
                )
            }
        }

        // verificamos si algo salio mal
        if(!response.isSuccessful) {
            // si el error es del lado del servidor
            if(response.code().toString().startsWith("5")) {
                return Result.retry()
            }
            // si el error es del cliente
            return Result.failure(
                workDataOf(
                    WorkerKeys.ERROR_MSG to "Error de la red"
                )
            )
        }
        // en caso de errores desconocidos
        return Result.failure(
            workDataOf(WorkerKeys.ERROR_MSG to "Error desconocido")
        )
    }
}