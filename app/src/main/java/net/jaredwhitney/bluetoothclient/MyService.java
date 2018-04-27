package net.jaredwhitney.bluetoothclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class MyService extends Service {

    NotificationManager notificationManager;
    Notification.Builder notiBuilder;
    MediaSession mediaSession;
    String noti_id = "np_01";
    BluetoothAdapter btadapter;
    BluetoothSocket socket;
    Scanner sc;
    Thread thread;

    static PrintWriter writer;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        thread = new Thread(new Runnable(){
            public void run()
            {
                if (notiBuilder != null)
                    startForeground(3, notiBuilder.build());

                btadapter = BluetoothAdapter.getDefaultAdapter();

                mediaSession = new MediaSession(getApplicationContext(), "NowPlayingProvider");
                VolumeProvider vProvider = new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 20, 10) {
                    @Override
                    public void onAdjustVolume(int direction) {
                        if (direction < 0)
                        {
                            Intent volDownIntent = new Intent(getApplicationContext(), ControlsReceiver.class);
                            volDownIntent.putExtra("action", "volume down");
                            sendBroadcast(volDownIntent);
                            Log.d("vbutton", "requested volume down send");
                        }
                        else
                        {
                            Intent volUpIntent = new Intent(getApplicationContext(), ControlsReceiver.class);
                            volUpIntent.putExtra("action", "volume up");
                            sendBroadcast(volUpIntent);
                            Log.d("vbutton", "requested volume up send");
                        }
                    }
                };
                mediaSession.setPlaybackToRemote(vProvider);

                notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                CharSequence name = "Now Playing";
                String description = "Displays messages about the global Now Playing state.";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel mChannel = new NotificationChannel(noti_id, name, importance);
                mChannel.setDescription(description);
                mChannel.enableLights(false);
                mChannel.enableVibration(false);
                //mChannel.setVibrationPattern(new long[]{100, 400, 80, 20});
                notificationManager.createNotificationChannel(mChannel);

                Notification syncNotification = new Notification.Builder(getApplicationContext(), noti_id).setContentTitle("Bluetooth Client").setContentText("Syncing...").setSmallIcon(R.drawable.ic_sync_white_24dp).build();

                try {
                    Set<BluetoothDevice> devices = btadapter.getBondedDevices();
                    if (devices.size() != 0)
                    {
                        for (BluetoothDevice device : devices)
                        {
                            boolean connected = false;
                            try {
                                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-B10E70074AE1"));
                                Log.d("service", "trying to connect");
                                socket.connect();
                                connected = true;
                            }
                            catch (IOException ex) {
                                Log.d("service", "connection failed for device " + device.getName());
                            }
                            if (connected)
                            {
                                Log.d("service", "connected");
                                writer = new PrintWriter(socket.getOutputStream());
                                sc = new Scanner(socket.getInputStream());
                                writer.println("Java Bluetooth Bridge");
                                writer.flush();
                                while (true)
                                {
                                    while (sc.hasNextLine()) {
                                        startForeground(3, syncNotification);
                                        String line = sc.nextLine();
                                        if (!line.equals("nothingplaying")) {
                                            Log.d("bt", line);
                                            String imgdat = sc.nextLine();
                                            String sizepart = imgdat.split("\\Q:\\E")[0];
                                            int imgwidth = Integer.parseInt(sizepart.split("\\Q,\\E")[0]);
                                            int imgheight = Integer.parseInt(sizepart.split("\\Q,\\E")[1]);
                                            int[] imagedata = new int[imgwidth * imgheight];
                                            imgdat = imgdat.split("\\Q:\\E")[1];
                                            String[] imagedatapoints = imgdat.split("\\Q, \\E");
                                            for (int i = 0; i < imagedata.length; i++)
                                                imagedata[i] = Integer.parseInt(imagedatapoints[i]);
                                            Bitmap icon = Bitmap.createBitmap(imagedata, imgwidth, imgheight, Bitmap.Config.RGB_565);
                                            Log.d("img", "Finished reconstructing image (" + imgwidth + "x" + imgheight + " px)");
                                            String[] parts = line.split("\0");
                                            MediaMetadata metadata = new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, parts[1]).putString(MediaMetadata.METADATA_KEY_ARTIST, parts[2]).putBitmap(MediaMetadata.METADATA_KEY_ART, icon).build();
                                            mediaSession.setMetadata(metadata);
                                            PlaybackState playbackState = new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1).build();
                                            mediaSession.setPlaybackState(playbackState);

                                            Intent volUpIntent = new Intent(getApplicationContext(), ControlsReceiver.class);
                                            volUpIntent.putExtra("action", "volume up");
                                            PendingIntent pVolUpIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, volUpIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                            Intent volDownIntent = new Intent(getApplicationContext(), ControlsReceiver.class);
                                            volDownIntent.putExtra("action", "volume down");
                                            PendingIntent pVolDownIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, volDownIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                            mediaSession.setActive(true);

                                            String[] cparts = parts[4].split("\\Q,\\E");
                                            int ncolor = Color.rgb(Integer.parseInt(cparts[0]), Integer.parseInt(cparts[1]), Integer.parseInt(cparts[2]));

                                            notiBuilder = new Notification.Builder(getApplicationContext(), noti_id).setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(1)).setContentTitle(parts[1]).setContentText(parts[2]).setSmallIcon(R.drawable.ic_bluetooth_audio_white_24dp).setLargeIcon(icon).setOngoing(true).setUsesChronometer(true).setVisibility(Notification.VISIBILITY_PUBLIC);
                                            Log.d("colorset", "set 0x" + Integer.toHexString(Color.rgb(Integer.parseInt(cparts[0]), Integer.parseInt(cparts[1]), Integer.parseInt(cparts[2]))));
                                            notiBuilder.setColor(ncolor).addAction(R.drawable.ic_volume_down_white_24dp, "Volume Down", pVolDownIntent).addAction(R.drawable.ic_volume_up_white_24dp, "Volume Up", pVolUpIntent);
                                        }
                                        else {
                                            notiBuilder = new Notification.Builder(getApplicationContext(), noti_id).setContentTitle("Now Playing Link").setContentText("Nothing is playing.").setSmallIcon(R.drawable.ic_hourglass_empty_white_24dp).setVisibility(Notification.VISIBILITY_PUBLIC);
                                            mediaSession.setActive(false);
                                        }

                                        startForeground(3, notiBuilder.build());
                                    }
                                    try{Thread.sleep(1500);}catch(InterruptedException ex){ex.printStackTrace();}
                                }
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    Log.e("error", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        Log.d("ondestroy", "running ondestroy");
        if (sc != null)
            sc.close();
        if (writer != null)
            writer.close();
        if (socket != null)
            try { socket.close(); } catch(IOException ex){}
        startService(new Intent(this, MyService.class));
    }
}