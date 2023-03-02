package io.metamask.nativeModules;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import android.view.WindowManager;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.taisys.simgap.Wallet;
import com.taisys.simgap.Wallet.SCSupported;
import com.taisys.simgap.Wallet.SimResponse;
import com.taisys.simgap.Wallet.ErrCodeResponse;
import com.taisys.simgap.SimResp;


public class SIMGapWallet extends ReactContextBaseJavaModule {
  private final static int WALLET_INF_SIZE = 100;
  private final static String WALLET_AID = "A0000000185078646A61636172642D31 ";
  private final static String WALLET_TAR = "584443";
  private final static String WALLET_PATH = "m/44'/60'/0'/0/0"; // null for non-static path, supported in stage 2

  private static final String SIMGAP_WALLET_ERROR_CODE = "SIMGAP_WALLET_ERROR_CODE";
  private final ReactApplicationContext reactContext;

  private Wallet _w = null;
  private SCSupported _scs = null;
  private SimResponse _srs = null;
  private int _sup = -1;
  private int _rcode = 0;
  private byte[] _rdata = null;

  private String[][] _map = null; // Element: [Wallet ID, Address, Path, Index]


  SIMGapWallet(ReactApplicationContext context) {
    super(context);
    reactContext = context;
    _scs = new SCSupported(){
      @Override
      public void isSupported(boolean success) {
        _sup = -1;
        if (success) _sup = 0; //_w.getOtiType();
      }
    };
    _srs = new SimResponse() {
      @Override
      public void simResponse(SimResp simResp) {
        _rcode = simResp.respCode;
        _rdata = simResp.respArray;
      }
    };
  }

  @Override
  public String getName() {
    return "SIMGapWallet";
  }

  public void log(String event, String msg) {
    if (reactContext == null) {
      Log.e("SIMGapWallet Java", "ReactContext is null");
      return;
    }
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
    .emit(event, msg);
  }

  public void log(String msg) {
    log("EVENT_SIMGAP_LOG", msg);
  }

  public void err(String msg) {
    log("EVENT_SIMGAP_ERROR", msg);
  }

  public void alert(String msg) {
    log("EVENT_SIMGAP_ALERT", msg);
  }


  private boolean connectWallet()
  {
    if (_w==null) _w = new Wallet();

    _w.openSEService(reactContext, WALLET_AID, WALLET_TAR, _scs);
    return (_sup>=0);
  }

  private void closeWallet() {_w.closeSEService();}

  private String[] getSingleAccount(byte[] data, int offset, int length)
  {
    // Scheme 1: *Deprecated | Wallet ID 16 bytes |
    // Scheme 2: | Wallet ID 16 bytes | Address 64 ASCII | Path 20 bytes |
    String[] inf = new String[4];
    inf[0] = a2hex(data, offset, 16);
    inf[2] = WALLET_PATH;
    inf[3] = "0";
    offset += 16;
    if (length==100)
    {
      inf[1] = new String(data, offset, 64);
      int i = 0;
      while (i++<64) { byte b=data[offset++]; if ((b>=0x30&&b<=0x39) || (b>=0x41&&b<=0x46) || (b>=0x61&&b<=0x66)) continue; return null;}
      inf[2] = a2hex(data, offset, 20);
      inf[3] = "" + (data[offset+19]&0xFF);
    }
    return inf;
  }

  private int findAddressInMap(String addr)
  {
    if (_map==null) return -1;
    if (addr.startsWith("0x")) addr = addr.substring(2);
    int pos = 1;
    if (_map[0][1]==null) pos = 0;
    String index = "0";
    int idx = addr.indexOf("@");
    if (idx>=0) {index=addr.substring(idx+1); addr=addr.substring(0, idx);}
    for (idx=0; idx<_map.length; idx++) if (_map[idx][pos].equals(addr) && _map[idx][3].equals(index) && _map[idx][2]!=null) return idx;
    return -1;
  }

  @ReactMethod
  public void available(Promise promise) {
    log("Function available() is called.");
    String list = null;
    _map = null;
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    log("Wallet Services is supported.");
    SimResp res = _w.simgapListWallets();
    if (res.respCode==0)
    {
      list = "";
      _rdata = res.respArray;
      int offset = 0;
      int idx = 0;
      String[][] map = new String[_rdata.length/WALLET_INF_SIZE][];
      while (offset<_rdata.length)
      {
        if (offset!=0) list += "|";
        String[] inf = getSingleAccount(_rdata, offset, WALLET_INF_SIZE);
        if (inf==null) {err("Wallet returned unknown information."); promise.resolve("null"); closeWallet(); return;}
        //{ address: string; index: number; balance: string }
        if (inf[1]==null) {list += "0x" + inf[0] + "@0@0";} else
        {
          list += "0x" + inf[0] + "@" + inf[3] + "@0";
        }
        offset += WALLET_INF_SIZE;
        map[idx++] = inf;
      }
      _map = map;
    }
    else
    {
      err("Wallet Services error code: " + res.respCode + ".");
    }
    closeWallet();
    if (list==null) list = "null";
    promise.resolve(list);
  }

  @ReactMethod
  public void newWallet(Promise promise) {
    log("Function newWallet() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String list = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (list==null) list = "null";
    promise.resolve(list);
   }

  @ReactMethod
  public void signMessage(String address, String data, Promise promise) {
    log("Function signMessage() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String sig = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (sig==null) sig = "null";
    promise.resolve(sig);
   }

  @ReactMethod
  public void signPersonalMessage(String address, String data, Promise promise) {
    log("Function signPersonalMessage() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String sig = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (sig==null) sig = "null";
    promise.resolve(sig);
   }

  @ReactMethod
  public void signTransaction(String address, int transType, String data, int chainId, Promise promise) {
    log("Function signTransaction() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String sig = null;
    int idx = findAddressInMap(address);
    if (idx<0) {err("Addrees is not in SIMGap Wallets."); closeWallet(); promise.resolve("null"); return;}
    _rcode = -1;

    // TODO: Compose parameters
    int n = 1;
    byte[] hashes = null;

    _w.simgapSignData(hex2ba(_map[idx][0]), hex2ba(_map[idx][1]), n, hashes, _srs);
    if (_rcode!=0) {err("Wallet Services sign error."); closeWallet(); promise.resolve("null"); return;}
    sig = a2hex(_rdata);
    _rdata = null;
    closeWallet();
    if (sig==null) sig = "null";
    promise.resolve(sig);
   }

  @ReactMethod
  public void signTypedData(String address, String typedData, Promise promise) {
    log("Function signTypedData() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String sig = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (sig==null) sig = "null";
    promise.resolve(sig);
   }

  @ReactMethod
  public void decryptMessage (String address, String encryptedData, Promise promise) {
    log("Function decryptMessage() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String res = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (res==null) res = "null";
    promise.resolve(res);
  }

  @ReactMethod
  public void getEncryptionPublicKey (String address, Promise promise) {
    log("Function getEncryptionPublicKey() is called.");
    if (!connectWallet()) {err("Wallet Services is NOT supported."); closeWallet(); promise.resolve("null"); return;}
    String res = null;
    err("Unsupported Function in current version.");
    closeWallet();
    if (res==null) res = "null";
    promise.resolve(res);
  }





  private final static char[] _TABHEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  public static String a2hex(byte[] arr) {return a2hex(arr, 0, arr.length);}
  public static String a2hex(byte[] arr, int offset, int length)
  {
    if (length<0) return null;
    char[] ca = new char[length*2];
    int idx = 0;
    while (length-->0) {int v=arr[offset++]&0xFF; ca[idx++]=_TABHEX[v>>4]; ca[idx++]=_TABHEX[v&0x0F];}
    return new String(ca);
  }

  public static byte[] hex2ba(String s) {return hex2ba(s, 0, s.length());}
	public static byte[] hex2ba(String s, int offset, int length)
	{
		if (s==null) return null;
		try
		{
			byte[] ba = s.getBytes();
			int idx = 0;
			int c;
			int v = -1;
			length += offset;
			while(offset<length)
			{
				c = ba[offset++]&0xFF;

				if (c<=0x20)
				{
					if (v>=0)
					{
						ba[idx++] = (byte)v;
						v = -1;
					}
					continue;
				}
				else if (c>=0x30 && c<=0x39)
					c -= 0x30;
				else if (c>=0x41 && c<=0x46)
					c -= (0x41 - 0x0A);
				else if (c>=0x61 && c<=0x66)
					c -= (0x61 - 0x0A);
				else
					return null;

				if (v>=0)
				{
					ba[idx++] = (byte)((v<<4) | c);
					v = -1;
				}
				else
					v = c;
			}
			if (v>=0)
				ba[idx++] = (byte)v;

//			if (idx==0)
//				return null;

			byte[] bs = new byte[idx];
			System.arraycopy(ba, 0, bs, 0, idx);
			return bs;
		}
		catch(Exception e)
		{
			return null;
		}
  }

}