# FloatingView

A library for creating floating views in Android

Where to Download
-----------------
```groovy
dependencies {
  implementation 'com.xlythe:floating-view:2.1.7'
}
```

Permissions
-----------------
The following permissions are required in your AndroidManfiest.xml
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" android:maxSdkVersion="29" />
<uses-permission android:name="android.permission.VIBRATE" android:maxSdkVersion="29" />
```

Bubble settings
-----------------
Android 6–10 use the overlay permission. Android 11 uses notification bubbles and requires “All conversations can bubble” because its public API only reports that app-wide choice. On Android 12 and later, “Selected conversations” also works when the user has enabled this particular floating window. Each opener activity has its own conversation shortcut.

A newly created channel can report `canBubble() == false` even when “All conversations” allows it. Android reserves per-conversation choices for the user. The public API also reports false for an explicitly disabled channel, so Android makes the final decision when the notification is posted. [Android 11 channel creation](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r1/services/core/java/com/android/server/notification/PreferencesHelper.java#897) · [Android bubble eligibility](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/core/java/com/android/server/notification/BubbleExtractor.java#108)

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

    @RequiresApi(30)
    @Override
    protected Notification createNotification() {
        NotificationChannel notificationChannel = new NotificationChannel(FloatingNotesService.CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);

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
