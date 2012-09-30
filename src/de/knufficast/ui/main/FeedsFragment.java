package de.knufficast.ui.main;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import de.knufficast.App;
import de.knufficast.R;
import de.knufficast.events.EventBus;
import de.knufficast.events.Listener;
import de.knufficast.events.NewImageEvent;
import de.knufficast.logic.AddFeedTask;
import de.knufficast.logic.model.Feed;

/**
 * Fragment in the main window that displays the "feeds" tab.
 * 
 * @author crazywater
 * 
 */
public class FeedsFragment extends BaseFragment implements
    AddFeedTask.Presenter, FeedsAdapter.Presenter {
  private ProgressDialog progressDialog;
  private Presenter presenter;
  private EventBus eventBus;
  private FeedsAdapter feedsAdapter;

  private List<Feed> feeds = new ArrayList<Feed>();

  private CharSequence feedText;

  private Listener<NewImageEvent> newImageListener = new Listener<NewImageEvent>() {
    @Override
    public void onEvent(NewImageEvent event) {
      refreshFeeds();
    }
  };

  @Override
  public int getLayoutId() {
    return R.layout.fragment_feeds;
  }

  @Override
  public int getTitleId() {
    return R.string.title_feeds;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    feedsAdapter = new FeedsAdapter(getContext(), R.layout.feed_list_item,
        feeds, this);
    
    eventBus = App.get().getEventBus();
  }

  @Override
  public void onStart() {
    super.onStart();
    refreshFeeds();

    Button addButton = findView(R.id.add_feed_button);
    final TextView addText = findView(R.id.add_feed_text);
    if (feedText != null) {
      addText.setText(feedText);
    }
    final AddFeedTask.Presenter presenter = this;
    addButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View unused) {
        String input = addText.getText().toString();
        new AddFeedTask(presenter, App.get().getConfiguration()).execute(input);
      }
    });

    ListView feedsList = findView(R.id.feeds_list_view);
    feedsList.setAdapter(feedsAdapter);


    eventBus.addListener(NewImageEvent.class, newImageListener);
  }

  @Override
  public void onStop() {
    super.onStop();
    eventBus.removeListener(NewImageEvent.class, newImageListener);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    try {
      presenter = (Presenter) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement "
          + this.getClass().getName() + ".Presenter");
    }
  }

  /**
   * Refreshes the displayed feeds.
   */
  public void refreshFeeds() {
    feeds.clear();
    feeds.addAll(App.get().getConfiguration().getAllFeeds());
    feedsAdapter.notifyDataSetChanged();
  }

  @Override
  public void onFeedAdded() {
    progressDialog.dismiss();
    progressDialog = null;
    refreshFeeds();
  }

  @Override
  public void onFeedAddError(String error) {
    progressDialog.dismiss();
    progressDialog = null;
    new AlertDialog.Builder(getContext()).setTitle(R.string.add_feed_failed)
        .setMessage(error)
        .show();
  }

  @Override
  public void onStartAddingFeed() {
    feedText = null;
    String title = getString(R.string.add_feed_progress_title);
    String message = getString(R.string.add_feed_progress_message);
    progressDialog = ProgressDialog.show(getContext(), title, message);
  }

  public void prepareForFeedText(CharSequence text) {
    feedText = text;
  }

  public interface Presenter extends FeedsAdapter.Presenter {

  }

  @Override
  public void feedClicked(Feed feed) {
    presenter.feedClicked(feed);
  }
}
