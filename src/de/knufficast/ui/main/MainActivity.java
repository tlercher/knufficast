/*******************************************************************************
 * Copyright 2012 Crazywater
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.knufficast.ui.main;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import de.knufficast.App;
import de.knufficast.R;
import de.knufficast.logic.model.DBEpisode;
import de.knufficast.logic.model.DBFeed;
import de.knufficast.logic.model.Queue;
import de.knufficast.player.QueuePlayer;
import de.knufficast.ui.episode.EpisodeDetailActivity;
import de.knufficast.ui.feed.FeedDetailActivity;
import de.knufficast.ui.settings.SettingsActivity;

/**
 * The main activity, showing the "Feeds" and "Queue" fragments. Mostly
 * auto-generated by Eclipse.
 * 
 * @author crazywater
 */
public class MainActivity extends FragmentActivity implements
    ActionBar.TabListener, FeedsFragment.Presenter, QueueFragment.Presenter {

  private static final int FEEDS_TAB = 1;

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
   * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
   * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
   * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;

  private QueueFragment queueFragment;
  private FeedsFragment feedsFragment;
  private final QueuePlayer queuePlayer = App.get().getPlayer();
  private final Queue queue = App.get().getQueue();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    queueFragment = new QueueFragment();
    feedsFragment = new FeedsFragment();

    setContentView(R.layout.activity_main);
    // Create the adapter that will return a fragment for each of the three primary sections
    // of the app.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the action bar.
    final ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    actionBar.setHomeButtonEnabled(false);

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.pager);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    // When swiping between different sections, select the corresponding tab.
    // We can also use ActionBar.Tab#select() to do this if we have a reference to the
    // Tab.
    mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        actionBar.setSelectedNavigationItem(position);
      }
    });

    // For each of the sections in the app, add a tab to the action bar.
    for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
      // Create a tab with text corresponding to the page title defined by the adapter.
      // Also specify this Activity object, which implements the TabListener interface, as the
      // listener for when this tab is selected.
      actionBar.addTab(
          actionBar.newTab()
          .setText(mSectionsPagerAdapter.getPageTitle(i))
          .setTabListener(this));
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Uri uri = getIntent().getData();
    if (uri != null) {
      feedsFragment.prepareForFeedText(uri.toString());
    }

    // auto-move to feeds tab if no feeds added or adding new feed
    if (uri != null || App.get().getConfiguration().getAllFeeds().size() == 0) {
      mViewPager.setCurrentItem(FEEDS_TAB, true);
    }
  }

  @Override
  public void onStop() {
    App.get().save();
    super.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    queuePlayer.releaseIfNotPlaying();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in the ViewPager.
    mViewPager.setCurrentItem(tab.getPosition());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_settings:
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    case R.id.menu_refresh_feeds:
      // hack to pass an unused argument... one null doesn't work :D
      new ToastRefresherTask(this).execute(null, null);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
   * sections of the app.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    BaseFragment[] tabs = { queueFragment, feedsFragment };

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int i) {
      return tabs[i];
    }

    @Override
    public int getCount() {
      return tabs.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return getString(tabs[position].getTitleId()).toUpperCase();
    }
  }

  @Override
  public void feedClicked(DBFeed feed) {
    Intent intent = new Intent(this, FeedDetailActivity.class);
    intent.putExtra(FeedDetailActivity.FEED_ID_INTENT, feed.getId());
    startActivity(intent);
  }

  @Override
  public void episodeClicked(DBEpisode episode) {
    Intent intent = new Intent(this, EpisodeDetailActivity.class);
    intent.putExtra(EpisodeDetailActivity.EPISODE_ID_INTENT, episode.getId());
    startActivity(intent);
  }

  @Override
  public void playClicked() {
    queuePlayer.togglePlaying();
  }

  @Override
  public void seekTo(int progress) {
    queuePlayer.seekTo(progress);
  }

  @Override
  public void moveEpisode(DBEpisode episode, int to) {
    queue.move(episode, to);
  }

  @Override
  public void removeEpisode(DBEpisode episode) {
    queue.remove(episode);
  }

  @Override
  public void onTabReselected(Tab tab, FragmentTransaction ft) {
  }

  @Override
  public void onTabUnselected(Tab tab, FragmentTransaction ft) {
  }
}
