package org.liquidbot.bot.script.randevent.impl;

import org.liquidbot.bot.script.api.interfaces.Condition;
import org.liquidbot.bot.script.api.interfaces.Filter;
import org.liquidbot.bot.script.api.interfaces.PaintListener;
import org.liquidbot.bot.script.api.methods.data.Game;
import org.liquidbot.bot.script.api.methods.data.movement.Camera;
import org.liquidbot.bot.script.api.methods.data.movement.Walking;
import org.liquidbot.bot.script.api.methods.interactive.GameEntities;
import org.liquidbot.bot.script.api.methods.interactive.Players;
import org.liquidbot.bot.script.api.methods.interactive.Widgets;
import org.liquidbot.bot.script.api.util.Random;
import org.liquidbot.bot.script.api.util.Time;
import org.liquidbot.bot.script.api.wrappers.*;
import org.liquidbot.bot.script.randevent.RandomEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


/**
 * Solve Maze RandomEvent
 *
 * Created by Lemons on 8/16/14.
 */
public class Maze extends RandomEvent implements PaintListener {


    private int IGNORED_DOOR_ID = -1;

    private final String FINAL_CHEST = "Strange shrine";
    private Door door;
    private Tile centerTile;
    private Tile endTile;
    private ArrayList<Tile> blackDoor = new ArrayList<Tile>();

    private int[][] flags;
    private int offX;
    private int offY;
    private Boolean[] canInteractCache = new Boolean[999999];


    private Tile[] tiles;

    @Override
    public String getName() {
        return "Maze";
    }

    @Override
    public String getAuthor() {
        return "Lemons";
    }

    @Override
    public boolean active() {
        return Game.isLoggedIn() && GameEntities.getNearest(FINAL_CHEST).isValid();
    }

    @Override
    public void solve() {
        if (door == null)
            door = new Door();
	    if(IGNORED_DOOR_ID == -1){
		    ArrayList<Integer> ids =new ArrayList<>();
		    for(GameObject gameObject : GameEntities.getAll("Wall")){
			     if(gameObject.isValid()){
				     ids.add(gameObject.getId());
			     }
		     }
		    IGNORED_DOOR_ID = getMostPopularElement(ids.toArray(new Integer[ids.size()]));
	    }
        Tile chest = chestLocation();
        if (centerTile == null) {
            GameObject endDoor = GameEntities.getNearest(chest.getLocation(), new Filter<GameObject>() {
                @Override
                public boolean accept(GameObject gameObject) {
                    return gameObject.isValid() && gameObject.getName() != null && gameObject.getName().equals("Wall") && gameObject.getId() != IGNORED_DOOR_ID;
                }
            });
            endTile = endDoor.getLocation();
            centerTile = center(endDoor.getLocation(), chest.getLocation());
        }
        if (chest.distanceTo() < 2) {
            if (chest.isOnScreen()) {
                chest.click();
                Time.sleep(new Condition() {
                    @Override
                    public boolean active() {
                        return GameEntities.getNearest("Strange shrine").isValid();
                    }
                }, 3000);
            } else {
                chest.turnTo();
            }
        } else {
            if (tiles == null || (centerTile.isOnMap() && !tiles[tiles.length - 1].equals(centerTile))) {
                setStatus("Generating Path");
                TilePath tilePath = findPath(Players.getLocal().getLocation(), centerTile.isOnMap() ? centerTile : center(centerTile));
                tiles = tilePath == null ? tiles : tilePath.getTiles();
            }
            if (tiles == null)
                log.error("Can't find error to center Tile");
            GameObject nextDoor = getNextDoor();
            if (nextDoor.isValid()) {
                setStatus("Going to next Door");
                door.solve(nextDoor);
            }
        }
    }

	private static int getMostPopularElement(Integer[] a) {
		int counter = 0, curr, maxvalue, maxcounter = -1;
		maxvalue = curr = a[0];
		for (int e : a) {
			if (curr == e) {
				counter++;
			} else {
				if (counter > maxcounter) {
					maxcounter = counter;
					maxvalue = curr;
				}
				counter = 0;
				curr = e;
			}
		}
		if (counter > maxcounter) {
			maxvalue = curr;
		}
		return maxvalue;
	}


    private Tile chestLocation() {
        ArrayList<Point> points = new ArrayList<>();
        for (GameObject gameObject : GameEntities.getAll("Strange shrine")) {
            if (gameObject.isValid()) {
                points.add(new Point(gameObject.getX(), gameObject.getY()));
            }
        }
        double centroidX = 0, centroidY = 0;
        for (Point point : points) {
            centroidX += point.getX();
            centroidY += point.getY();
        }
        return new Tile((int) (centroidX / points.size()), (int) (centroidY / points.size()));
    }

    private GameObject getNextDoor() {
        for (Tile tile : tiles) {
            GameObject obj = getDoorAt(tile);
            if (obj.isValid() && !blackDoor.contains(obj.getLocation())) {
                return obj;
            }
        }
        return GameEntities.nil();
    }


    private Tile center(Tile p, Tile l) {
        return new Tile((l.getX() + p.getX()) / 2, (l.getY() + p.getY()) / 2, Game.getPlane());
    }

    private Tile center(Tile l) {
        return center(Players.getLocal().getLocation(), l);
    }

    @Override
    public void reset() {
        blackDoor.clear();
        centerTile = null;
    }

    @Override
    public void render(Graphics2D graphics) {
        if (tiles != null) {
            for (Tile tile : tiles) {
                if (tile.isOnMap()) {
                    Point point = tile.getPointOnMap();
                    graphics.setColor(Color.CYAN);
                    graphics.fillRect(point.x, point.y, 3, 3);
                }
            }
        }
        if (centerTile != null) {
            if (centerTile.isOnScreen()) {
                centerTile.draw(graphics, Color.GREEN);
            }
        }
        if (blackDoor.size() > 0) {
            for (Tile t : blackDoor) {
                if (t.isOnScreen()) {
                    t.draw(graphics, Color.RED);
                }
            }
        }
    }

    public TilePath findPath(Tile start, Tile end) {
        for (int i = 0; i < 30 && !Game.isLoggedIn(); i++)
            Time.sleep(100, 200);
        if (start.getZ() != end.getZ())
            return null;
        final int currPlane = start.getZ();
        final int currX = start.getX() - baseX(), currY = start.getY() - baseY();
        int destX = end.getX() - baseX(), destY = end.getY() - baseY();

        final int plane = Game.getPlane();
        if (currPlane != plane)
            return null;
        flags = Walking.getCollisionFlags(plane);
        final Tile offset = Walking.getCollisionOffset(plane);
        offX = offset.getX();
        offY = offset.getY();

        if (flags == null || currX < 0 || currY < 0 || currX >= flags.length
                || currY >= flags.length) {
            return null;
        } else if (destX < 0 || destY < 0 || destX >= flags.length
                || destY >= flags.length) {
            if (destX < 0) {
                destX = 0;
            } else if (destX >= flags.length) {
                destX = flags.length - 1;
            }
            if (destY < 0) {
                destY = 0;
            } else if (destY >= flags.length) {
                destY = flags.length - 1;
            }
        }

        final HashSet<Node> open = new HashSet<Node>();
        final HashSet<Node> closed = new HashSet<Node>();
        Node curr = new Node(currX, currY, currPlane);
        final Node dest = new Node(destX, destY, currPlane);

        curr.f = heuristic(curr, dest);
        open.add(curr);

        while (!open.isEmpty()) {
            curr = lowest_f(open);
            if (curr.equals(dest)) {
                return new TilePath(path(curr, baseX(), baseY()));
            }
            open.remove(curr);
            closed.add(curr);
            for (final Node next : successors(curr)) {
                if (!closed.contains(next)) {
                    final double t = curr.g + dist(curr, next);
                    boolean use_t = false;
                    if (!open.contains(next)) {
                        open.add(next);
                        use_t = true;
                    } else if (t < next.g) {
                        use_t = true;
                    }
                    if (use_t) {
                        next.prev = curr;
                        next.g = t;
                        next.f = t + heuristic(next, dest);
                    }
                }
            }
        }

        return null;
    }

    public int baseX() {
        return Game.getBaseX();
    }

    public int baseY() {
        return Game.getBaseY();
    }

    private double heuristic(final Node start, final Node end) {
        final double dx = Math.abs(start.x - end.x);
        final double dy = Math.abs(start.y - end.y);
        final double diag = Math.min(dx, dy);
        final double straight = dx + dy;
        return Math.sqrt(2.0) * diag + straight - 2 * diag;
    }

    private double dist(final Node start, final Node end) {
        if (start.x != end.x && start.y != end.y) {
            return 1.41421356;
        } else {
            return 1.0;
        }
    }

    private Node lowest_f(final Set<Node> open) {
        Node best = null;
        for (final Node t : open) {
            if (best == null || t.f < best.f) {
                best = t;
            }
        }
        return best;
    }

    private GameObject[] gameObjectAt(final int x, final int y, boolean regionCoords) {
        final int addX = regionCoords ? baseX() : 0;
        final int addY = regionCoords ? baseY() : 0;
        try {
            return GameEntities.getAll(new Filter<GameObject>() {

                @Override
                public boolean accept(GameObject obj) {
                    return obj.getX() == addX + x && obj.getY() == addY + y && obj.getName() != null;
                }

            });
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private GameObject[] gameObjectAt(final int x, final int y) {
        return gameObjectAt(x, y, true);
    }

    private java.util.List<Node> successors(final Node t) {
        LinkedList<Node> tiles = new LinkedList<Node>();
        final int x = t.x, y = t.y, z = t.z;
        final int f_x = x - offX, f_y = y - offY;
        final int here = flags[f_x][f_y];
        final int upper = flags.length - 1;

        Tile curTile = new Tile(t.x + baseX(), t.y + baseY(), Game.getPlane());

        boolean noMoreObjs = false;

        if (f_x < upper) {
            if ((here & Walking.Flag.WALL_EAST) == 0 && (flags[f_x + 1][f_y] & Walking.Flag.BLOCKED) == 0) {
                tiles.add(new Node(x + 1, y, z));
            } else if (!multiWall(here)
                    && (canInteract(0, gameObjectAt(f_x, f_y)) || canInteract(2, gameObjectAt(f_x + 1, f_y)))) {
                noMoreObjs = true;
                tiles.add(new Node(x + 1, y, z));
            } else if (!noMoreObjs && ((here & Walking.Flag.WALL_EAST) != 0 && (here & Walking.Flag.WALL_NORTH) != 0)
                    && canInteract(0, gameObjectAt(f_x, f_y))) {
                noMoreObjs = true;
                tiles.add(new Node(x + 1, y, z));
            }
        }
        if (f_y < upper) {
            if ((here & Walking.Flag.WALL_NORTH) == 0 && (flags[f_x][f_y + 1] & Walking.Flag.BLOCKED) == 0) {
                tiles.add(new Node(x, y + 1, z));
            } else if (!multiWall(here) && !noMoreObjs
                    && (canInteract(1, gameObjectAt(f_x, f_y)) || canInteract(3, gameObjectAt(f_x, f_y + 1)))) {
                noMoreObjs = true;
                tiles.add(new Node(x, y + 1, z));
            } else if (!noMoreObjs && ((here & Walking.Flag.WALL_NORTH) != 0 && (here & Walking.Flag.WALL_WEST) != 0)
                    && canInteract(1, gameObjectAt(f_x, f_y))) {
                noMoreObjs = true;
                tiles.add(new Node(x, y + 1, z));
            }
        }
        if (f_x > 0) {
            if ((here & Walking.Flag.WALL_WEST) == 0 && (flags[f_x - 1][f_y] & Walking.Flag.BLOCKED) == 0) {
                tiles.add(new Node(x - 1, y, z));
            } else if (!multiWall(here)
                    && !noMoreObjs && (canInteract(2, gameObjectAt(f_x, f_y)) || canInteract(0, gameObjectAt(f_x - 1, f_y)))) {
                noMoreObjs = true;
                tiles.add(new Node(x - 1, y, z));
            } else if (((here & Walking.Flag.WALL_WEST) != 0 && (here & Walking.Flag.WALL_SOUTH) != 0)
                    && !noMoreObjs && canInteract(2, gameObjectAt(f_x, f_y))) {
                noMoreObjs = true;
                tiles.add(new Node(x - 1, y, z));
            }
        }
        if (f_y > 0) {
            if ((here & Walking.Flag.WALL_SOUTH) == 0 && (flags[f_x][f_y - 1] & Walking.Flag.BLOCKED) == 0) {
                tiles.add(new Node(x, y - 1, z));
            } else if (!multiWall(here)
                    && !noMoreObjs && (canInteract(3, gameObjectAt(f_x, f_y)) || canInteract(1, gameObjectAt(f_x, f_y - 1)))) {
                noMoreObjs = true;
                tiles.add(new Node(x, y - 1, z));
            } else if (((here & Walking.Flag.WALL_SOUTH) != 0 && (here & Walking.Flag.WALL_EAST) != 0)
                    && !noMoreObjs && (canInteract(3, gameObjectAt(f_x, f_y)))) {
                noMoreObjs = true;
                tiles.add(new Node(x, y - 1, z));
            }
        }

        if (f_x > 0
                && f_y > 0
                && (here & (Walking.Flag.WALL_SOUTHWEST
                | Walking.Flag.WALL_SOUTH | Walking.Flag.WALL_WEST)) == 0
                && (flags[f_x - 1][f_y - 1] & Walking.Flag.BLOCKED) == 0
                && (flags[f_x][f_y - 1] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_WEST)) == 0
                && (flags[f_x - 1][f_y] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_SOUTH)) == 0) {
            tiles.add(new Node(x - 1, y - 1, z));
        }
        if (f_x > 0
                && f_y < upper
                && (here & (Walking.Flag.WALL_NORTHWEST
                | Walking.Flag.WALL_NORTH | Walking.Flag.WALL_WEST)) == 0
                && (flags[f_x - 1][f_y + 1] & Walking.Flag.BLOCKED) == 0
                && (flags[f_x][f_y + 1] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_WEST)) == 0
                && (flags[f_x - 1][f_y] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_NORTH)) == 0) {
            tiles.add(new Node(x - 1, y + 1, z));
        }
        if (f_x < upper
                && f_y > 0
                && (here & (Walking.Flag.WALL_SOUTHEAST
                | Walking.Flag.WALL_SOUTH | Walking.Flag.WALL_EAST)) == 0
                && (flags[f_x + 1][f_y - 1] & Walking.Flag.BLOCKED) == 0
                && (flags[f_x][f_y - 1] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_EAST)) == 0
                && (flags[f_x + 1][f_y] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_SOUTH)) == 0) {
            tiles.add(new Node(x + 1, y - 1, z));
        }
        if (f_x > 0
                && f_y < upper
                && (here & (Walking.Flag.WALL_NORTHEAST
                | Walking.Flag.WALL_NORTH | Walking.Flag.WALL_EAST)) == 0
                && (flags[f_x + 1][f_y + 1] & Walking.Flag.BLOCKED) == 0
                && (flags[f_x][f_y + 1] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_EAST)) == 0
                && (flags[f_x + 1][f_y] & (Walking.Flag.BLOCKED | Walking.Flag.WALL_NORTH)) == 0) {
            tiles.add(new Node(x + 1, y + 1, z));
        }

        // if (!CWalking.stop) Time.sleep(200);

        return tiles;
    }

    private boolean multiWall(int here) {
        int i = 0;
        if ((here & Walking.Flag.WALL_EAST) != 0)
            i++;
        if ((here & Walking.Flag.WALL_WEST) != 0)
            i++;
        if ((here & Walking.Flag.WALL_NORTH) != 0)
            i++;
        if ((here & Walking.Flag.WALL_SOUTH) != 0)
            i++;
        return i > 1;
    }

    private boolean canInteract(int dir, GameObject[] objs) {
        for (GameObject obj : objs) {
            if (obj == null) {
                continue;
            }
            int flag = Walking.getCollisionFlags(Game.getPlane())
                    [obj.getLocation().getX() - baseX()]
                    [obj.getLocation().getY() - baseY()];

            if ((flag & Walking.Flag.WALL_EAST) != 0 && (flag & Walking.Flag.WALL_NORTH) != 0 && dir == 0) {
                if (processInteract(obj)) return true;
            } else if ((flag & Walking.Flag.WALL_NORTH) != 0 && (flag & Walking.Flag.WALL_WEST) != 0 && dir == 1) {
                if (processInteract(obj)) return true;
            } else if ((flag & Walking.Flag.WALL_WEST) != 0 && (flag & Walking.Flag.WALL_SOUTH) != 0 && dir == 3) {
                if (processInteract(obj)) return true;
            } else if ((flag & Walking.Flag.WALL_SOUTH) != 0 && (flag & Walking.Flag.WALL_EAST) != 0 && dir == 2) {
                if (processInteract(obj)) return true;
            } else {
                if (processInteract(obj)) return true;
            }

            if (processInteract(obj)) return true;
        }

        return false;
    }

    private boolean processInteract(GameObject obj) {
        int tmpId = obj.getId();
        if (canInteractCache[tmpId] != null) {
            if (canInteractCache[tmpId])
                return true;
            return false;
        }

        boolean canInteract = door.isActive(obj);
        canInteractCache[obj.getId()] = canInteract;

        return canInteract;
    }

    private Tile[] path(final Node end, final int base_x,
                        final int base_y) {
        final LinkedList<Tile> path = new LinkedList<Tile>();
        Node p = end;
        while (p != null) {
            path.addFirst(p.get(base_x, base_y));
            p = p.prev;
        }
        return path.toArray(new Tile[path.size()]);
    }

    private final class Node {
        public final int x, y, z;
        public Node prev;
        public double g, f;

        public Node(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            g = f = 0;
        }

        @Override
        public int hashCode() {
            return x << 4 | y;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Node) {
                final Node n = (Node) o;
                return x == n.x && y == n.y && z == n.z;
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        public Tile get(final int baseX, final int baseY) {
            return new Tile(x + baseX(), y + baseY(), z);
        }
    }

    public GameObject getDoorAt(final Tile tile) {
        return GameEntities.getNearest(new Filter<GameObject>() {

            @Override
            public boolean accept(GameObject obj) {
                return obj.isValid() && obj.getLocation().equals(tile) && obj.getName() != null && obj.getName().equalsIgnoreCase("Wall") && obj.getId() != IGNORED_DOOR_ID;
            }
        });
    }

    private final class Door {

        public boolean isActive(GameObject obj) {
            return obj.isValid() && obj.getName() != null && obj.getName().equalsIgnoreCase("Wall") && obj.getId() != IGNORED_DOOR_ID;
        }

        public boolean isDoorOpen(Tile start, int startId) {
            GameObject o = getDoorAt(start);
            return !o.isValid() || o.getId() == 83 || o.getId() != startId || o.getActions().length == 0;
        }


        public boolean solve(GameObject object) {
            boolean success = false;
            if (Camera.getPitch() < 80) {
                Camera.setPitch(Random.nextInt(80, 90));
            }
            if (object.isValid()) {
                if (object.distanceTo() >= 2) {
                    Walking.walkTo(object);
                    Camera.setAngle(Camera.getAngleTo(endTile));
                    Time.sleepTillStop(5000);
                }
                if (object.isOnScreen() && object.distanceTo() < 2) {
                    Tile start = object.getLocation();
                    int id = object.getId();


                    Camera.setAngle(Camera.getAngleTo(object.getLocation().equals(endTile) ? chestLocation() :endTile));
                    success = object.interact("Open", object.getName());
                    if (success) {
                        Time.sleepTillStop(1000);
                        for (int x = 0; x < 15; x++) {
                            if (isDoorOpen(start, id)) {
                                setStatus("Found door Open");
                                blackDoor.add(start);
                                break;
                            }
                            Time.sleep(300, 350);
                        }
                        WidgetChild widgetChild = Widgets.get(204, 0);
                        if (widgetChild.isVisible() && widgetChild.getText() != null && widgetChild.getText().toLowerCase().contains("the right way")) {
                            blackDoor.add(start);
                            Widgets.clickContinue();
                        }
                        Time.sleepTillStop(3000);
                    }
                }
            }
            return success;
        }
    }
}
