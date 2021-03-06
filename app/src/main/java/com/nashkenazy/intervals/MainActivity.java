package com.nashkenazy.intervals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements OnIntervalClick {

	public static final List<String> NOTES_FILE_NAMES = ImmutableList.of("a_0_1", "as_0_2", "b_0_3", "c_1_4", "cs_1_5", "d_1_6", "ds_1_7", "e_1_8", "f_1_9", "fs_1_10", "g_1_11", "gs_1_12", "a_1_13", "as_1_14", "b_1_15", "c_2_16", "cs_2_17", "d_2_18", "ds_2_19", "e_2_20", "f_2_21", "fs_2_22", "g_2_23", "gs_2_24", "a_2_25", "as_2_26", "b_2_27", "c_3_28", "cs_3_29", "d_3_30", "ds_3_31", "e_3_32", "f_3_33", "fs_3_34", "g_3_35", "gs_3_36", "a_3_37", "as_3_38", "b_3_39", "c_4_40", "cs_4_41", "d_4_42", "ds_4_43", "e_4_44", "f_4_45", "fs_4_46", "g_4_47", "gs_4_48", "a_4_49", "as_4_50", "b_4_51", "c_5_52", "cs_5_53", "d_5_54", "ds_5_55", "e_5_56", "f_5_57", "fs_5_58", "g_5_59", "gs_5_60", "a_5_61", "as_5_62", "b_5_63", "c_6_64", "cs_6_65", "d_6_66", "ds_6_67", "e_6_68", "f_6_69", "fs_6_70", "g_6_71", "gs_6_72", "a_6_73", "as_6_74", "b_6_75", "c_7_76", "cs_7_77", "d_7_78", "ds_7_79", "e_7_80", "f_7_81", "fs_7_82", "g_7_83", "gs_7_84", "a_7_85", "as_7_86", "b_7_87", "c_8_88");
	final Random RAND = new Random();
	final private Handler handler = new Handler();
	final private int INTERVAL_DELAY = 600;
	int randomIntervalType = -1;
	private int intervalDistance;
	private int lowerNote;
	private int upperNote;
	private SharedPreferences sharedPref;
	private MediaPlayer mediaPlayer;
	private List<Interval> includedIntervalTypes;
	private String intervalType;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		intervalType = sharedPref.getString(SettingsActivity.KEY_PREF_PLAYBACK_SEQUENCES, "All");

		// Set the answer according to the settings
		includedIntervalTypes = new ArrayList<>();
		for (String interval : sharedPref.getStringSet(SettingsActivity.KEY_PREF_INCLUDED_INTERVAL_TYPES, null)) {
			includedIntervalTypes.add(new Interval(Integer.parseInt(interval)));
		}
		// Avoid crashing if user selects no included intervals
		if (includedIntervalTypes.size() < 1) {
			includedIntervalTypes.add(new Interval(0));
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putStringSet(SettingsActivity.KEY_PREF_INCLUDED_INTERVAL_TYPES, Sets.newHashSet("0"));
			editor.apply();
		}

		Collections.sort(includedIntervalTypes, (lhs, rhs) -> lhs.getSemitones() - rhs.getSemitones()); // Makes sure user sees options in order

		// Set answer options
		RecyclerView recyclerView = findViewById(R.id.my_recycler_view);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
		recyclerView.setAdapter(new IntervalChoicesAdapter(includedIntervalTypes, this));

		setNewRound();

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// onClick for answer choice from adapter
	@Override
	public void onClick(int semitone) {
		stopAllAudio();
		if (semitone == intervalDistance) {
			MaterialButton nextIntervalBtn = findViewById(R.id.button_next_interval);
			nextIntervalBtn.setEnabled(true);
			playNote(88);
		} else {
			playNote(1);
		}
	}

	//  Does not setup elements in RecyclerView as it must be implemented in the class
	private void setNewRound() {

		MaterialButton repeatIntervalBtn = findViewById(R.id.button_repeat);
		MaterialButton nextIntervalBtn = findViewById(R.id.button_next_interval);

		final int octaveSetting = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_OCTAVE, "4"));
		final String positionSetting = sharedPref.getString(SettingsActivity.KEY_PREF_POSITION, "Lower");
		final int noteSetting = Integer.parseInt(sharedPref.getString(SettingsActivity.KEY_PREF_NOTE, "4"));


		nextIntervalBtn.setOnClickListener(v -> {

			// Prevents user from repeating interval when no interval is set
			if (!repeatIntervalBtn.isEnabled()) {
				repeatIntervalBtn.setEnabled(true);
			}

			nextIntervalBtn.setEnabled(false);


			intervalDistance = includedIntervalTypes.get(RAND.nextInt(includedIntervalTypes.size())).getSemitones();

			// Set the answer according to the settings
			// 0 indicates random
			int noteOffset = noteSetting == -1 ? RAND.nextInt(12) + 1 : noteSetting;
			int octaveOffset = octaveSetting == -1 ? RAND.nextInt(5) + 2 : octaveSetting;

			switch (positionSetting) {
				case "Upper":
					upperNote = 12 * (octaveOffset - 1) + noteOffset;
					lowerNote = upperNote - intervalDistance;
					break;
				default:
					lowerNote = 12 * (octaveOffset - 1) + noteOffset;
					upperNote = lowerNote + intervalDistance;
			}
			playInterval(true);
		});

		repeatIntervalBtn.setOnClickListener(v1 -> playInterval(false));
	}

	private void stopAllAudio() {
		if (mediaPlayer != null) { mediaPlayer.release(); }
		handler.removeCallbacksAndMessages(null);
	}

	private void playNote(int noteNumber) {
		mediaPlayer = MediaPlayer.create(this, getResources().getIdentifier(NOTES_FILE_NAMES.get(noteNumber - 1), "raw", getPackageName()));
		mediaPlayer.setOnCompletionListener(MediaPlayer::release);
		mediaPlayer.start();
	}

	private void playNoteAfterDelay(final int noteNumber, int delay) {
		handler.postDelayed(() -> playNote(noteNumber), delay);
	}

	private void playInterval(boolean firstTimeNewRound) {
		stopAllAudio();

		// Check only the first three letters to reduce size
		// Based on interval type setting
		switch (intervalType.substring(0,3)) {
			case "Low":
				playAscendingInterval();
				break;
			case "Upp":
				playDescendingInterval();
				break;
			case "Sim":
				playHarmonicInterval();
				break;
			case "All":
				playAllIntervalTypes();
				break;
			case "Ran":
				List<IntervalType> intervalTypes = ImmutableList.of(this::playAscendingInterval, this::playDescendingInterval, this::playHarmonicInterval, this::playAllIntervalTypes);

				// Makes sure repeatIntervalBtn plays the same interval in the round
				if (firstTimeNewRound) { randomIntervalType = RAND.nextInt(intervalTypes.size()); }
				intervalTypes.get(randomIntervalType).playInterval();
				break;
		}

	}

	private void playAllIntervalTypes() {
		playNote(lowerNote);
		playNoteAfterDelay(upperNote, INTERVAL_DELAY);

		playNoteAfterDelay(upperNote, INTERVAL_DELAY * 3);
		playNoteAfterDelay(lowerNote, INTERVAL_DELAY * 4);

		playNoteAfterDelay(lowerNote, INTERVAL_DELAY * 6);
		playNoteAfterDelay(upperNote, INTERVAL_DELAY * 6);
	}

	private void playAscendingInterval() {
		playNote(lowerNote);
		playNoteAfterDelay(upperNote, INTERVAL_DELAY);
	}

	private void playDescendingInterval() {
		playNote(upperNote);
		playNoteAfterDelay(lowerNote, INTERVAL_DELAY);
	}

	private void playHarmonicInterval() {
		playNote(lowerNote);
		playNote(upperNote);
	}

}
