package com.emmanuelmess.voicer;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * @author Emmanuel
 *         on 2015-03-08, at 02:44 PM
 */

@ReportsCrashes(
		formUri = "https://emmanuelmess.cloudant.com/acra-voicer/_design/acra-storage/_update/report",
		reportType = org.acra.sender.HttpSender.Type.JSON,
		httpMethod = org.acra.sender.HttpSender.Method.PUT,
		formUriBasicAuthLogin="adeforgedstaryingralturt",
		formUriBasicAuthPassword="8611ffa302dad27941f2b0a9b9a279e7a393f190",
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_toast_text)
public class VoicerApplication extends Application {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		if(!BuildConfig.DEBUG)
			ACRA.init(this);
	}

}