'use strict';
import TestHelpers from '../helpers';

import OnboardingView from '../pages/Onboarding/OnboardingView';
import OnboardingCarouselView from '../pages/Onboarding/OnboardingCarouselView';
import ImportWalletView from '../pages/Onboarding/ImportWalletView';

import NetworkView from '../pages/Drawer/Settings/NetworksView';

import MetaMetricsOptIn from '../pages/Onboarding/MetaMetricsOptInView';
import WalletView from '../pages/WalletView';
import DrawerView from '../pages/Drawer/DrawerView';
import SettingsView from '../pages/Drawer/Settings/SettingsView';
import { Browser } from '../pages/Drawer/Browser';

import NetworkEducationModal from '../pages/modals/NetworkEducationModal';

import OnboardingWizardModal from '../pages/modals/OnboardingWizardModal';
import WhatsNewModal from '../pages/modals/WhatsNewModal';
import EnableAutomaticSecurityChecksView from '../pages/EnableAutomaticSecurityChecksView';

import Accounts from '../../wdio/helpers/Accounts';
import { acceptTermOfUse } from '../viewHelper';

const LOCALHOST_URL = 'http://localhost:8545/';

describe('Custom RPC Tests', () => {
  let validAccount;

  beforeAll(() => {
    validAccount = Accounts.getValidAccount();
  });

  beforeEach(() => {
    jest.setTimeout(170000);
  });

  it('should import a wallet', async () => {
    await OnboardingCarouselView.isVisible();
    await OnboardingCarouselView.tapOnGetStartedButton();

    await OnboardingView.isVisible();
    await OnboardingView.tapImportWalletFromSeedPhrase();

    await MetaMetricsOptIn.isVisible();
    await MetaMetricsOptIn.tapNoThanksButton();
    await acceptTermOfUse();

    await ImportWalletView.isVisible();
    await ImportWalletView.enterSecretRecoveryPhrase(validAccount.seedPhrase);
    await ImportWalletView.enterPassword(validAccount.password);
    await ImportWalletView.reEnterPassword(validAccount.password);

    await WalletView.isVisible();
  });

  it('Should dismiss Automatic Security checks screen', async () => {
    await TestHelpers.delay(3500);
    await EnableAutomaticSecurityChecksView.isVisible();
    await EnableAutomaticSecurityChecksView.tapNoThanks();
  });

  it('should tap on "Got it" to dimiss the whats new modal', async () => {
    // dealing with flakiness on bitrise.
    await TestHelpers.delay(2500);
    try {
      await WhatsNewModal.isVisible();
      await WhatsNewModal.tapGotItButton();
    } catch {
      //
    }
  });
  it('should dismiss the onboarding wizard', async () => {
    // dealing with flakiness on bitrise
    await TestHelpers.delay(1000);
    try {
      await OnboardingWizardModal.isVisible();
      await OnboardingWizardModal.tapNoThanksButton();
      await OnboardingWizardModal.isNotVisible();
    } catch {
      //
    }
  });

  it('should go to settings then networks', async () => {
    // Open Drawer
    await WalletView.tapDrawerButton(); // tapping burger menu

    await DrawerView.isVisible();
    await DrawerView.tapSettings();

    await SettingsView.tapNetworks();

    await NetworkView.isNetworkViewVisible();
  });

  it('should add localhost network', async () => {
    // Tap on Add Network button
    await TestHelpers.delay(3000);
    await NetworkView.tapAddNetworkButton();
    await NetworkView.switchToCustomNetworks();

    //await NetworkView.isRpcViewVisible();
    await NetworkView.typeInNetworkName('Localhost');
    await NetworkView.typeInRpcUrl(LOCALHOST_URL);
    await NetworkView.typeInChainId('1337');
    await NetworkView.typeInNetworkSymbol('TST\n');

    await NetworkView.swipeToRPCTitleAndDismissKeyboard(); // Focus outside of text input field
    await NetworkView.tapRpcNetworkAddButton();

    await WalletView.isVisible();
    await WalletView.isNetworkNameVisible('Localhost');
  });
  it('should dismiss network education modal', async () => {
    await NetworkEducationModal.isVisible();
    await NetworkEducationModal.isNetworkNameCorrect('Localhost');
    await NetworkEducationModal.tapGotItButton();
    await NetworkEducationModal.isNotVisible();
  });

  it('should navigate to browser', async () => {
    // Wait for toast notification to disappear
    await TestHelpers.delay(3000);
    await WalletView.isVisible();
    await WalletView.tapBrowser();
    // Check that we are on the browser screen
    await Browser.isVisible();
  });

  it('should go to local test dapp', async () => {
    await TestHelpers.delay(3000);
    // Tap on search in bottom navbar
    await Browser.tapUrlInputBox();
    await Browser.navigateToURL('http://localhost:8000/');

    // Wait for page to load
    await Browser.waitForBrowserPageToLoad();

    if (device.getPlatform() === 'android') {
      // Check that the dapp title is correct
      await TestHelpers.checkIfElementWithTextIsVisible('E2E Test Dapp', 0);
    }
    await TestHelpers.delay(5000);

    // Android has weird behavior where the URL modal stays open, so this closes it
    // Close URL modal
    if (device.getPlatform() === 'android') {
      await device.pressBack();
    }
    await Browser.isVisible();
  });
});
