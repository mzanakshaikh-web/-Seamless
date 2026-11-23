package com.seamless.player;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Build;
import android.widget.RemoteViews;

public class PlayerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    static void refresh(Context c,String title,boolean playing){AppWidgetManager m=AppWidgetManager.getInstance(c);ComponentName cn=new ComponentName(c,PlayerWidget.class);for(int id:m.getAppWidgetIds(cn)){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);v.setTextViewText(R.id.widget_title,title);v.setTextViewText(R.id.widget_play,playing?"Ⅱ":"▶");wire(c,v);m.updateAppWidget(id,v);}}
    private static void wire(Context c,RemoteViews v){v.setOnClickPendingIntent(R.id.widget_play,serviceIntent(c,PlaybackService.ACTION_TOGGLE,21));v.setOnClickPendingIntent(R.id.widget_next,serviceIntent(c,PlaybackService.ACTION_NEXT,22));Intent open=new Intent(c,MainActivity.class);v.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(c,23,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}
    private static PendingIntent serviceIntent(Context c,String action,int request){Intent i=new Intent(c,PlaybackService.class).setAction(action);return PendingIntent.getService(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private static void update(Context c,AppWidgetManager m,int id){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);wire(c,v);m.updateAppWidget(id,v);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);String a=i.getAction();if("com.seamless.player.PLAY_PAUSE".equals(a)){Intent s=new Intent(c,PlaybackService.class).setAction(PlaybackService.ACTION_TOGGLE);start(c,s);}else if("com.seamless.player.NEXT".equals(a)){Intent s=new Intent(c,PlaybackService.class).setAction(PlaybackService.ACTION_NEXT);start(c,s);}}
    private void start(Context c,Intent i){if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
}
