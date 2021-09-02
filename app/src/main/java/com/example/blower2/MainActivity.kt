package com.example.blower2


import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.example.blower2.audio.features.MFCC
import com.example.blower2.audio.features.WavFile
import com.example.blower2.audio.features.WavFileException
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {
    private var startButton: Button? = null
    private var outputText: TextView? = null
    // Working variables.
    var recordingBuffer = ShortArray(MainActivity.RECORDING_LENGTH)
    var recordingBufferClean = ShortArray(MainActivity.RECORDING_LENGTH-960)
    var recordingOffset = 0
    var shouldContinue = true
    private var recordingThread: HandlerThread = HandlerThread("recordingThread")
    private var recordingHandler: Handler? = null
    var shouldContinueRecognition = true
    private var recognitionThread: HandlerThread = HandlerThread("recognitionThread")
    private var recognitionHandler: Handler? = null
    private var recordingBufferLock = ReentrantLock()
    private var tfLiteLock = ReentrantLock()

    lateinit var  tfliteModel: MappedByteBuffer
     var tflite: Interpreter? = null
    /** Options for configuring the Interpreter.  */
    val tfliteOptions: Interpreter.Options = Interpreter.Options()

    val imageTensorIndex = 0
    var imageShape : IntArray? = null
    lateinit var imageDataType: DataType
    val probabilityTensorIndex = 0
    var probabilityShape : IntArray? = null
    lateinit var probabilityDataType: DataType



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById<View>(R.id.start) as Button?
        startButton!!.setOnClickListener {

                    startRecording()



        }
        outputText = findViewById<View>(R.id.output_text) as TextView?
        requestMicrophonePermission();
        recordingThread.start()
        recordingHandler = HandlerCompat.createAsync(recordingThread.looper)

        //lOAD mODEL
        tfliteOptions.setNumThreads(1)
        tfliteModel = FileUtil.loadMappedFile(this, getModelPath())
        recreateInterpreter()

        imageShape = tflite!!.getInputTensor(imageTensorIndex)?.shape()
        imageDataType= tflite!!.getInputTensor(imageTensorIndex)!!.dataType()
        probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape()
        probabilityDataType= tflite!!.getOutputTensor(probabilityTensorIndex).dataType()


    }

    override fun onDestroy() {
        super.onDestroy()
        recordingThread.quit()
        recognitionThread.quit()

    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestMicrophonePermission() {
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),
            MainActivity.REQUEST_RECORD_AUDIO
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_RECORD_AUDIO && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }
    @Synchronized
    fun startRecording() {
        /*if (recordingThread != null) {
            return
        }*/
        shouldContinue = true


           /* recordingHandler?.postDelayed({

                val timeR = measureTimeMillis {
                    record()
                }
                Log.v(MainActivity.LOG_TAG, "This took:$timeR")


            },5)*/

        //Here Starts the code

        // Define the classification runnable
        val run = object : Runnable {
            override fun run() {
                val startTime = System.currentTimeMillis()
                record()
                val finishTime = System.currentTimeMillis()

                Log.d(LOG_TAG, "Latency = ${finishTime - startTime}ms")
                //recordingHandler?.postDelayed(this, 5)


            }
        }

// Start the classification process
        recordingHandler?.post(run)







    }

    private fun record() {
        //recordingBuffer = ShortArray(MainActivity.RECORDING_LENGTH)
        //recordingBufferClean = ShortArray(MainActivity.RECORDING_LENGTH-960)


        // Estimate the buffer size we'll need for this device.
        var bufferSize: Int = AudioRecord.getMinBufferSize(
            MainActivity.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        //Log.v(MainActivity.LOG_TAG, "Min buffer size:$bufferSize")
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = 1280
        //bufferSize = SAMPLE_RATE * 2;
        }
        var audioBuffer = ShortArray(bufferSize /4)
        var record = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            MainActivity.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(MainActivity.LOG_TAG, "Audio Record can't initialize!")
            return
        }
        record.startRecording()
        //Log.v(MainActivity.LOG_TAG, "Start recording")
        while (shouldContinue) {
            var numberRead: Int = record.read(audioBuffer, 0, audioBuffer.size)
            //Log.v(MainActivity.LOG_TAG, "read: $numberRead")
            val maxLength = recordingBuffer.size
            recordingBufferLock.lock()
            try {
                if (recordingOffset + numberRead < maxLength) {
                    System.arraycopy(audioBuffer,0, recordingBuffer, recordingOffset, numberRead)
                } else {
                    shouldContinue = false
                }
                recordingOffset += numberRead
            } finally {

                recordingBufferLock.unlock()
            }
        }
        recordingOffset =0
        recordingBufferLock.lock()


        recordingBufferClean = recordingBuffer.sliceArray(479..2078)



        recordingBufferLock.unlock()
       // Log.v("TAG",Arrays.toString(recordingBufferClean))
        record.stop()
        record.release()




        startRecognition()
    }

    @Synchronized
    fun startRecognition() {
       // Log.v(LOG_TAG, "We are recognizing your data")

            recognize()



    }

    private fun recognize() {




       // Log.v(LOG_TAG, "Start recognition")
        val inputBuffer = ShortArray(1600)
        val doubleInputBuffer = DoubleArray(1600)
        val outputScores = LongArray(157)
        val outputScoresNames = arrayOf(OUTPUT_SCORES_NAME)
        recordingBufferLock.lock()
        try {
            val maxLength = recordingBufferClean.size
            System.arraycopy(recordingBufferClean, 0, inputBuffer, 0, maxLength)
        } finally {
            recordingBufferLock.unlock()
        }



        // signed 16-bit inputs.
        for (i in 0 until 1600) {
            doubleInputBuffer[i] = inputBuffer[i]/ 32767.0;
        }
       // Log.v(LOG_TAG, "RECORDING Obtained")
        val result= classifyNoise(doubleInputBuffer)
       Log.v(LOG_TAG, result.toString())



       // this@MainActivity.runOnUiThread(java.lang.Runnable {
          //  this.output_text.text = "Predicted Noise : $result"
       // })

       // Log.v(LOG_TAG, "Clasify DONE")

    }

    fun classifyNoise ( doubleInputBuffer: DoubleArray ): String? {
        val mNumFrames: Int
        val mSampleRate: Int
        val mChannels: Int
        var flattenMFCCValues : FloatArray = FloatArray(1)

        var predictedResult: String? = "Unknown"

        var wavFile: WavFile? = null

        try {

            mNumFrames = 1600
            mSampleRate = MainActivity.SAMPLE_RATE
            mChannels = 1




            ///////PROBLEM CODE
            //trimming the magnitude values to 5 decimal digits
            //val df = DecimalFormat("#.#####")
            //df.roundingMode = RoundingMode.CEILING
            //val meanBuffer = DoubleArray(mNumFrames)



            //for (q in 0 until mNumFrames) {

                //meanBuffer[q] = df.format(doubleInputBuffer[q]).replace(',','.').toDouble()
            //}


            //MFCC java library.
            val mfccConvert = MFCC()
            //mfccConvert.setSampleRate(16000)
            val nMFCC = 20
            //mfccConvert.setN_mfcc(nMFCC)
            val mfccInput = mfccConvert.process(doubleInputBuffer)
            //predictedResult = Arrays.toString(mfccInput)
            val nFFT = mfccInput.size / nMFCC

            val mfccValues =
                Array(nMFCC) { FloatArray(nFFT) }

            //loop to convert the mfcc values into multi-dimensional array
            for (i in 0 until nFFT) {
                var indexCounter = i * nMFCC
                val rowIndexValue = i % nFFT
                for (j in 0 until nMFCC) {
                    mfccValues[j][rowIndexValue] = mfccInput[indexCounter].toFloat()
                    indexCounter++
                }
            }

            //code to take the mean of mfcc values across the rows such that
            //[nMFCC x nFFT] matrix would be converted into
            //[nMFCC xnFFT x 1] dimension - which would act as an input to tflite model
            flattenMFCCValues = FloatArray(nMFCC*nFFT)
            var count:Int = 0
            for (p in 0 until nMFCC) {

                for (q in 0 until nFFT) {
                    flattenMFCCValues[count] = mfccValues[p][q]
                    if(count < nMFCC*nFFT -1) {
                        count+=1
                    }


                }


            }




        }catch (e: IOException) {
            e.printStackTrace()
        } catch (e: WavFileException) {
            e.printStackTrace()
        }

       // Log.d("TAG",Arrays.toString(flattenMFCCValues))



        predictedResult = loadModelAndMakePredictions( flattenMFCCValues)



        return predictedResult
    }


    protected fun loadModelAndMakePredictions(flatMFCCValues :
                                              FloatArray) : String? {


        var predictedResult: String? = "unknown"
            /*
        //load the TFLite model in 'MappedByteBuffer' format using TF Interpreter
        val tfliteModel: MappedByteBuffer =
            FileUtil.loadMappedFile(this, getModelPath())
        val tflite: Interpreter
        /** Options for configuring the Interpreter.  */
        val tfliteOptions: Interpreter.Options =
            Interpreter.Options()
        tfliteOptions.setNumThreads(2)
        tflite = Interpreter(tfliteModel, tfliteOptions)
*/
        //obtain the input and output tensor size required by the model
        //for urban sound classification, input tensor should be of 1x40x1x1 shape



      /*  val imageTensorIndex = 0
        val imageShape = tflite!!.getInputTensor(imageTensorIndex)?.shape()
        val imageDataType: DataType = tflite!!.getInputTensor(imageTensorIndex)!!.dataType()
        val probabilityTensorIndex = 0
        val probabilityShape =
            tflite!!.getOutputTensor(probabilityTensorIndex).shape()
        val probabilityDataType: DataType =
            tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

*/
        //need to transform the MFCC 1d float buffer into 1x40x1x1 dimension tensor using TensorBuffer
        val inBuffer: TensorBuffer = TensorBuffer.createDynamic(imageDataType)
        if (imageShape != null) {
            inBuffer.loadArray(flatMFCCValues, imageShape!!)
        }
        val inpBuffer: ByteBuffer = inBuffer.getBuffer()
        val outputTensorBuffer: TensorBuffer =
            TensorBuffer.createFixedSize(probabilityShape!!, probabilityDataType)
        //run the predictions with input and output buffer tensors to get probability values across the labels
        tflite!!.run(inpBuffer, outputTensorBuffer.getBuffer())

        //Log.d("TAG",getMaxValue(outputTensorBuffer.floatArray ).toString() )

/*
        //Code to transform the probability predictions into label values
        val ASSOCIATED_AXIS_LABELS = "labels.txt"
        var associatedAxisLabels: List<String?>? = null
        try {
            associatedAxisLabels = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS)
        } catch (e: IOException) {
            Log.e("tfliteSupport", "Error reading label file", e)
        }

        //Tensor processor for processing the probability values and to sort them based on the descending order of probabilities
        val probabilityProcessor: TensorProcessor = TensorProcessor.Builder()
            .add(NormalizeOp(0.0f, 255.0f)).build()
        if (null != associatedAxisLabels) {
            // Map of labels and their corresponding probability
            val labels = TensorLabel(
                associatedAxisLabels,
                probabilityProcessor.process(outputTensorBuffer)
            )

            // Create a map to access the result based on label
            val floatMap: Map<String, Float> =
                labels.getMapWithFloatValue()

            //function to retrieve the top K probability values, in this case 'k' value is 1.
            //retrieved values are storied in 'Recognition' object with label details.
            val resultPrediction: List<Recognition>? = getTopKProbability(floatMap);
*/
            //get the top 1 prediction from the retrieved list of top predictions
            predictedResult = getMaxValue(outputTensorBuffer.floatArray ).toString()

      //  }




        return predictedResult
    }

    private fun recreateInterpreter() {
        tfLiteLock.lock()
        try {
            if (tflite != null) {
                tflite!!.close()
                tflite = null
            }
            tflite = Interpreter(tfliteModel, tfliteOptions)

        } finally {
            tfLiteLock.unlock()
        }
    }


    fun getMaxValue(floatArray: FloatArray): Int{
        var index: Int=0
        if(floatArray[1]>floatArray[0]){
            index = 1
        }
        else{
            index = 0
        }
        return index
    }

    fun getModelPath(): String {
        // you can download this file from
        // see build.gradle for where to obtain this file. It should be auto
        // downloaded into assets.
        return "model.tflite"
    }
    /** Gets the top-k results.  */


    companion object {
        // Constants that control the behavior of the recognition code and model
        // settings.
        private const val SAMPLE_RATE = 16000
        private const val SAMPLE_DURATION_MS = 160
        private const val RECORDING_LENGTH = (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000)
        private const val MODEL_FILENAME = "file:///android_asset/q_wavenet_mobile.pb"
        private const val INPUT_DATA_NAME = "Placeholder:0"
        private const val OUTPUT_SCORES_NAME = "output"


        // UI elements.
        private const val REQUEST_RECORD_AUDIO = 13
        private val LOG_TAG = "BLOWER2"
    }
}