package com.reactnativeacuantsdkbridge.support
import android.util.Log
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.facebook.react.bridge.Callback;
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.model.*
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.acuant.acuantechipreader.initializer.EchipInitializer
import com.acuant.acuantcamera.initializer.MrzCameraInitializer
import com.facebook.react.bridge.Promise
import com.reactnativeacuantsdkbridge.backgroundtasks.AcuantTokenService
import com.reactnativeacuantsdkbridge.backgroundtasks.AcuantTokenServiceListener
import com.facebook.react.bridge.ReactApplicationContext
fun initializerAcuant(context: ReactApplicationContext,callback:IAcuantPackageCallback) {
  var isInitialized =  false
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
          print(token)
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
    // AcuantInitializer.initializeWithToken("",
    //   context,listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer()),initCallback)

  }catch(e: AcuantException){
   // callback.onInitializeFailed(e.toString())
    Log.e("Acuant Error", e.toString())
  }
}
