# FloatingView

A library for creating floating views in Android

Where to Download
-----------------
```groovy
dependencies {
  compile 'com.xlythe:floating-view:1.1.4'
}
```

Permissions
-----------------
The following permissions are required in your AndroidManfiest.xml
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.VIBRATE" />
```

FloatingView
-----------------
Extend FloatingView and override the required methods. When open, your view will appear beneath your button.
```java
public class FloatingNotes extends FloatingView  {
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

License
-------

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
