import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * RIFT ROGUE - a League-themed 2D top-down action-roguelike (single file, plain Java Swing).
 * Compile: javac RiftRogue.java   Run: java RiftRogue   (run from the folder that contains /assets)
 * Optional sprites: assets/garen.png, assets/ashe.png, assets/lux.png, assets/masteryi.png
 */
public class RiftRogue extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
    // ===================== CONSTANTS =====================
    static final int W = 1280, H = 720, T = 40, MW = 50, MH = 50;               // screen size, tile size, map size (50x50)
    static final int S_CHAMP = 0, S_SPELL = 1, S_PLAY = 2, S_VILLAGE = 3, S_DEAD = 4, S_WIN = 5; // game states

    // ===================== STATIC DATA TABLES =====================
    static final String[] CH = {"Garen", "Ashe", "Lux", "Master Yi"};                 // champion names
    static final String[] ROLE = {"Tank", "Ranger", "Mage", "Assassin"};               // champion roles
    static final String[] PASSIVE = {                                                  // passive descriptions
        "Perseverance: regen % max HP/s after 5s without damage",
        "Frost Shot: attacks slow targets",
        "Illumination: spells mark; attacks detonate the mark",
        "Double Strike: every 4th attack hits twice"};
    static final String[] SPRITE_FILES = {"garen", "ashe", "lux", "masteryi"};         // expected sprite file names
    static final Color[] CH_COL = {new Color(255, 205, 60), new Color(110, 200, 255), new Color(255, 250, 150), new Color(90, 255, 130)};
    // base stats per champion: HP, Mana, AD, AP, Armor, MR, MoveSpeed(px/s), AttackSpeed, BasicRange (0 = melee)
    static final double[][] BASE = {
        {620, 300, 60, 0, 36, 32, 230, 0.70, 0},
        {540, 320, 58, 0, 24, 30, 225, 0.85, 520},
        {500, 480, 52, 40, 18, 30, 220, 0.65, 480},
        {560, 300, 66, 0, 30, 32, 245, 0.90, 0}};
    static final String[][] SK = {                                                     // skill names [champ][slot]
        {"Decisive Strike", "Courage", "Judgment", "Demacian Justice"},
        {"Ranger's Focus", "Volley", "Hawkshot", "Enchanted Crystal Arrow"},
        {"Light Binding", "Prismatic Barrier", "Lucent Singularity", "Final Spark"},
        {"Alpha Strike", "Meditate", "Wuju Style", "Highlander"}};
    // aim type: 0 line, 1 circle at cursor, 2 cone, 3 self ring, 4 locked target
    static final int[][] AIM = {{3, 3, 3, 4}, {3, 2, 1, 0}, {0, 0, 1, 0}, {3, 3, 3, 3}};
    static final int[][] RNG = {{0, 0, 0, 650}, {0, 400, 1500, 1500}, {750, 550, 650, 1300}, {450, 0, 0, 0}};
    static final int[][] SZ = {{0, 0, 120, 0}, {0, 50, 350, 45}, {55, 55, 160, 95}, {450, 0, 0, 0}}; // width / radius / cone angle
    static final double[][] CD = {{8, 14, 9, 60}, {12, 8, 16, 50}, {9, 12, 10, 55}, {10, 16, 9, 70}};
    static final int[][] MANA = {{30, 40, 40, 80}, {30, 40, 30, 100}, {50, 50, 60, 100}, {40, 50, 30, 100}};
    static final Color[] SLOT_COL = {new Color(220, 70, 70), new Color(70, 160, 255), new Color(90, 220, 120),
        new Color(200, 90, 255), new Color(255, 200, 60), new Color(255, 140, 40)};
    static final String[] SUM = {"Flash", "Ignite", "Cleanse", "Sprint", "Barrier", "Heal", "Exhaust"};
    static final int[] SUM_AIM = {1, 4, 3, 3, 3, 3, 4};                                // summoner aim types
    static final int[] SUM_RNG = {280, 600, 0, 0, 0, 0, 600};
    static final double[] SUM_CD = {20, 20, 25, 20, 25, 25, 25};
    static final String[] SUM_DESC = {"Instant dash", "Burn + 60% anti-heal", "Remove debuffs, +30% tenacity", "+40% move speed",
        "Damage shield", "Heal + speed burst", "30% slow, -40% damage"};

    // ===================== ITEMS =====================
    static final int AD = 0, AP = 1, AR = 2, HP = 3, AS = 4, CR = 5, LS = 6, MN = 7, CDR = 8, MP = 9; // stat indexes
    static final String[] STAT_LBL = {"AD", "AP", "Armor", "HP", "AS%", "Crit%", "LS%", "Mana", "CDR", "MPen"};

    static class Item {                                                                // an item definition
        String n; int cost; double[] s; String[] rec;
        Item(String n, int c, double[] s, String... rec) { this.n = n; cost = c; this.s = s; this.rec = rec; }
    }
    static double[] st(double ad, double ap, double ar, double hp, double as, double cr, double ls, double mn, double cdr, double mp) {
        return new double[]{ad, ap, ar, hp, as, cr, ls, mn, cdr, mp};                  // compact stat array builder
    }
    static final List<Item> BASEI = new ArrayList<>(), LEG = new ArrayList<>();
    static {
        // shop (base) items
        BASEI.add(new Item("Long Sword", 350, st(12, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Dagger", 300, st(0, 0, 0, 0, 15, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Cloth Armor", 300, st(0, 0, 20, 0, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Ruby Crystal", 400, st(0, 0, 0, 150, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Amplifying Tome", 435, st(0, 25, 0, 0, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Pickaxe", 875, st(25, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("BF Sword", 1300, st(40, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        BASEI.add(new Item("Needlessly Large Rod", 1250, st(0, 60, 0, 0, 0, 0, 0, 0, 0, 0)));
        // tier 3 legendary items (components + gold at the Blacksmith)
        LEG.add(new Item("Infinity Edge", 625, st(70, 0, 0, 0, 0, 20, 0, 0, 0, 0), "BF Sword", "Pickaxe"));
        LEG.add(new Item("Blade of the Ruined King", 700, st(40, 0, 0, 0, 25, 0, 10, 0, 0, 0), "Long Sword", "Dagger"));
        LEG.add(new Item("Bloodthirster", 650, st(55, 0, 0, 0, 0, 20, 15, 0, 0, 0), "BF Sword", "Long Sword"));
        LEG.add(new Item("Rabadon's Deathcap", 800, st(0, 120, 0, 0, 0, 0, 0, 0, 0, 0), "Needlessly Large Rod", "Amplifying Tome"));
        LEG.add(new Item("Luden's Companion", 700, st(0, 80, 0, 0, 0, 0, 0, 600, 10, 0), "Needlessly Large Rod", "Ruby Crystal"));
        LEG.add(new Item("Shadowflame", 700, st(0, 120, 0, 0, 0, 0, 0, 0, 0, 12), "Needlessly Large Rod", "Needlessly Large Rod"));
        LEG.add(new Item("Liandry's Torment", 700, st(0, 90, 0, 300, 0, 0, 0, 0, 0, 0), "Amplifying Tome", "Ruby Crystal"));
        LEG.add(new Item("Sunfire Aegis", 600, st(0, 0, 50, 500, 0, 0, 0, 0, 0, 0), "Ruby Crystal", "Cloth Armor"));
        LEG.add(new Item("Thornmail", 600, st(0, 0, 70, 350, 0, 0, 0, 0, 0, 0), "Cloth Armor", "Cloth Armor"));
        LEG.add(new Item("Warmog's Armor", 700, st(0, 0, 0, 1000, 0, 0, 0, 0, 0, 0), "Ruby Crystal", "Ruby Crystal"));
    }

    // ===================== ENTITY CLASSES =====================
    static class Ent {                                                                 // anything with a body + status effects
        double x, y, hp, maxhp, r;
        double slowT, slowA, stunT, rootT, silT, exhT, burnT, burnDps, healCutT, markT; // status expiry times (game seconds)
    }
    static class Mob extends Ent {                                                     // monsters, bosses, honeyfruit bushes
        String name = ""; Color col = Color.RED; int sides = 4, ai = 0, boss = 0, elem = 0;
        double ad, armor, mr, spd, atkCd = 1.1, atkT, t1, t2, gold, xp, fa, pa, shield, burnNext;
        boolean enraged, bush;
    }
    static class Proj {                                                                // projectile (player or enemy)
        double x, y, vx, vy, r, dmg, life, slow, slowD, stun, root, expl, dot;
        int type, pierce = 1, flags; boolean friendly, basic, mark, big; Color c; Mob src;
        Set<Mob> hit = new HashSet<>();
    }
    static class Zone {                                                                // ground area effect (telegraphs, orbs, fire...)
        double x, y, r, start, until, tick, every = 0.5, next, slow; int flags, kind;
        boolean friendly, follow, reveal; Color c; Runnable end; Mob src;
    }
    static class Timed { double t; Runnable r; Timed(double t, Runnable r) { this.t = t; this.r = r; } }
    static class Beam { double x1, y1, x2, y2, w, until, life; Color c; }
    static class FText { double x, y, until; String s; Color c; }
    static class Pickup { double x, y; int type, amt; Pickup(double x, double y, int t, int a) { this.x = x; this.y = y; type = t; amt = a; } }

    // ===================== GAME STATE =====================
    int state = S_CHAMP, champ = 0;                       // current screen / chosen champion
    int[] sum = {0, 1};                                   // chosen summoner spells
    boolean[] sumPick = new boolean[7];                   // spell-select toggles
    double now = 0; long last = System.nanoTime(); Random rnd = new Random();
    Ent p = new Ent();                                    // the player body
    int level = 1, xp = 0, points = 1, gold = 0, floor = 1, bestFloor = 0;
    int[] rank = new int[4];                              // skill ranks
    double[] cds = new double[6], cdTot = new double[6], lastTap = new double[6]; // cooldown expiries / totals / last hotkey tap times
    int aim = -1;                                         // slot currently being aimed (-1 = none)
    boolean isCasting = false; double castEnd = 0, castLock = 0;                  // cast-queue lock
    ArrayDeque<double[]> queue = new ArrayDeque<>();      // FIFO skill queue: {slot, targetX, targetY}
    Item[] inv = new Item[6];                             // 6 inventory slots
    // player buff timers and values
    double shield, shT, spdT, spdA, drT, drA, qNextT, focusT, spinT, spinTick, meditT, wujuT, hlT, tenT, invisT, selfRootT;
    double atkReady, lastHurt = -99, healCutT, pBurnT, pBurnDps, pBurnAcc, sunTick;
    int atkCount;
    // derived stats (recalculated every frame)
    double sAD, sAP, sArm, sMR, sAS, sMS, sCrit, sCDR, sLS, sMPen, maxHp, maxMana, mana;
    // world
    boolean[][] wall; List<Rectangle> rooms = new ArrayList<>();
    double stairX, stairY, camX, camY; boolean cleared; int lastBush = 0, nextGap = 3;
    List<Mob> mobs = new ArrayList<>(); List<Proj> projs = new ArrayList<>(); List<Zone> zones = new ArrayList<>();
    List<Timed> timed = new ArrayList<>(); List<Beam> beams = new ArrayList<>(); List<FText> ftexts = new ArrayList<>();
    List<Pickup> pickups = new ArrayList<>();
    // input
    boolean[] keys = new boolean[512]; boolean hold, showStats; int mx, my;
    String msg = ""; double msgT;
    List<Object[]> btns = new ArrayList<>();              // clickable buttons registered while painting
    BufferedImage[] sprites = new BufferedImage[4];       // optional champion sprites

    // ===================== STARTUP =====================
    public static void main(String[] a) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Rift Rogue");              // create window
            RiftRogue g = new RiftRogue();
            f.add(g); f.pack(); f.setResizable(false); f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); f.setVisible(true); g.requestFocusInWindow();
        });
    }

    RiftRogue() {
        setPreferredSize(new Dimension(W, H)); setFocusable(true);
        addKeyListener(this); addMouseListener(this); addMouseMotionListener(this);
        loadSprites();                                        // try to load assets/*.png (falls back to shapes)
        new javax.swing.Timer(16, e -> {                      // ~60 FPS game loop
            long n = System.nanoTime(); double dt = Math.min(0.033, (n - last) / 1e9); last = n;
            update(dt); repaint();
        }).start();
    }

    void loadSprites() {
        for (int i = 0; i < 4; i++) {
            try {
                File f = new File("assets/" + SPRITE_FILES[i] + ".png");     // relative to working directory
                if (f.exists()) sprites[i] = ImageIO.read(f);                // missing file => stays null => shape fallback
            } catch (Exception ex) { sprites[i] = null; }
        }
    }

    // ===================== RUN / FLOOR SETUP =====================
    void newRun() {                                        // perma-death reset: fresh character, floor 1
        level = 1; xp = 0; points = 1; gold = 0; floor = 1;
        Arrays.fill(rank, 0); Arrays.fill(cds, 0); Arrays.fill(inv, null);
        shield = shT = spdT = spdA = drT = drA = qNextT = focusT = spinT = meditT = wujuT = hlT = tenT = invisT = selfRootT = 0;
        healCutT = pBurnT = 0; lastBush = 0; nextGap = 2 + rnd.nextInt(4);
        p.maxhp = 0; recalc(); p.hp = maxHp; mana = maxMana; p.stunT = p.rootT = p.slowT = 0;
        genFloor(); state = S_PLAY;
    }

    void genFloor() {                                      // procedural 50x50 dungeon with 2-wide corridors
        mobs.clear(); projs.clear(); zones.clear(); timed.clear(); beams.clear(); pickups.clear(); queue.clear();
        isCasting = false; aim = -1; cleared = false;
        wall = new boolean[MW][MH];
        for (boolean[] c : wall) Arrays.fill(c, true);     // start as solid rock
        rooms = new ArrayList<>();
        boolean bossFloor = floor == 20 || floor == 40 || floor == 45 || floor == 50;
        if (bossFloor) rooms.add(new Rectangle(10, 10, 30, 30));          // one big arena
        else {
            for (int tries = 0; tries < 300 && rooms.size() < 9; tries++) { // scatter non-overlapping rooms
                int w = 6 + rnd.nextInt(6), h = 6 + rnd.nextInt(6), x = 2 + rnd.nextInt(MW - w - 4), y = 2 + rnd.nextInt(MH - h - 4);
                Rectangle r = new Rectangle(x, y, w, h); boolean ok = true;
                for (Rectangle o : rooms) if (r.intersects(new Rectangle(o.x - 3, o.y - 3, o.width + 6, o.height + 6))) ok = false;
                if (ok) rooms.add(r);
            }
        }
        for (Rectangle r : rooms) carve(r.x, r.y, r.width, r.height);     // dig rooms
        for (int i = 1; i < rooms.size(); i++) connect(rooms.get(i - 1), rooms.get(i)); // dig corridors
        Rectangle r0 = rooms.get(0), rl = rooms.get(rooms.size() - 1);
        if (bossFloor) {                                                  // boss arena layout
            p.x = 13 * T; p.y = 25 * T; stairX = 37 * T; stairY = 25 * T;
            Mob b = null; double cx = 25 * T, cy = 25 * T;
            if (floor == 20) b = mk("Dragon", cx, cy);
            else if (floor == 40) b = mk("Rift Herald", cx, cy);
            else if (floor == 45) b = mk("Baron Nashor", cx, cy);
            else b = mk("Elder Dragon", cx, cy);
            mobs.add(b);
        } else {
            p.x = (r0.x + r0.width / 2.0) * T; p.y = (r0.y + r0.height / 2.0) * T;
            stairX = (rl.x + rl.width / 2.0) * T; stairY = (rl.y + rl.height / 2.0) * T;
            for (int i = 1; i < rooms.size(); i++) {                      // fill rooms with mobs
                Rectangle r = rooms.get(i); int n = 2 + rnd.nextInt(3) + floor / 15;
                for (int k = 0; k < n && mobs.size() < 30; k++) {
                    String[] pool = pool(); String t = pool[rnd.nextInt(pool.length)];
                    double x = (r.x + 1 + rnd.nextDouble() * (r.width - 2)) * T, y = (r.y + 1 + rnd.nextDouble() * (r.height - 2)) * T;
                    mobs.add(mk(t, x, y));
                }
            }
            if (floor > 20 && floor < 40 && floor % 5 == 0)               // dragon mini-boss every 5 floors
                mobs.add(mk("Mini Dragon", stairX, stairY - 60));
            if (floor - lastBush >= nextGap) {                            // honeyfruit bush in a safe corner of the start room
                Mob b = mk("Honeyfruit Bush", (r0.x + 1.5) * T, (r0.y + 1.5) * T); mobs.add(b);
                lastBush = floor; nextGap = 2 + rnd.nextInt(4);
            }
        }
        say("Floor " + floor + " - " + floorName());
    }

    String[] pool() {                                                     // which mobs appear on this floor
        if (floor < 5) return new String[]{"Wolf"};
        if (floor < 10) return new String[]{"Wolf", "Brambleback"};
        if (floor < 15) return new String[]{"Gromp"};
        if (floor < 20) return new String[]{"Blue Golem", "Red Golem"};
        return new String[]{"Wolf", "Brambleback", "Gromp", "Blue Golem", "Red Golem"};
    }

    String floorName() {
        for (Mob m : mobs) if (m.boss == 1 && !m.name.startsWith("Mini")) return new String[]{"Infernal", "Ocean", "Mountain", "Cloud"}[m.elem - 1] + " Dragon's Lair";
        if (floor == 40) return "Rift Herald's Pit"; if (floor == 45) return "Baron Nashor's Pit"; if (floor == 50) return "Elder Dragon's Sanctum";
        if (floor < 5) return "Wolf Den"; if (floor < 10) return "Bramble Thicket"; if (floor < 15) return "Gromp Swamp";
        if (floor < 20) return "Golem Quarry"; return "Deep Jungle";
    }

    void carve(int x, int y, int w, int h) {                              // clear a rectangle of tiles
        for (int i = x; i < x + w; i++) for (int j = y; j < y + h; j++) if (i > 0 && j > 0 && i < MW - 1 && j < MH - 1) wall[i][j] = false;
    }
    void connect(Rectangle a, Rectangle b) {                              // L-shaped, 2-tile-wide corridor
        int ax = a.x + a.width / 2, ay = a.y + a.height / 2, bx = b.x + b.width / 2, by = b.y + b.height / 2;
        carve(Math.min(ax, bx), ay, Math.abs(ax - bx) + 2, 2); carve(bx, Math.min(ay, by), 2, Math.abs(ay - by) + 2);
    }

    Mob mk(String t, double x, double y) {                                // mob factory with floor scaling
        Mob m = new Mob(); m.name = t; m.x = x; m.y = y;
        double hp = 60, ad = 9, ar = 8, mr = 8, sp = 190, r = 13, xpv = 10, g = 6; Color c = new Color(255, 120, 60); int sides = 3, ai = 1;
        switch (t) {
            case "Wolf": break;                                                                // pounce leap + bite
            case "Brambleback": hp = 180; ad = 14; ar = 20; mr = 15; sp = 110; r = 20; c = new Color(90, 220, 90); sides = 6; xpv = 18; g = 10; ai = 0; break;
            case "Gromp": hp = 230; ad = 16; ar = 15; mr = 15; sp = 95; r = 24; c = new Color(170, 230, 60); sides = 8; xpv = 24; g = 14; ai = 2; break;
            case "Blue Golem": hp = 380; ad = 22; ar = 30; mr = 30; sp = 85; r = 28; c = new Color(70, 140, 255); sides = 4; xpv = 35; g = 22; ai = 3; break;
            case "Red Golem": hp = 400; ad = 28; ar = 35; mr = 25; sp = 95; r = 28; c = new Color(255, 70, 70); sides = 4; xpv = 35; g = 22; ai = 0; break;
            case "Honeyfruit Bush": hp = 120; ad = 0; ar = 0; mr = 0; sp = 0; r = 18; c = new Color(255, 170, 40); sides = 12; ai = 9; m.bush = true; xpv = 0; g = 0; break;
            case "Mini Dragon": case "Dragon": hp = t.equals("Dragon") ? 1800 : 1000; ad = 40; ar = 40; mr = 40; sp = 100; r = 38; sides = 7; ai = 4; m.boss = 1; xpv = 150; g = 120;
                m.elem = 1 + rnd.nextInt(4); c = new Color[]{new Color(255, 90, 30), new Color(60, 140, 255), new Color(170, 130, 80), new Color(210, 235, 255)}[m.elem - 1];
                if (m.elem == 3) { ar *= 2; mr *= 2; }                                         // Mountain: high armor
                if (m.elem == 4) m.atkCd = 0.8; break;                                         // Cloud: fast attacks
            case "Rift Herald": hp = 3500; ad = 45; ar = 50; mr = 40; sp = 90; r = 46; c = new Color(180, 120, 255); sides = 5; ai = 10; m.boss = 10; xpv = 300; g = 250; break;
            case "Baron Nashor": hp = 5500; ad = 55; ar = 60; mr = 60; sp = 0; r = 62; c = new Color(200, 80, 220); sides = 9; ai = 11; m.boss = 11; xpv = 500; g = 400; break;
            default: hp = 6500; ad = 70; ar = 70; mr = 70; sp = 85; r = 52; c = new Color(255, 60, 20); sides = 8; ai = 12; m.boss = 12; xpv = 800; g = 800; // Elder Dragon
        }
        double hs = Math.pow(1.08, floor - 1), as = Math.pow(1.08, (floor - 1) * 0.85);   // scaling formulas from the spec
        m.maxhp = m.hp = hp * hs; m.ad = ad * as; m.armor = ar + 1.5 * (floor - 1); m.mr = mr + 1.5 * (floor - 1);
        m.spd = sp; m.r = r; m.col = c; m.sides = sides; m.ai = ai; m.xp = xpv * (1 + floor * 0.05); m.gold = g * (1 + floor * 0.12);
        if (m.elem == 3) m.shield = m.maxhp * 0.3;                                          // Mountain dragon starts shielded
        return m;
    }

    // ===================== STATS / ITEMS =====================
    boolean has(String n) { for (Item i : inv) if (i != null && i.n.equals(n)) return true; return false; }

    void recalc() {                                        // rebuild all derived stats from level + items + buffs
        double[] b = BASE[champ]; int L = level - 1; double[] it = new double[10];
        for (Item i : inv) if (i != null) for (int k = 0; k < 10; k++) it[k] += i.s[k];   // sum item stats
        maxHp = b[0] + L * 90 + it[HP]; maxMana = b[1] + L * 35 + it[MN];
        sAD = b[2] + L * 3.2 + it[AD]; sAP = b[3] + L * 4 + it[AP]; if (has("Rabadon's Deathcap")) sAP *= 1.3;
        sArm = b[4] + L * 3.5 + it[AR]; sMR = b[5] + L * 1.25;
        sAS = b[7] * (1 + L * 0.02 + it[AS] / 100.0 + (focusT > now ? 0.3 + 0.1 * rank[0] : 0) + (hlT > now ? 0.5 : 0));
        sMS = b[6] * (1 + (spdT > now ? spdA : 0) + (hlT > now ? 0.4 : 0));
        sCrit = it[CR]; sCDR = Math.min(40, it[CDR]); sLS = it[LS]; sMPen = it[MP];
        if (maxHp > p.maxhp && p.maxhp > 0) p.hp += maxHp - p.maxhp;                      // gain HP when max HP rises
        p.maxhp = maxHp; p.hp = Math.min(p.hp, maxHp); mana = Math.min(mana, maxMana); p.r = 14;
    }

    boolean canForge(Item l) {                             // do we own the components and the gold?
        if (gold < l.cost) return false;
        List<Item> tmp = new ArrayList<>(Arrays.asList(inv));
        for (String r : l.rec) { boolean f = false; for (int i = 0; i < tmp.size(); i++) if (tmp.get(i) != null && tmp.get(i).n.equals(r)) { tmp.set(i, null); f = true; break; } if (!f) return false; }
        return true;
    }
    void forge(Item l) {                                   // consume components + gold, add legendary item
        if (!canForge(l)) return;
        for (String r : l.rec) for (int i = 0; i < 6; i++) if (inv[i] != null && inv[i].n.equals(r)) { inv[i] = null; break; }
        for (int i = 0; i < 6; i++) if (inv[i] == null) { inv[i] = l; break; }
        gold -= l.cost; recalc();
    }
    void buy(Item it) {
        if (gold < it.cost) return;
        for (int i = 0; i < 6; i++) if (inv[i] == null) { inv[i] = it; gold -= it.cost; recalc(); return; }
        say("Inventory full!");
    }
    String desc(Item it) {                                 // short stat string
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 10; k++) if (it.s[k] != 0) sb.append(STAT_LBL[k]).append(" +").append((int) it.s[k]).append("  ");
        return sb.toString();
    }

    // ===================== HELPERS =====================
    void say(String s) { msg = s; msgT = now + 2.5; }
    void ftext(double x, double y, String s, Color c) { FText f = new FText(); f.x = x + rnd.nextInt(20) - 10; f.y = y; f.s = s; f.c = c; f.until = now + 1.0; ftexts.add(f); }
    static double angDiff(double a, double b) { double d = a - b; while (d > Math.PI) d -= 2 * Math.PI; while (d < -Math.PI) d += 2 * Math.PI; return d; }
    double dist(Ent e) { return Math.hypot(e.x - p.x, e.y - p.y); }
    boolean isWall(int x, int y) { return x < 0 || y < 0 || x >= MW || y >= MH || wall[x][y]; }
    boolean blocked(double x, double y, double r) {        // circle vs. wall tiles
        for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) if (isWall((int) Math.floor((x + i * r) / T), (int) Math.floor((y + j * r) / T))) return true;
        return false;
    }
    void move(Ent e, double dx, double dy) { if (!blocked(e.x + dx, e.y, e.r)) e.x += dx; if (!blocked(e.x, e.y + dy, e.r)) e.y += dy; } // axis-separated
    static double segDist(double px, double py, double x1, double y1, double x2, double y2) { // point-to-segment distance
        double dx = x2 - x1, dy = y2 - y1, l = dx * dx + dy * dy, t = l == 0 ? 0 : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / l));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }
    boolean visible(Mob m) {                               // fog of war: nearby or inside a Hawkshot reveal
        if (dist(m) < 560) return true;
        for (Zone z : zones) if (z.reveal && Math.hypot(z.x - m.x, z.y - m.y) < z.r) return true;
        return false;
    }
    Mob lockTarget(double wx, double wy) {                 // monster nearest the cursor (target lock)
        Mob best = null; double bd = 1e9;
        for (Mob m : mobs) { if (m.bush || !visible(m)) continue; double d = Math.hypot(m.x - wx, m.y - wy); if (d < m.r + 45 && d < bd) { bd = d; best = m; } }
        return best;
    }
    void addShield(double a, double dur) { shield += a; shT = Math.max(shT, now + dur); }
    void heal(double a) {                                  // healing, reduced by anti-heal; Bloodthirster turns overheal into shield
        if (healCutT > now) a *= 0.4;
        if (has("Bloodthirster") && p.hp + a > maxHp) addShield(Math.min(a - (maxHp - p.hp), 150), 3);
        p.hp = Math.min(maxHp, p.hp + a);
    }
    void stunPlayer(double d) { if (tenT > now) d *= 0.7; p.stunT = Math.max(p.stunT, now + d); }
    void mark(Mob m) { if (champ == 2) m.markT = now + 4; }          // Lux illumination mark
    Zone nz(double x, double y, double r, double dur, Color c, boolean friendly) {
        Zone z = new Zone(); z.x = x; z.y = y; z.r = r; z.start = now; z.until = now + dur; z.c = c; z.friendly = friendly; z.next = now; return z;
    }
    Proj pj(double ang, double spd, double range, double r, double dmg, int type, Color c) { // friendly projectile from the player
        Proj q = new Proj(); q.x = p.x; q.y = p.y; q.vx = Math.cos(ang) * spd; q.vy = Math.sin(ang) * spd; q.life = range / spd; q.r = r;
        q.dmg = dmg; q.type = type; q.c = c; q.friendly = true; projs.add(q); return q;
    }
    void eproj(Mob m, double ang, double spd, double dmg, int type, Color c, int fl, double dot) { // enemy projectile
        Proj q = new Proj(); q.x = m.x; q.y = m.y; q.vx = Math.cos(ang) * spd; q.vy = Math.sin(ang) * spd; q.life = 3; q.r = 10;
        q.dmg = dmg; q.type = type; q.c = c; q.flags = fl; q.dot = dot; q.src = m; projs.add(q);
    }
    void beam(double x1, double y1, double x2, double y2, double w, double dur, Color c) {
        Beam b = new Beam(); b.x1 = x1; b.y1 = y1; b.x2 = x2; b.y2 = y2; b.w = w; b.life = dur; b.until = now + dur; b.c = c; beams.add(b);
    }

    // ===================== DAMAGE =====================
    void hitMob(Mob m, double raw, int type, boolean crit) {           // type: 0 phys, 1 magic, 2 true
        if (m.hp <= 0 || !mobs.contains(m)) return;
        double d = raw;
        if (type == 0) d = raw * 100 / (100 + m.armor);                 // armor reduction
        else if (type == 1) {
            d = raw * 100 / (100 + Math.max(0, m.mr - sMPen));          // magic resist minus magic pen
            if (has("Shadowflame") && m.hp < 0.35 * m.maxhp) { d *= 1.25; crit = true; } // magic crit when low
            if (has("Liandry's Torment")) { m.burnT = now + 3; m.burnDps = Math.max(m.burnDps, 0.02 * m.maxhp); } // 2% max HP burn
        }
        if (m.boss == 10) {                                              // Rift Herald: armored front, weak back
            double dot = Math.cos(m.fa) * (p.x - m.x) + Math.sin(m.fa) * (p.y - m.y);
            if (dot > 0) d *= 0.35; else { d *= 1.6; crit = true; }
        }
        if (m.shield > 0) { double a = Math.min(m.shield, d); m.shield -= a; d -= a; } // mob shield soaks
        m.hp -= d;
        ftext(m.x, m.y - m.r, "" + Math.round(d), crit ? Color.YELLOW : type == 0 ? new Color(255, 80, 80) : type == 1 ? new Color(190, 90, 255) : Color.WHITE);
        if (m.hp <= 0) kill(m);
    }

    void hurt(double raw, int type, Mob src, int fl) {                  // damage to the player; fl: 1 = execute, 2 = anti-heal
        if (state != S_PLAY || p.hp <= 0) return;
        double d = raw;
        if (type == 0) d = d * 100 / (100 + sArm); else if (type == 1) d = d * 100 / (100 + sMR);
        if (drT > now) d *= 1 - drA;                                     // damage reduction buffs
        if (src != null && src.exhT > now) d *= 0.6;                     // Exhaust: -40% damage
        if (shield > 0 && shT > now) { double a = Math.min(shield, d); shield -= a; d -= a; }
        lastHurt = now; p.hp -= d;
        if ((fl & 2) != 0) healCutT = now + 3;                           // anti-heal on player
        if (src != null && has("Thornmail")) { src.healCutT = now + 3; hitMob(src, d * 0.3 + 10, 2, false); } // reflect + anti-heal
        if (d > 0.5) ftext(p.x, p.y - 30, "" + Math.round(d), new Color(255, 130, 130));
        if ((fl & 1) != 0 && p.hp < 0.2 * maxHp) p.hp = 0;               // Elder Dragon execute
        if (p.hp <= 0) { p.hp = 0; state = S_DEAD; bestFloor = Math.max(bestFloor, floor); }
    }

    void kill(Mob m) {
        mobs.remove(m);
        if (m.bush) { for (int i = 0; i < 5; i++) pickups.add(new Pickup(m.x + rnd.nextGaussian() * 30, m.y + rnd.nextGaussian() * 30, 1, 0)); return; } // 5 honeyfruits
        pickups.add(new Pickup(m.x, m.y, 0, (int) m.gold));              // gold drop
        addXp((int) m.xp);
        if (hlT > now) hlT = Math.min(now + 10, hlT + 1.5);              // Highlander: takedowns extend duration
        if (m.boss == 12) state = S_WIN;                                 // Elder Dragon = victory
    }

    void addXp(int a) {
        xp += a;
        while (level < 18 && xp >= 40 + level * 30) { xp -= 40 + level * 30; level++; points++; recalc(); p.hp = Math.min(maxHp, p.hp + maxHp * 0.1); say("LEVEL UP! Spend your skill point (+)"); }
    }
    boolean canUp(int i) {                                               // skill-point rules from the spec
        if (points <= 0) return false;
        if (i < 3) return level >= i + 1 && rank[i] < 4 && rank[i] < (level + 1) / 2 + 1;
        return level >= 5 && rank[3] < 3 && rank[3] < (level >= 13 ? 3 : level >= 9 ? 2 : 1);
    }

    // ===================== BASIC ATTACK =====================
    void basicAttack() {
        if (now < atkReady || p.stunT > now || isCasting) return;
        atkReady = now + 1.0 / sAS;
        double ang = Math.atan2(my + camY - p.y, mx + camX - p.x);
        if (BASE[champ][8] == 0) {                                       // melee: 120-degree arc in front
            for (Mob m : new ArrayList<>(mobs)) {
                double dx = m.x - p.x, dy = m.y - p.y;
                if (Math.hypot(dx, dy) < 95 + m.r && Math.abs(angDiff(Math.atan2(dy, dx), ang)) < 1.1) onHit(m);
            }
            beam(p.x, p.y, p.x + Math.cos(ang) * 80, p.y + Math.sin(ang) * 80, 10, 0.12, CH_COL[champ]);
        } else {                                                         // ranged: arrow / bolt (Ashe focus = 3 arrows)
            int n = (champ == 1 && focusT > now) ? 3 : 1;
            for (int i = 0; i < n; i++) { Proj q = pj(ang + (i - (n - 1) / 2.0) * 0.14, 750, BASE[champ][8], 7, sAD, 0, CH_COL[champ]); q.basic = true; }
        }
    }

    void onHit(Mob m) {                                                  // on-hit effects + passives
        boolean crit = rnd.nextDouble() * 100 < sCrit; double dmg = sAD * (crit ? 1.75 + (has("Infinity Edge") ? 0.4 : 0) : 1);
        if (champ == 0 && qNextT > now) { qNextT = 0; dmg += 30 + 25 * rank[0] + sAD * 0.5; m.silT = now + 1.5; ftext(m.x, m.y - 40, "SILENCED", Color.WHITE); } // Decisive Strike
        hitMob(m, dmg, 0, crit);
        heal(dmg * sLS / 100);                                           // lifesteal
        if (has("Blade of the Ruined King")) hitMob(m, Math.max(8, m.hp * 0.04), 0, false); // % current HP on-hit
        if (champ == 1) { m.slowT = now + 1.5; m.slowA = 0.3; }          // Frost Shot
        if (champ == 2 && m.markT > now) { m.markT = 0; hitMob(m, 20 + level * 8 + sAP * 0.35, 1, false); } // Illumination detonation
        if (champ == 3) {                                                // Master Yi
            atkCount++; if (atkCount % 4 == 0) hitMob(m, sAD * 0.6, 0, false);          // Double Strike
            if (wujuT > now) hitMob(m, 15 + 12 * rank[2], 2, false);                    // Wuju true damage
        }
    }

    // ===================== SPELL CASTING =====================
    void tap(int slot) {                                                 // hotkey press: double-tap = quick cast, single = aim mode
        if (state != S_PLAY) return;
        double wx = mx + camX, wy = my + camY;
        if (now - lastTap[slot] < 0.3) { enqueue(slot, wx, wy); aim = -1; lastTap[slot] = -9; }
        else { aim = slot; lastTap[slot] = now; }
    }
    void enqueue(int slot, double wx, double wy) { if (queue.size() < 6) queue.add(new double[]{slot, wx, wy}); }

    void processQueue() {                                                // FIFO queue with isCasting lock (no animation canceling)
        if (isCasting && now >= castEnd) isCasting = false;
        if (isCasting || queue.isEmpty() || p.stunT > now) return;
        double[] q = queue.poll(); castLock = 0;
        if (cast((int) q[0], q[1], q[2])) { isCasting = true; castEnd = now + (castLock > 0 ? castLock : 0.2); }
    }

    boolean cast(int s, double tx, double ty) {                          // generic checks, then champion/summoner logic
        if (now < cds[s]) { say("On cooldown"); return false; }
        if (s < 4) {
            if (rank[s] == 0) { say(SK[champ][s] + " not learned - click +"); return false; }
            if (mana < MANA[champ][s]) { say("Not enough mana"); return false; }
        }
        boolean ok = s < 4 ? castSkill(s, tx, ty) : castSummoner(sum[s - 4], tx, ty);
        if (!ok) return false;
        if (s < 4) {
            mana -= MANA[champ][s]; double cd = CD[champ][s] * (1 - sCDR / 100); cds[s] = now + cd; cdTot[s] = cd;
            if (has("Luden's Companion")) for (Mob m : new ArrayList<>(mobs)) if (dist(m) < 220) hitMob(m, 30 + 0.12 * sAP, 1, false); // splash burst
        } else { double cd = SUM_CD[sum[s - 4]]; cds[s] = now + cd; cdTot[s] = cd; }
        return true;
    }

    boolean castSkill(int s, double tx, double ty) {
        int rk = rank[s]; double ang = Math.atan2(ty - p.y, tx - p.x);
        double cdx = tx - p.x, cdy = ty - p.y, cdl = Math.hypot(cdx, cdy);
        switch (champ * 10 + s) {
            // ---------- GAREN ----------
            case 0: spdT = now + 3; spdA = 0.4; qNextT = now + 5; say("Decisive Strike ready"); return true;
            case 1: addShield(70 + 45 * rk, 2.5); drT = now + 2.5; drA = 0.6; return true;      // shield + 60% DR
            case 2: spinT = now + 3; spinTick = now; return true;                               // Judgment (ticks in update)
            case 3: {                                                                           // Demacian Justice
                final Mob t = lockTarget(tx, ty);
                if (t == null || dist(t) > RNG[0][3] + t.r) { say("No target locked in range"); return false; }
                Zone z = nz(t.x, t.y, 70, 0.7, new Color(255, 215, 0, 140), true); z.kind = 1;
                final int r = rk;
                z.end = () -> { if (mobs.contains(t)) hitMob(t, 100 + 120 * r + 0.3 * (t.maxhp - t.hp) + sAD * 0.5, 2, false); }; // true dmg + missing HP
                zones.add(z); return true;
            }
            // ---------- ASHE ----------
            case 10: focusT = now + 5; say("Ranger's Focus!"); return true;
            case 11:                                                                            // Volley cone
                for (int i = 0; i < 7; i++) {
                    double a = ang + Math.toRadians(-SZ[1][1] / 2.0 + SZ[1][1] * i / 6.0);
                    Proj q = pj(a, 800, RNG[1][1], 8, 15 + 18 * rk + sAD * 0.5, 0, new Color(160, 230, 255)); q.slow = 0.3; q.slowD = 2;
                }
                return true;
            case 12: {                                                                          // Hawkshot reveal
                double d = Math.min(cdl, RNG[1][2]); Zone z = nz(p.x + Math.cos(ang) * d, p.y + Math.sin(ang) * d, SZ[1][2], 6, new Color(120, 200, 255, 50), true); z.reveal = true; zones.add(z); return true;
            }
            case 13: { Proj q = pj(ang, 700, RNG[1][3], 22, 150 + 130 * rk + sAD, 1, new Color(150, 230, 255)); q.stun = 1.5 + 0.5 * rk; q.expl = 130; q.big = true; return true; } // crystal arrow
            // ---------- LUX ----------
            case 20: { Proj q = pj(ang, 850, RNG[2][0], 20, 50 + 45 * rk + 0.6 * sAP, 1, new Color(255, 240, 150)); q.root = 1.5; q.pierce = 2; q.mark = true; return true; } // roots up to 2
            case 21: {                                                                          // shield out and back
                final double v = 50 + 35 * rk + 0.4 * sAP; addShield(v, 2.5);
                timed.add(new Timed(now + 0.9, () -> addShield(v, 2.5)));
                beam(p.x, p.y, p.x + Math.cos(ang) * RNG[2][1], p.y + Math.sin(ang) * RNG[2][1], 8, 0.9, new Color(150, 200, 255)); return true;
            }
            case 22: {                                                                          // Lucent Singularity
                double d = Math.min(cdl, RNG[2][2]); final Zone z = nz(p.x + Math.cos(ang) * d, p.y + Math.sin(ang) * d, SZ[2][2], 2.5, new Color(255, 245, 160, 90), true);
                z.slow = 0.4; final int r = rk;
                z.end = () -> { for (Mob m : new ArrayList<>(mobs)) if (Math.hypot(m.x - z.x, m.y - z.y) < z.r + m.r) { hitMob(m, 70 + 55 * r + 0.7 * sAP, 1, false); mark(m); } beam(z.x, z.y, z.x + 1, z.y, z.r * 2, 0.25, new Color(255, 255, 200, 180)); };
                zones.add(z); return true;
            }
            case 23: {                                                                          // Final Spark
                double ex = p.x + Math.cos(ang) * RNG[2][3], ey = p.y + Math.sin(ang) * RNG[2][3];
                for (Mob m : new ArrayList<>(mobs)) if (segDist(m.x, m.y, p.x, p.y, ex, ey) < SZ[2][3] / 2.0 + m.r) { hitMob(m, 250 + 200 * rk + sAP, 1, false); mark(m); }
                beam(p.x, p.y, ex, ey, 95, 0.5, new Color(255, 120, 220, 110)); beam(p.x, p.y, ex, ey, 60, 0.5, new Color(255, 240, 160, 170)); beam(p.x, p.y, ex, ey, 25, 0.5, Color.WHITE);
                return true;
            }
            // ---------- MASTER YI ----------
            case 30: {                                                                          // Alpha Strike
                final List<Mob> ts = new ArrayList<>();
                for (Mob m : mobs) if (!m.bush && dist(m) < RNG[3][0]) ts.add(m);
                ts.sort((a, b) -> Double.compare(dist(a), dist(b)));
                if (ts.isEmpty()) { say("No enemies in range"); return false; }
                while (ts.size() > 4) ts.remove(ts.size() - 1);                                // max 4 targets
                final int n = ts.size(), r = rk; invisT = now + 0.1 * n + 0.15; castLock = 0.1 * n + 0.15;
                for (int i = 0; i < n; i++) {
                    final Mob m = ts.get(i);
                    timed.add(new Timed(now + 0.1 * i, () -> {                                  // one strike every 100ms
                        if (!mobs.contains(m)) return;
                        double ox = p.x, oy = p.y; p.x = m.x - 26; p.y = m.y; if (blocked(p.x, p.y, p.r)) { p.x = m.x; p.y = m.y; }
                        beam(ox, oy, m.x, m.y, 7, 0.3, new Color(60, 255, 200));               // neon slash trail
                        hitMob(m, 40 + 45 * r + 0.6 * sAD, 0, false);
                    }));
                }
                timed.add(new Timed(now + 0.1 * n + 0.05, () -> { Mob m = ts.get(0); if (mobs.contains(m)) { p.x = m.x - 30; p.y = m.y; if (blocked(p.x, p.y, p.r)) { p.x = m.x; p.y = m.y; } } })); // reappear at primary
                return true;
            }
            case 31: meditT = now + 4; drT = now + 4; drA = 0.6; selfRootT = now + 4; return true; // Meditate channel
            case 32: wujuT = now + 6; return true;                                              // Wuju Style
            case 33: hlT = now + 7; say("HIGHLANDER!"); return true;
        }
        return false;
    }

    boolean castSummoner(int id, double tx, double ty) {
        double ang = Math.atan2(ty - p.y, tx - p.x);
        switch (id) {
            case 0: {                                                                           // Flash
                double d = Math.min(Math.hypot(tx - p.x, ty - p.y), SUM_RNG[0]);
                for (double k = d; k > 0; k -= 8) { double nx = p.x + Math.cos(ang) * k, ny = p.y + Math.sin(ang) * k; if (!blocked(nx, ny, p.r)) { beam(p.x, p.y, nx, ny, 6, 0.2, Color.WHITE); p.x = nx; p.y = ny; return true; } }
                return false;
            }
            case 1: { Mob t = lockTarget(tx, ty); if (t == null || dist(t) > SUM_RNG[1]) { say("No target locked"); return false; }
                t.burnT = now + 5; t.burnDps = (50 + 20 * level) / 5.0; t.healCutT = now + 5; return true; } // burn + 60% anti-heal
            case 2: p.stunT = p.rootT = p.slowT = p.silT = 0; pBurnT = 0; tenT = now + 3; return true; // Cleanse
            case 3: spdT = now + 3; spdA = 0.4; return true;                                   // Sprint
            case 4: addShield(100 + 25 * level, 2); return true;                               // Barrier
            case 5: heal(90 + 20 * level); spdT = now + 2; spdA = 0.3; return true;            // Heal
            default: { Mob t = lockTarget(tx, ty); if (t == null || dist(t) > SUM_RNG[6]) { say("No target locked"); return false; }
                t.exhT = now + 3; t.slowT = now + 3; t.slowA = 0.3; return true; }              // Exhaust
        }
    }

    // ===================== UPDATE LOOP =====================
    void update(double dt) {
        if (state != S_PLAY) return;
        now += dt; recalc();
        for (Timed t : new ArrayList<>(timed)) if (now >= t.t) { timed.remove(t); t.r.run(); } // scheduled events
        if (state != S_PLAY) return;
        // ----- movement -----
        double dx = 0, dy = 0;
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) dy -= 1; if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) dy += 1;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) dx -= 1; if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) dx += 1;
        boolean canMove = p.stunT <= now && p.rootT <= now && selfRootT <= now && invisT <= now;
        if (canMove && (dx != 0 || dy != 0)) {
            double l = Math.hypot(dx, dy), sp = sMS * (p.slowT > now && hlT <= now ? 1 - p.slowA : 1);
            move(p, dx / l * sp * dt, dy / l * sp * dt);                                        // 8-directional
        }
        camX = Math.max(0, Math.min(MW * T - W, p.x - W / 2)); camY = Math.max(0, Math.min(MH * T - H, p.y - H / 2));
        if (hold && aim < 0) basicAttack();                                                     // hold left click = auto-attack
        processQueue();
        // ----- buffs, regen, passives -----
        if (shT <= now) shield = 0;
        double regen = maxHp * 0.004 * (has("Warmog's Armor") ? 2 : 1);
        if (now - lastHurt > 5) { if (champ == 0) regen += maxHp * 0.015; if (has("Warmog's Armor")) regen += maxHp * 0.03; } // Perseverance / Warmog
        heal(regen * dt);
        if (meditT > now) heal((50 + 35 * rank[1]) * dt);                                       // Meditate heal
        mana = Math.min(maxMana, mana + (5 + maxMana * 0.015) * dt);
        if (pBurnT > now) { pBurnAcc += dt; if (pBurnAcc >= 0.5) { pBurnAcc = 0; hurt(pBurnDps * 0.5, 1, null, 0); } } // poison / burn on player
        if (spinT > now && now >= spinTick) {                                                   // Judgment blades
            spinTick = now + 0.25;
            for (Mob m : new ArrayList<>(mobs)) if (dist(m) < 125 + m.r) hitMob(m, 10 + 10 * rank[2] + 0.25 * sAD, 0, false);
        }
        if (has("Sunfire Aegis") && now >= sunTick) { sunTick = now + 0.5; for (Mob m : new ArrayList<>(mobs)) if (dist(m) < 130 + m.r) hitMob(m, 10 + 0.03 * maxHp, 1, false); } // immolate
        // ----- projectiles -----
        for (Proj q : new ArrayList<>(projs)) {
            q.x += q.vx * dt; q.y += q.vy * dt; q.life -= dt;
            if (q.life <= 0 || blocked(q.x, q.y, 0)) { projs.remove(q); continue; }
            if (q.friendly) {
                for (Mob m : new ArrayList<>(mobs)) {
                    if (q.hit.contains(m) || Math.hypot(m.x - q.x, m.y - q.y) >= m.r + q.r) continue;
                    q.hit.add(m); projHit(q, m);
                    if (--q.pierce <= 0) { projs.remove(q); break; }
                }
            } else if (Math.hypot(p.x - q.x, p.y - q.y) < p.r + q.r) {                          // enemy projectile hits player
                hurt(q.dmg, q.type, q.src, q.flags); if (q.dot > 0) { pBurnT = now + 3; pBurnDps = q.dot; } projs.remove(q);
            }
        }
        // ----- zones -----
        for (Zone z : new ArrayList<>(zones)) {
            if (now >= z.until) { zones.remove(z); if (z.end != null) z.end.run(); continue; }
            if (now < z.start) continue;                                                        // still telegraphing
            boolean tick = z.tick > 0 && now >= z.next;
            if (z.friendly) {
                for (Mob m : mobs) if (Math.hypot(m.x - z.x, m.y - z.y) < z.r + m.r && z.slow > 0) { m.slowT = now + 0.3; m.slowA = z.slow; }
            } else if (Math.hypot(p.x - z.x, p.y - z.y) < z.r + p.r) {
                if (tick) hurt(z.tick, 1, z.src, z.flags);
                if (z.slow > 0 && hlT <= now) { p.slowT = now + 0.3; p.slowA = z.slow; }
            }
            if (tick) z.next = now + z.every;
        }
        // ----- mobs -----
        for (Mob m : new ArrayList<>(mobs)) if (mobs.contains(m)) ai(m, dt);
        for (int i = 0; i < mobs.size(); i++) for (int j = i + 1; j < mobs.size(); j++) {      // soft separation so mobs don't stack
            Mob a = mobs.get(i), b = mobs.get(j); if (a.spd == 0 || b.spd == 0) continue;
            double ddx = b.x - a.x, ddy = b.y - a.y, d = Math.hypot(ddx, ddy), min = (a.r + b.r) * 0.8;
            if (d < min && d > 0.01) { double push = (min - d) / 2; move(a, -ddx / d * push, -ddy / d * push); move(b, ddx / d * push, ddy / d * push); }
        }
        // ----- pickups -----
        for (Pickup k : new ArrayList<>(pickups)) {
            double d = Math.hypot(k.x - p.x, k.y - p.y);
            if (d < 130) { k.x += (p.x - k.x) * 4 * dt; k.y += (p.y - k.y) * 4 * dt; }          // magnet
            if (d < 24) {
                if (k.type == 0) { gold += k.amt; ftext(p.x, p.y - 40, "+" + k.amt + "g", Color.YELLOW); }
                else { heal(maxHp * 0.08); mana = Math.min(maxMana, mana + maxMana * 0.08); ftext(p.x, p.y - 40, "Honeyfruit!", new Color(255, 190, 80)); } // 8% HP + Mana
                pickups.remove(k);
            }
        }
        // ----- floor cleared / stairs -----
        if (!cleared) { boolean any = false; for (Mob m : mobs) if (!m.bush) { any = true; break; } if (!any) { cleared = true; say("Floor cleared! Go to the stairs"); } }
        ftexts.removeIf(f -> now > f.until); beams.removeIf(b -> now > b.until);
    }

    void projHit(Proj q, Mob m) {                                                               // what a friendly projectile does on contact
        if (q.basic) { onHit(m); return; }
        hitMob(m, q.dmg, q.type, false);
        if (q.slow > 0) { m.slowT = now + q.slowD; m.slowA = q.slow; }
        if (q.stun > 0) m.stunT = now + q.stun;
        if (q.root > 0) m.rootT = now + q.root;
        if (q.mark) mark(m);
        if (q.expl > 0) for (Mob o : new ArrayList<>(mobs)) if (o != m && Math.hypot(o.x - q.x, o.y - q.y) < q.expl + o.r) { hitMob(o, q.dmg * 0.6, q.type, false); o.stunT = now + q.stun; } // crystal arrow explosion
    }

    // ===================== MOB AI =====================
    void meleeAtk(Mob m, int fl) {
        if (now >= m.atkT && Math.hypot(p.x - m.x, p.y - m.y) < m.r + p.r + 16) { m.atkT = now + m.atkCd; hurt(m.ad, 0, m, fl); }
    }

    void ai(Mob m, double dt) {
        if (m.bush) return;
        if (m.burnT > now && now >= m.burnNext) { m.burnNext = now + 0.5; hitMob(m, m.burnDps * 0.5, 2, false); if (!mobs.contains(m)) return; } // burn ticks
        if (m.stunT > now) return;
        double dx = p.x - m.x, dy = p.y - m.y, d = Math.hypot(dx, dy) + 1e-6, ux = dx / d, uy = dy / d;
        double sp = m.spd * (m.slowT > now ? 1 - m.slowA : 1) * (m.rootT > now ? 0 : 1);
        boolean sil = m.silT > now; int fl = (m.boss == 12 ? 1 : 0) | (m.elem == 1 ? 2 : 0);
        if (m.boss != 10) m.fa = Math.atan2(dy, dx);                                            // face the player (Herald turns slowly)
        switch (m.ai) {
            case 0: if (d < 800 && d > m.r + p.r + 6) move(m, ux * sp * dt, uy * sp * dt); meleeAtk(m, 0); break;  // plain melee chaser
            case 1:                                                                             // wolf: Pack Pounce leap + bite
                if (m.t2 > now) move(m, Math.cos(m.pa) * 520 * dt, Math.sin(m.pa) * 520 * dt);
                else if (d < 280 && d > 90 && now >= m.t1 && !sil) { m.t1 = now + 4; m.t2 = now + 0.3; m.pa = Math.atan2(dy, dx); }
                else if (d < 800 && d > m.r + p.r + 6) move(m, ux * sp * dt, uy * sp * dt);
                meleeAtk(m, 0); break;
            case 2:                                                                             // gromp: kites, spits poison, hop knockback
                if (d < 230) move(m, -ux * sp * 0.8 * dt, -uy * sp * 0.8 * dt); else if (d > 420 && d < 900) move(m, ux * sp * dt, uy * sp * dt);
                if (d < 450 && now >= m.t1 && !sil) { m.t1 = now + 2.2; eproj(m, Math.atan2(dy, dx), 330, m.ad * 0.8, 1, new Color(150, 255, 60), 0, m.ad * 0.15); }
                if (d < 80 && now >= m.t2 && !sil) { m.t2 = now + 3.5; move(p, ux * 90, uy * 90); hurt(m.ad, 0, m, 0); }
                break;
            case 3:                                                                             // blue golem: ranged orbs
                if (d < 260) move(m, -ux * sp * dt, -uy * sp * dt); else if (d > 400 && d < 900) move(m, ux * sp * dt, uy * sp * dt);
                if (d < 600 && now >= m.t1 && !sil) { m.t1 = now + 2.4; eproj(m, Math.atan2(dy, dx), 330, m.ad * 1.1, 1, new Color(90, 170, 255), 0, 0); }
                break;
            case 4:                                                                             // elemental dragons
                if (d > 120 && d < 900) move(m, ux * sp * dt, uy * sp * dt); meleeAtk(m, fl);
                if (now >= m.t1 && d < 650 && !sil) { m.t1 = now + (m.elem == 4 ? 1.1 : 2.2); eproj(m, Math.atan2(dy, dx), 380, m.ad * 0.9, 1, m.col, fl, 0); }
                if (m.elem == 1 && now >= m.t2) {                                               // Infernal: burning ground + anti-heal
                    m.t2 = now + 3.5; Zone z = nz(p.x, p.y, 85, 4.6, new Color(255, 90, 20, 120), false); z.start = now + 0.6; z.tick = m.ad * 0.25; z.flags = 2; z.src = m; zones.add(z); }
                if (m.elem == 2 && m.healCutT <= now) m.hp = Math.min(m.maxhp, m.hp + m.maxhp * 0.015 * dt); // Ocean: self-heal
                if (m.elem == 4 && now >= m.t2) {                                               // Cloud: tornado
                    m.t2 = now + 5; Zone z = nz(p.x, p.y, 90, 3, new Color(220, 240, 255, 110), false); z.start = now + 0.4; z.tick = m.ad * 0.15; z.every = 0.4; z.slow = 0.4; z.src = m; zones.add(z); }
                break;
            case 10: {                                                                          // Rift Herald: armored front, charges
                if (m.t2 > now) {
                    move(m, Math.cos(m.pa) * 500 * dt, Math.sin(m.pa) * 500 * dt);
                    if (d < m.r + p.r + 10 && now >= m.atkT) { m.atkT = now + 1; hurt(m.ad * 1.5, 0, m, 0); }
                } else {
                    double want = Math.atan2(dy, dx); m.fa += Math.max(-0.9 * dt, Math.min(0.9 * dt, angDiff(want, m.fa))); // slow turn rate = exploitable back
                    if (d > m.r + p.r + 6) move(m, ux * sp * dt, uy * sp * dt); meleeAtk(m, 0);
                    if (now >= m.t1) { m.t1 = now + 6; m.t2 = now + 1.2; m.pa = m.fa; ftext(m.x, m.y - 70, "CHARGE!", Color.ORANGE); }
                }
                break;
            }
            case 11: {                                                                          // Baron Nashor: stationary, acid rain, spikes, enrage
                if (m.hp < 0.3 * m.maxhp && !m.enraged) { m.enraged = true; m.ad *= 1.4; m.col = new Color(255, 40, 80); ftext(m.x, m.y - 90, "ENRAGED!", Color.RED); }
                double k = m.enraged ? 0.6 : 1;
                if (now >= m.t1) { m.t1 = now + 4 * k;                                          // acid rain
                    for (int i = 0; i < 3; i++) { Zone z = nz(p.x + (i == 0 ? 0 : rnd.nextGaussian() * 140), p.y + (i == 0 ? 0 : rnd.nextGaussian() * 140), 95, 3.5, new Color(110, 255, 60, 110), false);
                        z.start = now + 1.0; z.tick = m.ad * 0.25; z.src = m; zones.add(z); } }
                if (now >= m.t2) { m.t2 = now + 6.5 * k;                                        // spike knock-up
                    final Zone z = nz(p.x, p.y, 75, 1.0, new Color(150, 110, 60, 140), false); z.start = now + 0.9; final Mob b = m;
                    z.end = () -> { if (Math.hypot(p.x - z.x, p.y - z.y) < z.r + p.r) { hurt(b.ad * 1.2, 0, b, 0); stunPlayer(0.8); } }; zones.add(z); }
                if (now >= m.atkT && d < 700) { m.atkT = now + 3 * k; eproj(m, Math.atan2(dy, dx), 360, m.ad, 1, new Color(120, 255, 80), 0, 0); } // acid spit
                break;
            }
            case 12:                                                                            // Elder Dragon: executes player below 20% HP
                if (d > 130 && d < 1000) move(m, ux * sp * dt, uy * sp * dt); meleeAtk(m, 1);
                if (now >= m.t1 && d < 750) { m.t1 = now + 2.4; double a = Math.atan2(dy, dx); for (int i = -2; i <= 2; i++) eproj(m, a + i * 0.17, 400, m.ad * 0.8, 1, new Color(255, 120, 40), 1, 0); }
                if (now >= m.t2) { m.t2 = now + 4; Zone z = nz(p.x, p.y, 90, 4.6, new Color(255, 60, 20, 120), false); z.start = now + 0.6; z.tick = m.ad * 0.25; z.flags = 3; z.src = m; zones.add(z); }
                break;
        }
    }

    // ===================== PAINTING =====================
    void button(Graphics2D g, Rectangle r, String text, Color c, Runnable a) {                  // draw + register a clickable button
        boolean hov = r.contains(mx, my);
        g.setColor(hov ? c.brighter() : c); g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(Color.WHITE); g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 14)); FontMetrics fm = g.getFontMetrics();
        g.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 3);
        btns.add(new Object[]{r, a});
    }
    void txt(Graphics2D g, String s, int x, int y, int size, Color c) { g.setFont(new Font("SansSerif", Font.BOLD, size)); g.setColor(c); g.drawString(s, x, y); }
    void ctr(Graphics2D g, String s, int y, int size, Color c) { g.setFont(new Font("SansSerif", Font.BOLD, size)); g.setColor(c); g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y); }
    void bar(Graphics2D g, int x, int y, int w, int h, double f, Color c, String label) {        // generic bar with text
        g.setColor(new Color(0, 0, 0, 170)); g.fillRect(x, y, w, h); g.setColor(c); g.fillRect(x, y, (int) (w * Math.max(0, Math.min(1, f))), h);
        g.setColor(Color.WHITE); g.drawRect(x, y, w, h);
        if (label != null) { g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.drawString(label, x + w / 2 - g.getFontMetrics().stringWidth(label) / 2, y + h - 5); }
    }
    void poly(Graphics2D g, double x, double y, double r, int sides, double rot, Color c) {      // glowing polygon (monster shape)
        Path2D.Double pa = new Path2D.Double();
        for (int i = 0; i < sides; i++) { double a = rot + i * 2 * Math.PI / sides; double px = x + Math.cos(a) * r, py = y + Math.sin(a) * r; if (i == 0) pa.moveTo(px, py); else pa.lineTo(px, py); }
        pa.closePath();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70)); g.setStroke(new BasicStroke(9)); g.draw(pa);   // glow
        g.setColor(c); g.fill(pa); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2)); g.draw(pa);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0; g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 12, 16)); g.fillRect(0, 0, W, H); btns.clear();
        switch (state) {
            case S_CHAMP: drawChamp(g); break;
            case S_SPELL: drawSpell(g); break;
            case S_VILLAGE: drawVillage(g); break;
            default: drawWorld(g); drawHud(g); if (state != S_PLAY) drawEnd(g);
        }
    }

    void drawChamp(Graphics2D g) {                                                              // champion select screen
        ctr(g, "RIFT ROGUE", 90, 54, new Color(255, 215, 80)); ctr(g, "Choose your Champion  (click or press 1-4)", 130, 20, Color.LIGHT_GRAY);
        for (int i = 0; i < 4; i++) {
            final int c = i; Rectangle r = new Rectangle(60 + i * 300, 180, 270, 430);
            g.setColor(new Color(30, 30, 40)); g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            g.setColor(r.contains(mx, my) ? CH_COL[i] : Color.GRAY); g.setStroke(new BasicStroke(3)); g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            if (sprites[i] != null) g.drawImage(sprites[i], r.x + 60, r.y + 30, 150, 150, null);
            else { poly(g, r.x + 135, r.y + 105, 60, 6, 0, CH_COL[i]); }
            txt(g, (i + 1) + ". " + CH[i], r.x + 20, r.y + 230, 26, CH_COL[i]); txt(g, ROLE[i], r.x + 20, r.y + 256, 16, Color.LIGHT_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(Color.WHITE);
            drawWrapped(g, PASSIVE[i], r.x + 20, r.y + 285, 235);
            for (int s = 0; s < 4; s++) { g.setColor(SLOT_COL[s]); g.drawString((s + 1) + ": " + SK[i][s], r.x + 20, r.y + 345 + s * 20); }
            btns.add(new Object[]{r, (Runnable) () -> { champ = c; state = S_SPELL; }});
        }
    }
    void drawWrapped(Graphics2D g, String s, int x, int y, int w) {                             // simple word wrap
        FontMetrics fm = g.getFontMetrics(); String line = "";
        for (String word : s.split(" ")) { if (fm.stringWidth(line + word) > w) { g.drawString(line, x, y); y += 16; line = ""; } line += word + " "; }
        g.drawString(line, x, y);
    }
    void drawSpell(Graphics2D g) {                                                              // summoner spell selection
        ctr(g, CH[champ] + " - choose 2 Summoner Spells (keys 5 & 6)", 90, 30, CH_COL[champ]);
        int picked = 0; for (boolean b : sumPick) if (b) picked++;
        for (int i = 0; i < 7; i++) {
            final int k = i; Rectangle r = new Rectangle(190 + (i % 4) * 220, 170 + (i / 4) * 130, 200, 100);
            g.setColor(sumPick[i] ? new Color(60, 90, 40) : new Color(30, 30, 40)); g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            g.setColor(sumPick[i] ? Color.GREEN : Color.GRAY); g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            txt(g, (i + 1) + ". " + SUM[i], r.x + 14, r.y + 36, 20, Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(Color.LIGHT_GRAY); drawWrapped(g, SUM_DESC[i], r.x + 14, r.y + 62, 175);
            btns.add(new Object[]{r, (Runnable) () -> toggleSum(k)});
        }
        if (picked == 2) button(g, new Rectangle(540, 470, 200, 60), "START RUN (Enter)", new Color(40, 140, 60), this::startFromSelect);
        button(g, new Rectangle(40, 640, 120, 40), "< Back", new Color(70, 70, 80), () -> state = S_CHAMP);
    }
    void toggleSum(int k) { int picked = 0; for (boolean b : sumPick) if (b) picked++; if (sumPick[k]) sumPick[k] = false; else if (picked < 2) sumPick[k] = true; }
    void startFromSelect() { int n = 0; for (int i = 0; i < 7; i++) if (sumPick[i] && n < 2) sum[n++] = i; if (n == 2) newRun(); }

    void drawVillage(Graphics2D g) {                                                            // shop + blacksmith screen
        ctr(g, "VILLAGE", 50, 36, new Color(255, 215, 80));
        txt(g, "Gold: " + gold, 40, 50, 22, Color.YELLOW);
        txt(g, "ITEM SHOP (click to buy)", 40, 85, 18, Color.WHITE); txt(g, "BLACKSMITH FORGE (needs components + gold)", 500, 85, 18, Color.WHITE);
        for (int i = 0; i < BASEI.size(); i++) {
            final Item it = BASEI.get(i); Rectangle r = new Rectangle(40, 95 + i * 56, 420, 50);
            g.setColor(gold >= it.cost ? new Color(40, 60, 40) : new Color(40, 30, 30)); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(r.contains(mx, my) ? Color.WHITE : Color.GRAY); g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            txt(g, it.n + "  (" + it.cost + "g)", r.x + 10, r.y + 22, 15, Color.WHITE); txt(g, desc(it), r.x + 10, r.y + 41, 13, Color.LIGHT_GRAY);
            btns.add(new Object[]{r, (Runnable) () -> buy(it)});
        }
        for (int i = 0; i < LEG.size(); i++) {
            final Item it = LEG.get(i); Rectangle r = new Rectangle(500, 95 + i * 48, 530, 44); boolean ok = canForge(it);
            g.setColor(ok ? new Color(40, 80, 50) : new Color(35, 35, 45)); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(r.contains(mx, my) ? Color.WHITE : ok ? Color.GREEN : Color.GRAY); g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            txt(g, it.n + "  (" + it.cost + "g)  = " + String.join(" + ", it.rec), r.x + 10, r.y + 18, 13, Color.WHITE); txt(g, desc(it), r.x + 10, r.y + 36, 12, new Color(255, 215, 120));
            btns.add(new Object[]{r, (Runnable) () -> forge(it)});
        }
        txt(g, "INVENTORY (right-click to sell for 50%)", 40, 580, 16, Color.WHITE);
        for (int i = 0; i < 6; i++) {
            Rectangle r = new Rectangle(40 + i * 170, 590, 160, 50); g.setColor(new Color(30, 30, 40)); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Color.GRAY); g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            if (inv[i] != null) { g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(Color.WHITE); drawWrapped(g, inv[i].n, r.x + 8, r.y + 20, 145); }
        }
        txt(g, "Level " + level + "   Floor reached: " + floor, 1060, 120, 16, Color.LIGHT_GRAY);
        button(g, new Rectangle(1060, 600, 200, 70), "DIVE: Floor " + (floor + 1), new Color(40, 140, 60), this::diveFromVillage);
        if (System.nanoTime() / 1000000 % 1000 < 0) g.drawString("", 0, 0);
        if (now < msgT) ctr(g, msg, 700, 18, Color.ORANGE);
    }
    void diveFromVillage() {                                                                    // leave village: full heal, next floor
        floor++; recalc(); p.hp = maxHp; mana = maxMana; genFloor(); state = S_PLAY;
    }

    void drawWorld(Graphics2D g) {
        AffineTransform base = g.getTransform(); g.translate(-camX, -camY);                    // camera
        int x0 = Math.max(0, (int) camX / T), x1 = Math.min(MW - 1, (int) (camX + W) / T), y0 = Math.max(0, (int) camY / T), y1 = Math.min(MH - 1, (int) (camY + H) / T);
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) {                         // tiles
            if (!wall[x][y]) {
                g.setColor(new Color(44, 44, 52)); g.fillRect(x * T, y * T, T, T);              // dark grey floor
                g.setColor(new Color(62, 62, 74)); g.setStroke(new BasicStroke(1)); g.drawRect(x * T, y * T, T, T); // grid
                g.setColor(new Color(205, 205, 235)); g.setStroke(new BasicStroke(4));          // thick light wall border
                if (isWall(x - 1, y)) g.drawLine(x * T, y * T, x * T, y * T + T); if (isWall(x + 1, y)) g.drawLine(x * T + T, y * T, x * T + T, y * T + T);
                if (isWall(x, y - 1)) g.drawLine(x * T, y * T, x * T + T, y * T); if (isWall(x, y + 1)) g.drawLine(x * T, y * T + T, x * T + T, y * T + T);
            } else if (!isWall(x - 1, y) || !isWall(x + 1, y) || !isWall(x, y - 1) || !isWall(x, y + 1)) { g.setColor(new Color(26, 26, 36)); g.fillRect(x * T, y * T, T, T); }
        }
        if (cleared) {                                                                          // stairs
            g.setColor(new Color(255, 215, 60, 70)); g.fillOval((int) stairX - 40, (int) stairY - 40, 80, 80);
            g.setColor(new Color(255, 215, 60)); g.setStroke(new BasicStroke(3)); g.drawRect((int) stairX - 24, (int) stairY - 24, 48, 48); g.drawString("STAIRS", (int) stairX - 24, (int) stairY + 5);
        }
        for (Zone z : zones) {                                                                  // ground zones / telegraphs
            int al = now < z.start ? 45 : z.c.getAlpha();
            g.setColor(new Color(z.c.getRed(), z.c.getGreen(), z.c.getBlue(), al)); g.fill(new Ellipse2D.Double(z.x - z.r, z.y - z.r, z.r * 2, z.r * 2));
            g.setColor(new Color(z.c.getRed(), z.c.getGreen(), z.c.getBlue(), 230)); g.setStroke(new BasicStroke(2)); g.draw(new Ellipse2D.Double(z.x - z.r, z.y - z.r, z.r * 2, z.r * 2));
            if (z.kind == 1) { double f = Math.max(0, (z.until - now) / 0.7); g.setColor(new Color(255, 230, 120)); g.fillRect((int) z.x - 8, (int) (z.y - 20 - 500 * f), 16, 60); g.setColor(Color.WHITE); g.drawRect((int) z.x - 8, (int) (z.y - 20 - 500 * f), 16, 60); } // falling sword
        }
        if (spinT > now) {                                                                      // Garen's spinning blades
            for (int i = 0; i < 3; i++) { double a = now * 9 + i * 2.094; g.setColor(new Color(255, 215, 80)); g.fillOval((int) (p.x + Math.cos(a) * 110) - 10, (int) (p.y + Math.sin(a) * 110) - 10, 20, 20); }
        }
        for (Pickup k : pickups) { g.setColor(k.type == 0 ? Color.YELLOW : new Color(255, 150, 190)); g.fillOval((int) k.x - 7, (int) k.y - 7, 14, 14); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1)); g.drawOval((int) k.x - 7, (int) k.y - 7, 14, 14); }
        Mob hov = lockTarget(mx + camX, my + camY);
        for (Mob m : mobs) {                                                                    // monsters (fog-of-war aware)
            if (!visible(m)) continue;
            poly(g, m.x, m.y, m.r, m.sides, m.fa * (m.bush ? 0 : 1), m.stunT > now ? Color.WHITE : m.col);
            if (m.shield > 0) { g.setColor(new Color(200, 200, 255, 120)); g.fill(new Ellipse2D.Double(m.x - m.r - 4, m.y - m.r - 4, m.r * 2 + 8, m.r * 2 + 8)); }
            int bw = (int) (m.r * 2 + 10); g.setColor(Color.BLACK); g.fillRect((int) m.x - bw / 2, (int) (m.y - m.r - 14), bw, 6);
            g.setColor(m.bush ? Color.ORANGE : new Color(220, 50, 50)); g.fillRect((int) m.x - bw / 2, (int) (m.y - m.r - 14), (int) (bw * Math.max(0, m.hp / m.maxhp)), 6);
            if (m.boss == 10) { g.setColor(Color.YELLOW); g.setStroke(new BasicStroke(3)); g.drawLine((int) m.x, (int) m.y, (int) (m.x + Math.cos(m.fa) * m.r * 1.5), (int) (m.y + Math.sin(m.fa) * m.r * 1.5)); } // facing line
        }
        for (Proj q : projs) { double r = q.big ? 14 : q.r; g.setColor(new Color(q.c.getRed(), q.c.getGreen(), q.c.getBlue(), 90)); g.fill(new Ellipse2D.Double(q.x - r - 4, q.y - r - 4, 2 * r + 8, 2 * r + 8)); g.setColor(q.c); g.fill(new Ellipse2D.Double(q.x - r, q.y - r, 2 * r, 2 * r)); }
        for (Beam b : beams) { float a = (float) Math.max(0, (b.until - now) / b.life); g.setColor(new Color(b.c.getRed(), b.c.getGreen(), b.c.getBlue(), (int) (b.c.getAlpha() * a))); g.setStroke(new BasicStroke((float) b.w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(new Line2D.Double(b.x1, b.y1, b.x2, b.y2)); }
        drawPlayer(g);
        drawTelegraph(g);
        double wx = mx + camX, wy = my + camY;
        if (hov != null) {                                                                      // target lock reticle
            double rr = hov.r + 12 + Math.sin(now * 8) * 2; g.setColor(new Color(255, 255, 120)); g.setStroke(new BasicStroke(3));
            for (int i = 0; i < 4; i++) { double a = now * 2 + i * Math.PI / 2; g.draw(new Arc2D.Double(hov.x - rr, hov.y - rr, rr * 2, rr * 2, -Math.toDegrees(a) - 25, 50, Arc2D.OPEN)); }
        }
        g.setColor(new Color(255, 255, 255, 200)); g.setStroke(new BasicStroke(2));              // crosshair at cursor
        g.draw(new Ellipse2D.Double(wx - 9, wy - 9, 18, 18)); g.drawLine((int) wx - 14, (int) wy, (int) wx + 14, (int) wy); g.drawLine((int) wx, (int) wy - 14, (int) wx, (int) wy + 14);
        for (FText f : ftexts) { double up = (1 - (f.until - now)) * 30; g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.setColor(Color.BLACK); g.drawString(f.s, (int) f.x + 1, (int) (f.y - up) + 1); g.setColor(f.c); g.drawString(f.s, (int) f.x, (int) (f.y - up)); }
        g.setTransform(base);
    }

    void drawPlayer(Graphics2D g) {
        double ang = Math.atan2(my + camY - p.y, mx + camX - p.x);
        g.setColor(new Color(255, 255, 255, 60)); g.setStroke(new BasicStroke(2)); g.drawLine((int) p.x, (int) p.y, (int) (p.x + Math.cos(ang) * 120), (int) (p.y + Math.sin(ang) * 120)); // aim ray
        Composite oc = g.getComposite(); if (invisT > now) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        if (sprites[champ] != null) {                                                           // use sprite, flipped to face left/right
            int sz = 56; boolean left = Math.cos(ang) < 0; g.drawImage(sprites[champ], (int) p.x + (left ? sz / 2 : -sz / 2), (int) p.y - sz / 2, left ? -sz : sz, sz, null);
        } else {                                                                                // fallback: glowing shape
            g.setColor(new Color(CH_COL[champ].getRed(), CH_COL[champ].getGreen(), CH_COL[champ].getBlue(), 80)); g.fill(new Ellipse2D.Double(p.x - 24, p.y - 24, 48, 48));
            g.setColor(CH_COL[champ]); g.fill(new Ellipse2D.Double(p.x - 16, p.y - 16, 32, 32)); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(2)); g.draw(new Ellipse2D.Double(p.x - 16, p.y - 16, 32, 32));
            g.setStroke(new BasicStroke(5)); g.drawLine((int) p.x, (int) p.y, (int) (p.x + Math.cos(ang) * 28), (int) (p.y + Math.sin(ang) * 28)); // weapon pointer
        }
        g.setComposite(oc);
        if (shield > 0 && shT > now) { g.setColor(new Color(150, 200, 255, 130)); g.setStroke(new BasicStroke(3)); g.draw(new Ellipse2D.Double(p.x - 26, p.y - 26, 52, 52)); }
        if (p.stunT > now) { g.setColor(Color.YELLOW); g.drawString("STUNNED", (int) p.x - 28, (int) p.y - 36); }
    }

    void drawTelegraph(Graphics2D g) {                                                          // aim-mode range indicators
        if (aim < 0) return;
        int at, rg, sz;
        if (aim < 4) { at = AIM[champ][aim]; rg = RNG[champ][aim]; sz = SZ[champ][aim]; } else { int s = sum[aim - 4]; at = SUM_AIM[s]; rg = SUM_RNG[s]; sz = 30; }
        double wx = mx + camX, wy = my + camY, ang = Math.atan2(wy - p.y, wx - p.x), d = Math.hypot(wx - p.x, wy - p.y);
        Color c = new Color(SLOT_COL[aim].getRed(), SLOT_COL[aim].getGreen(), SLOT_COL[aim].getBlue(), 90);
        g.setColor(c); AffineTransform old = g.getTransform();
        switch (at) {
            case 0: g.translate(p.x, p.y); g.rotate(ang); g.fill(new Rectangle2D.Double(0, -sz / 2.0, rg, sz)); g.setTransform(old); break;       // line
            case 1: { double k = Math.min(d, rg); double cx = p.x + Math.cos(ang) * k, cy = p.y + Math.sin(ang) * k; g.fill(new Ellipse2D.Double(cx - sz, cy - sz, sz * 2, sz * 2)); g.draw(new Ellipse2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2)); break; } // circle at cursor
            case 2: g.fill(new Arc2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2, -Math.toDegrees(ang) - sz / 2.0, sz, Arc2D.PIE)); break;                  // cone
            case 3: { int rr = sz > 0 ? sz : 70; g.fill(new Ellipse2D.Double(p.x - rr, p.y - rr, rr * 2, rr * 2)); break; }                              // self ring
            default: g.setStroke(new BasicStroke(2)); g.draw(new Ellipse2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2));                                   // target range
        }
    }

    void drawHud(Graphics2D g) {
        // ----- top bar -----
        g.setColor(new Color(0, 0, 0, 170)); g.fillRect(0, 0, W, 40);
        g.setColor(Color.YELLOW); g.fillOval(14, 11, 18, 18); txt(g, String.format("%,d Gold", gold), 40, 28, 18, Color.YELLOW);
        ctr(g, "Floor " + floor + " - " + floorName(), 28, 20, Color.WHITE);
        txt(g, "Lv " + level + "  XP " + xp + "/" + (40 + level * 30), W - 220, 28, 16, Color.CYAN);
        // ----- boss bar -----
        for (Mob m : mobs) if (m.boss != 0 && !m.name.startsWith("Mini")) { bar(g, W / 2 - 250, 50, 500, 18, m.hp / m.maxhp, new Color(200, 40, 60), m.name + (m.elem > 0 ? " (" + new String[]{"Infernal", "Ocean", "Mountain", "Cloud"}[m.elem - 1] + ")" : "")); break; }
        // ----- bottom bar -----
        int sx = (W - 376) / 2, sy = H - 84;
        bar(g, sx, sy - 52, 376, 22, p.hp / maxHp, new Color(40, 190, 70), (int) p.hp + " / " + (int) maxHp);
        if (shield > 0 && shT > now) { g.setColor(new Color(200, 230, 255, 200)); g.fillRect(sx, sy - 56, (int) (376 * Math.min(1, shield / maxHp)), 4); }
        bar(g, sx, sy - 26, 376, 18, mana / maxMana, new Color(50, 110, 230), (int) mana + " / " + (int) maxMana);
        for (int i = 0; i < 6; i++) {
            final int slot = i; Rectangle r = new Rectangle(sx + i * 64, sy, 56, 56);
            g.setColor(new Color(20, 20, 28)); g.fillRoundRect(r.x, r.y, 56, 56, 10, 10);
            g.setColor(SLOT_COL[i]); g.fillRoundRect(r.x + 4, r.y + 4, 48, 48, 8, 8);
            g.setColor(i == aim ? Color.WHITE : Color.GRAY); g.setStroke(new BasicStroke(i == aim ? 4 : 2)); g.drawRoundRect(r.x, r.y, 56, 56, 10, 10);
            String nm = i < 4 ? SK[champ][i] : SUM[sum[i - 4]]; g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.setColor(Color.BLACK); g.drawString(nm.length() > 9 ? nm.substring(0, 9) : nm, r.x + 5, r.y + 32);
            txt(g, "" + (i + 1), r.x + 6, r.y + 15, 13, Color.WHITE);                           // hotkey label
            if (now < cds[i]) {                                                                 // translucent cooldown overlay
                double f = Math.min(1, (cds[i] - now) / Math.max(0.1, cdTot[i])); g.setColor(new Color(0, 0, 0, 175)); g.fillRoundRect(r.x + 2, r.y + 2, 52, (int) (52 * f), 8, 8);
                txt(g, String.format("%.1f", cds[i] - now), r.x + 14, r.y + 36, 14, Color.WHITE);
            }
            if (i < 4) {                                                                        // rank pips + level-up '+' button
                for (int k = 0; k < rank[i]; k++) { g.setColor(Color.YELLOW); g.fillRect(r.x + 4 + k * 12, r.y + 60, 9, 4); }
                if (canUp(i)) { Rectangle pb = new Rectangle(r.x + 36, r.y - 20, 20, 18); button(g, pb, "+", new Color(200, 160, 30), () -> { if (canUp(slot)) { rank[slot]++; points--; } }); }
            }
        }
        if (points > 0) txt(g, "Skill points: " + points, sx, sy - 62, 14, Color.YELLOW);
        // inventory strip
        for (int i = 0; i < 6; i++) { g.setColor(new Color(0, 0, 0, 150)); g.fillRect(sx + 390 + (i % 3) * 70, sy + (i / 3) * 28, 66, 24); if (inv[i] != null) { g.setFont(new Font("SansSerif", Font.PLAIN, 10)); g.setColor(Color.WHITE); g.drawString(inv[i].n.length() > 11 ? inv[i].n.substring(0, 11) : inv[i].n, sx + 393 + (i % 3) * 70, sy + 16 + (i / 3) * 28); } }
        // messages / prompts
        if (now < msgT) ctr(g, msg, 100, 22, Color.ORANGE);
        if (cleared && Math.hypot(p.x - stairX, p.y - stairY) < 60) ctr(g, floor >= 50 ? "" : "E: Next Floor     V: Return to Village", H - 160, 22, Color.GREEN);
        if (isCasting) { g.setColor(new Color(255, 255, 255, 120)); g.fillRect(sx, sy - 58, 8, 4); }
        if (showStats) drawStats(g);
        txt(g, "WASD move | LMB attack | 1-6 spells (double-tap = quick cast, tap+click = aim) | RMB cancel | C stats", 10, H - 8, 11, new Color(255, 255, 255, 120));
    }

    void drawStats(Graphics2D g) {                                                              // 'C' stats panel
        g.setColor(new Color(0, 0, 0, 215)); g.fillRoundRect(20, 60, 260, 290, 14, 14); g.setColor(Color.WHITE); g.drawRoundRect(20, 60, 260, 290, 14, 14);
        String[] l = {"AD: " + (int) sAD, "AP: " + (int) sAP, "Armor: " + (int) sArm, "Magic Resist: " + (int) sMR, String.format("Attack Speed: %.2f", sAS),
            "Move Speed: " + (int) sMS, "Crit: " + (int) sCrit + "%", "CDR: " + (int) sCDR + "%", "Lifesteal: " + (int) sLS + "%"};
        txt(g, CH[champ] + " - Stats", 36, 90, 18, CH_COL[champ]);
        for (int i = 0; i < l.length; i++) txt(g, l[i], 36, 125 + i * 24, 16, Color.WHITE);
    }

    void drawEnd(Graphics2D g) {                                                                // death / victory overlay
        g.setColor(new Color(0, 0, 0, 190)); g.fillRect(0, 0, W, H);
        if (state == S_WIN) { ctr(g, "VICTORY!", 300, 72, new Color(255, 215, 60)); ctr(g, "The Elder Dragon has fallen.", 360, 26, Color.WHITE); }
        else { ctr(g, "YOU DIED", 300, 72, new Color(220, 50, 50)); ctr(g, "Reached Floor " + floor + " - perma-death reset", 360, 26, Color.WHITE); }
        ctr(g, "Press ENTER to start a new run", 430, 22, Color.LIGHT_GRAY);
    }

    // ===================== INPUT =====================
    boolean clickBtn() { for (Object[] b : btns) if (((Rectangle) b[0]).contains(mx, my)) { ((Runnable) b[1]).run(); return true; } return false; }

    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode(); if (k >= 512 || keys[k]) return; keys[k] = true;                // ignore auto-repeat
        if (state == S_CHAMP && k >= KeyEvent.VK_1 && k <= KeyEvent.VK_4) { champ = k - KeyEvent.VK_1; state = S_SPELL; return; }
        if (state == S_SPELL) { if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_7) toggleSum(k - KeyEvent.VK_1); if (k == KeyEvent.VK_ENTER) startFromSelect(); return; }
        if ((state == S_DEAD || state == S_WIN) && k == KeyEvent.VK_ENTER) { state = S_CHAMP; return; }
        if (state == S_VILLAGE && k == KeyEvent.VK_ENTER) { diveFromVillage(); return; }
        if (state != S_PLAY) return;
        if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_6) tap(k - KeyEvent.VK_1);
        if (k == KeyEvent.VK_C) showStats = !showStats;
        if (k == KeyEvent.VK_ESCAPE) aim = -1;
        if (cleared && Math.hypot(p.x - stairX, p.y - stairY) < 60) {                           // stairs interaction
            if (k == KeyEvent.VK_E && floor < 50) { floor++; genFloor(); }
            if (k == KeyEvent.VK_V) state = S_VILLAGE;
        }
    }
    @Override public void keyReleased(KeyEvent e) { if (e.getKeyCode() < 512) keys[e.getKeyCode()] = false; }
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        mx = e.getX(); my = e.getY();
        if (state == S_PLAY) {
            if (SwingUtilities.isRightMouseButton(e)) { aim = -1; return; }                    // right click cancels aiming
            if (clickBtn()) return;                                                             // '+' skill buttons
            if (aim >= 0) { enqueue(aim, mx + camX, my + camY); aim = -1; } else hold = true;   // tap & click fires; otherwise auto-attack
        } else if (state == S_VILLAGE && SwingUtilities.isRightMouseButton(e)) {                // sell item
            for (int i = 0; i < 6; i++) if (new Rectangle(40 + i * 170, 590, 160, 50).contains(mx, my) && inv[i] != null) { gold += inv[i].cost / 2; inv[i] = null; recalc(); }
        } else clickBtn();
    }
    @Override public void mouseReleased(MouseEvent e) { hold = false; }
    @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }
    @Override public void mouseDragged(MouseEvent e) { mx = e.getX(); my = e.getY(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
