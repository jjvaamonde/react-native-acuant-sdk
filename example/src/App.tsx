import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import AcuantSdkBridge from 'react-native-acuant-sdk';

export default function App() {
  const [result, setResult] = React.useState("");

  React.useEffect(() => {
    async function acuantCall() {
         const result = await AcuantSdkBridge.callAcuant()
        console.log(result)
        setResult(result)
    }
    acuantCall()
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
