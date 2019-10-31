package com.example.ytdl;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;



public class MainActivity extends AppCompatActivity {
    View mainLayout;
    ListView VideoList;
    VideoListAdapter listAdapter;
    static String nextToken = "";
    static String query = "";
    static String Key = "AIzaSyAIfzvhK5TpgtpXAuItLlKd-jjM3byoIOw";
    static boolean isSearching = false;
    ProgressDialog Downloadpd;
    CompositeDisposable disposable = new CompositeDisposable();
    public DownloadProgressCallback callback = new DownloadProgressCallback() {
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            final float p = progress;
            final long es = etaInSeconds;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Downloadpd.setProgress((int)p);
                    Downloadpd.setMessage(p + "% (완료까지 " + es + " 초)");
                }
            });
        }
    };
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    @NonNull
    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "youtube-downloads");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }

    public void openDownloadDialog(VideoItem video, boolean isAudio) {
        System.out.println("다운로드 시작 - " + video.getTitle());
        if(!isStoragePermissionGranted()) {
            System.out.println("승인되지 않음");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("파일 시스템 권한이 승인되지 않아 다운로드 할 수 없습니다.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            builder.create();
        }
        else {
            try {
                YoutubeDL.getInstance().init(getApplication());
                FFmpeg.getInstance().init(getApplication());
                final YoutubeDLRequest req = new YoutubeDLRequest("https://www.youtube.com/watch?v=" + video.getVideoId());
                File path = getDownloadLocation();
                String AbsolutePath = path.getAbsolutePath() + "/" + video.getTitle().replaceAll("/", "|");
                if(!isAudio) {
                    req.setOption("-o", AbsolutePath + ".%(ext)s");
                    AbsolutePath += ".mp4";
                }
                else {
                    req.setOption("-o", AbsolutePath + ".%(ext)s");
                    req.setOption("-x");
                    req.setOption("--audio-format", "mp3");
                    AbsolutePath += ".mp3";
                }
                final String apath = AbsolutePath;
                Downloadpd = new ProgressDialog(MainActivity.this);
                Downloadpd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                Downloadpd.setMessage("다운로드 시작");
                Downloadpd.show();
                System.out.println("다이얼로그 띄움");
                Disposable disp = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(req, callback)).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(YoutubeDLResponse -> {
                    Downloadpd.setMessage("다운로드 완료");
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"+ apath)));
                    Downloadpd.dismiss();
                });
                disposable.add(disp);
                System.out.println("다운로드 시작");
                Snackbar.make(mainLayout, "다운로드 완료", Snackbar.LENGTH_LONG);
            }
            catch (Exception Ex)
            {
                Ex.printStackTrace();
                Downloadpd.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File f = new File(Environment.getDataDirectory() + "/data" + getPackageName(), "config.properties");
        if(f.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                Properties p = new Properties();
                p.load(fis);
                Key = p.getProperty("APIKey");
                System.out.println(Key);
            }
            catch(Exception Ex)
            {
                Ex.printStackTrace();
            }
        }
        mainLayout = findViewById(R.id.mainLayout);
        setSupportActionBar(findViewById(R.id.toolbar));
        VideoList = findViewById(R.id.ResultList);
            System.out.println("업데이트 시작");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(getApplication());
                    } catch (YoutubeDLException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        listAdapter = new VideoListAdapter();
        VideoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                String[] Options = new String[2];
                Options[0] = "영상 다운로드"; Options[1] = "음성 다운로드";
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("다운로드 옵션")
                        .setItems(Options, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                switch(which) {
                                    case 0: {
                                        openDownloadDialog(listAdapter.getItem(position), false);
                                        break;
                                    }
                                    case 1: {
                                        openDownloadDialog(listAdapter.getItem(position), true);
                                        break;
                                    }
                                }
                            }
                        });
                builder.create().show();
            }
        });
        VideoList.setOnScrollListener(new AbsListView.OnScrollListener() {
            boolean lastItemVisible = false;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && lastItemVisible && listAdapter.getCount() != 0 && nextToken != null && !isSearching)
                {
                    isSearching = true;
                    final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"검색 진행 중", query + "에 대한 검색을 진행중입니다...", true);
                    try {
                        new Thread() {
                            public void run() {
                                try {
                                    YouTube yt = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                                        @Override
                                        public void initialize(HttpRequest request) throws IOException {

                                        }
                                    }).setApplicationName("ytdl-app").build();
                                    YouTube.Search.List search = yt.search().list("id,snippet");
                                    search.setKey(Key);
                                    search.setQ(query);
                                    search.setType("video");
                                    search.setPageToken(nextToken);
                                    search.setFields("nextPageToken,items(id/kind,id/videoId,snippet/title,snippet/channelTitle,snippet/thumbnails/medium/url)");
                                    long max = 15;
                                    search.setMaxResults(max);
                                    SearchListResponse Resp = search.execute();
                                    nextToken = Resp.getNextPageToken();
                                    for(SearchResult result :  Resp.getItems()) {
                                        listAdapter.addItem(DrawableFromUrl(result.getSnippet().getThumbnails().getMedium().getUrl()), result.getSnippet().getTitle(), result.getSnippet().getChannelTitle(), result.getId().getVideoId());
                                    }
                                }
                                catch(IOException Ex) {
                                    Ex.printStackTrace();
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pd.dismiss();
                                        int Pos = VideoList.getFirstVisiblePosition();
                                        Snackbar.make(mainLayout, "검색 완료 - " + query, Snackbar.LENGTH_LONG).show();
                                        VideoList.setAdapter(listAdapter);
                                        isSearching = false;
                                        VideoList.setSelection(Pos + 1);
                                    }
                                });
                            }
                        }.start();
                    }
                    catch(Exception Ex)
                    {
                        Ex.printStackTrace();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lastItemVisible = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem settingsItem = menu.findItem(R.id.action_options);
        settingsItem.setOnMenuItemClickListener(item -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("API키 설정");
            final EditText keyTextView = new EditText(MainActivity.this);
            keyTextView.setText(Key);
            builder.setView(keyTextView);
            builder.setPositiveButton("저장", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Key = keyTextView.getText().toString();
                    File file = new File(Environment.getDataDirectory() + "/data/" + getPackageName(), "config.properties");
                    FileOutputStream fos = null;
                    try{
                        if(!file.exists()){
                            file.createNewFile();
                        }
                        fos = new FileOutputStream(file);
                        Properties props = new Properties();
                        props.setProperty("APIKey" , Key);
                        props.store(fos, "Key info");
                    }catch (Exception Ex){
                        Ex.printStackTrace();
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return false;
        });
        SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("검색...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String str)
            {
                Snackbar.make(mainLayout, "검색 시작 - " + str, Snackbar.LENGTH_LONG).show();
                query = str;
                listAdapter.clear();
                nextToken = "";
                final ProgressDialog pd = ProgressDialog.show(MainActivity.this,"검색 진행 중", str + "에 대한 검색을 진행중입니다...", true);
                try {
                    new Thread() {
                        public void run() {
                            try {
                                YouTube yt = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                                    @Override
                                    public void initialize(HttpRequest request) throws IOException {

                                    }
                                }).setApplicationName("ytdl-app").build();
                                YouTube.Search.List search = yt.search().list("id,snippet");
                                search.setKey(Key);
                                search.setQ(query);
                                search.setType("video");
                                search.setFields("nextPageToken,items(id/kind,id/videoId,snippet/title,snippet/channelTitle,snippet/thumbnails/medium/url)");
                                long max = 15;
                                search.setMaxResults(max);
                                SearchListResponse Resp = search.execute();
                                if(Resp.getItems() == null)
                                {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("검색 결과를 받아올 수 없었습니다. 겁색 결과가 없거나 API 키가 유효하지 않을 수 있습니다.")
                                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    builder.create().show();
                                    return;
                                }
                                nextToken = Resp.getNextPageToken();
                                for(SearchResult result :  Resp.getItems()) {
                                    listAdapter.addItem(DrawableFromUrl(result.getSnippet().getThumbnails().getMedium().getUrl()), result.getSnippet().getTitle(), result.getSnippet().getChannelTitle(), result.getId().getVideoId());
                                }
                            }
                            catch(IOException Ex) {
                                Ex.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.dismiss();
                                    Snackbar.make(mainLayout, "검색 완료 - " + query, Snackbar.LENGTH_LONG).show();
                                    VideoList.setAdapter(listAdapter);
                                }
                            });
                        }
                    }.start();
                }
                catch(Exception Ex)
                {
                    Ex.printStackTrace();
                }
                return false;
            }
            public boolean onQueryTextChange(String string)
            {
                return false;
            }
        });
        return true;
    }

    public static Drawable DrawableFromUrl(String url) throws IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(Resources.getSystem(), x);
    }
}
