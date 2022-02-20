package com.imorning.screencapture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class RecordService extends Service {
  public static final String KEY_CODE = "key_code";
  public static final String KEY_DATA = "data";
  private static final String TAG = "RecordService";
  private static final int NOTIFICATION_ID = 220220;
  private static final int KEY_STOP = 180927;
  private File file;
  private MediaProjection mediaProjection;
  private MediaRecorder mediaRecorder;
  private final RecordServiceBinder binder = new RecordServiceBinder();

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    createNotificationChannel();
    int resultCode = intent.getIntExtra(KEY_CODE, KEY_STOP);
    Intent intentParcelableExtra = intent.getParcelableExtra(KEY_DATA);
    Log.i(TAG, "onStartCommand: result code is " + resultCode);
    MediaProjectionManager mediaProjectionManager =
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intentParcelableExtra);
    initMediaRecorder();
    mediaRecorder.start();
    return binder;
  }


  private void createNotificationChannel() {
    Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
    Intent intent = new Intent(this, RecordService.class);
    builder
        .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentText(getString(R.string.recording))
        .setWhen(System.currentTimeMillis());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setChannelId(getString(R.string.notification_id));
    }
    NotificationManager notificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    NotificationChannel channel;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      channel =
          new NotificationChannel(
              getString(R.string.notification_id),
              getString(R.string.recording),
              NotificationManager.IMPORTANCE_LOW);
      notificationManager.createNotificationChannel(channel);
    }
    Notification notification = builder.build();
    notification.defaults = Notification.DEFAULT_SOUND;
    startForeground(NOTIFICATION_ID, notification);
  }

  private void initMediaRecorder() {
    createFile();
    mediaRecorder = new MediaRecorder();
    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mediaRecorder.setOutputFile(file);
    mediaRecorder.setVideoSize(720, 1080);
    mediaRecorder.setVideoFrameRate(60);
    mediaRecorder.setVideoEncodingBitRate(720 * 1080);
    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    try {
      mediaRecorder.prepare();
    } catch (IOException e) {
      Log.e(TAG, "initMediaRecorder error: " + e.getMessage());
      return;
    }
    mediaProjection.createVirtualDisplay(
        TAG + "-display",
        720,
        1080,
        1,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
        mediaRecorder.getSurface(),
        null,
        null);
  }

  /** Release resources after recording */
  private void release() {
    if (mediaRecorder != null) {
      mediaRecorder.setOnErrorListener(null);
      mediaProjection.stop();
      mediaRecorder.reset();
      mediaRecorder.release();
      mediaRecorder = null;
    }
    if (mediaProjection != null) {
      mediaProjection.stop();
      mediaProjection = null;
    }
  }

  /** construct a file object to save video */
  private void createFile() {
    Calendar calendar = Calendar.getInstance();
    String stringBuffer =
        calendar.get(Calendar.YEAR)
            + "_"
            + (calendar.get(Calendar.MONTH) + 1)
            + "_"
            + calendar.get(Calendar.DATE)
            + "_"
            + calendar.get(Calendar.HOUR_OF_DAY)
            + "_"
            + calendar.get(Calendar.MINUTE);
    file = new File(getExternalCacheDir() + File.separator + stringBuffer + ".mp4");
  }

  @Override
  public void onDestroy() {
    release();
    stopForeground(true);
    Toast.makeText(getApplicationContext(), "文件已保存到" + file.getAbsolutePath(), Toast.LENGTH_LONG)
        .show();
    super.onDestroy();
  }

  public class RecordServiceBinder extends Binder {
    public RecordService getRecordService() {
      return RecordService.this;
    }
  }
}
