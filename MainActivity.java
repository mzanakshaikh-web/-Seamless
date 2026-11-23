package com.seamless.player;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST=9;
    private final ArrayList<Track> tracks=new ArrayList<>();
    private LinearLayout root,list,controls;
    private TextView title,status,section;
    private SeekBar seek;
    private int accent=0xFFB7A58A;
    private boolean seeking=false;
    private String currentUri="";
    private static class Track{Uri uri;String title,artist;Track(Uri u,String t,String a){uri=u;title=t;artist=a;}}
    private final BroadcastReceiver stateReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(i==null)return;String t=i.getStringExtra("title");boolean playing=i.getBooleanExtra("playing",false);if(t!=null){title.setText(t);status.setText(playing?"NOW PLAYING · LOCAL AUDIO":"PAUSED · LOCAL AUDIO");int duration=i.getIntExtra("duration",0),position=i.getIntExtra("position",0);if(!seeking)seek.setProgress(duration>0?(int)(1000L*position/duration):0);}}};

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);accent=getPreferences(0).getInt("accent",accent);build();registerStateReceiver();requestAudioPermissionOrLoad();}
    private void registerStateReceiver(){IntentFilter f=new IntentFilter(PlaybackService.BROADCAST_STATE);if(Build.VERSION.SDK_INT>=33)registerReceiver(stateReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(stateReceiver,f);}
    private boolean hasPermission(){return Build.VERSION.SDK_INT>=33?checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)==PackageManager.PERMISSION_GRANTED:Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
    private void requestAudioPermissionOrLoad(){if(hasPermission())loadLibrary();else if(Build.VERSION.SDK_INT>=23)requestPermissions(new String[]{Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_AUDIO:Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSION_REQUEST);}
    private void build(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,26,22,18);root.setBackgroundColor(Color.BLACK);setContentView(root);
        TextView brand=text("seamless",30,Color.WHITE);root.addView(brand);root.addView(text("YOUR MUSIC. YOUR SPACE.",10,0xFF999999));
        title=text("Choose a track",22,Color.WHITE);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=30;root.addView(title,tp);
        status=text("AMOLED · LOCAL MUSIC",12,0xFFAAAAAA);root.addView(status);
        seek=new SeekBar(this);seek.setMax(1000);root.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){}public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){seeking=false;service(PlaybackService.ACTION_SEEK,null);}});
        controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);root.addView(controls);addButton(controls,"PREV",()->service(PlaybackService.ACTION_PREV,null));addButton(controls,"PLAY / PAUSE",()->service(PlaybackService.ACTION_TOGGLE,null));addButton(controls,"NEXT",()->service(PlaybackService.ACTION_NEXT,null));
        section=text("YOUR LIBRARY",12,accent);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=24;root.addView(section,sp);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);
        Button theme=new Button(this);theme.setText("CUSTOMIZE APPEARANCE");theme.setTextColor(accent);theme.setBackgroundColor(0xFF171717);root.addView(theme);theme.setOnClickListener(v->themeDialog());applyAccent();
    }
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private void addButton(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setTextColor(accent);parent.addView(b);b.setOnClickListener(v->action.run());}
    private void service(String action,Uri uri){Intent i=new Intent(this,PlaybackService.class).setAction(action);if(PlaybackService.ACTION_SEEK.equals(action))i.putExtra(PlaybackService.EXTRA_POSITION,seek.getProgress());if(uri!=null)i.putExtra(PlaybackService.EXTRA_URI,uri.toString());if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
    private void loadLibrary(){
        tracks.clear();String[] columns={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST};
        try(Cursor c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,columns,MediaStore.Audio.Media.IS_MUSIC+"!=0",null,MediaStore.Audio.Media.TITLE+" ASC")){if(c!=null)while(c.moveToNext())tracks.add(new Track(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,c.getString(0)),c.getString(1),c.getString(2)));}catch(SecurityException ignored){}
        list.removeAllViews();if(tracks.isEmpty()){list.addView(text("No local music found. Add audio files to your phone and reopen Seamless.",14,0xFFAAAAAA));return;}
        for(Track track:tracks){TextView row=text("♫   "+track.title+"\n      "+track.artist,16,Color.WHITE);row.setPadding(8,15,8,15);list.addView(row);row.setOnClickListener(v->{title.setText(track.title);currentUri=track.uri.toString();service(PlaybackService.ACTION_PLAY,track.uri);});}
    }
    private void themeDialog(){String[] names={"Warm stone","Soft sage","Lavender","Ice blue","Amber"};int[] colors={0xFFB7A58A,0xFF9CAF9A,0xFFB5A5CC,0xFF91B7C7,0xFFD5A35B};new android.app.AlertDialog.Builder(this).setTitle("Accent color").setItems(names,(d,w)->{accent=colors[w];getPreferences(0).edit().putInt("accent",accent).apply();applyAccent();status.setText("THEME UPDATED · "+names[w].toUpperCase());}).setNegativeButton("Close",null).show();}
    private void applyAccent(){section.setTextColor(accent);for(int i=0;i<controls.getChildCount();i++)((Button)controls.getChildAt(i)).setTextColor(accent);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==PERMISSION_REQUEST){if(grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)loadLibrary();else{list.removeAllViews();list.addView(text("Allow music access to browse your local audio library. You can grant it in App info → Permissions.",14,0xFFAAAAAA));}}}
    @Override protected void onDestroy(){try{unregisterReceiver(stateReceiver);}catch(Exception ignored){}super.onDestroy();}
}
