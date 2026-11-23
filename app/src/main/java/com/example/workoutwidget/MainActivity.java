package com.example.workoutwidget;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class MainActivity extends Activity { @Override public void onCreate(Bundle b){super.onCreate(b);TextView t=new TextView(this);t.setText("Мои тренировки\n\nДобавь виджет «Мои тренировки» на главный экран.\n\nПонедельник: плечи, грудь, ноги\nСреда: спина и руки\nПятница: всё тело\n\nНа виджете нажми «Отметить выполненной», чтобы перейти к следующей тренировке.");t.setTextSize(19);t.setPadding(30,45,30,30);setContentView(t);} }