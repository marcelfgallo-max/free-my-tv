package io.freemytv;
import android.graphics.*;

/** Layout specimen only: photo placeholder and illustrative data, not an Android screenshot. */
public final class DreamPreview {
 public static void main(String[] args) throws Exception {
  for (boolean night : new boolean[] {false,true}) {
   int bg=Theme.color(Theme.BACKGROUND,night),surface=Theme.color(Theme.SURFACE,night);
   int ink=Theme.color(Theme.TEXT_PRIMARY,night),muted=night?0xffb8b9bf:0xff62636a;
   int line=Theme.color(Theme.SOFT_DIVIDER,night);
   String mode=night?"night":"day";
   Canvas c=new Canvas();
   Preview.rect(c,64,64,1792,952,Theme.DREAM_PHOTO_RADIUS,night?0xff414b4a:0xffb4bfbc);
   // Placeholder, deliberately not a fetched or generated film photograph.
   Preview.text(c,"FOTO DO CATÁLOGO",128,212,28,night?0xffd0d6d3:0xff414a45);
   Preview.text(c,"Área ilustrativa. Na TV, a foto real mantém zoom lento e troca suave.",128,252,19,night?0xffd0d6d3:0xff414a45);
   float left=1824-Theme.DREAM_CLOCK_CARD_WIDTH,x=left+32,edge=1792;
   Preview.rect(c,left,96,Theme.DREAM_CLOCK_CARD_WIDTH,352,Theme.DREAM_PANEL_RADIUS,surface);
   Preview.text(c,"QUI, 22 OUT",x,156,23,ink);Preview.text(c,"18°",1750,156,23,ink);
   DrawerSymbols.draw(c,"cloud",1714,146,32,ink);Preview.rule(c,x,180,edge-x,false,line);
   String time="07:44";float height=DotMatrix.fittedHeight(time,Theme.DREAM_CLOCK_HEIGHT,edge-x);
   float width=DotMatrix.widthUnits(time)*height/DotMatrix.HEIGHT;
   new DotDisplay().draw(c,time,x+(edge-x-width)/2,344,height,edge-x,Preview.ink(ink,132));
   Preview.text(c,"$ 5,03",x,394,22,muted);Preview.text(c,"€ 5,64",x+154,394,22,muted);
   Preview.text(c,"↗ 206K",x+308,394,22,muted);Preview.text(c,"B 414K",x+462,394,22,muted);
   Preview.rect(c,96,774,Theme.DREAM_TITLE_WIDTH+64,210,Theme.DREAM_PANEL_RADIUS,surface);
   Path dot=new Path();dot.addCircle(132,813,4,Path.Direction.CW);c.drawPath(dot,Preview.ink(Theme.ACCENT_ORANGE,0));
   Preview.text(c,"ESTA MANHÃ",150,822,22,muted);Preview.rule(c,128,840,Theme.DREAM_TITLE_WIDTH,false,line);
   Paint title=Preview.ink(ink,Theme.DREAM_TITLE_SIZE);title.face="sans-serif";
   c.drawText("Uma história para descobrir",128,908,title);
   Preview.text(c,"Aventura · 2h 10min · Streaming",128,952,22,muted);
   Preview.save(c,args[0]+"/screensaver-"+mode+".svg","0 0 1920 1080",bg);
   c=new Canvas();float h=Theme.DREAM_FALLBACK_CLOCK_HEIGHT,base=540+h/2-10;
   Preview.rect(c,300,base-h-130,1320,h+274,Theme.DREAM_PANEL_RADIUS,surface);
   Preview.centered(c,"QUINTA-FEIRA, 22 DE OUTUBRO",960,base-h-48,26,ink);
   width=DotMatrix.widthUnits(time)*h/DotMatrix.HEIGHT;
   new DotDisplay().draw(c,time,960-width/2,base,h,1120,Preview.ink(ink,h));
   DrawerSymbols.draw(c,"cloud",780,base+64,32,ink);
   Preview.text(c,"18°    Máx. 26°  Mín. 16°",810,base+76,24,muted);
   Preview.centered(c,"$ 5,03       € 5,64       ↗ 206.495       B 414.333",960,972,24,muted);
   Preview.save(c,args[0]+"/screensaver-fallback-"+mode+".svg","0 0 1920 1080",bg);
  }
 }
}
