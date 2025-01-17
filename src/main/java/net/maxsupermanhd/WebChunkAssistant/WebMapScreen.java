package net.maxsupermanhd.WebChunkAssistant;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.maxsupermanhd.WebChunkAssistant.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class WebMapScreen extends Screen {
    public static final Logger LOGGER = LogManager.getLogger("WebMapScreen");
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Identifier TEX_INVALID = new Identifier("webchunkassistant", "textures/gui/invalid.png");
    private final Identifier TEX_LOADING0 = new Identifier("webchunkassistant", "textures/gui/logo-0.png");
    private final Identifier TEX_LOADING1 = new Identifier("webchunkassistant", "textures/gui/logo-1.png");
    private final Identifier TEX_LOADING2 = new Identifier("webchunkassistant", "textures/gui/logo-2.png");
    private final Identifier TEX_LOADING3 = new Identifier("webchunkassistant", "textures/gui/logo-3.png");
    private final Identifier TEX_LOADING4 = new Identifier("webchunkassistant", "textures/gui/logo-4.png");
    private final Text TrNoCache = Text.translatable("WebChunkAssistant.gui.NoCache");
    private final Text TrCache = Text.translatable("WebChunkAssistant.gui.Cache");
    private final Text TrSelectMap = Text.translatable("WebChunkAssistant.gui.SelectMap");
    private final Text TrSelectMapScreen = Text.translatable("WebChunkAssistant.gui.SelectMapScreen");
    private final Text TrSelectLayer = Text.translatable("WebChunkAssistant.gui.SelectLayer");
    private final Text TrSelectLayerScreen = Text.translatable("WebChunkAssistant.gui.SelectLayerScreen");
    private final ConcurrentHashMap<MapTilePos, WebTexture> loadedTextures = new ConcurrentHashMap<>(16);
    public long mapx, mapz;
    public int mapzoom = 4;
    public double mapoffsetx = 0.5, mapoffsety = 0.5;
    public int maptilesize = 256;
    public int zoombreaklow = 192;
    public int zoombreakhigh = 320;
    public int zoomsensitivity = 32;
    public int propagateChangesTo = 8;
    public int allowedTileUpdatesPerTick = 16;
    public String worldName = "";
    public String dimensionName = "";
    public String format = "terrain";
    public String[] overlays = {};
    public boolean disableCache = false;
    public static String cachedUserAgent = "WebChunk Assistant";
    public boolean reinitWidgets = false;
//    private final ExecutorService executorService = Executors.newFixedThreadPool(12);
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, 4, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>());
    private final ConcurrentLinkedQueue updateTilesQueue = new ConcurrentLinkedQueue<>(); // to ensure thread safety of incoming tiles
    protected WebMapScreen(Text title) {
        super(title);
        for (ModContainer m : FabricLoaderImpl.INSTANCE.getAllMods()) {
            if(m.getMetadata().getId().equalsIgnoreCase("webchunkassistant")) {
                cachedUserAgent = "WebChunk Assistant " + m.getMetadata().getVersion().getFriendlyString();
                break;
            }
        }
    }

    @Override
    public void tick() {
        if(reinitWidgets) {
            initWidgets();
            reinitWidgets = false;
        }
        processTileUpdates();
    }
    @Override
    public void init() {
        zoombreaklow = Config.Map.ZOOM_BREAK_LOW.getIntegerValue();
        zoombreakhigh = Config.Map.ZOOM_BREAK_HIGH.getIntegerValue();
        executorService.setMaximumPoolSize(Config.Map.REQUEST_POOL_SIZE.getIntegerValue());
        initWidgets();
    }

    public void initWidgets() {
        this.clearChildren();
        int selmapw = textRenderer.getWidth(TrSelectMap.asOrderedText());
        this.addDrawableChild(new ButtonWidget(
                this.width - (selmapw + 4), 2,
                selmapw + 4, 10,
                TrSelectMap, (button) -> {
                    assert this.client != null;
                    this.client.setScreen(new WorldAndDimSelectorScreen(this, TrSelectMapScreen));
                }
        ));
        int selrend = textRenderer.getWidth(TrSelectLayer.asOrderedText());
        this.addDrawableChild(new ButtonWidget(
                this.width - (selrend + 4), 14,
                selrend + 4, 10,
                TrSelectLayer, (button) -> {
                    assert this.client != null;
                    this.client.setScreen(new RendererSelectorScreen(this, TrSelectLayerScreen));
                }
        ));
        Text cl = this.getCacheLabel();
        int clwidth = textRenderer.getWidth(cl);
        this.addDrawableChild(new PressableTextWidget(
                this.width - clwidth - 2, this.height - textRenderer.fontHeight - 2,
                clwidth, textRenderer.fontHeight, cl,
                b -> {this.disableCache = !this.disableCache;this.reinitWidgets = true;}, this.textRenderer));
    }

    public Text getCacheLabel() {
        if(this.disableCache) {
            return TrNoCache;
        }
        return TrCache;
    }
    private void processTileUpdates() {
        int updated = 0;
        while(!updateTilesQueue.isEmpty() && updated < allowedTileUpdatesPerTick) {
            updateTilesQueue.remove();
            updated++;
        }
    }
    public void updateTile(int x, int z, NativeImage with) {
//        ConcurrentHashMap.KeySetView<MapTilePos, Boolean> ks = ConcurrentHashMap.newKeySet();
//        MapTilePos t = new MapTilePos();
//        for (int s = 1; s < propagateChangesTo; s++) {
//            t.cx = what.cx/(2<<s);
//            t.cz = what.cz/(2<<s);
//            t.zoom = s;
//            WebTexture tex = loadedTextures.get(t);
//            if(tex != nil) {
//
//            }
//            tex.img
//        }
//        img.close();
    }

    public boolean shouldKeepTileLoaded(int x, int z, int s, String world, String dim, String type) {
        if(Arrays.stream(overlays).noneMatch(s1 -> Objects.equals(s1, type))) {
            return false;
        }
        if(!Objects.equals(world, worldName) || !Objects.equals(dim, dimensionName)) {
            return false;
        }
        // TODO: check how far it is
        return true;
    }

    public void drawBox(MatrixStack matrices, int x0, int y0, int x1, int y1) {
        this.drawHorizontalLine(matrices, x0, x1, y0, 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, x0, x1, y1, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, x0, y0, y1, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, x1, y0, y1, 0xFFFFFFFF);
    }

    public void renderTile(MatrixStack matrices, MapTilePos pos, int offx, int offy) {
        int ox = (int) (this.width/2 - maptilesize*mapoffsetx) + offx;
        int oy = (int) (this.height/2 - maptilesize*mapoffsety) + offy;
        WebTexture tex = loadedTextures.computeIfAbsent(pos, (MapTilePos p) -> {
            WebTexture t;
            List<String> h = new ArrayList<>();
            h.add("User-Agent");
            h.add(cachedUserAgent);
            if(this.disableCache) {
                h.add("Cache-Control");
                h.add("no-cache");
            }
            try {
                t = new WebTexture(getURIfromPos(pos), pos.toIdentifier(), h.toArray(new String[0]));
            } catch (Exception e) {
                LOGGER.info(e);
                return null;
            }
            executorService.submit(t);
            return t;
        });
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        boolean drawTex = true;
        if(tex == null) {
            RenderSystem.setShaderTexture(0, TEX_INVALID);
        } else if(Objects.equals(tex.status, "init")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING0);
        } else if(Objects.equals(tex.status, "allocating")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING1);
        } else if(Objects.equals(tex.status, "requesting")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING2);
        } else if(Objects.equals(tex.status, "parsing")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING3);
        } else if(Objects.equals(tex.status, "registering")) {
            RenderSystem.setShaderTexture(0, TEX_LOADING4);
            mc.getTextureManager().registerTexture(tex.id, new NativeImageBackedTexture(tex.img));
            tex.status = "done";
        } else if(Objects.equals(tex.status, "done")) {
            RenderSystem.setShaderTexture(0, tex.id);
        } else {
            drawTex = false;
        }
        if(drawTex) {
            drawTexture(matrices, ox, oy, 0.0f, 0.0f, maptilesize, maptilesize, maptilesize, maptilesize);
        }
        RenderSystem.disableBlend();
        drawBox(matrices, ox, oy, (ox+maptilesize), (oy+maptilesize));
    }

    public URL getURIfromPos(MapTilePos pos) throws MalformedURLException {
        return new URL(String.format(Config.Server.BASE_URL.getValue() + Config.Server.ENDPOINT_MAP_DATA.getValue(), pos.world, pos.dimension, pos.format, pos.zoom, pos.cx, pos.cz));
    }

    public void setWorldName(String w) {
        this.worldName = w;
    }
    public void setDimensionName(String d) {
        this.dimensionName = d;
    }
    public void setFormat(String f) {
        this.format = f;
    }
    public void setOverlays(String[] o) {
        this.overlays = o;
    }

    public void renderError(MatrixStack matrices, String errtext) {
        renderCenteredText(matrices, errtext, 0x00FF6666);
    }

    public void renderCenteredText(MatrixStack matrices, String errtext, int color) {
        int xpos = width/2 - this.textRenderer.getWidth(errtext)/2;
        this.textRenderer.drawWithShadow(matrices, errtext, xpos, 40, color);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Screen.fill(matrices, 0, 0, this.width, this.height, 0xFF000000);
        int fitx = width / maptilesize + 3;
        int fitz = height / maptilesize + 3;
        if(worldName.isEmpty() || dimensionName.isEmpty()) {
            renderCenteredText(matrices, "World/dimension not selected", 0x00FFFFFF);
            super.render(matrices, mouseX, mouseY, delta);
            return;
        }
        for(int offz = -(fitz/2+1); offz < fitz/2+1; offz++) {
            for(int offx = -(fitx/2+1); offx < fitx/2+1; offx++) {
                renderTile(matrices, new MapTilePos(worldName, dimensionName, format, mapx+offx, mapz+offz, mapzoom), offx*maptilesize, offz*maptilesize);
            }
        }
        if(overlays != null) {
            for (String o : overlays) {
                for(int offz = -(fitz/2+1); offz < fitz/2+1; offz++) {
                    for(int offx = -(fitx/2+1); offx < fitx/2+1; offx++) {
                        renderTile(matrices, new MapTilePos(worldName, dimensionName, o, mapx+offx, mapz+offz, mapzoom), offx*maptilesize, offz*maptilesize);
                    }
                }
            }
        }
        String worlddim = worldName + " " + dimensionName;
        this.textRenderer.drawWithShadow(matrices, worlddim, width - textRenderer.getWidth(worlddim)-textRenderer.getWidth("Select map")-8, 2, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, format, width - textRenderer.getWidth(format)-textRenderer.getWidth("Select renderer")-8, 14, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Map position: %d:%d (x%d z%d)", mapx, mapz, tileToCoord(mapzoom, mapx+mapoffsetx), tileToCoord(mapzoom, mapz+mapoffsety)), 10, 10, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Map offset: %3.3f %3.3f", mapoffsetx, mapoffsety), 10, 20, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Map zoom: %d %d", mapzoom, maptilesize), 10, 30, 0x00FFFFFF);
        this.textRenderer.drawWithShadow(matrices, String.format("Tiles loaded: %d", loadedTextures.size()), 10, 40, 0x00FFFFFF);
        this.drawVerticalLine(matrices, width/2-1, height/2-3, height/2-10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2  , height/2-3, height/2-10, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, width/2+1, height/2-3, height/2-10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2-1, height/2+3, height/2+10, 0xFF000000);
        this.drawVerticalLine(matrices, width/2  , height/2+3, height/2+10, 0xFFFFFFFF);
        this.drawVerticalLine(matrices, width/2+1, height/2+3, height/2+10, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2+1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2  , 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, width/2-3, width/2-10, height/2-1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2+1, 0xFF000000);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2  , 0xFFFFFFFF);
        this.drawHorizontalLine(matrices, width/2+3, width/2+10, height/2-1, 0xFF000000);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public void normalizeOffset() {
        if (mapoffsetx > 1) {
            mapx += 1;
            mapoffsetx -= 1;
        } else if (mapoffsetx < 0) {
            mapx -= 1;
            mapoffsetx += 1;
        }
        if (mapoffsety > 1) {
            mapz += 1;
            mapoffsety -= 1;
        } else if (mapoffsety < 0) {
            mapz -= 1;
            mapoffsety += 1;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double dx = deltaX / maptilesize;
        double dy = deltaY / maptilesize;
        mapoffsetx -= dx;
        mapoffsety -= dy;
        normalizeOffset();
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public double coordToTile(int zoom, long coord) {
        return coord/(Math.pow(2, zoom)*16);
    }

    public long tileToCoord(int zoom, double tile) {
        return (long) (Math.pow(2, zoom)*16*tile);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if(amount > 0) {
            maptilesize += zoomsensitivity;
        } else if(amount < 0) {
            maptilesize -= zoomsensitivity;
        }
        int zoomchunks = (int) Math.pow(2, mapzoom);
        long keepx = tileToCoord(mapzoom, mapx+mapoffsetx);
        long keepz = tileToCoord(mapzoom, mapz+mapoffsety);
        if(maptilesize-1 < zoombreaklow) {
            maptilesize += zoombreakhigh - zoombreaklow;
            mapzoom += 1;
        } else if (maptilesize+1 > zoombreakhigh) {
            if(mapzoom == 0) {
                return false;
            }
            maptilesize -= zoombreakhigh - zoombreaklow;
//            mapx = (long) ((mapx * zoomchunks) / Math.pow(2, mapzoom-1))+1;
//            mapz = (long) ((mapz * zoomchunks) / Math.pow(2, mapzoom-1))+1;
//            mapoffsetx = mapoffsetx*2-1;
//            mapoffsety = mapoffsety*2-1;
            mapzoom -= 1;
        }
        double newmapx = coordToTile(mapzoom, keepx);
        double newmapz = coordToTile(mapzoom, keepz);
        mapx = (long)newmapx;
        mapz = (long)newmapz;
        mapoffsetx = newmapx - mapx;
        mapoffsety = newmapz - mapz;
        normalizeOffset();
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_R) {
            loadedTextures.clear();
        }
        if(keyCode == GLFW.GLFW_KEY_D) {
            try {
                Util.getOperatingSystem().open(this.getURIfromPos(new MapTilePos(this.worldName, this.dimensionName, this.format, this.mapx, this.mapz, this.mapzoom)));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public static class MapTilePos {
        public String world, dimension, format;
        public long cx, cz, zoom;
        MapTilePos(String world, String dimension, String format, long cx, long cz, int zoom) {
            this.world = world;
            this.dimension = dimension;
            this.cx = cx;
            this.cz = cz;
            this.zoom = zoom;
            this.format = format;
        }
        public Identifier toIdentifier() {
            if(dimension.contains("minecraft:")) {
                dimension = dimension.replaceFirst("minecraft:", "");
            }
            return new Identifier(String.format("%s/%s/%s/%d/%d/%d", world, dimension, format, zoom, cx, cz));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapTilePos that = (MapTilePos) o;
            return cx == that.cx && cz == that.cz && zoom == that.zoom && world.equals(that.world) && dimension.equals(that.dimension) && format.equals(that.format);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, dimension, cx, cz, zoom, format);
        }
    }

    public static class TileUpdate {
        public MapTilePos pos;
        public NativeImage img;

    }
}
