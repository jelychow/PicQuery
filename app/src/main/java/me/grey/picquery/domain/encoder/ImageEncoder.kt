package me.grey.picquery.domain.encoder

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.grey.picquery.PicQueryApplication
import me.grey.picquery.common.AssetUtil
import me.grey.picquery.common.MemoryFormat
import me.grey.picquery.common.allocateFloatBuffer
import me.grey.picquery.common.bitmapToFloatBuffer
import java.nio.FloatBuffer
import java.util.Collections

private val normMeanRGB = floatArrayOf(0.48145467f, 0.4578275f, 0.40821072f)
private val normStdRGB = floatArrayOf(0.26862955f, 0.2613026f, 0.2757771f)

val IMAGE_INPUT_SIZE = Size(224, 224)

class ImageEncoder {
    companion object {
        private const val TAG = "ImageEncoder"
        private const val modelPath = "clip-image-int8.ort"
        private const val floatBufferElementCount = 3 * 224 * 224
    }

    private var ortSession: OrtSession? = null

    private var options = OrtSession.SessionOptions().apply {
        addConfigEntry("session.load_model_format", "ORT")
    }

    init {
        Log.d(TAG, "Init $TAG")
    }

    fun preprocess(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap,224,224,true)
    }


    suspend fun loadModel() {
        if (ortSession == null) {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Loading Image Encoder model...")
                val ortEnv = OrtEnvironment.getEnvironment()
                ortSession = ortEnv.createSession(
                    AssetUtil.assetFilePath(PicQueryApplication.context, modelPath),
                    options
                )
                Log.d(TAG, "Finish loading Image Encoder model!")
            }
        } else {
            Log.d(TAG, "Already loaded Image Encoder model, skip loading.")
        }
    }

    suspend fun encode(bitmap: Bitmap, usePreprocess: Boolean = true) =
        withContext<FloatBuffer>(Dispatchers.Default) {
            val ortEnv = OrtEnvironment.getEnvironment()
            if (ortSession == null) {
                loadModel()
            }
            val imageBitmap = if (usePreprocess) {
                preprocess(bitmap)
            } else {
                bitmap
            }

            val floatBuffer = allocateFloatBuffer(floatBufferElementCount)
            floatBuffer.rewind()
            bitmapToFloatBuffer(
                imageBitmap,
                0, 0,
                224, 224,
                normMeanRGB,
                normStdRGB,
                floatBuffer,
                0,
                MemoryFormat.CONTIGUOUS,
            )
            floatBuffer.rewind()

            val inputName = ortSession?.inputNames?.iterator()?.next()
            val shape: LongArray = longArrayOf(1, 3, 224, 224)
            ortEnv.use { env ->
                val tensor = OnnxTensor.createTensor(env, floatBuffer, shape)
                val output: OrtSession.Result? =
                    ortSession?.run(Collections.singletonMap(inputName, tensor))
                val resultBuffer = output?.get(0) as OnnxTensor
                return@withContext (resultBuffer.floatBuffer)
            }
        }

}