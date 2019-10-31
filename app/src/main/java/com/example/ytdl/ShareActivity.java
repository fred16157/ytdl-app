package com.example.ytdl;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Button;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ShareActivity extends AppCompatActivity {

    ProgressDialog Downloadpd;
    CompositeDisposable disposable = new CompositeDisposable();
    boolean isAudio;
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

    @Override
    protected void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
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
        TextView linkText = findViewById(R.id.linkText);
        Bundle extras = getIntent().getExtras();
        linkText.setText(extras.getString(Intent.EXTRA_TEXT));
        Button runBtn = findViewById(R.id.runBtn);
        RadioGroup optionGroup = findViewById(R.id.optionGroup);
        optionGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            if(i == R.id.videoRadio)
            {
                isAudio = false;
            }
            else if(i == R.id.audioRadio)
            {
                isAudio = true;
            }
        });
        runBtn.setOnClickListener(v -> {
            if(!isStoragePermissionGranted()) {
                System.out.println("승인되지 않음");
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
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
                    final YoutubeDLRequest req = new YoutubeDLRequest(extras.getString(Intent.EXTRA_TEXT));
                    File path = getDownloadLocation();
                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(extras.getString(Intent.EXTRA_TEXT));
                    String AbsolutePath = path.getAbsolutePath() + "/" + streamInfo.getTitle().replaceAll("/", "|");
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
                    Downloadpd = new ProgressDialog(ShareActivity.this);
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
                }
                catch (Exception Ex)
                {
                    Ex.printStackTrace();
                    Downloadpd.dismiss();
                }
            }
        });

    }
}
