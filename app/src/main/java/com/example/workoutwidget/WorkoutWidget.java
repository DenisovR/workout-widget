package com.example.workoutwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.DayOfWeek;

public class WorkoutWidget extends AppWidgetProvider {
 static final String ACTION_DONE="com.example.workoutwidget.ACTION_DONE";
 static final String[] titles={"Плечи · Грудь · Ноги","Спина · Руки","Всё тело"};
 static final String[][] moves={{"Махи гантелями · 2 × 12–15","Жим гантелей на полу · 2 × 8–12","Приседания с гантелью · 2 × 10–12"},{"Тяга гантели · 2 × 10–12","Молотковые сгибания · 2 × 10–12","Разгибание из-за головы · 2 × 10–12"},{"Тяга штанги в наклоне · 2 × 10–12","Махи гантелями · 2 × 12–15","Румынская тяга · 2 × 10–12"}};
 static LocalDate nextDate(Context c){
  SharedPreferences p=c.getSharedPreferences("workout",0);
  LocalDate today=LocalDate.now();
  for(int i=0;i<15;i++) {LocalDate d=today.plusDays(i); DayOfWeek w=d.getDayOfWeek(); if(w==DayOfWeek.MONDAY||w==DayOfWeek.WEDNESDAY||w==DayOfWeek.FRIDAY) if(!p.getBoolean("done_"+d,false)) return d;}
  return today;
 }
 static int index(LocalDate d){return d.getDayOfWeek()==DayOfWeek.MONDAY?0:d.getDayOfWeek()==DayOfWeek.WEDNESDAY?1:2;}
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c); int[] ids=m.getAppWidgetIds(new ComponentName(c,WorkoutWidget.class)); if(ids.length>0) new WorkoutWidget().onUpdate(c,m,ids);}
 @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent); if(ACTION_DONE.equals(intent.getAction())) {String date=intent.getStringExtra("date"); if(date!=null) c.getSharedPreferences("workout",0).edit().putBoolean("done_"+date,true).apply(); refresh(c);}}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){LocalDate d=nextDate(c);int idx=index(d);for(int id:ids){RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);v.setTextViewText(R.id.day,"БЛИЖАЙШАЯ · "+d.toString());v.setTextViewText(R.id.title,titles[idx]);v.setTextViewText(R.id.e1,"01  "+moves[idx][0]);v.setTextViewText(R.id.e2,"02  "+moves[idx][1]);v.setTextViewText(R.id.e3,"03  "+moves[idx][2]);v.setTextViewText(R.id.progress,"3 упражнения · 2 подхода");Intent intent=new Intent(c,WorkoutWidget.class).setAction(ACTION_DONE).putExtra("date",d.toString());PendingIntent pi=PendingIntent.getBroadcast(c,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.complete,pi);m.updateAppWidget(id,v);}}
}