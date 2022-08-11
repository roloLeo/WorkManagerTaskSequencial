package org.danp.workmanagertasksequencial

import android.content.Context
import android.graphics.*
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
class ColorFilterWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    // este método tiene que obtener la imagen de la uri
    // y después aplicar el filtro y guardarla con otro nombre
    override suspend fun doWork(): Result {
        // obtenemos la imagen
        val imageFile = workerParams.inputData.getString(WorkerKeys.IMAGE_URI)
            ?.toUri()
            ?.toFile()
        delay(5000L) // retraso de 5 segundos
        // devuelve la salida de la imagen como bmp
        return imageFile?.let { file ->
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            val resultBmp = bmp.copy(bmp.config, true)
            // realizamos el filtrado de la imagen
            val paint = Paint()
            paint.colorFilter = LightingColorFilter(0x08FF04, 1)
            val canvas = Canvas(resultBmp)
            canvas.drawBitmap(resultBmp, 0f, 0f, paint)

            // guardamos la imagen filtrada en jpg
            withContext(Dispatchers.IO) {
                val resultImageFile = File(context.cacheDir, "new-image.jpg")
                val outputStream = FileOutputStream(resultImageFile)
                val successful = resultBmp.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    outputStream
                )
                // devolvemos el resultado si fue exitoso
                if(successful) {
                    Result.success(
                        workDataOf(
                            WorkerKeys.FILTER_URI to resultImageFile.toUri().toString()
                        )
                    )
                } else Result.failure()
            }
        } ?: Result.failure() // devolvemos el resultado en caso de falla
    }
}