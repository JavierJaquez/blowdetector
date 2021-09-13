package com. example.blower2


import android.Manifest
import android.R.attr
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.blower2.audio.features.MFCC
import com.example.blower2.audio.features.WavFileException
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import android.R.attr.button
import android.R.attr.width
import android.graphics.Color.rgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.graphics.LightingColorFilter
import android.graphics.PorterDuff


class MainActivity : AppCompatActivity(), View.OnTouchListener {
    private var startButton: Button? = null
    private var stopButton: Button? = null
    private var oneButton : Button? = null
    private var twoButton : Button? = null

    private var b1 : Button? = null
    private var b2 : Button? = null
    private var b3 : Button? = null
    private var b4 : Button? = null
    private var b5 : Button? = null
    private var b6 : Button? = null
    private var b7 : Button? = null
    private var b8: Button? = null

    private val buttonsIds = listOf(R.id.b1,R.id.b2,R.id.b3,R.id.b4,R.id.b5,R.id.b6,R.id.b7,R.id.b8)
    private val buttonsOrder = arrayOf(6,2,7,3,0,4,1,5,2,6)
    private var buttons:ArrayList<Button>? = null
    private var counter : Int = 0
    private var counter2 : Int = 0
    private val widthOrder = arrayOf(100,80,60)


    private var outputText: TextView? = null
    // Working variables.
    var recordingBuffer = ShortArray(MainActivity.RECORDING_LENGTH)
    var recordingBufferClean = ShortArray(MainActivity.RECORDING_LENGTH-960)
    var recordingOffset = 0
    var shouldContinue = true
    private var recordingThread: Thread? = null
    //private var recordingHandler: Handler? = null
    var shouldContinueRecognition = true
    private var  recognitionThread: Thread? = null
    private var mHandler :Handler? = null
    // private var recognitionHandler: Handler? = null
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


    // APP VARIABLE
    var currentButtonToPress: Int = 0
    var lastButtonPressed: Int = 0
    var pressedButton: Button? = null


    //BUTTON CONFIG
    val PURPLE= 0
    val GREEN = 1

    val buttondelay: Long= 500// in Milliseconds



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton = findViewById<View>(R.id.start) as Button?
        startButton!!.setOnClickListener {

                    startRecording()
                    startRecognition()



        }

        stopButton = findViewById<View>(R.id.stop) as Button?
        stopButton!!.setOnClickListener {

            stopRecording()
            stopRecognition()



        }

        oneButton = findViewById<View>(R.id.one) as Button
        oneButton!!.setOnTouchListener(this)
        twoButton = findViewById<View>(R.id.two) as Button
        twoButton!!.setOnTouchListener(this)

        b1 = findViewById<View>(R.id.b1) as Button
        b1!!.setOnTouchListener(this)
        b2 = findViewById<View>(R.id.b2) as Button
        b2!!.setOnTouchListener(this)
        b3 = findViewById<View>(R.id.b3) as Button
        b3!!.setOnTouchListener(this)
        b4 = findViewById<View>(R.id.b4) as Button
        b4!!.setOnTouchListener(this)
        b5 = findViewById<View>(R.id.b5) as Button
        b5!!.setOnTouchListener(this)
        b6 = findViewById<View>(R.id.b6) as Button
        b6!!.setOnTouchListener(this)
        b7 = findViewById<View>(R.id.b7) as Button
        b7!!.setOnTouchListener(this)
        b8 = findViewById<View>(R.id.b8) as Button
        b8!!.setOnTouchListener(this)




        outputText = findViewById<View>(R.id.output_text) as TextView?
        requestMicrophonePermission();

        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                // TODO Auto-generated method stub
                when (msg.what) {

                    PURPLE -> {
                        pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238))
                        //mHandler?.sendEmptyMessageDelayed(YELLOW, buttondelay)
                    }

                }
            }
        }


        //lOAD mODEL
        tfliteOptions.setNumThreads(1)
        tfliteModel = FileUtil.loadMappedFile(this, getModelPath())
        recreateInterpreter()

        imageShape = tflite!!.getInputTensor(imageTensorIndex)?.shape()
        imageDataType= tflite!!.getInputTensor(imageTensorIndex)!!.dataType()
        probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape()
        probabilityDataType= tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

        //recordingThread?.start()
        //recordingHandler = HandlerCompat.createAsync(recordingThread.looper)



        //buttons = ArrayList<Button>(buttonsIds.size)

  /*      for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
             //buttons!![i]= button

            changeButtonSize(button,50)
            //val centerPoint = getCenterPointOfView(b1!!)
            //Log.d(LOG_TAG, "view center point x,y (" + centerPoint!!.x + ", " + centerPoint!!.y + ")")
            Handler(Looper.getMainLooper()).postDelayed({
                changeButtonSize(button,150)

            }, 5000)

        }*/



    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopRecognition()



    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (view) {

           b1 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 1
                        pressedButton =b1
                        b1!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))




                        Log.d(LOG_TAG, xCoord.toString())
                        Log.d(LOG_TAG, yCoord.toString())
                        val centerPoint = getCenterPointOfView(b1!!)
                        Log.d(LOG_TAG, "view center point x,y (" + centerPoint!!.x + ", " + centerPoint!!.y + ")")
                        Log.d(LOG_TAG, b1!!.width.toString())



                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b2 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 2
                        pressedButton =b2
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }

            b3-> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 3
                        pressedButton =b3
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b4 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 4
                        pressedButton =b4
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b5 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 5
                        pressedButton =b5
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b6 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 6
                        pressedButton =b6
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b7 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 7
                        pressedButton =b7
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            b8 -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.rawX
                        var yCoord = motionEvent.rawY
                        lastButtonPressed = 8
                        pressedButton =b8
                        pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                        val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }
            oneButton -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.x
                        var yCoord = motionEvent.y
                        lastButtonPressed = 1
                        pressedButton =oneButton
                        pressedButton?.setBackgroundColor(Color.rgb(187, 134, 252))
                        Log.d(LOG_TAG, xCoord.toString())
                        Log.d(LOG_TAG, yCoord.toString())



                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }

            twoButton -> {
                when (motionEvent.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        var xCoord = motionEvent.x
                        var yCoord = motionEvent.y
                        lastButtonPressed = 2
                        pressedButton =twoButton
                        pressedButton?.setBackgroundColor(Color.rgb(187, 134, 252))
                        Log.d(LOG_TAG, xCoord.toString())
                        Log.d(LOG_TAG, yCoord.toString())



                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                    }
                }
            }







        }

        return true
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

    private fun changeButtonSize(button:Button, size:Int){
        button!!.layoutParams = LinearLayout.LayoutParams(size, size)
    }
    fun sizeChanger(size: Int){
        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button


            changeButtonSize(button,size)


        }
    }



    private fun getCenterPointOfView(view: View): Point? {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0] + view.width / 2
        val y = location[1] + view.height / 2
        return Point(x, y)
    }


    @Synchronized
    fun startRecording() {

        if (recordingThread != null) {
            return
        }
        shouldContinue = true
        recordingThread = Thread { record() }
        recordingThread!!.start()

    }

    @Synchronized
    fun stopRecording() {
        if (recordingThread == null) {
            return
        }
        shouldContinue = false
        recordingThread = null
    }

    private fun record() {

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);


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
            //val startTime = System.currentTimeMillis()

            var numberRead: Int = record.read(audioBuffer, 0, audioBuffer.size)
            //Log.v(MainActivity.LOG_TAG, "read: $numberRead")
            val maxLength = recordingBuffer.size

            var newRecordingOffset: Int = recordingOffset + numberRead;
            var secondCopyLength:Int = Math.max(0, newRecordingOffset - maxLength);
            var firstCopyLength:Int = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }


            //val finishTime = System.currentTimeMillis()

            //Log.d(LOG_TAG, "Latency = ${finishTime - startTime}ms")

            //Log.v("TAG", Arrays.toString(recordingBuffer))
        }

        record.stop()
        record.release()





    }

    @Synchronized
    fun startRecognition() {
        if (recognitionThread != null) {
            return
        }
        shouldContinueRecognition = true
        recognitionThread = Thread { recognize() }
        recognitionThread!!.start()

    }

    @Synchronized
    fun stopRecognition() {
        if (recognitionThread == null) {
            return
        }
        shouldContinueRecognition = false
        recognitionThread = null
    }

    private fun recognize() {




       // Log.v(LOG_TAG, "Start recognition")
        val inputBuffer = ShortArray(RECORDING_LENGTH)
        val doubleInputBuffer = DoubleArray(RECORDING_LENGTH)
        //val outputScores = LongArray(157)
        //val outputScoresNames = arrayOf(OUTPUT_SCORES_NAME)



        while (shouldContinueRecognition) {

            //val startTime = System.currentTimeMillis()

            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                var maxLength:Int = recordingBuffer.size;
                var firstCopyLength:Int= maxLength -recordingOffset;
                var secondCopyLength:Int = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }


            // signed 16-bit inputs.
            for (i in 0 until 1600) {
                doubleInputBuffer[i] = inputBuffer[i] / 32767.0;
            }
            // Log.v(LOG_TAG, "RECORDING Obtained")
            val result = classifyNoise(doubleInputBuffer)

            //val finishTime = System.currentTimeMillis()

            //Log.d(LOG_TAG, "Latency = ${finishTime - startTime}ms")


            Log.v(LOG_TAG, result.toString())

            if(result == "1"){
                currentButtonToPress = buttonsOrder[counter]+1
                this@MainActivity.runOnUiThread(java.lang.Runnable {
                    if(currentButtonToPress == lastButtonPressed) {
                        pressedButton?.setBackgroundColor(Color.rgb(0, 255, 0))
                        mHandler?.sendEmptyMessageDelayed(PURPLE, buttondelay);
                        lastButtonPressed = 0
                       counter += 1
                        if (counter > 9){
                            counter = 0
                            counter2+=1
                            if(counter2 > widthOrder.size){
                                counter2 = 0
                            }
                            sizeChanger(widthOrder[counter2])
                        }
                        var buttonid = buttonsOrder[counter]
                        var button = findViewById<View>(buttonsIds[buttonid]) as Button
                        Handler(Looper.getMainLooper()).postDelayed({
                            button!!.setBackgroundColor(Color.rgb(0, 0, 255))

                        }, 3000)

                    }
                })
            }

        }
    }

    fun classifyNoise ( doubleInputBuffer: DoubleArray ): String? {

      // VALUES BELOW NOT GIVEN HERE BUT IN THE MFCC LIBRARY FILE
       // val mNumFrames: Int
       // val mSampleRate: Int
       // val mChannels: Int
        var flattenMFCCValues : FloatArray = FloatArray(1)

        var predictedResult: String? = "Unknown"

       // var wavFile: WavFile? = null

        try {

           // mNumFrames = 1600
            //mSampleRate = MainActivity.SAMPLE_RATE
          //  mChannels = 1




            ///////PROBLEM CODE
            //trimming the magnitude values to 5 decimal digits
            //val df = DecimalFormat("#.#####")
            //df.roundingMode = RoundingMode.CEILING
            //val meanBuffer = DoubleArray(mNumFrames)
            //for (q in 0 until mNumFrames) {

                //meanBuffer[q] = df.format(doubleInputBuffer[q]).replace(',','.').toDouble()
            //}


           // val startTime = System.currentTimeMillis()

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



           // val finishTime = System.currentTimeMillis()

           // Log.d(LOG_TAG, "Latency = ${finishTime - startTime}ms")


        }catch (e: IOException) {
            e.printStackTrace()
        } catch (e: WavFileException) {
            e.printStackTrace()
        }

       // Log.d("TAG",Arrays.toString(flattenMFCCValues))


      //  val startTime = System.currentTimeMillis()

        predictedResult = loadModelAndMakePredictions( flattenMFCCValues)

       // val finishTime = System.currentTimeMillis()

        //Log.d(LOG_TAG, "Latency = ${finishTime - startTime}ms")




        return predictedResult
    }


    protected fun loadModelAndMakePredictions(flatMFCCValues :
                                              FloatArray) : String? {


        var predictedResult: String? = "unknown"

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
        private const val SAMPLE_DURATION_MS = 100
        private const val RECORDING_LENGTH = (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000)
        private const val MODEL_FILENAME = "file:///android_asset/q_wavenet_mobile.pb"
        private const val INPUT_DATA_NAME = "Placeholder:0"
        private const val OUTPUT_SCORES_NAME = "output"


        // UI elements.
        private const val REQUEST_RECORD_AUDIO = 13
        private val LOG_TAG = "BLOWER2"
    }
}