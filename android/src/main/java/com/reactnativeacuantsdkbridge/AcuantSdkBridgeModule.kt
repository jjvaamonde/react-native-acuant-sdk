package com.reactnativeacuantsdkbridge

import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.reactnativeacuantsdkbridge.support.*
import com.acuant.acuantcommon.model.*
class AcuantSdkBridgeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    var context = reactContext
    override fun getName(): String {
        return "AcuantSdkBridge"
    }

    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @ReactMethod
    fun multiply(a: Int, b: Int, promise: Promise) {

      promise.resolve(a * b)

    }
    @ReactMethod
    fun callAcuant(promise: Promise){
      initializerAcuant(context, object: IAcuantPackageCallback {
            override fun onInitializeSuccess() {
              context.runOnNativeModulesQueueThread {
                promise.resolve(true)
              }
            }

            override fun onInitializeFailed(error: List<Error>) {
                context.runOnNativeModulesQueueThread {
                     promise.resolve("Could not initialize.\n"+error[0].errorDescription)
                }
            }
        })
    }

}
