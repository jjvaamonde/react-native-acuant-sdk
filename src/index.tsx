import { NativeModules } from 'react-native';

type AcuantSdkBridgeType = {

  callAcuant(): Promise<any>;
  callScanDocumentAcuantCamera(typeDocument:String,retry:Boolean) :  Promise<any>;
  processImage(url:String):Promise<any>;
  processFrontOfDocument():Promise<any>;
  callScanBackDocumentAcuantCamera(retry:Boolean):  Promise<any>;
  uploadBackImageOfDocument():Promise<any>;
  getDataFaceImage():Promise<any>;
  callSelfieCam():Promise<any>
  facialMacth():Promise<any>

};

const { AcuantSdkBridge } = NativeModules;

export default AcuantSdkBridge as AcuantSdkBridgeType;
