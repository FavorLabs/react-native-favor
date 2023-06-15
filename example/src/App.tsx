import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import * as FavorX from 'react-native-favor';

export default function App() {
  const [result, setResult] = React.useState<string | undefined>();

  React.useEffect(() => {
    FavorX.version().then(setResult);
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Button title="start node" onPress={FavorStart} />
      <Button title="stop node" onPress={FavorStop} />
      <Button title="start VPN" onPress={VPNStart} />
      <Button title="stop VPN" onPress={VPNStop} />
    </View>
  );
}

async function FavorStart() {
  try {
    await FavorX.start({
      'network-id': 1,
      'boot-nodes': '',
      'chain-endpoint': '',
      'oracle-contract-addr': '',
    });
    console.log('favorX start ok');
  } catch (e) {
    console.error(e);
  }
}

async function FavorStop() {
  try {
    await FavorX.stop();
    console.log('favorX stop ok');
  } catch (e) {
    console.error(e);
  }
}

async function VPNStart() {
  try {
    FavorX.addVpnStateListener((e) => {
      console.log(e);
    });
    await FavorX.startVPN({
      group: 'changeMe',
      nodes: ['changeMe'],
      // whitelist: ['org.telegram.messenger.web', 'com.twitter.android'],
      blacklist: ['com.favorexample'],
    });
    console.log('favorX vpn start ok');
  } catch (e) {
    console.error(e);
  }
}

async function VPNStop() {
  try {
    await FavorX.stopVPN();
    console.log('favorX vpn stop ok');
  } catch (e) {
    console.error(e);
  }
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
