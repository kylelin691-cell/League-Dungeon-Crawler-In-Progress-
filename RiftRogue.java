import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * RIFT ROGUE - League-themed top-down action-roguelike (single file, Java
 * Swing).
 * Compile: javac RiftRogue.java Run: java RiftRogue (run from the folder that
 * contains /assets)
 */
public class RiftRogue extends JPanel
  implements KeyListener, MouseListener, MouseMotionListener
{
  // ===================== CONSTANTS =====================
  static final int W = 1280, H = 720, T = 40, MW = 50, MH = 50; // screen size,
                                                                // tile size
                                                                // map size
                                                                // (50x50)
  static final double ZOOM = 0.8; // camera zoom (smaller = see more of the map)
  static final int S_CHAMP = 0, S_SPELL = 1, S_PLAY = 2, S_VILLAGE = 3,
    S_DEAD = 4, S_WIN = 5; // game states
  static final int NCH = 8; // number of champions (4 start unlocked)

  // ===================== CHAMPION DATA (index 0-3 = starters, 4-7 = unlockable
  // with rubies) =====================
  static final String[] CH =
    {"Garen", "Ashe", "Lux", "Master Yi", "Akali", "Miss Fortune", "Ahri",
     "Malphite"};
  static final String[] ROLE =
    {"Tank", "Ranger", "Mage", "Assassin", "Assassin", "Ranger", "Mage",
     "Tank"};
  static final int[] PRICE = {0, 0, 0, 0, 40, 40, 40, 40}; // ruby price to
                                                           // unlock
  static final String[] PASSIVE =
    {"Perseverance: regen % max HP/s after 5s without damage",
     "Frost Shot: attacks slow. Attacks build Focus for Ranger's Focus",
     "Illumination: spells mark; your next attack detonates the mark",
     "Double Strike: every 4th attack hits twice",
     "Assassin's Mark: after a spell, your next attack deals bonus magic damage",
     "Love Tap: first hit on a new target deals bonus damage",
     "Essence Theft: every 9 spell hits heals you",
     "Granite Shield: a shield (10% max HP) that regrows after 10s without damage"};
  static final String[] SPRITE_FILES =
    {"garen", "ashe", "lux", "masteryi", "akali", "missfortune", "ahri",
     "malphite"};
  static final String[] ANIMS =
    {"idle", "run", "attack", "attack2", "spell1", "spell2", "spell3", "spell4",
     "death"}; // champion sheets
  static final int A_IDLE = 0, A_RUN = 1, A_ATK = 2, A_ATK2 = 3, A_SP = 4,
    A_DEATH = 8;
  static final String[] MOB_ANIMS = {"idle", "run", "attack", "cast", "death"}; // monster
                                                                                // sheets
                                                                                // (assets/mobs/<name>_<anim>.png)
  static final int CELL = 160; // frame size inside a sheet
  static final int SPR_PX = 104; // on-screen frame size (smaller = smaller
                                 // characters)
  static final double[] SPR_MUL = {1.0, 1.0, 1.3, 1.0, 1.0, 1.0, 1.1, 1.1}; // per-champion
                                                                            // size
                                                                            // tweak
  static final boolean PIXEL_ART = false; // true = nearest-neighbour scaling
  static final Color[] CH_COL =
    {new Color(255, 205, 60), new Color(110, 200, 255),
     new Color(255, 250, 150), new Color(90, 255, 130), new Color(190, 90, 255),
     new Color(255, 100, 100), new Color(255, 150, 220),
     new Color(160, 180, 200)};
  // base stats: HP, Mana, AD, AP, Armor, MR, MoveSpeed(px/s), AttackSpeed,
  // BasicRange (0 = melee)
  static final double[][] BASE =
    {{620, 300, 60, 0, 36, 32, 230, 0.70, 0},
     {540, 320, 58, 0, 24, 30, 225, 0.85, 520},
     {500, 480, 52, 40, 18, 30, 220, 0.65, 480},
     {560, 300, 66, 0, 30, 32, 245, 0.90, 0},
     {560, 320, 62, 20, 32, 32, 250, 0.95, 0},
     {540, 340, 58, 0, 24, 30, 225, 0.85, 500},
     {520, 420, 52, 45, 20, 30, 230, 0.68, 480},
     {700, 300, 58, 0, 45, 32, 215, 0.62, 0}};
  static final String[][] SK =
    {{"Decisive Strike", "Courage", "Judgment", "Demacian Justice"},
     {"Ranger's Focus", "Volley", "Hawkshot", "Enchanted Crystal Arrow"},
     {"Light Binding", "Prismatic Barrier", "Lucent Singularity",
      "Final Spark"},
     {"Alpha Strike", "Meditate", "Wuju Style", "Highlander"},
     {"Five Point Strike", "Twilight Shroud", "Shuriken Flip",
      "Perfect Execution"},
     {"Double Up", "Strut", "Make It Rain", "Bullet Time"},
     {"Orb of Deception", "Fox-Fire", "Charm", "Spirit Rush"},
     {"Seismic Shard", "Thunderclap", "Ground Slam", "Unstoppable Force"}};
  // aim type: 0 line, 1 circle at cursor, 2 cone, 3 self ring, 4 locked target
  static final int[][] AIM =
    {{3, 3, 3, 4}, {3, 2, 0, 0}, {0, 0, 1, 0}, {3, 3, 3, 3}, {2, 3, 0, 4},
     {4, 3, 1, 2}, {0, 3, 0, 1}, {4, 3, 3, 1}};
  static final int[][] RNG =
    {{0, 0, 0, 650}, {0, 450, 1800, 3000}, {900, 800, 650, 1300},
     {450, 0, 0, 0}, {450, 0, 600, 600}, {650, 0, 800, 700}, {880, 0, 900, 450},
     {625, 0, 0, 1000}};
  static final int[][] SZ =
    {{0, 0, 120, 0}, {0, 55, 0, 45}, {55, 55, 160, 100}, {450, 0, 0, 0},
     {75, 250, 40, 0}, {0, 0, 250, 80}, {70, 550, 55, 40}, {0, 120, 250, 300}};
  static final double[][] CD =
    {{8, 14, 9, 60}, {12, 8, 16, 50}, {9, 12, 10, 55}, {10, 16, 9, 70},
     {7, 16, 10, 50}, {7, 12, 10, 60}, {7, 12, 10, 55}, {8, 12, 10, 70}};
  static final int[][] MANA =
    {{30, 40, 40, 80}, {50, 40, 30, 100}, {50, 50, 60, 100}, {40, 50, 30, 100},
     {40, 60, 40, 100}, {40, 40, 50, 100}, {50, 40, 60, 100},
     {40, 30, 50, 100}};
  static final double[][] ANIM_T =
    {{0.45, 0.4, 3.0, 0.7}, {0.4, 0.5, 0.4, 0.7}, {0.4, 0.5, 0.5, 1.2},
     {0.5, 4.0, 0.4, 0.6}, {0.5, 0.5, 0.5, 0.5}, {0.4, 0.4, 0.5, 2.0},
     {0.5, 0.5, 0.5, 0.5}, {0.5, 0.5, 0.5, 0.7}};

  static boolean isLoop(int c, int s)
  {
    return (c == 0 && s == 2) || (c == 3 && s == 1) || (c == 5 && s == 3);
  } // channelled skills loop their animation

  static final String[][] DESC =
    {{"Gain 40% move speed for 3s and remove slows. Your next attack within 5s silences (1.5s) and deals bonus physical damage: 30+25/rank +50% AD.",
      "Gain a shield (70+45/rank) and 60% damage reduction for 2.5s.",
      "Spin for 3s, hitting nearby enemies every 0.35s for 10+10/rank +25% AD physical damage.",
      "Lock a target (range 650). A sword falls after 0.7s: true damage 100+120/rank +50% AD, +30% of the target's missing HP."},
     {"Needs 4 Focus stacks (built by attacking). For 5s gain attack speed and your attacks fire a flurry of 5 arrows.",
      "Fire 9 arrows in a cone: 15+18/rank +50% AD each, slowing 30%.",
      "Send your hawk flying in a line. It reveals the fog of war along its path for 5s.",
      "Launch a giant arrow across the map. Stun grows with distance (1s up to 3.5s). Explodes on the first enemy: 150+130/rank +AD."},
     {"Fire a bolt that roots up to 2 enemies for 1.5s: 50+45/rank +60% AP magic damage.",
      "Throw your wand. It shields you now (50+35/rank +40% AP) and again when it returns to you.",
      "Lob a light orb that slows enemies. Recast to detonate early, otherwise it detonates after 4s: 70+55/rank +70% AP.",
      "After a 1s delay, fire a huge beam: 250+200/rank +100% AP magic damage. Detonates Illumination."},
     {"Become untargetable and strike up to 4 nearby enemies, 40+45/rank +60% AD each, then reappear at the first target.",
      "Channel 4s: 60% damage reduction and heal 50+35/rank per second. You cannot move.",
      "For 6s your attacks deal bonus true damage: 15+12/rank.",
      "For 7s: +40% move speed, +50% attack speed, slow immunity. Takedowns extend the duration by 1.5s."},
     {"Throw kunai in a cone: 30+25/rank, +50% AP, +40% AD magic damage.",
      "Drop a smoke bomb (radius 250) for 4s. Enemies lose sight of you inside it. You gain 30% move speed.",
      "Throw a shuriken forward (50+40/rank +50% AP) and flip backward, away from the cursor.",
      "Dash to a locked target (range 600). Recast within 3s to dash again; damage grows with the target's missing HP."},
     {"Shoot the locked target for 20+25/rank +AD, then ricochet to a nearby enemy for the same damage.",
      "Gain 40% attack speed for 4s.",
      "Rain bullets over an area for 2s. Slows 40% and deals damage every 0.25s.",
      "Channel 2s (you cannot move): waves of bullets rake a cone toward your cursor."},
     {"Send an orb out for magic damage, then pull it back for true damage. 40+35/rank +35% AP each way.",
      "Release 3 fox-fires that seek the nearest enemies: 30+25/rank +30% AP each.",
      "Blow a kiss that charms the first enemy hit for 1.4s: 60+40/rank +50% AP.",
      "Dash toward the cursor and fire bolts at up to 3 enemies. Recast up to 2 more times within 10s."},
     {"Hurl a shard at the locked target: 60+40/rank +60% AP, slows 25% for 3s and steals some speed.",
      "For 5s gain +40 armor and your attacks deal splash damage around the target.",
      "Slam the ground (radius 250): 50+35/rank +40% AP. Slows enemies' movement and attack speed.",
      "Charge to the cursor (range 1000). Enemies there are knocked up 1.5s and take 150+100/rank +80% AP."}};
  static final Color[] SLOT_COL =
    {new Color(220, 70, 70), new Color(70, 160, 255), new Color(90, 220, 120),
     new Color(200, 90, 255), new Color(255, 200, 60), new Color(255, 140, 40)};
  static final String[] SUM =
    {"Flash", "Ignite", "Cleanse", "Sprint", "Barrier", "Heal", "Exhaust"};
  static final int[] SUM_AIM = {1, 4, 3, 3, 3, 3, 4};
  static final int[] SUM_RNG = {280, 600, 0, 0, 0, 0, 600};
  static final double[] SUM_CD = {20, 20, 25, 20, 25, 25, 25};
  static final String[] SUM_DESC =
    {"Instant dash", "Burn + 60% anti-heal", "Remove debuffs, +30% tenacity",
     "+40% move speed", "Damage shield", "Heal + speed burst",
     "30% slow, -40% damage"};

  // ===================== ITEMS =====================
  static final int AD = 0, AP = 1, AR = 2, HP = 3, AS = 4, CR = 5, LS = 6,
    MN = 7, CDR = 8, MP = 9;
  static final String[] STAT_LBL =
    {"AD", "AP", "Armor", "HP", "AS%", "Crit%", "LS%", "Mana", "CDR", "MPen"};

  static class Item
  {
    String n;
    int cost;
    double[] s;
    String[] rec;

    Item(String n, int c, double[] s, String... rec)
    {
      this.n = n;
      cost = c;
      this.s = s;
      this.rec = rec;
    }
  }

  static double[] st(double ad, double ap, double ar, double hp, double as,
                     double cr, double ls, double mn, double cdr, double mp)
  {
    return new double[] {ad, ap, ar, hp, as, cr, ls, mn, cdr, mp};
  }

  static final List<Item> BASEI = new ArrayList<>(), LEG = new ArrayList<>(),
    CHEST_ITEMS = new ArrayList<>();
  static
  {
    BASEI.add(new Item("Long Sword", 350, st(12, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    BASEI.add(new Item("Dagger", 300, st(0, 0, 0, 0, 15, 0, 0, 0, 0, 0)));
    BASEI.add(new Item("Cloth Armor", 300, st(0, 0, 20, 0, 0, 0, 0, 0, 0, 0)));
    BASEI
      .add(new Item("Ruby Crystal", 400, st(0, 0, 0, 150, 0, 0, 0, 0, 0, 0)));
    BASEI
      .add(new Item("Amplifying Tome", 435, st(0, 25, 0, 0, 0, 0, 0, 0, 0, 0)));
    BASEI.add(new Item("Pickaxe", 875, st(25, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    BASEI.add(new Item("BF Sword", 1300, st(40, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    BASEI.add(new Item("Needlessly Large Rod", 1250,
                       st(0, 60, 0, 0, 0, 0, 0, 0, 0, 0)));
    LEG.add(new Item("Infinity Edge", 625, st(70, 0, 0, 0, 0, 20, 0, 0, 0, 0),
                     "BF Sword", "Pickaxe"));
    LEG.add(new Item("Blade of the Ruined King", 700,
                     st(40, 0, 0, 0, 25, 0, 10, 0, 0, 0), "Long Sword",
                     "Dagger"));
    LEG.add(new Item("Bloodthirster", 650, st(55, 0, 0, 0, 0, 20, 15, 0, 0, 0),
                     "BF Sword", "Long Sword"));
    LEG.add(new Item("Rabadon's Deathcap", 800,
                     st(0, 120, 0, 0, 0, 0, 0, 0, 0, 0), "Needlessly Large Rod",
                     "Amplifying Tome"));
    LEG.add(new Item("Luden's Companion", 700,
                     st(0, 80, 0, 0, 0, 0, 0, 600, 10, 0),
                     "Needlessly Large Rod", "Ruby Crystal"));
    LEG.add(new Item("Shadowflame", 700, st(0, 120, 0, 0, 0, 0, 0, 0, 0, 12),
                     "Needlessly Large Rod", "Needlessly Large Rod"));
    LEG.add(new Item("Liandry's Torment", 700,
                     st(0, 90, 0, 300, 0, 0, 0, 0, 0, 0), "Amplifying Tome",
                     "Ruby Crystal"));
    LEG.add(new Item("Sunfire Aegis", 600, st(0, 0, 50, 500, 0, 0, 0, 0, 0, 0),
                     "Ruby Crystal", "Cloth Armor"));
    LEG.add(new Item("Thornmail", 600, st(0, 0, 70, 350, 0, 0, 0, 0, 0, 0),
                     "Cloth Armor", "Cloth Armor"));
    LEG.add(new Item("Warmog's Armor", 700, st(0, 0, 0, 1000, 0, 0, 0, 0, 0, 0),
                     "Ruby Crystal", "Ruby Crystal"));
    // small items found in chests (Dagger / Long Sword / Cloth Armor are also
    // shop components)
    CHEST_ITEMS.add(BASEI.get(1));
    CHEST_ITEMS.add(BASEI.get(0));
    CHEST_ITEMS.add(BASEI.get(2));
    CHEST_ITEMS
      .add(new Item("Short Sword", 200, st(8, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    CHEST_ITEMS
      .add(new Item("Small Ruby", 250, st(0, 0, 0, 90, 0, 0, 0, 0, 0, 0)));
  }

  // ===================== ENTITY CLASSES =====================
  static class Ent
  { // anything with a body + status effects
    double x, y, hp, maxhp, r;
    double slowT, slowA, stunT, rootT, silT, exhT, burnT, burnDps, healCutT,
      markT;
  }

  static class Mob extends Ent
  { // monsters, bosses, honeyfruit bushes
    String name = "";
    Color col = Color.RED;
    int sides = 4, ai = 0, boss = 0, elem = 0, state = 0, group = 0;
    double ad, armor, mr, spd, atkCd = 1.1, atkT, t1, t2, gold, xp, fa, pa,
      shield, burnNext;
    double hx, hy, aggro = 260, leash = 650, lastAtk = -9, lastCast = -9,
      asDownT, charmT;
    boolean enraged, bush, moving;
    int[][] homeFlow;
  }

  static class Proj
  { // projectile (player or enemy)
    double x, y, vx, vy, r, dmg, life, slow, slowD, stun, root, expl, dot, dist,
      maxRange, tickEvery, nextTick, mul = 1;
    int type, pierce = 1, flags, kind, retType = -1;
    boolean friendly, basic, mark, big, boom, returning, visual, distStun;
    Color c;
    Mob src, homing;
    Runnable onTick, onReturn;
    Set<Mob> hit = new HashSet<>();
  }

  static class Zone
  { // ground area effect
    double x, y, r, start, until, tick, every = 0.5, next, slow;
    int flags, kind;
    boolean friendly, follow, reveal;
    Color c;
    Runnable end;
    Mob src;
  }

  static class Timed
  {
    double t;
    Runnable r;

    Timed(double t, Runnable r)
    {
      this.t = t;
      this.r = r;
    }
  }

  static class Beam
  {
    double x1, y1, x2, y2, w, until, life;
    Color c;
  }

  static class FText
  {
    double x, y, until;
    String s;
    Color c;
  }

  static class Pickup
  {
    double x, y;
    int type, amt;

    Pickup(double x, double y, int t, int a)
    {
      this.x = x;
      this.y = y;
      type = t;
      amt = a;
    }
  }

  static class Chest
  {
    double x, y;
    boolean open;

    Chest(double x, double y)
    {
      this.x = x;
      this.y = y;
    }
  }

  static class Part
  {
    double x, y, vx, vy, born, life, size;
    Color c;
  } // particle

  static class Ring
  {
    double x, y, r0, r1, born, dur, a0, span;
    int w;
    Color c;
    boolean follow;
  } // expanding ring / arc effect

  static class Corpse
  {
    double x, y, born, r;
    String key;
    int dir, sides;
    Color c;
  }

  // ===================== GAME STATE =====================
  int state = S_CHAMP, champ = 0;
  int[] sum = {0, 1};
  boolean[] sumPick = new boolean[7];
  double now = 0;
  long last = System.nanoTime();
  Random rnd = new Random();
  Ent p = new Ent();
  int level = 1, xp = 0, points = 1, gold = 0, floor = 1, bestFloor = 0,
    rubies = 0, runRubies = 0;
  boolean[] unlocked = {true, true, true, true, false, false, false, false};
  int[] rank = new int[4];
  double[] cds = new double[6], cdTot = new double[6], lastTap = new double[6];
  int aim = -1;
  boolean isCasting = false, recastFree = false;
  double castEnd = 0, castLock = 0;
  ArrayDeque<double[]> queue = new ArrayDeque<>();
  Item[] inv = new Item[6];
  // buffs
  double shield, shT, spdT, spdA, drT, drA, qNextT, focusT, spinT, spinTick,
    meditT, wujuT, hlT, tenT, invisT, selfRootT, asT, asA, armT, thunderT,
    akPassT, invulnT;
  double atkReady, lastHurt = -99, healCutT, pBurnT, pBurnDps, pBurnAcc,
    sunTick;
  int atkCount, focusStacks, ahriHits, ahriRC;
  double lastFocusT, ahriRT, akRT, smokeT, smokeX, smokeY;
  Mob akRTarget, lastTarget;
  Zone luxOrb;
  double sAD, sAP, sArm, sMR, sAS, sMS, sCrit, sCDR, sLS, sMPen, maxHp, maxMana,
    mana;
  // world
  boolean[][] wall, door;
  List<Rectangle> rooms = new ArrayList<>();
  Rectangle vault;
  double stairX, stairY, camX, camY;
  boolean cleared;
  int lastBush = 0, nextGap = 3, theme, groupId;
  BufferedImage mapImg;
  List<Mob> mobs = new ArrayList<>();
  List<Proj> projs = new ArrayList<>();
  List<Zone> zones = new ArrayList<>();
  List<Timed> timed = new ArrayList<>();
  List<Beam> beams = new ArrayList<>();
  List<FText> ftexts = new ArrayList<>();
  List<Pickup> pickups = new ArrayList<>();
  List<Chest> chests = new ArrayList<>();
  List<Part> parts = new ArrayList<>();
  List<Ring> rings = new ArrayList<>();
  List<Corpse> corpses = new ArrayList<>();
  int[][] flow;
  int flowTX = -1, flowTY = -1;
  double flowT; // path-finding distance field toward the player
  // input
  boolean[] keys = new boolean[512];
  boolean hold, showStats;
  int mx, my;
  String msg = "";
  double msgT;
  List<Object[]> btns = new ArrayList<>();
  // art
  BufferedImage[][] sheets = new BufferedImage[NCH][ANIMS.length];
  Map<String, BufferedImage[]> mobSheets = new HashMap<>();
  Map<String, BufferedImage> tileCache = new HashMap<>();
  double clock, animStart, animEnd, animDur, deathStart;
  int animKind, faceDir = 2, lastMoveDir = 2;
  boolean isMoving, animLoop, atkAlt;

  double mwx()
  {
    return mx / ZOOM + camX;
  } // mouse position in world coordinates

  double mwy()
  {
    return my / ZOOM + camY;
  }

  // ===================== ASSET LOADING / SAVE =====================
  BufferedImage loadImg(String path)
  { // classpath first (src/assets), then the working folder
    try
    {
      java.io.InputStream in = RiftRogue.class.getResourceAsStream("/" + path);
      if (in != null)
      {
        BufferedImage b = ImageIO.read(in);
        in.close();
        return b;
      }
      File f = new File(path);
      if (f.exists())
        return ImageIO.read(f);
    }
    catch (Exception ex)
    {
      /* missing art is fine: shape fallback */ }
    return null;
  }

  void loadSprites()
  {
    for (int i = 0; i < NCH; i++)
      for (int a = 0; a < ANIMS.length; a++)
        sheets[i][a] =
          loadImg("assets/" + SPRITE_FILES[i] + "_" + ANIMS[a] + ".png");
  }

  BufferedImage[] mobSheet(String key)
  { // lazily load a monster's sheets
    if (!mobSheets.containsKey(key))
    {
      BufferedImage[] arr = new BufferedImage[MOB_ANIMS.length];
      for (int a = 0; a < arr.length; a++)
        arr[a] = loadImg("assets/mobs/" + key + "_" + MOB_ANIMS[a] + ".png");
      mobSheets.put(key, arr);
    }
    return mobSheets.get(key);
  }

  BufferedImage tileImg(String name)
  {
    if (!tileCache.containsKey(name))
      tileCache.put(name, loadImg("assets/tiles/" + name + ".png"));
    return tileCache.get(name);
  }

  void loadSave()
  { // rubies + unlocked champions persist between runs
    try
    {
      File f = new File("rift_save.properties");
      if (f.exists())
      {
        Properties pr = new Properties();
        FileInputStream in = new FileInputStream(f);
        pr.load(in);
        in.close();
        rubies = Integer.parseInt(pr.getProperty("rubies", "0"));
        for (int i = 4; i < NCH; i++)
          unlocked[i] = Boolean.parseBoolean(pr.getProperty("u" + i, "false"));
      }
    }
    catch (Exception ex)
    {
      /* ignore corrupt save */ }
  }

  void saveGame()
  {
    try
    {
      Properties pr = new Properties();
      pr.setProperty("rubies", "" + rubies);
      for (int i = 4; i < NCH; i++)
        pr.setProperty("u" + i, "" + unlocked[i]);
      FileOutputStream out = new FileOutputStream("rift_save.properties");
      pr.store(out, "Rift Rogue save");
      out.close();
    }
    catch (Exception ex)
    {
      /* ignore */ }
  }

  void addRubies(int n)
  {
    if (n <= 0)
      return;
    rubies += n;
    runRubies += n;
    saveGame();
    ftext(p.x, p.y - 60, "+" + n + " Ruby", new Color(255, 80, 120));
    say("+" + n + " Rubies!");
  }

  // ===================== RUN / FLOOR SETUP =====================
  void newRun()
  { // perma-death reset
    level = 1;
    xp = 0;
    points = 1;
    gold = 0;
    floor = 1;
    runRubies = 0;
    Arrays.fill(rank, 0);
    Arrays.fill(cds, 0);
    Arrays.fill(inv, null);
    shield =
      shT =
        spdT =
          spdA =
            drT =
              drA =
                qNextT =
                  focusT =
                    spinT =
                      meditT =
                        wujuT =
                          hlT =
                            tenT =
                              invisT =
                                selfRootT =
                                  asT = armT = thunderT = akPassT = invulnT = 0;
    healCutT = pBurnT = 0;
    focusStacks = 0;
    ahriHits = 0;
    ahriRC = 0;
    akRT = 0;
    smokeT = 0;
    lastHurt = -99;
    lastTarget = null;
    luxOrb = null;
    lastBush = 0;
    nextGap = 2 + rnd.nextInt(4);
    p.maxhp = 0;
    p.stunT = p.rootT = p.slowT = 0;
    recalc();
    p.hp = maxHp;
    mana = maxMana;
    genFloor();
    state = S_PLAY;
  }

  void genFloor()
  { // procedural 50x50 dungeon, fewer mobs, vault with doors + chests
    mobs.clear();
    projs.clear();
    zones.clear();
    timed.clear();
    beams.clear();
    pickups.clear();
    chests.clear();
    parts.clear();
    rings.clear();
    corpses.clear();
    queue.clear();
    isCasting = false;
    aim = -1;
    cleared = false;
    luxOrb = null;
    akRT = 0;
    ahriRC = 0;
    smokeT = 0;
    vault = null;
    groupId = 0;
    wall = new boolean[MW][MH];
    door = new boolean[MW][MH];
    for (boolean[] c : wall)
      Arrays.fill(c, true);
    rooms = new ArrayList<>();
    boolean bossFloor =
      floor == 20 || floor == 40 || floor == 45 || floor == 50;
    if (bossFloor)
      rooms.add(new Rectangle(10, 10, 30, 30));
    else
    {
      for (int tries = 0; tries < 400 && rooms.size() < 7; tries++)
      { // fewer, larger-spaced rooms
        int w = 7 + rnd.nextInt(6), h = 7 + rnd.nextInt(5),
          x = 2 + rnd.nextInt(MW - w - 4), y = 2 + rnd.nextInt(MH - h - 4);
        Rectangle r = new Rectangle(x, y, w, h);
        boolean ok = true;
        for (Rectangle o : rooms)
          if (r.intersects(new Rectangle(o.x - 4, o.y - 4, o.width + 8,
                                         o.height + 8)))
            ok = false;
        if (ok)
          rooms.add(r);
      }
    }
    for (Rectangle r : rooms)
      carve(r.x, r.y, r.width, r.height);
    for (int i = 1; i < rooms.size(); i++)
      connect(rooms.get(i - 1), rooms.get(i));
    if (!bossFloor && floor >= 2 && rnd.nextDouble() < 0.6)
      makeVault(); // treasure vault behind doors
    Rectangle r0 = rooms.get(0), rl = rooms.get(rooms.size() - 1);
    if (bossFloor)
    {
      p.x = 13 * T;
      p.y = 25 * T;
      stairX = 37 * T;
      stairY = 25 * T;
      String bn =
        floor == 20 ? "Dragon"
          : floor == 40 ? "Rift Herald"
            : floor == 45 ? "Baron Nashor" : "Elder Dragon";
      Mob b = mk(bn, 25 * T, 25 * T);
      b.group = ++groupId;
      mobs.add(b);
    }
    else
    {
      p.x = (r0.x + r0.width / 2.0) * T;
      p.y = (r0.y + r0.height / 2.0) * T;
      stairX = (rl.x + rl.width / 2.0) * T;
      stairY = (rl.y + rl.height / 2.0) * T;
      for (int i = 1; i < rooms.size(); i++)
      { // guard groups, not every room
        Rectangle r = rooms.get(i);
        if (i == rooms.size() - 1 || rnd.nextDouble() < 0.55)
          spawnGroup(r);
        if (floor >= 25 && rnd.nextDouble() < 0.25)
          spawnGroup(r);
      }
      if (floor > 20 && floor < 40 && floor % 5 == 0)
      {
        Mob d = mk("Mini Dragon", stairX, stairY - 70);
        d.group = ++groupId;
        mobs.add(d);
      }
      if (floor - lastBush >= nextGap)
      {
        mobs.add(mk("Honeyfruit Bush", (r0.x + 1.5) * T, (r0.y + 1.5) * T));
        lastBush = floor;
        nextGap = 2 + rnd.nextInt(4);
      }
      if (vault != null)
      { // guards + chests inside the vault
        groupId++;
        for (int k = 0; k < 2; k++)
        {
          Mob g =
            mk(pool()[rnd.nextInt(pool().length)],
               (vault.x + 1.5 + k * 2.5) * T, (vault.y + 2.5) * T);
          g.group = groupId;
          mobs.add(g);
        }
        chests.add(new Chest((vault.x + 1.0) * T, (vault.y + 0.9) * T));
        chests.add(new Chest((vault.x + vault.width - 1.0) * T,
                             (vault.y + 0.9) * T));
      }
      if (rooms.size() > 2 && rnd.nextDouble() < 0.3)
      {
        Rectangle r = rooms.get(1 + rnd.nextInt(rooms.size() - 1));
        chests.add(new Chest((r.x + r.width - 1.2) * T,
                             (r.y + r.height - 1.2) * T));
      }
    }
    renderMap();
    flowTX = -1;
    say("Floor " + floor + " - " + floorName());
  }

  void makeVault()
  { // 5x5 room attached to the dungeon by a corridor, sealed with doors
    for (int tries = 0; tries < 120; tries++)
    {
      int w = 5 + rnd.nextInt(2), h = 5, x = 3 + rnd.nextInt(MW - w - 6),
        y = 3 + rnd.nextInt(MH - h - 6);
      boolean clear = true;
      for (int i = x - 2; i < x + w + 2 && clear; i++)
        for (int j = y - 2; j < y + h + 2; j++)
          if (!wall[i][j])
          {
            clear = false;
            break;
          }
      if (!clear)
        continue;
      Rectangle r = new Rectangle(x, y, w, h);
      carve(x, y, w, h);
      Rectangle best = null;
      double bd = 1e9;
      for (Rectangle o : rooms)
      {
        double d =
          Math.hypot(o.getCenterX() - r.getCenterX(),
                     o.getCenterY() - r.getCenterY());
        if (d < bd)
        {
          bd = d;
          best = o;
        }
      }
      connect(best, r);
      vault = r;
      for (int i = r.x; i < r.x + r.width; i++)
      {
        setDoor(i, r.y - 1);
        setDoor(i, r.y + r.height);
      }
      for (int j = r.y; j < r.y + r.height; j++)
      {
        setDoor(r.x - 1, j);
        setDoor(r.x + r.width, j);
      }
      return;
    }
  }

  void setDoor(int x, int y)
  {
    if (x > 0 && y > 0 && x < MW - 1 && y < MH - 1 && !wall[x][y])
      door[x][y] = true;
  }

  void spawnGroup(Rectangle r)
  { // 1-3 mobs that guard a room together
    String[] pl = pool();
    String t = pl[rnd.nextInt(pl.length)];
    int n =
      t.equals("Wolf") ? 2 + rnd.nextInt(2)
        : t.contains("Golem") ? 1 : 1 + rnd.nextInt(2);
    double cx = (r.x + 2 + rnd.nextDouble() * (r.width - 4)) * T,
      cy = (r.y + 2 + rnd.nextDouble() * (r.height - 4)) * T;
    groupId++;
    for (int k = 0; k < n; k++)
    {
      double x = cx + rnd.nextGaussian() * 36, y = cy + rnd.nextGaussian() * 36;
      if (blocked(x, y, 22))
      {
        x = cx;
        y = cy;
      }
      Mob m = mk(t, x, y);
      m.group = groupId;
      mobs.add(m);
    }
  }

  String[] pool()
  {
    if (floor < 5)
      return new String[] {"Wolf"};
    if (floor < 10)
      return new String[] {"Wolf", "Brambleback"};
    if (floor < 15)
      return new String[] {"Gromp"};
    if (floor < 20)
      return new String[] {"Blue Golem", "Red Golem"};
    return new String[] {"Wolf", "Brambleback", "Gromp", "Blue Golem",
                         "Red Golem"};
  }

  Mob mk(String t, double x, double y)
  { // mob factory (stronger than before + aggro radius)
    Mob m = new Mob();
    m.name = t;
    m.x = x;
    m.y = y;
    m.hx = x;
    m.hy = y;
    double hp = 60, ad = 9, ar = 8, mr = 8, sp = 190, r = 13, xpv = 10, g = 6,
      ag = 250;
    Color c = new Color(255, 120, 60);
    int sides = 3, ai = 1;
    switch (t)
    {
      case "Wolf":
        break;
      case "Brambleback":
        hp = 180;
        ad = 14;
        ar = 20;
        mr = 15;
        sp = 110;
        r = 20;
        c = new Color(90, 220, 90);
        sides = 6;
        xpv = 18;
        g = 10;
        ai = 0;
        ag = 230;
        break;
      case "Gromp":
        hp = 230;
        ad = 16;
        ar = 15;
        mr = 15;
        sp = 95;
        r = 24;
        c = new Color(170, 230, 60);
        sides = 8;
        xpv = 24;
        g = 14;
        ai = 2;
        ag = 270;
        break;
      case "Blue Golem":
        hp = 380;
        ad = 22;
        ar = 30;
        mr = 30;
        sp = 85;
        r = 28;
        c = new Color(70, 140, 255);
        sides = 4;
        xpv = 35;
        g = 22;
        ai = 3;
        ag = 290;
        break;
      case "Red Golem":
        hp = 400;
        ad = 28;
        ar = 35;
        mr = 25;
        sp = 95;
        r = 28;
        c = new Color(255, 70, 70);
        sides = 4;
        xpv = 35;
        g = 22;
        ai = 0;
        ag = 290;
        break;
      case "Honeyfruit Bush":
        hp = 120;
        ad = 0;
        ar = 0;
        mr = 0;
        sp = 0;
        r = 18;
        c = new Color(255, 170, 40);
        sides = 12;
        ai = 9;
        m.bush = true;
        xpv = 0;
        g = 0;
        break;
      case "Mini Dragon":
      case "Dragon":
        hp = t.equals("Dragon") ? 1800 : 1000;
        ad = 40;
        ar = 40;
        mr = 40;
        sp = 100;
        r = 38;
        sides = 7;
        ai = 4;
        m.boss = 1;
        xpv = 150;
        g = 120;
        ag = 420;
        m.elem = 1 + rnd.nextInt(4);
        c =
          new Color[] {new Color(255, 90, 30), new Color(60, 140, 255),
                       new Color(170, 130, 80), new Color(210, 235, 255)}[m.elem
                         - 1];
        if (m.elem == 3)
        {
          ar *= 2;
          mr *= 2;
        }
        if (m.elem == 4)
          m.atkCd = 0.8;
        break;
      case "Rift Herald":
        hp = 3500;
        ad = 45;
        ar = 50;
        mr = 40;
        sp = 90;
        r = 46;
        c = new Color(180, 120, 255);
        sides = 5;
        ai = 10;
        m.boss = 10;
        xpv = 300;
        g = 250;
        break;
      case "Baron Nashor":
        hp = 5500;
        ad = 55;
        ar = 60;
        mr = 60;
        sp = 0;
        r = 62;
        c = new Color(200, 80, 220);
        sides = 9;
        ai = 11;
        m.boss = 11;
        xpv = 500;
        g = 400;
        break;
      default:
        hp = 6500;
        ad = 70;
        ar = 70;
        mr = 70;
        sp = 85;
        r = 52;
        c = new Color(255, 60, 20);
        sides = 8;
        ai = 12;
        m.boss = 12;
        xpv = 800;
        g = 800;
    }
    double hs = Math.pow(1.08, floor - 1),
      as = Math.pow(1.08, (floor - 1) * 0.85); // scaling formulas from the spec
    double hm = m.bush ? 1 : m.boss > 0 ? 1.25 : 1.7,
      am = m.bush ? 1 : m.boss > 0 ? 1.15 : 1.35; // monsters are tougher now
    m.maxhp = m.hp = hp * hs * hm;
    m.ad = ad * as * am;
    m.armor = ar + 1.5 * (floor - 1);
    m.mr = mr + 1.5 * (floor - 1);
    m.spd = sp;
    m.r = r;
    m.col = c;
    m.sides = sides;
    m.ai = ai;
    m.xp = xpv * (1 + floor * 0.04) * 0.6;
    m.gold = g * (1 + floor * 0.12);
    m.aggro = m.boss > 0 ? (m.boss == 11 ? 900 : 750) : ag;
    m.leash = m.boss > 0 ? 1e9 : 650;
    if (m.elem == 3)
      m.shield = m.maxhp * 0.3;
    return m;
  }

  String mobKey(Mob m)
  { // sprite file prefix for assets/mobs/
    switch (m.name)
    {
      case "Wolf":
        return "wolf";
      case "Brambleback":
        return "brambleback";
      case "Gromp":
        return "gromp";
      case "Blue Golem":
        return "bluegolem";
      case "Red Golem":
        return "redgolem";
      case "Honeyfruit Bush":
        return "bush";
      case "Rift Herald":
        return "herald";
      case "Baron Nashor":
        return "baron";
      case "Elder Dragon":
        return "elder";
      default:
        return "dragon_"
          + new String[] {"infernal", "ocean", "mountain", "cloud"}[Math
            .max(0, m.elem - 1)];
    }
  }

  // ===================== MAP RENDERING (pre-rendered once per floor)
  // =====================
  static Color mixc(Color a, Color b, double t)
  {
    return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                     (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                     (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
  }

  boolean wl(int x, int y)
  {
    return x < 0 || y < 0 || x >= MW || y >= MH || wall[x][y];
  }

  boolean nearFloor(int x, int y)
  {
    for (int i = -1; i <= 1; i++)
      for (int j = -1; j <= 1; j++)
        if (!wl(x + i, y + j))
          return true;
    return false;
  }

  void renderMap()
  {
    theme = floor < 20 ? 0 : floor < 40 ? 1 : 2; // forest / volcano / rift
    String[] tn = {"forest", "volcano", "rift"};
    Color[][] P =
      { // floorA, floorB, wall, wall top, edge, accent
       {new Color(52, 64, 54), new Color(46, 58, 48), new Color(24, 38, 30),
        new Color(70, 104, 76), new Color(190, 235, 190),
        new Color(80, 170, 90)},
       {new Color(66, 46, 44), new Color(58, 40, 40), new Color(36, 20, 20),
        new Color(120, 52, 40), new Color(255, 170, 100),
        new Color(255, 100, 20)},
       {new Color(48, 44, 72), new Color(42, 38, 64), new Color(26, 20, 46),
        new Color(92, 72, 140), new Color(210, 190, 255),
        new Color(170, 120, 255)}};
    Color[] pal = P[theme];
    mapImg = new BufferedImage(MW * T, MH * T, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = mapImg.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
    BufferedImage fImg = tileImg(tn[theme] + "_floor"),
      wImg = tileImg(tn[theme] + "_wall"); // optional art overrides
    Random r = new Random(floor * 7919L + 13);
    for (int x = 0; x < MW; x++)
      for (int y = 0; y < MH; y++)
      {
        int px = x * T, py = y * T;
        if (!wall[x][y])
        { // floor tile
          if (fImg != null)
          {
            g.drawImage(fImg, px, py, T, T, null);
            continue;
          }
          g.setColor(mixc(pal[0], pal[1], r.nextDouble()));
          g.fillRect(px, py, T, T);
          g.setColor(new Color(0, 0, 0, 45));
          g.drawRect(px, py, T - 1, T - 1); // stone seams
          g.setColor(new Color(255, 255, 255, 14));
          g.drawLine(px + 1, py + 1, px + T - 2, py + 1);
          g.drawLine(px + 1, py + 1, px + 1, py + T - 2);
          int dec = r.nextInt(9);
          Color a = pal[5];
          if (dec == 0)
          {
            if (theme == 0)
            {
              g.setColor(a);
              for (int k = 0; k < 3; k++)
              {
                int gx = px + 6 + r.nextInt(28), gy = py + 10 + r.nextInt(24);
                g.drawLine(gx, gy, gx - 2, gy - 6);
                g.drawLine(gx, gy, gx + 2, gy - 5);
              }
            } // grass tufts
            else if (theme == 1)
            {
              g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 170));
              g.setStroke(new BasicStroke(2));
              int gx = px + 4 + r.nextInt(12), gy = py + 4 + r.nextInt(12);
              g.drawLine(gx, gy, gx + 10, gy + 6);
              g.drawLine(gx + 10, gy + 6, gx + 8, gy + 18);
              g.setStroke(new BasicStroke(1));
            } // lava cracks
            else
            {
              g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 150));
              g.fillOval(px + 8 + r.nextInt(22), py + 8 + r.nextInt(22), 3, 3);
            } // rift sparkles
          }
          else if (dec == 1)
          {
            g.setColor(new Color(0, 0, 0, 40));
            g.fillOval(px + 10 + r.nextInt(14), py + 10 + r.nextInt(14), 8, 5);
          }
        }
        else if (nearFloor(x, y))
        { // wall tile visible next to a floor
          if (wImg != null)
          {
            g.drawImage(wImg, px, py, T, T, null);
            continue;
          }
          g.setColor(pal[2]);
          g.fillRect(px, py, T, T);
          g.setColor(new Color(0, 0, 0, 80)); // brick pattern
          for (int k = 1; k < 3; k++)
            g.drawLine(px, py + 8 + k * 11, px + T, py + 8 + k * 11);
          for (int row = 0; row < 3; row++)
          {
            int off = (row % 2) * 10;
            for (int k = 0; k < 2; k++)
              g.drawLine(px + off + 8 + k * 20, py + 8 + row * 11,
                         px + off + 8 + k * 20, py + 19 + row * 11);
          }
          g.setColor(pal[3]);
          g.fillRect(px, py, T, 8);
          g.setColor(new Color(255, 255, 255, 40));
          g.drawLine(px, py, px + T, py); // lit top face
        }
      }
    g.setColor(pal[4]);
    g.setStroke(new BasicStroke(3)); // bright wall-edge outline for contrast
    for (int x = 0; x < MW; x++)
      for (int y = 0; y < MH; y++)
        if (!wall[x][y])
        {
          int px = x * T, py = y * T;
          if (wl(x - 1, y))
            g.drawLine(px, py, px, py + T);
          if (wl(x + 1, y))
            g.drawLine(px + T, py, px + T, py + T);
          if (wl(x, y - 1))
            g.drawLine(px, py, px + T, py);
          if (wl(x, y + 1))
            g.drawLine(px, py + T, px + T, py + T);
        }
    g.dispose();
  }

  // ===================== SPRITES =====================
  void startAnim(int kind, double dur, boolean loop)
  { // start an attack / spell animation facing the cursor
    animKind = kind;
    animStart = clock;
    animDur = Math.max(0.1, dur);
    animEnd = clock + dur;
    animLoop = loop;
    faceDir = dirIdx(Math.atan2(mwy() - p.y, mwx() - p.x));
  }

  boolean drawSheet(Graphics2D g, int ch, int anim, int frame, int dir,
                    double fx, double fy, double size)
  {
    BufferedImage[] set = sheets[ch];
    BufferedImage s = set[anim];
    if (s == null && anim == A_ATK2)
      s = set[A_ATK];
    if (s == null && anim >= A_SP && anim < A_DEATH)
      s = set[A_ATK];
    if (s == null)
      s = set[A_IDLE];
    return drawImg(g, s, frame, dir, fx, fy, size * SPR_MUL[ch]);
  }

  boolean drawImg(Graphics2D g, BufferedImage s, int frame, int dir, double fx,
                  double fy, double d)
  { // feet sit at 68% of the cell height
    if (s == null)
      return false;
    int sx = Math.max(0, Math.min(7, frame)) * CELL, sy = dir * CELL;
    Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                       PIXEL_ART
                         ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                         : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(s, (int) (fx - d / 2), (int) (fy - 0.68 * d),
                (int) (fx + d / 2), (int) (fy + 0.32 * d), sx, sy, sx + CELL,
                sy + CELL, null);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old != null ? old
      : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    return true;
  }

  // ===================== STATS =====================
  boolean isWall(int x, int y)
  {
    return x < 0 || y < 0 || x >= MW || y >= MH || wall[x][y] || door[x][y];
  } // closed doors block like walls

  void recalc()
  { // rebuild derived stats from level + items + buffs
    double[] b = BASE[champ];
    int L = level - 1;
    double[] it = new double[10];
    for (Item i : inv)
      if (i != null)
        for (int k = 0; k < 10; k++)
          it[k] += i.s[k];
    maxHp = b[0] + L * 90 + it[HP];
    maxMana = b[1] + L * 35 + it[MN];
    sAD = b[2] + L * 3.2 + it[AD];
    sAP = b[3] + L * 4 + it[AP];
    if (has("Rabadon's Deathcap"))
      sAP *= 1.3;
    sArm = b[4] + L * 3.5 + it[AR] + (armT > now ? 40 : 0);
    sMR = b[5] + L * 1.25;
    sAS =
      b[7] * (1 + L * 0.02 + it[AS] / 100.0
        + (focusT > now ? 0.3 + 0.1 * rank[0] : 0) + (hlT > now ? 0.5 : 0)
        + (asT > now ? asA : 0));
    sMS = b[6] * (1 + (spdT > now ? spdA : 0) + (hlT > now ? 0.4 : 0));
    sCrit = it[CR];
    sCDR = Math.min(40, it[CDR]);
    sLS = it[LS];
    sMPen = it[MP];
    if (maxHp > p.maxhp && p.maxhp > 0)
      p.hp += maxHp - p.maxhp;
    p.maxhp = maxHp;
    p.hp = Math.min(p.hp, maxHp);
    mana = Math.min(mana, maxMana);
    p.r = 14;
  }

  // ===================== PATH-FINDING / MOVEMENT HELPERS =====================
  int[][] bfs(int tx, int ty)
  { // breadth-first distance field over walkable tiles (8-directional)
    int[][] d = new int[MW][MH];
    for (int[] row : d)
      Arrays.fill(row, -1);
    if (isWall(tx, ty))
      return d;
    ArrayDeque<int[]> q = new ArrayDeque<>();
    d[tx][ty] = 0;
    q.add(new int[] {tx, ty});
    while (!q.isEmpty())
    {
      int[] c = q.poll();
      for (int dx = -1; dx <= 1; dx++)
        for (int dy = -1; dy <= 1; dy++)
        {
          if (dx == 0 && dy == 0)
            continue;
          int nx = c[0] + dx, ny = c[1] + dy;
          if (isWall(nx, ny) || d[nx][ny] >= 0)
            continue;
          if (dx != 0 && dy != 0
            && (isWall(c[0] + dx, c[1]) || isWall(c[0], c[1] + dy)))
            continue; // no cutting corners
          d[nx][ny] = d[c[0]][c[1]] + 1;
          q.add(new int[] {nx, ny});
        }
    }
    return d;
  }

  double[] stepFrom(int[][] f, double x, double y)
  { // unit vector toward the neighbouring tile that is closest to the goal
    int tx = (int) Math.floor(x / T), ty = (int) Math.floor(y / T);
    if (tx < 0 || ty < 0 || tx >= MW || ty >= MH || f == null || f[tx][ty] < 0)
      return null;
    int best = f[tx][ty], bx = tx, by = ty;
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
      {
        if (dx == 0 && dy == 0)
          continue;
        int nx = tx + dx, ny = ty + dy;
        if (isWall(nx, ny) || f[nx][ny] < 0 || f[nx][ny] >= best)
          continue;
        if (dx != 0 && dy != 0 && (isWall(tx + dx, ty) || isWall(tx, ty + dy)))
          continue;
        best = f[nx][ny];
        bx = nx;
        by = ny;
      }
    if (bx == tx && by == ty)
      return null;
    double cx = (bx + 0.5) * T - x, cy = (by + 0.5) * T - y,
      l = Math.hypot(cx, cy) + 1e-6;
    return new double[] {cx / l, cy / l};
  }

  boolean los(double x1, double y1, double x2, double y2, double r)
  { // clear straight line (no walls) between two points
    double d = Math.hypot(x2 - x1, y2 - y1);
    int n = (int) (d / 12) + 1;
    for (int i = 1; i < n; i++)
    {
      double t = (double) i / n;
      if (blocked(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, r))
        return false;
    }
    return true;
  }

  double[] pathDir(Mob m)
  { // direction a chasing mob should walk: straight if visible, else follow the
    // path
    double dx = p.x - m.x, dy = p.y - m.y, d = Math.hypot(dx, dy) + 1e-6;
    if (d < 70 || los(m.x, m.y, p.x, p.y, Math.min(m.r, 16) * 0.8))
      return new double[] {dx / d, dy / d};
    double[] v = stepFrom(flow, m.x, m.y);
    return v != null ? v : new double[] {dx / d, dy / d};
  }

  void wake(Mob m)
  { // aggro this mob and its pack-mates
    m.state = 1;
    for (Mob o : mobs)
      if (o != m && o.group == m.group && o.state == 0
        && Math.hypot(o.x - m.x, o.y - m.y) < 380)
        o.state = 1;
  }

  void startReturn(Mob m)
  {
    m.state = 2;
    m.homeFlow = bfs((int) (m.hx / T), (int) (m.hy / T));
  }

  void unstick()
  { // push the player out of a wall if they ended up inside one
    for (double rad = 4; rad < 240; rad += 4)
      for (int k = 0; k < 16; k++)
      {
        double nx = p.x + Math.cos(k * Math.PI / 8) * rad,
          ny = p.y + Math.sin(k * Math.PI / 8) * rad;
        if (!blocked(nx, ny, p.r))
        {
          p.x = nx;
          p.y = ny;
          return;
        }
      }
  }

  boolean setPosSafe(double x, double y)
  { // teleport next to (x,y) on a free spot, never inside walls
    for (double rad = 0; rad <= 70; rad += 14)
      for (int k = 0; k < 8; k++)
      {
        double nx = x + Math.cos(k * Math.PI / 4) * rad,
          ny = y + Math.sin(k * Math.PI / 4) * rad;
        if (!blocked(nx, ny, p.r))
        {
          p.x = nx;
          p.y = ny;
          return true;
        }
      }
    return false;
  }

  void dashTo(double ang, double dist, Color c)
  { // move the player along a line, stopping at walls
    double ox = p.x, oy = p.y;
    for (double k = 8; k <= dist; k += 8)
    {
      double nx = ox + Math.cos(ang) * k, ny = oy + Math.sin(ang) * k;
      if (blocked(nx, ny, p.r))
        break;
      p.x = nx;
      p.y = ny;
    }
    beam(ox, oy, p.x, p.y, 12, 0.3, c);
  }

  int coneHit(double ang, double range, double deg, double dmg, int type)
  { // instant cone damage in front of the player
    int n = 0;
    for (Mob m : new ArrayList<>(mobs))
    {
      double dx = m.x - p.x, dy = m.y - p.y, d = Math.hypot(dx, dy);
      if (d < range + m.r
        && Math.abs(angDiff(Math.atan2(dy, dx), ang)) < Math.toRadians(deg / 2))
      {
        hitMob(m, dmg, type, false);
        mark(m);
        n++;
      }
    }
    return n;
  }

  // ===================== VISUAL EFFECT HELPERS =====================
  void burst(double x, double y, int n, Color c, double spd, double size)
  { // particle burst
    for (int i = 0; i < n && parts.size() < 900; i++)
    {
      Part q = new Part();
      double a = rnd.nextDouble() * Math.PI * 2,
        s = spd * (0.3 + rnd.nextDouble());
      q.x = x;
      q.y = y;
      q.vx = Math.cos(a) * s;
      q.vy = Math.sin(a) * s;
      q.born = now;
      q.life = 0.35 + rnd.nextDouble() * 0.4;
      q.size = size * (0.6 + rnd.nextDouble() * 0.8);
      q.c = c;
      parts.add(q);
    }
  }

  void ring(double x, double y, double r0, double r1, double dur, Color c,
            int w)
  {
    arc(x, y, r0, r1, 0, Math.PI * 2, dur, c, w);
  }

  void arc(double x, double y, double r0, double r1, double a0, double span,
           double dur, Color c, int w)
  { // expanding ring or arc (slash)
    Ring r = new Ring();
    r.x = x;
    r.y = y;
    r.r0 = r0;
    r.r1 = r1;
    r.a0 = a0;
    r.span = span;
    r.born = now;
    r.dur = dur;
    r.c = c;
    r.w = w;
    rings.add(r);
  }

  void essence()
  {
    ahriHits++;
    if (ahriHits >= 9)
    {
      ahriHits = 0;
      heal(maxHp * 0.05 + 20);
      ring(p.x, p.y, 10, 60, 0.5, new Color(255, 150, 220), 4);
      ftext(p.x, p.y - 50, "Essence Theft", new Color(255, 170, 230));
    }
  }

  void loveTap(Mob m)
  {
    if (lastTarget != m)
    {
      lastTarget = m;
      hitMob(m, 0.45 * sAD + level * 2, 0, true);
    }
  }

  // ===================== DAMAGE =====================
  void hitMob(Mob m, double raw, int type, boolean crit)
  { // type: 0 physical, 1 magic, 2 true
    if (m.hp <= 0 || !mobs.contains(m))
      return;
    double d = raw;
    if (type == 0)
      d = raw * 100 / (100 + m.armor);
    else if (type == 1)
    {
      d = raw * 100 / (100 + Math.max(0, m.mr - sMPen));
      if (has("Shadowflame") && m.hp < 0.35 * m.maxhp)
      {
        d *= 1.25;
        crit = true;
      }
      if (has("Liandry's Torment"))
      {
        m.burnT = now + 3;
        m.burnDps = Math.max(m.burnDps, 0.02 * m.maxhp);
      }
    }
    if (m.boss == 10)
    { // Rift Herald: armored front, weak back
      double dot = Math.cos(m.fa) * (p.x - m.x) + Math.sin(m.fa) * (p.y - m.y);
      if (dot > 0)
        d *= 0.35;
      else
      {
        d *= 1.6;
        crit = true;
      }
    }
    if (m.shield > 0)
    {
      double a = Math.min(m.shield, d);
      m.shield -= a;
      d -= a;
    }
    m.hp -= d;
    Color dc =
      crit ? Color.YELLOW
        : type == 0 ? new Color(255, 80, 80)
          : type == 1 ? new Color(190, 90, 255) : Color.WHITE;
    ftext(m.x, m.y - m.r, "" + Math.round(d), dc);
    burst(m.x, m.y, 3, dc, 120, 3);
    if (m.hp <= 0)
      kill(m);
  }

  void hurt(double raw, int type, Mob src, int fl)
  { // damage to the player; fl: 1 = execute, 2 = anti-heal
    if (state != S_PLAY || p.hp <= 0 || invulnT > now)
      return;
    double d = raw;
    if (type == 0)
      d = d * 100 / (100 + sArm);
    else if (type == 1)
      d = d * 100 / (100 + sMR);
    if (drT > now)
      d *= 1 - drA;
    if (src != null && src.exhT > now)
      d *= 0.6;
    if (shield > 0 && shT > now)
    {
      double a = Math.min(shield, d);
      shield -= a;
      d -= a;
    }
    lastHurt = now;
    p.hp -= d;
    if ((fl & 2) != 0)
      healCutT = now + 3;
    if (src != null && has("Thornmail"))
    {
      src.healCutT = now + 3;
      hitMob(src, d * 0.3 + 10, 2, false);
    }
    if (d > 0.5)
    {
      ftext(p.x, p.y - 30, "" + Math.round(d), new Color(255, 130, 130));
      burst(p.x, p.y, 3, new Color(255, 80, 80), 100, 3);
    }
    if ((fl & 1) != 0 && p.hp < 0.2 * maxHp)
      p.hp = 0;
    if (p.hp <= 0)
    {
      p.hp = 0;
      state = S_DEAD;
      deathStart = clock;
      bestFloor = Math.max(bestFloor, floor);
    }
  }

  void kill(Mob m)
  {
    mobs.remove(m);
    Corpse c = new Corpse();
    c.x = m.x;
    c.y = m.y;
    c.born = now;
    c.r = m.r;
    c.key = mobKey(m);
    c.dir = dirIdx(m.fa);
    c.sides = m.sides;
    c.c = m.col;
    corpses.add(c);
    burst(m.x, m.y, 16, m.col, 170, 5);
    if (m.bush)
    {
      for (int i = 0; i < 5; i++)
        pickups.add(new Pickup(m.x + rnd.nextGaussian() * 30,
                               m.y + rnd.nextGaussian() * 30, 1, 0));
      return;
    }
    pickups.add(new Pickup(m.x, m.y, 0, (int) m.gold));
    addXp((int) m.xp);
    if (hlT > now)
      hlT = Math.min(now + 10, hlT + 1.5);
    if (m.boss == 1)
      addRubies(m.name.startsWith("Mini") ? 2 : 5); // ruby rewards for bosses
    else if (m.boss == 10)
      addRubies(7);
    else if (m.boss == 11)
      addRubies(10);
    else if (m.boss == 12)
    {
      addRubies(12);
      state = S_WIN;
    }
  }

  void addXp(int a)
  { // slower levelling than before
    xp += a;
    while (level < 18 && xp >= 60 + level * 45)
    {
      xp -= 60 + level * 45;
      level++;
      points++;
      recalc();
      p.hp = Math.min(maxHp, p.hp + maxHp * 0.05);
      say("LEVEL UP! Spend your skill point (+)");
      ring(p.x, p.y, 10, 90, 0.7, Color.YELLOW, 4);
    }
  }

  // ===================== BASIC ATTACK =====================
  void basicAttack()
  {
    if (now < atkReady || p.stunT > now || isCasting)
      return;
    atkReady = now + 1.0 / sAS;
    double ang = Math.atan2(mwy() - p.y, mwx() - p.x);
    atkAlt = !atkAlt;
    startAnim(atkAlt ? A_ATK : A_ATK2, Math.min(0.5, 1.0 / sAS), false);
    if (BASE[champ][8] == 0)
    { // melee: 120-degree arc
      for (Mob m : new ArrayList<>(mobs))
      {
        double dx = m.x - p.x, dy = m.y - p.y;
        if (Math.hypot(dx, dy) < 95 + m.r
          && Math.abs(angDiff(Math.atan2(dy, dx), ang)) < 1.1)
          onHit(m, 1);
      }
      arc(p.x, p.y, 40, 70, ang - 0.9, 1.8, 0.18, CH_COL[champ], 6);
    }
    else
    { // ranged: arrow / orb / bullet
      boolean flurry = champ == 1 && focusT > now;
      int n = flurry ? 5 : 1;
      for (int i = 0; i < n; i++)
      {
        Proj q =
          pj(ang + (i - (n - 1) / 2.0) * 0.1, champ == 1 ? 900 : 750,
             BASE[champ][8], 7, sAD, 0, CH_COL[champ]);
        q.basic = true;
        q.kind = champ == 1 ? 2 : champ == 5 ? 3 : 0;
        if (flurry)
          q.mul = 0.5;
      }
    }
  }

  void onHit(Mob m, double mul)
  { // on-hit effects + passives
    boolean crit = rnd.nextDouble() * 100 < sCrit;
    double dmg =
      sAD * mul * (crit ? 1.75 + (has("Infinity Edge") ? 0.4 : 0) : 1);
    if (champ == 0 && qNextT > now && mul == 1)
    {
      qNextT = 0;
      dmg += 30 + 25 * rank[0] + sAD * 0.5;
      m.silT = now + 1.5;
      ftext(m.x, m.y - 40, "SILENCED", Color.WHITE);
      ring(m.x, m.y, 10, 50, 0.3, Color.WHITE, 3);
    }
    hitMob(m, dmg, 0, crit);
    heal(dmg * sLS / 100);
    if (has("Blade of the Ruined King"))
      hitMob(m, Math.max(8, m.hp * 0.04), 0, false);
    if (champ == 1)
    {
      m.slowT = now + 1.5;
      m.slowA = 0.3;
      if (mul == 1)
      {
        focusStacks = Math.min(4, focusStacks + 1);
        lastFocusT = now;
      }
    }
    if (champ == 2 && m.markT > now)
    {
      m.markT = 0;
      hitMob(m, 20 + level * 8 + sAP * 0.35, 1, false);
      ring(m.x, m.y, 8, 40, 0.3, new Color(255, 250, 150), 3);
    }
    if (champ == 3)
    {
      atkCount++;
      if (atkCount % 4 == 0)
        hitMob(m, sAD * 0.6, 0, false);
      if (wujuT > now)
        hitMob(m, 15 + 12 * rank[2], 2, false);
    }
    if (champ == 4 && akPassT > now)
    {
      akPassT = 0;
      hitMob(m, 25 + level * 5 + 0.4 * sAP, 1, false);
      ring(m.x, m.y, 8, 44, 0.3, new Color(190, 90, 255), 3);
    }
    if (champ == 5)
      loveTap(m);
    if (champ == 7 && thunderT > now)
      for (Mob o : new ArrayList<>(mobs))
        if (o != m && Math.hypot(o.x - m.x, o.y - m.y) < 140)
          hitMob(o, 0.4 * sAD + 0.3 * sAP, 1, false);
  }

  // ===================== INTERACTION (chests, doors) =====================
  void openChest(Chest c)
  {
    c.open = true;
    int g = (int) ((40 + rnd.nextInt(60)) * (1 + floor * 0.15));
    gold += g;
    ftext(c.x, c.y - 24, "+" + g + "g", Color.YELLOW);
    burst(c.x, c.y, 18, new Color(255, 220, 90), 150, 4);
    if (rnd.nextDouble() < 0.7)
    { // small item
      Item it = CHEST_ITEMS.get(rnd.nextInt(CHEST_ITEMS.size()));
      boolean placed = false;
      for (int i = 0; i < 6; i++)
        if (inv[i] == null)
        {
          inv[i] = it;
          placed = true;
          recalc();
          break;
        }
      if (placed)
        say("Chest: " + it.n + " + " + g + " gold");
      else
      {
        gold += it.cost / 2;
        say("Inventory full - sold " + it.n);
      }
    }
    else
      say("Chest: " + g + " gold");
    if (rnd.nextDouble() < 0.35)
      addRubies(rnd.nextDouble() < 0.1 ? 2 : 1); // rubies for unlocking new
                                                 // champions
  }

  boolean nearDoor()
  {
    for (int x = 0; x < MW; x++)
      for (int y = 0; y < MH; y++)
        if (door[x][y]
          && Math.hypot((x + 0.5) * T - p.x, (y + 0.5) * T - p.y) < 90)
          return true;
    return false;
  }

  boolean interact()
  { // E key: chest > door > stairs handled by caller
    for (Chest c : chests)
      if (!c.open && Math.hypot(c.x - p.x, c.y - p.y) < 70)
      {
        openChest(c);
        return true;
      }
    if (nearDoor())
    {
      for (int x = 0; x < MW; x++)
        for (int y = 0; y < MH; y++)
          if (door[x][y])
          {
            door[x][y] = false;
            burst((x + 0.5) * T, (y + 0.5) * T, 6, new Color(180, 130, 70), 90,
                  4);
          }
      say("The vault door swings open!");
      flowTX = -1;
      return true;
    }
    return false;
  }

  // ===================== SPELL CASTING =====================
  boolean recastable(int s)
  { // skills that can be pressed again without cooldown/mana
    if (champ == 2 && s == 2)
      return luxOrb != null && zones.contains(luxOrb);
    if (champ == 4 && s == 3)
      return akRT > now && akRTarget != null && mobs.contains(akRTarget);
    if (champ == 6 && s == 3)
      return ahriRC > 0 && now < ahriRT;
    return false;
  }

  boolean cast(int s, double tx, double ty)
  {
    boolean rc = s < 4 && recastable(s);
    if (!rc)
    {
      if (now < cds[s])
      {
        say("On cooldown");
        return false;
      }
      if (s < 4)
      {
        if (rank[s] == 0)
        {
          say(SK[champ][s] + " not learned - click the + button");
          return false;
        }
        if (mana < MANA[champ][s])
        {
          say("Not enough mana");
          return false;
        }
      }
    }
    recastFree = false;
    boolean ok =
      s < 4 ? castSkill(s, tx, ty) : castSummoner(sum[s - 4], tx, ty);
    if (!ok)
      return false;
    if (s < 4)
    {
      startAnim(A_SP + s, ANIM_T[champ][s], isLoop(champ, s)); // each skill
                                                               // plays its own
                                                               // animation
      if (champ == 4)
        akPassT = now + 4; // Akali passive window
      if (!recastFree)
      {
        mana -= MANA[champ][s];
        double cd = CD[champ][s] * (1 - sCDR / 100);
        cds[s] = now + cd;
        cdTot[s] = cd;
        if (has("Luden's Companion"))
          for (Mob m : new ArrayList<>(mobs))
            if (dist(m) < 220)
              hitMob(m, 30 + 0.12 * sAP, 1, false);
      }
    }
    else
    {
      double cd = SUM_CD[sum[s - 4]];
      cds[s] = now + cd;
      cdTot[s] = cd;
    }
    return true;
  }

  boolean castSkill(int s, double tx, double ty)
  {
    int rk = rank[s];
    double ang = Math.atan2(ty - p.y, tx - p.x),
      cdl = Math.hypot(tx - p.x, ty - p.y);
    final Color neon = new Color(60, 255, 200), ice = new Color(170, 235, 255),
      gold = new Color(255, 225, 110);
    switch (champ * 10 + s)
    {
      // ---------- GAREN ----------
      case 0:
        spdT = now + 3;
        spdA = 0.4;
        p.slowT = 0;
        qNextT = now + 5;
        ring(p.x, p.y, 10, 60, 0.4, gold, 4);
        say("Decisive Strike ready");
        return true;
      case 1:
        addShield(70 + 45 * rk, 2.5);
        drT = now + 2.5;
        drA = 0.6;
        ring(p.x, p.y, 10, 56, 0.5, new Color(150, 200, 255), 5);
        return true;
      case 2:
        spinT = now + 3;
        spinTick = now;
        return true;
      case 3:
      {
        final Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > RNG[0][3] + t.r)
        {
          say("No target locked in range");
          return false;
        }
        Zone z = nz(t.x, t.y, 70, 0.7, new Color(255, 215, 0, 140), true);
        z.kind = 1;
        final int r = rk;
        z.end = () -> {
          if (mobs.contains(t))
          {
            hitMob(t, 100 + 120 * r + 0.3 * (t.maxhp - t.hp) + sAD * 0.5, 2,
                   false);
            ring(t.x, t.y, 10, 140, 0.5, gold, 6);
            burst(t.x, t.y, 26, gold, 230, 5);
          }
        };
        zones.add(z);
        return true;
      }
      // ---------- ASHE ----------
      case 10:
        if (focusStacks < 4)
        {
          say("Need 4 Focus stacks (attack to build them): " + focusStacks
            + "/4");
          return false;
        }
        focusStacks = 0;
        focusT = now + 5;
        ring(p.x, p.y, 10, 60, 0.5, ice, 4);
        say("Ranger's Focus!");
        return true;
      case 11:
        for (int i = 0; i < 9; i++)
        {
          double a = ang + Math.toRadians(-SZ[1][1] / 2.0 + SZ[1][1] * i / 8.0);
          Proj q = pj(a, 850, RNG[1][1], 8, 15 + 18 * rk + sAD * 0.5, 0, ice);
          q.slow = 0.3;
          q.slowD = 2;
          q.kind = 2;
        }
        return true;
      case 12:
      { // hawk flies out and reveals the fog along its path
        final Proj q =
          pj(ang, 700, RNG[1][2], 14, 0, 1, new Color(230, 200, 140));
        q.visual = true;
        q.kind = 8;
        q.tickEvery = 0.12;
        q.onTick = () -> {
          Zone z = nz(q.x, q.y, 230, 5, new Color(120, 200, 255, 30), true);
          z.reveal = true;
          zones.add(z);
        };
        return true;
      }
      case 13:
      {
        Proj q = pj(ang, 900, RNG[1][3], 22, 150 + 130 * rk + sAD, 1, ice);
        q.stun = 1.0;
        q.distStun = true;
        q.expl = 140;
        q.big = true;
        q.kind = 4;
        return true;
      }
      // ---------- LUX ----------
      case 20:
      {
        Proj q =
          pj(ang, 1000, RNG[2][0], 20, 50 + 45 * rk + 0.6 * sAP, 1,
             new Color(255, 240, 150));
        q.root = 1.5;
        q.pierce = 2;
        q.mark = true;
        q.kind = 0;
        q.big = true;
        return true;
      }
      case 21:
      { // wand boomerang: shield now, shield again when it returns
        final double v = 50 + 35 * rk + 0.4 * sAP;
        addShield(v, 3);
        Proj q = pj(ang, 650, RNG[2][1], 12, 0, 1, new Color(180, 220, 255));
        q.visual = true;
        q.boom = true;
        q.maxRange = RNG[2][1];
        q.life = 8;
        q.kind = 6;
        q.onReturn = () -> {
          addShield(v, 3);
          ring(p.x, p.y, 10, 60, 0.4, new Color(180, 220, 255), 4);
        };
        ring(p.x, p.y, 10, 60, 0.4, new Color(180, 220, 255), 4);
        return true;
      }
      case 22:
      { // Lucent Singularity (press again to detonate)
        if (luxOrb != null && zones.contains(luxOrb))
        {
          luxOrb.until = now;
          recastFree = true;
          return true;
        }
        double d = Math.min(cdl, RNG[2][2]);
        final Zone z =
          nz(p.x + Math.cos(ang) * d, p.y + Math.sin(ang) * d, SZ[2][2], 4.0,
             new Color(255, 245, 160, 80), true);
        z.slow = 0.4;
        z.kind = 2;
        final int r = rk;
        z.end = () -> {
          for (Mob m : new ArrayList<>(mobs))
            if (Math.hypot(m.x - z.x, m.y - z.y) < z.r + m.r)
            {
              hitMob(m, 70 + 55 * r + 0.7 * sAP, 1, false);
              mark(m);
            }
          ring(z.x, z.y, 20, z.r + 30, 0.4, new Color(255, 255, 200), 8);
          burst(z.x, z.y, 30, new Color(255, 245, 160), 260, 5);
          luxOrb = null;
        };
        luxOrb = z;
        zones.add(z);
        return true;
      }
      case 23:
      { // Final Spark: 1s telegraph, then the beam
        final double sx = p.x, sy = p.y, ex = p.x + Math.cos(ang) * RNG[2][3],
          ey = p.y + Math.sin(ang) * RNG[2][3];
        final int r = rk;
        selfRootT = now + 1.0;
        castLock = 1.2;
        beam(sx, sy, ex, ey, 24, 1.0, new Color(255, 240, 150, 90));
        timed.add(new Timed(now + 1.0, () -> {
          for (Mob m : new ArrayList<>(mobs))
            if (segDist(m.x, m.y, sx, sy, ex, ey) < SZ[2][3] / 2.0 + m.r)
            {
              hitMob(m, 250 + 200 * r + sAP, 1, false);
              mark(m);
            }
          beam(sx, sy, ex, ey, 110, 0.55, new Color(255, 120, 220, 110));
          beam(sx, sy, ex, ey, 70, 0.55, new Color(255, 240, 160, 170));
          beam(sx, sy, ex, ey, 26, 0.55, Color.WHITE);
          for (int i = 0; i < 14; i++)
            burst(sx + (ex - sx) * i / 13.0, sy + (ey - sy) * i / 13.0, 3,
                  new Color(255, 240, 160), 120, 4);
        }));
        return true;
      }
      // ---------- MASTER YI ----------
      case 30:
      { // Alpha Strike (safe landing: never inside walls)
        final List<Mob> ts = new ArrayList<>();
        for (Mob m : mobs)
          if (!m.bush && dist(m) < RNG[3][0])
            ts.add(m);
        ts.sort((a, b) -> Double.compare(dist(a), dist(b)));
        if (ts.isEmpty())
        {
          say("No enemies in range");
          return false;
        }
        while (ts.size() > 4)
          ts.remove(ts.size() - 1);
        final int n = ts.size(), r = rk;
        final double ox = p.x, oy = p.y;
        invisT = now + 0.1 * n + 0.15;
        invulnT = invisT;
        castLock = 0.1 * n + 0.15;
        for (int i = 0; i < n; i++)
        {
          final Mob m = ts.get(i);
          timed.add(new Timed(now + 0.1 * i, () -> {
            if (!mobs.contains(m))
              return;
            double fx = p.x, fy = p.y, a = Math.atan2(m.y - p.y, m.x - p.x);
            if (!setPosSafe(m.x - Math.cos(a) * 30, m.y - Math.sin(a) * 30))
              return; // only lands on free ground
            beam(fx, fy, p.x, p.y, 8, 0.3, neon);
            arc(m.x, m.y, 12, 56, rnd.nextDouble() * 6, 2.2, 0.22, neon, 4);
            burst(m.x, m.y, 8, neon, 200, 3);
            hitMob(m, 40 + 45 * r + 0.6 * sAD, 0, false);
          }));
        }
        timed.add(new Timed(now + 0.1 * n + 0.05, () -> { // reappear at the
                                                          // primary target,
                                                          // then double-check
                                                          // we are not stuck
          Mob m = ts.get(0);
          if (mobs.contains(m))
            setPosSafe(m.x - 30, m.y);
          if (blocked(p.x, p.y, p.r))
          {
            p.x = ox;
            p.y = oy;
            if (blocked(p.x, p.y, p.r))
              unstick();
          }
        }));
        return true;
      }
      case 31:
        meditT = now + 4;
        drT = now + 4;
        drA = 0.6;
        selfRootT = now + 4;
        ring(p.x, p.y, 10, 70, 0.6, gold, 5);
        return true;
      case 32:
        wujuT = now + 6;
        ring(p.x, p.y, 10, 50, 0.4, neon, 4);
        return true;
      case 33:
        hlT = now + 7;
        ring(p.x, p.y, 10, 90, 0.6, new Color(255, 200, 60), 6);
        say("HIGHLANDER!");
        return true;
      // ---------- AKALI ----------
      case 40:
      {
        for (int i = 0; i < 5; i++)
        {
          Proj q =
            pj(ang + (i - 2) * 0.28, 900, RNG[4][0], 6, 0, 1,
               new Color(210, 120, 255));
          q.visual = true;
          q.kind = 3;
        }
        int n =
          coneHit(ang, RNG[4][0], SZ[4][0],
                  30 + 25 * rk + 0.5 * sAP + 0.4 * sAD, 1);
        arc(p.x, p.y, 40, RNG[4][0], ang - 0.62, 1.24, 0.3,
            new Color(210, 120, 255), 8);
        return true;
      }
      case 41:
      {
        smokeT = now + 4;
        smokeX = p.x;
        smokeY = p.y;
        spdT = now + 4;
        spdA = 0.3;
        Zone z = nz(p.x, p.y, SZ[4][1], 4, new Color(110, 60, 170, 85), true);
        z.kind = 4;
        zones.add(z);
        ring(p.x, p.y, 10, SZ[4][1], 0.5, new Color(160, 100, 220), 6);
        return true;
      }
      case 42:
      {
        Proj q =
          pj(ang, 950, RNG[4][2], 12, 50 + 40 * rk + 0.5 * sAP, 1,
             new Color(210, 120, 255));
        q.mark = true;
        q.kind = 3;
        q.big = true;
        dashTo(ang + Math.PI, 220, new Color(190, 100, 255));
        return true;
      }
      case 43:
      {
        if (akRT > now && akRTarget != null && mobs.contains(akRTarget))
        { // recast: execute dash
          Mob t = akRTarget;
          double a = Math.atan2(t.y - p.y, t.x - p.x);
          setPosSafe(t.x - Math.cos(a) * 30, t.y - Math.sin(a) * 30);
          hitMob(t, 100 + 120 * rk + 0.35 * (t.maxhp - t.hp) + 0.5 * sAD, 1,
                 true);
          ring(t.x, t.y, 10, 80, 0.4, new Color(255, 80, 120), 6);
          burst(t.x, t.y, 20, new Color(255, 80, 120), 220, 5);
          akRT = 0;
          recastFree = true;
          return true;
        }
        Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > RNG[4][3] + t.r)
        {
          say("No target locked in range");
          return false;
        }
        double a = Math.atan2(t.y - p.y, t.x - p.x);
        double fx = p.x, fy = p.y;
        setPosSafe(t.x - Math.cos(a) * 30, t.y - Math.sin(a) * 30);
        beam(fx, fy, p.x, p.y, 12, 0.3, new Color(190, 100, 255));
        hitMob(t, 60 + 60 * rk + 0.3 * sAP, 1, false);
        akRT = now + 3;
        akRTarget = t;
        say("Perfect Execution: press 4 again!");
        return true;
      }
      // ---------- MISS FORTUNE ----------
      case 50:
      {
        final Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > RNG[5][0] + t.r)
        {
          say("No target locked in range");
          return false;
        }
        double d1 = 20 + 25 * rk + sAD * 0.8;
        loveTap(t);
        hitMob(t, d1, 0, false);
        beam(p.x, p.y, t.x, t.y, 5, 0.18, new Color(255, 220, 120));
        Mob b = null;
        double bd = 450;
        for (Mob o : mobs)
          if (o != t && !o.bush)
          {
            double d = Math.hypot(o.x - t.x, o.y - t.y);
            if (d < bd)
            {
              bd = d;
              b = o;
            }
          }
        if (b != null)
        {
          hitMob(b, d1, 0, false);
          beam(t.x, t.y, b.x, b.y, 5, 0.18, new Color(255, 160, 80));
        }
        return true;
      }
      case 51:
        asT = now + 4;
        asA = 0.4;
        ring(p.x, p.y, 10, 56, 0.4, new Color(255, 120, 120), 4);
        return true;
      case 52:
      {
        double d = Math.min(cdl, RNG[5][2]);
        Zone z =
          nz(p.x + Math.cos(ang) * d, p.y + Math.sin(ang) * d, SZ[5][2], 2.0,
             new Color(255, 140, 70, 70), true);
        z.slow = 0.4;
        z.kind = 3;
        z.tick = 15 + 12 * rk + 0.25 * sAD;
        z.every = 0.25;
        zones.add(z);
        return true;
      }
      case 53:
      {
        selfRootT = now + 2.0;
        castLock = 2.0;
        final int r = rk;
        for (int i = 0; i < 10; i++)
          timed.add(new Timed(now + 0.2 * i, () -> {
            double a = Math.atan2(mwy() - p.y, mwx() - p.x); // the cone follows
                                                             // your cursor
            coneHit(a, RNG[5][3], SZ[5][3], 20 + 15 * r + 0.4 * sAD, 0);
            for (int j = 0; j < 4; j++)
            {
              Proj q =
                pj(a + (rnd.nextDouble() - 0.5) * 1.3, 950, RNG[5][3], 5, 0, 0,
                   new Color(255, 220, 120));
              q.visual = true;
              q.kind = 3;
            }
          }));
        return true;
      }
      // ---------- AHRI ----------
      case 60:
      { // orb out (magic) and back (true damage)
        Proj q =
          pj(ang, 700, RNG[6][0], 18, 40 + 35 * rk + 0.35 * sAP, 1,
             new Color(255, 160, 230));
        q.boom = true;
        q.maxRange = RNG[6][0];
        q.life = 8;
        q.pierce = 99;
        q.retType = 2;
        q.kind = 7;
        return true;
      }
      case 61:
      {
        final List<Mob> ts = new ArrayList<>();
        for (Mob m : mobs)
          if (!m.bush && dist(m) < SZ[6][1])
            ts.add(m);
        ts.sort((a, b) -> Double.compare(dist(a), dist(b)));
        if (ts.isEmpty())
        {
          say("No enemies in range");
          return false;
        }
        for (int i = 0; i < 3; i++)
        {
          Mob m = ts.get(Math.min(i, ts.size() - 1));
          double a0 = ang + i * 2.09;
          Proj q =
            pj(a0, 520, 700, 9, 30 + 25 * rk + 0.3 * sAP, 1,
               new Color(255, 150, 90));
          q.homing = m;
          q.kind = 5;
          q.life = 2.2;
          q.x = p.x + Math.cos(a0) * 24;
          q.y = p.y + Math.sin(a0) * 24;
        }
        return true;
      }
      case 62:
      {
        Proj q =
          pj(ang, 850, RNG[6][2], 22, 60 + 40 * rk + 0.5 * sAP, 1,
             new Color(255, 120, 200));
        q.stun = 1.4;
        q.flags = 7;
        q.kind = 7;
        q.big = true;
        return true;
      }
      case 63:
      {
        boolean re = ahriRC > 0 && now < ahriRT;
        if (re)
        {
          ahriRC--;
          recastFree = true;
        }
        else
        {
          ahriRC = 2;
          ahriRT = now + 10;
        }
        dashTo(ang, Math.min(cdl, RNG[6][3]), new Color(255, 150, 220));
        final List<Mob> ts = new ArrayList<>();
        for (Mob m : mobs)
          if (!m.bush && dist(m) < 550)
            ts.add(m);
        ts.sort((a, b) -> Double.compare(dist(a), dist(b)));
        for (int i = 0; i < Math.min(3, ts.size()); i++)
        {
          Mob m = ts.get(i);
          hitMob(m, 60 + 50 * rk + 0.35 * sAP, 1, false);
          essence();
          beam(p.x, p.y, m.x, m.y, 6, 0.2, new Color(255, 150, 220));
          burst(m.x, m.y, 8, new Color(255, 150, 220), 160, 4);
        }
        return true;
      }
      // ---------- MALPHITE ----------
      case 70:
      {
        final Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > RNG[7][0] + t.r)
        {
          say("No target locked in range");
          return false;
        }
        hitMob(t, 60 + 40 * rk + 0.6 * sAP, 1, false);
        t.slowT = now + 3;
        t.slowA = 0.25;
        spdT = now + 3;
        spdA = 0.2;
        beam(p.x, p.y, t.x, t.y, 14, 0.25, new Color(190, 170, 140));
        burst(t.x, t.y, 14, new Color(190, 170, 140), 200, 5);
        return true;
      }
      case 71:
        thunderT = now + 5;
        armT = now + 5;
        ring(p.x, p.y, 10, 70, 0.5, new Color(200, 190, 160), 6);
        return true;
      case 72:
      {
        for (Mob m : new ArrayList<>(mobs))
          if (dist(m) < SZ[7][2] + m.r)
          {
            hitMob(m, 50 + 35 * rk + 0.4 * sAP, 1, false);
            m.slowT = now + 3;
            m.slowA = 0.3;
            m.asDownT = now + 4;
          }
        ring(p.x, p.y, 20, SZ[7][2], 0.45, new Color(200, 180, 140), 8);
        burst(p.x, p.y, 26, new Color(190, 170, 140), 260, 5);
        return true;
      }
      case 73:
      {
        double d = Math.min(cdl, RNG[7][3]);
        dashTo(ang, d, new Color(200, 180, 140));
        invulnT = now + 0.5;
        castLock = 0.5;
        final int r = rk;
        timed.add(new Timed(now + 0.25, () -> {
          for (Mob m : new ArrayList<>(mobs))
            if (dist(m) < SZ[7][3] + m.r)
            {
              hitMob(m, 150 + 100 * r + 0.8 * sAP, 1, false);
              m.stunT = now + 1.5;
            }
          ring(p.x, p.y, 30, SZ[7][3], 0.5, new Color(220, 200, 150), 10);
          burst(p.x, p.y, 40, new Color(200, 180, 140), 320, 6);
        }));
        return true;
      }
    }
    return false;
  }

  // ===================== UPDATE LOOP =====================
  void update(double dt)
  {
    clock += dt;
    if (state != S_PLAY)
      return;
    now += dt;
    recalc();
    for (Timed t : new ArrayList<>(timed))
      if (now >= t.t)
      {
        timed.remove(t);
        t.r.run();
      }
    if (state != S_PLAY)
      return;
    if (blocked(p.x, p.y, p.r - 3))
      unstick(); // safety net: never stay inside a wall
    // ----- movement -----
    double dx = 0, dy = 0;
    if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP])
      dy -= 1;
    if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN])
      dy += 1;
    if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT])
      dx -= 1;
    if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT])
      dx += 1;
    boolean canMove =
      p.stunT <= now && p.rootT <= now && selfRootT <= now && invisT <= now;
    isMoving = canMove && (dx != 0 || dy != 0);
    if (isMoving)
    {
      lastMoveDir = dirIdx(Math.atan2(dy, dx));
      faceDir = lastMoveDir;
    }
    if (isMoving)
    {
      double l = Math.hypot(dx, dy),
        sp = sMS * (p.slowT > now && hlT <= now ? 1 - p.slowA : 1);
      move(p, dx / l * sp * dt, dy / l * sp * dt);
    }
    camX = Math.max(0, Math.min(MW * T - W / ZOOM, p.x - W / ZOOM / 2));
    camY = Math.max(0, Math.min(MH * T - H / ZOOM, p.y - H / ZOOM / 2));
    int ptx = (int) (p.x / T), pty = (int) (p.y / T);
    if (ptx != flowTX || pty != flowTY || now > flowT)
    {
      flow = bfs(ptx, pty);
      flowTX = ptx;
      flowTY = pty;
      flowT = now + 0.5;
    } // path field toward the player
    if (hold && aim < 0)
      basicAttack();
    processQueue();
    // ----- buffs, regeneration (slower than before) -----
    if (focusStacks > 0 && focusT <= now && now - lastFocusT > 4.5)
      focusStacks = 0;
    if (champ == 7)
    {
      if (now - lastHurt > 10 && shield < maxHp * 0.1)
        shield = maxHp * 0.1;
      if (shT < now + 500)
        shT = now + 9999;
    } // Granite Shield
    else if (shT <= now)
      shield = 0;
    double regen = maxHp * 0.0015 * (has("Warmog's Armor") ? 2 : 1);
    if (now - lastHurt > 5)
    {
      if (champ == 0)
        regen += maxHp * 0.01;
      if (has("Warmog's Armor"))
        regen += maxHp * 0.025;
    }
    heal(regen * dt);
    if (meditT > now)
      heal((50 + 35 * rank[1]) * dt);
    mana = Math.min(maxMana, mana + (1.5 + maxMana * 0.004) * dt);
    if (pBurnT > now)
    {
      pBurnAcc += dt;
      if (pBurnAcc >= 0.5)
      {
        pBurnAcc = 0;
        hurt(pBurnDps * 0.5, 1, null, 0);
      }
    }
    if (spinT > now && now >= spinTick)
    { // Judgment blades
      spinTick = now + 0.35;
      for (Mob m : new ArrayList<>(mobs))
        if (dist(m) < 125 + m.r)
          hitMob(m, 10 + 10 * rank[2] + 0.25 * sAD, 0, false);
      arc(p.x, p.y, 40, 125, rnd.nextDouble() * 6, 2.0, 0.25,
          new Color(255, 215, 80), 6);
    }
    if (has("Sunfire Aegis") && now >= sunTick)
    {
      sunTick = now + 0.5;
      for (Mob m : new ArrayList<>(mobs))
        if (dist(m) < 130 + m.r)
          hitMob(m, 10 + 0.03 * maxHp, 1, false);
    }
    if (hlT > now && rnd.nextInt(4) == 0)
      burst(p.x, p.y + 6, 1, new Color(255, 200, 60), 60, 3); // Highlander aura
                                                              // sparks
    // ----- projectiles -----
    for (Proj q : new ArrayList<>(projs))
    {
      if (q.homing != null)
      { // homing (Fox-Fire)
        if (mobs.contains(q.homing))
        {
          double a = Math.atan2(q.homing.y - q.y, q.homing.x - q.x),
            sp = Math.hypot(q.vx, q.vy);
          q.vx = Math.cos(a) * sp;
          q.vy = Math.sin(a) * sp;
        }
        else
          q.homing = null;
      }
      if (q.boom && q.returning)
      { // boomerang flying back to the player
        double a = Math.atan2(p.y - q.y, p.x - q.x),
          sp = Math.hypot(q.vx, q.vy);
        q.vx = Math.cos(a) * sp * 1.15;
        q.vy = Math.sin(a) * sp * 1.15;
        if (Math.hypot(p.x - q.x, p.y - q.y) < 30)
        {
          projs.remove(q);
          if (q.onReturn != null)
            q.onReturn.run();
          continue;
        }
      }
      double step = Math.hypot(q.vx, q.vy) * dt;
      q.x += q.vx * dt;
      q.y += q.vy * dt;
      q.dist += step;
      q.life -= dt;
      if (q.onTick != null && now >= q.nextTick)
      {
        q.nextTick = now + q.tickEvery;
        q.onTick.run();
      }
      if (q.kind != 0 && rnd.nextInt(3) == 0 && parts.size() < 900)
      {
        Part t = new Part();
        t.x = q.x;
        t.y = q.y;
        t.vx = rnd.nextGaussian() * 15;
        t.vy = rnd.nextGaussian() * 15;
        t.born = now;
        t.life = 0.3;
        t.size = q.big ? 4 : 2.5;
        t.c = q.c;
        parts.add(t);
      }
      boolean wallHit = blocked(q.x, q.y, 0);
      if (q.boom && !q.returning && (q.dist >= q.maxRange || wallHit))
      {
        q.returning = true;
        q.hit.clear();
        if (q.retType >= 0)
          q.type = q.retType;
      }
      else if (!q.boom && (q.life <= 0 || wallHit))
      {
        projs.remove(q);
        if (q.expl > 0)
          burst(q.x, q.y, 10, q.c, 150, 4);
        continue;
      }
      else if (q.boom && q.life <= 0)
      {
        projs.remove(q);
        continue;
      }
      if (q.visual)
        continue;
      if (q.friendly)
      {
        for (Mob m : new ArrayList<>(mobs))
        {
          if (q.hit.contains(m)
            || Math.hypot(m.x - q.x, m.y - q.y) >= m.r + q.r)
            continue;
          q.hit.add(m);
          projHit(q, m);
          if (--q.pierce <= 0)
          {
            projs.remove(q);
            break;
          }
        }
      }
      else if (Math.hypot(p.x - q.x, p.y - q.y) < p.r + q.r)
      {
        hurt(q.dmg, q.type, q.src, q.flags);
        if (q.dot > 0)
        {
          pBurnT = now + 3;
          pBurnDps = q.dot;
        }
        projs.remove(q);
      }
    }
    // ----- zones -----
    for (Zone z : new ArrayList<>(zones))
    {
      if (now >= z.until)
      {
        zones.remove(z);
        if (z.end != null)
          z.end.run();
        continue;
      }
      if (now < z.start)
        continue;
      boolean tick = z.tick > 0 && now >= z.next;
      if (z.friendly)
      {
        for (Mob m : new ArrayList<>(mobs))
          if (Math.hypot(m.x - z.x, m.y - z.y) < z.r + m.r)
          {
            if (z.slow > 0)
            {
              m.slowT = now + 0.3;
              m.slowA = z.slow;
            }
            if (tick)
              hitMob(m, z.tick, 0, false);
          }
        if (tick && z.kind == 3)
          burst(z.x + (rnd.nextDouble() - 0.5) * z.r * 1.6,
                z.y + (rnd.nextDouble() - 0.5) * z.r * 1.6, 5,
                new Color(255, 200, 100), 90, 3);
      }
      else if (Math.hypot(p.x - z.x, p.y - z.y) < z.r + p.r)
      {
        if (tick)
          hurt(z.tick, 1, z.src, z.flags);
        if (z.slow > 0 && hlT <= now)
        {
          p.slowT = now + 0.3;
          p.slowA = z.slow;
        }
      }
      if (tick)
        z.next = now + z.every;
    }
    // ----- mobs -----
    for (Mob m : new ArrayList<>(mobs))
      if (mobs.contains(m))
        ai(m, dt);
    for (int i = 0; i < mobs.size(); i++)
      for (int j = i + 1; j < mobs.size(); j++)
      { // soft separation
        Mob a = mobs.get(i), b = mobs.get(j);
        if (a.spd == 0 || b.spd == 0)
          continue;
        double ddx = b.x - a.x, ddy = b.y - a.y, d = Math.hypot(ddx, ddy),
          min = (a.r + b.r) * 0.8;
        if (d < min && d > 0.01)
        {
          double push = (min - d) / 2;
          move(a, -ddx / d * push, -ddy / d * push);
          move(b, ddx / d * push, ddy / d * push);
        }
      }
    // ----- pickups -----
    for (Pickup k : new ArrayList<>(pickups))
    {
      double d = Math.hypot(k.x - p.x, k.y - p.y);
      if (d < 130)
      {
        k.x += (p.x - k.x) * 4 * dt;
        k.y += (p.y - k.y) * 4 * dt;
      }
      if (d < 24)
      {
        if (k.type == 0)
        {
          gold += k.amt;
          ftext(p.x, p.y - 40, "+" + k.amt + "g", Color.YELLOW);
        }
        else
        {
          heal(maxHp * 0.08);
          mana = Math.min(maxMana, mana + maxMana * 0.08);
          ftext(p.x, p.y - 40, "Honeyfruit!", new Color(255, 190, 80));
        }
        pickups.remove(k);
      }
    }
    // ----- effects -----
    for (Part q : parts)
    {
      q.x += q.vx * dt;
      q.y += q.vy * dt;
      q.vx *= 0.93;
      q.vy *= 0.93;
    }
    parts.removeIf(q -> now > q.born + q.life);
    rings.removeIf(r -> now > r.born + r.dur);
    corpses.removeIf(c -> now > c.born + 1.4);
    if (!cleared)
    {
      boolean any = false;
      for (Mob m : mobs)
        if (!m.bush)
        {
          any = true;
          break;
        }
      if (!any)
      {
        cleared = true;
        say("Floor cleared! Walk to the stairs");
      }
    }
    ftexts.removeIf(f -> now > f.until);
    beams.removeIf(b -> now > b.until);
  }

  void projHit(Proj q, Mob m)
  { // what a friendly projectile does on contact
    if (q.basic)
    {
      onHit(m, q.mul);
      return;
    }
    hitMob(m, q.dmg, q.type, false);
    if (champ == 6)
      essence();
    if (q.slow > 0)
    {
      m.slowT = now + q.slowD;
      m.slowA = q.slow;
    }
    if (q.distStun)
    {
      double st = Math.min(3.5, 1.0 + q.dist / 1500.0 * 2.5);
      m.stunT = now + st;
      ftext(m.x, m.y - 50, String.format("Stun %.1fs", st), ice());
      q.stun = st;
    }
    else if (q.stun > 0)
    {
      m.stunT = now + q.stun;
      if (q.flags == 7)
        m.charmT = now + q.stun;
    }
    if (q.root > 0)
      m.rootT = now + q.root;
    if (q.mark)
      mark(m);
    if (q.expl > 0)
    {
      for (Mob o : new ArrayList<>(mobs))
        if (o != m && Math.hypot(o.x - q.x, o.y - q.y) < q.expl + o.r)
        {
          hitMob(o, q.dmg * 0.6, q.type, false);
          o.stunT = now + q.stun;
        }
      ring(q.x, q.y, 10, q.expl, 0.4, ice(), 6);
      burst(q.x, q.y, 24, ice(), 240, 5);
    }
  }

  static Color ice()
  {
    return new Color(170, 235, 255);
  }

  // ===================== MOB AI =====================
  void meleeAtk(Mob m, int fl)
  {
    if (now >= m.atkT && Math.hypot(p.x - m.x, p.y - m.y) < m.r + p.r + 16)
    {
      m.atkT = now + m.atkCd * (m.asDownT > now ? 1.5 : 1);
      m.lastAtk = now;
      hurt(m.ad, 0, m, fl);
    }
  }

  void ai(Mob m, double dt)
  {
    if (m.bush)
      return;
    double ox = m.x, oy = m.y;
    if (m.burnT > now && now >= m.burnNext)
    {
      m.burnNext = now + 0.5;
      hitMob(m, m.burnDps * 0.5, 2, false);
      if (!mobs.contains(m))
        return;
    }
    if (m.stunT > now)
    {
      m.moving = false;
      return;
    }
    double dx = p.x - m.x, dy = p.y - m.y, d = Math.hypot(dx, dy) + 1e-6,
      ux = dx / d, uy = dy / d;
    double dh = Math.hypot(m.hx - m.x, m.hy - m.y);
    boolean hidden =
      m.boss == 0 && (invisT > now || (smokeT > now
        && Math.hypot(p.x - smokeX, p.y - smokeY) < SZ[4][1] && d > 90)); // Akali
                                                                          // smoke
                                                                          // /
                                                                          // Yi
                                                                          // alpha
    // --- aggro zone: mobs sleep at home until the player steps inside their
    // radius ---
    if (m.state == 0)
    {
      if (!hidden && d < m.aggro)
        wake(m);
      if (m.state == 0)
      {
        m.moving = false;
        if (m.hp < m.maxhp)
          m.hp = Math.min(m.maxhp, m.hp + m.maxhp * 0.02 * dt);
        return;
      }
    }
    if (m.state == 2)
    { // leash: walk back home and heal
      double[] hv = stepFrom(m.homeFlow, m.x, m.y);
      if (hv == null)
        hv =
          new double[] {(m.hx - m.x) / (dh + 1e-6), (m.hy - m.y) / (dh + 1e-6)};
      move(m, hv[0] * m.spd * 1.5 * dt, hv[1] * m.spd * 1.5 * dt);
      m.hp = Math.min(m.maxhp, m.hp + m.maxhp * 0.08 * dt);
      m.moving = true;
      if (dh < 30)
      {
        m.state = 0;
        m.homeFlow = null;
      }
      else if (!hidden && d < m.aggro * 0.7)
      {
        m.state = 1;
        m.homeFlow = null;
      }
      return;
    }
    if (m.boss == 0 && (hidden || dh > m.leash || d > m.aggro * 2.5 + 250))
    {
      startReturn(m);
      return;
    }
    double[] pv = pathDir(m);
    double vx = pv[0], vy = pv[1]; // path-finding direction (goes around walls)
    double sp =
      m.spd * (m.slowT > now ? 1 - m.slowA : 1) * (m.rootT > now ? 0 : 1);
    boolean sil = m.silT > now;
    int fl = (m.boss == 12 ? 1 : 0) | (m.elem == 1 ? 2 : 0);
    if (m.boss != 10)
      m.fa = Math.atan2(dy, dx);
    switch (m.ai)
    {
      case 0:
        if (d > m.r + p.r + 6)
          move(m, vx * sp * dt, vy * sp * dt);
        meleeAtk(m, 0);
        break;
      case 1: // wolf: Pack Pounce leap + bite
        if (m.t2 > now)
          move(m, Math.cos(m.pa) * 520 * dt, Math.sin(m.pa) * 520 * dt);
        else if (d < 280 && d > 90 && now >= m.t1 && !sil
          && los(m.x, m.y, p.x, p.y, 8))
        {
          m.t1 = now + 4;
          m.t2 = now + 0.3;
          m.pa = Math.atan2(dy, dx);
          m.lastCast = now;
        }
        else if (d > m.r + p.r + 6)
          move(m, vx * sp * dt, vy * sp * dt);
        meleeAtk(m, 0);
        break;
      case 2: // gromp: kites, spits poison, telegraphed hop
        if (d < 230)
          move(m, -ux * sp * 0.8 * dt, -uy * sp * 0.8 * dt);
        else if (d > 420)
          move(m, vx * sp * dt, vy * sp * dt);
        if (d < 450 && now >= m.t1 && !sil && los(m.x, m.y, p.x, p.y, 8))
        {
          m.t1 = now + 2.2;
          m.lastCast = now;
          eproj(m, Math.atan2(dy, dx), 330, m.ad * 0.8, 1,
                new Color(150, 255, 60), 0, m.ad * 0.15);
        }
        if (d < 95 && now >= m.t2 && !sil)
        { // hop: warning ring first, then a short push
          m.t2 = now + 4;
          m.lastAtk = now;
          final Mob gm = m;
          Zone z = nz(m.x, m.y, 95, 0.5, new Color(170, 230, 60, 110), false);
          z.end = () -> {
            if (mobs.contains(gm)
              && Math.hypot(p.x - gm.x, p.y - gm.y) < 95 + p.r)
            {
              double a = Math.atan2(p.y - gm.y, p.x - gm.x);
              move(p, Math.cos(a) * 55, Math.sin(a) * 55);
              hurt(gm.ad, 0, gm, 0);
            }
          };
          zones.add(z);
        }
        break;
      case 3: // blue golem: ranged orbs
        if (d < 260)
          move(m, -ux * sp * dt, -uy * sp * dt);
        else if (d > 400)
          move(m, vx * sp * dt, vy * sp * dt);
        if (d < 600 && now >= m.t1 && !sil && los(m.x, m.y, p.x, p.y, 8))
        {
          m.t1 = now + 2.4;
          m.lastCast = now;
          eproj(m, Math.atan2(dy, dx), 330, m.ad * 1.1, 1,
                new Color(90, 170, 255), 0, 0);
        }
        break;
      case 4: // elemental dragons
        if (d > 120)
          move(m, vx * sp * dt, vy * sp * dt);
        meleeAtk(m, fl);
        if (now >= m.t1 && d < 650 && !sil)
        {
          m.t1 = now + (m.elem == 4 ? 1.1 : 2.2);
          m.lastCast = now;
          eproj(m, Math.atan2(dy, dx), 380, m.ad * 0.9, 1, m.col, fl, 0);
        }
        if (m.elem == 1 && now >= m.t2)
        {
          m.t2 = now + 3.5;
          Zone z = nz(p.x, p.y, 85, 4.6, new Color(255, 90, 20, 120), false);
          z.start = now + 0.6;
          z.tick = m.ad * 0.25;
          z.flags = 2;
          z.src = m;
          zones.add(z);
        }
        if (m.elem == 2 && m.healCutT <= now)
          m.hp = Math.min(m.maxhp, m.hp + m.maxhp * 0.015 * dt);
        if (m.elem == 4 && now >= m.t2)
        {
          m.t2 = now + 5;
          Zone z = nz(p.x, p.y, 90, 3, new Color(220, 240, 255, 110), false);
          z.start = now + 0.4;
          z.tick = m.ad * 0.15;
          z.every = 0.4;
          z.slow = 0.4;
          z.src = m;
          zones.add(z);
        }
        break;
      case 10: // Rift Herald: armored front, charges
        if (m.t2 > now)
        {
          move(m, Math.cos(m.pa) * 500 * dt, Math.sin(m.pa) * 500 * dt);
          if (d < m.r + p.r + 10 && now >= m.atkT)
          {
            m.atkT = now + 1;
            m.lastAtk = now;
            hurt(m.ad * 1.5, 0, m, 0);
          }
        }
        else
        {
          double want = Math.atan2(dy, dx);
          m.fa += Math.max(-0.9 * dt, Math.min(0.9 * dt, angDiff(want, m.fa)));
          if (d > m.r + p.r + 6)
            move(m, vx * sp * dt, vy * sp * dt);
          meleeAtk(m, 0);
          if (now >= m.t1)
          {
            m.t1 = now + 6;
            m.t2 = now + 1.2;
            m.pa = m.fa;
            m.lastCast = now;
            ftext(m.x, m.y - 70, "CHARGE!", Color.ORANGE);
          }
        }
        break;
      case 11:
      { // Baron Nashor: stationary
        if (m.hp < 0.3 * m.maxhp && !m.enraged)
        {
          m.enraged = true;
          m.ad *= 1.4;
          m.col = new Color(255, 40, 80);
          ftext(m.x, m.y - 90, "ENRAGED!", Color.RED);
        }
        double k = m.enraged ? 0.6 : 1;
        if (now >= m.t1)
        {
          m.t1 = now + 4 * k;
          m.lastCast = now;
          for (int i = 0; i < 3; i++)
          {
            Zone z =
              nz(p.x + (i == 0 ? 0 : rnd.nextGaussian() * 140),
                 p.y + (i == 0 ? 0 : rnd.nextGaussian() * 140), 95, 3.5,
                 new Color(110, 255, 60, 110), false);
            z.start = now + 1.0;
            z.tick = m.ad * 0.25;
            z.src = m;
            zones.add(z);
          }
        }
        if (now >= m.t2)
        {
          m.t2 = now + 6.5 * k;
          final Zone z =
            nz(p.x, p.y, 75, 1.0, new Color(150, 110, 60, 140), false);
          z.start = now + 0.9;
          final Mob b = m;
          z.end = () -> {
            if (Math.hypot(p.x - z.x, p.y - z.y) < z.r + p.r)
            {
              hurt(b.ad * 1.2, 0, b, 0);
              stunPlayer(0.8);
            }
          };
          zones.add(z);
        }
        if (now >= m.atkT && d < 700)
        {
          m.atkT = now + 3 * k;
          m.lastAtk = now;
          eproj(m, Math.atan2(dy, dx), 360, m.ad, 1, new Color(120, 255, 80), 0,
                0);
        }
        break;
      }
      case 12: // Elder Dragon: executes below 20% HP
        if (d > 130)
          move(m, vx * sp * dt, vy * sp * dt);
        meleeAtk(m, 1);
        if (now >= m.t1 && d < 750)
        {
          m.t1 = now + 2.4;
          m.lastCast = now;
          double a = Math.atan2(dy, dx);
          for (int i = -2; i <= 2; i++)
            eproj(m, a + i * 0.17, 400, m.ad * 0.8, 1, new Color(255, 120, 40),
                  1, 0);
        }
        if (now >= m.t2)
        {
          m.t2 = now + 4;
          Zone z = nz(p.x, p.y, 90, 4.6, new Color(255, 60, 20, 120), false);
          z.start = now + 0.6;
          z.tick = m.ad * 0.25;
          z.flags = 3;
          z.src = m;
          zones.add(z);
        }
        break;
    }
    m.moving = Math.hypot(m.x - ox, m.y - oy) > 0.3;
  }

  // ===================== DRAWING =====================
  String menuMsg = "";
  double menuMsgT;

  BufferedImage propImg(String name)
  {
    String k = "prop:" + name;
    if (!tileCache.containsKey(k))
      tileCache.put(k, loadImg("assets/props/" + name + ".png"));
    return tileCache.get(k);
  }

  void heart(Graphics2D g, double x, double y, double s)
  {
    g.setColor(new Color(255, 90, 160));
    g.fill(new Ellipse2D.Double(x - s, y - s, s, s));
    g.fill(new Ellipse2D.Double(x, y - s, s, s));
    Path2D.Double t = new Path2D.Double();
    t.moveTo(x - s, y - s * 0.4);
    t.lineTo(x + s, y - s * 0.4);
    t.lineTo(x, y + s);
    t.closePath();
    g.fill(t);
  }

  void diamond(Graphics2D g, int x, int y, int s, Color c)
  { // ruby icon
    Path2D.Double d = new Path2D.Double();
    d.moveTo(x, y - s);
    d.lineTo(x + s, y);
    d.lineTo(x, y + s);
    d.lineTo(x - s, y);
    d.closePath();
    g.setColor(c);
    g.fill(d);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(1));
    g.draw(d);
  }

  void drawChamp(Graphics2D g)
  { // champion select with locked champions
    ctr(g, "RIFT ROGUE", 56, 42, new Color(255, 215, 80));
    diamond(g, W - 150, 42, 11, new Color(230, 40, 90));
    txt(g, "Rubies: " + rubies, W - 130, 50, 20, new Color(255, 120, 150));
    txt(g,
        "Choose your Champion (click, or press 1-8). Locked champions are bought with rubies from chests and bosses.",
        40, 76, 14, Color.LIGHT_GRAY);
    int hov = -1;
    for (int i = 0; i < NCH; i++)
    {
      final int c = i;
      Rectangle r =
        new Rectangle(40 + (i % 4) * 305, 90 + (i / 4) * 240, 290, 225);
      boolean un = unlocked[i];
      g.setColor(new Color(30, 30, 40));
      g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
      g.setColor(r.contains(mx, my) ? CH_COL[i] : Color.GRAY);
      g.setStroke(new BasicStroke(3));
      g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
      if (!drawSheet(g, i, A_IDLE, (int) (clock / 2.0 * 8) % 8, 2, r.x + 145,
                     r.y + 135, 150))
        poly(g, r.x + 145, r.y + 95, 48, 6, 0, CH_COL[i]);
      txt(g, (i + 1) + ". " + CH[i], r.x + 16, r.y + 160, 24,
          un ? CH_COL[i] : Color.GRAY);
      txt(g, ROLE[i], r.x + 16, r.y + 180, 15, Color.LIGHT_GRAY);
      if (r.contains(mx, my))
        hov = i;
      if (un)
        btns.add(new Object[] {r, (Runnable) () -> {
          champ = c;
          state = S_SPELL;
        }});
      else
      {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        txt(g, "LOCKED", r.x + 100, r.y + 40, 22, new Color(255, 120, 120));
        button(g, new Rectangle(r.x + 45, r.y + 188, 200, 30),
               "Unlock: " + PRICE[i] + " rubies", new Color(150, 30, 70),
               () -> {
                 if (rubies >= PRICE[c])
                 {
                   rubies -= PRICE[c];
                   unlocked[c] = true;
                   saveGame();
                   menuMsg = CH[c] + " unlocked!";
                 }
                 else
                   menuMsg =
                     "Not enough rubies (" + rubies + "/" + PRICE[c] + ")";
                 menuMsgT = clock + 2.5;
               });
      }
    }
    if (hov >= 0)
    { // info panel for the hovered champion
      g.setColor(new Color(20, 20, 30, 235));
      g.fillRoundRect(40, 575, 1200, 120, 12, 12);
      g.setColor(CH_COL[hov]);
      g.drawRoundRect(40, 575, 1200, 120, 12, 12);
      txt(g, CH[hov] + " - " + PASSIVE[hov], 56, 600, 15, Color.WHITE);
      for (int s = 0; s < 4; s++)
      {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(SLOT_COL[s]);
        drawWrapped(g, (s + 1) + ": " + SK[hov][s] + " - " + DESC[hov][s],
                    56 + (s % 2) * 590, 622 + (s / 2) * 36, 570);
      }
    }
    if (clock < menuMsgT)
      ctr(g, menuMsg, 560, 20, Color.ORANGE);
  }

  void drawDoor(Graphics2D g, int x, int y)
  {
    int px = x * T, py = y * T;
    BufferedImage im = propImg("door");
    if (im != null)
    {
      g.drawImage(im, px, py, T, T, null);
      return;
    }
    g.setColor(new Color(110, 72, 40));
    g.fillRect(px, py, T, T);
    g.setColor(new Color(70, 44, 24));
    for (int k = 1; k < 4; k++)
      g.drawLine(px + k * 10, py, px + k * 10, py + T);
    g.setColor(new Color(170, 170, 185));
    g.fillRect(px, py + 8, T, 4);
    g.fillRect(px, py + T - 12, T, 4);
    g.setColor(new Color(255, 215, 90));
    g.fillOval(px + T / 2 - 4, py + T / 2 - 4, 8, 8);
    g.setColor(Color.BLACK);
    g.drawRect(px, py, T - 1, T - 1);
  }

  void drawChest(Graphics2D g, Chest c)
  {
    BufferedImage im = propImg(c.open ? "chest_open" : "chest_closed");
    if (im != null)
    {
      g.drawImage(im, (int) c.x - 20, (int) c.y - 20, 40, 40, null);
      return;
    }
    int x = (int) c.x - 16, y = (int) c.y - 10;
    g.setColor(new Color(0, 0, 0, 80));
    g.fillOval(x - 2, y + 18, 36, 10);
    g.setColor(new Color(120, 76, 36));
    g.fillRoundRect(x, y + 8, 32, 18, 4, 4); // chest body
    g.setColor(c.open ? new Color(60, 40, 20) : new Color(140, 90, 44));
    if (c.open)
      g.fillRect(x + 2, y + 8, 28, 6);
    else
      g.fillRoundRect(x, y, 32, 12, 8, 8); // lid
    if (c.open)
    {
      g.setColor(new Color(150, 100, 50));
      g.fillRoundRect(x, y - 6, 32, 6, 4, 4);
      g.setColor(new Color(255, 225, 110, 90));
      g.fillOval(x + 4, y - 4, 24, 12);
    }
    g.setColor(new Color(255, 205, 70));
    g.fillRect(x, y + 10, 32, 3);
    g.fillRect(x + 13, y + 6, 6, 10);
    g.setColor(Color.BLACK);
    g.drawRoundRect(x, y + 8, 32, 18, 4, 4);
  }

  void drawProj(Graphics2D g, Proj q)
  { // arrows look like arrows, orbs glow, etc.
    double ang = Math.atan2(q.vy, q.vx);
    AffineTransform old = g.getTransform();
    g.translate(q.x, q.y);
    g.rotate(ang);
    Color c = q.c;
    Color glow = new Color(c.getRed(), c.getGreen(), c.getBlue(), 70);
    if (q.kind == 2)
    { // ice arrow: shaft, arrowhead, fletching, frost trail
      int len = 26;
      g.setColor(glow);
      g.fillRect(-len - 16, -2, len + 16, 4);
      g.setStroke(new BasicStroke(2.5f));
      g.setColor(new Color(225, 240, 255));
      g.drawLine(-len, 0, 0, 0);
      Path2D.Double h = new Path2D.Double();
      h.moveTo(10, 0);
      h.lineTo(0, -4.5);
      h.lineTo(0, 4.5);
      h.closePath();
      g.setColor(new Color(150, 220, 255));
      g.fill(h);
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(1));
      g.draw(h);
      g.setColor(new Color(120, 200, 255));
      g.setStroke(new BasicStroke(2));
      g.drawLine(-len, 0, -len - 6, -4);
      g.drawLine(-len, 0, -len - 6, 4);
      g.drawLine(-len + 5, 0, -len - 1, -4);
      g.drawLine(-len + 5, 0, -len - 1, 4);
    }
    else if (q.kind == 4)
    { // Enchanted Crystal Arrow
      g.setColor(new Color(150, 230, 255, 60));
      g.fill(new Ellipse2D.Double(-70, -20, 110, 40));
      g.setColor(new Color(120, 210, 255, 120));
      g.fillRect(-60, -6, 60, 12);
      g.setColor(new Color(220, 245, 255));
      g.setStroke(new BasicStroke(5));
      g.drawLine(-55, 0, 0, 0);
      Path2D.Double h = new Path2D.Double();
      h.moveTo(24, 0);
      h.lineTo(4, -13);
      h.lineTo(-4, 0);
      h.lineTo(4, 13);
      h.closePath();
      g.setColor(new Color(90, 200, 255));
      g.fill(h);
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(2));
      g.draw(h);
      g.setColor(new Color(120, 200, 255));
      g.setStroke(new BasicStroke(3));
      g.drawLine(-55, 0, -68, -10);
      g.drawLine(-55, 0, -68, 10);
      g.drawLine(-42, 0, -55, -10);
      g.drawLine(-42, 0, -55, 10);
    }
    else if (q.kind == 3)
    { // bullet / kunai streak
      g.setColor(glow);
      g.fillRect(-26, -3, 26, 6);
      g.setColor(c);
      g.fill(new Ellipse2D.Double(-6, -3, 14, 6));
      g.setColor(Color.WHITE);
      g.fill(new Ellipse2D.Double(-1, -1.5, 7, 3));
    }
    else if (q.kind == 5)
    { // fire orb / fox-fire
      for (int i = 3; i >= 1; i--)
      {
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40 * i));
        double s = q.r + i * 4;
        g.fill(new Ellipse2D.Double(-s, -s, 2 * s, 2 * s));
      }
      g.setColor(new Color(255, 240, 200));
      g.fill(new Ellipse2D.Double(-q.r * 0.5, -q.r * 0.5, q.r, q.r));
    }
    else if (q.kind == 6)
    { // Lux wand, spinning
      g.rotate(clock * 12 - ang);
      g.setColor(new Color(255, 245, 200));
      g.setStroke(new BasicStroke(4));
      g.drawLine(-22, 0, 22, 0);
      g.setColor(new Color(255, 215, 90));
      g.fill(new Ellipse2D.Double(16, -6, 12, 12));
      g.setColor(glow);
      g.fill(new Ellipse2D.Double(-30, -30, 60, 60));
    }
    else if (q.kind == 7)
    { // Ahri orb
      for (int i = 3; i >= 1; i--)
      {
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35 * i));
        double s = q.r + i * 5;
        g.fill(new Ellipse2D.Double(-s, -s, 2 * s, 2 * s));
      }
      g.setColor(Color.WHITE);
      g.fill(new Ellipse2D.Double(-q.r * 0.45, -q.r * 0.45, q.r * 0.9,
                                  q.r * 0.9));
      g.setColor(c);
      g.setStroke(new BasicStroke(2));
      g.draw(new Ellipse2D.Double(-q.r, -q.r, 2 * q.r, 2 * q.r));
    }
    else if (q.kind == 8)
    { // Ashe's hawk
      double fl = Math.sin(clock * 22) * 8;
      g.setColor(new Color(140, 100, 60));
      g.fill(new Ellipse2D.Double(-8, -4, 18, 8));
      g.setColor(new Color(200, 160, 100));
      Path2D.Double w = new Path2D.Double();
      w.moveTo(0, 0);
      w.lineTo(-10, -18 - fl);
      w.lineTo(-16, 0);
      w.lineTo(-10, 18 + fl);
      w.closePath();
      g.fill(w);
      g.setColor(new Color(255, 220, 80));
      g.fillPolygon(new int[] {10, 16, 10}, new int[] {-2, 0, 2}, 3);
    }
    else
    { // generic glowing orb
      double s = q.big ? 14 : q.r;
      g.setColor(glow);
      g.fill(new Ellipse2D.Double(-s - 5, -s - 5, 2 * s + 10, 2 * s + 10));
      g.setColor(c);
      g.fill(new Ellipse2D.Double(-s, -s, 2 * s, 2 * s));
      g.setColor(Color.WHITE);
      g.fill(new Ellipse2D.Double(-s * 0.4, -s * 0.4, s * 0.8, s * 0.8));
    }
    g.setTransform(old);
  }

  void drawMob(Graphics2D g, Mob m)
  {
    if (!visible(m))
      return;
    g.setColor(new Color(0, 0, 0, 80));
    g.fill(new Ellipse2D.Double(m.x - m.r, m.y + m.r * 0.2, m.r * 2,
                                m.r * 0.9));
    BufferedImage[] set = mobSheet(mobKey(m));
    int anim = 0, frame;
    if (now - m.lastCast < 0.5)
      anim = 3;
    else if (now - m.lastAtk < 0.45)
      anim = 2;
    else if (m.moving)
      anim = 1;
    BufferedImage s = set[anim] != null ? set[anim] : set[0];
    if (anim >= 2)
      frame = (int) ((now - (anim == 3 ? m.lastCast : m.lastAtk)) / 0.45 * 8);
    else if (anim == 1)
      frame = (int) (clock / 0.6 * 8) % 8;
    else
      frame = (int) (clock / 2.0 * 8) % 8;
    if (!drawImg(g, s, frame, dirIdx(m.fa), m.x, m.y + m.r * 0.7,
                 Math.max(64, m.r * 4.6)))
      poly(g, m.x, m.y, m.r, m.sides, m.fa * (m.bush ? 0 : 1),
           m.stunT > now ? Color.WHITE : m.col);
    if (m.shield > 0)
    {
      g.setColor(new Color(200, 200, 255, 110));
      g.fill(new Ellipse2D.Double(m.x - m.r - 4, m.y - m.r - 4, m.r * 2 + 8,
                                  m.r * 2 + 8));
    }
    int bw = (int) (m.r * 2 + 14), by = (int) (m.y - m.r - 22);
    g.setColor(Color.BLACK);
    g.fillRect((int) m.x - bw / 2, by, bw, 6);
    g.setColor(m.bush ? Color.ORANGE : new Color(220, 50, 50));
    g.fillRect((int) m.x - bw / 2, by, (int) (bw * Math.max(0, m.hp / m.maxhp)),
               6);
    if (m.state == 1 && m.boss == 0)
    {
      g.setColor(Color.RED);
      g.setFont(new Font("SansSerif", Font.BOLD, 14));
      g.drawString("!", (int) m.x - 3, by - 3);
    }
    if (m.charmT > now)
      heart(g, m.x, m.y - m.r - 34, 7);
    if (m.stunT > now && m.charmT <= now)
    {
      g.setColor(Color.YELLOW);
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      g.drawString("STUN", (int) m.x - 12, by - 3);
    }
    if (m.boss == 10)
    {
      g.setColor(Color.YELLOW);
      g.setStroke(new BasicStroke(3));
      g.drawLine((int) m.x, (int) m.y, (int) (m.x + Math.cos(m.fa) * m.r * 1.5),
                 (int) (m.y + Math.sin(m.fa) * m.r * 1.5));
    }
  }

  void drawPlayer(Graphics2D g)
  {
    double ang = Math.atan2(mwy() - p.y, mwx() - p.x);
    g.setColor(new Color(255, 255, 255, 50));
    g.setStroke(new BasicStroke(2));
    g.drawLine((int) p.x, (int) p.y, (int) (p.x + Math.cos(ang) * 110),
               (int) (p.y + Math.sin(ang) * 110)); // aim ray
    int anim, frame, dir = faceDir; // pick animation, frame and facing
    if (state == S_DEAD)
    {
      anim = A_DEATH;
      frame = (int) ((clock - deathStart) / 1.2 * 8);
    }
    else if (clock < animEnd)
    {
      anim = animKind;
      frame =
        animLoop ? (int) ((clock - animStart) / 0.55 * 8) % 8
          : (int) ((clock - animStart) / animDur * 8);
      if (champ == 0 && animKind == A_SP + 2 && spinT > now)
        dir = (int) (clock * 14) % 8; // Garen spins during Judgment
    }
    else if (isMoving)
    {
      anim = A_RUN;
      dir = lastMoveDir;
      frame = (int) (clock / (0.7 / Math.max(0.5, sMS / 230.0)) * 8) % 8;
    }
    else
    {
      anim = A_IDLE;
      frame = (int) (clock / 2.0 * 8) % 8;
    }
    g.setColor(new Color(0, 0, 0, 90));
    g.fill(new Ellipse2D.Double(p.x - 16, p.y + 4, 32, 12));
    if (hlT > now)
    {
      g.setColor(new Color(255, 200, 60, 90));
      g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT,
                                  BasicStroke.JOIN_MITER, 10,
                                  new float[] {8, 6}, (float) (clock * 40)));
      g.draw(new Ellipse2D.Double(p.x - 30, p.y - 22, 60, 44));
    }
    if (meditT > now)
    {
      g.setColor(new Color(255, 225, 110,
                           60 + (int) (30 * Math.sin(clock * 6))));
      g.fill(new Ellipse2D.Double(p.x - 36, p.y - 36, 72, 72));
    }
    Composite oc = g.getComposite();
    if (invisT > now)
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                0.15f));
    if (!drawSheet(g, champ, anim, frame, dir, p.x, p.y + 10, SPR_PX))
    { // fallback shape when no art
      g.setColor(new Color(CH_COL[champ].getRed(), CH_COL[champ].getGreen(),
                           CH_COL[champ].getBlue(), 80));
      g.fill(new Ellipse2D.Double(p.x - 24, p.y - 24, 48, 48));
      g.setColor(CH_COL[champ]);
      g.fill(new Ellipse2D.Double(p.x - 16, p.y - 16, 32, 32));
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(2));
      g.draw(new Ellipse2D.Double(p.x - 16, p.y - 16, 32, 32));
      g.setStroke(new BasicStroke(5));
      g.drawLine((int) p.x, (int) p.y, (int) (p.x + Math.cos(ang) * 28),
                 (int) (p.y + Math.sin(ang) * 28));
    }
    g.setComposite(oc);
    if (spinT > now)
      for (int i = 0; i < 3; i++)
      {
        double a = clock * 12 + i * 2.094;
        g.setColor(new Color(255, 225, 120));
        g.setStroke(new BasicStroke(5));
        g.draw(new Arc2D.Double(p.x - 110, p.y - 110, 220, 220,
                                -Math.toDegrees(a) - 20, 40, Arc2D.OPEN));
      }
    if (shield > 0 && shT > now)
    {
      g.setColor(new Color(150, 200, 255, 120));
      g.setStroke(new BasicStroke(3));
      g.draw(new Ellipse2D.Double(p.x - 26, p.y - 26, 52, 52));
    }
    if (thunderT > now)
    {
      g.setColor(new Color(200, 190, 160, 110));
      g.setStroke(new BasicStroke(4));
      g.draw(new Ellipse2D.Double(p.x - 32, p.y - 32, 64, 64));
    }
    if (p.stunT > now)
    {
      g.setColor(Color.YELLOW);
      g.drawString("STUNNED", (int) p.x - 28, (int) p.y - 40);
    }
  }

  void drawWorld(Graphics2D g)
  {
    AffineTransform base = g.getTransform();
    g.scale(ZOOM, ZOOM);
    g.translate(-camX, -camY); // camera + zoom
    double vw = W / ZOOM, vh = H / ZOOM;
    if (mapImg != null)
    {
      int sx = (int) Math.max(0, camX), sy = (int) Math.max(0, camY),
        ex = (int) Math.min(MW * T, camX + vw + 2),
        ey = (int) Math.min(MH * T, camY + vh + 2);
      if (ex > sx && ey > sy)
        g.drawImage(mapImg, sx, sy, ex, ey, sx, sy, ex, ey, null);
    }
    int x0 = Math.max(0, (int) camX / T),
      x1 = Math.min(MW - 1, (int) (camX + vw) / T),
      y0 = Math.max(0, (int) camY / T),
      y1 = Math.min(MH - 1, (int) (camY + vh) / T);
    for (int x = x0; x <= x1; x++)
      for (int y = y0; y <= y1; y++)
        if (door[x][y])
          drawDoor(g, x, y);
    for (Chest c : chests)
      drawChest(g, c);
    if (cleared)
    { // stairs
      g.setColor(new Color(255, 215, 60,
                           60 + (int) (30 * Math.sin(clock * 4))));
      g.fillOval((int) stairX - 40, (int) stairY - 40, 80, 80);
      g.setColor(new Color(255, 215, 60));
      g.setStroke(new BasicStroke(3));
      g.drawRect((int) stairX - 24, (int) stairY - 24, 48, 48);
      for (int k = 0; k < 4; k++)
        g.drawLine((int) stairX - 20, (int) stairY - 14 + k * 9,
                   (int) stairX + 20, (int) stairY - 14 + k * 9);
    }
    for (Mob m : mobs)
      if (m.state == 0 && !m.bush && m.boss == 0 && dist(m) < m.aggro + 320)
      { // aggro zones (dashed red circles)
        g.setColor(new Color(255, 60, 60, 60));
        g.fill(new Ellipse2D.Double(m.hx - m.aggro, m.hy - m.aggro, m.aggro * 2,
                                    m.aggro * 2));
        g.setColor(new Color(255, 80, 80, 130));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER, 10,
                                    new float[] {10, 8}, 0));
        g.draw(new Ellipse2D.Double(m.hx - m.aggro, m.hy - m.aggro, m.aggro * 2,
                                    m.aggro * 2));
      }
    for (Zone z : zones)
    { // ground effects / telegraphs
      int al = now < z.start ? 45 : z.c.getAlpha();
      Color zc = z.c;
      g.setColor(new Color(zc.getRed(), zc.getGreen(), zc.getBlue(), al));
      g.fill(new Ellipse2D.Double(z.x - z.r, z.y - z.r, z.r * 2, z.r * 2));
      g.setColor(new Color(zc.getRed(), zc.getGreen(), zc.getBlue(), 230));
      g.setStroke(new BasicStroke(2));
      g.draw(new Ellipse2D.Double(z.x - z.r, z.y - z.r, z.r * 2, z.r * 2));
      if (z.kind == 2)
      {
        double pu = 0.55 + 0.25 * Math.sin(clock * 8);
        g.setColor(new Color(255, 250, 200, 150));
        g.fill(new Ellipse2D.Double(z.x - z.r * pu * 0.4, z.y - z.r * pu * 0.4,
                                    z.r * pu * 0.8, z.r * pu * 0.8));
      }
      if (z.kind == 1)
      {
        double f = Math.max(0, (z.until - now) / 0.7);
        int sy = (int) (z.y - 20 - 520 * f);
        g.setColor(new Color(255, 240, 160, 90));
        g.fillRect((int) z.x - 22, sy - 600, 44, 600 + 40);
        g.setColor(new Color(215, 225, 235));
        g.fillRect((int) z.x - 9, sy - 60, 18, 120);
        g.setColor(new Color(255, 215, 80));
        g.fillRect((int) z.x - 22, sy + 56, 44, 7);
        g.setColor(Color.WHITE);
        g.drawRect((int) z.x - 9, sy - 60, 18, 120);
      }
    }
    for (Pickup k : pickups)
    {
      if (k.type == 0)
      {
        g.setColor(new Color(255, 215, 60));
        g.fillOval((int) k.x - 7, (int) k.y - 7, 14, 14);
        g.setColor(new Color(255, 245, 160));
        g.fillOval((int) k.x - 3, (int) k.y - 5, 5, 5);
      }
      else
      {
        g.setColor(new Color(255, 150, 190));
        g.fillOval((int) k.x - 7, (int) k.y - 7, 14, 14);
        g.setColor(new Color(120, 200, 90));
        g.fillRect((int) k.x - 1, (int) k.y - 11, 3, 5);
      }
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(1));
      g.drawOval((int) k.x - 7, (int) k.y - 7, 14, 14);
    }
    for (Corpse c : corpses)
    { // death animation / fade-out
      double f = (now - c.born) / 1.4;
      BufferedImage[] set = mobSheet(c.key);
      if (set[4] == null || !drawImg(g, set[4], (int) (f * 8), c.dir, c.x,
                                     c.y + c.r * 0.7, Math.max(64, c.r * 4.6)))
      {
        Composite oc = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  (float) Math.max(0, 1 - f)));
        poly(g, c.x, c.y, c.r * (1 - f * 0.5), c.sides, f * 3, c.c);
        g.setComposite(oc);
      }
    }
    for (Mob m : mobs)
      if (m.y <= p.y)
        drawMob(g, m); // depth sorting around the player
    drawPlayer(g);
    for (Mob m : mobs)
      if (m.y > p.y)
        drawMob(g, m);
    for (Proj q : projs)
      drawProj(g, q);
    for (Beam b : beams)
    {
      float a = (float) Math.max(0, (b.until - now) / b.life);
      g.setColor(new Color(b.c.getRed(), b.c.getGreen(), b.c.getBlue(),
                           (int) (b.c.getAlpha() * a)));
      g.setStroke(new BasicStroke((float) b.w, BasicStroke.CAP_ROUND,
                                  BasicStroke.JOIN_ROUND));
      g.draw(new Line2D.Double(b.x1, b.y1, b.x2, b.y2));
    }
    for (Ring r : rings)
    { // expanding rings / slash arcs
      double f = (now - r.born) / r.dur, rad = r.r0 + (r.r1 - r.r0) * f;
      g.setColor(new Color(r.c.getRed(), r.c.getGreen(), r.c.getBlue(),
                           (int) (200 * (1 - f))));
      g.setStroke(new BasicStroke((float) Math.max(1, r.w * (1 - f * 0.6))));
      if (r.span >= Math.PI * 2 - 0.01)
        g.draw(new Ellipse2D.Double(r.x - rad, r.y - rad, rad * 2, rad * 2));
      else
        g.draw(new Arc2D.Double(r.x - rad, r.y - rad, rad * 2, rad * 2,
                                -Math.toDegrees(r.a0 + r.span),
                                Math.toDegrees(r.span), Arc2D.OPEN));
    }
    for (Part q : parts)
    {
      double f = (now - q.born) / q.life;
      g.setColor(new Color(q.c.getRed(), q.c.getGreen(), q.c.getBlue(),
                           (int) (220 * (1 - f))));
      double s = q.size * (1 - f * 0.5);
      g.fill(new Ellipse2D.Double(q.x - s, q.y - s, s * 2, s * 2));
    }
    drawTelegraph(g);
    Mob hov = lockTarget(mwx(), mwy());
    double wx = mwx(), wy = mwy();
    if (hov != null)
    { // target lock reticle
      double rr = hov.r + 12 + Math.sin(now * 8) * 2;
      g.setColor(new Color(255, 255, 120));
      g.setStroke(new BasicStroke(3));
      for (int i = 0; i < 4; i++)
      {
        double a = now * 2 + i * Math.PI / 2;
        g.draw(new Arc2D.Double(hov.x - rr, hov.y - rr, rr * 2, rr * 2,
                                -Math.toDegrees(a) - 25, 50, Arc2D.OPEN));
      }
    }
    g.setColor(new Color(255, 255, 255, 210));
    g.setStroke(new BasicStroke(2));
    g.draw(new Ellipse2D.Double(wx - 9, wy - 9, 18, 18));
    g.drawLine((int) wx - 14, (int) wy, (int) wx + 14, (int) wy);
    g.drawLine((int) wx, (int) wy - 14, (int) wx, (int) wy + 14);
    for (FText f : ftexts)
    {
      double up = (1 - (f.until - now)) * 30;
      g.setFont(new Font("SansSerif", Font.BOLD, 16));
      g.setColor(Color.BLACK);
      g.drawString(f.s, (int) f.x + 1, (int) (f.y - up) + 1);
      g.setColor(f.c);
      g.drawString(f.s, (int) f.x, (int) (f.y - up));
    }
    g.setTransform(base);
  }

  void addBuff(List<Object[]> l, String n, double until, double total, Color c)
  {
    if (until > now)
      l.add(new Object[] {n, until - now, Math.max(total, until - now), c});
  }

  void drawHud(Graphics2D g)
  {
    g.setColor(new Color(0, 0, 0, 170));
    g.fillRect(0, 0, W, 40); // ----- top bar -----
    g.setColor(Color.YELLOW);
    g.fillOval(14, 11, 18, 18);
    txt(g, String.format("%,d Gold", gold), 40, 28, 18, Color.YELLOW);
    diamond(g, 200, 21, 9, new Color(230, 40, 90));
    txt(g, "" + rubies + " Rubies", 214, 28, 16, new Color(255, 120, 150));
    ctr(g, "Floor " + floor + " - " + floorName(), 28, 20, Color.WHITE);
    txt(g, "Lv " + level + "  XP " + xp + "/" + (60 + level * 45), W - 220, 28,
        16, Color.CYAN);
    for (Mob m : mobs)
      if (m.boss != 0 && !m.name.startsWith("Mini") && m.state == 1)
      {
        bar(g, W / 2 - 250, 50, 500, 18, m.hp / m.maxhp, new Color(200, 40, 60),
            m.name
              + (m.elem > 0
                ? " (" + new String[] {"Infernal", "Ocean", "Mountain",
                                       "Cloud"}[m.elem - 1]
                  + ")"
                : ""));
        break;
      }
    int sx = (W - 376) / 2, sy = H - 84;
    // ----- barrier bar (above health) -----
    if (shield > 0 && (shT > now || champ == 7))
    {
      double left = shT - now;
      String lbl =
        "Barrier " + (int) shield
          + (left < 100 ? String.format("  (%.1fs)", left) : "");
      bar(g, sx, sy - 76, 376, 14, Math.min(1, shield / maxHp),
          new Color(170, 210, 255), lbl);
    }
    bar(g, sx, sy - 52, 376, 22, p.hp / maxHp, new Color(40, 190, 70),
        (int) p.hp + " / " + (int) maxHp);
    bar(g, sx, sy - 26, 376, 18, mana / maxMana, new Color(50, 110, 230),
        (int) mana + " / " + (int) maxMana);
    // ----- active effects with remaining time -----
    List<Object[]> bl = new ArrayList<>();
    addBuff(bl, "Highlander", hlT, 7, new Color(255, 200, 60));
    addBuff(bl, "Meditate", meditT, 4, new Color(255, 225, 110));
    addBuff(bl, "Judgment", spinT, 3, new Color(255, 215, 80));
    addBuff(bl, "Wuju Style", wujuT, 6, new Color(90, 255, 200));
    addBuff(bl, "Ranger's Focus", focusT, 5, new Color(150, 220, 255));
    addBuff(bl, "Decisive Strike", qNextT, 5, new Color(255, 160, 80));
    addBuff(bl, "Courage (damage reduction)", drT, 2.5,
            new Color(150, 200, 255));
    addBuff(bl, "Speed boost", spdT, 3, new Color(120, 255, 160));
    addBuff(bl, "Attack speed", asT, 4, new Color(255, 150, 120));
    addBuff(bl, "Thunderclap", thunderT, 5, new Color(200, 190, 160));
    addBuff(bl, "Twilight Shroud", smokeT, 4, new Color(170, 110, 230));
    addBuff(bl, "Assassin's Mark", akPassT, 4, new Color(210, 120, 255));
    if (champ == 4 && akRT > now)
      addBuff(bl, "Recast R", akRT, 3, new Color(255, 80, 120));
    if (champ == 6 && ahriRC > 0 && ahriRT > now)
      addBuff(bl, "Spirit Rush x" + ahriRC, ahriRT, 10,
              new Color(255, 150, 220));
    if (champ == 2 && luxOrb != null && zones.contains(luxOrb))
      addBuff(bl, "Singularity (recast 3)", luxOrb.until, 4,
              new Color(255, 245, 160));
    addBuff(bl, "Tenacity", tenT, 3, new Color(200, 200, 200));
    addBuff(bl, "Invulnerable", invulnT, 1, Color.WHITE);
    addBuff(bl, "Anti-heal", healCutT, 3, new Color(255, 90, 90));
    addBuff(bl, "Slowed", p.slowT, 1, new Color(120, 160, 255));
    addBuff(bl, "Stunned", p.stunT, 1.5, Color.YELLOW);
    addBuff(bl, "Burning", pBurnT, 3, new Color(255, 120, 40));
    if (champ == 1 && focusT <= now && focusStacks > 0)
      bl.add(new Object[] {"Focus " + focusStacks + "/4", focusStacks * 1.0,
                           4.0, new Color(150, 220, 255)});
    for (int i = 0; i < bl.size() && i < 9; i++)
    {
      Object[] b = bl.get(i);
      Color c = (Color) b[3];
      double rem = (Double) b[1], tot = (Double) b[2];
      int cy = H - 34 - i * 22;
      g.setColor(new Color(0, 0, 0, 170));
      g.fillRoundRect(16, cy, 200, 18, 6, 6);
      g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
      g.fillRoundRect(16, cy, (int) (200 * Math.min(1, rem / tot)), 18, 6, 6);
      g.setColor(Color.WHITE);
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      String t =
        ((String) b[0]).startsWith("Focus ") ? (String) b[0]
          : (String) b[0] + String.format("  %.1fs", rem);
      g.drawString(t, 22, cy + 13);
    }
    // ----- skill slots -----
    int hoverSlot = -1;
    for (int i = 0; i < 6; i++)
    {
      final int slot = i;
      Rectangle r = new Rectangle(sx + i * 64, sy, 56, 56);
      if (r.contains(mx, my))
        hoverSlot = i;
      g.setColor(new Color(20, 20, 28));
      g.fillRoundRect(r.x, r.y, 56, 56, 10, 10);
      g.setColor(SLOT_COL[i]);
      g.fillRoundRect(r.x + 4, r.y + 4, 48, 48, 8, 8);
      g.setColor(i == aim ? Color.WHITE : Color.GRAY);
      g.setStroke(new BasicStroke(i == aim ? 4 : 2));
      g.drawRoundRect(r.x, r.y, 56, 56, 10, 10);
      String nm = i < 4 ? SK[champ][i] : SUM[sum[i - 4]];
      g.setFont(new Font("SansSerif", Font.BOLD, 10));
      g.setColor(Color.BLACK);
      g.drawString(nm.length() > 9 ? nm.substring(0, 9) : nm, r.x + 5,
                   r.y + 32);
      txt(g, "" + (i + 1), r.x + 6, r.y + 15, 13, Color.WHITE);
      if (i < 4 && recastable(i))
      {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(r.x + 1, r.y + 1, 54, 54, 9, 9);
        txt(g, "RECAST", r.x + 7, r.y + 46, 10, Color.WHITE);
      }
      else if (now < cds[i])
      {
        double f = Math.min(1, (cds[i] - now) / Math.max(0.1, cdTot[i]));
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRoundRect(r.x + 2, r.y + 2, 52, (int) (52 * f), 8, 8);
        txt(g, String.format("%.1f", cds[i] - now), r.x + 14, r.y + 36, 14,
            Color.WHITE);
      }
      if (i < 4)
      {
        for (int k = 0; k < rank[i]; k++)
        {
          g.setColor(Color.YELLOW);
          g.fillRect(r.x + 4 + k * 12, r.y + 60, 9, 4);
        }
        if (canUp(i))
        {
          Rectangle pb = new Rectangle(r.x + 36, r.y - 20, 20, 18);
          button(g, pb, "+", new Color(200, 160, 30), () -> {
            if (canUp(slot))
            {
              rank[slot]++;
              points--;
            }
          });
        }
      }
    }
    if (points > 0)
      txt(g, "Skill points: " + points, sx, sy - 82, 14, Color.YELLOW);
    if (champ == 1)
      for (int k = 0; k < 4; k++)
      {
        g.setColor(k < focusStacks ? new Color(150, 220, 255)
          : new Color(40, 60, 80));
        g.fillOval(sx + 6 + k * 12, sy - 8, 9, 9);
      } // Ashe focus pips
    for (int i = 0; i < 6; i++)
    {
      g.setColor(new Color(0, 0, 0, 150));
      g.fillRect(sx + 390 + (i % 3) * 70, sy + (i / 3) * 28, 66, 24);
      if (inv[i] != null)
      {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(Color.WHITE);
        g.drawString(inv[i].n.length() > 11 ? inv[i].n.substring(0, 11)
          : inv[i].n, sx + 393 + (i % 3) * 70, sy + 16 + (i / 3) * 28);
      }
    }
    if (hoverSlot >= 0)
    { // skill tooltip
      String title, body, info;
      if (hoverSlot < 4)
      {
        title = SK[champ][hoverSlot] + "  (rank " + rank[hoverSlot] + ")";
        body = DESC[champ][hoverSlot];
        info =
          "Cooldown " + (int) CD[champ][hoverSlot] + "s   Mana "
            + MANA[champ][hoverSlot];
      }
      else
      {
        int s = sum[hoverSlot - 4];
        title = SUM[s];
        body = SUM_DESC[s];
        info = "Cooldown " + (int) SUM_CD[s] + "s";
      }
      int tw = 380, ty = sy - 230;
      g.setColor(new Color(15, 15, 25, 240));
      g.fillRoundRect(sx - 2, ty, tw, 130, 10, 10);
      g.setColor(SLOT_COL[hoverSlot]);
      g.setStroke(new BasicStroke(2));
      g.drawRoundRect(sx - 2, ty, tw, 130, 10, 10);
      txt(g, title, sx + 10, ty + 24, 16, Color.WHITE);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      g.setColor(Color.LIGHT_GRAY);
      drawWrapped(g, body, sx + 10, ty + 46, tw - 24);
      txt(g, info, sx + 10, ty + 120, 12, new Color(150, 200, 255));
    }
    if (now < msgT)
      ctr(g, msg, 100, 22, Color.ORANGE);
    String prompt = null;
    for (Chest c : chests)
      if (!c.open && Math.hypot(c.x - p.x, c.y - p.y) < 70)
        prompt = "E: Open chest";
    if (prompt == null && nearDoor())
      prompt = "E: Open vault door";
    if (prompt == null && cleared
      && Math.hypot(p.x - stairX, p.y - stairY) < 60)
      prompt = floor >= 50 ? null : "E: Next Floor     V: Return to Village";
    if (prompt != null)
      ctr(g, prompt, H - 160, 22, Color.GREEN);
    if (showStats)
      drawStats(g);
    txt(g,
        "WASD move | LMB attack | 1-6 spells (double-tap = quick cast, tap+click = aim) | RMB cancel | C stats | E interact",
        10, H - 8, 11, new Color(255, 255, 255, 120));
  }

  void drawEnd(Graphics2D g)
  {
    g.setColor(new Color(0, 0, 0, 190));
    g.fillRect(0, 0, W, H);
    if (state == S_WIN)
    {
      ctr(g, "VICTORY!", 280, 72, new Color(255, 215, 60));
      ctr(g, "The Elder Dragon has fallen.", 340, 26, Color.WHITE);
    }
    else
    {
      ctr(g, "YOU DIED", 280, 72, new Color(220, 50, 50));
      ctr(g, "Reached Floor " + floor + " - perma-death reset", 340, 26,
          Color.WHITE);
    }
    ctr(g,
        "Rubies earned this run: " + runRubies + "     Total rubies: " + rubies,
        395, 24, new Color(255, 120, 150));
    ctr(g, "Spend rubies on new champions at the champion select screen", 430,
        18, Color.LIGHT_GRAY);
    ctr(g, "Press ENTER to start a new run", 480, 22, Color.LIGHT_GRAY);
  }

  // ===================== INPUT =====================
  @Override
  public void keyPressed(KeyEvent e)
  {
    int k = e.getKeyCode();
    if (k >= 512 || keys[k])
      return;
    keys[k] = true; // ignore auto-repeat
    if (state == S_CHAMP)
    {
      if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_8)
      {
        int i = k - KeyEvent.VK_1;
        if (unlocked[i])
        {
          champ = i;
          state = S_SPELL;
        }
        else
        {
          menuMsg =
            CH[i] + " is locked - unlock it with " + PRICE[i] + " rubies";
          menuMsgT = clock + 2.5;
        }
      }
      return;
    }
    if (state == S_SPELL)
    {
      if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_7)
        toggleSum(k - KeyEvent.VK_1);
      if (k == KeyEvent.VK_ENTER)
        startFromSelect();
      return;
    }
    if ((state == S_DEAD || state == S_WIN) && k == KeyEvent.VK_ENTER)
    {
      state = S_CHAMP;
      return;
    }
    if (state == S_VILLAGE && k == KeyEvent.VK_ENTER)
    {
      diveFromVillage();
      return;
    }
    if (state != S_PLAY)
      return;
    if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_6)
      tap(k - KeyEvent.VK_1);
    if (k == KeyEvent.VK_C)
      showStats = !showStats;
    if (k == KeyEvent.VK_ESCAPE)
      aim = -1;
    if (k == KeyEvent.VK_E && interact())
      return; // chests and doors first
    if (cleared && Math.hypot(p.x - stairX, p.y - stairY) < 60)
    {
      if (k == KeyEvent.VK_E && floor < 50)
      {
        floor++;
        genFloor();
      }
      if (k == KeyEvent.VK_V)
        state = S_VILLAGE;
    }
  }

  @Override
  public void mousePressed(MouseEvent e)
  {
    mx = e.getX();
    my = e.getY();
    if (state == S_PLAY)
    {
      if (SwingUtilities.isRightMouseButton(e))
      {
        aim = -1;
        return;
      }
      if (clickBtn())
        return;
      if (aim >= 0)
      {
        enqueue(aim, mwx(), mwy());
        aim = -1;
      }
      else
        hold = true;
    }
    else if (state == S_VILLAGE && SwingUtilities.isRightMouseButton(e))
    {
      for (int i = 0; i < 6; i++)
        if (new Rectangle(40 + i * 170, 590, 160, 50).contains(mx, my)
          && inv[i] != null)
        {
          gold += inv[i].cost / 2;
          inv[i] = null;
          recalc();
        }
    }
    else
      clickBtn();
  }

  public static void main(String[] a)
  {
    SwingUtilities.invokeLater(() -> {
      JFrame f = new JFrame("Rift Rogue"); // create window
      RiftRogue g = new RiftRogue();
      f.add(g);
      f.pack();
      f.setResizable(false);
      f.setLocationRelativeTo(null);
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.setVisible(true);
      g.requestFocusInWindow();
    });
  }

  RiftRogue()
  {
    setPreferredSize(new Dimension(W, H));
    setFocusable(true);
    addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
    loadSprites();
    loadSave(); // try to load assets/*.png (falls back to shapes)
    new javax.swing.Timer(16, e -> { // ~60 FPS game loop
      long n = System.nanoTime();
      double dt = Math.min(0.033, (n - last) / 1e9);
      last = n;
      update(dt);
      repaint();
    }).start();
  }

  boolean has(String n)
  {
    for (Item i : inv)
      if (i != null && i.n.equals(n))
        return true;
    return false;
  }

  boolean canForge(Item l)
  { // do we own the components and the gold?
    if (gold < l.cost)
      return false;
    List<Item> tmp = new ArrayList<>(Arrays.asList(inv));
    for (String r : l.rec)
    {
      boolean f = false;
      for (int i = 0; i < tmp.size(); i++)
        if (tmp.get(i) != null && tmp.get(i).n.equals(r))
        {
          tmp.set(i, null);
          f = true;
          break;
        }
      if (!f)
        return false;
    }
    return true;
  }

  void forge(Item l)
  { // consume components + gold, add legendary item
    if (!canForge(l))
      return;
    for (String r : l.rec)
      for (int i = 0; i < 6; i++)
        if (inv[i] != null && inv[i].n.equals(r))
        {
          inv[i] = null;
          break;
        }
    for (int i = 0; i < 6; i++)
      if (inv[i] == null)
      {
        inv[i] = l;
        break;
      }
    gold -= l.cost;
    recalc();
  }

  void buy(Item it)
  {
    if (gold < it.cost)
      return;
    for (int i = 0; i < 6; i++)
      if (inv[i] == null)
      {
        inv[i] = it;
        gold -= it.cost;
        recalc();
        return;
      }
    say("Inventory full!");
  }

  String desc(Item it)
  { // short stat string
    StringBuilder sb = new StringBuilder();
    for (int k = 0; k < 10; k++)
      if (it.s[k] != 0)
        sb.append(STAT_LBL[k]).append(" +").append((int) it.s[k]).append("  ");
    return sb.toString();
  }

  static int dirIdx(double ang)
  {
    return ((int) Math.round(ang / (Math.PI / 4)) % 8 + 8) % 8;
  } // angle -> 0..7 (E,SE,S,SW,W,NW,N,NE)

  void say(String s)
  {
    msg = s;
    msgT = now + 2.5;
  }

  void ftext(double x, double y, String s, Color c)
  {
    FText f = new FText();
    f.x = x + rnd.nextInt(20) - 10;
    f.y = y;
    f.s = s;
    f.c = c;
    f.until = now + 1.0;
    ftexts.add(f);
  }

  static double angDiff(double a, double b)
  {
    double d = a - b;
    while (d > Math.PI)
      d -= 2 * Math.PI;
    while (d < -Math.PI)
      d += 2 * Math.PI;
    return d;
  }

  double dist(Ent e)
  {
    return Math.hypot(e.x - p.x, e.y - p.y);
  }

  boolean blocked(double x, double y, double r)
  { // circle vs. wall tiles
    for (int i = -1; i <= 1; i += 2)
      for (int j = -1; j <= 1; j += 2)
        if (isWall((int) Math.floor((x + i * r) / T),
                   (int) Math.floor((y + j * r) / T)))
          return true;
    return false;
  }

  void move(Ent e, double dx, double dy)
  {
    if (!blocked(e.x + dx, e.y, Math.min(e.r, 15)))
      e.x += dx;
    if (!blocked(e.x, e.y + dy, Math.min(e.r, 15)))
      e.y += dy;
  } // axis-separated

  static double segDist(double px, double py, double x1, double y1, double x2,
                        double y2)
  { // point-to-segment distance
    double dx = x2 - x1, dy = y2 - y1, l = dx * dx + dy * dy,
      t =
        l == 0 ? 0
          : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / l));
    return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
  }

  boolean visible(Mob m)
  { // fog of war: nearby or inside a Hawkshot reveal
    if (dist(m) < 560)
      return true;
    for (Zone z : zones)
      if (z.reveal && Math.hypot(z.x - m.x, z.y - m.y) < z.r)
        return true;
    return false;
  }

  Mob lockTarget(double wx, double wy)
  { // monster nearest the cursor (target lock)
    Mob best = null;
    double bd = 1e9;
    for (Mob m : mobs)
    {
      if (m.bush || !visible(m))
        continue;
      double d = Math.hypot(m.x - wx, m.y - wy);
      if (d < m.r + 45 && d < bd)
      {
        bd = d;
        best = m;
      }
    }
    return best;
  }

  void addShield(double a, double dur)
  {
    shield += a;
    shT = Math.max(shT, now + dur);
  }

  void heal(double a)
  { // healing, reduced by anti-heal; Bloodthirster turns overheal into shield
    if (healCutT > now)
      a *= 0.4;
    if (has("Bloodthirster") && p.hp + a > maxHp)
      addShield(Math.min(a - (maxHp - p.hp), 150), 3);
    p.hp = Math.min(maxHp, p.hp + a);
  }

  void stunPlayer(double d)
  {
    if (tenT > now)
      d *= 0.7;
    p.stunT = Math.max(p.stunT, now + d);
  }

  void mark(Mob m)
  {
    if (champ == 2)
      m.markT = now + 4;
  } // Lux illumination mark

  Zone nz(double x, double y, double r, double dur, Color c, boolean friendly)
  {
    Zone z = new Zone();
    z.x = x;
    z.y = y;
    z.r = r;
    z.start = now;
    z.until = now + dur;
    z.c = c;
    z.friendly = friendly;
    z.next = now;
    return z;
  }

  Proj pj(double ang, double spd, double range, double r, double dmg, int type,
          Color c)
  { // friendly projectile from the player
    Proj q = new Proj();
    q.x = p.x;
    q.y = p.y;
    q.vx = Math.cos(ang) * spd;
    q.vy = Math.sin(ang) * spd;
    q.life = range / spd;
    q.r = r;
    q.dmg = dmg;
    q.type = type;
    q.c = c;
    q.friendly = true;
    projs.add(q);
    return q;
  }

  void eproj(Mob m, double ang, double spd, double dmg, int type, Color c,
             int fl, double dot)
  { // enemy projectile
    Proj q = new Proj();
    q.x = m.x;
    q.y = m.y;
    q.vx = Math.cos(ang) * spd;
    q.vy = Math.sin(ang) * spd;
    q.life = 3;
    q.r = 10;
    q.dmg = dmg;
    q.type = type;
    q.c = c;
    q.flags = fl;
    q.dot = dot;
    q.src = m;
    projs.add(q);
  }

  void beam(double x1, double y1, double x2, double y2, double w, double dur,
            Color c)
  {
    Beam b = new Beam();
    b.x1 = x1;
    b.y1 = y1;
    b.x2 = x2;
    b.y2 = y2;
    b.w = w;
    b.life = dur;
    b.until = now + dur;
    b.c = c;
    beams.add(b);
  }

  boolean canUp(int i)
  { // skill-point rules from the spec
    if (points <= 0)
      return false;
    if (i < 3)
      return level >= i + 1 && rank[i] < 4 && rank[i] < (level + 1) / 2 + 1;
    return level >= 5 && rank[3] < 3
      && rank[3] < (level >= 13 ? 3 : level >= 9 ? 2 : 1);
  }

  void tap(int slot)
  { // hotkey press: double-tap = quick cast, single = aim mode
    if (state != S_PLAY)
      return;
    double wx = mwx(), wy = mwy();
    if (now - lastTap[slot] < 0.3)
    {
      enqueue(slot, wx, wy);
      aim = -1;
      lastTap[slot] = -9;
    }
    else
    {
      aim = slot;
      lastTap[slot] = now;
    }
  }

  void enqueue(int slot, double wx, double wy)
  {
    if (queue.size() < 6)
      queue.add(new double[] {slot, wx, wy});
  }

  void processQueue()
  { // FIFO queue with isCasting lock (no animation canceling)
    if (isCasting && now >= castEnd)
      isCasting = false;
    if (isCasting || queue.isEmpty() || p.stunT > now)
      return;
    double[] q = queue.poll();
    castLock = 0;
    if (cast((int) q[0], q[1], q[2]))
    {
      isCasting = true;
      castEnd = now + (castLock > 0 ? castLock : 0.2);
    }
  }

  boolean castSummoner(int id, double tx, double ty)
  {
    double ang = Math.atan2(ty - p.y, tx - p.x);
    switch (id)
    {
      case 0:
      { // Flash
        double d = Math.min(Math.hypot(tx - p.x, ty - p.y), SUM_RNG[0]);
        for (double k = d; k > 0; k -= 8)
        {
          double nx = p.x + Math.cos(ang) * k, ny = p.y + Math.sin(ang) * k;
          if (!blocked(nx, ny, p.r))
          {
            beam(p.x, p.y, nx, ny, 6, 0.2, Color.WHITE);
            p.x = nx;
            p.y = ny;
            return true;
          }
        }
        return false;
      }
      case 1:
      {
        Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > SUM_RNG[1])
        {
          say("No target locked");
          return false;
        }
        t.burnT = now + 5;
        t.burnDps = (50 + 20 * level) / 5.0;
        t.healCutT = now + 5;
        return true;
      } // burn + 60% anti-heal
      case 2:
        p.stunT = p.rootT = p.slowT = p.silT = 0;
        pBurnT = 0;
        tenT = now + 3;
        return true; // Cleanse
      case 3:
        spdT = now + 3;
        spdA = 0.4;
        return true; // Sprint
      case 4:
        addShield(100 + 25 * level, 2);
        return true; // Barrier
      case 5:
        heal(90 + 20 * level);
        spdT = now + 2;
        spdA = 0.3;
        return true; // Heal
      default:
      {
        Mob t = lockTarget(tx, ty);
        if (t == null || dist(t) > SUM_RNG[6])
        {
          say("No target locked");
          return false;
        }
        t.exhT = now + 3;
        t.slowT = now + 3;
        t.slowA = 0.3;
        return true;
      } // Exhaust
    }
  }

  void button(Graphics2D g, Rectangle r, String text, Color c, Runnable a)
  { // draw + register a clickable button
    boolean hov = r.contains(mx, my);
    g.setColor(hov ? c.brighter() : c);
    g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
    g.setColor(Color.WHITE);
    g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
    g.setFont(new Font("SansSerif", Font.BOLD, 14));
    FontMetrics fm = g.getFontMetrics();
    g.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2,
                 r.y + (r.height + fm.getAscent()) / 2 - 3);
    btns.add(new Object[] {r, a});
  }

  void txt(Graphics2D g, String s, int x, int y, int size, Color c)
  {
    g.setFont(new Font("SansSerif", Font.BOLD, size));
    g.setColor(c);
    g.drawString(s, x, y);
  }

  void ctr(Graphics2D g, String s, int y, int size, Color c)
  {
    g.setFont(new Font("SansSerif", Font.BOLD, size));
    g.setColor(c);
    g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y);
  }

  void bar(Graphics2D g, int x, int y, int w, int h, double f, Color c,
           String label)
  { // generic bar with text
    g.setColor(new Color(0, 0, 0, 170));
    g.fillRect(x, y, w, h);
    g.setColor(c);
    g.fillRect(x, y, (int) (w * Math.max(0, Math.min(1, f))), h);
    g.setColor(Color.WHITE);
    g.drawRect(x, y, w, h);
    if (label != null)
    {
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.drawString(label, x + w / 2 - g.getFontMetrics().stringWidth(label) / 2,
                   y + h - 5);
    }
  }

  void poly(Graphics2D g, double x, double y, double r, int sides, double rot,
            Color c)
  { // glowing polygon (monster shape)
    Path2D.Double pa = new Path2D.Double();
    for (int i = 0; i < sides; i++)
    {
      double a = rot + i * 2 * Math.PI / sides;
      double px = x + Math.cos(a) * r, py = y + Math.sin(a) * r;
      if (i == 0)
        pa.moveTo(px, py);
      else
        pa.lineTo(px, py);
    }
    pa.closePath();
    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
    g.setStroke(new BasicStroke(9));
    g.draw(pa); // glow
    g.setColor(c);
    g.fill(pa);
    g.setColor(Color.WHITE);
    g.setStroke(new BasicStroke(2));
    g.draw(pa);
  }

  @Override
  protected void paintComponent(Graphics g0)
  {
    Graphics2D g = (Graphics2D) g0;
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(new Color(12, 12, 16));
    g.fillRect(0, 0, W, H);
    btns.clear();
    switch (state)
    {
      case S_CHAMP:
        drawChamp(g);
        break;
      case S_SPELL:
        drawSpell(g);
        break;
      case S_VILLAGE:
        drawVillage(g);
        break;
      default:
        drawWorld(g);
        drawHud(g);
        if (state != S_PLAY)
          drawEnd(g);
    }
  }

  void drawWrapped(Graphics2D g, String s, int x, int y, int w)
  { // simple word wrap
    FontMetrics fm = g.getFontMetrics();
    String line = "";
    for (String word : s.split(" "))
    {
      if (fm.stringWidth(line + word) > w)
      {
        g.drawString(line, x, y);
        y += 16;
        line = "";
      }
      line += word + " ";
    }
    g.drawString(line, x, y);
  }

  void drawSpell(Graphics2D g)
  { // summoner spell selection
    ctr(g, CH[champ] + " - choose 2 Summoner Spells (keys 5 & 6)", 90, 30,
        CH_COL[champ]);
    int picked = 0;
    for (boolean b : sumPick)
      if (b)
        picked++;
    for (int i = 0; i < 7; i++)
    {
      final int k = i;
      Rectangle r =
        new Rectangle(190 + (i % 4) * 220, 170 + (i / 4) * 130, 200, 100);
      g.setColor(sumPick[i] ? new Color(60, 90, 40) : new Color(30, 30, 40));
      g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
      g.setColor(sumPick[i] ? Color.GREEN : Color.GRAY);
      g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
      txt(g, (i + 1) + ". " + SUM[i], r.x + 14, r.y + 36, 20, Color.WHITE);
      g.setFont(new Font("SansSerif", Font.PLAIN, 13));
      g.setColor(Color.LIGHT_GRAY);
      drawWrapped(g, SUM_DESC[i], r.x + 14, r.y + 62, 175);
      btns.add(new Object[] {r, (Runnable) () -> toggleSum(k)});
    }
    if (picked == 2)
      button(g, new Rectangle(540, 470, 200, 60), "START RUN (Enter)",
             new Color(40, 140, 60), this::startFromSelect);
    button(g, new Rectangle(40, 640, 120, 40), "< Back", new Color(70, 70, 80),
           () -> state = S_CHAMP);
  }

  void toggleSum(int k)
  {
    int picked = 0;
    for (boolean b : sumPick)
      if (b)
        picked++;
    if (sumPick[k])
      sumPick[k] = false;
    else if (picked < 2)
      sumPick[k] = true;
  }

  void startFromSelect()
  {
    int n = 0;
    for (int i = 0; i < 7; i++)
      if (sumPick[i] && n < 2)
        sum[n++] = i;
    if (n == 2)
      newRun();
  }

  void drawVillage(Graphics2D g)
  { // shop + blacksmith screen
    ctr(g, "VILLAGE", 50, 36, new Color(255, 215, 80));
    txt(g, "Gold: " + gold, 40, 50, 22, Color.YELLOW);
    diamond(g, 260, 42, 9, new Color(230, 40, 90));
    txt(g, "Rubies: " + rubies, 275, 50, 20, new Color(255, 120, 150));
    txt(g, "ITEM SHOP (click to buy)", 40, 85, 18, Color.WHITE);
    txt(g, "BLACKSMITH FORGE (needs components + gold)", 500, 85, 18,
        Color.WHITE);
    for (int i = 0; i < BASEI.size(); i++)
    {
      final Item it = BASEI.get(i);
      Rectangle r = new Rectangle(40, 95 + i * 56, 420, 50);
      g.setColor(gold >= it.cost ? new Color(40, 60, 40)
        : new Color(40, 30, 30));
      g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      g.setColor(r.contains(mx, my) ? Color.WHITE : Color.GRAY);
      g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      txt(g, it.n + "  (" + it.cost + "g)", r.x + 10, r.y + 22, 15,
          Color.WHITE);
      txt(g, desc(it), r.x + 10, r.y + 41, 13, Color.LIGHT_GRAY);
      btns.add(new Object[] {r, (Runnable) () -> buy(it)});
    }
    for (int i = 0; i < LEG.size(); i++)
    {
      final Item it = LEG.get(i);
      Rectangle r = new Rectangle(500, 95 + i * 48, 530, 44);
      boolean ok = canForge(it);
      g.setColor(ok ? new Color(40, 80, 50) : new Color(35, 35, 45));
      g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      g.setColor(r.contains(mx, my) ? Color.WHITE
        : ok ? Color.GREEN : Color.GRAY);
      g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      txt(g, it.n + "  (" + it.cost + "g)  = " + String.join(" + ", it.rec),
          r.x + 10, r.y + 18, 13, Color.WHITE);
      txt(g, desc(it), r.x + 10, r.y + 36, 12, new Color(255, 215, 120));
      btns.add(new Object[] {r, (Runnable) () -> forge(it)});
    }
    txt(g, "INVENTORY (right-click to sell for 50%)", 40, 580, 16, Color.WHITE);
    for (int i = 0; i < 6; i++)
    {
      Rectangle r = new Rectangle(40 + i * 170, 590, 160, 50);
      g.setColor(new Color(30, 30, 40));
      g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      g.setColor(Color.GRAY);
      g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      if (inv[i] != null)
      {
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        drawWrapped(g, inv[i].n, r.x + 8, r.y + 20, 145);
      }
    }
    txt(g, "Level " + level + "   Floor reached: " + floor, 1060, 120, 16,
        Color.LIGHT_GRAY);
    button(g, new Rectangle(1060, 600, 200, 70), "DIVE: Floor " + (floor + 1),
           new Color(40, 140, 60), this::diveFromVillage);
    if (System.nanoTime() / 1000000 % 1000 < 0)
      g.drawString("", 0, 0);
    if (now < msgT)
      ctr(g, msg, 700, 18, Color.ORANGE);
  }

  void diveFromVillage()
  { // leave village: full heal, next floor
    floor++;
    recalc();
    p.hp = maxHp;
    mana = maxMana;
    genFloor();
    state = S_PLAY;
  }

  void drawTelegraph(Graphics2D g)
  { // aim-mode range indicators
    if (aim < 0)
      return;
    int at, rg, sz;
    if (aim < 4)
    {
      at = AIM[champ][aim];
      rg = RNG[champ][aim];
      sz = SZ[champ][aim];
    }
    else
    {
      int s = sum[aim - 4];
      at = SUM_AIM[s];
      rg = SUM_RNG[s];
      sz = 30;
    }
    double wx = mwx(), wy = mwy(), ang = Math.atan2(wy - p.y, wx - p.x),
      d = Math.hypot(wx - p.x, wy - p.y);
    Color c =
      new Color(SLOT_COL[aim].getRed(), SLOT_COL[aim].getGreen(),
                SLOT_COL[aim].getBlue(), 90);
    g.setColor(c);
    AffineTransform old = g.getTransform();
    switch (at)
    {
      case 0:
        g.translate(p.x, p.y);
        g.rotate(ang);
        g.fill(new Rectangle2D.Double(0, -sz / 2.0, rg, sz));
        g.setTransform(old);
        break; // line
      case 1:
      {
        double k = Math.min(d, rg);
        double cx = p.x + Math.cos(ang) * k, cy = p.y + Math.sin(ang) * k;
        g.fill(new Ellipse2D.Double(cx - sz, cy - sz, sz * 2, sz * 2));
        g.draw(new Ellipse2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2));
        break;
      } // circle at cursor
      case 2:
        g.fill(new Arc2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2,
                                -Math.toDegrees(ang) - sz / 2.0, sz,
                                Arc2D.PIE));
        break; // cone
      case 3:
      {
        int rr = sz > 0 ? sz : 70;
        g.fill(new Ellipse2D.Double(p.x - rr, p.y - rr, rr * 2, rr * 2));
        break;
      } // self ring
      default:
        g.setStroke(new BasicStroke(2));
        g.draw(new Ellipse2D.Double(p.x - rg, p.y - rg, rg * 2, rg * 2)); // target
                                                                          // range
    }
  }

  void drawStats(Graphics2D g)
  { // 'C' stats panel
    g.setColor(new Color(0, 0, 0, 215));
    g.fillRoundRect(20, 60, 260, 290, 14, 14);
    g.setColor(Color.WHITE);
    g.drawRoundRect(20, 60, 260, 290, 14, 14);
    String[] l =
      {"AD: " + (int) sAD, "AP: " + (int) sAP, "Armor: " + (int) sArm,
       "Magic Resist: " + (int) sMR, String.format("Attack Speed: %.2f", sAS),
       "Move Speed: " + (int) sMS, "Crit: " + (int) sCrit + "%",
       "CDR: " + (int) sCDR + "%", "Lifesteal: " + (int) sLS + "%"};
    txt(g, CH[champ] + " - Stats", 36, 90, 18, CH_COL[champ]);
    for (int i = 0; i < l.length; i++)
      txt(g, l[i], 36, 125 + i * 24, 16, Color.WHITE);
  }

  boolean clickBtn()
  {
    for (Object[] b : btns)
      if (((Rectangle) b[0]).contains(mx, my))
      {
        ((Runnable) b[1]).run();
        return true;
      }
    return false;
  }

  String floorName()
  {
    for (Mob m : mobs)
      if (m.boss == 1 && !m.name.startsWith("Mini"))
        return new String[] {"Infernal", "Ocean", "Mountain", "Cloud"}[m.elem
          - 1] + " Dragon's Lair";
    if (floor == 40)
      return "Rift Herald's Pit";
    if (floor == 45)
      return "Baron Nashor's Pit";
    if (floor == 50)
      return "Elder Dragon's Sanctum";
    if (floor < 5)
      return "Wolf Den";
    if (floor < 10)
      return "Bramble Thicket";
    if (floor < 15)
      return "Gromp Swamp";
    if (floor < 20)
      return "Golem Quarry";
    return "Deep Jungle";
  }

  void carve(int x, int y, int w, int h)
  { // clear a rectangle of tiles
    for (int i = x; i < x + w; i++)
      for (int j = y; j < y + h; j++)
        if (i > 0 && j > 0 && i < MW - 1 && j < MH - 1)
          wall[i][j] = false;
  }

  void connect(Rectangle a, Rectangle b)
  { // L-shaped, 2-tile-wide corridor
    int ax = a.x + a.width / 2, ay = a.y + a.height / 2, bx = b.x + b.width / 2,
      by = b.y + b.height / 2;
    carve(Math.min(ax, bx), ay, Math.abs(ax - bx) + 2, 2);
    carve(bx, Math.min(ay, by), 2, Math.abs(ay - by) + 2);
  }

  @Override
  public void keyReleased(KeyEvent e)
  {
    if (e.getKeyCode() < 512)
      keys[e.getKeyCode()] = false;
  }

  @Override
  public void keyTyped(KeyEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
    hold = false;
  }

  @Override
  public void mouseMoved(MouseEvent e)
  {
    mx = e.getX();
    my = e.getY();
  }

  @Override
  public void mouseDragged(MouseEvent e)
  {
    mx = e.getX();
    my = e.getY();
  }

  @Override
  public void mouseClicked(MouseEvent e)
  {
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
  public void mouseExited(MouseEvent e)
  {
  }
}
