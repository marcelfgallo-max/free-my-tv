package android.graphics;
import java.util.ArrayDeque;
public class Canvas {
 public final StringBuilder svg=new StringBuilder(); private final ArrayDeque<String> stack=new ArrayDeque<>();private String transform="";
 public int saves,restores,texts;
 public void save(){stack.push(transform);saves++;} public void restore(){transform=stack.pop();restores++;}
 public void translate(float x,float y){transform+=" translate("+x+" "+y+")";} public void scale(float x,float y){transform+=" scale("+x+" "+y+")";}
 public static String color(int c){return String.format("#%06x",c&0xffffff);}
 public void drawPath(Path p,Paint ink){svg.append("<path transform=\"").append(transform).append("\" d=\"").append(p.svg).append("\" ");
  if(ink.style==Paint.Style.STROKE)svg.append("fill=\"none\" stroke=\"").append(color(ink.color)).append("\" stroke-width=\"").append(ink.width).append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\"");
  else svg.append("fill=\"").append(color(ink.color)).append("\"");svg.append("/>\n");}
 public void drawText(String s,float x,float y,Paint ink){texts++;svg.append("<text x=\"").append(x).append("\" y=\"").append(y).append("\" fill=\"").append(color(ink.color)).append("\" font-family=\"").append(ink.face).append("\" font-size=\"").append(ink.size).append("\">").append(s.replace("&","&amp;").replace("<","&lt;")).append("</text>\n");}
}
