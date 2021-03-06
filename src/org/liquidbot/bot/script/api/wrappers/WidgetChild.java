package org.liquidbot.bot.script.api.wrappers;

import org.liquidbot.bot.Constants;
import org.liquidbot.bot.client.reflection.Reflection;
import org.liquidbot.bot.script.api.interfaces.Interactable;
import org.liquidbot.bot.script.api.methods.data.Menu;
import org.liquidbot.bot.script.api.methods.input.Mouse;
import org.liquidbot.bot.script.api.methods.interactive.Widgets;
import org.liquidbot.bot.script.api.util.Random;
import org.liquidbot.bot.script.api.util.Time;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/*
 * Created on 8/2/14
 */
public class WidgetChild implements Interactable {

    private Object raw;
    private int index;
    private int widgetX;
    private int widgetY;

    public WidgetChild(Object raw, int index) {
        this.raw = raw;
        this.index = index;
    }

    public int getX() {
        if (raw == null)
            return -1;
        if (widgetX > 0)
            return widgetX;
        int staticPosition = getStaticPosition();
        int relX = (int) Reflection.value("Widget#getRelativeX()", raw);
        int[] posX = (int[]) Reflection.value("Client#getWidgetPositionsX()", null);
        WidgetChild parent = getParent();
        int x = 0;
        if (parent != null) {
            x = parent.getX() - parent.getScrollX();
        } else {
            if (staticPosition != -1 && posX[staticPosition] > 0) {
                x = (posX[staticPosition] + (getType() > 0 ? relX : 0));
                widgetX = Constants.GAME_SCREEN.contains(new Point(x, 0)) ? x : relX;
                return widgetX;
            }
        }
        widgetX = (relX + x);
        return widgetX;
    }


    public int getY() {
        if (raw == null) {
            return -1;
        }
        if (widgetY > 0)
            return widgetY;
        int staticPosition = getStaticPosition();
        int relY = (int) Reflection.value("Widget#getRelativeY()", raw);
        int[] posY = (int[]) Reflection.value("Client#getWidgetPositionsY()", null);
        WidgetChild parent = getParent();
        int y = 0;
        if (parent != null) {
            y = parent.getY() - parent.getScrollY();
        } else {
            if (staticPosition != -1 && posY[staticPosition] > 0) {
                y = (posY[staticPosition] + (getType() > 0 ? relY : 0));
                widgetY = Constants.GAME_SCREEN.contains(new Point(0, y)) ? y : relY;
                return widgetY;
            }
        }
        widgetY = (y + relY);
        return widgetY;
    }

    public WidgetChild getParent() {
        if (raw == null) {
            return null;
        }
        Field widgetNodes = Reflection.field("Client#getWidgetNodes()");
        Field id = Reflection.field("WidgetNode#getId()");
        Field nodeUid = Reflection.field("Node#getUid()");
        int uid = getParentId();
        if (uid == -1) {
            int groupIdx = getId() >>> 16;
            final HashTableIterator hti = new HashTableIterator(Reflection.value(widgetNodes, null));
            for (Object n = hti.getFirst(); n != null; n = hti.getNext()) {
                if ((Reflection.value("WidgetNode#getId()", id, n)+"").equalsIgnoreCase(groupIdx+"")) {
                    uid = Integer.parseInt(((Long) Reflection.value(nodeUid, n)) + "");
                }
            }
        }

        if (uid == -1) {
            return null;
        }
        int parent = uid >> 16;
        int child = uid & 0xffff;

        return Widgets.get(parent, child);
    }

    public int getId() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getId()", raw);
    }

    public int getModelType() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getModelType()", raw);
    }

    public String[] getActions() {
        return (String[]) Reflection.value("Widget#getActions()", raw);
    }

    public String getText() {
        return (String) Reflection.value("Widget#getText()", raw);
    }

    public String getName() {
        return ((String) Reflection.value("Widget#getName()", raw)).replaceAll("<col=(.*?)>", "");
    }

    public int getTextColor() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getTextColor()", raw);
    }

    public int getRelativeX() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getRelativeX()", raw);
    }

    public int getRelativeY() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getRelativeY()", raw);
    }

    public int getWidth() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getWidth()", raw);
    }

    public int getHeight() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getHeight()", raw);
    }

	/**
	 * @Lorex
	 *
	 * @return Boolean : if widget is visible
	 */
	public boolean isVisible() {
		if(raw == null){
			return false;
		}
		if((boolean) Reflection.value("Widget#isHidden()", raw)){
			return false;
		}
		int parentId = this.getParentId();
		if(parentId == -1){
			return true;
		}
		if(parentId == 0){
			return false;
		}
		final WidgetChild parent =  Widgets.get(parentId >> 16, parentId & 65535);
		if(!parent.isVisible()){
			return false;
		}
		return true;
	}

    public int getRotationX() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getRotationX()", raw);
    }

    public int getRotationY() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getRotationY()", raw);
    }

    public int getRotationZ() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getRotationZ()", raw);
    }

    public int getContentType() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getContentType()", raw);
    }

    public int getScrollX() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getScrollX()", raw);
    }

    public int getScrollY() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getScrollY()", raw);
    }

    public int getTextureId() {
	    if(raw == null)
		    return -1;
        return (int) Reflection.value("Widget#getTextureId()", raw);
    }

    public int getModelId() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getModelId()", raw);
    }

    public int getBorderThickness() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getBorderThickness()", raw);
    }


    public int getType() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getType()", raw);
    }

    public int getParentId() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getParentId()", raw);
    }


    public int getItemStack() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getItemStack()", raw);
    }

    public int getStaticPosition() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getStaticPosition()", raw);
    }

    public int getParentIndex() {
        return getId() >> 16;
    }

    public int getItemId() {
        if(raw == null)
            return -1;
        return (int) Reflection.value("Widget#getItemId()", raw);
    }

    public int[] getSlotContentIds() {
        return (int[]) Reflection.value("Widget#getSlotContentIds()", raw);
    }

    public int[] getStackSizes() {
        return (int[]) Reflection.value("Widget#getStackSizes()", raw);
    }

    public int getIndex() {
        return index;
    }


    public WidgetChild[] getChildren() {
        List<WidgetChild> list = new ArrayList<>();
        Object[] children = (Object[]) Reflection.value("Widget#getChildren()", raw);
        if (children == null)
            return list.toArray(new WidgetChild[list.size()]);
        for (int i = 0; i < children.length; i++) {
            list.add(new WidgetChild(children[i], i));
        }
        return list.toArray(new WidgetChild[list.size()]);
    }

    public WidgetChild getChild(int index) {
        Object[] children = (Object[]) Reflection.value("Widget#getChildren()", raw);
        if (children == null || children.length <= index)
            return new WidgetChild(null, index);
        return new WidgetChild(children[index], index);
    }

    @Override
    public Point getInteractPoint() {
        Rectangle rect = getArea();
        return new Point(Random.nextInt(rect.x, rect.x + rect.width), Random.nextInt(rect.y, rect.y + rect.height));
    }

    public Point getLocation() {
        return new Point(getX(), getY());
    }

    public Rectangle getArea() {
        return new Rectangle(getLocation().x, getLocation().y, getWidth(), getHeight());
    }

    @Override
    public boolean interact(String action, String option) {
        int menuIndex = -1;
	    Point interactPoint = getInteractPoint();
        for (int i = 0; i < 5; i++) {
            menuIndex = Menu.index(action, option);
	        if (menuIndex > -1 && this.getArea().contains(Mouse.getLocation()))
                break;
            if (org.liquidbot.bot.script.api.methods.data.Menu.isOpen() && menuIndex == -1)
                org.liquidbot.bot.script.api.methods.data.Menu.interact("Cancel");
            Mouse.move(interactPoint);
            Time.sleep(100, 150);
        }
        return menuIndex > -1 && org.liquidbot.bot.script.api.methods.data.Menu.interact(action, option);
    }

    @Override
    public boolean interact(String action) {
        return interact(action, getName());
    }


	@Override
	public boolean click(boolean left) {
		Point interactingPoint;
		Rectangle bounds;
		for(int i = 0; i < 3; i++){
			interactingPoint = this.getInteractPoint();
			bounds = getArea();
			Mouse.move(interactingPoint);
			if (bounds == null || bounds.contains(Mouse.getLocation())){
				Mouse.click(left);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean click() {
		return click(true);
	}

    class HashTableIterator {

        private Object hashTable;
        private int currindex;
        private Object curr;

        HashTableIterator(Object hashTable) {
            this.hashTable = hashTable;
        }

        final Object getFirst() {
            currindex = 0;
            return getNext();
        }

        final Object getNext() {
            if (hashTable == null)
                return null;
            final Object[] buckets = (Object[]) Reflection.value("NodeHashTable#getBuckets()", hashTable);
            if (buckets == null)
                return null;
            if (currindex > 0 && curr != buckets[currindex - 1]) {
                final Object node = curr;
                if (node == null) {
                    return null;
                }
                curr = Reflection.value("Node#getPrevious()", node);
                return node;
            }
            while (currindex < buckets.length) {
                final Object node1 = Reflection.value("Node#getPrevious()", buckets[currindex++]);
                if (node1 != null) {
                    if (buckets[currindex - 1] != node1) {
                        curr = Reflection.value("Node#getPrevious()", node1);
                        return node1;
                    }
                }
            }
            return null;
        }
    }

}
