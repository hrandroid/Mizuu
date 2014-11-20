/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.mizuu;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.MizLib;
import com.miz.mizuu.fragments.TvShowEpisodeDetailsFragment;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public class TvShowEpisodeDetails extends MizActivity {

	private static final String SHOW_ID = "showId";

	private ArrayList<TvShowEpisode> mEpisodes = new ArrayList<TvShowEpisode>();
	private int mSeason, mEpisode;
	private String mShowId, mShowTitle;
	private ViewPager mViewPager;
	private DbAdapterTvShowEpisodes mDatabaseHelper;
	private Bus mBus;
	private Drawable mActionBarBackgroundDrawable;
	private ImageView mActionBarOverlay;
	private ActionBar mActionBar;

	@Override
	protected int getLayoutResource() {
		return R.layout.viewpager;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		mBus = MizuuApplication.getBus();

		if (isFullscreen())
			setTheme(R.style.Mizuu_Theme_NoBackground_FullScreen);
		else
			setTheme(R.style.Mizuu_Theme_NoBackground);

		if (MizLib.isPortrait(this)) {
			getWindow().setBackgroundDrawableResource(R.drawable.bg);
		}

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);
		
		mActionBar = getActionBar();

		mShowId = getIntent().getExtras().getString(SHOW_ID);
		mSeason = getIntent().getExtras().getInt("season");
		mEpisode = getIntent().getExtras().getInt("episode");

		mDatabaseHelper = MizuuApplication.getTvEpisodeDbAdapter();

		Cursor cursor = mDatabaseHelper.getAllEpisodes(mShowId, DbAdapterTvShowEpisodes.OLDEST_FIRST);	
		try {
			while (cursor.moveToNext()) {
				mEpisodes.add(new TvShowEpisode(this, mShowId,
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
						));
			}
		} catch (Exception e) {
		} finally {
			cursor.close();
		}

		mShowTitle = MizuuApplication.getTvDbAdapter().getShowTitle(mShowId);
		mActionBar.setTitle(mShowTitle);

		mViewPager = (ViewPager) findViewById(R.id.awesomepager);
		mViewPager.setAdapter(new TvShowEpisodeDetailsAdapter(getSupportFragmentManager()));
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				updateActionBar(position);

				updateActionBarDrawable(1, true, false);
			}
		});

		if (savedInstanceState != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
		} else {
			for (int i = 0; i < mEpisodes.size(); i++) {
				if (mEpisodes.get(i).getSeason().equals(MizLib.addIndexZero(mSeason)) && mEpisodes.get(i).getEpisode().equals(MizLib.addIndexZero(mEpisode))) {
					mViewPager.setCurrentItem(i);
					updateActionBar(i);
					continue;
				}
			}
		}
	}

	@Subscribe
	public void onScrollChanged(Integer newAlpha) {
		updateActionBarDrawable(newAlpha, true, false);
	}

	private void updateActionBarDrawable(int newAlpha, boolean setBackground, boolean showActionBar) {
		mActionBarOverlay.setVisibility(View.VISIBLE);

		if (MizLib.isPortrait(this) && !MizLib.isTablet(this) && !MizLib.usesNavigationControl(this))
			if (newAlpha == 0) {
				mActionBar.hide();
				mActionBarOverlay.setVisibility(View.GONE);
			} else
				mActionBar.show();

		if (setBackground) {
			mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "000000"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "000000") : 0xaa000000});
			mActionBarOverlay.setImageDrawable(mActionBarBackgroundDrawable);
		}

		if (showActionBar) {
			mActionBar.show();
		}
	}

	public void onResume() {
		super.onResume();

		mBus.register(this);
		updateActionBarDrawable(1, true, true);
	}

	@Override
	public void onPause() {
		super.onPause();

		mBus.unregister(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("tab", mViewPager.getCurrentItem());
	}

	private void updateActionBar(int position) {
		mActionBar.setSubtitle("S" + mEpisodes.get(position).getSeason() + "E" + mEpisodes.get(position).getEpisode());
	}

	private class TvShowEpisodeDetailsAdapter extends FragmentPagerAdapter {

		public TvShowEpisodeDetailsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override  
		public Fragment getItem(int index) {
			return TvShowEpisodeDetailsFragment.newInstance(mShowId, Integer.parseInt(mEpisodes.get(index).getSeason()), Integer.parseInt(mEpisodes.get(index).getEpisode()));
		}  

		@Override  
		public int getCount() {  
			return mEpisodes.size();
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				setResult(Activity.RESULT_OK);
				finish();
			}
		}
	}
}