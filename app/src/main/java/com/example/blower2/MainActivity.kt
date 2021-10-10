package com. example.blower2


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
import android.content.Context
import android.media.*
import android.view.inputmethod.InputMethodManager
import com.opencsv.CSVWriter
import java.io.FileWriter
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
import android.widget.EditText





class MainActivity : AppCompatActivity(), View.OnTouchListener {
    private var startButton: Button? = null
    private var idButton: Button? = null
   // private var stopButton: Button? = null
    private var oneButton : Button? = null
    private var continueButton : Button? = null

    private var b1 : Button? = null
    private var b2 : Button? = null
    private var b3 : Button? = null
    private var b4 : Button? = null
    private var b5 : Button? = null
    private var b6 : Button? = null
    private var b7 : Button? = null
    private var b8: Button? = null

    private val buttonsIds = listOf(R.id.b1,R.id.b2,R.id.b3,R.id.b4,R.id.b5,R.id.b6,R.id.b7,R.id.b8)
    private val buttonsOrder = arrayOf(6,2,7,3,0,4,1,5,2,6,2,7,3,0,4,1,5,2,6)
    private val modeList1 = arrayOf(1,2,3,4)
    private val modeList2 = arrayOf(2,1,4,3)
    private val testMode1 = arrayOf(3,4,1,2)
    private val testMode2 = arrayOf(4,3,1,2)
    private var selectedModeList = arrayOf(0,0,0,0)
    //private var buttons:ArrayList<Button>? = null
    private var modeCounter: Int = 0
    private var practiceCounter : Int = 0
    private var counter : Int = 0 // for buttons
    private var counter2 : Int = 0 // for changing width
    private var endOfSection: Int = 0
    private var endOfMode: Int = 0
    private val widthOrder = arrayOf(100,80,60,25)


    private var textView: TextView? = null
    private var modeTextView: TextView? = null
    // Working variables.
    var recordingBuffer = ShortArray(MainActivity.RECORDING_LENGTH)
    //   var recordingBufferClean = ShortArray(MainActivity.RECORDING_LENGTH-960)
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


    // APP VARIABLES
    private var currentButtonToPress: Int = 7
    private var lastButtonPressed: Int = 0
    var pressedButton: Button? = null
    var waitForConfirmation:Int = 0
    var waitForNextBlow:Int = 0
    var endOfTest:Int = 0
    private val sizeList1: MutableList<String> = ArrayList()
    private val centerListX: MutableList<String> = ArrayList()
    private val centerListY: MutableList<String> = ArrayList()
    private val centerPointsX: MutableList<Array<String>> = ArrayList()//Mode1
    private val centerPointsY: MutableList<Array<String>> = ArrayList()
    private val centerPointsX2: MutableList<Array<String>> = ArrayList()
    private val centerPointsY2: MutableList<Array<String>> = ArrayList()//Mode2
    private val centerPointsX3: MutableList<String> = ArrayList()
    private val centerPointsY3: MutableList<String> = ArrayList()//Mode1
    private val centerPointsX4: MutableList<String> = ArrayList()
    private val centerPointsY4: MutableList<String> = ArrayList()//Mode2

    private val clicksX: MutableList<String> = ArrayList() //MODE1
    private val clicksY: MutableList<String> = ArrayList()
    private val clicksX2: MutableList<String> = ArrayList() //MODE2
    private val clicksY2: MutableList<String> = ArrayList()
    private val centerDistList: MutableList<String> = ArrayList()//Mode1
    private val centerDistList2: MutableList<String> = ArrayList()//Mode2
    private val reactionTime1: MutableList<String> = ArrayList()//Mode1
    private val reactionTime2: MutableList<String> = ArrayList()//Mode2

    private val pressedButtonList: MutableList<String> = ArrayList() //Mode1
    private val pressedButtonList2: MutableList<String> = ArrayList()
    private val timeList: MutableList<String> = ArrayList() //MODE1
    private val timeList2: MutableList<String> = ArrayList() //MODE2
    private val euclidianDistList: MutableList<String> = ArrayList()
    private val iDe: MutableList<String> = ArrayList()
    private val iDe2: MutableList<String> = ArrayList()
    private var startTime: Long = 0
    private val durationInMilliSeconds: Long = 100 //Vibration Duration
    private lateinit var soundPool: SoundPool
    private  var sound:Int = 0
    private var stdWidths = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    private var stdWidths2 = doubleArrayOf(0.0, 0.0, 0.0, 0.0)


    //BUTTON CONFIG
    val PURPLE= 0
    val GREEN = 1
    val TURQUOISE = 2
    val ALL = 3

    //APP STATE
    val MODE0 = 0 //Practice Mode
    val MODE1 = 1 // Blow and click TP
    val MODE2 = 2
    val MODE3 = 3
    val MODE4 = 4
    var STATE = 0

    val buttondelay: Long= 400// in Milliseconds



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //This needs to be replace with selector using user Id
        //selectedModeList = testMode1
        //////////////////////////////////

        STATE = MODE0
        startRecording()
        startRecognition()
        val idText = findViewById<View>(R.id.editText) as EditText
        val circleLayout = findViewById<View>(R.id.CircleLayout) as LinearLayout


        //Id Button
        idButton = findViewById<View>(R.id.buttonId) as Button?
        idButton!!.setOnClickListener {
            var strValue = idText.text.toString()
            if(strValue[strValue.length-2].toString() == "0" && strValue[strValue.length-1].toString() == "1"  ){
                selectedModeList = modeList1
                modeTextView!!.text = "Mode 1 selected"
                startButton!!.isEnabled = true
            } else if (strValue[strValue.length-2].toString() == "0" && strValue[strValue.length-1].toString() == "2" ){
                selectedModeList = modeList2
                startButton!!.isEnabled = true
                modeTextView!!.text = "Mode 2 selected"
            }else{
                    modeTextView!!.text = "Invalid Input"
            }


            Log.d("LOG_TAG",strValue[strValue.length-1].toString())
            Log.d("LOG_TAG",strValue[strValue.length-2].toString())
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            }
            idText.clearFocus()

        }


        //Start Button
        startButton = findViewById<View>(R.id.start) as Button?
        startButton!!.setOnClickListener {
            STATE = selectedModeList[modeCounter]
            currentButtonToPress = 7
            lastButtonPressed = 0
            sizeChanger(widthOrder[0])
            startButton!!.isEnabled = false
            mHandler?.sendEmptyMessageDelayed(ALL, 0)
           // startTime = System.currentTimeMillis()
            when(STATE){
                1 -> {modeTextView!!.text = "Mode Click and Blow"}
                2 -> {modeTextView!!.text = "Mode Only Click"}
                3 -> {modeTextView!!.text = "Reaction Only Blow"}
                4-> {modeTextView!!.text = "Reaction Only Click"}

            }
        }

      //  stopButton = findViewById<View>(R.id.stop) as Button?
        /*
        stopButton!!.setOnClickListener {

            stopRecording()
            stopRecognition()



        } */
        //BUTTON TO SEND DATA TO CSV FILE
        oneButton = findViewById<View>(R.id.one) as Button
        oneButton!!.isEnabled = false
        oneButton!!.setOnClickListener {

            stopRecording()
            stopRecognition()

            //Calculating the Euclidean distance from the center point
            var tpCounter = 0
            for( condition in 0 until centerPointsX.size){
                for (event in buttonsOrder.indices){
                    var pressedId = pressedButtonList[tpCounter].toInt()
                    var centerX = centerPointsX[condition][pressedId-1]
                    var centerY = centerPointsY[condition][pressedId-1]
                    var euclideanDistance = sqrt((centerX.toFloat() - clicksX[tpCounter].toFloat()).pow(2) +(centerY.toFloat() - clicksY[tpCounter].toFloat()).pow(2))
                    euclidianDistList.add(euclideanDistance.toString())
                    if(tpCounter < pressedButtonList.size -1) {
                        tpCounter+=1
                    }
                }
            }
            // Calculating Throughput
                //Mode1
             var euclideanList  = DoubleArray(centerDistList.size)
            var countdist = 0
            for (distance in centerDistList){
                euclideanList[countdist]= distance.toDouble()
                countdist += 1
            }
            //Mode 2
            var euclideanList2  = DoubleArray(centerDistList2.size)
            var countdist2 = 0
            for (distance in centerDistList2){
                euclideanList2[countdist2]= distance.toDouble()
                countdist2 += 1
            }

            //Log.d(LOG_TAG, euclideanList.size.toString())
            //Log.d(LOG_TAG, centerPointsX.size.toString())

            for( condition in 0 until widthOrder.size){
                var radialDist = 0.0F
                var radialDist2 = 0.0F
               // Compensate for different number of condition
                if(condition == 0){
                    //Mode1
                    var centerX = centerPointsX3[0]
                    var centerY = centerPointsY3[0]
                    var centerX2 = centerPointsX3[1]
                    var centerY2 = centerPointsY3[1]
                     radialDist = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths[condition] = calculateSD(euclideanList.slice(0..18))*4.133
                    //Mode2
                    centerX = centerPointsX4[0]
                    centerY = centerPointsY4[0]
                    centerX2 = centerPointsX4[1]
                    centerY2 = centerPointsY4[1]
                    radialDist2 = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths2[condition] = calculateSD(euclideanList2.slice(0..18))*4.133
                }else if (condition ==1){
                    var centerX = centerPointsX3[19]
                    var centerY = centerPointsY3[19]
                    var centerX2 = centerPointsX3[20]
                    var centerY2 = centerPointsY3[20]
                    radialDist = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths[condition] = calculateSD(euclideanList.slice(19..37))*4.133
                    //Mode2
                    centerX = centerPointsX4[19]
                    centerY = centerPointsY4[19]
                    centerX2 = centerPointsX4[20]
                    centerY2 = centerPointsY4[20]
                    radialDist2 = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths2[condition] = calculateSD(euclideanList2.slice(19..37))*4.133
                } else if (condition ==2){
                    var centerX = centerPointsX3[38]
                    var centerY = centerPointsY3[38]
                    var centerX2 = centerPointsX3[39]
                    var centerY2 = centerPointsY3[39]
                    radialDist = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths[condition] = calculateSD(euclideanList.slice(38..56))*4.133
                    //Mode2
                    centerX = centerPointsX4[38]
                    centerY = centerPointsY4[38]
                    centerX2 = centerPointsX4[39]
                    centerY2 = centerPointsY4[39]
                    radialDist2 = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths2[condition] = calculateSD(euclideanList2.slice(38..56))*4.133
                }else if (condition ==3){
                    var centerX = centerPointsX3[57]
                    var centerY = centerPointsY3[57]
                    var centerX2 = centerPointsX3[58]
                    var centerY2 = centerPointsY3[58]
                    radialDist = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths[condition] = calculateSD(euclideanList.slice(57 until euclideanList.size))*4.133
                    //Mode2
                    centerX = centerPointsX4[57]
                    centerY = centerPointsY4[57]
                    centerX2 = centerPointsX4[58]
                    centerY2 = centerPointsY4[58]
                    radialDist2 = sqrt((centerX.toFloat() - centerX2.toFloat()).pow(2) +(centerY.toFloat() - centerY2.toFloat().toFloat()).pow(2))
                    stdWidths2[condition] = calculateSD(euclideanList2.slice(57 until euclideanList.size))*4.133
        }
                if(condition <= stdWidths.size -1){
                    iDe.add(log2(radialDist/stdWidths[condition]+1).toString())
                    iDe2.add(log2(radialDist2/stdWidths2[condition]+1).toString())
                }


            }
            var TP = DoubleArray(timeList.size)
            var counterTP = 0
            for(time in timeList){
                if(counterTP in 0..18 ){
                    TP[counterTP] = (iDe[0].toDouble()/timeList[counterTP].toDouble())*1000
                }else if (counterTP in 19..37){
                    TP[counterTP] = (iDe[1].toDouble()/timeList[counterTP].toDouble())*1000

                }else if (counterTP in 38..56){
                    TP[counterTP] = (iDe[2].toDouble()/timeList[counterTP].toDouble())*1000

                }else if (counterTP in 57 until timeList.size){
                    TP[counterTP] = (iDe[3].toDouble()/timeList[counterTP].toDouble())*1000

                }

                counterTP += 1



            }
            //Mode2
            var TP2 = DoubleArray(timeList2.size)
            var counterTP2 = 0
            for(time in timeList2){
                if(counterTP2 in 0..18 ){
                    TP2[counterTP2] = (iDe2[0].toDouble()/timeList2[counterTP2].toDouble())*1000
                }else if (counterTP2 in 19..37){
                    TP2[counterTP2] = (iDe2[1].toDouble()/timeList2[counterTP2].toDouble())*1000

                }else if (counterTP2 in 38..56){
                    TP2[counterTP2] = (iDe2[2].toDouble()/timeList2[counterTP2].toDouble())*1000

                }else if (counterTP2 in 57 until timeList2.size){
                    TP2[counterTP2] = (iDe2[3].toDouble()/timeList2[counterTP2].toDouble())*1000

                }

                counterTP2 += 1



            }

            var reactionTime1Float = FloatArray(reactionTime1.size)
            var reactionTime2Float = FloatArray(reactionTime2.size)
            for(time in reactionTime1Float.indices){
                reactionTime1Float[time] = reactionTime1[time].toFloat()
            }
            for(time in reactionTime2Float.indices){
                reactionTime2Float[time] = reactionTime2[time].toFloat()
            }
            var averageReactionTime1 = reactionTime1Float.average()
            var averageReactionTime2 = reactionTime2Float.average()

            // Sending data to csv file
            var averageTp = arrayOf(TP.average().toString())
            var averageTp2 = arrayOf(TP2.average().toString())
            val csv: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            Log.d(LOG_TAG, csv)
            val data: MutableList<Array<String>> = ArrayList()
            data.add(sizeList1.toTypedArray())
           // data.add(centerListX.toTypedArray())
           // data.add(centerListY.toTypedArray())
          /*  for(i in 0 until centerPointsX.size ){
                data.add(centerPointsX[i])
                data.add(centerPointsY[i])

            } */
            data.add(clicksX.toTypedArray())
            data.add(clicksY.toTypedArray())
            data.add(pressedButtonList.toTypedArray())
            data.add(euclidianDistList.toTypedArray())
            data.add(timeList.toTypedArray())
            data.add(centerDistList.toTypedArray())
            data.add(centerPointsX3.toTypedArray())
            data.add(centerPointsY3.toTypedArray())
            data.add(averageTp)

            //Mode2
            data.add(clicksX2.toTypedArray())
            data.add(clicksY2.toTypedArray())
           // data.add(pressedButtonList2.toTypedArray())
            data.add(timeList2.toTypedArray())
            data.add(centerDistList2.toTypedArray())
            data.add(centerPointsX4.toTypedArray())
            data.add(centerPointsY4.toTypedArray())
            data.add(averageTp2)

            //Mode 3
            data.add(reactionTime1.toTypedArray())
            data.add(arrayOf((averageReactionTime1/1000).toString()))

            data.add(reactionTime2.toTypedArray())
            data.add(arrayOf((averageReactionTime2/1000).toString()))

            var fileName = idText.text.toString()

            val writer = CSVWriter(FileWriter(csv + "/"+fileName+".csv"))
            writer.writeAll(data)
            //writer.writeNext(sizeList1.toTypedArray())

            writer.close()
            modeTextView!!.text ="Data Saved"
            textView!!.text = "Check Downloads"

        }

        //CONTINUE BUTTON
        continueButton = findViewById<View>(R.id.two) as Button
        continueButton!!.setOnClickListener {
            endOfSection = 0
            waitForNextBlow = 0
            //enableAllButtons()
             b7!!.isEnabled = true
            continueButton!!.isEnabled = false

            when(STATE){
                MODE1 -> {
                    textView!!.text = "Click and Blow"
                    modeTextView!!.text = "Mode Click and Blow"
                }
                MODE2 -> {
                    textView!!.text = "Click "
                    modeTextView!!.text = "Mode Only Click"
                }
                MODE3 -> {
                    textView!!.text = "Blow "
                    modeTextView!!.text = "Reaction Only Blow"
                }
                MODE4 -> {
                    textView!!.text = "Click "
                    modeTextView!!.text = "Reaction Only Click"
                }
            }
            if(endOfMode == 1 ){
                counter = 0
                counter2 = 0
                currentButtonToPress = 7
                lastButtonPressed = 0
                sizeChanger(widthOrder[counter2])
                endOfMode = 0
            }

            b7!!.setBackgroundColor(Color.rgb(3, 244, 252))
            startTime = System.currentTimeMillis()



        }

        //APPLICATION BUTTONS
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




        textView = findViewById<View>(R.id.textView) as TextView?
        modeTextView = findViewById<View>(R.id.modeTextView) as TextView?


        // Request Microphone and Writing Permissions from User
        requestMicrophonePermission();


       //Handler to change color back to Beginning Color
        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                // TODO Auto-generated method stub
                when (msg.what) {

                    PURPLE -> {
                        pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238))
                        //mHandler?.sendEmptyMessageDelayed(YELLOW, buttondelay)
                    }
                    TURQUOISE ->{
                        b7!!.setBackgroundColor(
                            Color.rgb(
                                3,
                                244,
                                252
                            ))
                        startTime = System.currentTimeMillis()

                    }
                    ALL -> {
                        for (id in buttonsIds) {
                            val button = findViewById<View>(id) as Button
                            button.setBackgroundColor(Color.rgb(98, 0, 238))
                        }
                        mHandler?.sendEmptyMessageDelayed(TURQUOISE, 100)

                    }

                }
            }
        }


        //Load model
        tfliteOptions.setNumThreads(1)
        tfliteModel = FileUtil.loadMappedFile(this, getModelPath())
        recreateInterpreter()

        imageShape = tflite!!.getInputTensor(imageTensorIndex)?.shape()
        imageDataType= tflite!!.getInputTensor(imageTensorIndex)!!.dataType()
        probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape()
        probabilityDataType= tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

        //recordingThread?.start()
        //recordingHandler = HandlerCompat.createAsync(recordingThread.looper)

        // Putting all button widths into an array converted to pixels
        for (width in widthOrder){
            val buttonSize = convertDpToPx(this,width.toFloat())
            sizeList1.add(buttonSize.toString())
        }

        //Depending on Android Version Sounds are produced differently
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            var audioAttributes:AudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
                 soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()
             sound = soundPool.load(this,R.raw.sound2,1 )
        }else
        {
            var soundPool = SoundPool(2, AudioManager.STREAM_MUSIC,0);
             sound = soundPool.load(this,R.raw.sound2,1 )

        }



        //val csv: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath




    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopRecognition()



    }

    override fun onStart() {
        super.onStart()
        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
            //val buttonSize = convertDpToPx(this,25.0.toFloat())
            val centerPoint = getCenterPointOfView(button!!)
            centerListX.add(centerPoint?.x.toString())
            centerListY.add(centerPoint?.y.toString())
        }
        b7!!.setBackgroundColor(
            Color.rgb(
                3,
                244,
                252
            ))
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }

                                }
                            }

                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }


                        }





                       // Log.d(LOG_TAG, xCoord.toString())
                       // Log.d(LOG_TAG, yCoord.toString())
                        //val centerPoint = getCenterPointOfView(b1!!)
                        //Log.d(LOG_TAG, "view center point x,y (" + centerPoint!!.x + ", " + centerPoint!!.y + ")")
                        //Log.d(LOG_TAG, b1!!.width.toString())



                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }

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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }


                        //val centerPoint = getCenterPointOfView(pressedButton!!)

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }

                        }


                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))


                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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
                        var centerButtonPoints = getCenterPointOfView(pressedButton!!)
                        var centerDistance = sqrt((centerButtonPoints!!.x - xCoord).pow(2) +(centerButtonPoints!!.y - yCoord).pow(2))

                        when(STATE){
                            MODE0->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    vibrator.vibrate(durationInMilliSeconds)
                                }
                            }
                            MODE1->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.parseColor("#FFBB86FC"))
                                    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if(waitForConfirmation == 0){
                                        waitForConfirmation = 1
                                        vibrator.vibrate(durationInMilliSeconds)
                                        clicksX.add(xCoord.toString())
                                        clicksY.add(yCoord.toString())
                                        centerPointsX3.add(centerButtonPoints!!.x.toString())
                                        centerPointsY3.add(centerButtonPoints!!.y.toString())
                                        centerDistList.add(centerDistance.toString())
                                        pressedButtonList.add(lastButtonPressed.toString())
                                    }
                                }
                            }
                            MODE2 -> {
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    timeList2.add((stopTime - startTime).toString())
                                    clicksX2.add(xCoord.toString())
                                    clicksY2.add(yCoord.toString())
                                    centerPointsX4.add(centerButtonPoints!!.x.toString())
                                    centerPointsY4.add(centerButtonPoints!!.y.toString())
                                    centerDistList2.add(centerDistance.toString())

                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                }

                            }
                            MODE3->{}
                            MODE4 ->{
                                currentButtonToPress = buttonsOrder[counter] + 1
                                if (currentButtonToPress == lastButtonPressed) {
                                    val stopTime = System.currentTimeMillis()
                                    soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                    pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                    reactionTime2.add((stopTime - startTime).toString())
                                    //Button Loop Logic
                                    buttonLoopLogic()
                                    lastButtonPressed = 0
                                    currentButtonToPress = buttonsOrder[counter] + 1
                                    //Change the color of the next button to press
                                    var buttonid = buttonsOrder[counter]
                                    var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                    if (endOfSection == 0) {
                                        Handler(Looper.getMainLooper()).postDelayed(
                                            {
                                                Log.d("LOG_TAG", "THIS IS EXECUTED")
                                                button!!.setBackgroundColor(
                                                    Color.rgb(
                                                        3,
                                                        244,
                                                        252
                                                    )
                                                ) //Turquoise
                                                startTime = System.currentTimeMillis()

                                            }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                    } else {
                                        button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                    }
                                }
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        when(STATE){
                            MODE0,MODE1 ->{
                                if(currentButtonToPress == lastButtonPressed){
                                    pressedButton!!.setBackgroundColor(Color.rgb(3, 244, 252))
                                    lastButtonPressed = 0
                                }
                            }
                        }
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.REQUEST_RECORD_AUDIO && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),MainActivity.REQUEST_WRITE_EXTERNAL )
        }
        if (requestCode == MainActivity.REQUEST_WRITE_EXTERNAL && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }
    private fun disableAllButtons(){
        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
            button!!.isEnabled = false

        }
    }
    private fun enableAllButtons(){
        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
            button!!.isEnabled = true

        }
    }
    private fun changeButtonSize(button:Button, size:Int){
        button!!.layoutParams = LinearLayout.LayoutParams(size, size)
    }
    private fun calculateSD(numArray: List<Double>): Double {
        var sum = 0.0
        var standardDeviation = 0.0

        for (num in numArray) {
            sum += num
        }

        val mean = sum / 10

        for (num in numArray) {
            standardDeviation += Math.pow(num - mean, 2.0)
        }

        return Math.sqrt(standardDeviation / 10)
    }
    private fun sizeChanger(size: Int){
        val cPointsX: MutableList<String> = ArrayList()
        val cPointsY: MutableList<String> = ArrayList()
        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
            var width = convertDpToPx(this,size.toFloat())
            changeButtonSize(button,width.toInt())}

        for (id in buttonsIds) {
            val button = findViewById<View>(id) as Button
            val centerPoint = getCenterPointOfView(button!!)
            cPointsX.add(centerPoint?.x.toString())
            cPointsY.add(centerPoint?.y.toString())
        }



        when(STATE){
            MODE1 ->{
                centerPointsX.add(cPointsX.toTypedArray())
                centerPointsY.add(cPointsY.toTypedArray())
            }
            MODE2 ->{
                centerPointsX2.add(cPointsX.toTypedArray())
                centerPointsY2.add(cPointsY.toTypedArray())
            }
        }

    }
    private fun buttonLoopLogic(){
        counter += 1
        if (counter > buttonsOrder.size - 1) {
            counter = 0
            endOfSection = 1
            counter2 += 1
            if (counter2 > widthOrder.size - 1) {
                counter2 = 0
                if(modeCounter>= selectedModeList.size-1){
                    endOfTest = 1
                    oneButton!!.isEnabled = true
                    textView!!.text = "Press to save Data"
                    modeTextView!!.text = "End Of Experiment"


                }else{
                    modeCounter+=1
                }

                endOfMode = 1
                STATE = selectedModeList[modeCounter]
                currentButtonToPress = 7
                lastButtonPressed = 0
                //Log.d(LOG_TAG, endOfSection.toString())
            }
            // Depends On Mode
            if (endOfMode!=1){
            sizeChanger(widthOrder[counter2])
            }
            b7!!.isEnabled = false
            if(endOfTest!= 1){
                continueButton!!.isEnabled = true
                textView!!.text = "When ready press continue"
            }


        }
        //Change Next Button

        if(STATE != MODE3 && STATE!= MODE4) {
            var buttonid = buttonsOrder[counter]
            var button = findViewById<View>(buttonsIds[buttonid]) as Button
            if (endOfSection == 0) {
                button!!.setBackgroundColor(
                    Color.rgb(
                        3,
                        244,
                        252
                    )
                ) //Turquoise
                startTime = System.currentTimeMillis()

            } else {
                button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
            }
        }

    }

    private fun convertDpToPx(context: Context, dp: Float): Float {
        return dp * context.getResources().getDisplayMetrics().density
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


            //Log.v(LOG_TAG, result.toString())

            //Behaviour of Thread depends on Mode
            // 1 indicates a blow has been detected
            if(result == "1"){

                when(STATE) {

                    //MODE 0 PRACTICE MODE

                    MODE0 -> {
                        currentButtonToPress = buttonsOrder[practiceCounter] + 1 // +1 to reflect buttons real value
                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            if (currentButtonToPress == lastButtonPressed) {
                                soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                lastButtonPressed = 0
                                practiceCounter += 1
                                if (practiceCounter > buttonsOrder.size - 1) {
                                    practiceCounter = 0}

                                //Change the color of the next button to press
                                var buttonid = buttonsOrder[practiceCounter]
                                var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                button!!.setBackgroundColor(
                                    Color.rgb(
                                        3,
                                        244,
                                        252
                                    ))//Turquoise
                            }
                        }) }

                    MODE1 -> {

                        //MODE 1 BLOW AND CLICK
                        currentButtonToPress = buttonsOrder[counter] + 1 // +1 to reflect buttons real value
                        this@MainActivity.runOnUiThread(java.lang.Runnable {
                            if (currentButtonToPress == lastButtonPressed) {
                                val stopTime = System.currentTimeMillis()

                                soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                pressedButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE
                                //pressedButton?.setBackgroundColor(Color.rgb(0, 255, 0))
                                timeList.add((stopTime - startTime).toString())
                                // mHandler?.sendEmptyMessageDelayed(PURPLE, buttondelay);
                                lastButtonPressed = 0
                                counter += 1
                                waitForConfirmation =0

                                //Loop Through Buttons and change size at the end of each test
                                if (counter > buttonsOrder.size - 1) {
                                    counter = 0
                                    endOfSection = 1
                                    counter2 += 1
                                    if (counter2 > widthOrder.size - 1) {
                                        counter2 = 0
                                        modeCounter+=1
                                        endOfMode = 1
                                        STATE = selectedModeList[modeCounter]
                                        currentButtonToPress = 7
                                        lastButtonPressed = 0
                                        //Log.d(LOG_TAG, endOfSection.toString())
                                    }

                                    currentButtonToPress = buttonsOrder[counter] + 1


                                    if (endOfMode!=1){
                                        sizeChanger(widthOrder[counter2])
                                    }

                                    b7!!.isEnabled = false
                                    continueButton!!.isEnabled = true
                                    textView!!.text = "When ready press continue"
                                    Log.d(LOG_TAG, widthOrder[counter2].toString())
                                }
                                //Change the color of the next button to press
                                var buttonid = buttonsOrder[counter]
                                var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                if (endOfSection == 0) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        button!!.setBackgroundColor(
                                            Color.rgb(
                                                3,
                                                244,
                                                252
                                            )
                                        ) //Turquoise
                                        startTime = System.currentTimeMillis()

                                    }, 1)
                                } else {
                                    button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                }


                            }
                        })
                    }
                    //MODE 2
                    MODE2 -> {

                    }

                    MODE3 ->{
                        if(waitForNextBlow == 0) {
                            waitForNextBlow =1


                            this@MainActivity.runOnUiThread(java.lang.Runnable {
                                val stopTime = System.currentTimeMillis()
                                var blowButtonid = buttonsOrder[counter]
                                var blowButton = findViewById<View>(buttonsIds[blowButtonid ]) as Button
                                soundPool.play(sound, 1F, 1F, 1, 0, 1F)
                                blowButton?.setBackgroundColor(Color.rgb(98, 0, 238)) //PURPLE

                                reactionTime1.add((stopTime - startTime).toString())
                                buttonLoopLogic()
                                //Change the color of the next button to press
                                var buttonid = buttonsOrder[counter]
                                var button = findViewById<View>(buttonsIds[buttonid]) as Button
                                if (endOfSection == 0) {
                                    Handler(Looper.getMainLooper()).postDelayed(
                                        {
                                            waitForNextBlow = 0
                                            Log.d("LOG_TAG", "THIS IS EXECUTED")
                                            button!!.setBackgroundColor(
                                                Color.rgb(
                                                    3,
                                                    244,
                                                    252
                                                )
                                            ) //Turquoise
                                            startTime = System.currentTimeMillis()

                                        }, ((500..501).random()).toLong()) // Wait a random time in milliseconds
                                } else {
                                    button!!.setBackgroundColor(Color.rgb(98, 0, 238))//Purple
                                }
                            })


                        }
                    }
                    MODE4 -> {

                    }


                }



            }

        }
    }

    fun classifyNoise ( doubleInputBuffer: DoubleArray ): String? {

      // VALUES BELOW NOT GIVEN HERE BUT IN THE MFCC JAVA LIBRARY FILE
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




            /////// Commented Code Gives Problems
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

            //Loop to convert the mfcc values into multi-dimensional array
            for (i in 0 until nFFT) {
                var indexCounter = i * nMFCC
                val rowIndexValue = i % nFFT
                for (j in 0 until nMFCC) {
                    mfccValues[j][rowIndexValue] = mfccInput[indexCounter].toFloat()
                    indexCounter++
                }
            }
            // Input needs to be converted into the correct format accepted by Tflite
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

        //Transform the MFCC 1d float buffer into 1x40x1x1 dimension tensor using TensorBuffer
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



        private const val REQUEST_RECORD_AUDIO = 13
        private const val REQUEST_WRITE_EXTERNAL = 55
        private val LOG_TAG = "BLOWER2"
    }
}