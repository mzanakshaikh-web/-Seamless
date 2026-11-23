package com.seamless.player;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import java.util.ArrayList;

public class PlaybackService extends Service {
    public static final String ACTION_PLAY="com.seamless.player.SERVICE_PLAY";
    public static final String ACTION_TOGGLE="com.seamless.player.SERVICE_TOGGLE";
    public static final String ACTION_NEXT="com.seamless.player.SERVICE_NEXT";
    public static final String ACTION_PREV="com.seamless.player.SERVICE_PREV";
    public static final String ACTION_SEEK="com.seamless.player.SERVICE_SEEK";
    public static final String EXTRA_URI="track_uri";
    public static final String EXTRA_POSITION="position";
    public static final String BROADCAST_STATE="com.seamless.player.STATE";
    private static final String CHANNEL="seamless_playback";
    private static final int NOTIFICATION_ID=41;
    private MediaPlayer player;
    private final ArrayList<Track> tracks=new ArrayList<>();
    private int index=-1;
    private boolean prepared=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){@Override public void run(){if(player!=null&&prepared){try{if(player.isPlaying())publish(current()==null?"Seamless":current().title,true);}catch(IllegalStateException ignored){}}handler.postDelayed(this,1000);}};

    private static class Track {
        final Uri uri; final String title, artist;
        Track(Uri u,String t,String a){uri=u;title=t;artist=a;}
    }

    @Override public void onCreate(){super.onCreate();createChannel();handler.post(ticker);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        startForeground(NOTIFICATION_ID,notification("Seamless",false));
        String action=intent.getAction();
        if(ACTION_PLAY.equals(action)){String value=intent.getStringExtra(EXTRA_URI);if(value!=null)playUri(Uri.parse(value));}
        else if(ACTION_TOGGLE.equals(action))toggle();
        else if(ACTION_NEXT.equals(action))step(1);
        else if(ACTION_PREV.equals(action))step(-1);
        else if(ACTION_SEEK.equals(action)&&player!=null&&prepared){try{player.seekTo((int)(player.getDuration()*intent.getIntExtra(EXTRA_POSITION,0)/1000f));}catch(IllegalStateException ignored){}}
        return START_NOT_STICKY;
    }
    @Override public IBinder onBind(Intent intent){return null;}

    private boolean hasAudioPermission(){
        if(Build.VERSION.SDK_INT>=33)return checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED;
        return Build.VERSION.SDK_INT>=23?checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)==android.content.pm.PackageManager.PERMISSION_GRANTED:true;
    }
    private void loadTracks(){
        tracks.clear();if(!hasAudioPermission())return;
        String[] columns={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST};
        try(Cursor c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,columns,MediaStore.Audio.Media.IS_MUSIC+"!=0",null,MediaStore.Audio.Media.TITLE+" ASC")){
            if(c!=null)while(c.moveToNext())tracks.add(new Track(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,c.getString(0)),c.getString(1),c.getString(2)));
        }catch(SecurityException ignored){}
    }
    private void playUri(Uri uri){
        loadTracks();index=-1;for(int i=0;i<tracks.size();i++)if(tracks.get(i).uri.equals(uri)){index=i;break;}
        if(index<0){tracks.add(new Track(uri,"Unknown track",""));index=tracks.size()-1;}
        playAt(index);
    }
    private void playAt(int requested){
        if(tracks.isEmpty())loadTracks();if(tracks.isEmpty()){publish("No music found",false);return;}
        index=(requested%tracks.size()+tracks.size())%tracks.size();Track track=tracks.get(index);
        releasePlayer();
        try{
            player=new MediaPlayer();prepared=false;
            player.setOnPreparedListener(mp->{prepared=true;try{mp.start();startForeground(NOTIFICATION_ID,notification(track.title,true));publish(track.title,true);}catch(IllegalStateException e){publish("Playback error",false);}});
            player.setOnCompletionListener(mp->step(1));
            player.setOnErrorListener((mp,what,extra)->{prepared=false;publish("Unable to play this track",false);return true;});
            player.setDataSource(this,track.uri);startForeground(NOTIFICATION_ID,notification(track.title,false));player.prepareAsync();
        }catch(Exception e){releasePlayer();publish("Unable to open this audio file",false);}
    }
    private void step(int amount){if(tracks.isEmpty())loadTracks();if(tracks.isEmpty()){publish("No music found",false);return;}if(index<0)index=amount>0?-1:0;playAt(index+amount);}
    private void toggle(){
        if(player==null){if(index>=0)playAt(index);else{if(tracks.isEmpty())loadTracks();if(!tracks.isEmpty())playAt(0);else publish("No music found",false);}return;}
        try{if(!prepared){return;}if(player.isPlaying()){player.pause();Track t=current();startForeground(NOTIFICATION_ID,notification(t==null?"Seamless":t.title,false));publish(t==null?"Paused":t.title,false);}else{player.start();Track t=current();startForeground(NOTIFICATION_ID,notification(t==null?"Seamless":t.title,true));publish(t==null?"Playing":t.title,true);}}catch(IllegalStateException ignored){}
    }
    private Track current(){return index>=0&&index<tracks.size()?tracks.get(index):null;}
    private void publish(String title,boolean playing){int position=0,duration=0;try{if(player!=null&&prepared){position=player.getCurrentPosition();duration=player.getDuration();}}catch(IllegalStateException ignored){}Intent i=new Intent(BROADCAST_STATE).setPackage(getPackageName()).putExtra("title",title).putExtra("playing",playing).putExtra("position",position).putExtra("duration",duration);sendBroadcast(i);PlayerWidget.refresh(this,title,playing);}
    private void releasePlayer(){if(player!=null){try{player.reset();player.release();}catch(Exception ignored){}player=null;}prepared=false;}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Music playback",NotificationManager.IMPORTANCE_LOW);c.setDescription("Playback controls for Seamless");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private Notification notification(String title,boolean playing){
        Intent open=new Intent(this,MainActivity.class);PendingIntent content=PendingIntent.getActivity(this,10,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent toggle=new Intent(this,PlaybackService.class).setAction(ACTION_TOGGLE);PendingIntent play=PendingIntent.getService(this,11,toggle,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent next=new Intent(this,PlaybackService.class).setAction(ACTION_NEXT);PendingIntent skip=PendingIntent.getService(this,12,next,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Seamless").setContentText(title).setContentIntent(content).setOngoing(playing).setOnlyAlertOnce(true).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pause":"Play",play).addAction(android.R.drawable.ic_media_next,"Next",skip);
        return b.build();
    }
    @Override public void onDestroy(){handler.removeCallbacks(ticker);releasePlayer();super.onDestroy();}
}
