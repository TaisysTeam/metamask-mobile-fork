package io.metamask.nativeModules;

import java.util.Calendar;

/**
 * Timeout solution to perform long-running task. A forked thread will be created to run the
 * long-running task, the long-running task thread will be interrupted in a specific time duration.
 */
public class HeavyDuty
{
	/** Returned code of <code>waitFor()</code>, represents task is finished without task errors **/
	public final static int WAITFOR_COMPLETE  = 0;
	/** Returned code of <code>waitFor()</code>, represents task is interrupted for timeout **/
	public final static int WAITFOR_TIMEOUT   = 0x400;
	/** Returned code of <code>waitFor()</code>, represents task is not run for resource busy **/
	public final static int WAITFOR_BUSY      = 0x401;
	/** Returned code of <code>waitFor()</code>, represents task is not run for bad parameters **/
	public final static int WAITFOR_BAD_PARAM = 0x402;
	/** Returned code of <code>waitFor()</code>, represents task is interrupted by exception **/
	public final static int WAITFOR_EXCEPTION = 0x403;

	/**
	 * Interface to be implemented for hold long-running task
	 */
	public static interface IHeavyDuty
	{
		/**
		 * Do longtime-running work here
		 **/
		public void duty();
	}

	/**
	 * Extended interface of <code>IHeaveDuty</code>, added method <code>onReturn</code>
	 */
	public static interface IHeavyDuty2 extends IHeavyDuty {
		/**
		 * Callback method will be called at the end of <code>waitFor()</code>,
		 * to do something according vary result of original<code>waitFor()</code>
		 * returned value, and convert to new return values.
		 * @param retCode    <code>waitFor()</code> original return value, see WAITFOR_*
		 * @return           Converted return code.
		 */
		public int onReturn(int retCode);
	}


	public HeavyDuty(){}

	/**
	 * Fetch error message of the last running of static method <code>waitFor</code> and delete it.
	 * This method can only be called once.
	 * @return    Error message
	 */
	public String popLastErrorMsg() {String s = _err_msg; _err_msg =null; return s;}

	/**
	 * Fetch <code>Throwable</code> object of the last running of static method <code>waitFor</code>
	 * and delete it. This method can only be called once.
	 * @return    <code>Throwable</code> object, null if no exception happened
	 */
	public Throwable popLastThrow() {Throwable t = _err_thw; _err_thw = null; return t;}


	// Default wait for 10 seconds

	/**
	 * <H1>Note</H1>
	 * Static methods <code>waitFor(IHeavyDuty2 d [, int timeout])</code>can only be called
	 * synchronously, asynchronous calling will be refused by returning <code>WAITFOR_BUSY</code>.
	 * <BR>
	 * Wait a long-running task to finish for 10 seconds unless task is complete or error occurs.
	 * This method will check if the task is complete every 10 ms.
	 * @param d    Instance of <code>IHeavyDuty2</code> which performs long-running task in its
	 *             <code>duty()</code> method, and perform its finally process in method
	 *             <code>onReturn()</code>
	 * @return     same with returned value of <code>IHeavyDuty2.onReturn()</code>
	 */
	public static int waitFor(IHeavyDuty2 d                ) {return waitFor(d, 10000);}

	/**
	 * <H1>Note</H1>
	 * Static methods <code>waitFor(IHeavyDuty2 d [, int timeout])</code>can only be called
	 * synchronously, asynchronous calling will be refused by returning <code>WAITFOR_BUSY</code>.
	 * <BR>
	 * Wait a long-running task to finish for <code>timeout</code> milli-seconds unless task is
	 * complete or error occurs. This method will check if the task is complete every 10 ms, same
	 * with <code>waitFor(IHeavyDuty2 d, int timeout, 10L)</code>
	 * @param d         Instance of <code>IHeavyDuty2</code> which performs long-running task in its
	 *                  <code>duty()</code> method, and perform its finally process in method
	 *                  <code>onReturn()</code>
	 * @param timeout   milli-seconds to wait task finish. Negative and zero value will be refused.
	 * @return          Same with returned value of <code>IHeavyDuty2.onReturn()</code>
	 */
	public static int waitFor(IHeavyDuty2 d, int timeout) {return new HeavyDuty().waitFor(d, (long)timeout, 10L);}

	/**
	 * <H1>Note</H1>
	 * Instance methods <code>waitFor(IHeavyDuty2 d [, int timeout])</code>can only be called
	 * asynchronously<BR>
	 * Wait a long-running task to finish for <code>timeout</code> milli-seconds unless task is
	 * complete or error occurs. This method will check if the task is complete every
	 * <code>delay</code> ms.
	 * @param d         Instance of <code>IHeavyDuty2</code> which performs long-running task in its
	 *                  <code>duty()</code> method, and perform its finally process in method
	 *                  <code>onReturn()</code>
	 * @param timeout   milli-seconds to wait task finish. Negative and zero value will be refused.
	 * @param delay     Time duration for checking if long-running task is complete. Negative and
	 *                  zero value will be refused.
	 * @return          Same with returned value of <code>IHeavyDuty2.onReturn()</code>
	 */
	public int waitFor(IHeavyDuty2 d, long timeout, long delay)
	{
		_err_msg = null;
		_err_thw = null;
		if (_t_single!=null && _t_single.isAlive()) return d.onReturn(WAITFOR_BUSY);
		if (d==null || timeout<=0 || delay<=0) return d.onReturn(WAITFOR_BAD_PARAM);
		_t_single = new Thread(){
			public void run()
			{
				try {
//System.err.println(Util.getCompactTimeStamp() + " ----- DUTY START");
					d.duty();
//System.err.println(Util.getCompactTimeStamp() + " ----- DUTY END");
				} catch(Throwable t)
				{
					_err_thw = t;
				}
				signal_waiting = SIG_IDLE;
			}
		};
		signal_waiting = SIG_WAIT;
		int res = WAITFOR_EXCEPTION;
		try {
			_err_thw = null;
			_t_single.start();
			long ts = Calendar.getInstance().getTimeInMillis();
			while((Calendar.getInstance().getTimeInMillis()-ts)<=timeout)
			{
				if (signal_waiting == SIG_IDLE) break;
				if (!_t_single.isAlive()) {signal_waiting = SIG_IDLE; break;}
				Util.delay(delay);
			}
			//if (_t_single.isAlive())
			if (signal_waiting != SIG_IDLE)
			{
				try
				{
					res = WAITFOR_TIMEOUT;
					_t_single.interrupt();
				} catch (Exception e){}
			}
			else if (_err_thw!=null) res = WAITFOR_EXCEPTION;
			else res = WAITFOR_COMPLETE;
			signal_waiting = SIG_IDLE;
			_t_single = null;
			return d.onReturn(res);
		} catch (Throwable t)
		{
			_err_thw = t;
			if (_t_single!=null && _t_single.isAlive())
			{
				try
				{
					_t_single.interrupt();
				} catch (Exception e){}
			}
			signal_waiting = SIG_IDLE;
			_t_single = null;
			return d.onReturn(WAITFOR_EXCEPTION);
		}
	}






	private final static int SIG_IDLE = 0x26E2;
	private final static int SIG_WAIT = 0x379F;

	private Thread _t_single = null;
	private int signal_waiting = SIG_IDLE;
	private String _err_msg = null;
	private Throwable _err_thw = null;

}