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
package de.knufficast.ui.search;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParserException;

import de.knufficast.App;
import de.knufficast.R;
import de.knufficast.events.EventBus;
import de.knufficast.events.Listener;
import de.knufficast.events.NewImageEvent;
import de.knufficast.logic.AddFeedTask;
import de.knufficast.logic.FeedDownloader;
import de.knufficast.logic.GetFeedTask;
import de.knufficast.logic.xml.XMLFeed;
import de.knufficast.search.ITunesLookup;
import de.knufficast.search.ITunesSearch;
import de.knufficast.search.PodcastSearch;
import de.knufficast.search.PodcastSearch.Result;
import de.knufficast.ui.main.MainActivity;
import de.knufficast.ui.settings.SettingsActivity;
import de.knufficast.util.BooleanCallback;

public class SearchFeedActivity extends Activity implements
    AddFeedTask.Presenter {
  private SearchView searchView;
  private ProgressBar searchProgress;
  private AddFeedTask addFeedTask;
  private ListView searchResultsList;
  private ProgressDialog progressDialog;

  private final PodcastSearch podcastSearch = new ITunesSearch();
  private final List<Result> searchResults = new ArrayList<Result>();

  private final Listener<NewImageEvent> newImageListener = new Listener<NewImageEvent>() {
    @Override
    public void onEvent(NewImageEvent event) {
      searchResultsAdapter.notifyDataSetChanged();
    }
  };

  private final OnItemClickListener addFeedListener = new OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> arg0, View view, int position,
        long arg3) {
      // toggle details
      addFeed(searchResults.get(position).getFeedUrl());
    }
  };
  
  private final OnQueryTextListener queryListener = new OnQueryTextListener() {
    @Override
    public boolean onQueryTextChange(String newText) {
      return false;
    }


    @Override
  public boolean onQueryTextSubmit(String query) {
    String input = query;
    if (!"".equals(input)) {
      searchView.clearFocus(); //Hide Keyboard
      if (input.startsWith("http://") || input.startsWith("https://")
              || input.startsWith("www.")) {
        addExternalFeed(input);
      } else if (input.startsWith("itpc://")) {
        addExternalFeed(input.replace("itpc://", "http://"));
      } else if (input.contains("itunes.apple.com") && input.contains("/podcast/")) {
        addITunesFeed(input);
      } else {
        searchProgress.setVisibility(View.VISIBLE);
        podcastSearch.search(input, searchCallback);
      }
    }
    return true;
  }
};

  private SearchResultsAdapter searchResultsAdapter;
  private EventBus eventBus;

  private final BooleanCallback<List<Result>, String> searchCallback = new BooleanCallback<List<Result>, String>() {
    @Override
    public void success(List<Result> a) {
      searchResults.clear();
      searchResults.addAll(a);
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          searchProgress.setVisibility(View.GONE);
          searchResultsAdapter.notifyDataSetChanged();
        }
      });
    }

    @Override
    public void fail(String error) {
      searchResults.clear();
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          searchProgress.setVisibility(View.GONE);
          searchResultsAdapter.notifyDataSetChanged();
        }
      });
    }
  };
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.activity_feed_search);

    searchProgress = (ProgressBar) findViewById(R.id.add_feed_progress);
    searchResultsList = (ListView) findViewById(R.id.add_feed_search_results);

    searchResultsAdapter = new SearchResultsAdapter(this,
        R.layout.search_result_list_item, searchResults);
    searchResultsList.setAdapter(searchResultsAdapter);

    searchResultsList.setOnItemClickListener(addFeedListener);
  }

  @Override
  public void onStart() {
    super.onStart();

    eventBus = App.get().getEventBus();
    eventBus.addListener(NewImageEvent.class, newImageListener);
  }


  private void addFeed(String url) {
    addFeedTask = new AddFeedTask(this);
    addFeedTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
  }

  private void addExternalFeed(String url) {
    String message = getString(R.string.add_feed_progress_message);
    enableProgressDialog(null, message);

    GetFeedTask feedTask = new GetFeedTask( new GetFeedTask.ResultCallback() {
      @Override
      public void onFeedFound(final XMLFeed feed) {
        disableProgressDialog();
        final LinearLayout subscribeView = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.dialog_feed_add, null);

        ImageView imageView = (ImageView) subscribeView.findViewById(R.id.dialog_feed_add_cover);
        imageView.setImageDrawable(App.get().getImageCache().getResource(feed.getImgUrl()));

        TextView textView = (TextView) subscribeView.findViewById(R.id.dialog_feed_add_name);
        textView.setText(feed.getTitle());

        EditText descriptionText = (EditText) subscribeView.findViewById(R.id.dialog_feed_add_description);
        descriptionText.setText(feed.getDescription());

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            AlertDialog alertDialog = new AlertDialog.Builder(SearchFeedActivity.this)
                    .setView(subscribeView)
                    .setIcon(R.drawable.ic_launcher)
                    .setPositiveButton(R.string.add_feed_alert_btn_positive, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        addFeed(feed.getDataUrl());
                      }
                    })
                    .setNeutralButton(R.string.add_feed_alert_btn_neutral, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                      }
                    })
                    .setCancelable(false)
                    .create();

            alertDialog.show();
          }
        });
      }

      @Override
      public void onError(String message) {
        disableProgressDialog();
        new AlertDialog.Builder(SearchFeedActivity.this).setTitle(R.string.add_feed_failed)
                .setMessage(message).show();
      }
    });

    feedTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
  }

  private void addITunesFeed(String url) {
    Uri uri = Uri.parse(url);

    String idSegment = uri.getLastPathSegment();
    if(idSegment != null && idSegment.startsWith("id")) {
      PodcastSearch itLookup = new ITunesLookup();

      itLookup.search(idSegment.substring(2), new BooleanCallback<List<Result>, String>() {
        @Override
        public void success(final List<Result> results) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              addExternalFeed(results.get(0).getFeedUrl());
            }
          });
        }

        @Override
        public void fail(String error) {
          searchResults.clear();
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              searchProgress.setVisibility(View.GONE);
              searchResultsAdapter.notifyDataSetChanged();
            }
          });
        }
      });
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (addFeedTask != null) {
      addFeedTask.cancel(true);
    }

    eventBus.removeListener(NewImageEvent.class, newImageListener);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      // The Android way to ensure correct behavior of the "Up" button in the
      // action bar
      Intent parentActivityIntent = new Intent(this, MainActivity.class);
      parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
          | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(parentActivityIntent);
      finish();
      return true;
    case R.id.menu_settings:
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    default:
      return false;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_feed_search, menu);

    searchView = (SearchView) menu.findItem(R.id.add_feed_search)
        .getActionView();

    searchView.setQueryHint(getString(R.string.search_feed_hint));
    searchView.setOnQueryTextListener(queryListener);
    searchView.setIconifiedByDefault(false);

    Uri uri = getIntent().getData();
    if (uri != null) {
      queryListener.onQueryTextSubmit(uri.toString());
      return true;
    }

    searchView.requestFocus();
    return true;
  }

  @Override
  public void onFeedAdded() {
    addFeedTask = null;
    disableProgressDialog();
    finish();
  }

  @Override
  public void onFeedAddError(String error) {
    addFeedTask = null;
    disableProgressDialog();
    new AlertDialog.Builder(this).setTitle(R.string.add_feed_failed)
        .setMessage(error).show();
  }

  @Override
  public void onStartAddingFeed() {
    String title = getString(R.string.add_feed_progress_title);
    String message = getString(R.string.add_feed_progress_message);
    enableProgressDialog(title, message);
    progressDialog.setCancelable(true);
    progressDialog.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        addFeedTask.cancel(true);
      }
    });
  }

  private void enableProgressDialog(String title, String message) {

    progressDialog = ProgressDialog.show(this, title, message);
    // lock orientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
  }

  private void disableProgressDialog() {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }
    // unlock orientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
  }
}
