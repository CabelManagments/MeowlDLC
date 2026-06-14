package com.yourcheat.gui;

import com.yourcheat.util.AccountManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@SuppressWarnings("All")
public class AltManager extends Screen {

    private final Screen parent;
    private boolean isTyping = false;
    private final StringBuilder inputText = new StringBuilder();
    private final List<String> accounts = AccountManager.INSTANCE.getAccounts();

    private float scrollOffset = 0, targetScrollOffset = 0;
    private float hoverAnimInput = 0;
    private float[] hoverAnim1, hoverAnim2;
    private int selectedIndex = -1;

    private float createHA=0, clearHA=0, randomHA=0;
    private float createS=1, clearS=1, randomS=1;

    private int shakeTime = 0;
    private float shakeOffsetY = 0;
    private boolean showConfirm = false;

    private static final float SCALE = 1.5f;
    private static final String TITLE = "AltManager";

    public AltManager(Screen parent) {
        super(Text.literal("AltManager"));
        this.parent = parent;
    }

    @Override
    protected void init() { super.init(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Плавный скролл
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.12f;

        // Анимированный фон
        long t = System.currentTimeMillis();
        float at = (t % 10000L) / 10000f;
        float s1=(float)Math.sin(at*Math.PI*2), s2=(float)Math.sin(at*Math.PI*2+Math.PI/2);
        float c1=(float)Math.cos(at*Math.PI*2), c2=(float)Math.cos(at*Math.PI*2+Math.PI/2);
        int c1r=clamp(45+(int)(s1*25)), c1g=clamp(12+(int)(c1*6)), c1b=clamp(15+(int)(s2*6));
        int c2r=clamp(50+(int)(c2*25)), c2g=clamp(14+(int)(s1*6)), c2b=clamp(18+(int)(c1*6));
        int c3r=clamp(40+(int)(c1*20)), c3g=clamp(10+(int)(s2*5)), c3b=clamp(12+(int)(s1*5));
        int c4r=clamp(55+(int)(s2*25)), c4g=clamp(15+(int)(c2*6)), c4b=clamp(20+(int)(c1*6));
        ctx.fillGradient(-1,-1,width/2+1,height/2+1, new Color(c1r,c1g,c1b,255).getRGB(), new Color(c2r,c2g,c2b,255).getRGB());
        ctx.fillGradient(width/2-1,-1,width+1,height/2+1, new Color(c2r,c2g,c2b,255).getRGB(), new Color(c3r,c3g,c3b,255).getRGB());
        ctx.fillGradient(-1,height/2-1,width/2+1,height+1, new Color(c4r,c4g,c4b,255).getRGB(), new Color(c1r,c1g,c1b,255).getRGB());
        ctx.fillGradient(width/2-1,height/2-1,width+1,height+1, new Color(c3r,c3g,c3b,255).getRGB(), new Color(c4r,c4g,c4b,255).getRGB());
        ctx.fill(-1,-1,width+2,height+2, new Color(0,0,0,195).getRGB());

        // Shake
        if (shakeTime>0) { shakeTime--; shakeOffsetY=(float)(Math.sin(shakeTime*0.5)*3); }
        else shakeOffsetY=0;

        var font = client.textRenderer;
        int titleW = font.getWidth(TITLE)*2;
        float titleX=(width-titleW)/2f, titleY=height/7f+shakeOffsetY;

        // Заголовок x2
        ctx.getMatrices().push();
        ctx.getMatrices().scale(2,2,1);
        ctx.drawText(font,TITLE,(int)(titleX/2)+1,(int)(titleY/2)+1,new Color(0,0,0,150).getRGB(),false);
        ctx.drawText(font,TITLE,(int)(titleX/2),(int)(titleY/2),new Color(160,40,50,255).getRGB(),false);
        ctx.getMatrices().pop();

        int cx=width/2, cy=height/2;
        int iW=(int)(220*SCALE), iH=(int)(17*SCALE);
        int iX=cx-(int)(110*SCALE), iY=cy-(int)(92*SCALE);

        // Input
        boolean hovI=isIn(mx,my,iX,iY,iW,iH);
        hoverAnimInput+=((hovI?1:0)-hoverAnimInput)*0.12f;
        int nameColor=lerpColor(new Color(160,160,160,255).getRGB(), new Color(220,220,220,255).getRGB(), hoverAnimInput);
        RenderUtil.drawRoundedRect(ctx,iX,iY,iW,iH,4,new Color(20,20,20,140).getRGB());
        RenderUtil.drawRoundedRect(ctx,iX,iY,iW,1,0,new Color(80,25,30,(int)(hoverAnimInput*120+40)).getRGB());
        if (!isTyping) {
            StringBuilder ph=new StringBuilder("Enter nickname");
            for (int i=0;i<(t/500%4);i++) ph.append(".");
            ctx.drawText(font,ph.toString(),iX+6,(int)(iY+iH/2f-4),nameColor,false);
        } else {
            String txt=inputText+((t/500%2)==0?"_":"");
            ctx.drawText(font,txt,iX+6,(int)(iY+iH/2f-4),nameColor,false);
        }

        // Список аккаунтов
        int lX=iX, lY=cy-(int)(70*SCALE), lW=(int)(220*SCALE), lH=(int)(140*SCALE);
        RenderUtil.drawRoundedRect(ctx,lX,lY,lW,lH,4,new Color(18,18,18,140).getRGB());

        ctx.enableScissor(lX,lY,lX+lW,lY+lH);
        if (hoverAnim1==null||hoverAnim1.length!=accounts.size()) hoverAnim1=new float[accounts.size()];
        if (hoverAnim2==null||hoverAnim2.length!=accounts.size()) hoverAnim2=new float[accounts.size()];

        float startY=lY+5, itemH=35*SCALE;
        for (int i=0;i<accounts.size();i++) {
            float y=startY-scrollOffset+i*itemH;
            int eX=cx-(int)(105*SCALE), eW=(int)(140*SCALE), eH=(int)(30*SCALE);

            int bgColor=(i==selectedIndex)?new Color(95,20,25,150).getRGB():new Color(20,20,20,90).getRGB();
            RenderUtil.drawRoundedRect(ctx,eX,(int)y,eW+10,eH,4,bgColor);

            ctx.drawText(font,accounts.get(i),eX+10,(int)(y+5),new Color(210,210,210,255).getRGB(),false);
            ctx.drawText(font,"Date "+LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    eX+10,(int)(y+18),new Color(130,130,130,255).getRGB(),false);

            int bW=(int)(60*SCALE), bH=(int)(13*SCALE);
            int sBX=eX+eW+(int)(10*SCALE), sBY=(int)y;
            boolean h1=isIn(mx,my,sBX,sBY,bW,bH);
            hoverAnim1[i]+=(( h1?1:0)-hoverAnim1[i])*0.12f;
            int sBg=lerpColor(new Color(20,20,20,140).getRGB(),new Color(45,15,18,180).getRGB(),hoverAnim1[i]);
            RenderUtil.drawRoundedRect(ctx,sBX,sBY,bW,bH+2,4,sBg);
            int stW=font.getWidth("Select");
            ctx.drawText(font,"Select",sBX+bW/2-stW/2,sBY+bH/2-4,0xFFFFFFFF,false);

            int dBX=sBX, dBY=sBY+bH+(int)(3*SCALE);
            boolean h2=isIn(mx,my,dBX,dBY,bW,bH);
            hoverAnim2[i]+=(( h2?1:0)-hoverAnim2[i])*0.12f;
            int dBg=lerpColor(new Color(20,20,20,140).getRGB(),new Color(45,15,18,180).getRGB(),hoverAnim2[i]);
            RenderUtil.drawRoundedRect(ctx,dBX,dBY,bW,bH+2,4,dBg);
            int dtW=font.getWidth("Delete");
            ctx.drawText(font,"Delete",dBX+bW/2-dtW/2,dBY+bH/2-4,0xFFFFFFFF,false);
        }
        ctx.disableScissor();

        // Кнопки снизу
        int bY=lY+lH+(int)(10*SCALE), bW=(int)(70*SCALE), bH=iH;
        int crX=cx-bW-(int)(40*SCALE), clX=cx-(bW/2), rX=cx+bW+(int)(-30*SCALE);
        float sp=0.04f;

        boolean hCr=isIn(mx,my,crX,bY,bW,bH);
        createHA=Math.max(0,Math.min(1,createHA+(hCr?sp:-sp)));
        createS=Math.max(1,Math.min(1.04f,createS+(hCr?sp*0.5f:-sp*0.5f)));
        drawBtn(ctx,crX,bY,bW,bH,"Create",createHA,createS,font);

        boolean hCl=isIn(mx,my,clX,bY,bW,bH);
        clearHA=Math.max(0,Math.min(1,clearHA+(hCl?sp:-sp)));
        clearS=Math.max(1,Math.min(1.04f,clearS+(hCl?sp*0.5f:-sp*0.5f)));
        drawBtn(ctx,clX,bY,bW,bH,"Clear all",clearHA,clearS,font);

        boolean hR=isIn(mx,my,rX,bY,bW,bH);
        randomHA=Math.max(0,Math.min(1,randomHA+(hR?sp:-sp)));
        randomS=Math.max(1,Math.min(1.04f,randomS+(hR?sp*0.5f:-sp*0.5f)));
        drawBtn(ctx,rX,bY,bW,bH,"Random",randomHA,randomS,font);

        // Инфо
        String selTxt="Selected: "+client.getSession().getUsername();
        ctx.drawText(font,selTxt,cx-font.getWidth(selTxt)/2,bY+bH+(int)(20*SCALE),0xFFCCCCCC,false);
        String qty="Accounts: "+accounts.size();
        ctx.drawText(font,qty,cx-font.getWidth(qty)/2,bY+bH+(int)(34*SCALE),0xFF999999,false);

        if (showConfirm) drawConfirm(ctx);
        super.render(ctx,mx,my,delta);
    }

    private void drawBtn(DrawContext ctx, int x, int y, int w, int h, String name,
                         float ha, float sc, net.minecraft.client.font.TextRenderer font) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x+w/2f,y+h/2f,0);
        ctx.getMatrices().scale(sc,sc,1);
        ctx.getMatrices().translate(-(x+w/2f),-(y+h/2f),0);
        RenderUtil.drawRoundedRect(ctx,x,y,w,h,4,lerpColor(new Color(20,20,20,140).getRGB(),new Color(45,15,18,180).getRGB(),ha));
        ctx.drawText(font,name,x+w/2-font.getWidth(name)/2,y+h/2-4,0xFFEEEEEE,false);
        ctx.getMatrices().pop();
    }

    private void drawConfirm(DrawContext ctx) {
        int bW=300,bH=130,bX=(width-bW)/2,bY=(height-bH)/2;
        ctx.fill(0,0,width,height,new Color(0,0,0,140).getRGB());
        RenderUtil.drawRoundedRect(ctx,bX,bY,bW,bH,6,new Color(30,30,30,245).getRGB());
        var font=client.textRenderer;
        String q="Clear all accounts?";
        ctx.drawText(font,q,bX+bW/2-font.getWidth(q)/2,bY+30,0xFFFFFFFF,false);
        int bBW=90,bBH=28,yesX=bX+35,noX=bX+bW-35-bBW,bBY=bY+bH-50;
        RenderUtil.drawRoundedRect(ctx,yesX,bBY,bBW,bBH,5,new Color(45,115,55).getRGB());
        ctx.drawText(font,"Yes",yesX+bBW/2-font.getWidth("Yes")/2,bBY+bBH/2-4,0xFFFFFFFF,false);
        RenderUtil.drawRoundedRect(ctx,noX,bBY,bBW,bBH,5,new Color(135,35,40).getRGB());
        ctx.drawText(font,"No",noX+bBW/2-font.getWidth("No")/2,bBY+bBH/2-4,0xFFFFFFFF,false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int cx=width/2, cy=height/2;
        int iW=(int)(220*SCALE),iH=(int)(17*SCALE),iX=cx-(int)(110*SCALE),iY=cy-(int)(92*SCALE);
        int bW=(int)(70*SCALE),bY=cy-(int)(70*SCALE)+(int)(140*SCALE)+(int)(10*SCALE);
        int crX=cx-bW-(int)(40*SCALE),clX=cx-(bW/2),rX=cx+bW+(int)(-30*SCALE);

        if (showConfirm) {
            int bBW=300,bBH=130,bBX=(width-bBW)/2,bBY=(height-bBH)/2;
            int btnW=90,btnH=28,yesX=bBX+35,noX=bBX+bBW-35-btnW,btnBY=bBY+bBH-50;
            if (isIn(mx,my,yesX,btnBY,btnW,btnH)) { accounts.clear(); AccountManager.INSTANCE.clearAll(); selectedIndex=-1; showConfirm=false; return true; }
            if (isIn(mx,my,noX,btnBY,btnW,btnH)) { showConfirm=false; return true; }
            return true;
        }

        // Title click → shake
        int tW=client.textRenderer.getWidth(TITLE)*2;
        float tX=(width-tW)/2f,tY=height/7f;
        if (mx>=tX&&mx<=tX+tW&&my>=tY&&my<=tY+30) { shakeTime=20; return true; }

        if (isIn(mx,my,iX,iY,iW,iH)&&!isTyping&&btn==0) { isTyping=true; return true; }
        if (!isIn(mx,my,iX,iY,iW,iH)&&!isIn(mx,my,crX,bY,bW,iH)&&!isIn(mx,my,clX,bY,bW,iH)&&!isIn(mx,my,rX,bY,bW,iH)&&isTyping&&btn==0) { isTyping=false; }

        // Create
        if (isIn(mx,my,crX,bY,bW,iH)&&isTyping&&btn==0) {
            String n=inputText.toString().trim();
            if (!n.isEmpty()&&accounts.stream().noneMatch(a->a.equalsIgnoreCase(n))) {
                isTyping=false; AccountManager.INSTANCE.addAccount(n); inputText.setLength(0); } return true; }

        // Clear
        if (isIn(mx,my,clX,bY,bW,iH)&&btn==0) { showConfirm=true; return true; }

        // Random
        if (isIn(mx,my,rX,bY,bW,iH)&&btn==0) {
            String rName=generateRandom();
            if (!accounts.contains(rName)) AccountManager.INSTANCE.addAccount(rName);
            AccountManager.INSTANCE.loginAccount(rName);
            selectedIndex=accounts.indexOf(rName);
            return true;
        }

        // List clicks
        int lX=iX,lY=cy-(int)(70*SCALE),lW=(int)(220*SCALE),lH=(int)(140*SCALE);
        if (isIn(mx,my,lX,lY,lW,lH)) {
            float startY=lY+5,itemH=35*SCALE;
            int eX=cx-(int)(105*SCALE),eW=(int)(140*SCALE),eH=(int)(30*SCALE);
            int bBW=(int)(60*SCALE),bBH=(int)(13*SCALE);
            for (int i=0;i<accounts.size();i++) {
                float y=startY-scrollOffset+i*itemH;
                if (isIn(mx,my,eX,(int)y,eW+10,eH)&&btn==0) { AccountManager.INSTANCE.loginAccount(accounts.get(i)); selectedIndex=i; return true; }
                int sBX=eX+eW+(int)(10*SCALE),sBY=(int)y;
                if (isIn(mx,my,sBX,sBY,bBW,bBH)&&btn==0) { AccountManager.INSTANCE.loginAccount(accounts.get(i)); selectedIndex=i; return true; }
                int dBX=sBX,dBY=sBY+bBH+(int)(3*SCALE);
                if (isIn(mx,my,dBX,dBY,bBW,bBH)&&btn==0) {
                    if (selectedIndex==i) selectedIndex=-1;
                    AccountManager.INSTANCE.removeAccount(accounts.get(i));
                    accounts.remove(i); return true;
                }
            }
        }
        return super.mouseClicked(mx,my,btn);
    }

    @Override
    public boolean mouseScrolled(double mx,double my,double sx,double sy) {
        int cy=height/2,lY=cy-(int)(70*SCALE),lH=(int)(140*SCALE);
        if (my>=lY&&my<=lY+lH) {
            targetScrollOffset-=sy*(int)(30*SCALE);
            int max=Math.max(0,(accounts.size()*(int)(36*SCALE))-lH);
            targetScrollOffset=Math.max(0,Math.min(targetScrollOffset,max));
            return true;
        }
        return super.mouseScrolled(mx,my,sx,sy);
    }

    @Override
    public boolean keyPressed(int key,int scan,int mods) {
        if (isTyping) {
            boolean ctrl=GLFW.glfwGetKey(client.getWindow().getHandle(),GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS;
            if (ctrl&&key==GLFW.GLFW_KEY_V) {
                String clip=GLFW.glfwGetClipboardString(client.getWindow().getHandle());
                if (clip!=null) { String f=clip.replaceAll("[^\\w]",""); int max=16-inputText.length(); if (max>0) inputText.append(f, 0, Math.min(f.length(), max)); }
                return true;
            }
            if (key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER) {
                String n=inputText.toString().trim();
                if (!n.isEmpty()&&accounts.stream().noneMatch(a->a.equalsIgnoreCase(n))) { isTyping=false; AccountManager.INSTANCE.addAccount(n); inputText.setLength(0); }
                return true;
            }
            if (key==GLFW.GLFW_KEY_BACKSPACE&&inputText.length()>0) { inputText.deleteCharAt(inputText.length()-1); return true; }
        }
        return super.keyPressed(key,scan,mods);
    }

    @Override
    public boolean charTyped(char c,int mods) {
        if (isTyping&&inputText.length()<16&&(Character.isLetterOrDigit(c)||c=='_')) { inputText.append(c); return true; }
        return false;
    }

    @Override public void close() { client.setScreen(parent); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // Helpers
    private static boolean isIn(double mx,double my,double x,double y,double w,double h) { return mx>=x&&mx<=x+w&&my>=y&&my<=y+h; }
    private static int clamp(int v) { return Math.max(0,Math.min(255,v)); }
    private static int lerpColor(int c1,int c2,float t) {
        int a1=(c1>>24)&0xFF,r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
        int a2=(c2>>24)&0xFF,r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        return(((int)(a1+(a2-a1)*t))<<24)|(((int)(r1+(r2-r1)*t))<<16)|(((int)(g1+(g2-g1)*t))<<8)|((int)(b1+(b2-b1)*t));
    }

    private String generateRandom() {
        Random r=new Random();
        String[] words={"Alex","Iq","Termo","Silent","Cat","Lone","Pro","Meow","Gator","Ninja",
                "Shadow","Fire","Ice","Dragon","Wolf","Eagle","Storm","Blade","Ghost","Pixel",
                "Neo","Cyber","Volt","Echo","Falcon","Hawk","Knight","Nova","Phantom","Titan"};
        int n=r.nextInt(2)+1; StringBuilder sb=new StringBuilder();
        for (int i=0;i<n;i++) {
            String w=words[r.nextInt(words.length)];
            if (r.nextBoolean()) w=w.toLowerCase();
            if (Math.random()<0.3&&w.length()>2) { int p=r.nextInt(w.length()-2)+1; w=w.substring(0,p)+r.nextInt(10)+w.substring(p+1); }
            sb.append(w);
            if (i<n-1&&r.nextInt(3)==0) sb.append("_");
        }
        if (Math.random()<0.7) for (int i=0;i<r.nextInt(4)+1;i++) sb.append(r.nextInt(10));
        String res=sb.toString(); if (res.length()>16) res=res.substring(0,16);
        return res.isEmpty()?"Player"+r.nextInt(1000):res;
    }
}

