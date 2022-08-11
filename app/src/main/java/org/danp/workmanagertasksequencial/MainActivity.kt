package org.danp.workmanagertasksequencial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.*
import coil.compose.rememberImagePainter
import org.danp.workmanagertasksequencial.ui.theme.WorkManagerTaskSequencialTheme
import java.time.Duration

// Primero se realiza los permisos
// luego la primera tarea y una vez que culmine
// se realizará la segunda tarea
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // solicitud de descarga utilizando el workManager y
        // especificamos las restricciones
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()
            )
            .build()
        // realizamos otra solicitud también para el filtro de la imagen,
        // aqui no hay restricciones
        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()
        // compilamos y obtenemos una instancia del workManager pasando el
        // contexto de nuestra aplicación. Singleton
        val workManager = WorkManager.getInstance(applicationContext)

        // Interfaz del usuario
        setContent {
            WorkManagerTaskSequencialTheme {
                // listas de informaciones de trabajo, tareas programadas
                val workInfos = workManager
                    .getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState()
                    .value
                // información sobre la descarga de la imagen
                val downloadInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == downloadRequest.id }
                }
                // información sobre el filtro de la imagen
                val filterInfo = remember(key1 = workInfos) {
                    workInfos?.find { it.id == colorFilterRequest.id }
                }
                // para obtener la imagen mediante un estado derivado
                // que almacenará en cache el resultado y se emplearan
                // efectos de controladores (effect handlers de jetpack compose)
                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)
                        ?.toUri()
                    val filterUri = filterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)
                        ?.toUri()
                    // se devuelve para poder mostrar la imagen
                    filterUri ?: downloadUri
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // si la img <> null muestra la img
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(
                                data = uri
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // cuando hagamos clic, debemos hacer que realice una tarea unica
                    // después solicitamos la solicitud de descarga de imagen
                    // luego ponemos en cola la solicitud de filtro de imagen
                    Button(
                        onClick = {
                            workManager
                                .beginUniqueWork(
                                    "download",
                                    ExistingWorkPolicy.KEEP,
                                    downloadRequest
                                )
                                .then(colorFilterRequest)
                                .enqueue()
                        },
                        // el botón se habilita si la tarea no se esta ejecutando
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Iniciar descarga")
                    }
                    // dos textos condicionales basados en el estado de nuestras tareas
                    Spacer(modifier = Modifier.height(8.dp))
                    when(downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Descargando...")
                        WorkInfo.State.SUCCEEDED -> Text("Descarga exitosa")
                        WorkInfo.State.FAILED -> Text("Descarga fallida")
                        WorkInfo.State.CANCELLED -> Text("Descarga cancelada")
                        WorkInfo.State.ENQUEUED -> Text("Descarga en cola")
                        WorkInfo.State.BLOCKED -> Text("Desacrga bloqueada")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(filterInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Aplicando filtro...")
                        WorkInfo.State.SUCCEEDED -> Text("Filtro exitoso")
                        WorkInfo.State.FAILED -> Text("Filtro fallido")
                        WorkInfo.State.CANCELLED -> Text("Filtro cancelado")
                        WorkInfo.State.ENQUEUED -> Text("Filtro en cola")
                        WorkInfo.State.BLOCKED -> Text("Filtro bloqueado")
                    }
                }
            }
        }
    }
}