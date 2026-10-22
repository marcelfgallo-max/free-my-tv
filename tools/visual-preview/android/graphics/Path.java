package android.graphics;
public class Path {
 public enum Direction {CW}
 public static int rebuilds,circles;
 public final StringBuilder svg=new StringBuilder();
 public void moveTo(float x,float y){svg.append("M ").append(x).append(' ').append(y).append(' ');}
 public void lineTo(float x,float y){svg.append("L ").append(x).append(' ').append(y).append(' ');}
 public void cubicTo(float a,float b,float c,float d,float e,float f){svg.append("C ").append(a).append(' ').append(b).append(' ').append(c).append(' ').append(d).append(' ').append(e).append(' ').append(f).append(' ');}
 public void close(){svg.append("Z ");} public void rewind(){svg.setLength(0);rebuilds++;}
 public void addCircle(float x,float y,float r,Direction d){circles++;moveTo(x-r,y);svg.append("a ").append(r).append(' ').append(r).append(" 0 1 0 ").append(2*r).append(" 0 a ").append(r).append(' ').append(r).append(" 0 1 0 ").append(-2*r).append(" 0 Z ");}
 public void addRoundRect(float l,float t,float r,float b,float rx,float ry,Direction d){
  moveTo(l+rx,t);lineTo(r-rx,t);svg.append("Q ").append(r).append(' ').append(t).append(' ').append(r).append(' ').append(t+ry).append(' ');
  lineTo(r,b-ry);svg.append("Q ").append(r).append(' ').append(b).append(' ').append(r-rx).append(' ').append(b).append(' ');
  lineTo(l+rx,b);svg.append("Q ").append(l).append(' ').append(b).append(' ').append(l).append(' ').append(b-ry).append(' ');
  lineTo(l,t+ry);svg.append("Q ").append(l).append(' ').append(t).append(' ').append(l+rx).append(' ').append(t).append(' ');close();
 }
}
