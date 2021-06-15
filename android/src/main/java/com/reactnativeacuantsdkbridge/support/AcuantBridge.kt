package com.reactnativeacuantsdkbridge.support
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_MRZ_RESULT
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.facebook.react.bridge.Callback;
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.model.*
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.acuant.acuantechipreader.initializer.EchipInitializer
import com.acuant.acuantcamera.initializer.MrzCameraInitializer
import com.acuant.acuantcommon.helper.CredentialHelper
import com.acuant.acuantdocumentprocessing.model.Classification
import com.acuant.acuantfacecapture.FaceCaptureActivity
import com.acuant.acuanthgliveness.model.FaceCapturedImage
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantpassiveliveness.AcuantPassiveLiveness
import com.acuant.acuantpassiveliveness.model.PassiveLivenessData
import com.acuant.acuantpassiveliveness.model.PassiveLivenessResult
import com.acuant.acuantpassiveliveness.service.PassiveLivenessListener
import com.facebook.react.bridge.Promise
import com.reactnativeacuantsdkbridge.backgroundtasks.AcuantTokenService
import com.reactnativeacuantsdkbridge.backgroundtasks.AcuantTokenServiceListener
import com.facebook.react.bridge.ReactApplicationContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.HashMap


class AcuantBridge {
  var numberOfClassificationAttempts: Int = 0
  var isInitialized = false
  var livenessSelected = 0
  var isKeyless = false
  var processingFacialLiveness = false
  val useTokenInit = true
  private var token = ""
  var capturedFrontImage: AcuantImage? = null
  var capturedBackImage: AcuantImage? = null
  var capturedSelfieImage: Bitmap? = null
  var capturedFaceImage: Bitmap? = null
  private var capturedBarcodeString: String? = null
  var frontCaptured: Boolean = false
  private var isHealthCard: Boolean = false
  private var isRetrying: Boolean = false
  private var insuranceButton: Button? = null
  private var idButton: Button? = null
  private var capturingImageData: Boolean = true
  private var capturingSelfieImage: Boolean = false
  private var capturingFacialMatch: Boolean = false
  private var facialResultString: String? = null
  private var facialLivelinessResultString: String? = null
  private var captureWaitTime: Int = 0
  var documentInstanceID: String = ""
  private var autoCaptureEnabled: Boolean = true
  var recentImage: AcuantImage? = null
  var typeDocumentCapture = ""

  fun cleanUpTransaction() {
    capturedFrontImage?.destroy()
    capturedBackImage?.destroy()
    facialResultString = null
    capturedFrontImage = null
    capturedBackImage = null
    capturedSelfieImage = null
    capturedFaceImage = null
    facialLivelinessResultString = null
    capturedBarcodeString = null
    isHealthCard = false
    processingFacialLiveness = false
    isRetrying = false
    capturingImageData = true
    documentInstanceID = ""
    numberOfClassificationAttempts = 0
  }

    fun setCaptureFaceImage(bitmap: Bitmap){
        this.capturedFaceImage = bitmap
    }
  fun getRetrying(): Boolean{
    return isRetrying
  }
  fun setRetrying(bool : Boolean){
    isRetrying = bool
  }
  fun initializerAcuant(context: ReactApplicationContext,callback:IAcuantPackageCallback) {

    val initCallback = object: IAcuantPackageCallback{
      override fun onInitializeSuccess() {
        callback.onInitializeSuccess()
        // getFacialLivenessCredentials(callback)
      }
      override fun onInitializeFailed(error: List<Error>) {
        callback.onInitializeFailed(error)
      }
    }
    Credential.initFromXml("acuant.config.xml", context)
    try{
      AcuantTokenService(Credential.get(), object : AcuantTokenServiceListener {
        override fun onSuccess(token: String) {
          if (!isInitialized) {
            this@AcuantBridge.token = token
            AcuantInitializer.initializeWithToken("acuant.config.xml",
                    token,
                    context,
                    listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer()),
                    initCallback)
          } else {
            if(Credential.setToken(token)) {
              print(token)
              initCallback.onInitializeSuccess()
            } else {
              print("Error kt")
              initCallback.onInitializeFailed(listOf(Error(-2, "Error in setToken\nBad/expired token")))
            }
          }

        }

        override fun onFail(responseCode: Int) {
          print(responseCode)
          initCallback.onInitializeFailed(listOf(Error(responseCode, "Error in getToken service.\nCode: $responseCode")))
        }

      }).execute()

    }catch(e: AcuantException){
      // callback.onInitializeFailed(e.toString())
      Log.e("Acuant Error", e.toString())
    }
  }

  fun typeDocumentTouched(typeDocument:String,context: ReactApplicationContext) {
    typeDocumentCapture = typeDocument
    frontCaptured = false
    cleanUpTransaction()
    captureWaitTime = 0
    showDocumentCaptureCamera(context)
  }

  //Show Rear Camera to Capture Image of ID,Passport or Health Insurance Card
  fun showDocumentCaptureCamera(context: ReactApplicationContext) {

    capturedBarcodeString = null
    val cameraIntent = Intent(
            context,
            AcuantCameraActivity::class.java
    )
    cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
            AcuantCameraOptions
                    .DocumentCameraOptionsBuilder()
                    .setAutoCapture(autoCaptureEnabled)
                    .build()
    )
    context.startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO,null)

  }
  fun showFaceCapture(context: ReactApplicationContext) {
    val cameraIntent = Intent(
      context,
      FaceCaptureActivity::class.java
    )

    context.startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_FACE_CAPTURE,null)
  }

  fun setCaptureBarCode(barCode:String){
    capturedBarcodeString = barCode
  }


    fun getBarCode() : String? {
        return capturedBarcodeString
    }
  fun isBackSideRequired(classification : Classification?):Boolean {
      var isBackSideScanRequired = false
      if (classification?.type != null && classification.type.supportedImages != null) {
          val list = classification.type.supportedImages as ArrayList<HashMap<*, *>>
          for (i in list.indices) {
              val map = list[i]
              if (map["Light"] == 0) {
                  if (map["Side"] == 1) {
                      isBackSideScanRequired = true
                  }
              }
          }
      }
      return isBackSideScanRequired
  }
  fun loadAssureIDImage(url: String?): Bitmap? {
        if (url != null) {
            val c = URL(url).openConnection() as HttpURLConnection
            val auth = CredentialHelper.getAcuantAuthHeader(Credential.get())
            c.setRequestProperty("Authorization", auth)
            c.useCaches = false
            c.connect()
            val img = BitmapFactory.decodeStream(c.inputStream)
            c.disconnect()
            return img
        }
        return null
  }
  fun readFromFile(fileUri: String): ByteArray{
    val file = File(fileUri)
    val bytes = ByteArray(file.length().toInt())
    try {
      val buf = BufferedInputStream(FileInputStream(file))
      buf.read(bytes, 0, bytes.size)
      buf.close()
    } catch (e: Exception){
      e.printStackTrace()
    }
    return bytes
  }



}

