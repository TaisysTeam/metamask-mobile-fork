import React, {
  Fragment,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Engine from '../../../core/Engine';
import SIMGapWallet from '../../../core/SIMGapWallet';
import SIMGapKeyring from '../../../core/SIMGapKeyring';

import AnimatedQRScannerModal from '../../UI/QRHardware/AnimatedQRScanner';
import SelectSIMGapAccounts from './SelectSIMGapAccounts';
import { SUPPORTED_UR_TYPE } from '../../../constants/qr';

import ConnectSIMGapInstruction from './Instruction';
import Icon from 'react-native-vector-icons/FontAwesome';
import BlockingActionModal from '../../UI/BlockingActionModal';
import { strings } from '../../../../locales/i18n';
import { IAccount } from './types';
import { UR } from '@ngraveio/bc-ur';
import Alert, { AlertType } from '../../Base/Alert';
import AnalyticsV2 from '../../../util/analyticsV2';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import Device from '../../../util/device';
import { useTheme } from '../../../util/theme';
import { fontStyles } from '../../../styles/common';
import Logger from '../../../util/Logger';
import {DeviceEventEmitter, ToastAndroid, PermissionsAndroid} from 'react-native';

const SIMGapKeyringType = "SIMGap Wallet Device";

interface IConnectSIMGapHardwareProps {
  navigation: any;
}
const createStyles = (colors: any) =>
  StyleSheet.create({
    container: {
      flex: 1,
      flexDirection: 'column',
      alignItems: 'center',
    },
    header: {
      marginTop: Device.isIphoneX() ? 50 : 20,
      flexDirection: 'row',
      width: '100%',
      paddingHorizontal: 32,
      alignItems: 'center',
    },
    navbarRightButton: {
      flexDirection: 'row',
      justifyContent: 'flex-end',
      height: 48,
      width: 48,
      flex: 1,
    },
    closeIcon: {
      fontSize: 28,
      color: colors.text.default,
    },
    qrcode: {
      flex: 1,
      flexDirection: 'row',
      justifyContent: 'flex-start',
    },
    error: {
      ...fontStyles.normal,
      fontSize: 14,
      color: colors.red,
    },
    text: {
      color: colors.text.default,
      fontSize: 14,
      ...fontStyles.normal,
    },
  });

const ConnectSIMGapHardware = ({ navigation }: IConnectSIMGapHardwareProps) => {
  const { colors } = useTheme();
  const styles = createStyles(colors);

  const KeyringController = useMemo(() => {
    const { KeyringController: keyring } = Engine.context as any;
    return keyring;
  }, []);

  const AccountTrackerController = useMemo(
    () => (Engine.context as any).AccountTrackerController,
    [],
  );

  const [blockingModalVisible, setBlockingModalVisible] = useState(false);
  const [accounts, setAccounts] = useState<
    { address: string; index: number; balance: string }[]
  >([]);
  const [trackedAccounts, setTrackedAccounts] = useState<{
    [p: string]: { balance: string };
  }>({});
  const [checkedAccounts, setCheckedAccounts] = useState<number[]>([]);
  const [errorMsg, setErrorMsg] = useState('');
  const resetError = useCallback(() => {
    setErrorMsg('');
  }, []);

  const [existingAccounts, setExistingAccounts] = useState<string[]>([]);

  useEffect(() => {
    KeyringController.getAccounts().then((value: string[]) => {
      setExistingAccounts(value);
    });
  }, [KeyringController]);

  const subscribeKeyringState = useCallback((storeValue: any) => {
    setQRState(storeValue);
  }, []);

  useEffect(() => {
    let memStore: any;
    KeyringController.getQRKeyringState().then((_memStore: any) => {
      memStore = _memStore;
      memStore.subscribe(subscribeKeyringState);
    });
    return () => {
      if (memStore) {
        memStore.unsubscribe(subscribeKeyringState);
      }
    };
  }, [KeyringController, subscribeKeyringState]);

  useEffect(() => {
    const unTrackedAccounts: string[] = [];
    accounts.forEach((account) => {
      if (!trackedAccounts[account.address]) {
        unTrackedAccounts.push(account.address);
      }
    });
    if (unTrackedAccounts.length > 0) {
      AccountTrackerController.syncBalanceWithAddresses(unTrackedAccounts).then(
        (_trackedAccounts: any) => {
          setTrackedAccounts(
            Object.assign({}, trackedAccounts, _trackedAccounts),
          );
        },
      );
    }
  }, [AccountTrackerController, accounts, trackedAccounts]);


  var setListener = false;
  var updateText;
  var fid = -1;

	const requestPermission = async() => {
	  var got = true;
	  try {
	    const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
	    {
	    	'title': 'Read Phone State Permission',
	    	'message': 'This is madatory for communication with SIMGapWalletin the SIM'
	    });
	    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
	    	console.log("== GRANTED ==");
			got = true;
	    } else {
	    	console.log("== REJECTED ==");
			got = false;
	    }
	  } catch (err) {
    	console.warn(err);
		got = false;
	  }
	  return got;
	};

  const fecthAvailable = useCallback(async (funcId) => {
	var acc = await KeyringController.connectHardware(SIMGapKeyringType);
Logger.log("onConnectHardware() got acc=" + acc==null?"<null>":("account[" + acc.length + "]"));
	if (acc==null || acc.length==0)
	{
		ToastAndroid.show("SIMGapWallet got an empty data {" + funcId + "}", ToastAndroid.LONG);
		updateText("No Wallet found.");
		return;
	}
	else setAccounts(acc);
  }, [KeyringController]);

  const onConnectHardware = useCallback(async (upd) => {

	updateText = upd;
	var got = await requestPermission();
	if (!got) {upd("Connect is canceled for permission insufficient"); return;}
    resetError();

	if (!setListener)
	{
	    DeviceEventEmitter.addListener('EVENT_SIMGAPWALLET_WAITING', (msg)=>{
	      fid = parseInt(msg.substr(0, msg.indexOf("|")));
	      var s = msg.substr(msg.indexOf("|")+1);
Logger.log("SIMGapWallet requests waiting -- " + s);
			ToastAndroid.show("SIMGapWallet Processing " + s, ToastAndroid.SHORT);
			//SIMGapKeyring.popupWait();
	    });

	    DeviceEventEmitter.addListener('EVENT_SIMGAPWALLET_WAIT_ERROR', (msg)=>{
			fid = parseInt(msg.substr(0, msg.indexOf("|")));
			var s = msg.substr(msg.indexOf("|")+1);
Logger.log("SIMGapWallet interrupt waiting by error -- " + s);
			ToastAndroid.show("SIMGapWallet Error " + s, ToastAndroid.SHORT);
			updateText("ERROR - " + s);
	    });

	    DeviceEventEmitter.addListener('EVENT_SIMGAPWALLET_WAIT_CLOSE', (msg)=>{
			fid = parseInt(msg.substr(0, msg.indexOf("|")));
			Logger.log("SIMGapWallet finished waiting from function -- " + msg.substr(msg.indexOf("|")+1));
			fecthAvailable(fid);

	    });
		setListener = true;
	}
    var r = await SIMGapWallet.available();
Logger.log("onConnectHardware() got result=" + r);
  }, [KeyringController, resetError]);


  const nextPage = useCallback(async () => {
    resetError();
    const _accounts = await KeyringController.connectHardware(SIMGapKeyringType, 1);
    setAccounts(_accounts);
  }, [KeyringController, resetError]);

  const prevPage = useCallback(async () => {
    resetError();
    const _accounts = await KeyringController.connectHardware(SIMGapKeyringType, -1);
    setAccounts(_accounts);
  }, [KeyringController, resetError]);

  const onToggle = useCallback(
    (index: number) => {
      resetError();
      if (!checkedAccounts.includes(index)) {
        setCheckedAccounts([...checkedAccounts, index]);
      } else {
        setCheckedAccounts(checkedAccounts.filter((i) => i !== index));
      }
    },
    [checkedAccounts, resetError],
  );

  const enhancedAccounts: IAccount[] = useMemo(
    () =>
      accounts.map((account) => {
        let checked = false;
        let exist = false;
        if (checkedAccounts.includes(account.index)) checked = true;
        if (
          existingAccounts.find(
            (item) => item.toLowerCase() === account.address.toLowerCase(),
          )
        ) {
          exist = true;
          checked = true;
        }
        return {
          ...account,
          checked,
          exist,
          balance: trackedAccounts[account.address]?.balance || '0x0',
        };
      }),
    [accounts, checkedAccounts, existingAccounts, trackedAccounts],
  );

  const onUnlock = useCallback(async () => {
    resetError();
    setBlockingModalVisible(true);
    try {
      for (const account of checkedAccounts) {
        await KeyringController.unlockHardwareWalletAccount(SIMGapKeyringType, account);
      }
    } catch (err) {
      Logger.log('Error: Connecting SIMGap hardware wallet', err);
    }
    setBlockingModalVisible(false);
    navigation.goBack();
  }, [KeyringController, checkedAccounts, navigation, resetError]);

  const onForget = useCallback(async () => {
    resetError();
    await KeyringController.forgetQRDevice();
    navigation.goBack();
  }, [KeyringController, navigation, resetError]);

  const renderAlert = () =>
    errorMsg !== '' && (
      <Alert type={AlertType.Error} onPress={resetError}>
        <Text style={styles.error}>{errorMsg}</Text>
      </Alert>
    );


  return (
    <Fragment>
      <View style={styles.container}>
        <View style={styles.header}>
          <Icon
            name="microchip"
            size={42}
            style={styles.qrcode}
            color={colors.text.default}
          />
          <TouchableOpacity
            onPress={navigation.goBack}
            style={styles.navbarRightButton}
          >
            <MaterialIcon name="close" size={15} style={styles.closeIcon} />
          </TouchableOpacity>
        </View>
        {accounts.length <= 0 ? (
          <ConnectSIMGapInstruction
            onConnect={onConnectHardware}
            renderAlert={renderAlert}
            navigation={navigation}
          />
        ) : (
          <SelectSIMGapAccounts
            canUnlock={checkedAccounts.length > 0}
            accounts={enhancedAccounts}
            nextPage={nextPage}
            prevPage={prevPage}
            toggleAccount={onToggle}
            onUnlock={onUnlock}
            onForget={onForget}
          />
        )}
      </View>
      <BlockingActionModal modalVisible={blockingModalVisible} isLoadingAction>
        <Text style={styles.text}>
          {strings('connect_qr_hardware.please_wait')}
        </Text>
      </BlockingActionModal>
    </Fragment>
  );
};

export default ConnectSIMGapHardware;
