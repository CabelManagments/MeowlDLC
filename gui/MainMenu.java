package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("All")
public class MainMenu extends Screen {

    private MenuButton singleplayerButton;
    private MenuButton multiplayerButton;
    private MenuButton altmanagerButton;
    private CombinedButton optionsQuitButton;

    private final String title   = "MeowlDLC";
    private final String version = "v1.0 BETA";

    private int shakeTime = 0;
    private float fadeIn  = 0f;

    private List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public MainMenu() {
        super(Text.literal("MeowlDLC"));
    }

    @Override
    protected void init() {
        fadeIn = 0f;
        particles.clear();
        for (int i = 0; i < 50; i++) particles.add(new Particle());

        int bw = 200, bh = 30;
        int cx = width / 2 - bw / 2;
        int startY = height / 2 - 10;
        int gap = 38;

        singleplayerButton = new MenuButton("Singleplayer", cx, startY,        bw, bh);
        multiplayerButton  = new MenuButton("Multiplayer",  cx, startY + gap,  bw, bh);
        altmanagerButton   = new MenuButton("AltManager",   cx, startY + gap*2,bw, bh);
        optionsQuitButton  = new CombinedButton(cx, startY + gap*3, bw, bh, "Options", "Quit");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (fadeIn < 1f) fadeIn = Math.min(1f, fadeIn + 0.02f);

        // Анимированный фон — марсаловый градиент
        long t = System.currentTimeMillis();
        float at = (t % 10000L) / 10000f;
        float s1 = (float) Math.sin(at * Math.PI * 2);
        float s2 = (float) Math.sin(at * Math.PI * 2 + Math.PI / 2);
        float c1 = (float) Math.cos(at * Math.PI * 2);
        float c2 = (float) Math.cos(at * Math.PI * 2 + Math.PI / 2);

        int r1 = clamp(45 + (int)(s1*25)), g1 = clamp(12 + (int)(c1*6)), b1 = clamp(15 + (int)(s2*6));
        int r2 = clamp(50 + (int)(c2*25)), g2 = clamp(14 + (int)(s1*6)), b2 = clamp(18 + (int)(c1*6));
        int r3 = clamp(40 + (int)(c1*20)), g3 = clamp(10 + (int)(s2*5)), b3 = clamp(12 + (int)(s1*5));
        int r4 = clamp(55 + (int)(s2*25)), g4 = clamp(15 + (int)(c2*6)), b4 = clamp(20 + (int)(c1*6));

        int col1 = new Color(r1,g1,b1,255).getRGB();
        int col2 = new Color(r2,g2,b2,255).getRGB();
        int col3 = new Color(r3,g3,b3,255).getRGB();
        int col4 = new Color(r4,g4,b4,255).getRGB();

        // Градиентный фон (4 угла)
        ctx.fillGradient(-1, -1, width/2+1, height/2+1, col1, col2);
        ctx.fillGradient(width/2-1, -1, width+1, height/2+1, col2, col3);
        ctx.fillGradient(-1, height/2-1, width/2+1, height+1, col4, col1);
        ctx.fillGradient(width/2-1, height/2-1, width+1, height+1, col3, col4);

        // Тёмный оверлей
        ctx.fill(-1, -1, width+2, height+2, new Color(0, 0, 0, 195).getRGB());

        // Частицы
        for (Particle p : particles) { p.update(); p.render(ctx); }

        // Shake эффект на заголовок
        float shakeOffsetY = 0;
        if (shakeTime > 0) {
            shakeOffsetY = (float)(Math.sin(shakeTime * 1.2) * 3 * (shakeTime / 20f));
            shakeTime--;
        }

        // Заголовок
        var font = client.textRenderer;
        int titleW = font.getWidth(title) * 3;
        float titleX = (width - titleW) / 2f;
        float titleY = height / 5f + shakeOffsetY;

        // Тень
        ctx.getMatrices().push();
        ctx.getMatrices().scale(3, 3, 1);
        ctx.drawText(font, title, (int)(titleX/3)+1, (int)(titleY/3)+1,
                new Color(0,0,0,150).getRGB(), false);
        // Градиентный заголовок (марсаловый → светло-красный)
        int lightRed1 = animatedColor(t, 0);
        int lightRed2 = animatedColor(t, 120);
        ctx.drawText(font, title, (int)(titleX/3), (int)(titleY/3), lightRed1, false);
        ctx.getMatrices().pop();

        // Версия
        String versionText = version;
        int vw = font.getWidth(versionText);
        float vx = width - vw - 10, vy = height - 20;
        ctx.drawText(font, versionText, (int)vx+1, (int)vy+1, new Color(0,0,0,150).getRGB(), false);
        ctx.drawText(font, versionText, (int)vx, (int)vy, new Color(140,140,140,180).getRGB(), false);

        // Время
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        int tw = font.getWidth(timeStr);
        ctx.drawText(font, timeStr, (int)((width - tw) / 2f), (int)(titleY + font.fontHeight*3 + 10),
                new Color(180,180,180,180).getRGB(), false);

        // Кнопки с fadeIn анимацией
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - fadeIn) * 20, 0);
        singleplayerButton.render(ctx, mx, my, fadeIn);
        multiplayerButton.render(ctx, mx, my, fadeIn);
        altmanagerButton.render(ctx, mx, my, fadeIn);
        optionsQuitButton.render(ctx, mx, my, fadeIn);
        ctx.getMatrices().pop();

        super.render(ctx, mx, my, delta);
    }

    private int animatedColor(long t, int hueOffset) {
        float hue = ((t % 5000L) / 5000f + hueOffset / 360f) % 1f;
        // Ограничиваем в красном диапазоне
        hue = 0.92f + (float)(Math.sin(t / 2000.0) * 0.04f);
        return Color.getHSBColor(hue, 0.7f, 0.9f).getRGB();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int titleW = client.textRenderer.getWidth(title) * 3;
        float titleX = (width - titleW) / 2f;
        float titleY = height / 5f;
        if (mx >= titleX && mx <= titleX + titleW && my >= titleY && my <= titleY + 54) {
            shakeTime = 20; return true;
        }
        if (singleplayerButton.isHovered(mx, my)) { client.setScreen(new SelectWorldScreen(this)); return true; }
        if (multiplayerButton.isHovered(mx, my))  { client.setScreen(new MultiplayerScreen(this)); return true; }
        if (altmanagerButton.isHovered(mx, my))   { client.setScreen(new AltManager(this)); return true; }
        if (optionsQuitButton.isOptionHovered(mx, my)) { client.setScreen(new OptionsScreen(this, client.options)); return true; }
        if (optionsQuitButton.isQuitHovered(mx, my))   { client.scheduleStop(); return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static int blendColors(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF, r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int a2=(c2>>24)&0xFF, r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return (((int)(a1+(a2-a1)*t))<<24)|(((int)(r1+(r2-r1)*t))<<16)|
               (((int)(g1+(g2-g1)*t))<<8)|((int)(b1+(b2-b1)*t));
    }

    // ── Частицы ───────────────────────────────────────────────────
    private class Particle {
        float x, y, vx, vy, size; int alpha;
        Particle() { reset(); y = random.nextFloat() * height; }
        void reset() {
            x = random.nextFloat() * width; y = -10;
            vx = (random.nextFloat()-0.5f)*0.5f; vy = random.nextFloat()*0.5f+0.3f;
            size = random.nextFloat()*2+1; alpha = random.nextInt(100)+50;
        }
        void update() { x+=vx; y+=vy; if (y>height+10||x<-10||x>width+10) reset(); }
        void render(DrawContext ctx) {
            RenderUtil.drawRoundedRect(ctx, x, y, size, size, size/2,
                    new Color(110, 25, 30, alpha).getRGB());
        }
    }

    // ── Кнопка ────────────────────────────────────────────────────
    private class MenuButton {
        final String name; int x, y, width, height;
        float hoverAnim=0, scale=1, glowAnim=0;
        MenuButton(String name, int x, int y, int width, int height) {
            this.name=name; this.x=x; this.y=y; this.width=width; this.height=height;
        }
        void render(DrawContext ctx, int mx, int my, float fa) {
            boolean hov = isHovered(mx, my);
            float sp = 0.04f;
            if (hov) { hoverAnim=Math.min(1,hoverAnim+sp); scale=Math.min(1.03f,scale+sp*0.75f); glowAnim=Math.min(1,glowAnim+sp*2); }
            else     { hoverAnim=Math.max(0,hoverAnim-sp); scale=Math.max(1,scale-sp*0.75f);     glowAnim=Math.max(0,glowAnim-sp*2); }

            ctx.getMatrices().push();
            ctx.getMatrices().translate(x+width/2f, y+height/2f, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.getMatrices().translate(-(x+width/2f), -(y+height/2f), 0);

            if (glowAnim>0) RenderUtil.drawRoundedRect(ctx, x-3, y-3, width+6, height+6, 10, new Color(115,22,28,(int)(glowAnim*80*fa)).getRGB());
            RenderUtil.drawRoundedRect(ctx, x+2, y+2, width, height, 7, new Color(0,0,0,(int)(120*fa)).getRGB());
            int bg = blendColors(new Color(20,20,20,(int)(140*fa)).getRGB(), new Color(45,12,15,(int)(190*fa)).getRGB(), hoverAnim);
            RenderUtil.drawRoundedRect(ctx, x, y, width, height, 7, bg);
            if (hoverAnim>0) RenderUtil.drawRoundedRect(ctx, x, y, width, height, 7, new Color(115,22,28,(int)(hoverAnim*150*fa)).getRGB());

            ctx.drawText(client.textRenderer, name, x + width/2 - client.textRenderer.getWidth(name)/2, y+(height-9)/2, new Color(235,235,235,(int)(255*fa)).getRGB(), false);
            ctx.getMatrices().pop();
        }
        boolean isHovered(double mx, double my) { return mx>=x&&mx<=x+width&&my>=y&&my<=y+height; }
    }

    // ── Двойная кнопка (Options | Quit) ───────────────────────────
    private class CombinedButton {
        int x, y, width, height; final String leftName, rightName;
        float lHA=0, rHA=0, lS=1, rS=1, lGA=0, rGA=0;
        CombinedButton(int x, int y, int width, int height, String l, String r) {
            this.x=x; this.y=y; this.width=width; this.height=height; leftName=l; rightName=r;
        }
        void render(DrawContext ctx, int mx, int my, float fa) {
            float sp=0.04f;
            boolean lH=isOptionHovered(mx,my), rH=isQuitHovered(mx,my);
            if(lH){lHA=Math.min(1,lHA+sp);lS=Math.min(1.03f,lS+sp*0.75f);lGA=Math.min(1,lGA+sp*2);}
            else  {lHA=Math.max(0,lHA-sp);lS=Math.max(1,lS-sp*0.75f);     lGA=Math.max(0,lGA-sp*2);}
            if(rH){rHA=Math.min(1,rHA+sp);rS=Math.min(1.03f,rS+sp*0.75f);rGA=Math.min(1,rGA+sp*2);}
            else  {rHA=Math.max(0,rHA-sp);rS=Math.max(1,rS-sp*0.75f);     rGA=Math.max(0,rGA-sp*2);}

            int bw=width/2-4, lx=x+2, rx=x+width/2+2;
            int base=new Color(20,20,20,(int)(140*fa)).getRGB(), hov=new Color(45,12,15,(int)(190*fa)).getRGB();
            var font=client.textRenderer;
            int tc=new Color(235,235,235,(int)(255*fa)).getRGB();

            // Left
            ctx.getMatrices().push();
            ctx.getMatrices().translate(lx+bw/2f,y+height/2f,0); ctx.getMatrices().scale(lS,lS,1); ctx.getMatrices().translate(-(lx+bw/2f),-(y+height/2f),0);
            if(lGA>0) RenderUtil.drawRoundedRect(ctx,lx-3,y-3,bw+6,height+6,10,new Color(115,22,28,(int)(lGA*80*fa)).getRGB());
            RenderUtil.drawRoundedRect(ctx,lx+2,y+2,bw,height,7,new Color(0,0,0,(int)(120*fa)).getRGB());
            RenderUtil.drawRoundedRect(ctx,lx,y,bw,height,7,blendColors(base,hov,lHA));
            if(lHA>0) RenderUtil.drawRoundedRect(ctx,lx,y,bw,height,7,new Color(115,22,28,(int)(lHA*150*fa)).getRGB());
            ctx.drawText(font,leftName,lx+bw/2-font.getWidth(leftName)/2,y+(height-9)/2,tc,false);
            ctx.getMatrices().pop();

            // Right
            ctx.getMatrices().push();
            ctx.getMatrices().translate(rx+bw/2f,y+height/2f,0); ctx.getMatrices().scale(rS,rS,1); ctx.getMatrices().translate(-(rx+bw/2f),-(y+height/2f),0);
            if(rGA>0) RenderUtil.drawRoundedRect(ctx,rx-3,y-3,bw+6,height+6,10,new Color(115,22,28,(int)(rGA*80*fa)).getRGB());
            RenderUtil.drawRoundedRect(ctx,rx+2,y+2,bw,height,7,new Color(0,0,0,(int)(120*fa)).getRGB());
            RenderUtil.drawRoundedRect(ctx,rx,y,bw,height,7,blendColors(base,hov,rHA));
            if(rHA>0) RenderUtil.drawRoundedRect(ctx,rx,y,bw,height,7,new Color(115,22,28,(int)(rHA*150*fa)).getRGB());
            ctx.drawText(font,rightName,rx+bw/2-font.getWidth(rightName)/2,y+(height-9)/2,tc,false);
            ctx.getMatrices().pop();
        }
        boolean isOptionHovered(double mx,double my){return mx>=x&&mx<x+width/2-1&&my>=y&&my<=y+height;}
        boolean isQuitHovered(double mx,double my) {return mx>x+width/2+1&&mx<=x+width&&my>=y&&my<=y+height;}
    }
}

