/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * YouTubeLib
 *
 * Copyright  2009-2014 United States Government as represented by
 * the Chief Information Officer of the National Center for Telehealth
 * and Technology. All Rights Reserved.
 *
 * Copyright  2009-2014 Contributors. All Rights Reserved.
 *
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE,
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY").
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES,
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 *
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: YouTubeLib001
 * Government Agency Original Software Title: YouTubeLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.youtube;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.t2.vhb.R;
import com.t2.youtube.lazylist.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

public class YouTubeSearchFragment extends ListFragment {
	private static final String API_KEY = "AIzaSyBWQ9ydq7ZE7GbRdD5_Tt1mlK66nahy96M";//"AI39si7MTKRN8WhD-PjPShszFM0plR95CWG-Tg24qdYrp49vqAJzGTVmLQzgNjtUThGQygW_ARBqScgQeIgpGW6af5e16ZEBRw";
	private static final String ROOT_URL = "https://www.googleapis.com";
	private static final String YOUTUBE_VIDEO_ROOT_URL = "https://www.youtube.com/watch?v=";

	private static final AsyncHttpClient sClient = new AsyncHttpClient();

	private YouTubeSearchListener mYouTubeSearchListener;

	private String mQuery;
	private String mPageToken;

	public void setYouTubeSearchListener(YouTubeSearchListener youTubeSearchListener) {
		mYouTubeSearchListener = youTubeSearchListener;
	}

	public interface YouTubeSearchListener {
		void onSearchStart();

		void onSearchFailure();

		void onSearchSuccess();
	}

	private static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		sClient.get(getAbsoluteUrl(url), params, responseHandler);
	}

	private static String getAbsoluteUrl(String relativeUrl) {
		return ROOT_URL + relativeUrl;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setCacheColorHint(Color.TRANSPARENT);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		setListShownNoAnimation(true);
		getListView().setDivider(new ColorDrawable(0x00000000));
	}

	public void search(String query) {
		search(query, "", new AsyncHttpResponseHandler() {
			@Override
			public void onStart() {
				super.onStart();
				if (mYouTubeSearchListener != null) {
					mYouTubeSearchListener.onSearchStart();
				}
			}

			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject json = new JSONObject(arg0);
					JSONArray jsonVideos = json.getJSONArray("items");
					final int total = json.getJSONObject("pageInfo").getInt("totalResults");
					final int itemsPer = json.getJSONObject("pageInfo").getInt("resultsPerPage");
					try {
						mPageToken = json.getString("nextPageToken");
					} catch(JSONException ex) {
						mPageToken = "";
					}

					getVideoDetails(jsonVideos, new AsyncHttpResponseHandler() {
						@Override
						public void onStart() {
							super.onStart();
							if (mYouTubeSearchListener != null) {
								mYouTubeSearchListener.onSearchStart();
							}
						}

						@Override
						public void onSuccess(String arg0) {
							try {
								JSONObject json = new JSONObject(arg0);
								JSONArray jsonVideos = json.getJSONArray("items");
								List<YouTubeVideo> videos = new ArrayList<>();
								for (int i = 0; i < jsonVideos.length(); i++) {
									JSONObject video = jsonVideos.getJSONObject(i);
									videos.add(jsonToVideo(video));
								}
								if (mYouTubeSearchListener != null) {
									mYouTubeSearchListener.onSearchSuccess();
								}

								setListAdapter(new YouTubeAdapter(getActivity(), videos, mPageToken, total, itemsPer));
							} catch (JSONException e) {
								e.printStackTrace();
							}

						}

						@Override
						public void onFailure(Throwable arg0) {
							super.onFailure(arg0);
							if (mYouTubeSearchListener != null) {
								mYouTubeSearchListener.onSearchFailure();
							}
						}
					});
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable arg0) {
				super.onFailure(arg0);
				if (mYouTubeSearchListener != null) {
					mYouTubeSearchListener.onSearchFailure();
				}
			}

		});
	}

	private void search(String query, String nextPage, AsyncHttpResponseHandler handler) {
		try {
			mQuery = query;
			RequestParams params = new RequestParams();

			params.put("part", "snippet");
			params.put("type", "video");
			params.put("q", URLEncoder.encode(mQuery, "UTF-8"));

			params.put("maxResults", 20 + "");
//			params.put("alt", "json");
			if (!TextUtils.isEmpty(nextPage)) {
				params.put("pageToken", nextPage);
			}
			params.put("key", API_KEY); 
//			params.put("fields", "openSearch:totalResults," + "	openSearch:startIndex," + "	openSearch:itemsPerPage,"
//					+ "	entry(" + "		title[@type='text']," + "		author(" + "			name" + "		)," + "		published,"
//					+ "		yt:statistics(" + "			@viewCount" + "		)," + "		media:group(" + "			yt:duration(" + "				@seconds"
//					+ "			)," + "			media:player," + "			media:thumbnail[@height=360](@url)" + "		)" + "	)");
			get("/youtube/v3/search", params, handler);
		} catch (UnsupportedEncodingException e) {
			mYouTubeSearchListener.onSearchFailure();
		}
	}

	private void getVideoDetails(JSONArray videos, AsyncHttpResponseHandler handler) {
		RequestParams params = new RequestParams();
		StringBuilder ids = new StringBuilder();
		for(int i = 0; i < videos.length(); i++) {
			try {
				JSONObject item = videos.getJSONObject(i);
				ids.append(item.getJSONObject("id").getString("videoId"));
				if(i < videos.length() - 1) {
					ids.append(",");
				}
			} catch(JSONException ex) {
				ex.printStackTrace();			
			}
		}

		params.put("part", "snippet,statistics");
		params.put("id", ids.toString());

		params.put("key", API_KEY); 
		get("/youtube/v3/videos", params, handler);
	}

	private static YouTubeVideo jsonToVideo(JSONObject json) {
		YouTubeVideo video = new YouTubeVideo();

		try {
			JSONObject snippet = json.getJSONObject("snippet");
			video.setTitle(snippet.getString("title"));
			video.setAuthor(snippet.getString("channelTitle"));
			
			JSONObject thumbnails = snippet.getJSONObject("thumbnails");
			video.setThumbnailUrl(thumbnails.getJSONObject("default").getString("url"));
			
			
			StringBuilder views = new StringBuilder();
			Formatter format = new Formatter(views);
			format.format("%,d", json.getJSONObject("statistics").getLong("viewCount"));
			video.setViewCount(views.toString());
			format.close();
			
			final String id = json.getString("id");
			video.setId(id);
			video.setUrl(YOUTUBE_VIDEO_ROOT_URL + id);

			final SimpleDateFormat date_parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			Date date = date_parser.parse(snippet.getString("publishedAt"));
			video.setPublished(DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(),
					DateUtils.HOUR_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return video;
	}

	private final class YouTubeAdapter extends ArrayAdapter<YouTubeVideo> {

		private final ImageLoader mImageLoader;
		private int mLoadedIndex;
		private final int mTotalVideos;
		private final int mItemsPerPage;
		private final String mNextPageToken;
		private final List<YouTubeVideo> mVideos;
		private boolean mLoading;

		public YouTubeAdapter(Context context, List<YouTubeVideo> objects, String nextPageToken, int totalVideos, int itemsPerPage) {
			super(context, android.R.layout.simple_list_item_1, objects);
			mVideos = objects;
			mLoadedIndex = 1;
			mTotalVideos = totalVideos;
			mNextPageToken = nextPageToken;
			mItemsPerPage = itemsPerPage;
			mImageLoader = new ImageLoader(context);
		}

		public void addVideos(List<YouTubeVideo> videos) {
			mVideos.addAll(videos);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (!hasMoreVideos()) {
				return super.getCount();
			} else {
				return super.getCount() + 1;
			}
		}

		private boolean hasMoreVideos() {
			return mLoadedIndex + mItemsPerPage < mTotalVideos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.video_row, null);
			}

			if (hasMoreVideos() && position == (getCount() - 1)) {
				RelativeLayout layout = new RelativeLayout(getContext());
				ProgressBar bar = new ProgressBar(getContext());
				bar.setIndeterminate(true);
				bar.setPadding(10, 10, 10, 10);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_IN_PARENT);
				bar.setLayoutParams(params);

				if (!mLoading) {
					mLoading = true;
					search(mQuery, mNextPageToken, new AsyncHttpResponseHandler() {
						@Override
						public void onSuccess(String arg0) {
							try {
								JSONObject json = new JSONObject(arg0);
								JSONArray jsonVideos = json.getJSONArray("items");
								try {
									mPageToken = json.getString("nextPageToken");
								} catch(JSONException ex) {
									mPageToken = "";
								}

								getVideoDetails(jsonVideos, new AsyncHttpResponseHandler() {
									@Override
									public void onStart() {
										super.onStart();
										if (mYouTubeSearchListener != null) {
											mYouTubeSearchListener.onSearchStart();
										}
									}

									@Override
									public void onSuccess(String arg0) {
										try {
											JSONObject json = new JSONObject(arg0);
											JSONArray jsonVideos = json.getJSONArray("items");
											List<YouTubeVideo> videos = new ArrayList<>();
											for (int i = 0; i < jsonVideos.length(); i++) {
												JSONObject video = jsonVideos.getJSONObject(i);
												final YouTubeVideo ytVideo = jsonToVideo(video);
												if (!mVideos.contains(ytVideo)) {
													videos.add(ytVideo);
												}
											}
											if (mYouTubeSearchListener != null) {
												mYouTubeSearchListener.onSearchSuccess();
											}

											mLoadedIndex += mItemsPerPage;
											addVideos(videos);
										} catch (JSONException e) {
											e.printStackTrace();
										}

									}

									@Override
									public void onFailure(Throwable arg0) {
										super.onFailure(arg0);
										if (mYouTubeSearchListener != null) {
											mYouTubeSearchListener.onSearchFailure();
										}
									}
								});
							} catch (JSONException e) {
								e.printStackTrace();
							}

						}

						@Override
						public void onFinish() {
							super.onFinish();
							mLoading = false;
						}
					});
				}
				layout.addView(bar);

				return layout;
			} else if (v.findViewById(R.id.lbl_title) == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.video_row, null);
			}

			YouTubeVideo video = getItem(position);
			if (video != null && video.getThumbnailUrl() != null) {
				mImageLoader.displayImage(video.getThumbnailUrl(), ((ImageView) v.findViewById(R.id.img_thumb)));
			}
			((TextView) v.findViewById(R.id.lbl_title)).setText(video.getTitle());
			((TextView) v.findViewById(R.id.lbl_author)).setText(video.getAuthor());
			((TextView) v.findViewById(R.id.lbl_published)).setText(video.getPublished() + " | " + video.getViewCount()
					+ " views");
			return v;
		}
	}
}
