package android.graphics;
public class Paint {
 public static final int ANTI_ALIAS_FLAG=1;
 public enum Style { FILL, STROKE } public enum Cap { ROUND } public enum Join { ROUND }
 public String face="monospace"; public Style style=Style.FILL; public int color=0xff292b2e; public float width=1,size=24;
 public Paint(int flags){} public void setStyle(Style v){style=v;} public void setColor(int v){color=v;}
 public void setStrokeWidth(float v){width=v;} public void setStrokeCap(Cap v){} public void setStrokeJoin(Join v){}
 public float measureText(String s){return s.length()*size*.6f;}
}
