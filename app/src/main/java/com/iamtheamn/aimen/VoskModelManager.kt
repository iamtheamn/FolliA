package com.iamtheamn.aimen

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipInputStream

/** Langues de dictée disponibles, avec le modèle Vosk (licence Apache 2.0) correspondant. */
enum class SttLanguage(val code: String, val modelName: String, val sizeMb: Int) {
    FRENCH("fr", "vosk-model-small-fr-0.22", 41),
    ENGLISH("en", "vosk-model-small-en-us-0.15", 40);

    val downloadUrl: String get() = "https://alphacephei.com/vosk/models/$modelName.zip"

    companion object {
        /** Langue de dictée qui correspond à la langue choisie dans l'appli ("system", "fr" ou "en"). */
        fun forAppLanguage(appLanguage: String): SttLanguage {
            val code = if (appLanguage == "system") Locale.getDefault().language else appLanguage
            return if (code == FRENCH.code) FRENCH else ENGLISH
        }
    }
}

sealed interface ModelState {
    data object NotInstalled : ModelState
    /** [progress] entre 0 et 1, ou null pendant la décompression (durée inconnue). */
    data class Downloading(val progress: Float?) : ModelState
    data object Installed : ModelState
    data object Failed : ModelState
}

/**
 * Télécharge, installe et charge les modèles de reconnaissance vocale hors ligne.
 * Le téléchargement n'a lieu que sur demande explicite de l'utilisateur ; ensuite,
 * la dictée fonctionne sans aucune connexion ni service Google.
 */
object VoskModelManager {

    private const val MARKER_FILE = ".installed"
    private const val BUFFER_SIZE = 64 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<SttLanguage, Job>()

    private val _states = MutableStateFlow<Map<SttLanguage, ModelState>>(emptyMap())
    val states: StateFlow<Map<SttLanguage, ModelState>> = _states.asStateFlow()

    private val modelLock = Any()
    private var loadedModel: Pair<SttLanguage, Model>? = null

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
    }

    private fun rootDir(context: Context) = File(context.applicationContext.filesDir, "vosk")
    private fun modelDir(context: Context, language: SttLanguage) = File(rootDir(context), language.code)

    fun isInstalled(context: Context, language: SttLanguage): Boolean =
        File(modelDir(context, language), MARKER_FILE).exists()

    /** Met à jour l'état affiché d'après les fichiers présents (sans toucher aux téléchargements en cours). */
    fun refresh(context: Context) {
        _states.update { current ->
            SttLanguage.entries.associateWith { language ->
                if (jobs[language]?.isActive == true) {
                    current[language] ?: ModelState.Downloading(null)
                } else if (isInstalled(context, language)) {
                    ModelState.Installed
                } else {
                    ModelState.NotInstalled
                }
            }
        }
    }

    fun download(context: Context, language: SttLanguage) {
        val appContext = context.applicationContext
        if (jobs[language]?.isActive == true) return

        jobs[language] = scope.launch {
            setState(language, ModelState.Downloading(0f))
            val zipFile = File(appContext.cacheDir, "${language.modelName}.zip")
            val stagingDir = File(rootDir(appContext), "${language.code}.tmp")
            try {
                downloadTo(language.downloadUrl, zipFile) { progress ->
                    setState(language, ModelState.Downloading(progress))
                }

                setState(language, ModelState.Downloading(null))
                stagingDir.deleteRecursively()
                unzip(zipFile, stagingDir)
                val unpackedModel = findModelRoot(stagingDir)
                    ?: throw IOException("Archive de modèle invalide")

                val target = modelDir(appContext, language)
                releaseModel(language)
                target.deleteRecursively()
                if (!unpackedModel.renameTo(target)) throw IOException("Installation du modèle impossible")
                File(target, MARKER_FILE).writeText(language.modelName)

                setState(language, ModelState.Installed)
            } catch (e: CancellationException) {
                setState(language, ModelState.NotInstalled)
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                setState(language, ModelState.Failed)
            } finally {
                zipFile.delete()
                stagingDir.deleteRecursively()
            }
        }
    }

    fun cancelDownload(language: SttLanguage) {
        jobs[language]?.cancel()
    }

    fun delete(context: Context, language: SttLanguage) {
        jobs[language]?.cancel()
        releaseModel(language)
        modelDir(context, language).deleteRecursively()
        setState(language, ModelState.NotInstalled)
    }

    /**
     * Charge le modèle (opération lente : plusieurs secondes au premier appel).
     * À appeler hors du thread principal. Le dernier modèle chargé reste en mémoire.
     */
    fun loadModel(context: Context, language: SttLanguage): Model = synchronized(modelLock) {
        loadedModel?.let { (loadedLanguage, model) ->
            if (loadedLanguage == language) return model
            model.close()
            loadedModel = null
        }
        if (!isInstalled(context, language)) throw IOException("Modèle non installé")
        Model(modelDir(context, language).absolutePath).also { loadedModel = language to it }
    }

    private fun releaseModel(language: SttLanguage) = synchronized(modelLock) {
        if (loadedModel?.first == language) {
            loadedModel?.second?.close()
            loadedModel = null
        }
    }

    private fun setState(language: SttLanguage, state: ModelState) {
        _states.update { it + (language to state) }
    }

    private suspend fun downloadTo(url: String, destination: File, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            val totalBytes = connection.contentLengthLong
            var readBytes = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break
                        output.write(buffer, 0, count)
                        readBytes += count
                        if (totalBytes > 0) {
                            val percent = (readBytes * 100 / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun unzip(zipFile: File, destination: File) {
        val canonicalDestination = destination.canonicalPath + File.separator
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val outFile = File(destination, entry.name)
                // Protection « zip slip » : aucun fichier ne doit sortir du dossier cible
                if (!outFile.canonicalPath.startsWith(canonicalDestination)) {
                    throw IOException("Entrée d'archive invalide : ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zip.copyTo(it, BUFFER_SIZE) }
                }
                zip.closeEntry()
            }
        }
    }

    /** Les archives Vosk contiennent un dossier racine (ex. "vosk-model-small-fr-0.22/"). */
    private fun findModelRoot(dir: File): File? {
        fun isModel(candidate: File) =
            File(candidate, "am").isDirectory && File(candidate, "conf").isDirectory
        if (isModel(dir)) return dir
        return dir.listFiles()?.firstOrNull { it.isDirectory && isModel(it) }
    }
}
