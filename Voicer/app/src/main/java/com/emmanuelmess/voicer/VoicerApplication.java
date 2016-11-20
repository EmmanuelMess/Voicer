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
@ReportsCrashes(mailTo = "emmanuelbendavid@gmail.com",
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_0)
public class VoicerApplication extends Application {

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}

}