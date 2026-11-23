package com.example.workoutwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

public class WorkoutWidget extends AppWidgetProvider {
 static final String ACTION_DONE="com.example.workoutwidget.ACTION_DONE";
 static final String ACTION_TOGGLE="com.example.workoutwidget.ACTION_TOGGLE";
 static final String ACTION_WEIGHT="com.example.workoutwidget.ACTION_WEIGHT";
 static final String[] titles={"Плечи · Грудь · Ноги","Спина · Руки","Всё тело"};
 static final String[][] names={{"Махи гантелями","Жим гантелей на полу","Приседания с гантелью"},{"Тяга гантели в наклоне","Молотковые сгибания","Разгибание из-за головы"},{"Тяга штанги в наклоне","Махи гантелями","Румынская тяга"}};
 static final String[][] reps={{"2 × 12–15","2 × 8–12","2 × 10–12"},{"2 × 10–12","2 × 10–12","2 × 10–12"},{"2 × 10–12","2 × 12–15","2 × 10–12"}};
 static final String[][] symbols={{"↗","▰","▾"},{"↙","💪","↑"},{"↙","↗","▾"}};
 static final int[][] poseIds={{R.drawable.exercise_pose_0,R.drawable.exercise_pose_1,R.drawable.exercise_pose_2},{R.drawable.exercise_pose_3,R.drawable.exercise_pose_4,R.drawable.exercise_pose_5},{R.drawable.exercise_pose_6,R.drawable.exercise_pose_7,R.drawable.exercise_pose_8}};
 static final int[] iconIds={R.id.icon1,R.id.icon2,R.id.icon3};
 static final String[][] exerciseKeys={{"lateral_raise","floor_press","goblet_squat"},{"one_arm_row","hammer_curl","overhead_extension"},{"barbell_row","lateral_raise","romanian_deadlift"}};
 static final int[] nameIds={R.id.name1,R.id.name2,R.id.name3};
 static final int[] repIds={R.id.reps1,R.id.reps2,R.id.reps3};
 static final int[] weightIds={R.id.weight1,R.id.weight2,R.id.weight3};
 static final int[] checkIds={R.id.check1,R.id.check2,R.id.check3};
 static final int[] plusIds={R.id.plus1,R.id.plus2,R.id.plus3};
 static final int[] minusIds={R.id.minus1,R.id.minus2,R.id.minus3};
 static LocalDate nextDate(Context c){
  SharedPreferences p=c.getSharedPreferences("workout",0);
  LocalDate today=LocalDate.now();
  for(int i=0;i<15;i++) {LocalDate d=today.plusDays(i); DayOfWeek w=d.getDayOfWeek(); if(w==DayOfWeek.MONDAY||w==DayOfWeek.WEDNESDAY||w==DayOfWeek.FRIDAY) if(!p.getBoolean("done_"+d,false)) return d;}
  return today;
 }
 static int index(LocalDate d){return d.getDayOfWeek()==DayOfWeek.MONDAY?0:d.getDayOfWeek()==DayOfWeek.WEDNESDAY?1:2;}
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c); int[] ids=m.getAppWidgetIds(new ComponentName(c,WorkoutWidget.class)); if(ids.length>0) new WorkoutWidget().onUpdate(c,m,ids);}
 static PendingIntent action(Context c,int widgetId,String action,String date,int exercise,int delta){
  Intent i=new Intent(c,WorkoutWidget.class).setAction(action).putExtra("date",date).putExtra("exercise",exercise).putExtra("delta",delta);
  int code=widgetId*100+exercise*10+(ACTION_TOGGLE.equals(action)?1:ACTION_DONE.equals(action)?2:delta>0?3:4);
  return PendingIntent.getBroadcast(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 @Override public void onReceive(Context c,Intent intent){
  super.onReceive(c,intent);
  String a=intent.getAction();
  if(!ACTION_DONE.equals(a)&&!ACTION_TOGGLE.equals(a)&&!ACTION_WEIGHT.equals(a))return;
  String date=intent.getStringExtra("date");if(date==null)return;
  SharedPreferences p=c.getSharedPreferences("workout",0);
  int e=intent.getIntExtra("exercise",0);if(e<0||e>2)return;
  if(ACTION_DONE.equals(a)){
   if(!date.equals(nextDate(c).toString())) return;
   boolean all=true;
   for(int j=0;j<3;j++) if(!p.getBoolean("check_"+date+"_"+j,false)) all=false;
   if(all) p.edit().putBoolean("done_"+date,true).apply();
  }
  if(ACTION_TOGGLE.equals(a)){if(!date.equals(nextDate(c).toString()))return;String key="check_"+date+"_"+e;p.edit().putBoolean(key,!p.getBoolean(key,false)).apply();}
  if(ACTION_WEIGHT.equals(a)){if(!date.equals(nextDate(c).toString()))return;int day=index(LocalDate.parse(date));String key="weight_ex_"+exerciseKeys[day][e];float current=p.getFloat(key,p.getFloat("weight_"+day+"_"+e,0));float value=Math.max(0,current+intent.getIntExtra("delta",0));p.edit().putFloat(key,value).apply();}
  refresh(c);
 }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
  LocalDate d=nextDate(c);String date=d.toString();int idx=index(d);SharedPreferences p=c.getSharedPreferences("workout",0);
  String startText=p.getString("program_start_date",null);
  if(startText==null){startText=LocalDate.now().toString();p.edit().putString("program_start_date",startText).apply();}
  LocalDate start;
  try{start=LocalDate.parse(startText);}catch(Exception ignored){start=LocalDate.now();}
  int sets=ChronoUnit.DAYS.between(start,d)>=14?3:2;
  int checked=0;for(int j=0;j<3;j++)if(p.getBoolean("check_"+date+"_"+j,false))checked++;
  for(int id:ids){
   RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
   v.setTextViewText(R.id.day,"БЛИЖАЙШАЯ · "+date);
   v.setTextViewText(R.id.title,titles[idx]);
   v.setTextViewText(R.id.count,checked+"/3");
   v.setTextViewText(R.id.subtitle,"3 упражнения · "+sets+" подхода · отдых 60–90 сек");
   v.setTextViewText(R.id.progress,"Выполнено "+checked+" из 3");
   for(int j=0;j<3;j++){
    v.setTextViewText(nameIds[j],names[idx][j]);
    v.setImageViewResource(iconIds[j],poseIds[idx][j]);
    v.setTextViewText(repIds[j],sets+" × "+reps[idx][j].substring(reps[idx][j].indexOf("×")+1).trim());
    float weight=p.getFloat("weight_ex_"+exerciseKeys[idx][j],p.getFloat("weight_"+idx+"_"+j,0));
    v.setTextViewText(weightIds[j],((weight==(int)weight)?Integer.toString((int)weight):Float.toString(weight))+" кг");
    boolean done=p.getBoolean("check_"+date+"_"+j,false);
    v.setTextViewText(checkIds[j],done?"✓ Готово":"○ Отметить");
    v.setTextColor(checkIds[j],done?Color.WHITE:Color.rgb(175,175,175));
    v.setOnClickPendingIntent(checkIds[j],action(c,id,ACTION_TOGGLE,date,j,0));
    v.setOnClickPendingIntent(plusIds[j],action(c,id,ACTION_WEIGHT,date,j,1));
    v.setOnClickPendingIntent(minusIds[j],action(c,id,ACTION_WEIGHT,date,j,-1));
   }
   v.setTextViewText(R.id.complete,checked==3?"Завершить →":"Отметь упражнения");
   v.setOnClickPendingIntent(R.id.complete,action(c,id,ACTION_DONE,date,0,0));
   m.updateAppWidget(id,v);
  }
 }
}
