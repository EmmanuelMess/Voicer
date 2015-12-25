package com.emmanuelmess.voicer;

import android.app.Application;

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
public class Voicer extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		ACRA.init(this);
	}

}