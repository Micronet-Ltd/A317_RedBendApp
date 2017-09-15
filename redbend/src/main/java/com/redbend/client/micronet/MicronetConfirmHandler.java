///////////////////////////////////////////////
/**
 * Created by dschmidt on 7/27/16.
 */
// Contains micronet-specific code for the Redbend Client.
//  Well, some of it. The base code was probably modified before this file was created to allow:
//      1) placing files on file system instead of installing apks
//      2) something unqiue about serial numbers and device IDs ?




// Changes Required to Client:
//      register these event handlers in ClientService.java, and remove many other registered handlers to UI components, etc.


///////////////////////////////////////////////


package com.redbend.client.micronet;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.redbend.app.Event;

import com.redbend.app.EventHandler;
import com.redbend.client.ClientService;

import com.redbend.client.R;

import java.util.Calendar;
import java.util.Date;

/**
 *  Confirms or Denies requests from the RBC
 */
public class MicronetConfirmHandler  {

    public static final String LOG_TAG = "RBC-MNConfirmHandler";


    public static final String MY_PREFS_NAME = "postponements";

    //////////////////////////////////////////////////////////
    // isInstallLocked()
    //  is the Installation currently blocked because other applications are holding locks?
    //////////////////////////////////////////////////////////
    private static boolean isInstallLocked(Context context) {
        MicronetMySQLiteHelper dbHelper = new MicronetMySQLiteHelper(context);

        long elapsed_now_s = SystemClock.elapsedRealtime() / 1000;

        long max_expires_s = dbHelper.getMaxExpirationElapsedSec();


        dbHelper.close();




        if (max_expires_s > elapsed_now_s) {

            Log.d(LOG_TAG, "Installation is locked for " + (max_expires_s - elapsed_now_s) + " more seconds (until " + max_expires_s + "s post-boot)" );

            return true;
        }

        return false;

    }


    //////////////////////////////////////////////////////////
    // isLockedTooLong()
    //  Have blocking locks been held too long (an indication that something is wrong and we should proceed w/ operation anyway)
    //////////////////////////////////////////////////////////
    private static boolean isInstallLockedTooLong(Context context) {

        // the lock is "too long" if:

        //    a) the last 100 installation checks had a bad date (indicating no RTC and at least one hour uptime without any date).
        //    b) we've had 4 installation checks with good dates on 4 different days


        PostponeInfo pp = getPostponeInfo(context);


        if (pp.bad_postpone_counter >= pp.max_bad_postpone_counter) {
            Log.i(LOG_TAG, "Locked Too Long. Postponed > " + pp.max_bad_postpone_counter + " times with no known current date.");
            return true;
        }

        if (pp.good_postpone_days_counter >= pp.max_good_postpone_days_counter) {
            Log.i(LOG_TAG, "Locked Too Long. Postponed on > " + pp.max_good_postpone_days_counter + " different days.");
            return true;
        }

        return false;

    } // isLockedTooLong()


    static final int DEFAULT_MAX_GOOD_POSTPONE_DAYS_COUNTER = 4; // max number of different days of postpones w/ good date, before finsihing an operation
    static final int DEFAULT_MAX_BAD_POSTPONE_COUNTER = 100; // max number of postpones w/o good date, before finishing an operation

    static class PostponeInfo {

        int bad_postpone_counter = 0;

        int good_postpone_counter = 0;
        long last_good_postpone_unix_s = 0;
        int good_postpone_days_counter = 0;


        // Maximum values to allow before accepting or canceling the campaign
        int max_good_postpone_days_counter = DEFAULT_MAX_GOOD_POSTPONE_DAYS_COUNTER;
        int max_bad_postpone_counter = DEFAULT_MAX_BAD_POSTPONE_COUNTER;


    }


    private static boolean isDateGood(long date_unix_s) {

        // If the current date is earlier than a cutoff date of 2016-01-01, then this is a BAD date.
        //      (It means we don't know the real date)

        Calendar cal  = Calendar.getInstance();
        cal.set(2016,1,1, 0,0);
        long cutoff_unix_s = cal.getTimeInMillis() / 1000L;

        if (date_unix_s < cutoff_unix_s) return false;
        return true;
    }


    private static PostponeInfo getPostponeInfo(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, context.MODE_PRIVATE);

        PostponeInfo pp = new PostponeInfo();

        pp.bad_postpone_counter = prefs.getInt("badPostponeCounter", 0); // counter of bad times

        pp.good_postpone_counter = prefs.getInt("goodPostponeCounter", 0); // counter of good times
        pp.good_postpone_days_counter = prefs.getInt("goodPostponeDaysCounter", 0); // counter of good days
        pp.last_good_postpone_unix_s = prefs.getLong("lastGoodPostponeUnixDate", 0); // last good time


        pp.max_good_postpone_days_counter = prefs.getInt("maxGoodPostponeDaysCounter", DEFAULT_MAX_GOOD_POSTPONE_DAYS_COUNTER);
        pp.max_bad_postpone_counter = prefs.getInt("maxBadPostponeCounter",DEFAULT_MAX_BAD_POSTPONE_COUNTER);


        return pp;

    } // getPostponeInfo()


    //////////////////////////////////////////////////////////
    // forgetPostpone()
    //  called when an operation is accepted or cancelled to clear out the record keeping track of postponements
    //////////////////////////////////////////////////////////
    private static void forgetPostpone(Context context) {

        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, context.MODE_PRIVATE).edit();

        PostponeInfo pp = new PostponeInfo();

        editor.putInt("badPostponeCounter", pp.bad_postpone_counter);

        editor.putInt("goodPostponeCounter", pp.good_postpone_counter);
        editor.putInt("goodPostponeDaysCounter", pp.good_postpone_days_counter);
        editor.putLong("lastGoodPostponeUnixDate", pp.last_good_postpone_unix_s);

        editor.apply();
    } // forgetPostpone()

    //////////////////////////////////////////////////////////
    // rememberPostpone()
    //  called when an operation is postponed to remember details about it
    //////////////////////////////////////////////////////////
    private static void rememberPostpone(Context context) {

        PostponeInfo pp = getPostponeInfo(context);


        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, context.MODE_PRIVATE).edit();
        long now = System.currentTimeMillis() / 1000L;

        if (isDateGood(now)) {
            // has the date changed between saved date and now ?

            // increment the number of postpones with a good date
            pp.good_postpone_counter++;
            editor.putInt("goodPostponeCounter", pp.good_postpone_counter);

            long midnight = (pp.last_good_postpone_unix_s / (24*60*60)) * (24*60*60) + 24*60*60;

            if (now > midnight) {
                // we've changed days
                pp.good_postpone_days_counter++;
                Log.d(LOG_TAG, "Operation has been postponed on " + pp.good_postpone_days_counter + " different utc days");
                editor.putInt("goodPostponeDaysCounter", pp.good_postpone_days_counter);
            }

            // just remember this time for next time
            editor.putLong("lastGoodPostponeUnixDate", now);
        } else {
            // Date is not good, just increment the number of postpones with a bad date
            pp.bad_postpone_counter++;
            Log.d(LOG_TAG, "Operation has been postponed " + pp.bad_postpone_counter + " times without a date");
            editor.putInt("badPostponeCounter", pp.bad_postpone_counter);
        }

        editor.apply();
    } // rememberPostpone()


    //////////////////////////////////////////////////////////
    // Overriding the locks

    private static int OVERRIDE_PERIOD_MS = 120000; // 120 seconds

    private static boolean tempOverrideLocks = false; // if true, temporarily ignore installation locks and allow installation


    private static Handler handler = new Handler();

    //////////////////////////////////////////////////////////
    // areLocksOverridden()
    //  is the Installation currently allowed because all installation locks were temporarily overridden?
    //////////////////////////////////////////////////////////
    private static boolean areLocksOverridden() {
        return tempOverrideLocks;
    }


    public static void overrideLocksNow() {
        // remove prior calls to the timer
        handler.removeCallbacks(deoverrideLocksTimer);
        handler.postDelayed(deoverrideLocksTimer, OVERRIDE_PERIOD_MS);

        tempOverrideLocks = true;

        Log.i(LOG_TAG, "Installation Locks are overridden for " + (OVERRIDE_PERIOD_MS / 1000 ) + " seconds");

    }

    /**
     * Locks are overriden so long as this timer has not expired
     */
    private static final Runnable deoverrideLocksTimer = new Runnable(){
        public void run(){
            try {
                //prepare and send the data here..
                Log.i(LOG_TAG, "Override Period has expired. Installation Locks are in effect again.");
                tempOverrideLocks = false;
            }
            catch (Exception e) {
                Log.e(LOG_TAG, "Exception on deoverrideLocksTimer()");
                e.printStackTrace();
            }
        }
    };

/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////
// HANDLERS
/////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////


    //////////////////////////////////
    // getRebootRequestHandler()
    //  returns an EventHandler for a reboot request
    //      Mimics a user action on confirming a reboot
    //      immediately respond that it is ok to reboot
    //////////////////////////////////
    public static EventHandler getRebootRequestHandler(final ClientService cs) {

        // Always accept reboot requests immediately

        return new EventHandler(cs) {
            @Override
            protected void genericHandler(Event ev) {

                Log.d(LOG_TAG, "Confirming reboot request w/ DMA_MSG_SCOMO_ACCEPT");
                cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
            }
        };
    }




    //////////////////////////////////
    // getNotifyDownloadHandler()
    //  returns an EventHandler for a download-type notification event (e.g. "Something is available for download")
    //      Mimics a user action on confirming a download.
    //      immediately respond that we got the notification
    //////////////////////////////////
    public static EventHandler getNotifyDownloadHandler(final ClientService cs) {

        // Always acknowledge we have seen this notification immediately

        return new EventHandler(cs) {
            @Override
            protected void genericHandler(Event ev) {

                Log.d(LOG_TAG, "Confirming download available w/ DMA_MSG_SCOMO_NOTIFY_DL");
                cs.sendEvent(new Event("DMA_MSG_SCOMO_NOTIFY_DL"));
            }
        };
    }



    //////////////////////////////////
    // getConfirmDownloadHandler()
    //  returns an EventHandler for a download-type event
    //      Mimics a user action on confirming a download.
    //      These are always accepted.
    //////////////////////////////////
    public static EventHandler getConfirmDownloadHandler(final ClientService cs) {


        // We always accept download requests immediately.


        return new EventHandler(cs) {
            @Override
            protected void genericHandler(Event ev) {

                Log.d(LOG_TAG, "Confirming download w/ DMA_MSG_SCOMO_ACCEPT");
                cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
            }
        };
    }


    //////////////////////////////////
    // getConfirmInstallHandler()
    //  returns an EventHandler for an install-type event
    //      Mimics a user action on confirming an installation
    //      These are accepted so long as no other apps have a no-install lock, otherwise they are postponed
    //////////////////////////////////

    public static EventHandler getConfirmInstallHandler(final ClientService cs) {

          return new EventHandler(cs) {
                @Override
                protected void genericHandler(Event ev) {

                    boolean isSilent = ev.getVarValue("DMA_VAR_SCOMO_ISSILENT") == 1;
                    boolean isCritical = ev.getVarValue("DMA_VAR_SCOMO_CRITICAL") != 0;

                    Context context = cs.getApplicationContext();


                    // Critical operations
                    // Critical operations are always accepted immediately

                    if (isCritical) {

                        // Installation is critical --> install immediately

                        Log.d(LOG_TAG, "Confirming critical install w/ DMA_MSG_SCOMO_ACCEPT");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
                        forgetPostpone(context);
                        return;
                    }

                    // Non-Critical operations

                    if (areLocksOverridden()) {

                        // we have temporarily overridden all installation locks -> Install immediately
                        Log.d(LOG_TAG, "Confirming non-critical install w/ DMA_MSG_SCOMO_ACCEPT b/c all locks are overridden");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
                        forgetPostpone(context);
                        return;

                    }



                    // Non-Critical applications are not installed right after boot
                    // Wait 90 seconds after boot-up before confirming
                    //  This allows other applications to re-acquire their installation locks
                    long elapsed_now_s = SystemClock.elapsedRealtime() / 1000;

                    if (elapsed_now_s < 90) {

                        Log.d(LOG_TAG, "Postponing non-critical install w/ DMA_MSG_SCOMO_POSTPONE b/c too close to boot (" + elapsed_now_s + "s)" );
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_POSTPONE"));
                        rememberPostpone(context);
                        return;

                    }







                    // Non-Critical operations can be locked (blocked by other applications)


                    if (!isInstallLocked(context)) {

                        // Installation is not locked --> install immediately

                        Log.d(LOG_TAG, "Confirming non-critical install w/ DMA_MSG_SCOMO_ACCEPT b/c no locks are held");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
                        forgetPostpone(context);
                        return;
                    }


                    // Locked operations
                    // Locked operations automatically unlock after a certain period of time

                    if (!isInstallLockedTooLong(context)) {

                        // We haven't been locked too long --> keep postponing

                        Log.d(LOG_TAG, "Postponing non-critical install w/ DMA_MSG_SCOMO_POSTPONE b/c locked");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_POSTPONE"));
                        rememberPostpone(context);
                        return;
                    }



                    Log.d(LOG_TAG, "Non-critical install has been blocked too long!!");

                    // Silent operations:
                    // Silent operations will fail if they are blocked too long (so they don't interrupt user)
                    // Non-Silent operations will install if they are blocked for too long (which could interrupt user),

                    if (isSilent) {
                        Log.d(LOG_TAG, "Cancelling operation b/c it is SILENT.");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_CANCEL"));
                    } else {
                        Log.d(LOG_TAG, "Accepting operation b/c it is not SILENT");
                        cs.sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
                    }
                    forgetPostpone(context);

                } // genericHandler() function
          }; // return event handler
    } // getConfirmInstallHandler()


    //////////////////////////////////
    // getConfirmDLTimeSlotHandler()
    //  returns an event handler for an operation that requires the correct time-of-day to download (download timeslot)
    //      This immediately sends an event indicating the alarm has expired, eliminating the download timeslot functionality
    //////////////////////////////////
    public static EventHandler getConfirmDLTimeSlotHandler(final ClientService cs) {
        return new EventHandler(cs) {
            @Override
            protected void genericHandler(Event ev) {


                Event event1 = new Event("DMA_MSG_SCOMO_SET_DL_TIMESLOT_DONE");
                // first confirm that we processed the event and set the alarm (even though we didn't)
                Log.d(LOG_TAG, "Confirming Timeslot Alarm Set w/ " + event1.getName());
                cs.sendEvent(event1);

                // now send the event saying that the alarm expired and we are currently in the timeslot (even though we aren't)
                Event event2 = new Event("DMA_MSG_DL_TIMESLOT_TIMEOUT");
                Log.d(LOG_TAG, "Pretending Timeslot Alarm expired w/ " + event2.getName());
                cs.sendEvent(event2);
            }
        };

    } // getConfirmDLTimeSlotHandler()


} // class MicronetConfirm()
