package com.lumina.bridge.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A list of values for {@link Data}: mostly sub-trees, now and then numbers or text. */
public final class DataList implements Iterable<Data> {
    final List<Object> items = new ArrayList<>();

    public DataList add(Data value) {
        items.add(value);
        return this;
    }

    public DataList addString(String value) {
        items.add(value);
        return this;
    }

    public int size() {
        return items.size();
    }

    /** The sub-tree at {@code index}; an empty one when that is something else. */
    public Data getData(int index) {
        return index >= 0 && index < items.size() && items.get(index) instanceof Data d ? d : new Data();
    }

    public String getString(int index) {
        return index >= 0 && index < items.size() && items.get(index) instanceof String s ? s : "";
    }

    /** Only the sub-trees, in order (the others are skipped, as a typed list read does in Minecraft). */
    @Override
    public java.util.Iterator<Data> iterator() {
        List<Data> out = new ArrayList<>();
        for (Object o : items)
            if (o instanceof Data d)
                out.add(d);
        return out.iterator();
    }

    /** The text entries, in order. */
    public List<String> strings() {
        List<String> out = new ArrayList<>();
        for (Object o : items)
            if (o instanceof String s)
                out.add(s);
        return out;
    }

    /** The stored values, for the version side to turn into its own tags. */
    public List<Object> raw() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataList l) || l.items.size() != items.size())
            return false;
        for (int i = 0; i < items.size(); i++)
            if (!Data.same(items.get(i), l.items.get(i)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        return items.size();
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
