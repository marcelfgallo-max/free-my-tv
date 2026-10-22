package io.freemytv;
import android.graphics.*;

/** Layout specimen only: photo placeholder and illustrative data, not an Android screenshot. */
public final class DreamPreview {
 public static void main(String[] args) throws Exception {
  for (boolean night : new boolean[] {false,true}) {
   int bg=Theme.color(Theme.BACKGROUND,night),surface=(Theme.color(Theme.SURFACE,night)&0x00ffffff)|(Theme.DREAM_SURFACE_ALPHA<<24);
   int ink=Theme.color(Theme.TEXT_PRIMARY,night),muted=night?0xffb8b9bf:0xff62636a;
   int line=Theme.color(Theme.SOFT_DIVIDER,night);
   String mode=night?"night":"day";
   Canvas c=new Canvas();
   Preview.rect(c,64,64,1792,952,Theme.DREAM_PHOTO_RADIUS,night?0xff414b4a:0xffb4bfbc);
   // Placeholder, deliberately not a fetched or generated film photograph.
   Preview.text(c,"FOTO DO CATÁLOGO",128,212,28,night?0xffd0d6d3:0xff414a45);
   Preview.text(c,"Área ilustrativa. Na TV, a foto real mantém zoom lento e troca suave.",128,252,19,night?0xffd0d6d3:0xff414a45);
   float left=1824-Theme.DREAM_CLOCK_CARD_WIDTH,x=left+Theme.DREAM_PANEL_PADDING,edge=1824-Theme.DREAM_PANEL_PADDING;
   Preview.rect(c,left,96,Theme.DREAM_CLOCK_CARD_WIDTH,Theme.DREAM_CLOCK_CARD_HEIGHT,Theme.DREAM_PANEL_RADIUS,surface);
   Preview.text(c,"THURSDAY, OCTOBER 22",x,144,Theme.DREAM_HEADER_SIZE,ink);Preview.text(c,"18°",1764,144,Theme.DREAM_HEADER_SIZE,Theme.ACCENT_ORANGE);
   DrawerSymbols.draw(c,"cloud",1736,136,32,Theme.ACCENT_ORANGE);Preview.rule(c,x,Theme.DREAM_CLOCK_RULE_Y,edge-x,false,line);
   String time="07:44";float height=DotMatrix.fittedHeight(time,Theme.DREAM_CLOCK_HEIGHT,edge-x);
   float width=DotMatrix.widthUnits(time)*height/DotMatrix.HEIGHT;
   new DotDisplay(false,true).draw(c,time,x+(edge-x-width)/2,Theme.DREAM_CLOCK_BASELINE,height,edge-x,Preview.ink(ink,116),Preview.ink(Theme.ACCENT_ORANGE,0));
   Preview.rect(c,96,800,Theme.DREAM_TITLE_WIDTH+48,184,Theme.DREAM_PANEL_RADIUS,surface);
   Path dot=new Path();dot.addCircle(124,837,4,Path.Direction.CW);c.drawPath(dot,Preview.ink(Theme.ACCENT_ORANGE,0));
   Preview.text(c,"ESTA MANHÃ",142,846,22,muted);Preview.rule(c,120,864,Theme.DREAM_TITLE_WIDTH,false,line);
   Paint title=Preview.ink(ink,Theme.DREAM_TITLE_SIZE);title.face="sans-serif";
   c.drawText("Matchbox the Movie",120,920,title);
   Preview.text(c,"Action · 2h 7m · Apple TV",120,960,Theme.DREAM_META_SIZE,muted);
   Preview.save(c,args[0]+"/screensaver-"+mode+".svg","0 0 1920 1080",bg);
   c=new Canvas();float h=Theme.DREAM_FALLBACK_CLOCK_HEIGHT,base=540+h/2-10;
   Preview.rect(c,300,base-h-130,1320,h+274,Theme.DREAM_PANEL_RADIUS,surface);
   Preview.centered(c,"QUINTA-FEIRA, 22 DE OUTUBRO",960,base-h-48,26,ink);
   width=DotMatrix.widthUnits(time)*h/DotMatrix.HEIGHT;
   new DotDisplay(false,true).draw(c,time,960-width/2,base,h,1120,Preview.ink(ink,h),Preview.ink(Theme.ACCENT_ORANGE,0));
   DrawerSymbols.draw(c,"cloud",780,base+64,32,Theme.ACCENT_ORANGE);
   Preview.text(c,"18°",810,base+76,24,Theme.ACCENT_ORANGE);
   Preview.text(c,"Máx. 26°  Mín. 16°",890,base+76,24,muted);
   Preview.centered(c,"$ 5,03       € 5,64       ↗ 206.495       B 414.333",960,972,24,muted);
   Preview.save(c,args[0]+"/screensaver-fallback-"+mode+".svg","0 0 1920 1080",bg);
  }
 }
}
