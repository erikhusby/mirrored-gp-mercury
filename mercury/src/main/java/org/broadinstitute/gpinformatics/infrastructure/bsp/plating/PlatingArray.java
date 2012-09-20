package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;


import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


//public class PlatingArray implements Iterable {
public class PlatingArray implements Iterable<PlatingArrayElement> {

    private Plateable[] array = new Plateable[96];

    private Plateable.Order order;


    private void add(int index, Plateable plateable) {
        array[index] = plateable;
    }


    private Plateable get(int index) {
        return array[index];
    }


    public int size() {
        int count = 0;
        for (Plateable plateable : array)
            if (plateable != null) count++;
        return count;
    }


    public PlatingArray(List<? extends Plateable> samples, List<? extends Plateable> controls, Plateable.Order order) {
        this(samples, controls, Plateable.Size.WELLS_96, order);
    }


    public PlatingArray(List<? extends Plateable> samples, List<? extends Plateable> controls, Plateable.Size size, Plateable.Order order) {

        this.order = order;

        if (samples == null)
            throw new RuntimeException("samples list can not be null!");


        if (controls != null) {
            // first array any controls with specified wells to their specified wells
            for (Plateable control : controls) {
                if (control.getSpecifiedWell() != null)
                    add(control.getSpecifiedWell().getIndex(order), control);
            }
        }


        // d is for destination index
        int d = 0;

        // samples don't have specified wells, just put them in wherever there's an empty slot from front to back
        // on the array
        for (Plateable sample : samples) {

            // make sure the slot we're looking at in the array is actually empty
            while (size() > d && get(d) != null) d++;
            add(d++, sample);
        }


        if (controls != null) {
            // same procedure for any controls with unspecified wells
            for (Plateable control : controls) {
                if (control.getSpecifiedWell() == null) {

                    // make sure the slot we're looking at in the array is actually empty
                    while (size() > d && get(d) != null) d++;
                    add(d++, control);
                }
            }
        }


        // now fill out the array with explicit empties if needed
        while (d < size()) {

            // only put an Empty here if the slot is actually empty.
            if (get(d) == null) {
                Empty empty = new Empty(d, size, order);
                add(d, empty);
            }

            // increment subscript irrespective of whether we inserted an Empty or not.  If we didn't insert an Empty
            // then there was already a fixed position Control in the slot.
            d++;
        }


    }


    private class BSPPlatingWellIterator implements Iterator<PlatingArrayElement> {

        private Plateable[] plateables;

        private Plateable.Order order;

        private int current = 0;

        public BSPPlatingWellIterator(Plateable[] plateables) {
            this.plateables = plateables;
        }

        @Override
        public boolean hasNext() {
            int temp = current;
            while (temp < plateables.length) {
                if (plateables[temp] != null)
                    return true;
                temp++;
            }

            return false;
        }

        @Override
        public PlatingArrayElement next() {
            if (!hasNext())
                throw new NoSuchElementException();

            while (current < plateables.length) {
                if (plateables[current] != null)
                    return new PlatingArrayElement(plateables[current], new Well(current++, Plateable.Size.WELLS_96, PlatingArray.this.order));
                current++;
            }

            // should not get here
            throw new NoSuchElementException("Sanity check failure");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<PlatingArrayElement> iterator() {
        return new BSPPlatingWellIterator(array);
    }

}