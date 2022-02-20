package com.imorning.screencapture;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "TAG_MainActivity";
  private static final int PERMISSION_CODE = 0;
  private static final int REQUEST_CODE = 1124;
  private static final String[] permissions =
      new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
  private FloatingActionButton fab;
  private MediaProjectionManager mediaProjectionManager;
  private boolean isRecording = false;
  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
          RecordService.RecordServiceBinder recordServiceBinder =
              (RecordService.RecordServiceBinder) binder;
          // recordServiceBinder.getRecordService();
          isRecording = true;
          fab.setImageResource(R.mipmap.ic_stop);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          isRecording = false;
          fab.setImageResource(R.mipmap.ic_play);
        }
      };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (EasyPermissions.hasPermissions(this, permissions)) {
      mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
      EasyPermissions.requestPermissions(
          this, getString(R.string.permission_details), PERMISSION_CODE, permissions);
    } else {
      Toast.makeText(getApplicationContext(), "无权限", Toast.LENGTH_SHORT).show();
      return;
    }
    fab = findViewById(R.id.fab);
    fab.setOnClickListener(
        v -> {
          if (!isRecording) {
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE);
          } else {
            unbindService(connection);
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_CANCELED || data == null) {
      return;
    }
    Intent intent = new Intent(MainActivity.this, RecordService.class);
    intent.putExtra(RecordService.KEY_CODE, resultCode);
    intent.putExtra(RecordService.KEY_DATA, data);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }
}
