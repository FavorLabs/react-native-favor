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
  FavorX.removeVpnStateListener();
  FavorX.addVpnStateListener((e) => {
    console.log(e);
  });
  try {
    await FavorX.start({
      'network-id': 1,
      'boot-nodes':
        // '/ip4/94.103.5.122/tcp/1818/p2p/12D3KooWA9J6uL7xjgYD1j8ybHqVeHMAstTzZXsNpDAU4VqRScwU',
        '/ip4/192.168.100.77/tcp/1634/p2p/12D3KooWGpaG46ChgJzGB2n7nnN3KvbTqnVL8JgirEKYKG8DMrt8',
      'chain-endpoint': '',
      'oracle-contract-addr': '',
      'vpn-enable': true,
      'debug-api-enable': true
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
    await FavorX.startVPN({
      group: 'proxy-test',
      nodes: [
        // '69e1256d685f684c5b903b70dc75b09c3a865a093bf18411973e42fc87fe682f',
        '9adae22e97b2f58e38b45cc1dff48b484868e4fc4f0a27fcd12bbe7733409b30',
      ],
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
