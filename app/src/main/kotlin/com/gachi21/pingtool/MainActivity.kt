package com.gachi21.pingtool

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : Activity() {
 private lateinit var host: EditText
 private lateinit var count: EditText
 private lateinit var interval: EditText
 private lateinit var timeout: EditText
 private lateinit var output: TextView
 private lateinit var stats: TextView
 private lateinit var start: Button
 private val exec=Executors.newSingleThreadExecutor()
 private val ui=Handler(Looper.getMainLooper())
 private val running=AtomicBoolean(false)
 private var sent=0; private var received=0; private var min=Long.MAX_VALUE; private var max=0L; private var total=0L; private var prev=-1L
 private val log=StringBuilder()

 override fun onCreate(b:Bundle?){super.onCreate(b);buildUi()}
 private fun buildUi(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,14,18,12);setBackgroundColor(Color.rgb(16,19,24))}
  root.addView(TextView(this).apply{text="◉ ANDROID PING TOOL";textSize=21f;setTextColor(Color.rgb(226,232,240))},lp())
  root.addView(TextView(this).apply{text="Continuous network diagnostics • terminal style";textSize=12f;setTextColor(Color.rgb(148,163,184))},lp())
  val hr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  host=edit("Host / IP","1.1.1.1");hr.addView(host,LinearLayout.LayoutParams(0,56.dp(),1f))
  val sp=Spinner(this).apply{
   adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Preset","1.1.1.1","8.8.8.8","google.com","cloudflare.com"))
   onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
    override fun onNothingSelected(p:AdapterView<*>?){}
    override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){if(pos>0)host.setText(p?.getItemAtPosition(pos).toString())}
   }
  }
  hr.addView(sp,LinearLayout.LayoutParams(120.dp(),56.dp()));root.addView(hr,lp())
  val sr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  count=edit("Count (0=∞)","0");interval=edit("Interval ms","1000");timeout=edit("Timeout ms","1500")
  sr.addView(count,weightLp());sr.addView(interval,weightLp());sr.addView(timeout,weightLp());root.addView(sr,lp())
  stats=TextView(this).apply{textSize=12f;setTextColor(Color.rgb(203,213,225));text="Packets 0/0 • Loss 0% • Min — • Avg — • Max — • Jitter —"};root.addView(stats,lp())
  output=TextView(this).apply{textSize=12f;setTextColor(Color.rgb(148,163,184));setBackgroundColor(Color.rgb(9,11,14));setPadding(12,10,12,10);typeface=android.graphics.Typeface.MONOSPACE}
  val scroll=ScrollView(this).apply{addView(output)};root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val br=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  start=Button(this).apply{text="START";setOnClickListener{if(running.get())stopPing() else startPing()}}
  val clear=Button(this).apply{text="CLEAR";setOnClickListener{if(!running.get())reset()}}
  val copy=Button(this).apply{text="COPY";setOnClickListener{(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Ping",log.toString()))}}
  val share=Button(this).apply{text="SHARE";setOnClickListener{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,log.toString());putExtra(Intent.EXTRA_SUBJECT,"Android Ping report")},"Share"))}}
  listOf(start,clear,copy,share).forEach{br.addView(it,weightLp())};root.addView(br,lp());setContentView(root)
 }
 private fun startPing(){
  val h=host.text.toString().trim();if(h.isEmpty()){host.error="Enter host";return}
  reset();running.set(true);inputs(false);start.text="STOP"
  val maxCount=count.text.toString().toIntOrNull()?.coerceAtLeast(0)?:0
  val delay=interval.text.toString().toLongOrNull()?.coerceIn(100,60000)?:1000
  val to=timeout.text.toString().toIntOrNull()?.coerceIn(100,10000)?:1500
  exec.execute{
   var seq=1
   while(running.get()&&(maxCount==0||sent<maxCount)){
    sent++;val t=System.nanoTime()
    try{
     val a=InetAddress.getByName(h);val ok=a.isReachable(to)
     val ms=((System.nanoTime()-t)/1_000_000.0).roundToInt().toLong()
     if(ok){
      received++;min=minOf(min,ms);max=maxOf(max,ms);total+=ms
      val jit=if(prev<0)0 else abs(ms-prev);prev=ms
      line("reply from "+a.hostAddress+": seq="+seq+" time="+ms+"ms jitter="+jit+"ms")
     }else line("timeout from "+h+": seq="+seq+" timeout="+to+"ms")
    }catch(e:Exception){line("error: seq="+seq+" "+e.javaClass.simpleName+": "+(e.message?:"unreachable"))}
    seq++;stats()
    if(running.get())try{Thread.sleep(delay)}catch(_:InterruptedException){break}
   }
   ui.post{stopPing()}
  }
 }
 private fun stopPing(){running.set(false);start.text="START";inputs(true)}
 private fun reset(){sent=0;received=0;min=Long.MAX_VALUE;max=0;total=0;prev=-1;log.clear();output.text="";stats()}
 private fun line(s:String){val now=SimpleDateFormat("HH:mm:ss",Locale.US).format(Date());log.append("[").append(now).append("]  ").append(s).append('\n');ui.post{output.text=log.toString();(output.parent as ScrollView).fullScroll(View.FOCUS_DOWN)}}
 private fun stats(){ui.post{val loss=if(sent==0)0 else ((sent-received)*100.0/sent).roundToInt();val av=if(received==0)"—" else (total/received).toString()+"ms";stats.text="Packets "+received+"/"+sent+" • Loss "+loss+"% • Min "+(if(min==Long.MAX_VALUE)"—" else min.toString()+"ms")+" • Avg "+av+" • Max "+(if(received==0)"—" else max.toString()+"ms")+" • Jitter live"}}
 private fun inputs(e:Boolean){host.isEnabled=e;count.isEnabled=e;interval.isEnabled=e;timeout.isEnabled=e}
 private fun edit(h:String,v:String)=EditText(this).apply{hint=h;setText(v);setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true);setPadding(10,0,10,0)}
 private fun lp()=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply{bottomMargin=6.dp()}
 private fun weightLp()=LinearLayout.LayoutParams(0,56.dp(),1f).apply{marginEnd=5.dp()}
 private fun Int.dp()=(this*resources.displayMetrics.density).roundToInt()
 override fun onDestroy(){running.set(false);exec.shutdownNow();super.onDestroy()}
}