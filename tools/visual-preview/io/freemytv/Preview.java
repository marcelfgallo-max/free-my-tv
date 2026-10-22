package io.freemytv;
import android.graphics.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Preview {
 static void require(boolean x,String message){if(!x)throw new AssertionError(message);}
 static Paint ink(int color,float size){Paint p=new Paint(1);p.setColor(color);p.size=size;return p;}
 static void rect(Canvas c,float x,float y,float w,float h,float radius,int color){Path p=new Path();p.addRoundRect(x,y,x+w,y+h,radius,radius,Path.Direction.CW);c.drawPath(p,ink(color,0));}
 static void text(Canvas c,String s,float x,float y,float size,int color){c.drawText(s,x,y,ink(color,size));}
 static void centered(Canvas c,String s,float x,float y,float size,int color){text(c,s,x-s.length()*size*.3f,y,size,color);}
 static void rule(Canvas c,float x,float y,float end,boolean vertical,int color){Paint p=ink(color,0);Path path=new Path();for(float n=.85f;n<end-.85f;n+=Theme.DOTTED_RULE_STEP)path.addCircle(x+(vertical?0:n),y+(vertical?n:0),Theme.DOTTED_RULE_RADIUS,Path.Direction.CW);c.drawPath(path,p);}
 static void save(Canvas c,String file,String viewBox,int bg)throws Exception{Files.writeString(Paths.get(file),"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""+viewBox+"\"><rect x=\"0\" y=\"0\" width=\"1920\" height=\"1080\" fill=\""+Canvas.color(bg)+"\"/>"+c.svg+"</svg>");}
 public static void main(String[] args)throws Exception{
  Canvas test=new Canvas();Paint testInk=ink(0xff000000,24);DotDisplay display=new DotDisplay();
  display.draw(test,"10:51",0,100,92,400,testInk);int before=Path.rebuilds,circles=Path.circles;
  testInk.setColor(0xffffffff);display.draw(test,"10:51",0,100,92,400,testInk);
  require(Path.rebuilds==before&&Path.circles==circles,"theme change rebuilt number geometry");
  display.draw(test,"10:52",0,100,92,400,testInk);require(Path.rebuilds==before+1,"new minute did not rebuild");
  display.draw(test,"—",0,100,92,400,testInk);require(test.texts==1,"text fallback missing");
  for(String name:new String[]{"controller","settop","airplay","handheld","live_tv","apps","settings","tv_settings","bedtime","cloud","rainy","sunny","foggy","thunderstorm"})require(DrawerSymbols.draw(test,name,50,50,48,0xff000000),"missing "+name);
  require(!DrawerSymbols.draw(test,"unknown",50,50,48,0xff000000),"unknown symbol must use original fallback");
  require(test.saves==test.restores,"unbalanced Canvas state");
  require(DotMatrix.fittedHeight("18°",142,280,true)==142,"temperature unnecessarily shrinks");
  for(int n=-99;n<200;n++){String value=n+"°";float h=DotMatrix.fittedHeight(value,142,280,true);require(DotMatrix.widthUnits(value,true)*h/DotMatrix.HEIGHT<=280.001f,"temperature overflow");}
  System.out.println("Production renderer host checks passed: cache reuse, theme recolor, minute change, fallback, Canvas state, 14 symbols, compact temperatures. SVG is a host preview, not Android raster output.");
  for(boolean night:new boolean[]{false,true}){
   String mode=night?"night":"day";int bg=Theme.color(Theme.BACKGROUND,night),surface=Theme.color(Theme.SURFACE,night),primary=Theme.color(Theme.TEXT_PRIMARY,night),muted=night?0xffb8b9bf:0xff62636a,rule=Theme.color(Theme.SOFT_DIVIDER,night);
   Canvas c=new Canvas();float width=new DotDisplay().draw(c,"10:51",96,155,Theme.CLOCK_DOT_HEIGHT,400,ink(primary,124));
   Paint greeting=ink(muted,40);greeting.face="sans-serif";c.drawText(night?"Good evening, Marcel":"Good morning, Marcel",96+width+48,155,greeting);
   save(c,args[0]+"/clock-"+mode+".svg","64 40 1400 145",bg);
   c=new Canvas();text(c,"INPUTS",96,418,25,primary);text(c,"SHORTCUTS",984,418,25,primary);
   String[] names={"PLAYSTATION 5","VIVO TV","APPLE TV","SWITCH 2","LIVE TV","ALL APPS","HOME SETTINGS","TV SETTINGS","SLEEP TIMER"};String[] symbols={"controller","settop","airplay","handheld","live_tv","apps","settings","tv_settings","bedtime"};
   for(int i=0;i<9;i++){
    int count=i<5?5:4,index=i<5?i:i-5;float cw=(840-(count-1)*Theme.DRAWER_CELL_GAP)/count,x=(i<5?96:984)+index*(cw+Theme.DRAWER_CELL_GAP);
    rect(c,x,450,cw,168,32,i==0?Theme.ACCENT_ORANGE:surface);DrawerSymbols.draw(c,symbols[i],x+cw/2,450+168*.40f,Theme.DRAWER_ICON_SIZE,i==0?0xffffffff:primary);
    float size=Math.min(20,(cw-16)/(.6f*names[i].length()));centered(c,names[i],x+cw/2,450+168*.79f,size,i==0?Theme.ACCENT_INK_SMALL:primary);
   }
   rect(c,96,650,840,368,32,surface);rect(c,984,650,840,368,32,surface);
   text(c,"SÃO PAULO",120,702,25,primary);text(c,"CLOUDY",775,702,21,primary);DrawerSymbols.draw(c,"cloud",892,693,Theme.WEATHER_CONDITION_ICON_SIZE,primary);rule(c,120,727,792,false,rule);
   new DotDisplay(true).draw(c,"18°",120,905,Theme.TEMPERATURE_DOT_HEIGHT,280,ink(primary,184));text(c,"H 25° · L 14°",120,962,20,muted);
   float hx=424,hw=(912-hx)/5;for(int i=0;i<5;i++){float cx=hx+hw*(i+.5f);rule(c,hx+hw*i,750,175,true,rule);centered(c,(11+i)+":00",cx,778,22,muted);DrawerSymbols.draw(c,i<3?"cloud":"rainy",cx,832,Theme.WEATHER_ICON_SIZE,primary);centered(c,i<2?"18°":"17°",cx,902,32,primary);}
   text(c,"Sunrise 5:28 · Sunset 18:14",hx,962,20,muted);
   text(c,"MARKETS",1008,702,25,primary);text(c,"CHANGE",1724,702,21,primary);rule(c,1008,727,792,false,rule);
   String[] coins={"DOLLAR","EURO","IBOVESPA","BITCOIN"},marks={"$","€","↗","B"},prices={"R$ 5.02","R$ 5.64","206,495","R$ 424,795"},changes={"0.00%","+0.08%","-0.9%","+1.5%"};
   for(int j=0;j<4;j++){float base=770+64*j;int color=j==2?Theme.color(Theme.NEGATIVE,night):j==0?muted:Theme.color(Theme.POSITIVE,night);text(c,marks[j],1010,base,26,muted);if(j==3){Paint bp=ink(muted,0);bp.setStyle(Paint.Style.STROKE);bp.setStrokeWidth(1.7f);Path b=new Path();b.moveTo(1015,base-24);b.lineTo(1015,base+5);b.moveTo(1021,base-24);b.lineTo(1021,base+5);c.drawPath(b,bp);}text(c,coins[j],1052,base,26,primary);text(c,prices[j],1650-prices[j].length()*26*.6f,base,26,primary);text(c,changes[j],1800-changes[j].length()*24*.6f,base,24,color);rule(c,1008,791+64*j,792,false,rule);
    Paint stroke=ink(color,0);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(2);Path chart=new Path();for(int k=0;k<30;k++){float x=1305+k*5.2f,y=base-10+(float)(Math.sin(k*1.6+j)*4+Math.sin(k*.24+j)*8);if(k==0)chart.moveTo(x,y);else chart.lineTo(x,y);}c.drawPath(chart,stroke);
   }
   save(c,args[0]+"/drawer-"+mode+".svg","64 380 1792 660",bg);
  }
 }
}
