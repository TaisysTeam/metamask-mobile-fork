/* eslint @typescript-eslint/no-var-requires: "off" */
/* eslint @typescript-eslint/no-require-imports: "off" */

import React from 'react';
import { View, Text, StyleSheet, Image, ScrollView } from 'react-native';
import { strings } from '../../../../../locales/i18n';

// CAUTION!!!
// URLs should be changed to Taisys SIMGap related pages
import {
  KEYSTONE_SUPPORT,
  KEYSTONE_SUPPORT_VIDEO,
} from '../../../../constants/urls';

import {
  fontStyles,
  colors as importedColors,
} from '../../../../styles/common';
import { useTheme } from '../../../../util/theme';
import StyledButton from '../../../UI/StyledButton';

interface IConnectSIMGapInstructionProps {
  navigation: any;
  onConnect: () => void;
  renderAlert: () => Element;
}

// CAUTION!!!
// images/connect-simgap-hardware.png should be re-designed, current image is a copy of QR image
const connectSIMGapHardwareImg = require('images/connect-simgap-hardware.png'); // eslint-disable-line import/no-commonjs

const createStyles = (colors: any) =>
  StyleSheet.create({
    wrapper: {
      flex: 1,
      width: '100%',
      alignItems: 'center',
    },
    container: {
      flexDirection: 'column',
      alignItems: 'center',
      paddingHorizontal: 32,
    },
    scrollWrapper: {
      width: '100%',
    },
    title: {
      width: '100%',
      marginTop: 40,
      fontSize: 24,
      marginBottom: 20,
      ...fontStyles.normal,
      color: colors.text.alternative,
    },
    textContainer: {
      width: '100%',
      marginTop: 20,
    },
    text: {
      fontSize: 14,
      marginBottom: 24,
      ...fontStyles.normal,
      color: colors.text.alternative,
    },
    link: {
      color: colors.primary.default,
      ...fontStyles.bold,
    },
    bottom: {
      alignItems: 'center',
      height: 80,
      justifyContent: 'space-between',
    },
    button: {
      padding: 5,
      paddingHorizontal: '30%',
    },
    buttonText: {
      color: importedColors.white,
      ...fontStyles.normal,
    },
    image: {
      width: 300,
      height: 120,
      marginTop: 40,
      marginBottom: 40,
    },
  });

const ConnectSIMGapInstruction = (props: IConnectSIMGapInstructionProps) => {
  const { onConnect, renderAlert, navigation } = props;
  const { colors } = useTheme();
  const styles = createStyles(colors);

  const str_connect_simgap_hardware_title = 'Connect SIMGap Wallet Howto'; // strings('connect_simgap_hardware.title');
  const str_connect_simgap_hardware_description1 = 'Attach Taisys film SIMoME onto SIM'; // strings('connect_simgap_hardware.description1');
  const str_connect_simgap_hardware_description2 = 'Attaching Video'; // strings('connect_simgap_hardware.description2');
  const str_connect_simgap_hardware_description3 = 'Setup SIMoME and Cold Wallet'; // strings('connect_simgap_hardware.description3');
  const str_connect_simgap_hardware_description4 = 'SIMoME setup Tutortial'; // strings('connect_simgap_hardware.description4');
  const str_connect_simgap_hardware_description5 = '5....'; // strings('connect_simgap_hardware.description5');
  const str_connect_simgap_hardware_description6 = '6....'; // strings('connect_simgap_hardware.description6');
  const str_connect_simgap_hardware_button_continue = strings('connect_qr_hardware.button_continue');


  const navigateToVideo = () => {
    navigation.navigate('Webview', {
      screen: 'SimpleWebview',
      params: {
        url: KEYSTONE_SUPPORT_VIDEO,
        title: str_connect_simgap_hardware_description2,
      },
    });
  };
  const navigateToTutorial = () => {
    navigation.navigate('Webview', {
      screen: 'SimpleWebview',
      params: {
        url: KEYSTONE_SUPPORT,
        title: str_connect_simgap_hardware_description4,
      },
    });
  };

  return (
    <View style={styles.wrapper}>
      <ScrollView
        contentContainerStyle={styles.container}
        style={styles.scrollWrapper}
      >
        <Text style={styles.title}>{str_connect_simgap_hardware_title}</Text>
        {renderAlert()}
        <View style={styles.textContainer}>
          <Text style={styles.text}>
            {str_connect_simgap_hardware_description1}
          </Text>
          <Text style={[styles.text, styles.link]} onPress={navigateToVideo}>
            {str_connect_simgap_hardware_description2}
          </Text>
          <Text style={styles.text}>
            {str_connect_simgap_hardware_description3}
          </Text>
          <Text style={[styles.text, styles.link]} onPress={navigateToTutorial}>
            {str_connect_simgap_hardware_description4}
          </Text>
          <Text style={styles.text}>
            {str_connect_simgap_hardware_description5}
          </Text>
          <Text style={styles.text}>
            {str_connect_simgap_hardware_description6}
          </Text>
        </View>
        <Image
          style={styles.image}
          source={connectSIMGapHardwareImg}
          resizeMode={'contain'}
        />
      </ScrollView>
      <View style={styles.bottom}>
        <StyledButton
          type={'confirm'}
          onPress={onConnect}
          style={styles.button}
        >
          {str_connect_simgap_hardware_button_continue}
        </StyledButton>
      </View>
    </View>
  );
};

export default ConnectSIMGapInstruction;
