import { NativeModules } from 'react-native';

type AcuantSdkBridgeType = {
  multiply(a: number, b: number): Promise<number>;
  callAcuant(): any;
};

const { AcuantSdkBridge } = NativeModules;

export default AcuantSdkBridge as AcuantSdkBridgeType;
