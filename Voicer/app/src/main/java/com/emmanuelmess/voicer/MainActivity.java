package com.emmanuelmess.voicer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.emmanuelmess.voicer.activities.DonationActivity;
import com.emmanuelmess.voicer.activities.SettingsActivity;

import org.acra.ACRA;

import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

	public static final String READABLE_TEXT = "text";
	public static final String ACRA_TEXT = "text";

	private SharedPreferences prefs;
	private TextView e;
	private SpeechThread thread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		final Context context = getApplicationContext();
		final AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		e = (TextView) findViewById(R.id.text);
		//e.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla varius tortor magna, ut mattis ligula venenatis id. Suspendisse potenti. Etiam vitae lacus ex. Sed felis lorem, tempor nec tortor a, tempor tristique libero. Cras egestas, elit semper tempor vestibulum, magna.");
		thread = new SpeechThread(getApplicationContext());
		thread.start();
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (thread.getStatus() == 1)
					Toast.makeText(context, context.getString(R.string.loaded), Toast.LENGTH_SHORT).show();
				else if (thread.getStatus() == TextToSpeech.ERROR)
					Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG).show();
				else if (("" + e.getText()).replace(" ", "").equals(""))
					Toast.makeText(context, context.getString(R.string.write), Toast.LENGTH_SHORT).show();
				else if (am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)
					Toast.makeText(context, context.getString(R.string.volume), Toast.LENGTH_SHORT).show();
				else {
					if(!BuildConfig.DEBUG)
						ACRA.getErrorReporter().putCustomData(ACRA_TEXT, e.getText().toString());

					if (!thread.getTTS().isSpeaking()) {
						thread.startTTS();

						synchronized (this) {
							notifyAll();
						}
					} else {
						thread.stopTTS();
					}
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		e.setText(prefs.getString(READABLE_TEXT, ""));
		if(!BuildConfig.DEBUG)
			ACRA.getErrorReporter().putCustomData(ACRA_TEXT, e.getText().toString());
	}

	@Override
	public void onPause() {
		super.onPause();

		//save text
		if(e.getText() != "")
			prefs.edit().putString(READABLE_TEXT, e.getText().toString()).apply();

		if(thread.getTTS().isSpeaking()) {
			thread.stopTTS();
		}

		if(!BuildConfig.DEBUG)
			ACRA.getErrorReporter().clearCustomData();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id) {
			case R.id.action_settings:
				startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
				return true;
			case R.id.action_donate:
				startActivity(new Intent(getApplicationContext(), DonationActivity.class));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static int speak(TextToSpeech t, final CharSequence text, final int queueMode, final Bundle params, final String utteranceId) {
		// Check if we're running on Android 5.0 or higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return t.speak(text, queueMode, params, utteranceId);
		else
			return t.speak(text.toString(), queueMode, null);
	}

	private class SpeechThread extends Thread implements TextToSpeech.OnInitListener {

		private static final String UTTERANCE_ID = "utterance_id";
		private static final int MAX_LENGTH = 2000;
		private TextToSpeech t;
		private int status;
		private volatile boolean speak;
		private final Object lock = new Object();
		private int timeBetweenWords;

		SpeechThread(Context context) {
			t = new TextToSpeech(context, this);

			timeBetweenWords = prefs.getInt(SettingsActivity.TIME_BETWEEN_WORDS_TEXT, 3);
			timeBetweenWords *= 1000;
		}

		@Override
		public void run() {
			while (!this.isInterrupted()) {
				if (speak) {
					String text = String.valueOf(e.getText());
					if (timeBetweenWords == 0) {
						speak(t, text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
					} else {
						int l = text.length();
						String[] subdividedText = new String[(int) Math.ceil((double) l/MAX_LENGTH)];

						for (int i = 0; i < subdividedText.length; i++)
							subdividedText[i] = text.substring((i)*MAX_LENGTH, (MAX_LENGTH < l - i*MAX_LENGTH? (i + 1)*MAX_LENGTH:l));

						loop0:
						for (String aSubdividedText : subdividedText) {
							Scanner s = new Scanner(aSubdividedText).useDelimiter(" ");

							while (s.hasNext()) {
								int time;
								synchronized (lock) {
									if (!speak) break loop0;

									String toRead = s.next();
									time = (int) (timeBetweenWords*(toRead.length()/5f));
									speak(t, toRead, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
								}

								try {
									sleep(time);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
							}

							while (t.isSpeaking()) {
								waiting();
							}
						}
					}
					speak = false;
				} else {
					try {
						sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}


		TextToSpeech getTTS() {
			return t;
		}

		void startTTS() {
			synchronized (lock) {
				speak = true;
			}
		}

		void stopTTS() {
			synchronized (lock) {
				speak = false;
			}
		}

		@Override
		public void onInit(int status) {
			this.status = status;
		}

		int getStatus() {
			return status;
		}

		private synchronized void waiting() {
			try {
				wait();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

}

