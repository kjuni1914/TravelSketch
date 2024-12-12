package com.travelsketch.data.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ReceiptClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var isModelAvailable = false

    init {
        try {
            val assetFiles = context.assets.list("")
            if (assetFiles != null && assetFiles.contains("receipt_classifier.tflite")) {
                val model = loadModelFile()
                val options = Interpreter.Options()
                interpreter = Interpreter(model, options)
                isModelAvailable = true
                Log.d("asdfasdfasdf", "Receipt classifier model loaded successfully")
            } else {
                Log.e("asdfasdfasdf", "Model file not found in assets")
            }
        } catch (e: Exception) {
            Log.e("asdfasdfasdf", "Error initializing receipt classifier", e)
        }
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            isModelAvailable = false
        } catch (e: Exception) {
            Log.e("asdfasdfasdf", "Error closing interpreter", e)
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val modelPath = "receipt_classifier.tflite"
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classifyImage(bitmap: Bitmap): Boolean {
        try {
            Log.d("asdfasdfasdf", "Starting image classification")
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

            val outputArray = Array(1) { FloatArray(2) } // 2개의 클래스 (영수증/비영수증)
            interpreter?.run(byteBuffer, outputArray)

            Log.d("asdfasdfasdf", "Classification result - Not Receipt: ${outputArray[0][0]}, Receipt: ${outputArray[0][1]}")

            val isReceipt = outputArray[0][RECEIPT_CLASS_INDEX] > 0.5f
            Log.d("asdfasdfasdf", "Final classification result: ${if (isReceipt) "Receipt" else "Not Receipt"}")

            return isReceipt
        } catch (e: Exception) {
            Log.e("asdfasdfasdf", "Error during image classification", e)
            return false
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(BYTES_PER_CHANNEL * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = pixels[pixel++]
                byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
            }
        }

        return byteBuffer
    }


    companion object {
        private const val INPUT_SIZE = 224 // 모델 입력 크기에 맞게 조정
        private const val PIXEL_SIZE = 3 // RGB
        private const val BYTES_PER_CHANNEL = 4 // float32 사용
        private const val RECEIPT_CLASS_INDEX = 1 // 영수증 클래스의 인덱스
    }
}