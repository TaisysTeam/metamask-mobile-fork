package io.metamask.nativeModules;

import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.util.Log;

import android.app.Activity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Vector;

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

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.taisys.simgap.utils.QCrypto;

public class SIMGapWallet extends ReactContextBaseJavaModule // implements ActivityCompat.OnRequestPermissionsResultCallback
{
	private final static String ERR_TAG = "Error: ";
	private final static String RES_TAG = "Result: ";

	private final static int FID_NEW_WALLET            = 0;
	private final static int FID_AVAILABLE             = 1;
	private final static int FID_SIGN_TRANSACTION      = 2;
	private final static int FID_SIGN_MESSAGE          = 3;
	private final static int FID_SIGN_PERSONAL_MESSAGE = 4;
	private final static int FID_SIGN_TYPED_DATA       = 5;
	private final static int FID_DECRYPT_MESSAGE       = 6;

	private final static String[] FNAMES = {
		"newWallet",
		"available",
		"signTransaction",
		"signMessage",
		"signPersonalMessage",
		"signTypedData",
		"decryptMessage"
	};

	private final static int IDX_WALLET_ID     = 0;
	private final static int IDX_ACCOUNT_ADDR  = 1;
	private final static int IDX_ACCOUNT_PATH  = 2;
	private final static int IDX_ACCOUNT_INDEX = 3;


	private final static int WALLET_INF_SIZE = 100;
	private final static String WALLET_AID = "A0000000185078646A61636172642D31 ";
	private final static String WALLET_TAR = "584443";
	private final static String WALLET_PATH = "m/44'/60'/0'/0/0"; // null for non-static path, supported in stage 2

//////////////////////////////////////////////////////////////////////////////////////////////
// Bitcoin Address:
// Legacy (P2PKH) startsWith("1")            1Fh7ajXabJBpZPZw8bjD3QU4CuQ3pRty9u
// Nested SegWit (P2SH) startsWith("3")      3KF9nXowQ4asSGxRRzeiTpDjMuwM2nypAN
// Native SegWit (Bech32)  startsWith("bc1") bc1qf3uwcxaz779nxedw0wry89v9cjh9w2xylnmqc3
//------------------------------------------------------------------------------------------
// Ethereum Address:
// 42 chars, startsWith("0x")                0x7F9B8EE7Eb4a7c0f9e16BCE6FAADA7179E84F3B9
//////////////////////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////////////////////
// Format Define of simgapListWallets() Return Data
// - | Wallet ID (16) | Path (20 bytes) | LV of Address Text |
//////////////////////////////////////////////////////////////////////////////////////////////


	private static String FAKE_WALL_ID_1 = "A0123456789BCDEF0000000000000001";
	private static String FAKE_ADDRESS_1 = "1FAKE01abJBpZPZw8bjD3QU4CuQ3pRty9u";
	private static String FAKE_WALL_ID_2 = "B0123456789BCDEF0000000000000002";
	private static String FAKE_ADDRESS_2 = "3FAKE02wQ4asSGxRRzeiTpDjMuwM2nypAN";
	private static String FAKE_WALL_ID_3 = "B0123456789BCDEF0000000000000003";
	private static String FAKE_ADDRESS_3 = "bc1FAKE03qfw779nxedw0wry89v9cjh9w2xylnmqc3";
	private static String FAKE_WALL_ID_4 = "B0123456789BCDEF0000000000000004";
	private static String FAKE_ADDRESS_4 = "0xFACE04A2FE1A7F9B8EE7Eb4a7c0f9e16BCE6FAAD";
	private static String FAKE_KEYPATH_1 = "8000002C80000001800000000000000000000000";  // BTC:
	private static String FAKE_KEYPATH_2 = "8000002C80000001800000000000000000000000";  // BTC:
	private static String FAKE_KEYPATH_3 = "8000002C80000001800000000000000000000000";  // BTC:
	private static String FAKE_KEYPATH_4 = "8000002C8000003C800000000000000000000000";  // ETH: 0xAbcDe...28 (0x + 40 ASCII)

	private static String FAKE_ACCOUNT_1 = " " + FAKE_WALL_ID_1 // Wallet Identifier, 16 bytes
	                                     + " " + FAKE_KEYPATH_1 // PATH, 20 bytes
	                                     + " " + Util.LV((FAKE_ADDRESS_1).getBytes()); // Address, LV of address text

	private static String FAKE_ACCOUNT_2 = " " + FAKE_WALL_ID_2 // Wallet Identifier, 16 bytes
	                                     + " " + FAKE_KEYPATH_2 // PATH, 20 bytes
	                                     + " " + Util.LV((FAKE_ADDRESS_2).getBytes()); // Address, LV of address text

	private static String FAKE_ACCOUNT_3 = " " + FAKE_WALL_ID_3 // Wallet Identifier, 16 bytes
	                                     + " " + FAKE_KEYPATH_3 // PATH, 20 bytes
	                                     + " " + Util.LV((FAKE_ADDRESS_3).getBytes()); // Address, LV of address text

	private static String FAKE_ACCOUNT_4 = " " + FAKE_WALL_ID_4 // Wallet Identifier, 16 bytes
	                                     + " " + FAKE_KEYPATH_4 // PATH, 20 bytes
	                                     + " " + Util.LV((FAKE_ADDRESS_4).getBytes()); // Address, LV of address text

	private final static int FUNC_LIST_WALLET       = 0x00A17232;
	private final static int FUNC_SIGN_TRANSACTION 	= 0x001E1342;

	private static final String SIMGAP_WALLET_ERROR_CODE = "SIMGAP_WALLET_ERROR_CODE";
	private final ReactApplicationContext reactContext;

	private Wallet _w = null;
	private SimResponse _srs = null;
	private int _rcode = 0;
	private byte[] _rdata = null;
	private static boolean _running = false;
	private Object _res = null;

	private String _msg = "";
	private String _err = "";

	private String[][] _map = null; // Element: [Wallet ID, Address, Path, Index]

	private byte[][] _para_signData = new byte[4][];
	private byte[] _pBoolean = {(byte)0};
	private byte[] _para_address = null;
	private byte[] _para_data = null;

	SIMGapWallet(ReactApplicationContext context) {
		super(context);
		reactContext = context;

		_srs = new SimResponse() {
			@Override
			public void simResponse(SimResp simResp) {
				_rcode = simResp.respCode;
				_rdata = simResp.respArray;
			}
		};
//		boolean canRead = (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
//		if (!canRead) ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, 12);
	}

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//		if (requestCode == 12 ) {
//			Log.i("Permission Request", "Code=" + requestCode + ", " + grantResults.length + " Results");
//			for (int i=0; i<grantResults.length; i++) {
//				Log.i("Permission Granting", permissions[i] + (grantResults[i]==PackageManager.PERMISSION_GRANTED?" Accepted":" Refused"));
//			}
//		}
//   }

	static {
		Security.removeProvider("BC");
		// Confirm that positioning this provider at the end works for your needs!
		Security.addProvider(new BouncyCastleProvider());
		Log.i("------------ Keccak-256(00) -------------->", Util.a2hex(QCrypto.keccak(QCrypto.KECCAK_256, new byte[1])));
	}

	@Override
	public String getName() {
		return "SIMGapWallet";
	}

	public void log(String event, String msg) {
		if (event=="EVENT_SIMGAP_ERROR") Log.println(Log.ERROR, "SIMGapWallet.java", ">>>" + event + ">>> " + msg);
		else Log.println(Log.INFO, "SIMGapWallet.java", ">>>" + event + ">>> " + msg);
		if (reactContext == null) {
			Log.println(Log.ERROR, "SIMGapWallet.Java", "ReactContext is null");
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



	private String[] _results = new String[FNAMES.length];

	@ReactMethod
	public void fetchResult(int funcID, Promise promise) {
		log("----fetchResult---- Function ID: " + funcID);
		log("----fetchResult---- Results.length: " + _results.length);
		for (int i = 0; i < _results.length; i++) {
			log("Result: " + _results[i]);
		}

		if (funcID<0 || funcID>=_results.length || _results[funcID]==null) {
			err("Error: Cannot fetch result.");
			promise.resolve("null"); 
			return;
		}
		String s = _results[funcID];
		if (s.startsWith(ERR_TAG)) {
			err(s);  
			promise.resolve("null"); return;
		}
		if (s.startsWith(RES_TAG)) {
			log(s);
			promise.resolve(s.substring(RES_TAG.length())); 
			return;
		}
		err("Invalid result information: " + s);
		promise.resolve("null");
	}

static boolean USE_FAKE_DATA_WHEN_ERROR = true;


	@ReactMethod
	public void available(Promise promise) {
		int fid = FID_AVAILABLE;
		if (!asyncWalletFunction(fid, new IJobs()
		{
			@Override
			public void doJobs()
			{
				String list = null;
				_map = null;
				_results[fid] = null;
				SimResp res = _w.simgapListWallets();
				int code = res.respCode;
if (USE_FAKE_DATA_WHEN_ERROR)
{
				///// Fake Data /////
				if (code!=0)
				{
//					Log.i("SIMGapWallet.java", "==== Use fake data ====");
//					Log.i("SIMGapWallet.java", "    ACCOUNT 1");
//					Log.i("SIMGapWallet.java", "             ID:" + FAKE_WALL_ID_1);
//					Log.i("SIMGapWallet.java", "           Path:" + FAKE_KEYPATH_1);
//					Log.i("SIMGapWallet.java", "        Address:" + FAKE_ADDRESS_1);
//					Log.i("SIMGapWallet.java", "    ACCOUNT 2");
//					Log.i("SIMGapWallet.java", "             ID:" + FAKE_WALL_ID_2);
//					Log.i("SIMGapWallet.java", "           Path:" + FAKE_KEYPATH_2);
//					Log.i("SIMGapWallet.java", "        Address:" + FAKE_ADDRESS_2);
//					Log.i("SIMGapWallet.java", "    ACCOUNT 3");
//					Log.i("SIMGapWallet.java", "             ID:" + FAKE_WALL_ID_3);
//					Log.i("SIMGapWallet.java", "           Path:" + FAKE_KEYPATH_3);
//					Log.i("SIMGapWallet.java", "        Address:" + FAKE_ADDRESS_3);
//					Log.i("SIMGapWallet.java", "    ACCOUNT 4");
//					Log.i("SIMGapWallet.java", "             ID:" + FAKE_WALL_ID_4);
//					Log.i("SIMGapWallet.java", "           Path:" + FAKE_KEYPATH_4);
//					Log.i("SIMGapWallet.java", "        Address:" + FAKE_ADDRESS_4);
//					Log.i("SIMGapWallet.java", "Fake String is: " + FAKE_ACCOUNT_1 + FAKE_ACCOUNT_2 + FAKE_ACCOUNT_3 + FAKE_ACCOUNT_4);
					res.respArray = Util.hex2ba(FAKE_ACCOUNT_1 + FAKE_ACCOUNT_2 + FAKE_ACCOUNT_3 + FAKE_ACCOUNT_4);
					code = 0;
				}
}


				if (code==0)
				{
					list = "";
					_rdata = res.respArray;
					int offset = 0;
					int size = 0;
					Vector vm = new Vector();
					//String[][] map = new String[_rdata.length/WALLET_INF_SIZE][];
					while (offset<_rdata.length)
					{
						if (offset!=0) list += "|";
						size = _rdata[offset+36];
						if (size<0 || (offset+ 37 + size)>_rdata.length) return;
						String[] inf = getSingleAccount(_rdata, offset, size);
						if (inf==null) return;
						//{ address: string; index: number; balance: string }
						list += inf[IDX_ACCOUNT_ADDR] + "@" + inf[IDX_ACCOUNT_INDEX] + "@0";
						vm.add(inf);
						offset += 37 + size;
					}
					if (offset!=_rdata.length || vm.size()==0) return;
					String[][] map = new String[vm.size()][];
					for (offset=0; offset<vm.size(); offset++) map[offset] = (String[])(vm.elementAt(offset));
					_map = map;
					_results[fid] = (list==null)?(ERR_TAG + " List is empty"):(RES_TAG + list);
				}
				else
				{
					_results[fid] = ERR_TAG + "Wallet Services error code: " + code + ".";
				}
			}

		}))  promise.resolve("null");
		else promise.resolve("" + fid);
	}

	@ReactMethod
	public void newWallet(Promise promise) {
		String func = "newWallet";
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);
	}

	@ReactMethod
	public void signMessage(String address, String data, Promise promise) {
		String func = "signMessage";
//		_para_address = address;
//		_para_data = data;
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);
	}

	@ReactMethod
	public void signPersonalMessage(String address, String data, Promise promise) {
		String func = "signPersonalMessage";
//		_para_address = address;
//		_para_data = data;
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);
	}

	@ReactMethod
	public void signTransaction(String address, int transType, String data, int chainId, Promise promise) {
		int fid = FID_AVAILABLE;
		if (!asyncWalletFunction(fid, new IJobs()
		{
			@Override
			public void doJobs()
			{
				_results[fid] = null;
				int idx = findAddressInMap(address);
				if (idx<0) {_results[fid] = ERR_TAG + "Addrees is not in SIMGap Wallets."; return;}
				_rcode = -1;

				// TODO: Compose parameters
				int n = 1;
				byte[] hashes = null;

			    _w.simgapSignData(Util.hex2ba(_map[idx][0]), Util.hex2ba(_map[idx][1]), n, hashes, _srs);
				if (_rcode!=0) {_results[fid] = ERR_TAG + "Wallet Services sign error.";return;}
				String sig = Util.a2hex(_rdata);
				_rdata = null;
				_results[fid] = (sig==null)?(ERR_TAG + "Cannot get signature"):(RES_TAG + sig);
			}
		}))  promise.resolve("null");
		else promise.resolve("" + fid);
	}

	@ReactMethod
	public void signTypedData(String address, String typedData, Promise promise) {
		String func = "signTypedData";
//		_para_address = address;
//		_para_data = typedData;
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);
	 }

	@ReactMethod
	public void decryptMessage (String address, String encryptedData, Promise promise) {
		String func = "decryptMessage";
//		_para_address = address;
//		_para_data = encryptedData;
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);
	}

	@ReactMethod
	public void getEncryptionPublicKey (String address, Promise promise) {
		String func = "getEncryptionPublicKey";
//		_para_address = address;
//		if (!asyncWalletFunction(func)) {promise.resolve("null"); return;}
		unsupport(func, promise);

	}





	private interface IJobs {
		public void doJobs();
	}

	private String[] getSingleAccount(byte[] data, int offset, int lenAddr)
	{
		// Scheme: | Wallet ID 16 bytes | Path 20 bytes | LV of Address ASCII |
		for(int i=0; i<lenAddr; i++) { byte b=data[offset+37+i]; if ((b>=0x30&&b<=0x39) || (b>=(byte)'A'&&b<=(byte)'Z') || (b>=(byte)'a'&&b<=(byte)'z')) continue; return null;}

//Log.i("SIMGapWallet.java", "Data Block = byte[] { " + Util.a2hex(data, offset, 37+lenAddr) + " }");
		String[] inf = new String[4];
		inf[IDX_WALLET_ID] = Util.a2hex(data, offset, 16);
//Log.i("SIMGapWallet.java", "info[IDX_WALLET_ID]     = '" + inf[IDX_WALLET_ID] + "'");
		offset += 16;
		inf[IDX_ACCOUNT_PATH] = Util.a2hex(data, offset, 20);
//Log.i("SIMGapWallet.java", "info[IDX_ACCOUNT_PATH]  = '" + inf[IDX_ACCOUNT_PATH] + "'");
		inf[IDX_ACCOUNT_INDEX] = "" + (data[offset+19]&0xFF);
//Log.i("SIMGapWallet.java", "info[IDX_ACCOUNT_INDEX] = '" + inf[IDX_ACCOUNT_INDEX] + "'");
		offset += 20;
			inf[IDX_ACCOUNT_ADDR] = new String(data, offset+1, lenAddr);
//Log.i("SIMGapWallet.java", "info[IDX_ACCOUNT_ADDR]  = '" + inf[IDX_ACCOUNT_ADDR] + "'");
		return inf;
	}

	private int findAddressInMap(String addr)
	{
		if (_map==null) return -1;
		String index = "0";
		int idx = addr.indexOf("@");
		if (idx>=0) {index=addr.substring(idx+1); addr=addr.substring(0, idx);}
		for (idx=0; idx<_map.length; idx++) if (_map[idx][IDX_ACCOUNT_ADDR].equals(addr) && _map[idx][IDX_ACCOUNT_INDEX].equals(index)) return idx;
		return -1;
	}

	private void unsupport(String funcName, Promise promise) { err("Unsupported Function " + funcName + "() in current version."); promise.resolve("null"); }



	private boolean asyncWalletFunction(int fid, IJobs jobs)
	{
		String func = FNAMES[fid];
		log("Function " + func + "() is calling.");
		Log.i("SIMGapWallet.java", "Calling connectWallet()");
		if (_w==null)
		{
			try
			{
//				Log.i("SIMGapWallet.java", "Creating Wallet instance...");
				_w = new Wallet();
//				Log.i("SIMGapWallet.java", "Wallet instance is created.");
			}catch (Throwable t)
			{
				Log.e("SIMGapWallet.java", "Wallet Creation Failure: " + t.getMessage());
				return false;
			}
			if (_w==null) return false;
		}
		try
		{
//			Log.i("SIMGapWallet.java", "Connecting Wallet...");
			log("EVENT_SIMGAPWALLET_WAITING", "" + fid + "|" + func);
			//0Thread t = new Thread() {
			//	public void run() {
					_w.openSEService(reactContext, WALLET_AID, WALLET_TAR, new SCSupported(){
						@Override
						public void isSupported(boolean success) {
if (USE_FAKE_DATA_WHEN_ERROR)
{
							if (!success) {log("EVENT_SIMGAPWALLET_WAIT_ERROR", "" + fid + "|Error: No connection."); }
} else
							if (!success) {log("EVENT_SIMGAPWALLET_WAIT_ERROR", "" + fid + "|Error: No connection."); return;}

							jobs.doJobs();
							_w.closeSEService();
							log("EVENT_SIMGAPWALLET_WAIT_CLOSE", "" + fid + "|" + func);
						}
					});

			//	}
			//};
			//t.start();
			return true;
		}catch(Throwable t)
		{
			Log.e("SIMGapWallet.java", "Function " + func + "() error", t);
			return false;
		}
	}


}