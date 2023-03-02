import { EventEmitter } from 'events';
import { ObservableStore } from '@metamask/obs-store';
import { CryptoHDKey, CryptoAccount, ETHSignature, EthSignRequest, DataType } from '@keystonehq/bc-ur-registry-eth';
import { stringify, v4 } from 'uuid';
import { TransactionFactory } from '@ethereumjs/tx';
import { publicToAddress, toChecksumAddress, BN, stripHexPrefix } from 'ethereumjs-util';
import { Transaction } from '@ethereumjs/tx';
import SIMGapWallet from './SIMGapWallet';
import Logger from '../util/Logger';
import {DeviceEventEmitter} from 'react-native';

const keyringType = "SIMGap Wallet Device";

var _initListeners = false;

class SIMGapKeyring {
  constructor(opts) {
    // @ts-ignore
    this.version = 1;
    this.type = keyringType;
    this.getName = () => {
      return this.name;
    };

    this.page = 0;
    this.perPage = 5;
    this.accs = [];
    this.accounts = [];
    this.currentAccount = 0;
    this.unlockedAccount = 0;
    this.name = "SIMGap Hardware";
    this.initialized = false; //hd props;

    this.deserialize(opts);

    this.setAccountToUnlock = index => {
      this.unlockedAccount = parseInt(index, 10);
    };

    if (SIMGapKeyring.instance) {
      SIMGapKeyring.instance.deserialize(opts);
      return SIMGapKeyring.instance;
    }

    SIMGapKeyring.instance = this;
    DeviceEventEmitter.addListener('EVENT_SIMGAP_LOG', (msg)=>{
      Logger.log("SIMGapWallet Log: " + msg);
    });
    DeviceEventEmitter.addListener('EVENT_SIMGAP_ERROR', (msg)=>{
      Logger.log("SIMGapWallet Error: " + msg);
    });
    DeviceEventEmitter.addListener('EVENT_SIMGAP_ALTER', (msg)=>{
      ToastAndroid.show("SIMGapWallet Notice: " + msg, ToastAndroid.SHORT);
    });
  }

// exportAccount should return a hex-encoded private key:
  exportAccount(address, opts = {}) {
    throw new Error(`Private key export is not supported.`);
  }

// removeAccount
  removeAccount(address) {
    if (!this.accounts.map(a => a.toLowerCase()).includes(address.toLowerCase())) {
      throw new Error(`Address ${address} not found in this keyring`);
    }

    this.accounts = this.accounts.filter(a => a.toLowerCase() !== address.toLowerCase());
  }

// addAccounts
  async addAccounts(n = 1) {
    const from = this.unlockedAccount;
    const to = from + n;
    const newAccounts = [];

    for (let i = from; i < to; i++) {
//      const address = await OTIColdWallet.newWallet();
      const address = this.accs[i].address;
      newAccounts.push(address);
      this.page = 0;
      this.unlockedAccount++;
    }



Logger.log("=== Add " + newAccounts + " accounts to " + this.accounts);
    this.accounts = this.accounts.concat(newAccounts);
Logger.log("=== Now accounts is " + this.accounts);
    return this.accounts;
  }


// getAccounts
  getAccounts() {
    return Promise.resolve(this.accounts);
  }

// signTransaction
  // tx is an instance of the ethereumjs-transaction class.
  async signTransaction(address, tx) {
    const {
      r,
      s,
      v
    } = await SIMGapWallet.signTransaction(address, tx.type, tx.getMessageToSign(false), tx.common.chainId());
    const txJson = tx.toJSON();
    txJson.v = v;
    txJson.s = s;
    txJson.r = r;
    txJson.type = tx.type;
    const transaction = TransactionFactory.fromTxData(txJson, {
      common: tx.common
    });
    return transaction;
  }

// signMessage
  async signMessage(withAccount, data) {
    const {
      r,
      s,
      v
    } = await SIMGapWallet.signMessage(withAccount, data);
    return "0x" + Buffer.concat([r, s, v]).toString("hex");
  }

// signPersonalMessage
  async signPersonalMessage(withAccount, messageHex) {
    const {
      r,
      s,
      v
    } = await SIMGapWallet.signPersonalMessage(withAccount, messageHex);
    return "0x" + Buffer.concat([r, s, v]).toString("hex");
  }

// signTypedData
  async signTypedData(withAccount, typedData) {
    const {
      r,
      s,
      v
    } = await SIMGapWallet.signTypedData(withAccount, Buffer.from(JSON.stringify(typedData), "utf-8"));
    return "0x" + Buffer.concat([r, s, v]).toString("hex");
  }

// decryptMessage
  // For eth_decryptMessage:
  async decryptMessage (withAccount, encryptedData) {
    const dec = await SIMGapWallet.signTypedData(withAccount, encryptedData);
    return dec;
  }

// getEncryptionPublicKey
  // get public key for nacl
  async getEncryptionPublicKey (withAccount) {
    return  await SIMGapWallet.getEncryptionPublicKey(withAccount);
  }

// getAppKeyAddress





  serialize() {
    return Promise.resolve({
      //common
      initialized: this.initialized,
      page: this.page,
      perPage: this.perPage,
      accs: this.accs,
      accounts: this.accounts,
      currentAccount: this.currentAccount,
      name: this.name,
      version: this.version,
    });
  }

  deserialize(opts) {
    if (opts) {
      //common props;
      this.page = opts.page;
      this.perPage = opts.perPage;
      this.accs = opts.accs;
      this.accounts = opts.accounts;
      this.currentAccount = opts.currentAccount;
      this.name = opts.name;
      this.initialized = opts.initialized;
    }
  }


  getFirstPage() {
    this.page = 0;
    return this.__getPage(1);
  }

  getNextPage() {
    return this.__getPage(1);
  }

  getPreviousPage() {
    return this.__getPage(-1);
  }

  async __getPage(increment) {
    if (!this.initialized) {
//      await this.readKeyring();
      this.accs = await SIMGapWallet.available();
      if (this.accs==null) return [];
      this.initialized = true;
    }

      this.page += increment;

      if (this.page <= 0) {
        this.page = 1;
      }

      var i = (this.page - 1) * this.perPage;
      var to = i + this.perPage;
      const accounts = [];

      if (i>=this.accs.length) return accounts;
      if (to>this.accs.length) to = this.accs.length;

      while (i < to) {
        const address = this.accs[i].address;
        accounts.push({
          address,
          balance: null,
          index: i
        });
//        this.indexes[toChecksumAddress(address)] = i;
        i++;
      }
      Logger.log('this.page: ' + this.page );
      Logger.log('accounts: ' + accounts );
      Logger.log('this.accs: ' + this.accs );

      return accounts;
  }


}
SIMGapKeyring.type = keyringType;

export { SIMGapKeyring };