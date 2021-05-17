package com.reactnativeacuantsdkbridge

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.*
import com.acuant.acuantcommon.type.CardSide
import com.acuant.acuantdocumentprocessing.AcuantDocumentProcessor
import com.acuant.acuantdocumentprocessing.model.*
import com.acuant.acuantdocumentprocessing.service.listener.CreateInstanceListener
import com.acuant.acuantdocumentprocessing.service.listener.GetDataListener
import com.acuant.acuantdocumentprocessing.service.listener.UploadImageListener
import com.acuant.acuantfacecapture.FaceCaptureActivity
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantfacematchsdk.service.FacialMatchListener
import com.acuant.acuanthgliveness.model.FaceCapturedImage
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantpassiveliveness.AcuantPassiveLiveness
import com.acuant.acuantpassiveliveness.model.PassiveLivenessData
import com.acuant.acuantpassiveliveness.model.PassiveLivenessResult
import com.acuant.acuantpassiveliveness.service.PassiveLivenessListener
import com.facebook.react.bridge.*
import com.reactnativeacuantsdkbridge.support.*
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


class AcuantSdkBridgeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    var acuantBridge = AcuantBridge()
    var context = reactContext
    override fun getName(): String {
        return "AcuantSdkBridge"
    }


    // See https://reactnative.dev/docs/native-modules-android

    @ReactMethod
    fun callAcuant(promise: Promise){

        acuantBridge.initializerAcuant(context, object : IAcuantPackageCallback {
            override fun onInitializeSuccess() {
                context.runOnNativeModulesQueueThread {
                    promise.resolve(true)
                }
            }

            override fun onInitializeFailed(error: List<Error>) {
                context.runOnNativeModulesQueueThread {
                    promise.resolve("Could not initialize.\n" + error[0].errorDescription)
                }
            }
        })
    }
    @ReactMethod
    fun callScanDocumentAcuantCamera(typeDocument: String,retry: Boolean, promise: Promise){
        acuantBridge.typeDocumentTouched(typeDocument, context)
        acuantBridge.setRetrying(retry)
        context.runOnNativeModulesQueueThread{

            val mActivityEventListener: ActivityEventListener = object : BaseActivityEventListener() {
                override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
                    if (requestCode == Constants.REQUEST_CAMERA_PHOTO && resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
                        val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
                        data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)?.let { acuantBridge.setCaptureBarCode(it) }
                        if (url != null) {
                            promise.resolve(url)
                        } else {
                            promise.resolve("Camera failed to return valid image path")

                        }
                    }else{
                        promise.reject(resultCode.toString())
                    }
                }
            }
            context.addActivityEventListener(mActivityEventListener)
        }
    }
    @ReactMethod
    fun callScanBackDocumentAcuantCamera(retry: Boolean, promise: Promise){
        acuantBridge.showDocumentCaptureCamera(context)
        acuantBridge.setRetrying(retry)
        context.runOnNativeModulesQueueThread{

            val mActivityEventListener: ActivityEventListener = object : BaseActivityEventListener() {
                override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
                    if (requestCode == Constants.REQUEST_CAMERA_PHOTO && resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
                        val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
                        data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)?.let { acuantBridge.setCaptureBarCode(it) }
                        if (url != null) {
                            val map: WritableMap = Arguments.createMap()
                            val uri: Uri = Uri.fromFile(File(url))
                            map.putString("imageBack", uri.toString())
                            promise.resolve(map)
                        } else {
                            promise.resolve("Camera failed to return valid image path")

                        }
                    }else{
                        promise.reject(resultCode.toString())
                    }
                }
            }
            context.addActivityEventListener(mActivityEventListener)
        }
    }
    @ReactMethod
    fun processImage(url:String, promise: Promise){
        AcuantImagePreparation.evaluateImage(context, CroppingData(url), object : EvaluateImageListener {
            override fun onSuccess(image: AcuantImage) {
                try {
                    val output = FileOutputStream(File(Environment.getExternalStorageDirectory().toString(), "test.png"))
                    FaceCapturedImage.acuantImage?.image?.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.flush()
                    output.close()

                } catch (e: Exception) {

                    e.printStackTrace()
                }
                val map: WritableMap = Arguments.createMap()
                val uri: Uri = Uri.fromFile(File(url))

                map.putString("image", uri.toString())
                map.putBoolean("isPassport",image.isPassport)
                map.putInt("dpi",image.dpi)
                map.putInt("glare",image.glare)
                map.putInt("sharpness",image.sharpness)
                map.putDouble("aspectRadio",image.aspectRatio.toDouble())
                map.putBoolean("isCorrectAspectRatio",image.isCorrectAspectRatio)
                map.putString("bytesImage",image.rawBytes.toString())
                promise.resolve(map)
                acuantBridge.setRecentImage(image)

            }

            override fun onError(error: Error) {
                promise.reject(error.errorDescription)
            }
        })
    }
    // Process Front image
    @ReactMethod
    fun processFrontOfDocument(promise: Promise) {
        acuantBridge.getRecentImage()?.let { acuantBridge.setcaptureFrontImage(it) }
        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Front
        idOptions.isHealthCard = false
        idOptions.isRetrying = acuantBridge.getRetrying()
        val capturedFrontImage = acuantBridge.getCaptureFrontImage()
        val frontData = if (capturedFrontImage?.rawBytes != null) {
            EvaluatedImageData(capturedFrontImage!!.rawBytes)
        } else {
            promise.reject("bytes null")
        }

        if (acuantBridge.getRetrying()) {
            uploadFrontImageOfDocument(acuantBridge.getDocumentInstanceId(), frontData as EvaluatedImageData, idOptions,promise)

        } else {
            AcuantDocumentProcessor.createInstance(idOptions, object: CreateInstanceListener {
                override fun instanceCreated(instanceId: String?, error: Error?) {
                    if (error == null) {
                        // Success : Instance Created
                        if (instanceId != null) {
                            acuantBridge.documentInstanceID= instanceId
                        }
                        uploadFrontImageOfDocument(instanceId!!, frontData as EvaluatedImageData, idOptions,promise)

                    } else {
                        // Failure
                            promise.reject(error.errorDescription)

                    }
                }
            })
        }
    }
    @ReactMethod
    fun uploadBackImageOfDocument(promise: Promise) {
        acuantBridge.capturedBackImage = acuantBridge.getRecentImage()
        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Back
        idOptions.isHealthCard = false
        idOptions.isRetrying = false
        val capturedBackImage = acuantBridge.capturedBackImage
        val backData = if (capturedBackImage?.rawBytes != null) {
            EvaluatedImageData(acuantBridge.capturedBackImage!!.rawBytes, acuantBridge.getBarCode())
        } else {

            promise.reject("bytes null")

        }

        AcuantDocumentProcessor.uploadImage(acuantBridge.documentInstanceID, backData as EvaluatedImageData, idOptions, object : UploadImageListener {
            override fun imageUploaded(error: Error?, classification: Classification?) {
                if (error == null) {
                    promise.resolve("SUCCESS")
                }else{
                    promise.resolve("uncategorized")
                }
            }
        })
    }
    // Upload front Image of Driving License
    fun uploadFrontImageOfDocument(instanceId: String, frontData: EvaluatedImageData, idOptions: IdOptions,promise: Promise) {
        var promise = promise
        acuantBridge.numberOfClassificationAttempts += 1
        // Upload front Image of DL
        Log.d("InstanceId:", instanceId)
        AcuantDocumentProcessor.uploadImage(instanceId, frontData, idOptions, object : UploadImageListener {
            override fun imageUploaded(error: Error?, classification: Classification?) {
                if (error == null) {
                    // Successfully uploaded

                   acuantBridge.frontCaptured = true
                    if (acuantBridge.isBackSideRequired(classification)) {
                        val map: WritableMap = Arguments.createMap()
                        map.putBoolean("isBackSideRequired",true)
                        promise.resolve(map)

                    } else {
                        val map: WritableMap = Arguments.createMap()
                        map.putBoolean("isBackSideRequired",false)
                        promise.resolve(map)
                    }

                } else {
                    // Failure

                    promise.resolve("uncategorized")
                }
            }
        })
    }
    @ReactMethod
    fun getDataFaceImage(promise: Promise) {
        AcuantDocumentProcessor.getData(acuantBridge.getDocumentInstanceId(),false, object : GetDataListener {
            override fun processingResultReceived(result: ProcessingResult?) {
                if (result == null || result.error != null) {

                    promise.resolve(result?.error?.errorDescription ?: ErrorDescriptions.ERROR_DESC_CouldNotGetConnectData)
                } else if ((result as IDResult).fields == null || result.fields.dataFieldReferences == null) {

                    promise.resolve("Unknown error happened.\nCould not extract data")

                }

                var faceImageUri: String? = null

                val fieldReferences = (result as IDResult).fields.dataFieldReferences
                for (reference in fieldReferences) {
                  if (reference.key == "Photo" && reference.type == "uri") {
                        faceImageUri = reference.value
                    }
                }
                val faceImage =  acuantBridge.loadAssureIDImage(faceImageUri, Credential.get())

                if (faceImage != null) {
                    val map: WritableMap = Arguments.createMap()
                    map.putString("faceImageUri",faceImageUri.toString())
                    acuantBridge.setCaptureFaceImage(faceImage)
                    promise.resolve(map)
                }

            }
        })
    }
    @ReactMethod
    fun callSelfieCam(promise: Promise){
        acuantBridge.showFaceCapture(context)
        context.runOnNativeModulesQueueThread{

            val mActivityEventListener: ActivityEventListener = object : BaseActivityEventListener() {
                override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
                    if (requestCode == Constants.REQUEST_CAMERA_FACE_CAPTURE) {

                        when (resultCode) {
                            FaceCaptureActivity.RESPONSE_SUCCESS_CODE -> {
                                val url = data?.getStringExtra(FaceCaptureActivity.OUTPUT_URL)

                                if (url == null) {
                                    promise.reject("INVALID OUTPUT")

                                }
                                val map: WritableMap = Arguments.createMap()
                                val uri: Uri = Uri.fromFile(File(url))
                                map.putString("selfieImageUri",uri.toString())
                                val bytes = url?.let { acuantBridge.readFromFile(it) }
                                promise.resolve("INVALID OUTPUT")
                                if (bytes != null) {
                                    acuantBridge.capturedSelfieImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                            }
                            FaceCaptureActivity.RESPONSE_CANCEL_CODE -> {
                                promise.reject("CANCEL USER")
                            }
                            else -> {
                                promise.reject("ERROR IN PROCESS")
                            }
                        }
                    }
                }
            }
            context.addActivityEventListener(mActivityEventListener)
        }

    }
    @ReactMethod
    fun facialMacth(promise: Promise){
        context.runOnNativeModulesQueueThread {
            val facialMatchData = FacialMatchData()
            facialMatchData.faceImageOne = acuantBridge.capturedFaceImage
            facialMatchData.faceImageTwo = acuantBridge.capturedSelfieImage

            if(facialMatchData.faceImageOne != null && facialMatchData.faceImageTwo != null){

                AcuantFaceMatch.processFacialMatch(facialMatchData, FacialMatchListener { result ->
                    context.runOnNativeModulesQueueThread {
                        if (result!!.error == null) {
                            val map: WritableMap = Arguments.createMap()
                            map.putBoolean("isMatch",result.isMatch)
                            map.putInt("score",result.score)
                            map.putString("transactionId",result.transactionId)

                            promise.resolve(map)
                        } else {
                            promise.reject(result.error.errorDescription)
                        }
                    }

                })
            }
            else{
                promise.reject("Face or facial image is null")
            }
        }
    }

}
