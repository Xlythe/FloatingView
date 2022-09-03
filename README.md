# FloatingView

A library for creating floating views in Android

Where to Download
-----------------
```groovy
dependencies {
  implementation 'com.xlythe:floating-view:2.1.1'
}
```

Permissions
-----------------
The following permissions are required in your AndroidManfiest.xml
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.VIBRATE" android:maxSdkVersion="29" />
```

FloatingView
-----------------
Extend FloatingViewService (<R) and and FloatingViewActivity (R+) to implement the floating window.
```java
public class FloatingNotesService extends FloatingViewService  {
    @NonNull
    @Override
    public View inflateButton(@NonNull ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.floating_icon, parent, false);
    }

    @NonNull
    @Override
    public View inflateView(@NonNull ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(R.layout.floating_notes, parent, false);
    }

    @NonNull
    @Override
    protected Notification createNotification() {
        Intent intent = new Intent(this, FloatingNotes.class).setAction(ACTION_OPEN);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Tap to open")
                .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
```
```java
public class FloatingNotesActivity extends FloatingViewActivity  {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
```
Extend OpenShortcutActivity to create the entrypoint for the floating window. This Activity will
prompt the user for the necessary permissions, and will subsequently launch the floating window.
```java
public class OpenActivity extends OpenShortcutActivity {
    @Override
    public Intent createServiceIntent() {
        return new Intent(this, FloatingNotesService.class);
    }

    @Override
    public Intent createActivityIntent() {
        return new Intent(this, FloatingNotesActivity.class);
    }

    @RequiresApi(29)
    @Override
    protected Notification createNotification() {
        NotificationChannel notificationChannel = new NotificationChannel(FloatingNotesService.CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
        notificationChannel.setAllowBubbles(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent intent = new Intent(ACTION_OPEN).setPackage(getPackageName());
        return new NotificationCompat.Builder(this, FloatingNotesService.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.floating_notification_description))
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }
}
```

Updating the AndroidManifest
-----------------
Your FloatingViewService, FloatingViewActivity, and OpenShortcutActivity components must be declared in AndroidManfiest.xml
```java
<activity android:name=".FloatingNotesActivity" />
<service android:name=".FloatingNotesService" />
<activity android:name=".OpenActivity" />
```

License
-------

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
