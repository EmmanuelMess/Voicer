package com.emmanuelmess.voicer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.emmanuelmess.voicer.activities.SettingsActivity;

import java.util.HashMap;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

	private TextView e;
	private SpeechThread thread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		final Context context = getApplicationContext();
		final AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		e = (TextView) findViewById(R.id.text);
		//e.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla varius tortor magna, ut mattis ligula venenatis id. Suspendisse potenti. Etiam vitae lacus ex. Sed felis lorem, tempor nec tortor a, tempor tristique libero. Cras egestas, elit semper tempor vestibulum, magna.");
		thread = new SpeechThread(getApplicationContext(), this);
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
	public void onPause() {
		super.onPause();

		if(thread.getTTS().isSpeaking()) {
				thread.stopTTS();
		}
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
		if (id == R.id.action_settings) {
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static int speak(TextToSpeech t, final CharSequence text, final int queueMode, final Bundle params, final String utteranceId) {
		// Check if we're running on Android 5.0 or higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return t.speak(text, queueMode, params, utteranceId);
		} else {
			HashMap<String, String> hashParams;

			if (params == null) hashParams = null;
			else throw new IllegalArgumentException();

			return t.speak(text.toString(), queueMode, hashParams);
		}
	}

	class SpeechThread extends Thread implements TextToSpeech.OnInitListener {

		private static final String UTTERANCE_ID = "utterance_id";
		private static final int MAX_LENGTH = 2000;
		private TextToSpeech t;
		private int status;
		private volatile boolean speak;
		private final Object lock = new Object();
		private int timeBetweenWords;

		public SpeechThread(Context context, AppCompatActivity activity) {
			SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

			t = new TextToSpeech(context, this);

			if (sharedPref.getString(SettingsActivity.TIME_BETWEEN_WORDS_TEXT, "") != "")
				timeBetweenWords = Integer.valueOf(sharedPref.getString(SettingsActivity.TIME_BETWEEN_WORDS_TEXT, ""));
			else timeBetweenWords = 3;

			timeBetweenWords *= 1000;
		}

		@Override
		public void run() {
			while(!this.isInterrupted()) {
				if (speak) {
					String text = String.valueOf(e.getText());
					int l = text.length();
					String [] subdividedText = new String [(int) Math.ceil((double)l/MAX_LENGTH)];

					for(int i = 0; i < subdividedText.length; i++)
						subdividedText[i] = text.substring((i)*MAX_LENGTH, (MAX_LENGTH < l - i*MAX_LENGTH? (i+1)*MAX_LENGTH:l));

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

		public TextToSpeech getTTS() {
			return t;
		}

		public void startTTS() {
			synchronized (lock) {
				speak = true;
			}
		}

		public void stopTTS() {
			synchronized (lock) {
				speak = false;
			}
		}

		@Override
		public void onInit(int status) {
			this.status = status;
		}

		public int getStatus() {
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
